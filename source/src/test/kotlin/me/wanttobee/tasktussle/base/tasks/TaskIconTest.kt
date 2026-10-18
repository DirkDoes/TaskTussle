package me.wanttobee.tasktussle.base.tasks

import me.wanttobee.tasktussle.TaskTussleGrouper
import me.wanttobee.tasktussle.TaskTussleSystem
import net.minecraft.SharedConstants
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.registries.VanillaRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskIconTest {
	@Test
	fun taskImplementationsKeepTheirIconsNamesAndLore() {
		SharedConstants.tryDetectVersion()
		Bootstrap.bootStrap()
		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup()).forEach { it.apply() }
		val system = TaskTussleSystem()
		val factory = TaskFactory(TaskTussleGrouper.taskManagers)
		val block = factory.create(TaskTemplate(TaskKind.BLOCK, Identifier.parse("minecraft:stone"), Difficulty.EASY))
		val blockIcon = taskStack(system, block)
		assertEquals(Identifier.parse("minecraft:stone"), blockIcon.get(DataComponents.ITEM_MODEL))
		assertEquals("Stone", blockIcon.get(DataComponents.CUSTOM_NAME)?.string)
		assertEquals(listOf("Task: Obtain Task", "Description: obtain this block"), blockIcon.get(DataComponents.LORE)?.lines?.map { it.string })

		val advancement = factory.create(TaskTemplate(TaskKind.ADVANCEMENT, Identifier.parse("minecraft:story/mine_diamond"), Difficulty.EASY))
		val advancementIcon = taskStack(system, advancement)
		assertEquals(Identifier.parse("tasktussle:advancement_icon"), advancementIcon.get(DataComponents.ITEM_MODEL))
		assertEquals(listOf("task", "minecraft:diamond"), advancementIcon.get(DataComponents.CUSTOM_MODEL_DATA)?.strings)
		assertEquals(listOf("Task: Advancements Task", "Description:", "complete this advancement"), advancementIcon.get(DataComponents.LORE)?.lines?.map { it.string })
		system.settings.customAdvancementIcons = false
		assertEquals(Items.KNOWLEDGE_BOOK, taskStack(system, advancement).item)

		val click = factory.create(TaskTemplate(TaskKind.CLICK, Identifier.parse("tasktussle:click"), Difficulty.EASY))
		assertEquals("CLICK ME", click.name(null))
		assertTrue(taskStack(system, click).get(DataComponents.ITEM_MODEL) in (1..3).map { Identifier.parse("tasktussle:click_me$it") })
		click.completed = true
		advancement.completed = true
		system.settings.customAdvancementIcons = true
		assertTrue(click.icon(null, system.settings).get(DataComponents.ITEM_MODEL) in (1..3).map { Identifier.parse("tasktussle:click_me$it") })
		val completionIcon = advancement.icon(null, system.settings)
		assertEquals(Identifier.parse("tasktussle:advancement_icon"), completionIcon.get(DataComponents.ITEM_MODEL))
		assertEquals(listOf("task", "minecraft:diamond"), completionIcon.get(DataComponents.CUSTOM_MODEL_DATA)?.strings)

		block.completed = true
		block.contributors += "Alice"
		val completeIcon = taskStack(system, block)
		assertEquals(Identifier.parse("tasktussle:task_complete"), completeIcon.get(DataComponents.ITEM_MODEL))
		assertEquals(listOf("Completed", "Alice"), completeIcon.get(DataComponents.LORE)?.lines?.map { it.string })
		click.completed = false
		click.failed = true
		assertEquals(listOf("Failed"), taskStack(system, click).get(DataComponents.LORE)?.lines?.map { it.string })
	}
}
