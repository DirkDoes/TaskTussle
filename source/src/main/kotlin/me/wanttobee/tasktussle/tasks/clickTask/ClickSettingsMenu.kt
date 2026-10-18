package me.wanttobee.tasktussle.tasks.clickTask

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.ManagerSettingsMenu
import net.minecraft.world.entity.player.Inventory

class ClickSettingsMenu(containerId: Int, inventory: Inventory, system: TaskTussleSystem) : ManagerSettingsMenu(containerId, inventory, system, ClickTaskManager) {
	init { refresh() }

	override fun populateSettings() {}
}
