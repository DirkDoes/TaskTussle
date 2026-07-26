package me.wanttobee.tasktussle

import kotlin.test.Test
import kotlin.test.assertEquals

class DelayedActionTest {
	@Test
	fun actionsOnlyBecomeDueAtTheirTick() {
		val scheduled = mutableListOf(5 to "item", 120 to "menu")
		assertEquals(emptyList(), takeDue(4, scheduled))
		assertEquals(listOf("item"), takeDue(5, scheduled))
		assertEquals(listOf("menu"), takeDue(120, scheduled))
	}

	@Test
	fun revealsTitleAcrossConfiguredTicks() {
		assertEquals(listOf(0 to "D", 13 to "DR", 26 to "DRA", 40 to "DRAW"), titleFrames("DRAW", 40))
	}
}
