package me.wanttobee.tasktussle.games.bingo

import kotlin.math.max
import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.games.ITTCardGui
import me.wanttobee.tasktussle.base.games.ITTGame
import me.wanttobee.tasktussle.base.games.ITTGameTeam
import me.wanttobee.tasktussle.base.games.onlinePlayers
import me.wanttobee.tasktussle.base.generic.ActionChestMenu
import me.wanttobee.tasktussle.base.generic.named
import me.wanttobee.tasktussle.base.tasks.taskStack
import me.wanttobee.tasktussle.teams.emptyTeamSlot
import me.wanttobee.tasktussle.teams.teamItem
import net.minecraft.world.entity.player.Inventory

class BingoCardGui(
	containerId: Int,
	inventory: Inventory,
	private val system: TaskTussleSystem,
	private val game: ITTGame,
	private val team: ITTGameTeam,
	private val viewersTeam: ITTGameTeam?,
) : ActionChestMenu(containerId, inventory, 5), ITTCardGui {
	init { refresh() }

	override fun refresh() {
		clear()
		fill()
		team.tasks.forEachIndexed { index, task ->
			val slot = 2 + index % 5 + 9 * (index / 5)
			val stack = taskStack(system, task)
			if (!task.completed && !task.failed && viewersTeam == team && !game.paused && !game.finished) {
				button(slot, stack) { player, click -> system.clickTask(player, team.index, index, click.right) }
			} else display(slot, stack)
		}
		repeat(10) { index -> display(9 * (index % 5) + 8 * (index / 5), emptyTeamSlot()) }
		game.teams.take(10).forEachIndexed { index, other ->
			val slot = 9 * (index % 5) + 8 * (index / 5)
			val private = other == team
			val visible = private || game.finished || system.settings.cardVisibility == "visible"
			val progressVisible = private || game.finished || system.settings.cardVisibility != "hidden"
			val lore = mutableListOf(other.onlinePlayers(currentPlayers()).joinToString { it.gameProfile.name })
			if (progressVisible) lore += "${other.completed}/25"
			if (game.finished) lore += contributions(other)
			val stack = named(
				teamItem(other.index, visible), if (private) "This Card" else other.displayName(), other.color, lore,
				count = if (progressVisible) max(1, other.completed) else 1,
			)
			if (!private && (game.finished || system.settings.cardVisibility == "visible")) button(slot, stack) { player, _ -> system.openCard(player, other.index) }
			else display(slot, stack)
		}
		broadcastChanges()
	}

	private fun currentPlayers() = checkNotNull(playerInventory.player.level().server).playerList.players

	private fun contributions(team: ITTGameTeam): List<String> {
		if (team.completed == 0) return team.onlinePlayers(currentPlayers()).map { "${it.gameProfile.name} = 0.0%" }
		return team.onlinePlayers(currentPlayers()).map { player ->
			val count = team.tasks.count { it.completed && player.gameProfile.name in it.contributors }
			"${player.gameProfile.name} = ${count * 10000 / team.completed / 100.0}%"
		}
	}
}
