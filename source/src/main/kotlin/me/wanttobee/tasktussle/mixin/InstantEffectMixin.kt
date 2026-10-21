package me.wanttobee.tasktussle.mixin

import me.wanttobee.tasktussle.TaskTussleMod
import me.wanttobee.tasktussle.tasks.statusEffectTask.StatusEffectEvent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

// Healing/harming override the base implementation; saturation uses the base implementation.
@Mixin(targets = ["net.minecraft.world.effect.MobEffect", "net.minecraft.world.effect.HealOrHarmMobEffect"])
abstract class InstantEffectMixin {
	@Inject(method = ["applyInstantaneousEffect"], at = [At("RETURN")])
	private fun instantEffectApplied(level: ServerLevel, source: Entity?, indirectSource: Entity?, target: LivingEntity, amplifier: Int, potency: Double, callback: CallbackInfo) {
		val effect = this as MobEffect
		if (target is ServerPlayer && effect.isInstantaneous) {
			TaskTussleMod.onTaskEvent(target, StatusEffectEvent(requireNotNull(BuiltInRegistries.MOB_EFFECT.getKey(effect))))
		}
	}
}
