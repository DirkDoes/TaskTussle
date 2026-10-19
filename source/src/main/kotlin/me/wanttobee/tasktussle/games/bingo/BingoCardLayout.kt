package me.wanttobee.tasktussle.games.bingo

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.Identifier

internal class BingoCardLayout(teamCount: Int) {
	private val teamsOnRight = teamCount > 10
	val taskSlots = List(25) { index -> (if (teamsOnRight) 0 else 2) + index % 5 + 9 * (index / 5) }
	val teamSlots = if (teamsOnRight) List(15) { 6 + it % 3 + 9 * (it / 3) }
		else List(10) { 9 * (it % 5) + 8 * (it / 5) }

	fun title(textured: Boolean): Component {
		if (!textured) return Component.literal("Bingo")
		// The font offsets Minecraft's title origin (8, 6) to the texture's top-left corner.
		val background = Component.literal(if (teamsOnRight) "\uF800\uF802\uF803" else "\uF800\uF801\uF803").withStyle {
			it.withFont(FontDescription.Resource(Identifier.fromNamespaceAndPath("tasktussle", "bingo_card")))
				.withColor(ChatFormatting.WHITE).withItalic(false)
		}
		return Component.empty().append(background).append(Component.literal("Bingo"))
	}
}
