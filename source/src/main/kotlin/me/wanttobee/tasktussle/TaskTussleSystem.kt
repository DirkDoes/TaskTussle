package me.wanttobee.tasktussle

import me.wanttobee.tasktussle.base.games.ITTCardGui
import me.wanttobee.tasktussle.base.games.ITTGame
import me.wanttobee.tasktussle.base.games.ITTGameManager
import me.wanttobee.tasktussle.base.games.ITTGameTeam
import me.wanttobee.tasktussle.base.games.onlinePlayers
import me.wanttobee.tasktussle.base.generic.IManager
import me.wanttobee.tasktussle.base.generic.TaskMenus
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.ITask
import me.wanttobee.tasktussle.base.tasks.TaskCatalog
import me.wanttobee.tasktussle.base.tasks.TaskEvent
import me.wanttobee.tasktussle.base.tasks.TaskCompletion
import me.wanttobee.tasktussle.base.tasks.TaskState
import me.wanttobee.tasktussle.base.tasks.TaskFactory
import me.wanttobee.tasktussle.base.tasks.deliveryTask
import me.wanttobee.tasktussle.items.ForcedItems
import me.wanttobee.tasktussle.pvp.PvpSystem
import me.wanttobee.tasktussle.sounds.TaskTussleSounds
import me.wanttobee.tasktussle.tasks.statusEffectTask.StatusEffectEvent
import me.wanttobee.tasktussle.teams.Team
import me.wanttobee.tasktussle.teams.TeamMakerMenu
import me.wanttobee.tasktussle.teams.TeamSystem
import me.wanttobee.tasktussle.teams.teamColor
import me.wanttobee.tasktussle.util.GameTimer
import me.wanttobee.tasktussle.util.message
import me.wanttobee.tasktussle.util.tagged
import me.wanttobee.tasktussle.util.takeDue
import me.wanttobee.tasktussle.util.titleFrames
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.DeathProtection
import net.minecraft.world.item.component.ResolvableProfile

