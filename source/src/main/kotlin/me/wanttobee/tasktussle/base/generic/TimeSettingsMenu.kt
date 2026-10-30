package me.wanttobee.tasktussle.base.generic

import me.wanttobee.tasktussle.TaskTussleSystem
import net.minecraft.ChatFormatting
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks

class TimeSettingsMenu(containerId: Int, inventory: Inventory, private val system: TaskTussleSystem) : ActionDispenserMenu(containerId, inventory) {
	override fun clicked(slotId: Int, button: Int, input: ContainerInput, player: Player) {
		if (player !is ServerPlayer || !system.menus.canEditSettings(player)) return
		super.clicked(slotId, button, input, player)
	}

	init { refresh() }
	private fun refresh() {
		clear(); repeat(9) { display(it, separator()) }
		button(4, named(Items.CLOCK, "Game Time: ${if (system.settings.gameMinutes == 0) "disabled" else "${system.settings.gameMinutes} minutes"}", ChatFormatting.GOLD, listOf("Click to go back"), enchanted = system.settings.gameMinutes > 0)) { player, _ -> system.menus.openSettings(player) }
		button(0, named(Blocks.DYED_CANDLE.red().asItem(), "-1", ChatFormatting.RED)) { _, _ -> system.settings.adjustGameTime(-1); refresh() }
		button(6, named(Blocks.WOOL.red().asItem(), "-10", ChatFormatting.RED)) { _, _ -> system.settings.adjustGameTime(-10); refresh() }
		button(2, named(Blocks.DYED_CANDLE.lime().asItem(), "+1", ChatFormatting.GREEN)) { _, _ -> system.settings.adjustGameTime(1); refresh() }
		button(8, named(Blocks.WOOL.lime().asItem(), "+10", ChatFormatting.GREEN)) { _, _ -> system.settings.adjustGameTime(10); refresh() }
		button(7, named(Items.BARRIER, "disable", ChatFormatting.RED)) { _, _ -> system.settings.gameMinutes = 0; refresh() }
		broadcastChanges()
	}
}
