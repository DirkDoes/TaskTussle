package me.wanttobee.tasktussle.tasks.achievementTask

import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.ITask
import me.wanttobee.tasktussle.base.tasks.TaskTemplate
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

class AdvancementsTask(template: TaskTemplate) : ITask(template) {
	override val typeName = "Advancements Task"
	override val separateDescription = true
	override fun name(server: MinecraftServer?) = advancementName(server, template.id)
	override fun icon(server: MinecraftServer?, settings: TaskTussleSettings) = advancementIcon(server, settings, template.id)
	override fun description(server: MinecraftServer?) = advancementDescription(server, template.id) ?: "complete this advancement"
	override fun completedDescription(server: MinecraftServer?) = advancementDescription(server, template.id)
	override fun poll(player: ServerPlayer, settings: TaskTussleSettings) =
		player.level().server.advancements.get(template.id)?.let { player.advancements.getOrStartProgress(it).isDone } == true
}
