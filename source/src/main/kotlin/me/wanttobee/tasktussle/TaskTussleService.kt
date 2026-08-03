package me.wanttobee.tasktussle

import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerBossEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions
import net.minecraft.world.BossEvent
import net.minecraft.world.MenuProvider
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.SmithingTemplateItem
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.CustomModelData
import net.minecraft.world.item.component.DeathProtection
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.world.level.gamerules.GameRules
import kotlin.math.max

class TaskTussleService {
	val settings = TaskTussleSettings()
	var logEnabled = true
	private var blockPools: TaskPools? = null
	private var advancementPools: TaskPools? = null
	private var game: TussleGame? = null
	private var selection: TeamSelection? = null
	private var listedTeams: List<TussleTeam> = emptyList()
	private var timerBar: ServerBossEvent? = null
	private var server: MinecraftServer? = null
	private val delayedActions = mutableListOf<Pair<Int, () -> Unit>>()
	private val deliverySpotLearned = mutableSetOf<java.util.UUID>()

	fun prepare(server: MinecraftServer) {
		this.server = server
		TaskCatalog.prepare(server)
		reloadPools()
	}

	fun reloadPools() {
		val server = requireNotNull(server) { "Server is not ready yet." }
		blockPools = TaskCatalog.blocks(settings.blocksFile)
		advancementPools = TaskCatalog.advancements(server, settings.advancementsFile)
	}

	fun start(server: MinecraftServer, commander: ServerPlayer?, teamCount: Int): Boolean {
		if (game != null || selection?.startsGame == true) return fail(commander, "there is already a game running")
		if (teamCount !in 1..15) return fail(commander, "team amount invalid, Bingo can have from 1 up to 15 teams total")
		if (server.playerList.players.isEmpty()) return fail(commander, "there are no players online")
		runCatching(::reloadPools).getOrElse { return fail(commander, it.message ?: "task generation failed") }

		if (settings.chooseTeams && teamCount > 1) {
			val starter = commander ?: return fail(null, "a player must start team selection")
			beginTeamSelection(starter, teamCount, true)
			return true
		}
		return startWithTeams(server, commander, randomTeams(server.playerList.players, teamCount))
	}

	private fun startWithTeams(server: MinecraftServer, commander: ServerPlayer?, teams: List<TussleTeam>): Boolean {
		if (teams.isEmpty()) return fail(commander, "Cant start a game without teams")
		val blocks = blockPools ?: return fail(commander, "Cant start a game, task generation failed")
		val advancements = advancementPools ?: return fail(commander, "Cant start a game, task generation failed")
		val ratios = Triple(settings.easyRatio, settings.mediumRatio, settings.hardRatio)
		val weights = Triple(settings.blockWeight, settings.advancementWeight, settings.clickWeight)
		val mutual = TaskGenerator.generate(settings.mutualTasks, blocks, advancements, ratios, weights)
			?: return fail(commander, "Cant start a game, task generation failed")

		for (team in teams) {
			team.succeedTokens = settings.succeedTokens
			team.succeedTokensMax = settings.succeedTokens
			val templates = when {
				settings.mutualTasks == 25 -> mutual
				settings.mutualTasks < 25 -> {
					val unique = TaskGenerator.generate(
						25 - settings.mutualTasks, blocks, advancements, ratios, weights,
						mutual.map { it.kind to it.id }.toSet(),
					) ?: return fail(commander, "Cant start a game, task generation failed")
					mutual + unique
				}
				else -> mutual.shuffled().take(25)
			}
			team.tasks += templates.shuffled().map(::BingoTask)
		}

		val created = TussleGame(teams, settings.gameMinutes * 60)
		game = created
		server.playerList.players.forEach(::removeSettingsTool)
		server.playerList.players.forEach(::removeTaskBrowser)
		deliverySpotLearned.clear()
		listedTeams = teams
		resetAllAdvancements(server, created)
		server.playerList.players.filter { player -> teams.any { player.uuid in it.members } }.forEach { player ->
			giveCard(player)
			if (settings.handInItems) giveDeliverySpot(player)
			player.sendSystemMessage(message("Click with the Task Tussle Card in your hand to see your teams progress.", ChatFormatting.GREEN))
			if (settings.blockWeight > 0) player.sendSystemMessage(message(
				if (settings.handInItems) "(Obtain Task) Carry the shown block and click the delivery spot."
				else "(Obtain Task) Obtain the shown block.",
				ChatFormatting.GRAY,
			))
			player.sendSystemMessage(message("The goal is set to: ${settings.winningCondition}", ChatFormatting.GREEN))
		}
		if (settings.gameMinutes > 0) createTimerBar(server, created)
		log("created ${teams.size} Bingo teams")
		return true
	}

	fun tick(server: MinecraftServer) {
		takeDue(server.tickCount, delayedActions).forEach { it() }
		if (server.tickCount % 20 == 0) restoreForcedItems(server)
		val active = game ?: return
		if (active.finished || active.paused) return
		if (server.tickCount % 5 == 0) checkTasks(server, active)
		if (active.remainingSeconds > 0 && server.tickCount % 20 == 0) {
			active.remainingSeconds--
			updateTimerBar(active)
			if (active.remainingSeconds == 0) timeUp(server)
		}
	}

