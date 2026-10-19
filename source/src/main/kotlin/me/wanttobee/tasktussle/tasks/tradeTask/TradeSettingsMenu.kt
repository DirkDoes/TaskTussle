package me.wanttobee.tasktussle.tasks.tradeTask

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.ManagerSettingsMenu
import net.minecraft.world.entity.player.Inventory

class TradeSettingsMenu(containerId: Int, inventory: Inventory, system: TaskTussleSystem) : ManagerSettingsMenu(containerId, inventory, system, TradeTaskManager) {
	init { refresh() }
	override fun populateSettings() { fileSetting(9, TradeTaskFiles) }
}
