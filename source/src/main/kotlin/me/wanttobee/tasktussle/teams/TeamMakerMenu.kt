package me.wanttobee.tasktussle.teams

import kotlin.math.max
import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.ActionChestMenu
import me.wanttobee.tasktussle.base.generic.named
import net.minecraft.world.entity.player.Inventory

class TeamMakerMenu(
	containerId: Int,
	inventory: Inventory,
	private val system: TaskTussleSystem,
	private val selection: TeamSelection,
) : ActionChestMenu(containerId, inventory, teamRows(selection.teamCount)) {
	init { refresh() }
	fun refresh() {
		clear(); fill()
		for (index in 1..selection.teamCount) {
			val memberIds = selection.members.filterValues { it == index }.keys
			val players = checkNotNull(playerInventory.player.level().server).playerList.players.filter { it.uuid in memberIds }
			val occupied = players.isNotEmpty()
			val selected = selection.members[playerInventory.player.uuid] == index
			button(index - 1, named(teamItem(index, occupied), "Team $index", teamColor(index), if (occupied) listOf(players.joinToString { it.gameProfile.name }) else listOf("Empty"), max(1, players.size), selected)) { player, _ ->
				system.teams.toggleTeam(player, index)
			}
		}
		broadcastChanges()
	}
}
