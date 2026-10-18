package me.wanttobee.tasktussle.tasks.obtainTask

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.ManagerSettingsMenu
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items

class ObtainSettingsMenu(containerId: Int, inventory: Inventory, system: TaskTussleSystem) : ManagerSettingsMenu(containerId, inventory, system, ObtainTaskManager) {
	init { refresh() }

	override fun populateSettings() {
		val settings = system.settings
		setting(9, Items.HOPPER, "Hand in items: ${if (settings.handInItems) "on" else "off"}", enchanted = settings.handInItems) { _, _ -> settings.handInItems = !settings.handInItems }
		fileSetting(10, ObtainTaskFiles)
	}
}
