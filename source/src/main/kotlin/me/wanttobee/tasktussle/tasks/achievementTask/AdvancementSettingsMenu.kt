package me.wanttobee.tasktussle.tasks.achievementTask

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.ManagerSettingsMenu
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items

class AdvancementSettingsMenu(containerId: Int, inventory: Inventory, system: TaskTussleSystem) : ManagerSettingsMenu(containerId, inventory, system, AdvancementsTaskManager) {
	init { refresh() }

	override fun populateSettings() {
		fileSetting(9, AdvancementsFiles)
		val custom = system.settings.customAdvancementIcons
		setting(10, Items.ITEM_FRAME, "Advancement icons: ${if (custom) "custom" else "recipe books"}", enchanted = custom) { _, _ ->
			system.settings.customAdvancementIcons = !custom
			system.refreshCards()
		}
	}
}
