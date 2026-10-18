package me.wanttobee.tasktussle.base.games

interface ITTGame {
	val manager: ITTGameManager
	val teams: List<ITTGameTeam>
	var remainingSeconds: Int
	var paused: Boolean
	var finished: Boolean
}