class TaskTussleSystem(
	val gameManagers: List<ITTGameManager> = TaskTussleGrouper.gameManagers,
	val taskFactory: TaskFactory = TaskFactory(TaskTussleGrouper.taskManagers),
) {
	val settings = TaskTussleSettings()
	val teams = TeamSystem(this)
	val menus = TaskMenus(this)
	val items = ForcedItems(this)
	val pvp = PvpSystem(this)
	private val timer = GameTimer()
	var logEnabled = true
	private var game: ITTGame? = null
	internal var server: MinecraftServer? = null
		private set
	private val delayedActions = mutableListOf<Pair<Int, () -> Unit>>()

	fun manager(key: String): IManager = (taskFactory.managers + gameManagers).first { it.key == key }

	fun prepare(server: MinecraftServer) {
		this.server = server
		TaskCatalog.prepare(server, taskFactory.managers.mapNotNull { it.files })
		reloadPools()
	}

	fun reloadPools() {
		taskFactory.reload(requireNotNull(server) { "Server is not ready yet." }, settings)
	}

	fun start(server: MinecraftServer, commander: ServerPlayer?, teamCount: Int, manager: ITTGameManager = gameManagers.first()): Boolean {
		if (game != null || teams.selection?.startsGame == true) return fail(commander, "there is already a game running")
		if (teamCount !in manager.teamRange) return fail(commander, "team amount invalid, ${manager.name} can have from ${manager.teamRange.first} up to ${manager.teamRange.last} teams total")
		if (server.playerList.players.isEmpty()) return fail(commander, "there are no players online")
		runCatching(::reloadPools).getOrElse { return fail(commander, it.message ?: "task generation failed") }

		if (settings.chooseTeams && teamCount > 1) {
			val starter = commander ?: return fail(null, "a player must start team selection")
			this.teams.beginTeamSelection(starter, teamCount, true, manager)
			return true
		}
		return startWithTeams(server, commander, this.teams.randomTeams(server.playerList.players, teamCount), manager)
	}

	internal fun startWithTeams(server: MinecraftServer, commander: ServerPlayer?, teams: List<Team>, manager: ITTGameManager): Boolean {
		if (teams.isEmpty()) return fail(commander, "Cant start a game without teams")
		val created = manager.create(teams, settings, taskFactory) ?: return fail(commander, "Cant start a game, task generation failed")
		game = created
		server.playerList.players.forEach { player ->
			items.syncPvpToggle(player)
			if (player.containerMenu is TeamMakerMenu) player.closeContainer()
		}
		server.playerList.players.forEach(items::removeSettingsTool)
		server.playerList.players.forEach(items::removeTaskBrowser)
		items.deliverySpotLearned.clear()
		this.teams.listedTeams = teams
		taskFactory.prepare(server, created.teams, settings)
		server.playerList.players.filter { player -> teams.any { player.uuid in it.members } }.forEach { player ->
			items.giveCard(player)
			if (settings.handInItems) items.giveDeliverySpot(player)
			player.sendSystemMessage(message("Click with the Task Tussle Card in your hand to see your teams progress.", ChatFormatting.GREEN))
			taskFactory.managers.forEach { taskManager ->
				taskManager.explanation(settings)?.let { player.sendSystemMessage(message(it, ChatFormatting.GRAY)) }
			}
			player.sendSystemMessage(message("The goal is set to: ${manager.goal(settings)}", ChatFormatting.GREEN))
		}
		if (created.remainingSeconds > 0) timer.createTimerBar(server, created)
		log("created ${teams.size} ${manager.name} teams")
		server.playerList.players.forEach(::checkCurrentEffects)
		return true
	}

	fun tick(server: MinecraftServer) {
		takeDue(server.tickCount, delayedActions).forEach { it() }
		if (server.tickCount % 20 == 0) {
			items.restoreForcedItems(server)
			pvp.refreshNames(server)
		}
		val active = game ?: return
		if (active.finished || active.paused) return
		if (server.tickCount % 5 == 0) checkTasks(server, active)
		if (active.finished || active.paused) return
		if (active.remainingSeconds > 0 && server.tickCount % 20 == 0) {
			active.remainingSeconds--
			timer.updateTimerBar(active)
			if (active.remainingSeconds == 0) timeUp(server)
		}
	}

	fun stop(server: MinecraftServer, commander: ServerPlayer?): Boolean {
		if (game == null) return fail(commander, "trying to stop a game that doesn't exists")
		game = null
		delayedActions.clear()
		items.deliverySpotLearned.clear()
		teams.listedTeams = emptyList()
		timer.clearTimerBar()
		server.playerList.players.forEach { player ->
			if (player.containerMenu is ITTCardGui) player.closeContainer()
		}
		server.playerList.players.forEach(items::removeGameItems)
		server.playerList.players.forEach(items::giveSettingsTool)
		server.playerList.players.forEach(items::giveTaskBrowser)
		server.playerList.players.forEach(items::syncPvpToggle)
		commander?.sendSystemMessage(tagged("stopped the running game", ChatFormatting.GREEN))
		return true
	}

	fun draw(server: MinecraftServer): Boolean = finish(server, null)

	fun timeUp(server: MinecraftServer): Boolean {
		val active = game ?: return false
		return finish(server, active.manager.timeUpWinner(active, settings))
	}

	fun openCard(player: ServerPlayer, requestedTeam: Int? = null): Boolean {
		val active = game ?: return fail(player, "no game is active")
		val own = active.teams.find { player.uuid in it.members }
		val target = (if (active.sharedTasks) own else requestedTeam?.let { index -> active.teams.find { it.index == index } } ?: own)
			?: return fail(player, "no game to open")
		if (!active.sharedTasks && target != own && !active.finished && settings.cardVisibility != "visible") return false
		menus.openMenu(player, active.manager.cardMenu(this, active, target, own))
		return true
	}

	fun clickTask(player: ServerPlayer, teamIndex: Int, taskIndex: Int, rightClick: Boolean): Boolean {
		val active = game ?: return false
		val team = active.teams.find { it.index == teamIndex } ?: return false
		val task = team.tasks.getOrNull(taskIndex) ?: return false
		if (player.uuid !in team.members || task.progress.state(team.index, active.completionLimit) != TaskState.ACTIVE || active.paused || active.finished) return false
		if (rightClick) return false
		return task.click(player, settings) && complete(player.level().server, active, team, task, player)
	}

	fun clickDeliverySpot(player: ServerPlayer, stack: ItemStack) {
		val active = game ?: return
		if (!settings.handInItems) return
		if (items.deliverySpotLearned.add(player.uuid)) items.giveDeliverySpot(player)
		if (stack.isEmpty || active.paused || active.finished) return
		val team = active.teams.find { player.uuid in it.members } ?: return
		val task = deliveryTask(team.tasks, stack.item, team.index, active.completionLimit) ?: return
		stack.shrink(1)
		player.inventory.setChanged()
		complete(player.level().server, active, team, task, player)
	}

	fun onTaskEvent(player: ServerPlayer, event: TaskEvent) {
		val active = game ?: return
		if (active.paused || active.finished) return
		val team = active.teams.find { player.uuid in it.members } ?: return
		for (task in team.tasks) {
			if (active.paused || active.finished) break
			if (task.progress.state(team.index, active.completionLimit) == TaskState.ACTIVE && task.onEvent(event, settings)) {
				complete(player.level().server, active, team, task, player)
			}
		}
	}

	fun debugStatus(player: ServerPlayer) {
		player.sendSystemMessage(tagged("active taskTussle game:", ChatFormatting.YELLOW))
		val active = game
		if (active == null) player.sendSystemMessage(message("there is no game active", ChatFormatting.GREEN))
		else {
			val openTasks = active.teams.flatMap { it.tasks }.distinct().count { !it.progress.closed(active.completionLimit) }
			player.sendSystemMessage(message("active: ${active.manager.name} (${active.teams.size} teams, $openTasks active tasks)", ChatFormatting.YELLOW))
		}
	}

	fun onJoin(player: ServerPlayer) {
		items.syncPvpToggle(player)
		timer.addPlayer(player)
		if (game == null && teams.selection == null) items.giveSettingsTool(player)
		if (game == null && teams.selection == null) items.giveTaskBrowser(player)
		if (game?.teams?.any { player.uuid in it.members } == true) {
			items.giveCard(player)
			if (settings.handInItems) items.giveDeliverySpot(player)
		}
		if (teams.selection != null) items.giveTeamNavigator(player)
		if (teams.selection?.starter == player.uuid) items.giveTeamFinisher(player)
		checkCurrentEffects(player)
	}

	private fun checkCurrentEffects(player: ServerPlayer) {
		for (effect in player.activeEffects.toList()) {
			onTaskEvent(player, StatusEffectEvent(requireNotNull(BuiltInRegistries.MOB_EFFECT.getKey(effect.effect.value()))))
		}
	}

	fun shutdown(server: MinecraftServer) {
		pvp.clearNames(server)
		timer.clearTimerBar()
		delayedActions.clear()
		teams.selection = null
	}

	fun currentGame(): ITTGame? = game

	fun refreshCards() = server?.let(::refreshOpenCards)

	private fun checkTasks(server: MinecraftServer, active: ITTGame) {
		for (team in active.teams) for (player in team.onlinePlayers(server.playerList.players)) for (task in team.tasks.filter { it.progress.state(team.index, active.completionLimit) == TaskState.ACTIVE }) {
			if (active.finished || active.paused) return
			if (task.poll(player, settings)) complete(server, active, team, task, player)
		}
	}

	private fun complete(server: MinecraftServer, active: ITTGame, team: ITTGameTeam, task: ITask, player: ServerPlayer): Boolean {
		if (active.paused || active.finished || player.uuid !in team.members) return false
		if (!task.progress.complete(TaskCompletion(team.index, player.uuid, player.gameProfile.name, server.tickCount), active.completionLimit)) return false
		server.playerList.players.forEach { recipient ->
			val sameTeam = recipient.uuid in team.members
			if (active.sharedTasks || settings.cardVisibility == "visible" || sameTeam) recipient.sendSystemMessage(completionMessage(player.gameProfile.name, team.color, task))
			else if (settings.cardVisibility != "hidden") recipient.sendSystemMessage(completionMessage(player.gameProfile.name, team.color, null))
		}
		val ended = active.manager.isFinished(active, team, settings)
		team.onlinePlayers(server.playerList.players).forEach { member ->
			if (!ended) TaskTussleSounds.rewardMid(member)
		}
		refreshOpenCards(server)
		if (ended) finish(server, active.manager.completionWinner(active, team, settings))
		after(server, TASK_ITEM_DISPLAY_DELAY_TICKS) {
			server.playerList.getPlayer(player.uuid)?.let { showCompletionItem(it, task) }
		}
		return true
	}

	private fun finish(server: MinecraftServer, winner: ITTGameTeam?): Boolean {
		val active = game ?: return false
		if (active.finished) return false
		active.finished = true
		active.paused = true
		delayedActions.clear()
		timer.clearTimerBar()
		server.playerList.players.filter { player -> active.teams.any { player.uuid in it.members } }.forEach { player ->
			player.closeContainer()
			TaskTussleSounds.rewardLarge(player)
			val ownTeam = active.teams.find { player.uuid in it.members }
			if (winner == null) {
				showResultTitle(server, player, "DRAW", ChatFormatting.YELLOW)
				player.sendSystemMessage(message("The game drew", ChatFormatting.YELLOW))
			} else {
				if (ownTeam === winner) {
					showResultTitle(server, player, "VICTORY", ChatFormatting.GOLD, Component.literal(winner.displayName()).withStyle(winner.color))
				} else {
					showResultTitle(server, player, "${winner.displayName()} won", winner.color, Component.literal("You lost").withStyle(ChatFormatting.RED))
				}
				player.sendSystemMessage(message("${winner.displayName()} Won the game", winner.color))
			}
		}
		after(server, GAME_RESULT_DELAY_TICKS) {
			if (game !== active) return@after
			server.playerList.players.filter { player -> active.teams.any { player.uuid in it.members } }.forEach { player ->
				(if (winner == null) active.teams.find { player.uuid in it.members } else winner)?.let { openCard(player, it.index) }
			}
		}
		return true
	}

	internal fun after(server: MinecraftServer, ticks: Int, action: () -> Unit) {
		delayedActions += server.tickCount + ticks to action
	}

	private fun showResultTitle(server: MinecraftServer, player: ServerPlayer, text: String, color: ChatFormatting, subtitle: Component? = null) {
		subtitle?.let { player.connection.send(ClientboundSetSubtitleTextPacket(it)) }
		titleFrames(text, minOf(TITLE_REVEAL_TICKS, GAME_RESULT_DELAY_TICKS)).forEach { (delay, frame) ->
			val send = {
				server.playerList.getPlayer(player.uuid)?.connection?.apply {
					send(ClientboundSetTitlesAnimationPacket(0, GAME_RESULT_DELAY_TICKS - delay, 0))
					send(ClientboundSetTitleTextPacket(Component.literal(frame).withStyle(color, ChatFormatting.BOLD)))
				}
				Unit
			}
			if (delay == 0) send() else after(server, delay, send)
		}
	}

	private fun refreshOpenCards(server: MinecraftServer) = server.playerList.players.forEach { player ->
		(player.containerMenu as? ITTCardGui)?.refresh()
	}

	internal fun fail(player: ServerPlayer?, text: String): Boolean {
		player?.sendSystemMessage(tagged(text, ChatFormatting.RED))
		return false
	}

	private fun log(text: String) { if (logEnabled) println("[Task Tussle] $text") }

	private fun showCompletionItem(player: ServerPlayer, task: ITask) {
		displayItem(player, task.icon(server, settings).apply {
			set(DataComponents.CUSTOM_NAME, message(task.name(server), ChatFormatting.WHITE))
		})
	}

	fun displayHeldItem(player: ServerPlayer) {
		val display = if (player.mainHandItem.isEmpty) ItemStack(Items.PLAYER_HEAD).apply {
			set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.gameProfile))
		} else player.mainHandItem.copy()
		displayItem(player, display)
	}

	private fun displayItem(player: ServerPlayer, display: ItemStack) {
		display.set(DataComponents.DEATH_PROTECTION, DeathProtection.TOTEM_OF_UNDYING)
		val menu = player.inventoryMenu
		player.connection.send(ClientboundContainerSetSlotPacket(menu.containerId, menu.stateId, InventoryMenu.SHIELD_SLOT, display))
		player.connection.send(ClientboundEntityEventPacket(player, 35))
		player.connection.send(ClientboundContainerSetSlotPacket(menu.containerId, menu.stateId, InventoryMenu.SHIELD_SLOT, player.offhandItem))
	}

	private fun completionMessage(playerName: String, teamColor: ChatFormatting, task: ITask?): Component = Component.literal(playerName).withStyle(teamColor)
		.append(Component.literal(if (task == null) " got a task" else " got a ${task.typeName.lowercase()} ").withStyle(ChatFormatting.WHITE))
		.apply { if (task != null) append(Component.literal("(${task.name(server)})").withStyle(ChatFormatting.GRAY)) }

	companion object {
		private const val TASK_ITEM_DISPLAY_DELAY_TICKS = 8
		private const val TITLE_REVEAL_TICKS = 30
		private const val GAME_RESULT_DELAY_TICKS = 122
	}
}
