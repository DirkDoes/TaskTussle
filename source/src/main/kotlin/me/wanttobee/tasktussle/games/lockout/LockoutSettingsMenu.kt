package me.wanttobee.tasktussle.games.lockout

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.games.TimeoutRule
import me.wanttobee.tasktussle.base.generic.ManagerSettingsMenu
import net.minecraft.ChatFormatting
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items

class LockoutSettingsMenu(containerId: Int, inventory: Inventory, system: TaskTussleSystem) : ManagerSettingsMenu(containerId, inventory, system, LockoutManager, ChatFormatting.LIGHT_PURPLE) {
	init { refresh() }
	override fun populateSettings() {
		val settings = system.settings
		setting(9, Items.TRIPWIRE_HOOK, "Teams per task: ${settings.sharedTaskCompletionLimit}",
			listOf("Left click: +1; right click: -1", "Capped to participating teams at game start"), settings.sharedTaskCompletionLimit) { _, click ->
			settings.sharedTaskCompletionLimit = (settings.sharedTaskCompletionLimit + if (click.right) -1 else 1).coerceIn(1, 12)
		}
		setting(10, Items.CLOCK, "Time up: ${settings.lockoutTimeoutRule.label}", listOf("Click: Change timeout rule"), enchanted = true) { _, _ ->
			settings.lockoutTimeoutRule = if (settings.lockoutTimeoutRule == TimeoutRule.DRAW) TimeoutRule.MOST_TASKS else TimeoutRule.DRAW
		}
	}
}
