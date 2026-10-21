package me.wanttobee.tasktussle.pvp

import java.util.UUID
import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.named
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.ItemStack

class PvpSystem(private val system: TaskTussleSystem) {
	private val optedOut = mutableSetOf<UUID>()
	private val lastItemUse = mutableMapOf<UUID, Int>()
	private val nameTags = PvpNameTags()
	val canChoose get() = system.currentGame() == null && system.settings.pvpMode == PvpMode.OPT_IN_OUT

	fun isOptedIn(player: UUID) = player !in optedOut

	fun choiceIcon(player: UUID): ItemStack {
		val optedIn = isOptedIn(player)
		val mode = if (optedIn) PvpMode.ON else PvpMode.OFF
		return named(
			mode.icon,
			"PVP: ${if (optedIn) "Opted in" else "Opted out"}",
			if (optedIn) ChatFormatting.GREEN else ChatFormatting.GRAY,
			listOf("Left or right click to toggle", "Your choice locks when the game starts", "Teammate damage is always enabled"),
		).apply {
			remove(DataComponents.ATTRIBUTE_MODIFIERS)
			mode.iconModel?.let { set(DataComponents.ITEM_MODEL, it) }
		}
	}

	fun toggle(player: ServerPlayer): Boolean {
		if (!canChoose) return false
		if (!optedOut.add(player.uuid)) optedOut.remove(player.uuid)
		system.items.syncPvpToggle(player)
		refreshNames(player.level().server)
		player.containerMenu.broadcastFullState()
		return true
	}

	fun useToggle(player: ServerPlayer): Boolean {
		val tick = player.level().server.tickCount
		// A single right-click can send interaction, item-use, and swing packets.
		if (lastItemUse[player.uuid]?.let { tick - it < 4 } == true) return false
		lastItemUse[player.uuid] = tick
		return toggle(player)
	}

	fun allowsDamage(target: ServerPlayer, source: DamageSource): Boolean {
		val game = system.currentGame() ?: return true
		val attacker = source.entity as? ServerPlayer ?: return true
		if (source.directEntity !== attacker && source.directEntity !is Projectile) return true
		val sameTeam = game.teams.any { attacker.uuid in it.members && target.uuid in it.members }
		return system.settings.pvpMode.allowsDamage(sameTeam, isOptedIn(attacker.uuid), isOptedIn(target.uuid))
	}

	fun refreshNames(server: MinecraftServer) {
		for (player in server.playerList.players) {
			val choice = isOptedIn(player.uuid).takeIf { system.settings.pvpMode == PvpMode.OPT_IN_OUT }
			nameTags.update(server.scoreboard, player.scoreboardName, choice).forEach(server.playerList::broadcastAll)
		}
	}

	fun onJoin(player: ServerPlayer) {
		nameTags.forJoiningPlayer().forEach(player.connection::send)
		refreshNames(player.level().server)
	}

	fun onDisconnect(player: ServerPlayer) {
		val server = player.level().server
		nameTags.update(server.scoreboard, player.scoreboardName, null).forEach(server.playerList::broadcastAll)
	}

	fun clearNames(server: MinecraftServer) {
		nameTags.clear(server.scoreboard).forEach(server.playerList::broadcastAll)
	}
}
