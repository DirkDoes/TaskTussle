package me.wanttobee.tasktussle.games.bingo

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.ActionChestMenu
import me.wanttobee.tasktussle.base.generic.named
import me.wanttobee.tasktussle.base.generic.separator
import net.minecraft.ChatFormatting
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks

class WinConditionMenu(containerId: Int, inventory: Inventory, private val system: TaskTussleSystem) : ActionChestMenu(containerId, inventory, 1) {
	init {
		val options = listOf(
			Blocks.SMALL_AMETHYST_BUD.asItem() to "1 line", Blocks.MEDIUM_AMETHYST_BUD.asItem() to "2 lines",
			Blocks.LARGE_AMETHYST_BUD.asItem() to "3 lines", Blocks.AMETHYST_CLUSTER.asItem() to "4 lines",
			Blocks.AMETHYST_BLOCK.asItem() to "full card", null, Items.AMETHYST_SHARD to "horizontal line",
			Items.AMETHYST_SHARD to "vertical line", Items.AMETHYST_SHARD to "diagonal line",
		)
		options.forEachIndexed { slot, option ->
			if (option == null) display(slot, separator()) else button(slot, named(option.first, option.second, ChatFormatting.LIGHT_PURPLE, if (system.settings.winningCondition == option.second) listOf("current") else emptyList(), enchanted = system.settings.winningCondition == option.second)) { player, _ ->
				if (!system.menus.canEditSettings(player)) return@button
				system.settings.winningCondition = option.second
				system.menus.openSettings(player)
			}
		}
	}
}
