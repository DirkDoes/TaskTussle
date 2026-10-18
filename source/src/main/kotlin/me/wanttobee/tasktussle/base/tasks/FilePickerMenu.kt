package me.wanttobee.tasktussle.base.tasks

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.ActionChestMenu
import me.wanttobee.tasktussle.base.generic.named
import me.wanttobee.tasktussle.base.generic.separator
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile

class FilePickerMenu(
	containerId: Int,
	inventory: Inventory,
	private val system: TaskTussleSystem,
	private val catalog: ITaskFiles,
	private val browse: Boolean = false,
	initialSelection: String? = null,
) : ActionChestMenu(containerId, inventory, if (browse) 6 else pickerRows(system.menus.catalogFiles(catalog).size)) {
	private var selected = initialSelection
	init { refresh() }

	private fun refresh() {
		clear()
		val files = system.menus.catalogFiles(catalog)
		if (browse) {
			for (slot in 9..17) display(slot, separator())
			button(0, named(Items.ARROW, "Back", ChatFormatting.WHITE)) { player, _ -> system.menus.openTaskBrowser(player) }
			button(8, named(Items.WRITABLE_BOOK, "Create new file", ChatFormatting.GREEN)) { player, _ -> system.menus.openCatalogCreation(player, catalog) }
			val chosen = files.find { it.name == selected }
			if (chosen == null) display(4, named(Items.DYE.gray(), "Nothing selected", ChatFormatting.GRAY))
			else {
				button(4, fileStack(chosen, files.indexOf(chosen), listOf("Click to open"), true)) { player, _ -> system.menus.openCatalogContents(player, catalog, chosen.name) }
				button(6, named(Items.PAPER, "Duplicate", ChatFormatting.AQUA, listOf(chosen.name))) { player, _ ->
					system.menus.duplicateCatalog(player, catalog, chosen.name)?.let { selected = it; refresh() }
				}
				if (system.menus.canEditCatalog(playerInventory.player as ServerPlayer, catalog, chosen.name)) {
					button(2, named(Items.BARRIER, "Delete", ChatFormatting.RED, listOf(chosen.name))) { player, _ ->
						if (system.menus.deleteCatalog(player, catalog, chosen.name)) { selected = null; refresh() }
					}
				}
			}
		} else fill()
		files.forEachIndexed { index, file ->
			val current = file.name == catalog.selectedFile(system.settings)
			val chosen = selected == file.name
			val lore = when { chosen -> listOf("Click again to open"); current && !browse -> listOf("current"); else -> emptyList() }
			button((if (browse) 18 else 0) + index, fileStack(file, index, lore, chosen || current && !browse)) { player, _ ->
				if (browse && chosen) system.menus.openCatalogContents(player, catalog, file.name)
				else if (browse) { selected = file.name; refresh() }
				else if (system.menus.selectCatalog(player, catalog, file.name)) system.menus.openManagerSettings(player, catalog.managerKey)
			}
		}
		broadcastChanges()
	}

	private fun fileStack(file: CatalogFile, index: Int, lore: List<String>, enchanted: Boolean): ItemStack {
		val stack = named(if (file.owner == null) if (index % 2 == 0) Items.PAPER else Items.MAP else Items.PLAYER_HEAD, file.name, ChatFormatting.YELLOW, lore, enchanted = enchanted)
		if (file.owner != null) stack.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(file.owner))
		return stack
	}
}