	fun stop(server: MinecraftServer, commander: ServerPlayer?): Boolean {
		if (game == null) return fail(commander, "trying to stop a game that doesn't exists")
		game = null
		delayedActions.clear()
		deliverySpotLearned.clear()
		listedTeams = emptyList()
		clearTimerBar()
		server.playerList.players.forEach(::removeGameItems)
		server.playerList.players.forEach(::giveSettingsTool)
		server.playerList.players.forEach(::giveTaskBrowser)
		commander?.sendSystemMessage(tagged("stopped the running game", ChatFormatting.GREEN))
		return true
	}

	fun draw(server: MinecraftServer): Boolean = finish(server, null)

	fun timeUp(server: MinecraftServer): Boolean {
		val active = game ?: return false
		if (settings.overtime == "most tasks wins") {
			val best = active.teams.maxOf(TussleTeam::completed)
			val leaders = active.teams.filter { it.completed == best }
			if (leaders.size == 1) return finish(server, leaders.single())
		}
		return finish(server, null)
	}

	fun openCard(player: ServerPlayer, requestedTeam: Int? = null): Boolean {
		val active = game ?: return fail(player, "no game is active")
		val own = active.teams.find { player.uuid in it.members }
		val target = requestedTeam?.let { index -> active.teams.find { it.index == index } } ?: own
			?: return fail(player, "no game to open")
		if (target != own && !active.finished && settings.cardVisibility != "visible") return false
		openMenu(player, SimpleMenuProvider(
			{ id, inventory, _ -> TaskTussleMenus.CardMenu(id, inventory, this, active, target, own) },
			Component.literal("Bingo"),
		))
		return true
	}

	fun openSettings(player: ServerPlayer) = openMenu(player, SimpleMenuProvider(
		{ id, inventory, _ -> TaskTussleMenus.SettingsMenu(id, inventory, this) }, Component.literal("Task Tussle Settings"),
	))

	fun openReadOnlySettings(player: ServerPlayer): Boolean {
		if (game != null || selection != null) return false
		openMenu(player, SimpleMenuProvider(
			{ id, inventory, _ -> TaskTussleMenus.SettingsMenu(id, inventory, this, true) }, Component.literal("Task Tussle Settings"),
		))
		return true
	}

	fun openManagerSettings(player: ServerPlayer, manager: String) = openMenu(player, SimpleMenuProvider(
		{ id, inventory, _ -> TaskTussleMenus.ManagerSettingsMenu(id, inventory, this, manager) },
		Component.literal("${manager.replaceFirstChar(Char::uppercase)} Settings"),
	))

	fun openTimeSettings(player: ServerPlayer) = openMenu(player, SimpleMenuProvider(
		{ id, inventory, _ -> TaskTussleMenus.TimeSettingsMenu(id, inventory, this) }, Component.literal("Time Settings"),
	))

	fun openWinConditions(player: ServerPlayer) = openMenu(player, SimpleMenuProvider(
		{ id, inventory, _ -> TaskTussleMenus.WinConditionMenu(id, inventory, this) }, Component.literal("Win condition picker"),
	))

	fun openTaskBrowser(player: ServerPlayer): Boolean {
		if (game != null || selection != null) return false
		openMenu(player, SimpleMenuProvider(
			{ id, inventory, _ -> TaskTussleMenus.CatalogTypeMenu(id, inventory, this) }, Component.literal("Task Browser"),
		))
		return true
	}

	fun openCatalogFiles(player: ServerPlayer, blocks: Boolean, selected: String? = null): Boolean {
		if (game != null || selection != null) return false
		openMenu(player, SimpleMenuProvider(
			{ id, inventory, _ -> TaskTussleMenus.FilePickerMenu(id, inventory, this, blocks, true, selected) }, Component.literal("Task Files"),
		))
		return true
	}

	fun returnToCatalogFiles(player: ServerPlayer, blocks: Boolean, selected: String) = after(player.level().server, 1) {
		if (player.containerMenu === player.inventoryMenu) openCatalogFiles(player, blocks, selected)
	}

	fun openFilePicker(player: ServerPlayer, blocks: Boolean) = openMenu(player, SimpleMenuProvider(
		{ id, inventory, _ -> TaskTussleMenus.FilePickerMenu(id, inventory, this, blocks) }, Component.literal("File Name picker"),
	))

	fun openCatalogCreation(player: ServerPlayer, blocks: Boolean): Boolean {
		if (game != null || selection != null) return false
		openMenu(player, SimpleMenuProvider(
			{ id, inventory, _ -> TaskTussleMenus.CatalogCreateMenu(id, inventory, this, blocks) }, Component.literal("Create Task File"),
		))
		return true
	}

	fun openCatalogContents(player: ServerPlayer, blocks: Boolean, file: String): Boolean {
		if (game != null || selection != null) return false
		val pools = runCatching { if (blocks) TaskCatalog.blocks(file) else TaskCatalog.advancements(player.level().server, file) }
			.getOrElse { return fail(player, it.message ?: "Invalid task file") }
		val removed = TaskCatalog.removed(player.level().server, blocks, pools)
		openMenu(player, SimpleMenuProvider(
			{ id, inventory, _ -> TaskTussleMenus.CatalogContentsMenu(id, inventory, this, blocks, file, pools, removed) }, Component.literal(file),
		))
		return true
	}

