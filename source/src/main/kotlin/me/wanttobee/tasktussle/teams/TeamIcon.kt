package me.wanttobee.tasktussle.teams

import me.wanttobee.tasktussle.base.generic.named
import net.minecraft.ChatFormatting
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks

internal val TEAMS_MODEL = Identifier.fromNamespaceAndPath("tasktussle", "teams")

internal fun teamItem(index: Int, solid: Boolean): Item = when ((index - 1) % 10) {
	0 -> if (solid) Blocks.CONCRETE.blue().asItem() else Blocks.STAINED_GLASS.blue().asItem()
	1 -> if (solid) Blocks.CONCRETE.red().asItem() else Blocks.STAINED_GLASS.red().asItem()
	2 -> if (solid) Blocks.CONCRETE.green().asItem() else Blocks.STAINED_GLASS.green().asItem()
	3 -> if (solid) Blocks.CONCRETE.yellow().asItem() else Blocks.STAINED_GLASS.yellow().asItem()
	4 -> if (solid) Blocks.CONCRETE.magenta().asItem() else Blocks.STAINED_GLASS.magenta().asItem()
	5 -> if (solid) Blocks.CONCRETE.orange().asItem() else Blocks.STAINED_GLASS.orange().asItem()
	6 -> if (solid) Blocks.CONCRETE.cyan().asItem() else Blocks.STAINED_GLASS.cyan().asItem()
	7 -> if (solid) Blocks.CONCRETE.green().asItem() else Blocks.STAINED_GLASS.green().asItem()
	8 -> if (solid) Blocks.CONCRETE.purple().asItem() else Blocks.STAINED_GLASS.purple().asItem()
	else -> if (solid) Blocks.CONCRETE.cyan().asItem() else Blocks.STAINED_GLASS.cyan().asItem()
}

internal fun teamColor(index: Int) = listOf(ChatFormatting.BLUE, ChatFormatting.RED, ChatFormatting.GREEN, ChatFormatting.YELLOW, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD, ChatFormatting.AQUA, ChatFormatting.DARK_GREEN, ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_AQUA)[(index - 1) % 10]

internal fun teamRows(count: Int): Int = minOf(6, count / 9 + 1)

internal fun emptyTeamSlot(): ItemStack = named(Blocks.STAINED_GLASS.gray().asItem(), "Empty team slot", ChatFormatting.GRAY)
