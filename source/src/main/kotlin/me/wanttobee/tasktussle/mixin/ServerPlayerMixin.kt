package me.wanttobee.tasktussle.mixin

import me.wanttobee.tasktussle.TaskTussleService
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(ServerPlayer::class)
abstract class ServerPlayerMixin {
	@Inject(method = ["drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;"], at = [At("HEAD")], cancellable = true)
	private fun preventForcedItemEntity(stack: ItemStack, randomDirection: Boolean, includeThrower: Boolean, callback: CallbackInfoReturnable<ItemEntity?>) {
		if (TaskTussleService.isForcedItem(stack)) callback.returnValue = null
	}
}