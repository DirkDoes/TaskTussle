package me.wanttobee.tasktussle.world

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import me.wanttobee.tasktussle.items.ForcedItems
import me.wanttobee.tasktussle.util.message
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Relative
import net.minecraft.world.clock.WorldClocks
import net.minecraft.world.level.gamerules.GameRules
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

class LobbyService {
	private var level: ServerLevel? = null
	private var center: BlockPos? = null
	private var oldBorder: Triple<Double, Double, Double>? = null
	private val replacedBlocks = mutableMapOf<BlockPos, BlockState>()
	private val reachedGoal = mutableSetOf<java.util.UUID>()
	private var parkourActive = false

	val active: Boolean get() = level != null

	fun start(invoker: ServerPlayer) {
		if (active) clear(invoker.level().server)
		val targetLevel = invoker.level()
		val server = targetLevel.server
		val clock = targetLevel.dimensionType().defaultClock().orElseGet { server.registryAccess().getOrThrow(WorldClocks.OVERWORLD) }
		server.clockManager().setTotalTicks(clock, 0)
		server.gameRules.set(GameRules.ADVANCE_TIME, false, server)
		val target = invoker.blockPosition()
		val border = targetLevel.worldBorder
		level = targetLevel
		center = target
		oldBorder = Triple(border.centerX, border.centerZ, border.size)
		border.setCenter(target.x + 0.5, target.z + 0.5)
		border.size = 15.0
		invoker.level().server.playerList.players.forEach(::apply)
		invoker.setGameMode(GameType.CREATIVE)
		invoker.sendSystemMessage(message("Lobby enabled.", ChatFormatting.GREEN))
	}

	fun clear(server: MinecraftServer) {
		val targetLevel = level ?: return
		server.gameRules.set(GameRules.ADVANCE_TIME, true, server)
		removeParkour()
		oldBorder?.let { (x, z, size) ->
			targetLevel.worldBorder.setCenter(x, z)
			targetLevel.worldBorder.size = size
		}
		server.playerList.players.forEach { player ->
			player.removeAllEffects()
			player.setGameMode(GameType.SURVIVAL)
			ForcedItems.clearNonForcedItems(player)
		}
		level = null
		center = null
		oldBorder = null
	}

	fun createParkour(invoker: ServerPlayer): Boolean {
		val targetLevel = level ?: return false
		val target = center ?: return false
		removeParkour()
		fun replace(pos: BlockPos, state: BlockState) {
			replacedBlocks.putIfAbsent(pos.immutable(), targetLevel.getBlockState(pos))
			targetLevel.setBlock(pos, state, 3)
		}
		repeat(50) {
			replace(target.offset(Random.nextInt(-6, 6), Random.nextInt(0, 5), Random.nextInt(-6, 6)), Blocks.STAINED_GLASS.lime().defaultBlockState())
		}
		repeat(500) {
			replace(target.offset(Random.nextInt(-8, 8), Random.nextInt(5, 35), Random.nextInt(-8, 8)), Blocks.STAINED_GLASS.cyan().defaultBlockState())
		}
		val ringY = target.y + 35
		for (angle in 0 until 360 step 10) {
			val radians = Math.toRadians(angle.toDouble())
			replace(BlockPos(target.x + (7 * cos(radians)).toInt(), ringY, target.z + (7 * sin(radians)).toInt()), Blocks.STAINED_GLASS.black().defaultBlockState())
		}
		for (x in target.x - 8..target.x + 8) for (z in target.z - 8..target.z + 8) {
			val distance = (x - target.x) * (x - target.x) + (z - target.z) * (z - target.z)
			if (distance > 45) replace(BlockPos(x, ringY, z), Blocks.STAINED_GLASS.black().defaultBlockState())
		}
		parkourActive = true
		invoker.sendSystemMessage(message("Parkour created.", ChatFormatting.GREEN))
		return true
	}

	fun removeParkour(): Boolean {
		val targetLevel = level ?: return false
		if (!parkourActive) return false
		replacedBlocks.forEach(targetLevel::setBlockAndUpdate)
		replacedBlocks.clear()
		reachedGoal.clear()
		parkourActive = false
		return true
	}

	fun tick(server: MinecraftServer) {
		val targetLevel = level ?: return
		if (!parkourActive) return
		server.playerList.players.filter { it.level() == targetLevel }.forEach { player ->
			if (targetLevel.getBlockState(player.blockPosition().below()).`is`(Blocks.STAINED_GLASS.black()) && reachedGoal.add(player.uuid)) {
				server.playerList.broadcastSystemMessage(message("${player.gameProfile.name} reached the top!", ChatFormatting.GOLD), false)
			}
		}
	}

	fun apply(player: ServerPlayer) {
		val targetLevel = level ?: return
		val target = center ?: return
		player.addEffect(MobEffectInstance(MobEffects.SATURATION, MobEffectInstance.INFINITE_DURATION, 10, true, false, false))
		player.setGameMode(GameType.ADVENTURE)
		ForcedItems.clearNonForcedItems(player)
		player.teleportTo(
			targetLevel,
			target.x + 0.5 + Random.nextDouble(-1.0, 1.0), target.y.toDouble(), target.z + 0.5 + Random.nextDouble(-1.0, 1.0),
			emptySet<Relative>(), player.yRot, player.xRot, false,
		)
	}

	fun remove(player: ServerPlayer) {
		player.removeAllEffects()
		player.setGameMode(GameType.SURVIVAL)
		ForcedItems.clearNonForcedItems(player)
	}
}
