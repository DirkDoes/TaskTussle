package me.wanttobee.tasktussle.tasks.statusEffectTask

import java.nio.file.Files
import java.nio.file.Path
import me.wanttobee.tasktussle.TaskTussleGrouper
import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.SettingsMenu
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.CatalogTypeMenu
import me.wanttobee.tasktussle.base.tasks.Difficulty
import me.wanttobee.tasktussle.base.tasks.TaskCatalog
import me.wanttobee.tasktussle.base.tasks.TaskFactory
import me.wanttobee.tasktussle.base.tasks.TaskTemplate
import me.wanttobee.tasktussle.base.tasks.taskStack
import me.wanttobee.tasktussle.tasks.tradeTask.TradeEvent
import net.minecraft.SharedConstants
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.registries.VanillaRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.Bootstrap
import net.minecraft.world.entity.EntityEquipment
import net.minecraft.world.entity.player.Inventory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StatusEffectTaskTest {
	private fun bootstrap() {
		SharedConstants.tryDetectVersion()
		Bootstrap.bootStrap()
		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup()).forEach { it.apply() }
	}

	@Test
	fun everyEffectHasOneTierAndAnExtractedIcon() {
		bootstrap()
		val pools = StatusEffectTaskFiles.pools()
		val all = Difficulty.entries.flatMap(pools::get)
		assertEquals(BuiltInRegistries.MOB_EFFECT.keySet(), all.toSet())
		assertEquals(all.size, all.toSet().size)
		assertEquals(40, all.size)
		val bundled = checkNotNull(javaClass.getResourceAsStream("/status_effects_default.yml")).bufferedReader().use { it.readLines() }
		assertEquals(pools, TaskCatalog.parseYaml(bundled))
		assertEquals(Difficulty.EASY, StatusEffectTaskFiles.difficulty("poison"))
		assertEquals(Difficulty.MEDIUM, StatusEffectTaskFiles.difficulty("fire_resistance"))
		assertEquals(Difficulty.HARD, StatusEffectTaskFiles.difficulty("hero_of_the_village"))
		val pack = Path.of("..", "resourcepack", "assets", "tasktussle")
		for (effect in all) {
			assertTrue(Files.size(pack.resolve("textures/item/status_effects/${effect.path}.png")) > 0)
			assertTrue(Files.readString(pack.resolve("models/item/status_effect/${effect.path}.json")).contains("tasktussle:item/status_effects/${effect.path}"))
			assertTrue(Files.readString(pack.resolve("items/status_effect/${effect.path}.json")).contains("tasktussle:item/status_effect/${effect.path}"))
		}
	}

	@Test
	fun onlyMatchingEffectsCompleteTasksAndTheirIconsAlsoWorkAfterCompletion() {
		bootstrap()
		val system = TaskTussleSystem()
		val factory = TaskFactory(TaskTussleGrouper.taskManagers)
		val settings = TaskTussleSettings()
		for (effect in StatusEffectTaskFiles.effectIds()) {
			val task = assertIs<StatusEffectTask>(factory.create(TaskTemplate(StatusEffectTaskManager.kind, effect, Difficulty.EASY)))
			assertTrue(task.onEvent(StatusEffectEvent(effect), settings))
			assertFalse(task.onEvent(StatusEffectEvent(Identifier.parse("test:other_effect")), settings))
			assertFalse(task.onEvent(TradeEvent(setOf(effect)), settings))
			val model = Identifier.fromNamespaceAndPath("tasktussle", "status_effect/${effect.path}")
			assertEquals(model, taskStack(system, task).get(DataComponents.ITEM_MODEL))
			assertEquals(task.name(null), taskStack(system, task).get(DataComponents.CUSTOM_NAME)?.string)
			task.completed = true
			// The completion display uses task.icon, independently of the completed card marker.
			assertEquals(model, task.icon(null, settings).get(DataComponents.ITEM_MODEL))
			assertEquals(Identifier.parse("tasktussle:task_complete"), taskStack(system, task).get(DataComponents.ITEM_MODEL))
		}
	}

	@Test
	fun settingsAndCatalogUseSpeedIconAndTheNewFileSetting() {
		bootstrap()
		val system = TaskTussleSystem()
		val inventory = Inventory::class.java.constructors.single().newInstance(null, EntityEquipment()) as Inventory
		val model = Identifier.parse("tasktussle:status_effect/speed")
		val index = system.taskFactory.managers.indexOf(StatusEffectTaskManager)
		assertEquals(model, SettingsMenu(1, inventory, system).getSlot(18 + index).item.get(DataComponents.ITEM_MODEL))
		assertEquals(model, StatusEffectSettingsMenu(2, inventory, system).getSlot(4).item.get(DataComponents.ITEM_MODEL))
		assertEquals(model, CatalogTypeMenu(3, inventory, system).getSlot(3).item.get(DataComponents.ITEM_MODEL))
		StatusEffectTaskManager.setWeight(system.settings, 7)
		assertEquals(7, StatusEffectTaskManager.weight(system.settings))
		StatusEffectTaskFiles.selectFile(system.settings, "status_effects_player_1.yml")
		assertEquals("status_effects_player_1.yml", StatusEffectTaskFiles.selectedFile(system.settings))
	}
}
