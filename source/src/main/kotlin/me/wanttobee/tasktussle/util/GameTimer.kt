package me.wanttobee.tasktussle.util

import kotlin.math.max
import me.wanttobee.tasktussle.base.games.ITTGame
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerBossEvent
import net.minecraft.world.BossEvent

class GameTimer(private val settings: TaskTussleSettings) {
	private var timerBar: ServerBossEvent? = null

	fun createTimerBar(server: MinecraftServer, active: ITTGame) {
		timerBar = ServerBossEvent(java.util.UUID.randomUUID(), Component.literal("TaskTussle"), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS).also { bar ->
			server.playerList.players.forEach(bar::addPlayer)
			updateTimerBar(active)
		}
	}

	fun updateTimerBar(active: ITTGame) {
		val total = max(1, settings.gameMinutes * 60)
		timerBar?.apply {
			progress = active.remainingSeconds.toFloat() / total
			name = Component.literal("Time left: ${active.remainingSeconds / 60}:${(active.remainingSeconds % 60).toString().padStart(2, '0')}")
			color = if (active.remainingSeconds > 120) BossEvent.BossBarColor.GREEN else BossEvent.BossBarColor.YELLOW
		}
	}

	fun clearTimerBar() {
		timerBar?.removeAllPlayers()
		timerBar = null
	}
}
