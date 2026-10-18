package me.wanttobee.tasktussle.base.tasks

// Open identifiers let additional task managers participate without changing a central enum.
data class TaskKind(val id: String) {
	companion object {
		val BLOCK = TaskKind("obtain")
		val ADVANCEMENT = TaskKind("advancements")
		val CLICK = TaskKind("click")
	}
}
