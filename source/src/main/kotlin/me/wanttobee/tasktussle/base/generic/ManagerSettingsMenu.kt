package me.wanttobee.tasktussle.base.generic

import kotlin.math.max
import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.tasks.ITaskFiles
import net.minecraft.ChatFormatting
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
		for (slot in 9 until 18) display(slot, emptySetting())
		button(4, named(manager.settingsIcon, manager.name, color, listOf("Click: Go back"), enchanted = true)) { player, _ -> system.menus.openSettings(player) }
		populateSettings()
		broadcastChanges()
	}

	protected abstract fun populateSettings()

	protected fun fileSetting(slot: Int, catalog: ITaskFiles) {
		val file = catalog.selectedFile(system.settings)
		setting(slot, Items.NAME_TAG, "File name: $file", listOf("Click to change the file used to generate tasks"), enchanted = true) { player, _ -> system.menus.openFilePicker(player, catalog) }
	}

	protected fun setting(slot: Int, item: Item, title: String, lore: List<String> = emptyList(), count: Int = 1, enchanted: Boolean = false, action: (ServerPlayer, Click) -> Unit) {
		button(slot, named(item, title, ChatFormatting.GOLD, lore, max(1, count), enchanted)) { player, click -> action(player, click); if (player.containerMenu === this) refresh() }
	}
}
