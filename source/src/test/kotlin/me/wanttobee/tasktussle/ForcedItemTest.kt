package me.wanttobee.tasktussle

import net.minecraft.SharedConstants
import net.minecraft.resources.Identifier
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.Items
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForcedItemTest {
	@Test
	fun onlyTaskTussleToolMarkersAreForced() {
		assertTrue(TaskTussleService.isForcedMarker("card"))
		assertTrue(TaskTussleService.isForcedMarker("team_navigator"))
		assertTrue(TaskTussleService.isForcedMarker("team_finisher"))
		assertTrue(TaskTussleService.isForcedMarker("delivery_spot"))
		assertTrue(TaskTussleService.isForcedMarker("settings"))
		assertTrue(TaskTussleService.isForcedMarker("task_browser"))
		assertFalse(TaskTussleService.isForcedMarker("anything_else"))
		assertFalse(TaskTussleService.isForcedMarker(null))
	}

	@Test
	fun deliveryMatchesOnlyAnUnfinishedBlockTask() {
		SharedConstants.tryDetectVersion()
		Bootstrap.bootStrap()
		val task = BingoTask(TaskTemplate(TaskKind.BLOCK, Identifier.fromNamespaceAndPath("minecraft", "iron_block"), Difficulty.EASY))
		assertSame(task, deliveryTask(listOf(task), Items.IRON_BLOCK))
		task.completed = true
		assertNull(deliveryTask(listOf(task), Items.IRON_BLOCK))
	}
}
