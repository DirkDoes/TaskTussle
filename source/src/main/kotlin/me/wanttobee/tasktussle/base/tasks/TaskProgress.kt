package me.wanttobee.tasktussle.base.tasks

import java.util.UUID

data class TaskCompletion(val teamIndex: Int, val playerId: UUID, val playerName: String, val tick: Int)

enum class TaskState { ACTIVE, COMPLETED, LOCKED_OUT, FAILED }

/** Completion order is insertion order. Each team can claim a task only once. */
class TaskProgress {
	private val claims = mutableListOf<TaskCompletion>()
	val completions: List<TaskCompletion> get() = claims.toList()
	var failed = false
		private set

	fun completedBy(teamIndex: Int) = claims.any { it.teamIndex == teamIndex }
	fun closed(limit: Int) = failed || claims.size >= limit
	fun state(teamIndex: Int?, limit: Int): TaskState = when {
		teamIndex != null && completedBy(teamIndex) -> TaskState.COMPLETED
		failed -> TaskState.FAILED
		closed(limit) -> TaskState.LOCKED_OUT
		else -> TaskState.ACTIVE
	}

	fun complete(completion: TaskCompletion, limit: Int): Boolean {
		require(limit > 0 && completion.teamIndex > 0)
		if (state(completion.teamIndex, limit) != TaskState.ACTIVE) return false
		claims += completion
		return true
	}

	fun fail() { failed = true }
}
