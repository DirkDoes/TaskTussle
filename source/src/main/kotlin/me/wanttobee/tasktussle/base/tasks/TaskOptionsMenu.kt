package me.wanttobee.tasktussle.base.tasks

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.ActionHopperMenu
import me.wanttobee.tasktussle.base.generic.named
import me.wanttobee.tasktussle.base.generic.separator
import net.minecraft.ChatFormatting
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.level.block.Blocks

class TaskOptionsMenu(
	containerId: Int,
	inventory: Inventory,
	private val system: TaskTussleSystem,
	private val teamIndex: Int,
	private val taskIndex: Int,
) : ActionHopperMenu(containerId, inventory) {
	init {
		val team = system.currentGame()?.teams?.find { it.index == teamIndex }
		val task = team?.tasks?.getOrNull(taskIndex)
		if (team != null && task != null) {
			button(0, named(task.icon(system.server, system.settings), task.name(system.server), ChatFormatting.WHITE, listOf("Click: go back to the card"))) { player, _ -> system.openCard(player, teamIndex) }
			display(1, separator())
			for (slot in 2..4) display(slot, named(Blocks.DYED_CANDLE.lightGray().asItem(), "Empty token slot", ChatFormatting.GRAY))
			val available = team.succeedTokens > 0
			button(2, named(if (available) Blocks.DYED_CANDLE.lime().asItem() else Blocks.DYED_CANDLE.gray().asItem(), "Succeed token", if (available) ChatFormatting.GREEN else ChatFormatting.GRAY, listOf("${team.succeedTokens} left", "Complete the task instantly"))) { player, _ ->
				if (system.useSucceedToken(player, teamIndex, taskIndex)) system.openCard(player, teamIndex)
			}
		}
	}
}
