package me.wanttobee.tasktussle.games.lockout

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.games.ITTGame
import me.wanttobee.tasktussle.base.games.ITTCardLogic
import me.wanttobee.tasktussle.base.games.ITTGameManager
import me.wanttobee.tasktussle.base.games.ITTGameTeam
import me.wanttobee.tasktussle.base.games.TaskGridLayout
import me.wanttobee.tasktussle.base.games.TimeoutRule
import me.wanttobee.tasktussle.base.games.uniqueLeader
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.TaskFactory
import me.wanttobee.tasktussle.resourcepack.TaskTussleResourcePackServer
import me.wanttobee.tasktussle.teams.Team
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items
import net.minecraft.resources.Identifier

object LockoutManager : ITTGameManager {
	override val key = "lockout"
	override val name = "Lockout"
	override val icon get() = Items.IRON_BARS
	override val teamRange = 1..12
	override val cardItemModel = Identifier.fromNamespaceAndPath("tasktussle", "lockout_card")

	override fun create(teams: List<Team>, settings: TaskTussleSettings, factory: TaskFactory): LockoutGame? {
		val templates = factory.generate(25, settings) ?: return null
		val limit = settings.sharedTaskCompletionLimit.coerceIn(1, teams.count { it.members.isNotEmpty() }.coerceAtLeast(1))
		val card = ITTCardLogic().apply { tasks += templates.map(factory::create) }
		return LockoutGame(teams.map { LockoutTeam(it, card) }, settings.gameSeconds, limit)
	}

	override fun cardMenu(system: TaskTussleSystem, game: ITTGame, team: ITTGameTeam, viewer: ITTGameTeam?): SimpleMenuProvider {
		val layout = TaskGridLayout(Identifier.fromNamespaceAndPath("tasktussle", "lockout_card"))
		return SimpleMenuProvider(
			{ id, inventory, _ -> LockoutCardMenu(id, inventory, system, game, team, viewer, layout, TaskTussleResourcePackServer.active) },
			layout.title(TaskTussleResourcePackServer.active),
		)
	}

	override fun isFinished(game: ITTGame, team: ITTGameTeam, settings: TaskTussleSettings) = team.tasks.isNotEmpty() && team.tasks.all { it.progress.closed(game.completionLimit) }
	override fun completionWinner(game: ITTGame, team: ITTGameTeam, settings: TaskTussleSettings) = mostTasks(game)
	override fun timeUpWinner(game: ITTGame, settings: TaskTussleSettings) = if (settings.lockoutTimeoutRule == TimeoutRule.DRAW) null else mostTasks(game)
	private fun mostTasks(game: ITTGame) = uniqueLeader(game.teams, compareBy { it.completed })
	override fun goal(settings: TaskTussleSettings) = "Claim the most tasks before the shared card closes (first ${settings.sharedTaskCompletionLimit} teams per task, capped to participating teams)"
	override fun settingsMenu(id: Int, inventory: Inventory, system: TaskTussleSystem) = LockoutSettingsMenu(id, inventory, system)
	override fun settingsControls(system: TaskTussleSystem) = LockoutSettingsMenu.controls(system)
	override fun settingsLore(settings: TaskTussleSettings) = listOf("Teams per task: ${settings.sharedTaskCompletionLimit}", "Time up: ${settings.lockoutTimeoutRule.label}", "Shared card and scores are always public")
}
