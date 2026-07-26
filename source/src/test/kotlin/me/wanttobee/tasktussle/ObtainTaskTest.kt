package me.wanttobee.tasktussle

import kotlin.test.Test
import kotlin.test.assertEquals

class ObtainTaskTest {
	@Test
	fun splitsRequiredAmountAcrossInventoryStacks() {
		assertEquals(listOf(2, 3, 0), takeAmounts(listOf(2, 3, 4), 5))
	}
}
