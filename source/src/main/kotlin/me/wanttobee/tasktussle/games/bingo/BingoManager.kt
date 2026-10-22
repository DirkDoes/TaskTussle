package me.wanttobee.tasktussle.games.bingo

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.games.ITTGame
import me.wanttobee.tasktussle.base.games.TaskGridLayout
import me.wanttobee.tasktussle.base.games.TaskGridMenu
import me.wanttobee.tasktussle.base.games.ITTGameManager
import me.wanttobee.tasktussle.base.games.ITTGameTeam
import me.wanttobee.tasktussle.base.games.TimeoutRule
import me.wanttobee.tasktussle.base.games.uniqueLeader
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.TaskFactory
import me.wanttobee.tasktussle.resourcepack.TaskTussleResourcePackServer
import me.wanttobee.tasktussle.teams.Team
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items
import net.minecraft.resources.Identifier

object BingoManager : ITTGameManager {
	override val key = "bingo"
	override val name = "Bingo"
	override val icon get() = Items.FILLED_MAP
	override val teamRange = 1..12
	override val cardItemModel = Identifier.fromNamespaceAndPath("tasktussle", "bingo_card")

	override fun create(teams: List<Team>, settings: TaskTussleSettings, factory: TaskFactory): BingoGame? {
		val mutual = factory.generate(settings.mutualTasks, settings) ?: return null
		val gameTeams = teams.map(::BingoTeam)
		for (team in gameTeams) {
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
		return BingoGame(gameTeams, settings.gameSeconds)
	}

	override fun cardMenu(system: TaskTussleSystem, game: ITTGame, team: ITTGameTeam, viewer: ITTGameTeam?): SimpleMenuProvider {
		val layout = TaskGridLayout()
		return SimpleMenuProvider(
			{ id, inventory, _ -> TaskGridMenu(id, inventory, system, game, team, viewer, layout, TaskTussleResourcePackServer.active) },
			layout.title(TaskTussleResourcePackServer.active),
		)
	}

	override fun isFinished(game: ITTGame, team: ITTGameTeam, settings: TaskTussleSettings) = BingoCardLogic.hasWon(team.tasks, settings.winningCondition, team.index)

	override fun timeUpWinner(game: ITTGame, settings: TaskTussleSettings): ITTGameTeam? = when (settings.bingoTimeoutRule) {
		TimeoutRule.DRAW -> null
		TimeoutRule.MOST_TASKS -> uniqueLeader(game.teams, compareBy { it.completed })
		TimeoutRule.LINES_THEN_TASKS -> uniqueLeader(game.teams,
			compareBy<ITTGameTeam> { BingoCardLogic.completedLines(it.tasks, it.index) }.thenBy { it.completed })
	}

	override fun goal(settings: TaskTussleSettings) = settings.winningCondition
	override fun settingsMenu(id: Int, inventory: Inventory, system: TaskTussleSystem) = BingoSettingsMenu(id, inventory, system)
	override fun settingsLore(settings: TaskTussleSettings) = listOf(
		"Time up: ${settings.bingoTimeoutRule.label}",
		"${if (settings.mutualTasks <= 25) "Mutual tasks" else "Task pool"}: ${settings.mutualTasks}",
		"Win condition: ${settings.winningCondition}",
	)

	fun openWinConditions(player: ServerPlayer, system: TaskTussleSystem) = system.menus.openMenu(player, SimpleMenuProvider(
		{ id, inventory, _ -> WinConditionMenu(id, inventory, system) }, Component.literal("Win condition picker"),
	))
}
