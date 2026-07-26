package me.wanttobee.tasktussle

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForcedItemTest {
	@Test
	fun onlyTaskTussleToolMarkersAreForced() {
		assertTrue(TaskTussleService.isForcedMarker("card"))
		assertTrue(TaskTussleService.isForcedMarker("team_navigator"))
		assertTrue(TaskTussleService.isForcedMarker("team_finisher"))
		assertFalse(TaskTussleService.isForcedMarker("anything_else"))
		assertFalse(TaskTussleService.isForcedMarker(null))
	}
}