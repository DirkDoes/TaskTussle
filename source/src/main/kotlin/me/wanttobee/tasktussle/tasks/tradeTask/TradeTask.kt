package me.wanttobee.tasktussle.tasks.tradeTask

import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.ITask
import me.wanttobee.tasktussle.base.tasks.TaskEvent
import me.wanttobee.tasktussle.base.tasks.TaskTemplate
import net.minecraft.server.MinecraftServer

class TradeTask(template: TaskTemplate) : ITask(template) {
	override val typeName = "Trade Task"
	override val separateDescription = true
	override fun name(server: MinecraftServer?) = TradeTaskFiles.name(server, template.id)
	override fun icon(server: MinecraftServer?, settings: TaskTussleSettings) = TradeTaskFiles.icon(server, settings, template.id)
	override fun description(server: MinecraftServer?) = "complete a trade that buys or sells this item"
	override fun onEvent(event: TaskEvent, settings: TaskTussleSettings) = event is TradeEvent && template.id in event.items
}