	fun catalogFiles(blocks: Boolean): List<CatalogFile> = TaskCatalog.catalogFiles(blocks)

	fun canEditCatalog(player: ServerPlayer, blocks: Boolean, file: String): Boolean = runCatching {
		mayEditCatalog(TaskCatalog.owner(blocks, file), player.uuid, isOperator(player))
	}.getOrDefault(false)

	fun createCatalog(player: ServerPlayer, blocks: Boolean, generated: Boolean): Boolean {
		val pools = if (generated) TaskCatalog.generated(player.level().server, blocks) else TaskPools(emptyList(), emptyList(), emptyList())
		val file = saveCatalog(player, blocks, pools) ?: return false
		return openCatalogFiles(player, blocks, file)
	}

	fun duplicateCatalog(player: ServerPlayer, blocks: Boolean, source: String): String? {
		val pools = runCatching { if (blocks) TaskCatalog.blocks(source) else TaskCatalog.advancements(player.level().server, source) }
			.getOrElse { fail(player, it.message ?: "Could not duplicate task file"); return null }
		return saveCatalog(player, blocks, pools)
	}

	fun moveCatalogTask(player: ServerPlayer, blocks: Boolean, file: String, id: Identifier, target: Difficulty?): Pair<TaskPools, List<Identifier>>? {
		if (!canEditCatalog(player, blocks, file)) { fail(player, "You cannot edit this task file."); return null }
		val current = runCatching { if (blocks) TaskCatalog.blocks(file) else TaskCatalog.advancements(player.level().server, file) }
			.getOrElse { fail(player, it.message ?: "Could not edit task file"); return null }
		val updated = moveTaskBetweenTiers(current, id, target)
		runCatching { TaskCatalog.update(blocks, file, updated) }.getOrElse { fail(player, it.message ?: "Could not save task file"); return null }
		if (file == if (blocks) settings.blocksFile else settings.advancementsFile) runCatching(::reloadPools)
		return updated to TaskCatalog.removed(player.level().server, blocks, updated)
	}
	fun deleteCatalog(player: ServerPlayer, blocks: Boolean, file: String): Boolean {
		if (!canEditCatalog(player, blocks, file)) return fail(player, "You do not own this task file.")
		runCatching { TaskCatalog.delete(blocks, file) }.getOrElse { return fail(player, it.message ?: "Could not delete task file") }
		val current = if (blocks) settings.blocksFile else settings.advancementsFile
		if (current == file) TaskCatalog.catalogFiles(blocks).firstOrNull()?.name?.let { replacement ->
			if (blocks) settings.blocksFile = replacement else settings.advancementsFile = replacement
			runCatching(::reloadPools)
		}
		return true
	}

	private fun saveCatalog(player: ServerPlayer, blocks: Boolean, pools: TaskPools): String? {
		val operator = isOperator(player)
		val owned = TaskCatalog.ownedFiles(player.uuid)
		val total = TaskCatalog.totalFiles(blocks)
		if (!mayCreateCatalog(owned, total, operator)) {
			fail(player, if (total >= 36) "This task list already has 36 files." else "You can create at most 3 task files.")
			return null
		}
		return runCatching { TaskCatalog.create(blocks, player.uuid, player.gameProfile.name, pools) }
			.getOrElse { fail(player, it.message ?: "Could not create task file"); null }
	}

	private fun isOperator(player: ServerPlayer) = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
	fun openTaskOptions(player: ServerPlayer, teamIndex: Int, taskIndex: Int) {
		val active = game ?: return
		val team = active.teams.find { it.index == teamIndex } ?: return
		val task = team.tasks.getOrNull(taskIndex) ?: return
		if (team.succeedTokensMax <= 0) return
		openMenu(player, SimpleMenuProvider(
			{ id, inventory, _ -> TaskTussleMenus.TaskOptionsMenu(id, inventory, this, teamIndex, taskIndex) },
			Component.literal(taskName(task)),
		))
	}

	fun clickTask(player: ServerPlayer, teamIndex: Int, taskIndex: Int, rightClick: Boolean): Boolean {
		val active = game ?: return false
		val team = active.teams.find { it.index == teamIndex } ?: return false
		val task = team.tasks.getOrNull(taskIndex) ?: return false
		if (player.uuid !in team.members || task.completed || task.failed || active.paused || active.finished) return false
		if (rightClick) {
			openTaskOptions(player, teamIndex, taskIndex)
			return true
		}
		return when (task.template.kind) {
			TaskKind.CLICK -> complete(player.level().server, active, team, task, player)
			TaskKind.BLOCK -> submitObtain(player.level().server, active, team, task, player)
			TaskKind.ADVANCEMENT -> false
		}
	}

