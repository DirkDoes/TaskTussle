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
	val completed get() = tasks.count { it.progress.completedBy(index) }
	fun contributions(players: Collection<ServerPlayer> = emptyList()): List<String> {
		val claims = tasks.flatMap { it.progress.completions }.filter { it.teamIndex == index }
		val names = claims.associate { it.playerId to it.playerName } + onlinePlayers(players).associate { it.uuid to it.gameProfile.name }
		return names.map { (id, name) ->
			val count = claims.count { it.playerId == id }
			"$name = ${if (claims.isEmpty()) 0.0 else count * 10000 / claims.size / 100.0}%"
		}
	}
	fun displayName() = team.displayName()
}

fun ITTGameTeam.onlinePlayers(players: Collection<ServerPlayer>): List<ServerPlayer> = players.filter { it.uuid in members }
