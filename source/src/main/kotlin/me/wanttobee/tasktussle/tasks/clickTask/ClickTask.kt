package me.wanttobee.tasktussle.tasks.clickTask

import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.ITask
import me.wanttobee.tasktussle.base.tasks.TaskTemplate
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class ClickTask(template: TaskTemplate) : ITask(template) {
	override val typeName = "Click Task"
	override fun name(server: MinecraftServer?) = "CLICK ME"
	override fun description(server: MinecraftServer?) = "click me"
	override fun click(player: ServerPlayer, settings: TaskTussleSettings) = true
	override fun icon(server: MinecraftServer?, settings: TaskTussleSettings) = ItemStack(Items.PAPER).apply {
		set(DataComponents.ITEM_MODEL, models.random())
	}

	companion object {
		private val models = (1..3).map { Identifier.fromNamespaceAndPath("tasktussle", "click_me$it") }
	}
}
