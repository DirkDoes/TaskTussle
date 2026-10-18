package me.wanttobee.tasktussle.base.tasks

internal fun deliveryTask(tasks: List<ITask>, item: net.minecraft.world.item.Item): ITask? = tasks.firstOrNull {
	!it.completed && !it.failed && it.acceptsDelivery(item)
}
