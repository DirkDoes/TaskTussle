package me.wanttobee.tasktussle.games.lockout

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.games.ITTGame
import me.wanttobee.tasktussle.base.games.ITTGameTeam
import me.wanttobee.tasktussle.base.games.TaskGridLayout
import me.wanttobee.tasktussle.base.games.TaskGridMenu
import net.minecraft.world.entity.player.Inventory

internal class LockoutCardMenu(
	containerId: Int, inventory: Inventory, system: TaskTussleSystem,
	game: ITTGame, team: ITTGameTeam, viewer: ITTGameTeam?, layout: TaskGridLayout, textured: Boolean,
) : TaskGridMenu(containerId, inventory, system, game, team, viewer, layout, textured) {
	override val allowsTeamNavigation: Boolean get() = false
}
