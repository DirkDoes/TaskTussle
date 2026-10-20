package me.wanttobee.tasktussle.games.bingo

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.games.ITTGame
import me.wanttobee.tasktussle.base.games.ITTGameManager
import me.wanttobee.tasktussle.base.games.ITTGameTeam
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.TaskFactory
import me.wanttobee.tasktussle.resourcepack.TaskTussleResourcePackServer
import me.wanttobee.tasktussle.teams.Team
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items

object BingoManager : ITTGameManager {
	override val key = "bingo"
	override val name = "Bingo"
	override val icon get() = Items.FILLED_MAP
	override val teamRange = 1..12

	override fun create(teams: List<Team>, settings: TaskTussleSettings, factory: TaskFactory): BingoGame? {
		val mutual = factory.generate(settings.mutualTasks, settings) ?: return null
		val gameTeams = teams.map(::BingoTeam)
		for (team in gameTeams) {
			team.succeedTokens = settings.succeedTokens
			team.succeedTokensMax = settings.succeedTokens
			val templates = when {
				settings.mutualTasks == 25 -> mutual
				settings.mutualTasks < 25 -> {
					val unique = factory.generate(25 - settings.mutualTasks, settings, mutual.map { it.kind to it.id }.toSet()) ?: return null
					mutual + unique
				}
				else -> mutual.shuffled().take(25)
			}
			team.tasks += templates.shuffled().map(factory::create)
		}
		return BingoGame(gameTeams, settings.gameMinutes * 60)
	}

	override fun cardMenu(system: TaskTussleSystem, game: ITTGame, team: ITTGameTeam, viewer: ITTGameTeam?): SimpleMenuProvider {
		val layout = BingoCardLayout()
		return SimpleMenuProvider(
			{ id, inventory, _ -> BingoCardGui(id, inventory, system, game, team, viewer, layout) },
			layout.title(TaskTussleResourcePackServer.active),
		)
	}

	override fun hasWon(team: ITTGameTeam, settings: TaskTussleSettings) = team.card.hasWon(settings.winningCondition)

	override fun timeUpWinner(game: ITTGame, settings: TaskTussleSettings): ITTGameTeam? {
		if (settings.overtime == "most tasks wins") {
			val best = game.teams.maxOf { it.completed }
			val leaders = game.teams.filter { it.completed == best }
			if (leaders.size == 1) return leaders.single()
		}
		return null
	}

	override fun goal(settings: TaskTussleSettings) = settings.winningCondition
	override fun settingsMenu(id: Int, inventory: Inventory, system: TaskTussleSystem) = BingoSettingsMenu(id, inventory, system)
	override fun settingsLore(settings: TaskTussleSettings) = listOf(
		"Succeed tokens: ${settings.succeedTokens}", "Overtime: ${settings.overtime}",
		"${if (settings.mutualTasks <= 25) "Mutual tasks" else "Task pool"}: ${settings.mutualTasks}",
		"Win condition: ${settings.winningCondition}",
	)

	fun openWinConditions(player: ServerPlayer, system: TaskTussleSystem) = system.menus.openMenu(player, SimpleMenuProvider(
		{ id, inventory, _ -> WinConditionMenu(id, inventory, system) }, Component.literal("Win condition picker"),
	))
}
