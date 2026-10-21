package me.wanttobee.tasktussle.games.bingo

import kotlin.math.max
import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.ManagerSettingsMenu
import me.wanttobee.tasktussle.base.generic.mutualLore
import net.minecraft.ChatFormatting
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items

class BingoSettingsMenu(containerId: Int, inventory: Inventory, system: TaskTussleSystem) : ManagerSettingsMenu(containerId, inventory, system, BingoManager, ChatFormatting.LIGHT_PURPLE) {
	init { refresh() }

	override fun populateSettings() {
		val settings = system.settings
		setting(9, Items.STICK, "Overdue: ${settings.overtime}", listOf("What happens when game time reaches zero"), enchanted = true) { _, _ -> settings.overtime = if (settings.overtime == "draw") "most tasks wins" else "draw" }
		setting(10, Items.ENDER_PEARL, "${if (settings.mutualTasks <= 25) "Mutual tasks" else "Task pool"}: ${settings.mutualTasks}", mutualLore, max(1, settings.mutualTasks), settings.mutualTasks > 0) { _, click ->
			settings.mutualTasks = when { click.left && click.shift -> 25; click.right && click.shift -> 0; click.right -> max(0, settings.mutualTasks - 1); else -> settings.mutualTasks + 1 }
		}
		setting(11, Items.AMETHYST_SHARD, "Win condition: ${settings.winningCondition}", enchanted = true) { player, _ -> BingoManager.openWinConditions(player, system) }
	}
}
