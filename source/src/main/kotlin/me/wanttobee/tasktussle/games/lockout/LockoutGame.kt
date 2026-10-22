package me.wanttobee.tasktussle.games.lockout

import me.wanttobee.tasktussle.base.games.ITTCardLogic
import me.wanttobee.tasktussle.base.games.ITTGame
import me.wanttobee.tasktussle.base.games.ITTGameTeam
import me.wanttobee.tasktussle.teams.Team

data class LockoutTeam(override val team: Team, override val card: ITTCardLogic) : ITTGameTeam

data class LockoutGame(
	override val teams: List<LockoutTeam>,
	override var remainingSeconds: Int,
	override val completionLimit: Int,
	override var paused: Boolean = false,
	override var finished: Boolean = false,
) : ITTGame {
	override val manager get() = LockoutManager
	override val sharedTasks = true
}
