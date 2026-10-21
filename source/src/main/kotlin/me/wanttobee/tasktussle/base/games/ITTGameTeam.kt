package me.wanttobee.tasktussle.base.games

import me.wanttobee.tasktussle.teams.Team
import net.minecraft.server.level.ServerPlayer

interface ITTGameTeam {
	val team: Team
	val card: ITTCardLogic
	val index get() = team.index
	val members get() = team.members
	val color get() = team.color
	val tasks get() = card.tasks
	val completed get() = card.completed
	fun displayName() = team.displayName()
}

fun ITTGameTeam.onlinePlayers(players: Collection<ServerPlayer>): List<ServerPlayer> = players.filter { it.uuid in members }
