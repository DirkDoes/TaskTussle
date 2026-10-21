package me.wanttobee.tasktussle.base.games

import me.wanttobee.tasktussle.base.tasks.ITask

abstract class ITTCardLogic {
	val tasks = mutableListOf<ITask>()
	val completed: Int get() = tasks.count { it.completed }
	abstract fun hasWon(condition: String): Boolean
}
