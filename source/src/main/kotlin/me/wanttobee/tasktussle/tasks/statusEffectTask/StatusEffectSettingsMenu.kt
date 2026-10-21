package me.wanttobee.tasktussle.tasks.statusEffectTask

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.ManagerSettingsMenu
import net.minecraft.world.entity.player.Inventory

class StatusEffectSettingsMenu(containerId: Int, inventory: Inventory, system: TaskTussleSystem) : ManagerSettingsMenu(containerId, inventory, system, StatusEffectTaskManager) {
	init { refresh() }
	override fun populateSettings() { fileSetting(9, StatusEffectTaskFiles) }
}
