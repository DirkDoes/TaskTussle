package me.wanttobee.tasktussle.resourcepack

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.Identifier
import net.minecraft.world.inventory.MenuType

internal fun chestMenuTitle(title: Component, type: MenuType<*>): Component {
	val rows = when (type) {
		MenuType.GENERIC_9x1 -> 1
		MenuType.GENERIC_9x2 -> 2
		MenuType.GENERIC_9x3 -> 3
		MenuType.GENERIC_9x4 -> 4
		MenuType.GENERIC_9x5 -> 5
		MenuType.GENERIC_9x6 -> 6
		else -> return title
	}
	val background = Component.literal("\uF800${(0xF800 + rows).toChar()}\uF807").withStyle {
		it.withFont(FontDescription.Resource(Identifier.fromNamespaceAndPath("tasktussle", "chest")))
			.withColor(ChatFormatting.WHITE).withItalic(false)
	}
	return Component.empty().append(background).append(title)
}
