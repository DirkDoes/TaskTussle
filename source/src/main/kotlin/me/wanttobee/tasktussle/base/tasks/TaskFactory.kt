package me.wanttobee.tasktussle.base.tasks

import me.wanttobee.tasktussle.base.games.ITTGameTeam
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer

/** Registrations are ordered so task selection keeps the existing randomization order. */
class TaskFactory(val managers: List<ITaskManager>) {
	private var pools: Map<TaskKind, TaskPools?>? = null

	init { require(managers.map { it.kind }.distinct().size == managers.size) { "Task kinds must be unique" } }

	fun reload(server: MinecraftServer, settings: TaskTussleSettings) {
		pools = managers.associate { manager -> manager.kind to manager.files?.let { it.load(server, it.selectedFile(settings)) } }
	}

	fun generate(amount: Int, settings: TaskTussleSettings, excluded: Set<Pair<TaskKind, Identifier>> = emptySet()): List<TaskTemplate>? {
		val loaded = pools ?: return null
		val sources = managers.map { TaskSource(it.kind, it.weight(settings), loaded[it.kind], it.repeatableTemplate()) }
		return TaskGenerator.generate(amount, sources, Triple(settings.easyRatio, settings.mediumRatio, settings.hardRatio), excluded)
	}

	fun create(template: TaskTemplate): ITask = managers.firstOrNull { it.kind == template.kind }?.create(template)
		?: error("No task manager registered for ${template.kind.id}")

	fun prepare(server: MinecraftServer, teams: List<ITTGameTeam>, settings: TaskTussleSettings) {
		managers.forEach { it.prepare(server, teams, settings) }
	}
}
