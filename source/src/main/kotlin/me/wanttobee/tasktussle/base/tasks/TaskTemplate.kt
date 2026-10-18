package me.wanttobee.tasktussle.base.tasks

import net.minecraft.resources.Identifier

data class TaskTemplate(
	val kind: TaskKind,
	val id: Identifier,
	val difficulty: Difficulty,
)
