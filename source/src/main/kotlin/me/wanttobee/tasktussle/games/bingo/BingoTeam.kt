package me.wanttobee.tasktussle.games.bingo

import me.wanttobee.tasktussle.base.games.ITTGameTeam
import me.wanttobee.tasktussle.teams.Team

data class BingoTeam(override val team: Team, override val card: BingoCardLogic = BingoCardLogic()) : ITTGameTeam
