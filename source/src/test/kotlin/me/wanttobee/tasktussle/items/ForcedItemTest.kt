package me.wanttobee.tasktussle.items

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ForcedItemTest {
	@Test
	fun onlyTaskTussleToolMarkersAreForced() {
		assertTrue(ForcedItems.isForcedMarker("card"))
		assertTrue(ForcedItems.isForcedMarker("team_navigator"))
		assertTrue(ForcedItems.isForcedMarker("team_finisher"))
		assertTrue(ForcedItems.isForcedMarker("settings"))
		assertTrue(ForcedItems.isForcedMarker("pvp_toggle"))
		assertTrue(ForcedItems.isForcedMarker("task_browser"))
		assertFalse(ForcedItems.isForcedMarker("anything_else"))
		assertFalse(ForcedItems.isForcedMarker(null))
	}
}
