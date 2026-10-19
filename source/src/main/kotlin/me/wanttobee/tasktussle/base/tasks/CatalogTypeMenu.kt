package me.wanttobee.tasktussle.base.tasks

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.ActionHopperMenu
import me.wanttobee.tasktussle.base.generic.named
import me.wanttobee.tasktussle.base.generic.separator
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.player.Inventory

class CatalogTypeMenu(containerId: Int, inventory: Inventory, system: TaskTussleSystem) : ActionHopperMenu(containerId, inventory) {
	init {
		repeat(5) { display(it, separator()) }
		val managers = system.taskFactory.managers.filter { it.files != null }
		// shortcut: this picker has five slots; paginate it when registering a sixth catalog.
		managers.forEachIndexed { index, manager ->
			val catalog = requireNotNull(manager.files)
			val slot = if (managers.size == 2) 1 + index * 2 else index
			val icon = named(manager.icon, catalog.browserName, ChatFormatting.AQUA).apply {
				manager.iconModel?.let { set(DataComponents.ITEM_MODEL, it) }
			}
			button(slot, icon) { player, _ -> system.menus.openCatalogFiles(player, catalog) }
		}
	}
}
