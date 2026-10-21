package me.wanttobee.tasktussle.tasks.achievementTask

import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.SmithingTemplateItem
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.world.item.component.TooltipDisplay

fun advancementName(server: MinecraftServer?, id: Identifier): String =
	server?.advancements?.get(id)?.value?.display?.orElse(null)?.title?.string
		?: id.path.substringAfterLast('/').replace('_', ' ').replaceFirstChar(Char::uppercase)

fun advancementDescription(server: MinecraftServer?, id: Identifier): String? =
	server?.advancements?.get(id)?.value?.display?.orElse(null)?.description?.string?.takeIf(String::isNotBlank)

fun advancementIcon(server: MinecraftServer?, id: Identifier): ItemStack {
	val icon = server?.advancements?.get(id)?.value?.display?.orElse(null)?.icon?.create() ?: ItemStack(Items.KNOWLEDGE_BOOK)
	val style = advancementIcons[id.toString()]
	val mappedItem = style?.let { BuiltInRegistries.ITEM.getValue(Identifier.parse(it.item)) }
	val selected = if (mappedItem != null && icon.item != mappedItem) ItemStack(mappedItem) else icon
	return (if (selected.item is SmithingTemplateItem) ItemStack(Items.PAPER) else selected).apply {
		set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT
			.withHidden(DataComponents.ATTRIBUTE_MODIFIERS, true)
			.withHidden(DataComponents.POT_DECORATIONS, true)
			.withHidden(DataComponents.BANNER_PATTERNS, true)
			.withHidden(DataComponents.BEES, true)
			.withHidden(DataComponents.BLOCK_STATE, true))
		if (style != null) {
			set(DataComponents.ITEM_MODEL, ADVANCEMENT_ICON_MODEL)
			set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData(emptyList(), emptyList(), listOf(style.plate, style.item), emptyList()))
		}
	}
}

private val ADVANCEMENT_ICON_MODEL = Identifier.fromNamespaceAndPath("tasktussle", "advancement_icon")
