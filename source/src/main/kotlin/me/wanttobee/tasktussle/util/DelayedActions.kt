package me.wanttobee.tasktussle.util

internal fun <T> takeDue(tick: Int, scheduled: MutableList<Pair<Int, T>>): List<T> {
	val due = scheduled.filter { it.first <= tick }
	scheduled.removeAll(due)
	return due.map { it.second }
}

internal fun titleFrames(text: String, ticks: Int): List<Pair<Int, String>> = text.indices.map { index ->
	(if (text.length == 1) 0 else index * ticks / (text.length - 1)) to text.take(index + 1)
}
