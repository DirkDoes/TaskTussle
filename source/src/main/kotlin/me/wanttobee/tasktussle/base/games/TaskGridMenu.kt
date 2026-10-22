package me.wanttobee.tasktussle.base.games

import kotlin.math.max
import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.ActionChestMenu
import me.wanttobee.tasktussle.base.generic.named
import me.wanttobee.tasktussle.base.tasks.taskStack
import me.wanttobee.tasktussle.base.tasks.TaskState
import me.wanttobee.tasktussle.teams.emptyTeamSlot
import me.wanttobee.tasktussle.teams.teamItem
import net.minecraft.world.entity.player.Inventory

internal open class TaskGridMenu(
	containerId: Int,
	inventory: Inventory,
	private val system: TaskTussleSystem,
	private val game: ITTGame,
	private val team: ITTGameTeam,
	private val viewersTeam: ITTGameTeam?,
	private val layout: TaskGridLayout,
	private val textured: Boolean,
) : ActionChestMenu(containerId, inventory, layout.rows), ITTCardGui {
	override val customBackground = true
	protected open val allowsTeamNavigation: Boolean get() = true
	init { refresh() }

	override fun refresh() {
		clear()
		if (!textured) {
			fill()
			layout.teamSlots.forEach { display(it, emptyTeamSlot()) }
		}
		display(layout.viewedTeamSlot, named(teamItem(team.index, true), team.displayName(), team.color))
		team.tasks.forEachIndexed { index, task ->
			val slot = layout.taskSlots[index]
			val stack = taskStack(system, task, team.index, game)
			if (task.progress.state(team.index, game.completionLimit) == TaskState.ACTIVE && viewersTeam == team && !game.paused && !game.finished) {
				button(slot, stack) { player, click -> system.clickTask(player, team.index, index, click.right) }
			} else display(slot, stack)
		}
		game.teams.forEachIndexed { index, other ->
			val slot = layout.teamSlots[index]
			val private = other == team
			val visible = private || allowsTeamNavigation && (game.finished || system.settings.cardVisibility == "visible")
			val progressVisible = !allowsTeamNavigation || private || game.finished || system.settings.cardVisibility != "hidden"
			val lore = mutableListOf(other.onlinePlayers(currentPlayers()).joinToString { it.gameProfile.name })
			if (progressVisible) lore += "${other.completed}/${other.tasks.size}"
			if (game.finished) lore += other.contributions(currentPlayers())
			val stack = named(
				teamItem(other.index, visible), if (private) "This Card" else other.displayName(), other.color, lore,
				count = if (progressVisible) max(1, other.completed) else 1,
				enchanted = private,
			)
			if (!private && allowsTeamNavigation && visible) button(slot, stack) { player, _ -> system.openCard(player, other.index) }
			else display(slot, stack)
		}
		broadcastChanges()
	}

	private fun currentPlayers() = system.server?.playerList?.players.orEmpty()
}
