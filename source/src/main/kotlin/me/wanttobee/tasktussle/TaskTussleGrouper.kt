package me.wanttobee.tasktussle

import me.wanttobee.tasktussle.base.games.ITTGameManager
import me.wanttobee.tasktussle.base.tasks.ITaskManager
import me.wanttobee.tasktussle.games.bingo.BingoManager
import me.wanttobee.tasktussle.games.lockout.LockoutManager
import me.wanttobee.tasktussle.tasks.achievementTask.AdvancementsTaskManager
import me.wanttobee.tasktussle.tasks.clickTask.ClickTaskManager
import me.wanttobee.tasktussle.tasks.obtainTask.ObtainTaskManager
import me.wanttobee.tasktussle.tasks.tradeTask.TradeTaskManager
import me.wanttobee.tasktussle.tasks.statusEffectTask.StatusEffectTaskManager

/** Add game and task implementations here; the system dispatches through their contracts. */
object TaskTussleGrouper {
	val taskManagers: List<ITaskManager> = listOf(ObtainTaskManager, AdvancementsTaskManager, ClickTaskManager, TradeTaskManager, StatusEffectTaskManager)
	val gameManagers: List<ITTGameManager> = listOf(BingoManager, LockoutManager)
}
