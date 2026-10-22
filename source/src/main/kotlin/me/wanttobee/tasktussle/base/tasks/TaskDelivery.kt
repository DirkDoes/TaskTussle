package me.wanttobee.tasktussle.base.tasks

internal fun deliveryTask(tasks: List<ITask>, item: net.minecraft.world.item.Item, teamIndex: Int, limit: Int): ITask? = tasks.firstOrNull {
	it.progress.state(teamIndex, limit) == TaskState.ACTIVE && it.acceptsDelivery(item)
}
