package me.wanttobee.tasktussle

import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ItemParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.Items

object TaskTussleSounds {
	private val TOTEM_USE = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("tasktussle", "item.totem.use"))
	private val REWARD_MID = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("tasktussle", "reward_mid"))
	private val REWARD_LARGE = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("tasktussle", "reward_large"))
	private val CLICK_BUTTON = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("tasktussle", "click_button"))
	private val CLICK_PAGE_BUTTON = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("tasktussle", "click_page_button"))
	private val LIME_PARTICLE = itemParticle("totem_lime_particle")
	private val YELLOW_PARTICLE = itemParticle("totem_yellow_particle")
	private val particleEmitters = mutableMapOf<LivingEntity, Int>()

	fun play(entity: LivingEntity) {
		entity.level().playSound(null, entity.x, entity.y, entity.z, TOTEM_USE, entity.soundSource, 1f, 1f)
		particleEmitters[entity] = 30
	}

	fun rewardMid(player: ServerPlayer) = send(player, REWARD_MID)
	fun rewardLarge(player: ServerPlayer) = send(player, REWARD_LARGE)
	fun click(player: ServerPlayer) = send(player, CLICK_BUTTON)
	fun clickPage(player: ServerPlayer) = send(player, CLICK_PAGE_BUTTON)

	private fun send(player: ServerPlayer, sound: SoundEvent) {
		player.connection.send(ClientboundSoundPacket(
			Holder.direct(sound), SoundSource.MASTER, player.x, player.y, player.z, 1f, 1f, player.random.nextLong(),
		))
	}
	fun tick(server: MinecraftServer) {
		val iterator = particleEmitters.iterator()
		while (iterator.hasNext()) {
			val (entity, remaining) = iterator.next()
			if (entity.isRemoved || entity.level().server !== server || remaining <= 1) iterator.remove()
			if (!entity.isRemoved && entity.level().server === server) {
				emitParticles(entity)
				if (remaining > 1) particleEmitters[entity] = remaining - 1
			}
		}
	}

	private fun emitParticles(entity: LivingEntity) {
		val level = entity.level() as? ServerLevel ?: return
		repeat(16) {
			val x = entity.random.nextDouble() * 2 - 1
			val y = entity.random.nextDouble() * 2 - 1
			val z = entity.random.nextDouble() * 2 - 1
			if (!isTotemParticleOffset(x, y, z)) return@repeat
			level.sendParticles(
				if (entity.random.nextInt(4) == 0) YELLOW_PARTICLE else LIME_PARTICLE,
				entity.getX(x / 4), entity.getY(0.5 + y / 4), entity.getZ(z / 4),
				0, x, y + 0.2, z, 1.0,
			)
		}
	}

	private fun itemParticle(model: String) = ItemParticleOption(
		ParticleTypes.ITEM,
		ItemStackTemplate.fromNonEmptyStack(ItemStack(Items.PAPER).apply {
			set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("tasktussle", model))
		}),
	)
}

internal fun isTotemParticleOffset(x: Double, y: Double, z: Double) = x * x + y * y + z * z <= 1