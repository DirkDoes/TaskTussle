package me.wanttobee.tasktussle.games.bingo

import kotlin.math.max
import me.wanttobee.tasktussle.base.games.TimeoutRule
import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.ManagerSettingsMenu
import me.wanttobee.tasktussle.base.generic.SettingControl
import me.wanttobee.tasktussle.base.generic.settingControl
import me.wanttobee.tasktussle.base.generic.mutualLore
import net.minecraft.ChatFormatting
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items

class BingoSettingsMenu(containerId: Int, inventory: Inventory, system: TaskTussleSystem) : ManagerSettingsMenu(containerId, inventory, system, BingoManager, ChatFormatting.LIGHT_PURPLE) {
	init { refresh() }
	override fun populateSettings() { controls(controls(system)) }

	companion object {
		fun controls(system: TaskTussleSystem): List<SettingControl> {
			val settings = system.settings
			return listOf(
				settingControl(Items.CLOCK, "Time up: ${settings.bingoTimeoutRule.label}", listOf("Click: Change timeout rule", "Lines include rows, columns and diagonals"), enchanted = true) { _, _ ->
					settings.bingoTimeoutRule = TimeoutRule.entries[(settings.bingoTimeoutRule.ordinal + 1) % TimeoutRule.entries.size]
				},
				settingControl(Items.ENDER_PEARL, "${if (settings.mutualTasks <= 25) "Mutual tasks" else "Task pool"}: ${settings.mutualTasks}", mutualLore, max(1, settings.mutualTasks), settings.mutualTasks > 0) { _, click ->
					settings.mutualTasks = when { click.left && click.shift -> 25; click.right && click.shift -> 0; click.right -> max(0, settings.mutualTasks - 1); else -> settings.mutualTasks + 1 }
				},
				settingControl(Items.AMETHYST_SHARD, "Win condition: ${settings.winningCondition}", enchanted = true) { player, _ -> BingoManager.openWinConditions(player, system) },
				settingControl(Items.ENDER_EYE, "Card visibility: ${settings.cardVisibility}", listOf("What other teams can see")) { _, _ ->
					val options = listOf("progress only", "visible", "hidden")
					settings.cardVisibility = options[(options.indexOf(settings.cardVisibility) + 1) % options.size]
					system.refreshCards()
				},
			)
		}
	}
}
