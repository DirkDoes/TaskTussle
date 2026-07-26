package me.wanttobee.tasktussle

import net.minecraft.resources.Identifier
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BingoWinTest {
	private fun card(completed: Set<Int>): List<BingoTask> = (0 until 25).map { index ->
		BingoTask(TaskTemplate(TaskKind.BLOCK, Identifier.fromNamespaceAndPath("test", "block_$index"), Difficulty.EASY)).apply {
			this.completed = index in completed
		}
	}

	@Test
	fun detectsLinesAndFullCard() {
		assertTrue(TaskTussleService.hasWon(card((0..4).toSet()), "horizontal line"))
		assertTrue(TaskTussleService.hasWon(card(setOf(0, 6, 12, 18, 24)), "diagonal line"))
		assertFalse(TaskTussleService.hasWon(card((0..23).toSet()), "full card"))
		assertTrue(TaskTussleService.hasWon(card((0..24).toSet()), "full card"))
	}
}
