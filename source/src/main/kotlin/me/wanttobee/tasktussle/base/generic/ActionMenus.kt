package me.wanttobee.tasktussle.base.generic

import me.wanttobee.tasktussle.sounds.MenuClickSound
import me.wanttobee.tasktussle.sounds.TaskTussleSounds
import me.wanttobee.tasktussle.sounds.menuClickSound
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.DispenserMenu
import net.minecraft.world.inventory.HopperMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack

data class Click(val button: Int, val shift: Boolean) {
	val left: Boolean get() = button == 0
	val right: Boolean get() = button == 1
}

private fun runAction(menu: AbstractContainerMenu, player: Player, action: ((ServerPlayer, Click) -> Unit)?, click: Click) {
	val serverPlayer = player as? ServerPlayer ?: return
	action ?: return
	val before = menu.items.map(ItemStack::copy)
	action(serverPlayer, click)
	when (menuClickSound(serverPlayer.containerMenu !== menu, !ItemStack.listMatches(before, menu.items))) {
		MenuClickSound.BUTTON -> TaskTussleSounds.click(serverPlayer)
		MenuClickSound.PAGE -> TaskTussleSounds.clickPage(serverPlayer)
		null -> Unit
	}
}

abstract class ActionChestMenu(
	containerId: Int,
	protected val playerInventory: Inventory,
	rows: Int,
	protected val contents: SimpleContainer = SimpleContainer(rows * 9),
) : ChestMenu(menuType(rows), containerId, playerInventory, contents, rows) {
	open val customBackground: Boolean get() = false
	private val actions = mutableMapOf<Int, (ServerPlayer, Click) -> Unit>()
	private val menuSlots = rows * 9

	final override fun quickMoveStack(player: Player, slotId: Int): ItemStack = ItemStack.EMPTY
	protected fun clear() { contents.clearContent(); actions.clear() }
	protected fun clearActions() = actions.clear()
	protected fun display(slot: Int, stack: ItemStack) { actions.remove(slot); contents.setItem(slot, stack) }
	protected fun button(slot: Int, stack: ItemStack, action: (ServerPlayer, Click) -> Unit) { display(slot, stack); actions[slot] = action }
	protected fun fill(stack: () -> ItemStack = ::separator) { repeat(menuSlots) { display(it, stack()) } }

	override fun clicked(slotId: Int, button: Int, input: ContainerInput, player: Player) {
		if (slotId in 0 until menuSlots) {
			if (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE) runAction(this, player, actions[slotId], Click(button, input == ContainerInput.QUICK_MOVE))
			broadcastChanges()
			return
		}
		super.clicked(slotId, button, input, player)
	}
}

abstract class ActionHopperMenu(
	containerId: Int,
	playerInventory: Inventory,
	protected val contents: SimpleContainer = SimpleContainer(5),
) : HopperMenu(containerId, playerInventory, contents) {
	private val actions = mutableMapOf<Int, (ServerPlayer, Click) -> Unit>()
	final override fun quickMoveStack(player: Player, slotId: Int): ItemStack = ItemStack.EMPTY
	protected fun display(slot: Int, stack: ItemStack) { actions.remove(slot); contents.setItem(slot, stack) }
	protected fun button(slot: Int, stack: ItemStack, action: (ServerPlayer, Click) -> Unit) { display(slot, stack); actions[slot] = action }
	override fun clicked(slotId: Int, button: Int, input: ContainerInput, player: Player) {
		if (slotId in 0 until 5) {
			if (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE) runAction(this, player, actions[slotId], Click(button, input == ContainerInput.QUICK_MOVE))
			broadcastChanges()
			return
		}
		super.clicked(slotId, button, input, player)
	}
}

abstract class ActionDispenserMenu(
	containerId: Int,
	playerInventory: Inventory,
	protected val contents: SimpleContainer = SimpleContainer(9),
) : DispenserMenu(containerId, playerInventory, contents) {
	private val actions = mutableMapOf<Int, (ServerPlayer, Click) -> Unit>()
	final override fun quickMoveStack(player: Player, slotId: Int): ItemStack = ItemStack.EMPTY
	protected fun clear() { contents.clearContent(); actions.clear() }
	protected fun clearActions() = actions.clear()
	protected fun display(slot: Int, stack: ItemStack) { actions.remove(slot); contents.setItem(slot, stack) }
	protected fun button(slot: Int, stack: ItemStack, action: (ServerPlayer, Click) -> Unit) { display(slot, stack); actions[slot] = action }
	override fun clicked(slotId: Int, button: Int, input: ContainerInput, player: Player) {
		if (slotId in 0 until 9) {
			if (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE) runAction(this, player, actions[slotId], Click(button, input == ContainerInput.QUICK_MOVE))
			broadcastChanges()
			return
		}
		super.clicked(slotId, button, input, player)
	}
}

private fun menuType(rows: Int): MenuType<*> = when (rows) {
	1 -> MenuType.GENERIC_9x1; 2 -> MenuType.GENERIC_9x2; 3 -> MenuType.GENERIC_9x3
	4 -> MenuType.GENERIC_9x4; 5 -> MenuType.GENERIC_9x5; 6 -> MenuType.GENERIC_9x6
	else -> error("Unsupported row count: $rows")
}
