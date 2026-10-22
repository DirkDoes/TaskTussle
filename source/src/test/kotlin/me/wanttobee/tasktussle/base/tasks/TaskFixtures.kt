package me.wanttobee.tasktussle.base.tasks

import java.util.UUID

internal fun ITask.claim(team: Int = 1, name: String = "Alice", limit: Int = 1, tick: Int = 0): Boolean =
	progress.complete(TaskCompletion(team, UUID.nameUUIDFromBytes(name.toByteArray()), name, tick), limit)
