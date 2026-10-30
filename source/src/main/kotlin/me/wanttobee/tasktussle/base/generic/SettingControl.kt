package me.wanttobee.tasktussle.base.generic

import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/** One setting binding can be rendered on game pages and in ordinary submenus. */
data class SettingControl(val stack: ItemStack, val action: (ServerPlayer, Click) -> Unit)

internal fun settingControl(item: Item, title: String, lore: List<String> = emptyList(), count: Int = 1,
	enchanted: Boolean = false, model: Identifier? = null, action: (ServerPlayer, Click) -> Unit): SettingControl =
	SettingControl(named(item, title, ChatFormatting.GOLD, lore, count.coerceAtLeast(1), enchanted).apply {
		remove(DataComponents.ATTRIBUTE_MODIFIERS)
		model?.let { set(DataComponents.ITEM_MODEL, it) }
	}, action)
