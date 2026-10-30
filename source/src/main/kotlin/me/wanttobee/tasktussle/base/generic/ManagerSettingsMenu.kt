package me.wanttobee.tasktussle.base.generic

import kotlin.math.max
import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.tasks.ITaskFiles
import me.wanttobee.tasktussle.base.tasks.ITaskManager
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

abstract class ManagerSettingsMenu(
	containerId: Int,
	inventory: Inventory,
	protected val system: TaskTussleSystem,
	private val manager: IManager,
	private val color: ChatFormatting = ChatFormatting.AQUA,
) : ActionChestMenu(containerId, inventory, 2) {
	protected fun refresh() {
		clear(); fill()
		val header = named(manager.settingsIcon, manager.name, color, listOf("Click: Go back"), enchanted = true).apply {
			if (manager.settingsIcon == manager.icon) manager.iconModel?.let { set(DataComponents.ITEM_MODEL, it) }
		}
		button(4, header) { player, _ -> system.menus.openSettings(player) }
		if (manager is ITaskManager) {
			val weight = manager.weight(system.settings)
			setting(0, Items.COMPARATOR, "Weight: $weight", ratioLore + "Shared across all games; zero disables this task type", count = max(1, weight), enchanted = weight > 0) { _, click ->
				manager.setWeight(system.settings, ratioChange(manager.weight(system.settings), click))
			}
		}
		populateSettings()
		broadcastChanges()
	}

	protected abstract fun populateSettings()

	protected fun controls(options: List<SettingControl>, firstSlot: Int = 9) {
		options.forEachIndexed { index, control ->
			button(firstSlot + index, control.stack) { player, click ->
				if (!system.menus.canEditSettings(player)) return@button
				control.action(player, click)
				if (player.containerMenu === this) refresh()
			}
		}
	}

	protected fun fileSetting(slot: Int, catalog: ITaskFiles) {
		val file = catalog.selectedFile(system.settings)
		setting(slot, Items.NAME_TAG, "File name: $file", listOf("Click to change the file used to generate tasks"), enchanted = true) { player, _ -> system.menus.openFilePicker(player, catalog) }
	}

	protected fun setting(slot: Int, item: Item, title: String, lore: List<String> = emptyList(), count: Int = 1, enchanted: Boolean = false, action: (ServerPlayer, Click) -> Unit) {
		button(slot, named(item, title, ChatFormatting.GOLD, lore, max(1, count), enchanted)) { player, click ->
			if (!system.menus.canEditSettings(player)) return@button
			action(player, click)
			if (player.containerMenu === this) refresh()
		}
	}
}
