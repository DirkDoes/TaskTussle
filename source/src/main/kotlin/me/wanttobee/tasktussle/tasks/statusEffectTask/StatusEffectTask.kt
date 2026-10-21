package me.wanttobee.tasktussle.tasks.statusEffectTask

import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.ITask
import me.wanttobee.tasktussle.base.tasks.TaskEvent
import me.wanttobee.tasktussle.base.tasks.TaskTemplate
import net.minecraft.server.MinecraftServer

class StatusEffectTask(template: TaskTemplate) : ITask(template) {
	override val typeName = "Status Effect Task"
	override val separateDescription = true
	override fun name(server: MinecraftServer?) = StatusEffectTaskFiles.name(server, template.id)
	override fun icon(server: MinecraftServer?, settings: TaskTussleSettings) = StatusEffectTaskFiles.icon(server, settings, template.id)
	override fun description(server: MinecraftServer?) = "have this status effect applied to you"
	override fun onEvent(event: TaskEvent, settings: TaskTussleSettings) = event is StatusEffectEvent && event.effect == template.id
}
