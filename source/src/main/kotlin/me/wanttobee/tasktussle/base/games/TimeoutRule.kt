package me.wanttobee.tasktussle.base.games

enum class TimeoutRule(val label: String) {
	LINES_THEN_TASKS("Most lines, then tasks, then draw"),
	MOST_TASKS("Most tasks, then draw"),
	DRAW("Always draw");
}

internal fun uniqueLeader(teams: List<ITTGameTeam>, comparator: Comparator<ITTGameTeam>): ITTGameTeam? {
	val best = teams.maxWithOrNull(comparator) ?: return null
	return teams.singleOrNull { comparator.compare(it, best) == 0 }
}
