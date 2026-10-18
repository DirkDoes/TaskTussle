package me.wanttobee.tasktussle.teams

import java.util.UUID
import net.minecraft.ChatFormatting

data class Team(val index: Int, val members: MutableSet<UUID>) {
	val color: ChatFormatting = TEAM_COLORS[(index - 1) % TEAM_COLORS.size]

	fun displayName(): String = "Team $index"

	companion object {
		private val TEAM_COLORS = listOf(
			ChatFormatting.BLUE, ChatFormatting.RED, ChatFormatting.GREEN, ChatFormatting.YELLOW, ChatFormatting.LIGHT_PURPLE,
			ChatFormatting.GOLD, ChatFormatting.AQUA, ChatFormatting.DARK_GREEN, ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_AQUA,
		)
	}
}
