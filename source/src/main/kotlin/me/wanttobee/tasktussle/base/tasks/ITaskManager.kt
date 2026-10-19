package me.wanttobee.tasktussle.base.tasks

import me.wanttobee.tasktussle.base.games.ITTGameTeam
import me.wanttobee.tasktussle.base.generic.IManager
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import net.minecraft.server.MinecraftServer

interface ITaskManager : IManager {
	val kind: TaskKind
	override val key: String get() = kind.id
	val files: ITaskFiles? get() = null
	fun weight(settings: TaskTussleSettings): Int
	fun setWeight(settings: TaskTussleSettings, value: Int)
	fun create(template: TaskTemplate): ITask
	fun repeatableTemplate(): TaskTemplate? = null
	fun explanation(settings: TaskTussleSettings): String? = null
	fun prepare(server: MinecraftServer, teams: List<ITTGameTeam>, settings: TaskTussleSettings) {}
}
