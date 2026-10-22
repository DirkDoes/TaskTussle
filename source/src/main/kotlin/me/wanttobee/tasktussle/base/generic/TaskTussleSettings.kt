package me.wanttobee.tasktussle.base.generic

import me.wanttobee.tasktussle.pvp.PvpMode
import me.wanttobee.tasktussle.base.games.TimeoutRule

data class TaskTussleSettings(
	var cardVisibility: String = "progress only",
	var chooseTeams: Boolean = false,
	var pvpMode: PvpMode = PvpMode.ON,
	var easyRatio: Int = 13,
	var mediumRatio: Int = 8,
	var hardRatio: Int = 4,
	var blockWeight: Int = 10,
	var advancementWeight: Int = 2,
	var clickWeight: Int = 0,
	var tradeWeight: Int = 2,
	var statusEffectWeight: Int = 2,
	var handInItems: Boolean = false,
	var mutualTasks: Int = 15,
	var winningCondition: String = "1 line",
	var gameMinutes: Int = 0,
	var bingoTimeoutRule: TimeoutRule = TimeoutRule.LINES_THEN_TASKS,
	var lockoutTimeoutRule: TimeoutRule = TimeoutRule.MOST_TASKS,
	var sharedTaskCompletionLimit: Int = 1,
	var blocksFile: String = "blocks_default.yml",
	var advancementsFile: String = "advancements_default.yml",
	var tradesFile: String = "trades_default.yml",
	var statusEffectsFile: String = "status_effects_default.yml",
) {
	val gameSeconds get() = gameMinutes.coerceIn(0, Int.MAX_VALUE / 60) * 60
	fun adjustGameTime(delta: Int) {
		gameMinutes = (gameMinutes.toLong() + delta).coerceIn(0L, (Int.MAX_VALUE / 60).toLong()).toInt()
	}
}
