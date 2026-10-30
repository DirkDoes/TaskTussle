package me.wanttobee.tasktussle.base.generic

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.pvp.PvpMode
import net.minecraft.SharedConstants
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.registries.VanillaRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.Bootstrap
import net.minecraft.world.entity.EntityEquipment
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import me.wanttobee.tasktussle.tasks.tradeTask.TradeSettingsMenu

class SettingsMenuTest {
	private fun actions(menu: ActionChestMenu) = ActionChestMenu::class.java.getDeclaredField("actions").apply { isAccessible = true }.get(menu) as Map<*, *>

	@Test
	fun gamePagesUseClickableMarkerCellsAndInlineRules() {
		SharedConstants.tryDetectVersion()
		Bootstrap.bootStrap()
		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup()).forEach { it.apply() }
		val system = TaskTussleSystem()
		val original = system.settings.copy()
		val inventory = Inventory::class.java.constructors.single().newInstance(null, EntityEquipment()) as Inventory
		val menu = SettingsMenu(1, inventory, system)
		assertTrue(menu.customBackground)
		assertEquals("Bingo", menu.getSlot(0).item.get(DataComponents.CUSTOM_NAME)?.string)
		assertEquals("Lockout", menu.getSlot(9).item.get(DataComponents.CUSTOM_NAME)?.string)
		assertTrue(menu.getSlot(0).item.get(DataComponents.LORE)?.lines.isNullOrEmpty())
		assertTrue(menu.getSlot(9).item.get(DataComponents.LORE)?.lines.isNullOrEmpty())
		assertEquals("Teams: 2", menu.getSlot(5).item.get(DataComponents.CUSTOM_NAME)?.string)
		assertEquals("Play Bingo", menu.getSlot(8).item.get(DataComponents.CUSTOM_NAME)?.string)
		assertEquals(Identifier.parse("tasktussle:play_button"), menu.getSlot(8).item.get(DataComponents.ITEM_MODEL))
		assertTrue(actions(menu).containsKey(8))
		assertSame(actions(menu)[0], actions(menu)[1])
		assertSame(actions(menu)[9], actions(menu)[10])
		assertEquals(true, menu.getSlot(0).item.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
		assertEquals(Identifier.parse("tasktussle:game_marker"), menu.getSlot(1).item.get(DataComponents.ITEM_MODEL))
		assertTrue(menu.getSlot(14).item.get(DataComponents.CUSTOM_NAME)!!.string.startsWith("Card visibility:"))
		assertEquals("Win condition: 1 line", menu.getSlot(13).item.get(DataComponents.CUSTOM_NAME)?.string)
		menu.selectGame("lockout")
		assertEquals(true, menu.getSlot(9).item.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
		assertEquals(null, menu.getSlot(0).item.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
		for (row in 0..5) {
			val slot = row * 9 + 1
			if (row == 1) assertEquals(Identifier.parse("tasktussle:game_marker"), menu.getSlot(slot).item.get(DataComponents.ITEM_MODEL))
			else assertTrue(menu.getSlot(slot).item.isEmpty)
			assertEquals(row < 2, actions(menu).containsKey(slot))
		}
		assertEquals(true, menu.getSlot(10).item.get(DataComponents.TOOLTIP_DISPLAY)?.hideTooltip())
		assertEquals("Play Lockout", menu.getSlot(8).item.get(DataComponents.CUSTOM_NAME)?.string)
		assertEquals("Teams per task: 1", menu.getSlot(11).item.get(DataComponents.CUSTOM_NAME)?.string)
		assertTrue(menu.getSlot(13).item.isEmpty)
		assertTrue(menu.getSlot(14).item.isEmpty)
		assertTrue(menu.getSlot(29).item.isEmpty)
		for (slot in 47..51) assertTrue(actions(menu).containsKey(slot))
		assertFalse(menu.items.take(54).any { it.get(DataComponents.CUSTOM_NAME)?.string?.let { name -> name.contains("rules") || name.contains("Game setup") || name.contains("Task mix") } == true })
		val preview = SettingsMenu(3, inventory, system, true, "lockout")
		assertEquals(setOf(0, 1, 9, 10), actions(preview).keys)
		assertTrue(preview.getSlot(8).item.isEmpty)
		assertFalse(actions(preview).containsKey(5))
		assertSame(actions(preview)[9], actions(preview)[10])
		assertEquals(original, system.settings)
		assertFalse(TradeSettingsMenu(4, inventory, system).customBackground)
		assertFalse(TradeSettingsMenu(5, inventory, system).getSlot(1).item.isEmpty)
	}

	@Test
	fun playDisappearsDuringTeamSelectionAndReturnsAfterCancellation() {
		SharedConstants.tryDetectVersion()
		Bootstrap.bootStrap()
		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup()).forEach { it.apply() }
		val system = TaskTussleSystem()
		val inventory = Inventory::class.java.constructors.single().newInstance(null, EntityEquipment()) as Inventory
		val menu = SettingsMenu(1, inventory, system)
		assertTrue(actions(menu).containsKey(8))
		system.teams.selection = me.wanttobee.tasktussle.teams.TeamSelection(2, java.util.UUID.randomUUID(), startsGame = true)
		menu.broadcastChanges()
		assertTrue(menu.getSlot(8).item.isEmpty)
		assertFalse(actions(menu).containsKey(8))
		system.teams.selection = null
		menu.broadcastChanges()
		assertEquals("Play Bingo", menu.getSlot(8).item.get(DataComponents.CUSTOM_NAME)?.string)
		assertTrue(actions(menu).containsKey(8))
	}

	@Test
	fun sharedValuesStayLinkedWhenSwitchingGamesAndRegistrationsAreNotLimitedToSix() {
		SharedConstants.tryDetectVersion()
		Bootstrap.bootStrap()
		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup()).forEach { it.apply() }
		val extraGames = (1..7).map { number -> object : me.wanttobee.tasktussle.base.games.ITTGameManager by me.wanttobee.tasktussle.games.bingo.BingoManager {
			override val key = "game_$number"
		} }
		val system = TaskTussleSystem(gameManagers = extraGames)
		val inventory = Inventory::class.java.constructors.single().newInstance(null, EntityEquipment()) as Inventory
		val menu = SettingsMenu(1, inventory, system, true)
		system.settings.pvpMode = PvpMode.OFF
		system.settings.gameMinutes = 15
		system.settings.easyRatio = 5
		system.settings.teamCount = 4
		menu.selectGame("game_2")
		assertEquals("PvP: Off", menu.getSlot(3).item.get(DataComponents.CUSTOM_NAME)?.string)
		assertEquals("Game Time: 15 minutes", menu.getSlot(4).item.get(DataComponents.CUSTOM_NAME)?.string)
		assertEquals("Easy Ratio: 5", menu.getSlot(20).item.get(DataComponents.CUSTOM_NAME)?.string)
		assertEquals("Teams: 4", menu.getSlot(5).item.get(DataComponents.CUSTOM_NAME)?.string)
		assertEquals(7, system.gameManagers.size)
	}

