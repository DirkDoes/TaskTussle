package me.wanttobee.tasktussle.mixin

import me.wanttobee.tasktussle.TaskTussleService
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(Inventory::class)
abstract class InventoryMixin {
	@Shadow abstract fun getSelectedItem(): ItemStack

	@Inject(method = ["removeFromSelected(Z)Lnet/minecraft/world/item/ItemStack;"], at = [At("HEAD")], cancellable = true)
	private fun preventForcedItemRemoval(dropAll: Boolean, callback: CallbackInfoReturnable<ItemStack>) {
		if (TaskTussleService.isForcedItem(getSelectedItem())) callback.returnValue = ItemStack.EMPTY
	}
}