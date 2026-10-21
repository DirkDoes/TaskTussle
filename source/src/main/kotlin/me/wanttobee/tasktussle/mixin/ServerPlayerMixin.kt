package me.wanttobee.tasktussle.mixin

import com.llamalad7.mixinextras.sugar.Local
import me.wanttobee.tasktussle.base.generic.ActionChestMenu
import me.wanttobee.tasktussle.TaskTussleMod
import me.wanttobee.tasktussle.tasks.statusEffectTask.StatusEffectEvent
import me.wanttobee.tasktussle.items.ForcedItems
import me.wanttobee.tasktussle.resourcepack.TaskTussleResourcePackServer
import me.wanttobee.tasktussle.resourcepack.chestMenuTitle
import net.minecraft.server.level.ServerPlayer
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyArgs
import org.spongepowered.asm.mixin.injection.invoke.arg.Args
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ServerPlayer::class)
abstract class ServerPlayerMixin {
	@Inject(method = ["onEffectAdded"], at = [At("RETURN")])
	private fun statusEffectAdded(effect: MobEffectInstance, source: Entity?, callback: CallbackInfo) {
		TaskTussleMod.onTaskEvent(this as ServerPlayer, StatusEffectEvent(requireNotNull(BuiltInRegistries.MOB_EFFECT.getKey(effect.effect.value()))))
	}

	@Inject(method = ["onEffectUpdated"], at = [At("RETURN")])
	private fun statusEffectUpdated(effect: MobEffectInstance, changed: Boolean, source: Entity?, callback: CallbackInfo) {
		if (changed) TaskTussleMod.onTaskEvent(this as ServerPlayer, StatusEffectEvent(requireNotNull(BuiltInRegistries.MOB_EFFECT.getKey(effect.effect.value()))))
	}

	@ModifyArgs(method = ["openMenu"], at = At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundOpenScreenPacket;<init>(ILnet/minecraft/world/inventory/MenuType;Lnet/minecraft/network/chat/Component;)V"))
	private fun restoreChestBackground(args: Args, @Local menu: AbstractContainerMenu) {
		if (!TaskTussleResourcePackServer.active || (menu as? ActionChestMenu)?.customBackground == true) return
		args.set(2, chestMenuTitle(args.get(2), args.get(1)))
	}

	@Inject(method = ["drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;"], at = [At("HEAD")], cancellable = true)
	private fun preventForcedItemEntity(stack: ItemStack, randomDirection: Boolean, includeThrower: Boolean, callback: CallbackInfoReturnable<ItemEntity?>) {
		if (ForcedItems.isForcedItem(stack)) callback.returnValue = null
	}
}
