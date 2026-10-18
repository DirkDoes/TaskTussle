package me.wanttobee.tasktussle.base.tasks

data class TaskSource(
	val kind: TaskKind,
	val weight: Int,
	val pools: TaskPools? = null,
	val repeatable: TaskTemplate? = null,
)
