package me.wanttobee.tasktussle.games.bingo

import me.wanttobee.tasktussle.base.games.ITTCardLogic
import me.wanttobee.tasktussle.base.tasks.ITask

class BingoCardLogic : ITTCardLogic() {
	companion object {
		private fun lineCounts(tasks: List<ITask>, teamIndex: Int?): List<Int> {
			val complete = tasks.map { if (teamIndex == null) it.progress.completions.isNotEmpty() else it.progress.completedBy(teamIndex) }
			return listOf(
				(0..4).count { row -> (0..4).all { column -> complete[row * 5 + column] } },
				(0..4).count { column -> (0..4).all { row -> complete[row * 5 + column] } },
				listOf((0..4).all { complete[it * 6] }, (0..4).all { complete[4 + it * 4] }).count { it },
			)
		}

		fun completedLines(tasks: List<ITask>, teamIndex: Int): Int = if (tasks.size == 25) lineCounts(tasks, teamIndex).sum() else 0

		fun hasWon(tasks: List<ITask>, condition: String, teamIndex: Int? = null): Boolean {
			if (tasks.size != 25) return false
			val (horizontal, vertical, diagonal) = lineCounts(tasks, teamIndex)
			return when (condition) {
				"horizontal line" -> horizontal > 0
				"vertical line" -> vertical > 0
				"diagonal line" -> diagonal > 0
				"full card" -> horizontal == 5
				"2 lines" -> horizontal + vertical + diagonal >= 2
				"3 lines" -> horizontal + vertical + diagonal >= 3
				"4 lines" -> horizontal + vertical + diagonal >= 4
				else -> horizontal + vertical + diagonal >= 1
			}
		}
	}
}
