package me.wanttobee.tasktussle.tasks.tradeTask

import me.wanttobee.tasktussle.base.tasks.claim

import com.mojang.serialization.JsonOps
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import me.wanttobee.tasktussle.TaskTussleGrouper
import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.SettingsMenu
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.CatalogTypeMenu
import me.wanttobee.tasktussle.base.tasks.Difficulty
import me.wanttobee.tasktussle.base.tasks.TaskCatalog
import me.wanttobee.tasktussle.base.tasks.TaskEvent
import me.wanttobee.tasktussle.base.tasks.TaskFactory
import me.wanttobee.tasktussle.base.tasks.TaskGenerator
import me.wanttobee.tasktussle.base.tasks.TaskSource
import me.wanttobee.tasktussle.base.tasks.TaskTemplate
import me.wanttobee.tasktussle.base.tasks.taskStack
import me.wanttobee.tasktussle.tasks.clickTask.ClickTaskManager
import net.minecraft.SharedConstants
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.data.registries.VanillaRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.Bootstrap
import net.minecraft.world.entity.EntityEquipment
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.trading.ItemCost
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.VillagerTrade

class TradeTaskTest {
	private fun bootstrap() {
		SharedConstants.tryDetectVersion()
		Bootstrap.bootStrap()
		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup()).forEach { it.apply() }
	}

	@Test
	fun completedOfferMatchesBothInputsAndOutputByItemOnly() {
		bootstrap()
		val settings = TaskTussleSettings()
		val result = ItemStack(Items.ENCHANTED_BOOK).apply { set(DataComponents.CUSTOM_NAME, Component.literal("Any enchantment")) }
		val offer = MerchantOffer(ItemCost(Items.EMERALD, 5), Optional.of(ItemCost(Items.BOOK)), result, 12, 1, 0.2f)
		val event = TradeEvent(offer)
		assertEquals(setOf("minecraft:emerald", "minecraft:book", "minecraft:enchanted_book"), event.items.map { it.toString() }.toSet())
		for (id in event.items) {
			val task = TradeTask(TaskTemplate(TradeTaskManager.kind, id, Difficulty.EASY))
			assertTrue(task.onEvent(event, settings))
			assertFalse(task.onEvent(object : TaskEvent {}, settings))
			assertFalse(task.onEvent(TradeEvent(setOf(Identifier.parse("minecraft:diamond"))), settings))
		}
		// A different merchant or reversed exchange has exactly the same item matching.
		val sellingBook = TradeEvent(MerchantOffer(ItemCost(Items.BOOK, 4), ItemStack(Items.EMERALD), 12, 1, 0.05f))
		assertEquals(setOf(Identifier.parse("minecraft:book"), Identifier.parse("minecraft:emerald")), sellingBook.items)
		val wanderingOffer = TradeEvent(MerchantOffer(ItemCost(Items.EMERALD), ItemStack(Items.SAND), 12, 1, 0.05f))
		assertTrue(TradeTask(TaskTemplate(TradeTaskManager.kind, Identifier.parse("minecraft:emerald"), Difficulty.EASY)).onEvent(wanderingOffer, settings))
		result.shrink(1)
		assertTrue(Identifier.parse("minecraft:enchanted_book") in event.items)
	}

	@Test
	fun bundledCatalogUsesTheLowestVillagerLevelAndRemovesExpertAndMasterItems() {
		bootstrap()
		val registries = VanillaRegistries.createLookup()
		val ops = registries.createSerializationContext(JsonOps.INSTANCE)
		val allTrades = registries.lookupOrThrow(Registries.VILLAGER_TRADE).listElements().toList()
		val regular = allTrades.filterNot { it.key().identifier().path.startsWith("wandering_trader/") }
		val levels = mutableMapOf<Identifier, Int>()
		for (trade in regular) {
			val level = trade.key().identifier().path.split('/')[1].toInt()
			for (item in TradeTaskFiles.tradeItems(VillagerTrade.CODEC.encodeStart(ops, trade.value()).orThrow.asJsonObject)) {
				levels.merge(item, level, ::minOf)
			}
		}
		val pools = defaultPools()
		val actual = Difficulty.entries.flatMap(pools::get)
		assertEquals(levels.filterValues { it == 1 }.keys, pools.easy.toSet())
		assertEquals(levels.filterValues { it == 2 }.keys, pools.medium.toSet())
		assertEquals(levels.filterValues { it == 3 }.keys, pools.hard.toSet())
		assertEquals(pools, TradeTaskFiles.poolsForLevels(levels))
		assertEquals(actual.size, actual.toSet().size)
		assertEquals(244, levels.size)
		assertTrue(Identifier.parse("minecraft:emerald") in pools.easy)
		assertTrue(Identifier.parse("minecraft:book") in pools.easy)
		assertTrue(Identifier.parse("minecraft:filled_map") in pools.medium)
		assertTrue(Identifier.parse("minecraft:glass") in pools.hard)
		val removed = levels.keys - actual.toSet()
		assertEquals(levels.filterValues { it > 3 }.keys, removed)
		assertTrue(Identifier.parse("minecraft:tipped_arrow") in removed)
		val exportedRemoved = checkNotNull(javaClass.getResourceAsStream("/trades_default.yml")).bufferedReader().use { it.readLines() }
			.dropWhile { it != "removed:" }.drop(1).filter { it.startsWith("  - ") }.map { Identifier.parse(it.removePrefix("  - ")) }.toSet()
		assertEquals(removed, exportedRemoved)
		assertFalse(Identifier.parse("minecraft:sand") in actual)
		assertFalse(Identifier.parse("minecraft:name_tag") in actual)
	}

	@Test
	fun tradeTasksUseTheSharedGeneratorFactoryAndSettingsMenu() {
		bootstrap()
		val templates = assertNotNull(TaskGenerator.generate(25, listOf(TaskSource(TradeTaskManager.kind, 1, defaultPools())), Triple(13, 8, 4)))
		val factory = TaskFactory(TaskTussleGrouper.taskManagers)
		val settings = TaskTussleSettings()
		assertEquals(25, templates.map { it.id }.toSet().size)
		val system = TaskTussleSystem()
		val tradeModel = Identifier.parse("tasktussle:trade_task")
		val overlayModel = Identifier.parse("tasktussle:item_overlay")
		for (template in templates) {
			val task = assertIs<TradeTask>(factory.create(template))
			assertEquals(overlayModel, task.icon(null, settings).get(DataComponents.ITEM_MODEL))
			val cardIcon = taskStack(system, task)
			assertEquals(overlayModel, cardIcon.get(DataComponents.ITEM_MODEL))
			assertEquals(BuiltInRegistries.ITEM.getValue(template.id), cardIcon.item)
			assertEquals(listOf(template.id.toString(), "trade_task_overlay"), cardIcon.get(DataComponents.CUSTOM_MODEL_DATA)?.strings)
			val editorIcon = TradeTaskFiles.icon(null, settings, template.id)
			assertEquals(cardIcon.get(DataComponents.ITEM_MODEL), editorIcon.get(DataComponents.ITEM_MODEL))
			assertEquals(cardIcon.get(DataComponents.CUSTOM_MODEL_DATA), editorIcon.get(DataComponents.CUSTOM_MODEL_DATA))
			assertEquals(ItemStack(BuiltInRegistries.ITEM.getValue(template.id)).hoverName.string, cardIcon.get(DataComponents.CUSTOM_NAME)?.string)
			task.claim()
			assertEquals(Identifier.parse("tasktussle:task_complete"), taskStack(system, task).get(DataComponents.ITEM_MODEL))
		}
		val inventory = Inventory::class.java.constructors.single().newInstance(null, EntityEquipment()) as Inventory
		val menu = SettingsMenu(1, inventory, system, true)
		val slot = 47 + system.taskFactory.managers.indexOf(TradeTaskManager)
		assertEquals(tradeModel, menu.getSlot(slot).item.get(DataComponents.ITEM_MODEL))
		assertEquals(tradeModel, SettingsMenu(2, inventory, system).getSlot(slot).item.get(DataComponents.ITEM_MODEL))
		assertEquals(tradeModel, TradeSettingsMenu(3, inventory, system).getSlot(4).item.get(DataComponents.ITEM_MODEL))
		assertEquals(Items.BELL, ClickTaskManager.settingsMenu(4, inventory, system).getSlot(4).item.item)
		assertEquals(Identifier.parse("minecraft:bell"), ClickTaskManager.settingsMenu(4, inventory, system).getSlot(4).item.get(DataComponents.ITEM_MODEL))
		val catalogSlot = system.taskFactory.managers.filter { it.files != null }.indexOf(TradeTaskManager)
		assertEquals(tradeModel, CatalogTypeMenu(5, inventory, system).getSlot(catalogSlot).item.get(DataComponents.ITEM_MODEL))
		system.settings.tradeWeight = 7
		menu.broadcastChanges()
		assertEquals("Trade Task Ratio: 7", menu.getSlot(slot).item.get(DataComponents.CUSTOM_NAME)?.string)
		assertEquals(tradeModel, menu.getSlot(slot).item.get(DataComponents.ITEM_MODEL))
	}

	private fun defaultPools() = TaskCatalog.parseYaml(checkNotNull(javaClass.getResourceAsStream("/trades_default.yml")).bufferedReader().use { it.readLines() })
}
