package me.wanttobee.tasktussle.pvp

import net.minecraft.ChatFormatting
import net.minecraft.data.AtlasIds
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.objects.AtlasSprite
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.resources.Identifier
import net.minecraft.world.scores.PlayerTeam
import net.minecraft.world.scores.Scoreboard
import net.minecraft.world.scores.Team
import java.util.Optional

/** Client-only team prefixes decorate names without changing the server's teams or damage rules. */
internal class PvpNameTags {
	private val tags = mutableMapOf<String, PlayerTeam>()
	private var nextId = 0

	fun update(scoreboard: Scoreboard, playerName: String, optedIn: Boolean?): List<ClientboundSetPlayerTeamPacket> {
		val previous = tags[playerName]
		val original = scoreboard.getPlayersTeam(playerName)
		if (optedIn == null) {
			if (previous == null) return emptyList()
			tags.remove(playerName)
			return buildList {
				add(ClientboundSetPlayerTeamPacket.createRemovePacket(previous))
				if (original != null) add(ClientboundSetPlayerTeamPacket.createPlayerPacket(original, playerName, ClientboundSetPlayerTeamPacket.Action.ADD))
			}
		}
		val baseName = "tt_pvp_${original?.name.orEmpty()}_$playerName"
		val team = previous ?: PlayerTeam(Scoreboard(), generateSequence(baseName) { "${baseName}_${nextId++}" }.first { scoreboard.getPlayerTeam(it) == null }).apply {
			players.add(playerName)
			tags[playerName] = this
		}
		val before = previous?.let { ClientboundSetPlayerTeamPacket.Parameters(it) }
		team.setPlayerPrefix(namePrefix(optedIn).append(original?.playerPrefix ?: Component.empty()))
		team.setPlayerSuffix(original?.playerSuffix ?: Component.empty())
		team.color = original?.color ?: Optional.empty()
		team.nameTagVisibility = original?.nameTagVisibility ?: Team.Visibility.ALWAYS
		team.collisionRule = original?.collisionRule ?: Team.CollisionRule.ALWAYS
		team.isAllowFriendlyFire = original?.isAllowFriendlyFire ?: true
		team.setSeeFriendlyInvisibles(original?.canSeeFriendlyInvisibles() ?: false)
		return if (before == ClientboundSetPlayerTeamPacket.Parameters(team)) emptyList()
		else listOf(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, previous == null))
	}

	fun forJoiningPlayer() = tags.values.map { ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(it, true) }

	fun clear(scoreboard: Scoreboard) = tags.keys.toList().flatMap { update(scoreboard, it, null) }

	private fun namePrefix(optedIn: Boolean) = Component.empty()
		.append(Component.`object`(
			AtlasSprite(AtlasIds.ITEMS, Identifier.parse(if (optedIn) "minecraft:item/iron_sword" else "tasktussle:item/peacefull")),
			Component.literal(if (optedIn) "PvP on" else "PvP off"),
		).withStyle { it.withColor(ChatFormatting.WHITE).withBold(false).withItalic(false) })
		.append(" ")
}
