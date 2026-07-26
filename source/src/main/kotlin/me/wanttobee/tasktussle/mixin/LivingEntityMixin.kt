package me.wanttobee.tasktussle.mixin

import com.llamalad7.mixinextras.sugar.Local
import me.wanttobee.tasktussle.TaskTussleSounds
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(LivingEntity::class)
abstract class LivingEntityMixin {
	@Inject(
		method = ["checkTotemDeathProtection(Lnet/minecraft/world/damagesource/DamageSource;)Z"],
		at = [At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;broadcastEntityEvent(Lnet/minecraft/world/entity/Entity;B)V",
		)],
	)
	private fun replayRealTotemSound(
		source: DamageSource,
		callback: CallbackInfoReturnable<Boolean>,
		@Local(ordinal = 0) usedStack: ItemStack,
	) {
		if (usedStack.`is`(Items.TOTEM_OF_UNDYING)) TaskTussleSounds.play(this as LivingEntity)
	}
}