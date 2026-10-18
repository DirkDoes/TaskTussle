package me.wanttobee.tasktussle.teams

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.games.ITTGameManager
import me.wanttobee.tasktussle.util.message
import me.wanttobee.tasktussle.util.tagged
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleMenuProvider

class TeamSystem(private val system: TaskTussleSystem) {
	var selection: TeamSelection? = null
		internal set
	var listedTeams: List<Team> = emptyList()
		internal set
	private val game get() = system.currentGame()

	fun beginTeamSelection(starter: ServerPlayer, teamCount: Int, startsGame: Boolean, manager: ITTGameManager? = null): Boolean {
		if (teamCount !in 2..54) return system.fail(starter, "team count has to be from 2 to 54")
		selection?.let { clearSelection(starter.level().server) }
		starter.level().server.playerList.players.forEach(system.items::removeSettingsTool)
		starter.level().server.playerList.players.forEach(system.items::removeTaskBrowser)
		selection = TeamSelection(teamCount, starter.uuid, startsGame = startsGame, manager = manager)
		starter.level().server.playerList.players.forEach(system.items::syncPvpToggle)
		starter.level().server.playerList.players.forEach(system.items::giveTeamNavigator)
		system.items.giveTeamFinisher(starter)
		starter.sendSystemMessage(tagged("use the Team Navigator Tool to choose teams, then Detonate Current Teams", ChatFormatting.GREEN))
		return true
	}

	fun openTeamMaker(player: ServerPlayer): Boolean {
		val current = selection ?: return false
		system.menus.openMenu(player, SimpleMenuProvider(
			{ id, inventory, _ -> TeamMakerMenu(id, inventory, system, current) }, Component.literal("Team Maker"),
		))
		return true
	}

	fun toggleTeam(player: ServerPlayer, team: Int): Boolean {
		val current = selection ?: return false
		if (team !in 1..current.teamCount) return false
		if (current.members[player.uuid] == team) current.members.remove(player.uuid) else current.members[player.uuid] = team
		requireNotNull(system.server).playerList.players.forEach { candidate ->
			(candidate.containerMenu as? TeamMakerMenu)?.refresh()
		}
		return true
	}

	fun finishTeamSelection(player: ServerPlayer): Boolean {
		val current = selection ?: return false
		if (player.uuid != current.starter) return false
		val server = player.level().server
		val teams = (1..current.teamCount).mapNotNull { index ->
			current.members.filterValues { it == index }.keys.takeIf { it.isNotEmpty() }?.let { Team(index, it.toMutableSet()) }
		}
		if (teams.isEmpty()) return system.fail(player, "no players selected a team")
		selection = null
		server.playerList.players.forEach(system.items::removeTeamItems)
		listedTeams = teams
		if (!current.startsGame) server.playerList.players.forEach(system.items::giveSettingsTool)
		if (!current.startsGame) server.playerList.players.forEach(system.items::giveTaskBrowser)
		return !current.startsGame || system.startWithTeams(server, player, teams, current.manager ?: system.gameManagers.first())
	}

	fun generateTeams(player: ServerPlayer, count: Int): Boolean {
		if (count < 1) return system.fail(player, "team amount must be at least 1")
		listedTeams = randomTeams(player.level().server.playerList.players, count)
		teamStatus(player)
		return true
	}

	fun clearTeams(server: MinecraftServer) {
		if (game != null) system.stop(server, null)
		clearSelection(server)
		listedTeams = emptyList()
	}

	fun teamStatus(player: ServerPlayer) {
		player.sendSystemMessage(tagged("active teamSets:", ChatFormatting.YELLOW))
		if (listedTeams.isEmpty()) player.sendSystemMessage(message("there are no teamSets active", ChatFormatting.GREEN))
		else listedTeams.forEach { team ->
			val names = team.onlinePlayers(player.level().server.playerList.players).joinToString { it.gameProfile.name }
			player.sendSystemMessage(message("- ${team.displayName()}: $names", team.color))
		}
	}

	internal fun randomTeams(players: List<ServerPlayer>, count: Int): List<Team> {
		val teams = (1..count).map { Team(it, mutableSetOf()) }
		players.shuffled().forEachIndexed { index, player -> teams[index % count].members += player.uuid }
		return teams
	}

	internal fun clearSelection(server: MinecraftServer) {
		selection = null
		server.playerList.players.forEach(system.items::removeTeamItems)
		if (game == null) server.playerList.players.forEach(system.items::giveSettingsTool)
		if (game == null) server.playerList.players.forEach(system.items::giveTaskBrowser)
	}
}

fun Team.onlinePlayers(players: Collection<ServerPlayer>): List<ServerPlayer> = players.filter { it.uuid in members }
