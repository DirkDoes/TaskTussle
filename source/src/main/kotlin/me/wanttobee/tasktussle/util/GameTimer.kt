package me.wanttobee.tasktussle.util

import kotlin.math.max
import me.wanttobee.tasktussle.base.games.ITTGame
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerBossEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.BossEvent

class GameTimer {
	private var timerBar: ServerBossEvent? = null
	private var totalSeconds = 1

	fun createTimerBar(server: MinecraftServer, active: ITTGame) {
		clearTimerBar()
		totalSeconds = max(1, active.remainingSeconds)
		timerBar = ServerBossEvent(java.util.UUID.randomUUID(), Component.literal("TaskTussle"), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS).also { bar ->
			server.playerList.players.forEach(bar::addPlayer)
		}
		updateTimerBar(active)
	}

	fun updateTimerBar(active: ITTGame) {
		timerBar?.apply {
			progress = (active.remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
			name = Component.literal("Time left: ${active.remainingSeconds / 60}:${(active.remainingSeconds % 60).toString().padStart(2, '0')}")
			color = if (active.remainingSeconds > 120) BossEvent.BossBarColor.GREEN else BossEvent.BossBarColor.YELLOW
		}
	}

	fun addPlayer(player: ServerPlayer) { timerBar?.addPlayer(player) }

	fun clearTimerBar() {
		timerBar?.removeAllPlayers()
		timerBar = null
	}
}
