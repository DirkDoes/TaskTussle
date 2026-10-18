package me.wanttobee.tasktussle.teams

import java.util.UUID
import me.wanttobee.tasktussle.base.games.ITTGameManager

data class TeamSelection(
	val teamCount: Int,
	val starter: UUID,
	val members: MutableMap<UUID, Int> = mutableMapOf(),
	val startsGame: Boolean,
	val manager: ITTGameManager? = null,
)
