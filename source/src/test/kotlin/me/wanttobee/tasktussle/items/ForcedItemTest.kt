package me.wanttobee.tasktussle.items

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import me.wanttobee.tasktussle.base.tasks.Difficulty
import me.wanttobee.tasktussle.base.tasks.TaskKind
import me.wanttobee.tasktussle.base.tasks.TaskTemplate
import me.wanttobee.tasktussle.base.tasks.deliveryTask
import me.wanttobee.tasktussle.tasks.obtainTask.ObtainTask
import net.minecraft.SharedConstants
import net.minecraft.resources.Identifier
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.Items

class ForcedItemTest {
	@Test
	fun onlyTaskTussleToolMarkersAreForced() {
		assertTrue(ForcedItems.isForcedMarker("card"))
		assertTrue(ForcedItems.isForcedMarker("team_navigator"))
		assertTrue(ForcedItems.isForcedMarker("team_finisher"))
		assertTrue(ForcedItems.isForcedMarker("delivery_spot"))
		assertTrue(ForcedItems.isForcedMarker("settings"))
		assertTrue(ForcedItems.isForcedMarker("pvp_toggle"))
		assertTrue(ForcedItems.isForcedMarker("task_browser"))
		assertFalse(ForcedItems.isForcedMarker("anything_else"))
		assertFalse(ForcedItems.isForcedMarker(null))
	}

	@Test
	fun deliveryMatchesOnlyAnUnfinishedBlockTask() {
		SharedConstants.tryDetectVersion()
		Bootstrap.bootStrap()
		val task = ObtainTask(TaskTemplate(TaskKind.BLOCK, Identifier.fromNamespaceAndPath("minecraft", "iron_block"), Difficulty.EASY))
		assertSame(task, deliveryTask(listOf(task), Items.IRON_BLOCK))
		assertNull(deliveryTask(listOf(task), Items.DIRT))
		task.completed = true
		assertNull(deliveryTask(listOf(task), Items.IRON_BLOCK))
		task.completed = false
		task.failed = true
		assertNull(deliveryTask(listOf(task), Items.IRON_BLOCK))
	}
}
