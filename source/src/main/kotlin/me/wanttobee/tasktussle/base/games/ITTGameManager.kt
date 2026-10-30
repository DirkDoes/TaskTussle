package me.wanttobee.tasktussle.base.games

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.IManager
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.TaskFactory
import me.wanttobee.tasktussle.teams.Team
import net.minecraft.world.MenuProvider
import net.minecraft.resources.Identifier
import me.wanttobee.tasktussle.base.generic.SettingControl

interface ITTGameManager : IManager {
	val teamRange: IntRange
	val cardItemModel: Identifier? get() = null
	fun settingsControls(system: TaskTussleSystem): List<SettingControl> = emptyList()
	fun create(teams: List<Team>, settings: TaskTussleSettings, factory: TaskFactory): ITTGame?
	fun cardMenu(system: TaskTussleSystem, game: ITTGame, team: ITTGameTeam, viewer: ITTGameTeam?): MenuProvider
	fun isFinished(game: ITTGame, team: ITTGameTeam, settings: TaskTussleSettings): Boolean
	fun completionWinner(game: ITTGame, team: ITTGameTeam, settings: TaskTussleSettings): ITTGameTeam? = team
	fun timeUpWinner(game: ITTGame, settings: TaskTussleSettings): ITTGameTeam?
	fun goal(settings: TaskTussleSettings): String
}
