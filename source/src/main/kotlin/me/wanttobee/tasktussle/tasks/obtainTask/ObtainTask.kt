package me.wanttobee.tasktussle.tasks.obtainTask

import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.ITask
import me.wanttobee.tasktussle.base.tasks.TaskTemplate
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class ObtainTask(template: TaskTemplate) : ITask(template) {
	override val typeName = "Obtain Task"
	override fun name(server: MinecraftServer?) = BuiltInRegistries.BLOCK.getValue(template.id).name.string
	private fun requiredItem(): Item = BuiltInRegistries.BLOCK.getValue(template.id).asItem()
	override fun description(server: MinecraftServer?) = "obtain this block"
	override fun icon(server: MinecraftServer?, settings: TaskTussleSettings) = ItemStack(Items.PAPER).apply {
		set(DataComponents.ITEM_MODEL, BuiltInRegistries.ITEM.getKey(requiredItem()))
	}

	override fun poll(player: ServerPlayer, settings: TaskTussleSettings): Boolean {
		val wanted = requiredItem()
		val stacks = buildList {
			for (slot in 0 until player.inventory.containerSize) add(player.inventory.getItem(slot))
			add(player.containerMenu.carried)
		}
		return stacks.any { it.item == wanted && !it.isEmpty }
	}

	override fun click(player: ServerPlayer, settings: TaskTussleSettings) = poll(player, settings)
}
