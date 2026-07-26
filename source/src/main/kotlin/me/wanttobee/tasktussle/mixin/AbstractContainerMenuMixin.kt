package me.wanttobee.tasktussle.mixin

import me.wanttobee.tasktussle.TaskTussleService
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(AbstractContainerMenu::class)
abstract class AbstractContainerMenuMixin {
	@Shadow abstract fun isValidSlotIndex(index: Int): Boolean
	@Shadow abstract fun getSlot(index: Int): Slot
	@Shadow abstract fun getCarried(): ItemStack

	@Inject(method = ["clicked(IILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V"], at = [At("HEAD")], cancellable = true)
	private fun preventForcedItemMove(slotId: Int, button: Int, input: ContainerInput, player: Player, callback: CallbackInfo) {
		if (
			TaskTussleService.isForcedItem(getCarried()) ||
			slotId >= 0 && isValidSlotIndex(slotId) && TaskTussleService.isForcedItem(getSlot(slotId).item) ||
			input == ContainerInput.SWAP && button in 0 until player.inventory.containerSize &&
				TaskTussleService.isForcedItem(player.inventory.getItem(button))
		) {
			callback.cancel()
			if (player is ServerPlayer) player.containerMenu.broadcastFullState()
		}
	}
}