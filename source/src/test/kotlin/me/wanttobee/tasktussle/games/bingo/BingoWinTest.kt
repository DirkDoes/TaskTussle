package me.wanttobee.tasktussle.games.bingo

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import me.wanttobee.tasktussle.base.tasks.Difficulty
import me.wanttobee.tasktussle.base.tasks.ITask
import me.wanttobee.tasktussle.base.tasks.TaskKind
import me.wanttobee.tasktussle.base.tasks.TaskTemplate
import me.wanttobee.tasktussle.tasks.obtainTask.ObtainTask
import net.minecraft.resources.Identifier

class BingoWinTest {
	private fun card(completed: Set<Int>): List<ITask> = (0 until 25).map { index ->
		ObtainTask(TaskTemplate(TaskKind.BLOCK, Identifier.fromNamespaceAndPath("test", "block_$index"), Difficulty.EASY)).apply {
			this.completed = index in completed
		}
	}

	@Test
	fun detectsLinesAndFullCard() {
		assertTrue(BingoCardLogic.hasWon(card((0..4).toSet()), "horizontal line"))
		assertTrue(BingoCardLogic.hasWon(card(setOf(0, 6, 12, 18, 24)), "diagonal line"))
		assertFalse(BingoCardLogic.hasWon(card((0..23).toSet()), "full card"))
		assertTrue(BingoCardLogic.hasWon(card((0..24).toSet()), "full card"))
	}

	@Test
	fun countsEachDirectionAndMultipleLines() {
		assertTrue(BingoCardLogic.hasWon(card(setOf(0, 5, 10, 15, 20)), "vertical line"))
		assertFalse(BingoCardLogic.hasWon(card((0..4).toSet()), "vertical line"))
		assertTrue(BingoCardLogic.hasWon(card(setOf(4, 8, 12, 16, 20)), "diagonal line"))
		for (lines in 2..4) {
			assertTrue(BingoCardLogic.hasWon(card((0 until lines * 5).toSet()), "$lines lines"))
			assertFalse(BingoCardLogic.hasWon(card((0 until (lines - 1) * 5).toSet()), "$lines lines"))
		}
		assertFalse(BingoCardLogic.hasWon(card(emptySet()), "1 line"))
		assertFalse(BingoCardLogic.hasWon(card((0..24).toSet()).dropLast(1), "full card"))
	}
}
