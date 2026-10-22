package me.wanttobee.tasktussle.base.games

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.Identifier

internal class TaskGridLayout(private val font: Identifier = Identifier.fromNamespaceAndPath("tasktussle", "bingo_card")) {
	val rows = 6
	val viewedTeamSlot = 4
	val taskSlots = List(25) { index -> 11 + index % 5 + 9 * (index / 5) }
	val teamSlots = List(12) { 9 * (it % 6) + 8 * (it / 6) }

	fun title(textured: Boolean): Component {
		if (!textured) return Component.empty()
		// The font offsets Minecraft's title origin (8, 6) to the texture's top-left corner.
		return Component.literal("\uF800\uF801").withStyle {
			it.withFont(FontDescription.Resource(font))
				.withColor(ChatFormatting.WHITE).withItalic(false)
		}
	}
}
