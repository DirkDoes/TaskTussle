package me.wanttobee.tasktussle.base.games

interface ITTGame {
	val manager: ITTGameManager
	val teams: List<ITTGameTeam>
	val sharedTasks: Boolean get() = false
	val completionLimit: Int get() = 1
	var remainingSeconds: Int
	var paused: Boolean
	var finished: Boolean
}
