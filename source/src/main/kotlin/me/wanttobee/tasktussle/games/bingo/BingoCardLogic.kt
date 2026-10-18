package me.wanttobee.tasktussle.games.bingo

import me.wanttobee.tasktussle.base.games.ITTCardLogic
import me.wanttobee.tasktussle.base.tasks.ITask

class BingoCardLogic : ITTCardLogic() {
	override fun hasWon(condition: String) = hasWon(tasks, condition)

	companion object {
		fun hasWon(tasks: List<ITask>, condition: String): Boolean {
			if (tasks.size != 25) return false
			val complete = tasks.map(ITask::completed)
			val horizontal = (0..4).count { row -> (0..4).all { column -> complete[row * 5 + column] } }
			val vertical = (0..4).count { column -> (0..4).all { row -> complete[row * 5 + column] } }
			val diagonal = listOf((0..4).all { complete[it * 6] }, (0..4).all { complete[4 + it * 4] }).count { it }
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
