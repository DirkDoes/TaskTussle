package me.wanttobee.tasktussle.base.generic

import me.wanttobee.tasktussle.pvp.PvpMode

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
	var handInItems: Boolean = false,
	var mutualTasks: Int = 15,
	var winningCondition: String = "1 line",
	var succeedTokens: Int = 0,
	var gameMinutes: Int = 0,
	var overtime: String = "draw",
	var blocksFile: String = "blocks_default.yml",
	var advancementsFile: String = "advancements_default.yml",
	var customAdvancementIcons: Boolean = true,
)
