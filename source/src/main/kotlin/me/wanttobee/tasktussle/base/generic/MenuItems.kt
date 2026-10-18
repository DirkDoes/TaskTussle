package me.wanttobee.tasktussle.base.generic

import kotlin.math.max
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.level.block.Blocks

internal val ratioLore = listOf("L Click: Increase ratio", "R Click: Decrease ratio", "Shift+R Click: Set ratio to 0", "A ratio of 0 turns it off")

internal val taskLore = listOf("Shift+L Click: Open details") + ratioLore

internal val mutualLore = listOf("L Click: Increase amount", "R Click: Decrease amount", "Shift+L Click: Set amount to 25", "Shift+R Click: Set amount to 0")

internal fun ratioChange(value: Int, click: Click): Int = when { click.right && click.shift -> 0; click.right -> max(0, value - 1); else -> minOf(100, value + 1) }

internal fun simpleChange(value: Int, click: Click, min: Int, max: Int): Int = (value + if (click.right) -1 else 1).coerceIn(min, max)

internal fun separator(title: String = " "): ItemStack = named(Blocks.STAINED_GLASS_PANE.gray().asItem(), title, ChatFormatting.DARK_GRAY).apply {
	set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("tasktussle", "empty_slot"))
}

internal fun emptyOption(type: String): ItemStack = named(Blocks.STAINED_GLASS.lightGray().asItem(), "Empty $type slot", ChatFormatting.GRAY, listOf("A spot for a future $type"))

internal fun emptySetting(): ItemStack = named(Blocks.STAINED_GLASS.lightGray().asItem(), "Empty setting slot", ChatFormatting.GRAY, listOf("a spot for a future setting"))

internal fun named(stack: ItemStack, name: String, color: ChatFormatting, lore: List<String> = emptyList(), count: Int = 1, enchanted: Boolean = false): ItemStack = stack.apply {
	val visualCount = count.coerceIn(1, 99)
	if (visualCount > maxStackSize) set(DataComponents.MAX_STACK_SIZE, visualCount)
	this.count = visualCount
	set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle { it.withColor(color).withItalic(false) })
	if (lore.isNotEmpty()) set(DataComponents.LORE, ItemLore(lore.filter(String::isNotBlank).map { Component.literal(it).withStyle { style -> style.withColor(ChatFormatting.GRAY).withItalic(false) } }))
	if (enchanted) set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
}

internal fun named(item: Item, name: String, color: ChatFormatting, lore: List<String> = emptyList(), count: Int = 1, enchanted: Boolean = false): ItemStack = named(ItemStack(item), name, color, lore, count, enchanted)
