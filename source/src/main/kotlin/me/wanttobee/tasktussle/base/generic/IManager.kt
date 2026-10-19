package me.wanttobee.tasktussle.base.generic

import me.wanttobee.tasktussle.TaskTussleSystem
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.Item

interface IManager {
	val key: String
	val name: String
	val icon: Item
	val iconModel: Identifier? get() = null
	val settingsIcon: Item get() = icon
	fun settingsMenu(id: Int, inventory: Inventory, system: TaskTussleSystem): AbstractContainerMenu
	fun settingsLore(settings: TaskTussleSettings): List<String> = emptyList()
}