	fun useSucceedToken(player: ServerPlayer, teamIndex: Int, taskIndex: Int): Boolean {
		val active = game ?: return false
		val team = active.teams.find { it.index == teamIndex } ?: return false
		val task = team.tasks.getOrNull(taskIndex) ?: return false
		if (player.uuid !in team.members || team.succeedTokens <= 0 || task.completed || task.failed || active.paused || active.finished) return false
		team.succeedTokens--
		return complete(player.level().server, active, team, task, player)
	}

	private fun submitObtain(server: MinecraftServer, active: TussleGame, team: TussleTeam, task: BingoTask, player: ServerPlayer): Boolean {
		if (settings.handInItems) return false
		val wanted = BuiltInRegistries.BLOCK.getValue(task.template.id).asItem()
		val stacks = buildList {
			for (slot in 0 until player.inventory.containerSize) add(player.inventory.getItem(slot))
			add(player.containerMenu.carried)
		}
		return stacks.any { it.item == wanted && !it.isEmpty } && complete(server, active, team, task, player)
	}

	fun clickDeliverySpot(player: ServerPlayer, stack: ItemStack) {
		val active = game ?: return
		if (!settings.handInItems) return
		if (deliverySpotLearned.add(player.uuid)) giveDeliverySpot(player)
		if (stack.isEmpty || active.paused || active.finished) return
		val team = active.teams.find { player.uuid in it.members } ?: return
		val task = deliveryTask(team.tasks, stack.item) ?: return
		stack.shrink(1)
		player.inventory.setChanged()
		complete(player.level().server, active, team, task, player)
	}

	fun beginTeamSelection(starter: ServerPlayer, teamCount: Int, startsGame: Boolean): Boolean {
		if (teamCount !in 2..54) return fail(starter, "team count has to be from 2 to 54")
		selection?.let { clearSelection(starter.level().server) }
		starter.level().server.playerList.players.forEach(::removeSettingsTool)
		starter.level().server.playerList.players.forEach(::removeTaskBrowser)
		selection = TeamSelection(teamCount, starter.uuid, startsGame = startsGame)
		starter.level().server.playerList.players.forEach(::giveTeamNavigator)
		giveTeamFinisher(starter)
		starter.sendSystemMessage(tagged("use the Team Navigator Tool to choose teams, then Detonate Current Teams", ChatFormatting.GREEN))
		return true
	}

	fun openTeamMaker(player: ServerPlayer): Boolean {
		val current = selection ?: return false
		openMenu(player, SimpleMenuProvider(
			{ id, inventory, _ -> TaskTussleMenus.TeamMakerMenu(id, inventory, this, current) }, Component.literal("Team Maker"),
		))
		return true
	}

	fun toggleTeam(player: ServerPlayer, team: Int): Boolean {
		val current = selection ?: return false
		if (team !in 1..current.teamCount) return false
		if (current.members[player.uuid] == team) current.members.remove(player.uuid) else current.members[player.uuid] = team
		currentServer().playerList.players.forEach { candidate ->
			(candidate.containerMenu as? TaskTussleMenus.TeamMakerMenu)?.refresh()
		}
		return true
	}

	fun finishTeamSelection(player: ServerPlayer): Boolean {
		val current = selection ?: return false
		if (player.uuid != current.starter) return false
		val server = player.level().server
		val teams = (1..current.teamCount).mapNotNull { index ->
			current.members.filterValues { it == index }.keys.takeIf { it.isNotEmpty() }?.let { TussleTeam(index, it.toMutableSet()) }
		}
		if (teams.isEmpty()) return fail(player, "no players selected a team")
		selection = null
		server.playerList.players.forEach(::removeTeamItems)
		listedTeams = teams
		if (!current.startsGame) server.playerList.players.forEach(::giveSettingsTool)
		if (!current.startsGame) server.playerList.players.forEach(::giveTaskBrowser)
		return !current.startsGame || startWithTeams(server, player, teams)
	}

	fun generateTeams(player: ServerPlayer, count: Int): Boolean {
		if (count < 1) return fail(player, "team amount must be at least 1")
		listedTeams = randomTeams(player.level().server.playerList.players, count)
		teamStatus(player)
		return true
	}

	fun clearTeams(server: MinecraftServer) {
		if (game != null) stop(server, null)
		clearSelection(server)
		listedTeams = emptyList()
	}

	fun teamStatus(player: ServerPlayer) {
		player.sendSystemMessage(tagged("active teamSets:", ChatFormatting.YELLOW))
		if (listedTeams.isEmpty()) player.sendSystemMessage(message("there are no teamSets active", ChatFormatting.GREEN))
		else listedTeams.forEach { team ->
			val names = team.onlinePlayers(player.level().server.playerList.players).joinToString { it.gameProfile.name }
			player.sendSystemMessage(message("- ${team.displayName()}: $names", team.color))
		}
	}

	fun debugStatus(player: ServerPlayer) {
		player.sendSystemMessage(tagged("active taskTussle game:", ChatFormatting.YELLOW))
		val active = game
		if (active == null) player.sendSystemMessage(message("there is no game active", ChatFormatting.GREEN))
		else player.sendSystemMessage(message("active: Bingo (${active.teams.size} teams, ${active.teams.sumOf { it.tasks.count { task -> !task.completed && !task.failed } }} active tasks)", ChatFormatting.YELLOW))
	}

