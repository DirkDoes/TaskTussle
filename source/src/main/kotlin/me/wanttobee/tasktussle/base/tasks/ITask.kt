package me.wanttobee.tasktussle.base.tasks

import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/** A task owns its completion checks and presentation; the running game owns rewards and winning. */
abstract class ITask(val template: TaskTemplate) {
	var completed = false
	var failed = false
	val contributors = mutableSetOf<String>()

	abstract val typeName: String
	abstract fun name(server: MinecraftServer?): String
	abstract fun icon(server: MinecraftServer?, settings: TaskTussleSettings): ItemStack
	abstract fun description(server: MinecraftServer?): String
	open val separateDescription: Boolean = false
	open fun completedDescription(server: MinecraftServer?): String? = null
	open fun poll(player: ServerPlayer, settings: TaskTussleSettings): Boolean = false
	open fun click(player: ServerPlayer, settings: TaskTussleSettings): Boolean = false
	open fun onEvent(event: TaskEvent, settings: TaskTussleSettings): Boolean = false
	open fun acceptsDelivery(item: Item): Boolean = false
}
