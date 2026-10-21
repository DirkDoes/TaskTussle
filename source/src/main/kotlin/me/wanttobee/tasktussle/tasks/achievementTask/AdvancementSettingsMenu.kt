package me.wanttobee.tasktussle.tasks.achievementTask

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.ManagerSettingsMenu
import net.minecraft.world.entity.player.Inventory

class AdvancementSettingsMenu(containerId: Int, inventory: Inventory, system: TaskTussleSystem) : ManagerSettingsMenu(containerId, inventory, system, AdvancementsTaskManager) {
	init { refresh() }

	override fun populateSettings() { fileSetting(9, AdvancementsFiles) }
}