	@Test
	fun teamCountUsesTheSelectedGamesLimitsWithoutOverflow() {
		val settings = TaskTussleSettings()
		assertEquals(2, settings.teamCount)
		settings.adjustTeamCount(1, 1..12)
		assertEquals(3, settings.teamCount)
		settings.adjustTeamCount(-1, 1..12)
		assertEquals(2, settings.teamCount)
		settings.teamCount = 1
		settings.adjustTeamCount(-1, 1..12)
		assertEquals(1, settings.teamCount)
		settings.teamCount = 12
		settings.adjustTeamCount(1, 1..12)
		assertEquals(12, settings.teamCount)
		settings.adjustTeamCount(-1, 2..4)
		assertEquals(4, settings.teamCount)
		settings.teamCount = Int.MAX_VALUE
		settings.adjustTeamCount(1, 1..12)
		assertEquals(12, settings.teamCount)
	}

	@Test
	fun openViewersFollowSharedSettings() {
		SharedConstants.tryDetectVersion()
		Bootstrap.bootStrap()
		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup()).forEach { it.apply() }
		val system = TaskTussleSystem()
		// Rendering and broadcasting these menus do not access the inventory's owner.
		val viewers = (1..2).map {
			val inventory = Inventory::class.java.constructors.single().newInstance(null, EntityEquipment()) as Inventory
			SettingsMenu(it, inventory, system, true)
		}
		system.settings.hardRatio = 9
		system.settings.blockWeight = 7
		system.settings.blocksFile = "blocks_test.yml"
		viewers.forEach { menu ->
			menu.broadcastChanges()
			assertEquals(Items.KNOWLEDGE_BOOK, menu.getSlot(48).item.item)
			assertEquals(Identifier.parse("tasktussle:advancement_settings"), menu.getSlot(48).item.get(DataComponents.ITEM_MODEL))
			assertEquals(Items.EMERALD, menu.getSlot(50).item.item)
			assertEquals(Identifier.parse("tasktussle:trade_task"), menu.getSlot(50).item.get(DataComponents.ITEM_MODEL))
			assertEquals("Hard Ratio: 9", menu.getSlot(22).item.get(DataComponents.CUSTOM_NAME)?.string)
			assertEquals(9, menu.getSlot(22).item.count)
			assertEquals("Obtain Task Ratio: 7", menu.getSlot(47).item.get(DataComponents.CUSTOM_NAME)?.string)
			assertEquals("File: blocks_test.yml", menu.getSlot(47).item.get(DataComponents.LORE)?.lines?.map { it.string }?.first { it.startsWith("File:") })
			val unchanged = menu.getSlot(22).item
			menu.broadcastChanges()
			assertSame(unchanged, menu.getSlot(22).item)
		}
		system.settings.hardRatio = 0
		viewers.forEach { menu ->
			menu.broadcastChanges()
			assertEquals("Hard Ratio: 0", menu.getSlot(22).item.get(DataComponents.CUSTOM_NAME)?.string)
			assertEquals(null, menu.getSlot(22).item.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
		}
		for ((mode, icon) in listOf(PvpMode.ON to Items.IRON_SWORD, PvpMode.OPT_IN_OUT to Items.WOODEN_SWORD, PvpMode.OFF to Items.PAPER)) {
			system.settings.pvpMode = mode
			viewers.forEach { menu ->
				menu.broadcastChanges()
				assertEquals("Choose teams: off", menu.getSlot(2).item.get(DataComponents.CUSTOM_NAME)?.string)
				assertEquals(icon, menu.getSlot(3).item.item)
				if (mode == PvpMode.OFF) assertEquals(Identifier.parse("tasktussle:peacefull"), menu.getSlot(3).item.get(DataComponents.ITEM_MODEL))
				assertEquals("PvP: ${mode.label}", menu.getSlot(3).item.get(DataComponents.CUSTOM_NAME)?.string)
				assertEquals(Items.CLOCK, menu.getSlot(4).item.item)
			}
		}
	}
}