	fun selectCatalog(player: ServerPlayer, blocks: Boolean, file: String): Boolean {
		val old = if (blocks) settings.blocksFile else settings.advancementsFile
		if (blocks) settings.blocksFile = file else settings.advancementsFile = file
		return runCatching(::reloadPools).fold(
			onSuccess = { true },
			onFailure = {
				if (blocks) settings.blocksFile = old else settings.advancementsFile = old
				fail(player, it.message ?: "Invalid task file")
			},
		)
	}

	fun onJoin(player: ServerPlayer) {
		if (game == null && selection == null) giveSettingsTool(player)
		if (game == null && selection == null) giveTaskBrowser(player)
		if (game?.teams?.any { player.uuid in it.members } == true) {
			giveCard(player)
			if (settings.handInItems) giveDeliverySpot(player)
		}
		if (selection != null) giveTeamNavigator(player)
		if (selection?.starter == player.uuid) giveTeamFinisher(player)
	}

	fun shutdown(server: MinecraftServer) {
		clearTimerBar()
		delayedActions.clear()
		selection = null
	}

	fun currentGame(): TussleGame? = game
	fun currentSelection(): TeamSelection? = selection
	fun blockFiles(): List<String> = TaskCatalog.blockFiles()
	fun advancementFiles(): List<String> = TaskCatalog.advancementFiles()
	fun refreshCards() = server?.let(::refreshOpenCards)

	fun isCard(stack: ItemStack): Boolean = hasMarker(stack, CARD_MARKER)
	fun isTeamNavigator(stack: ItemStack): Boolean = hasMarker(stack, TEAM_NAVIGATOR_MARKER)
	fun isTeamFinisher(stack: ItemStack): Boolean = hasMarker(stack, TEAM_FINISHER_MARKER)
	fun isDeliverySpot(stack: ItemStack): Boolean = hasMarker(stack, DELIVERY_SPOT_MARKER)
	fun isSettingsTool(stack: ItemStack): Boolean = hasMarker(stack, SETTINGS_MARKER)
	fun isTaskBrowser(stack: ItemStack): Boolean = hasMarker(stack, TASK_BROWSER_MARKER)

	private fun checkTasks(server: MinecraftServer, active: TussleGame) {
		for (team in active.teams) for (player in team.onlinePlayers(server.playerList.players)) for (task in team.tasks.filter { !it.completed && !it.failed }) {
			when (task.template.kind) {
				TaskKind.BLOCK -> submitObtain(server, active, team, task, player)
				TaskKind.ADVANCEMENT -> if (server.advancements.get(task.template.id)?.let { player.advancements.getOrStartProgress(it).isDone } == true) complete(server, active, team, task, player)
				TaskKind.CLICK -> Unit
			}
		}
	}

	private fun complete(server: MinecraftServer, active: TussleGame, team: TussleTeam, task: BingoTask, player: ServerPlayer): Boolean {
		if (task.completed || task.failed || active.paused || active.finished) return false
		task.completed = true
		task.contributors += player.gameProfile.name
		server.playerList.players.forEach { recipient ->
			val sameTeam = recipient.uuid in team.members
			if (settings.cardVisibility == "visible" || sameTeam) recipient.sendSystemMessage(completionMessage(player.gameProfile.name, team.color, task))
			else if (settings.cardVisibility != "hidden") recipient.sendSystemMessage(completionMessage(player.gameProfile.name, team.color, null))
		}
		val won = hasWon(team.tasks, settings.winningCondition)
		team.onlinePlayers(server.playerList.players).forEach { member ->
			if (!won) TaskTussleSounds.rewardMid(member)
		}
		refreshOpenCards(server)
		if (won) finish(server, team)
		after(server, TASK_ITEM_DISPLAY_DELAY_TICKS) {
			server.playerList.getPlayer(player.uuid)?.let { showCompletionItem(it, task) }
		}
		return true
	}

