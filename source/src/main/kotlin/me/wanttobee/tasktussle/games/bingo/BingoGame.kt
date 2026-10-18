package me.wanttobee.tasktussle.games.bingo

import me.wanttobee.tasktussle.base.games.ITTGame
import me.wanttobee.tasktussle.base.games.ITTGameManager

data class BingoGame(
	override val teams: List<BingoTeam>,
	override var remainingSeconds: Int,
	override var paused: Boolean = false,
	override var finished: Boolean = false,
) : ITTGame {
	override val manager: ITTGameManager get() = BingoManager
}
