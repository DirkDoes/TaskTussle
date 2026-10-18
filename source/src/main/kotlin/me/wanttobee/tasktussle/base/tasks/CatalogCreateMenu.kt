package me.wanttobee.tasktussle.base.tasks

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.ActionHopperMenu
import me.wanttobee.tasktussle.base.generic.named
import me.wanttobee.tasktussle.base.generic.separator
import net.minecraft.ChatFormatting
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items

class CatalogCreateMenu(containerId: Int, inventory: Inventory, system: TaskTussleSystem, catalog: ITaskFiles) : ActionHopperMenu(containerId, inventory) {
	init {
		repeat(5) { display(it, separator()) }
		button(0, named(Items.ARROW, "Back", ChatFormatting.WHITE)) { player, _ -> system.menus.openCatalogFiles(player, catalog) }
		button(2, named(Items.KNOWLEDGE_BOOK, "Generated baseline", ChatFormatting.AQUA, listOf("Start with generated Easy, Normal and Hard tiers"))) { player, _ -> system.menus.createCatalog(player, catalog, true) }
		button(3, named(Items.PAPER, "Empty", ChatFormatting.GRAY, listOf("Start with every task removed"))) { player, _ -> system.menus.createCatalog(player, catalog, false) }
	}
}