	private fun finish(server: MinecraftServer, winner: TussleTeam?): Boolean {
		val active = game ?: return false
		if (active.finished) return false
		active.finished = true
		active.paused = true
		delayedActions.clear()
		clearTimerBar()
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

	private fun after(server: MinecraftServer, ticks: Int, action: () -> Unit) {
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

	private fun resetAllAdvancements(server: MinecraftServer, active: TussleGame) {
		if (settings.cardVisibility != "visible") server.allLevels.forEach { level ->
			level.gameRules.set(GameRules.SHOW_ADVANCEMENT_MESSAGES, false, server)
		}
		for (player in server.playerList.players.filter { candidate -> active.teams.any { candidate.uuid in it.members } }) {
			for (advancement in server.advancements.allAdvancements) {
				val progress = player.advancements.getOrStartProgress(advancement)
				progress.completedCriteria.toList().forEach { player.advancements.revoke(advancement, it) }
			}
		}
	}

	private fun randomTeams(players: List<ServerPlayer>, count: Int): List<TussleTeam> {
		val teams = (1..count).map { TussleTeam(it, mutableSetOf()) }
		players.shuffled().forEachIndexed { index, player -> teams[index % count].members += player.uuid }
		return teams
	}

	private fun giveCard(player: ServerPlayer) = giveAt(
		player, 8, Items.PAPER, "Task Tussle Card", ChatFormatting.GREEN, CARD_MARKER,
		BINGO_CARD_MODEL.takeIf { TaskTussleResourcePackServer.active },
	)
	private fun giveTeamNavigator(player: ServerPlayer) = giveAt(
		player, 8, Items.PAPER, "Teams", ChatFormatting.BLUE, TEAM_NAVIGATOR_MARKER,
		TEAMS_MODEL.takeIf { TaskTussleResourcePackServer.active },
	)
	private fun giveTeamFinisher(player: ServerPlayer) = giveAt(player, 7, Items.FIRE_CHARGE, "Detonate Current Teams", ChatFormatting.GOLD, TEAM_FINISHER_MARKER)

	private fun giveSettingsTool(player: ServerPlayer) {
		val displaced = player.inventory.getItem(SETTINGS_SLOT).takeUnless(::isSettingsTool)?.takeUnless(ItemStack::isEmpty)?.copy()
		giveAt(player, SETTINGS_SLOT, Items.COMPASS, "Task Tussle Settings", ChatFormatting.AQUA, SETTINGS_MARKER)
		if (displaced != null && !player.inventory.add(displaced)) player.drop(displaced, false)
	}

	private fun giveTaskBrowser(player: ServerPlayer) {
		val displaced = player.inventory.getItem(TASK_BROWSER_SLOT).takeUnless(::isTaskBrowser)?.takeUnless(ItemStack::isEmpty)?.copy()
		giveAt(player, TASK_BROWSER_SLOT, Items.MAP, "Task Browser", ChatFormatting.GREEN, TASK_BROWSER_MARKER)
		if (displaced != null && !player.inventory.add(displaced)) player.drop(displaced, false)
	}

	private fun giveDeliverySpot(player: ServerPlayer) {
		val displaced = player.inventory.getItem(DELIVERY_SPOT_SLOT).takeUnless(::isDeliverySpot)?.takeUnless(ItemStack::isEmpty)?.copy()
		giveAt(
			player, DELIVERY_SPOT_SLOT, Items.PAPER, "Delivery spot", ChatFormatting.GOLD, DELIVERY_SPOT_MARKER,
			if (player.uuid in deliverySpotLearned) DELIVERY_SPOT_MODEL else DELIVERY_SPOT_SIGN_MODEL,
		)
		if (displaced != null && !player.inventory.add(displaced)) player.drop(displaced, false)
	}

	private fun giveAt(player: ServerPlayer, slot: Int, item: net.minecraft.world.item.Item, name: String, color: ChatFormatting, marker: String, model: Identifier? = null) {
		for (index in 0 until player.inventory.containerSize) if (hasMarker(player.inventory.getItem(index), marker)) player.inventory.setItem(index, ItemStack.EMPTY)
		player.inventory.setItem(slot, ItemStack(item).apply {
			set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle { it.withColor(color).withItalic(false) })
			set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().apply { putString(TOOL_MARKER, marker) }))
			if (model != null) set(DataComponents.ITEM_MODEL, model)
		})
	}

	private fun removeGameItems(player: ServerPlayer) {
		for (slot in 0 until player.inventory.containerSize) {
			val stack = player.inventory.getItem(slot)
			if (isCard(stack) || isDeliverySpot(stack)) player.inventory.setItem(slot, ItemStack.EMPTY)
		}
	}

	private fun removeTeamItems(player: ServerPlayer) {
		for (slot in 0 until player.inventory.containerSize) {
			val stack = player.inventory.getItem(slot)
			if (isTeamNavigator(stack) || isTeamFinisher(stack)) player.inventory.setItem(slot, ItemStack.EMPTY)
		}
	}

	private fun removeSettingsTool(player: ServerPlayer) {
		for (slot in 0 until player.inventory.containerSize) if (isSettingsTool(player.inventory.getItem(slot))) player.inventory.setItem(slot, ItemStack.EMPTY)
	}

	private fun removeTaskBrowser(player: ServerPlayer) {
		for (slot in 0 until player.inventory.containerSize) if (isTaskBrowser(player.inventory.getItem(slot))) player.inventory.setItem(slot, ItemStack.EMPTY)
	}

	private fun clearSelection(server: MinecraftServer) {
		selection = null
		server.playerList.players.forEach(::removeTeamItems)
		if (game == null) server.playerList.players.forEach(::giveSettingsTool)
		if (game == null) server.playerList.players.forEach(::giveTaskBrowser)
	}

	private fun refreshOpenCards(server: MinecraftServer) = server.playerList.players.forEach { player ->
		(player.containerMenu as? TaskTussleMenus.CardMenu)?.refresh()
	}

	private fun restoreForcedItems(server: MinecraftServer) {
		if (game == null && selection == null) server.playerList.players.filter { !hasMarker(it, SETTINGS_MARKER) }.forEach(::giveSettingsTool)
		if (game == null && selection == null) server.playerList.players.filter { !hasMarker(it, TASK_BROWSER_MARKER) }.forEach(::giveTaskBrowser)
		game?.teams?.flatMap(TussleTeam::members)?.toSet()?.let { members ->
			server.playerList.players.filter { it.uuid in members && !hasMarker(it, CARD_MARKER) }.forEach(::giveCard)
			if (settings.handInItems) server.playerList.players.filter { it.uuid in members && !hasMarker(it, DELIVERY_SPOT_MARKER) }.forEach(::giveDeliverySpot)
		}
		selection?.let { current ->
			server.playerList.players.filter { !hasMarker(it, TEAM_NAVIGATOR_MARKER) }.forEach(::giveTeamNavigator)
			server.playerList.getPlayer(current.starter)?.takeIf { !hasMarker(it, TEAM_FINISHER_MARKER) }?.let(::giveTeamFinisher)
		}
	}

	private fun createTimerBar(server: MinecraftServer, active: TussleGame) {
		timerBar = ServerBossEvent(java.util.UUID.randomUUID(), Component.literal("TaskTussle"), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS).also { bar ->
			server.playerList.players.forEach(bar::addPlayer)
			updateTimerBar(active)
		}
	}

	private fun updateTimerBar(active: TussleGame) {
		val total = max(1, settings.gameMinutes * 60)
		timerBar?.apply {
			progress = active.remainingSeconds.toFloat() / total
			name = Component.literal("Time left: ${active.remainingSeconds / 60}:${(active.remainingSeconds % 60).toString().padStart(2, '0')}")
			color = if (active.remainingSeconds > 120) BossEvent.BossBarColor.GREEN else BossEvent.BossBarColor.YELLOW
		}
	}

	private fun clearTimerBar() {
		timerBar?.removeAllPlayers()
		timerBar = null
	}

	private fun currentServer(): MinecraftServer = requireNotNull(server)
	private fun hasMarker(stack: ItemStack, marker: String): Boolean =
		stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr(TOOL_MARKER, "") == marker
	private fun hasMarker(player: ServerPlayer, marker: String): Boolean =
		(0 until player.inventory.containerSize).any { hasMarker(player.inventory.getItem(it), marker) }
	private fun openMenu(player: ServerPlayer, provider: MenuProvider) = player.containerMenu.takeUnless { it === player.inventoryMenu }?.let { previous ->
		player.containerMenu = player.inventoryMenu
		player.openMenu(provider).also { opened ->
			if (opened.isEmpty) player.containerMenu = previous
			else { previous.removed(player); player.inventoryMenu.transferState(previous) }
		}
	} ?: player.openMenu(provider)
	private fun fail(player: ServerPlayer?, text: String): Boolean {
		player?.sendSystemMessage(tagged(text, ChatFormatting.RED))
		return false
	}
	private fun log(text: String) { if (logEnabled) println("[Task Tussle] $text") }

	fun catalogName(blocks: Boolean, id: Identifier): String = if (blocks) BuiltInRegistries.BLOCK.getValue(id).name.string
	else server?.advancements?.get(id)?.value?.display?.orElse(null)?.title?.string
		?: id.path.substringAfterLast('/').replace('_', ' ').replaceFirstChar(Char::uppercase)

	fun advancementDescription(id: Identifier): String? =
		server?.advancements?.get(id)?.value?.display?.orElse(null)?.description?.string?.takeIf(String::isNotBlank)

	fun advancementIcon(id: Identifier): ItemStack {
		if (!settings.customAdvancementIcons) return ItemStack(Items.KNOWLEDGE_BOOK)
		val icon = server?.advancements?.get(id)?.value?.display?.orElse(null)?.icon?.create() ?: ItemStack(Items.KNOWLEDGE_BOOK)
		return (if (icon.item is SmithingTemplateItem) ItemStack(Items.PAPER) else icon).apply {
			set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT
				.withHidden(DataComponents.ATTRIBUTE_MODIFIERS, true)
				.withHidden(DataComponents.POT_DECORATIONS, true)
				.withHidden(DataComponents.BANNER_PATTERNS, true)
				.withHidden(DataComponents.BEES, true)
				.withHidden(DataComponents.BLOCK_STATE, true))
			if (id.namespace == "minecraft") {
				set(DataComponents.ITEM_MODEL, ADVANCEMENT_ICON_MODEL)
				set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData(emptyList(), emptyList(), listOf(id.toString()), emptyList()))
			}
		}
	}

	fun taskName(task: BingoTask): String = when (task.template.kind) {
		TaskKind.BLOCK -> BuiltInRegistries.BLOCK.getValue(task.template.id).name.string
		TaskKind.ADVANCEMENT -> server?.advancements?.get(task.template.id)?.value?.display?.orElse(null)?.title?.string
			?: task.template.id.path.substringAfterLast('/').replace('_', ' ').replaceFirstChar(Char::uppercase)
		TaskKind.CLICK -> "CLICK ME"
	}

	private fun taskKindName(task: BingoTask) = when (task.template.kind) {
		TaskKind.BLOCK -> "obtain task"
		TaskKind.ADVANCEMENT -> "advancements task"
		TaskKind.CLICK -> "click task"
	}

	private fun showCompletionItem(player: ServerPlayer, task: BingoTask) {
		val item = when (task.template.kind) {
			TaskKind.BLOCK -> BuiltInRegistries.BLOCK.getValue(task.template.id).asItem()
			TaskKind.ADVANCEMENT -> Items.KNOWLEDGE_BOOK
			TaskKind.CLICK -> Items.BELL
		}
		displayItem(player, ItemStack(item).apply {
			set(DataComponents.CUSTOM_NAME, message(taskName(task), ChatFormatting.WHITE))
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
	private fun completionMessage(playerName: String, teamColor: ChatFormatting, task: BingoTask?): Component = Component.literal(playerName).withStyle(teamColor)
		.append(Component.literal(if (task == null) " got a task" else " got a ${taskKindName(task)} ").withStyle(ChatFormatting.WHITE))
		.apply { if (task != null) append(Component.literal("(${taskName(task)})").withStyle(ChatFormatting.GRAY)) }

	companion object {
		private const val TASK_ITEM_DISPLAY_DELAY_TICKS = 8
		private const val TITLE_REVEAL_TICKS = 30
		private const val GAME_RESULT_DELAY_TICKS = 122
		private const val TOOL_MARKER = "tasktussle_tool"
		private const val CARD_MARKER = "card"
		private const val TEAM_NAVIGATOR_MARKER = "team_navigator"
		private const val TEAM_FINISHER_MARKER = "team_finisher"
		private const val DELIVERY_SPOT_MARKER = "delivery_spot"
		private const val SETTINGS_MARKER = "settings"
		private const val TASK_BROWSER_MARKER = "task_browser"
		private const val DELIVERY_SPOT_SLOT = 17
		private const val SETTINGS_SLOT = 4
		private const val TASK_BROWSER_SLOT = 5
		private val BINGO_CARD_MODEL = Identifier.fromNamespaceAndPath("tasktussle", "bingo_card")
		private val TEAMS_MODEL = Identifier.fromNamespaceAndPath("tasktussle", "teams")
		private val DELIVERY_SPOT_MODEL = Identifier.fromNamespaceAndPath("tasktussle", "delivering_spot")
		private val DELIVERY_SPOT_SIGN_MODEL = Identifier.fromNamespaceAndPath("tasktussle", "delivering_spot_sign")
		private val ADVANCEMENT_ICON_MODEL = Identifier.fromNamespaceAndPath("tasktussle", "advancement_icon")

		@JvmStatic
		fun isForcedItem(stack: ItemStack): Boolean = isForcedMarker(stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr(TOOL_MARKER, ""))

		internal fun isForcedMarker(marker: String?) =
			marker == CARD_MARKER || marker == TEAM_NAVIGATOR_MARKER || marker == TEAM_FINISHER_MARKER || marker == DELIVERY_SPOT_MARKER || marker == SETTINGS_MARKER || marker == TASK_BROWSER_MARKER

		fun clearNonForcedItems(player: ServerPlayer) {
			for (slot in 0 until player.inventory.containerSize) {
				if (!isForcedItem(player.inventory.getItem(slot))) player.inventory.setItem(slot, ItemStack.EMPTY)
			}
		}

		fun message(text: String, color: ChatFormatting): Component = Component.literal(text).withStyle(color)
		fun tagged(text: String, color: ChatFormatting): Component = Component.literal("[").withStyle(ChatFormatting.GRAY)
			.append(Component.literal("Task Tussle").withStyle(ChatFormatting.GOLD))
			.append(Component.literal("] ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(text).withStyle(color))

		fun hasWon(tasks: List<BingoTask>, condition: String): Boolean {
			if (tasks.size != 25) return false
			val complete = tasks.map(BingoTask::completed)
			val horizontal = (0..4).count { row -> (0..4).all { column -> complete[row * 5 + column] } }
			val vertical = (0..4).count { column -> (0..4).all { row -> complete[row * 5 + column] } }
			val diagonal = listOf((0..4).all { complete[it * 6] }, (0..4).all { complete[4 + it * 4] }).count { it }
			return when (condition) {
				"horizontal line" -> horizontal > 0
				"vertical line" -> vertical > 0
				"diagonal line" -> diagonal > 0
				"full card" -> horizontal == 5
				"2 lines" -> horizontal + vertical + diagonal >= 2
				"3 lines" -> horizontal + vertical + diagonal >= 3
				"4 lines" -> horizontal + vertical + diagonal >= 4
				else -> horizontal + vertical + diagonal >= 1
			}
		}
	}
}


internal fun deliveryTask(tasks: List<BingoTask>, item: net.minecraft.world.item.Item): BingoTask? = tasks.firstOrNull {
	!it.completed && !it.failed && it.template.kind == TaskKind.BLOCK &&
		BuiltInRegistries.BLOCK.getValue(it.template.id).asItem() == item
}

internal fun <T> takeDue(tick: Int, scheduled: MutableList<Pair<Int, T>>): List<T> {
	val due = scheduled.filter { it.first <= tick }
	scheduled.removeAll(due)
	return due.map { it.second }
}

internal fun titleFrames(text: String, ticks: Int): List<Pair<Int, String>> = text.indices.map { index ->
	(if (text.length == 1) 0 else index * ticks / (text.length - 1)) to text.take(index + 1)
}
