package me.wanttobee.tasktussle.pvp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import net.minecraft.data.AtlasIds
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.ObjectContents
import net.minecraft.network.chat.contents.objects.AtlasSprite
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.resources.Identifier
import net.minecraft.world.scores.Scoreboard
import java.nio.file.Files
import java.nio.file.Path

class PvpModeTest {
	@Test
	fun crossTeamDamageRequiresTheSelectedModeAndBothPlayersConsent() {
		assertEquals(PvpMode.ON, TaskTussleSettings().pvpMode)
		for (attackerOptedIn in listOf(false, true)) for (targetOptedIn in listOf(false, true)) {
			for (mode in PvpMode.entries) {
				assertTrue(mode.allowsDamage(true, attackerOptedIn, targetOptedIn), "Teammates can always damage each other")
				val expected = mode == PvpMode.ON || mode == PvpMode.OPT_IN_OUT && attackerOptedIn && targetOptedIn
				assertEquals(expected, mode.allowsDamage(false, attackerOptedIn, targetOptedIn), "$mode: attacker=$attackerOptedIn, target=$targetOptedIn")
			}
		}
	}

	@Test
	fun nameIconsTrackChoicesWithoutChangingServerTeams() {
		val scoreboard = Scoreboard()
		val original = scoreboard.addPlayerTeam("existing").apply { setPlayerPrefix(Component.literal("[Team] ")) }
		scoreboard.addPlayerToTeam("Alice", original)
		val tags = PvpNameTags()
		fun sprite(packet: ClientboundSetPlayerTeamPacket): AtlasSprite {
			val prefix = packet.parameters.orElseThrow().playerPrefix()
			assertEquals(original.playerPrefix, prefix.siblings.last())
			return assertIs<AtlasSprite>(assertIs<ObjectContents>(prefix.siblings.first().contents).contents())
		}
		val added = tags.update(scoreboard, "Alice", true).single()
		assertEquals(ClientboundSetPlayerTeamPacket.Action.ADD, added.teamAction)
		assertEquals(AtlasSprite(AtlasIds.ITEMS, Identifier.parse("minecraft:item/iron_sword")), sprite(added))
		assertTrue(tags.update(scoreboard, "Alice", true).isEmpty())
		val changed = tags.update(scoreboard, "Alice", false).single()
		assertEquals(AtlasSprite(AtlasIds.ITEMS, Identifier.parse("tasktussle:item/peacefull")), sprite(changed))
		assertEquals(sprite(changed), sprite(tags.forJoiningPlayer().single()))
		assertTrue(Files.isRegularFile(Path.of("..", "resourcepack", "assets", "tasktussle", "textures", "item", "peacefull.png")))
		assertSame(original, scoreboard.getPlayersTeam("Alice"))
		assertEquals(listOf("existing"), scoreboard.teamNames.toList())
		val removed = tags.update(scoreboard, "Alice", null)
		assertEquals(ClientboundSetPlayerTeamPacket.Action.REMOVE, removed[0].teamAction)
		assertEquals(ClientboundSetPlayerTeamPacket.Action.ADD, removed[1].playerAction)
		assertEquals("existing", removed[1].name)
		assertTrue(tags.forJoiningPlayer().isEmpty())
		tags.update(scoreboard, "Bob", true)
		assertEquals(ClientboundSetPlayerTeamPacket.Action.REMOVE, tags.clear(scoreboard).single().teamAction)
		assertTrue(tags.forJoiningPlayer().isEmpty())
		assertTrue(tags.clear(scoreboard).isEmpty())
	}
}
