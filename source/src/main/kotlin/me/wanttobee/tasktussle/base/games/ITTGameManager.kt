package me.wanttobee.tasktussle.base.games

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.IManager
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.TaskFactory
import me.wanttobee.tasktussle.teams.Team
import net.minecraft.world.MenuProvider

interface ITTGameManager : IManager {
	val teamRange: IntRange
	fun create(teams: List<Team>, settings: TaskTussleSettings, factory: TaskFactory): ITTGame?
	fun cardMenu(system: TaskTussleSystem, game: ITTGame, team: ITTGameTeam, viewer: ITTGameTeam?): MenuProvider
	fun hasWon(team: ITTGameTeam, settings: TaskTussleSettings): Boolean
	fun timeUpWinner(game: ITTGame, settings: TaskTussleSettings): ITTGameTeam?
	fun goal(settings: TaskTussleSettings): String
}
