package me.wanttobee.tasktussle

import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.DispenserMenu
import net.minecraft.world.inventory.HopperMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.level.block.Blocks
import kotlin.math.max

object TaskTussleMenus {
	data class Click(val button: Int, val shift: Boolean) {
		val left: Boolean get() = button == 0
		val right: Boolean get() = button == 1
	}

	private fun runAction(menu: AbstractContainerMenu, player: Player, action: ((ServerPlayer, Click) -> Unit)?, click: Click) {
		val serverPlayer = player as? ServerPlayer ?: return
		action ?: return
		val before = menu.items.map(ItemStack::copy)
		action(serverPlayer, click)
		when (menuClickSound(serverPlayer.containerMenu !== menu, !ItemStack.listMatches(before, menu.items))) {
			MenuClickSound.BUTTON -> TaskTussleSounds.click(serverPlayer)
			MenuClickSound.PAGE -> TaskTussleSounds.clickPage(serverPlayer)
			null -> Unit
		}
	}

	abstract class ActionChestMenu(
		containerId: Int,
		protected val playerInventory: Inventory,
		rows: Int,
		protected val contents: SimpleContainer = SimpleContainer(rows * 9),
	) : ChestMenu(menuType(rows), containerId, playerInventory, contents, rows) {
		private val actions = mutableMapOf<Int, (ServerPlayer, Click) -> Unit>()
		private val menuSlots = rows * 9

		final override fun quickMoveStack(player: Player, slotId: Int): ItemStack = ItemStack.EMPTY
		protected fun clear() { contents.clearContent(); actions.clear() }
		protected fun display(slot: Int, stack: ItemStack) = contents.setItem(slot, stack)
		protected fun button(slot: Int, stack: ItemStack, action: (ServerPlayer, Click) -> Unit) { display(slot, stack); actions[slot] = action }
		protected fun fill(stack: () -> ItemStack = ::separator) { repeat(menuSlots) { display(it, stack()) } }

		override fun clicked(slotId: Int, button: Int, input: ContainerInput, player: Player) {
			if (slotId in 0 until menuSlots) {
				if (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE) runAction(this, player, actions[slotId], Click(button, input == ContainerInput.QUICK_MOVE))
				broadcastChanges()
				return
			}
			super.clicked(slotId, button, input, player)
		}
	}

	abstract class ActionHopperMenu(
		containerId: Int,
		playerInventory: Inventory,
		protected val contents: SimpleContainer = SimpleContainer(5),
	) : HopperMenu(containerId, playerInventory, contents) {
		private val actions = mutableMapOf<Int, (ServerPlayer, Click) -> Unit>()
		final override fun quickMoveStack(player: Player, slotId: Int): ItemStack = ItemStack.EMPTY
		protected fun display(slot: Int, stack: ItemStack) = contents.setItem(slot, stack)
		protected fun button(slot: Int, stack: ItemStack, action: (ServerPlayer, Click) -> Unit) { display(slot, stack); actions[slot] = action }
		override fun clicked(slotId: Int, button: Int, input: ContainerInput, player: Player) {
			if (slotId in 0 until 5) {
				if (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE) runAction(this, player, actions[slotId], Click(button, input == ContainerInput.QUICK_MOVE))
				broadcastChanges()
				return
			}
			super.clicked(slotId, button, input, player)
		}
	}

	abstract class ActionDispenserMenu(
		containerId: Int,
		playerInventory: Inventory,
		protected val contents: SimpleContainer = SimpleContainer(9),
	) : DispenserMenu(containerId, playerInventory, contents) {
		private val actions = mutableMapOf<Int, (ServerPlayer, Click) -> Unit>()
		final override fun quickMoveStack(player: Player, slotId: Int): ItemStack = ItemStack.EMPTY
		protected fun clear() { contents.clearContent(); actions.clear() }
		protected fun display(slot: Int, stack: ItemStack) = contents.setItem(slot, stack)
		protected fun button(slot: Int, stack: ItemStack, action: (ServerPlayer, Click) -> Unit) { display(slot, stack); actions[slot] = action }
		override fun clicked(slotId: Int, button: Int, input: ContainerInput, player: Player) {
			if (slotId in 0 until 9) {
				if (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE) runAction(this, player, actions[slotId], Click(button, input == ContainerInput.QUICK_MOVE))
				broadcastChanges()
				return
			}
			super.clicked(slotId, button, input, player)
		}
	}

	class CardMenu(
		containerId: Int,
		inventory: Inventory,
		private val service: TaskTussleService,
		private val game: TussleGame,
		private val team: TussleTeam,
		private val viewersTeam: TussleTeam?,
	) : ActionChestMenu(containerId, inventory, 5) {
		init { refresh() }

		fun refresh() {
			clear()
			fill()
			team.tasks.forEachIndexed { index, task ->
				val slot = 2 + index % 5 + 9 * (index / 5)
				val stack = taskStack(service, task)
				if (!task.completed && !task.failed && viewersTeam == team && !game.paused && !game.finished) {
					button(slot, stack) { player, click -> service.clickTask(player, team.index, index, click.right) }
				} else display(slot, stack)
			}
			repeat(10) { index -> display(9 * (index % 5) + 8 * (index / 5), emptyTeamSlot()) }
			game.teams.take(10).forEachIndexed { index, other ->
				val slot = 9 * (index % 5) + 8 * (index / 5)
				val private = other == team
				val visible = private || game.finished || service.settings.cardVisibility == "visible"
				val progressVisible = private || game.finished || service.settings.cardVisibility != "hidden"
				val lore = mutableListOf(other.onlinePlayers(currentPlayers()).joinToString { it.gameProfile.name })
				if (progressVisible) lore += "${other.completed}/25"
				if (game.finished) lore += contributions(other)
				val stack = named(
					teamItem(other.index, visible), if (private) "This Card" else other.displayName(), other.color, lore,
					count = if (progressVisible) max(1, other.completed) else 1,
				)
				if (!private && (game.finished || service.settings.cardVisibility == "visible")) button(slot, stack) { player, _ -> service.openCard(player, other.index) }
				else display(slot, stack)
			}
			broadcastChanges()
		}

		private fun currentPlayers() = checkNotNull(playerInventory.player.level().server).playerList.players

		private fun contributions(team: TussleTeam): List<String> {
			if (team.completed == 0) return team.onlinePlayers(currentPlayers()).map { "${it.gameProfile.name} = 0.0%" }
			return team.onlinePlayers(currentPlayers()).map { player ->
				val count = team.tasks.count { it.completed && player.gameProfile.name in it.contributors }
				"${player.gameProfile.name} = ${count * 10000 / team.completed / 100.0}%"
			}
		}
	}

	class SettingsMenu(
		containerId: Int,
		inventory: Inventory,
		private val service: TaskTussleService,
	) : ActionChestMenu(containerId, inventory, 6) {
		init { refresh() }

		private fun refresh() {
			clear(); fill()
			val settings = service.settings
			val visibilityItem = when (settings.cardVisibility) {
				"visible" -> Items.ENDER_EYE
				"hidden" -> Items.FIREWORK_STAR
				else -> Items.ENDER_PEARL
			}
			setting(0, visibilityItem, "Card Visibility: ${settings.cardVisibility}") { _, _ ->
				val options = listOf("progress only", "visible", "hidden")
				settings.cardVisibility = options[(options.indexOf(settings.cardVisibility) + 1) % options.size]
				service.refreshCards()
			}
			setting(1, Items.BRICK, "Easy Ratio: ${settings.easyRatio}", ratioLore, settings.easyRatio, settings.easyRatio > 0) { _, click -> settings.easyRatio = ratioChange(settings.easyRatio, click) }
			setting(2, Items.NETHER_BRICK, "Normal Ratio: ${settings.mediumRatio}", ratioLore, settings.mediumRatio, settings.mediumRatio > 0) { _, click -> settings.mediumRatio = ratioChange(settings.mediumRatio, click) }
			setting(3, Items.NETHERITE_INGOT, "Hard Ratio: ${settings.hardRatio}", ratioLore, settings.hardRatio, settings.hardRatio > 0) { _, click -> settings.hardRatio = ratioChange(settings.hardRatio, click) }
			setting(4, Items.PAPER, "Chose Teams: ${if (settings.chooseTeams) "on" else "off"}", enchanted = settings.chooseTeams, model = TEAMS_MODEL) { _, _ -> settings.chooseTeams = !settings.chooseTeams }
			setting(5, Items.CLOCK, "Game Time: ${if (settings.gameMinutes == 0) "disabled" else "${settings.gameMinutes} minutes"}", enchanted = settings.gameMinutes > 0) { player, _ -> service.openTimeSettings(player) }
			button(8, named(Items.WRITABLE_BOOK, "Load a pre-set", ChatFormatting.GOLD)) { player, _ -> player.sendSystemMessage(TaskTussleService.message("Does nothing yet", ChatFormatting.RED)) }

			for (slot in 27 until 36) display(slot, emptyOption("task type"))
			for (slot in 45 until 54) display(slot, emptyOption("game mode"))
			taskManager(27, Items.SHULKER_SHELL, "Obtain Task", settings.blockWeight, "obtain")
			taskManager(28, Items.KNOWLEDGE_BOOK, "Advancements Task", settings.advancementWeight, "advancements")
			taskManager(29, Items.PAPER, "Click Task", settings.clickWeight, "click")
			button(45, named(Items.FILLED_MAP, "Bingo", ChatFormatting.LIGHT_PURPLE, listOf("Click: Open details"))) { player, _ -> service.openManagerSettings(player, "bingo") }
			broadcastChanges()
		}

		private fun taskManager(slot: Int, item: Item, title: String, weight: Int, manager: String) {
			button(slot, named(item, "$title Ratio: ${if (weight == 0) "Disabled" else weight}", ChatFormatting.AQUA, taskLore, max(1, weight), weight > 0).apply {
				if (manager == "click") set(DataComponents.ITEM_MODEL, CLICK_MODELS.first())
			}) { player, click ->
				if (click.left && click.shift) service.openManagerSettings(player, manager)
				else {
					val value = when { click.right && click.shift -> 0; click.right -> max(0, weight - 1); else -> minOf(100, weight + 1) }
					when (manager) { "obtain" -> service.settings.blockWeight = value; "advancements" -> service.settings.advancementWeight = value; else -> service.settings.clickWeight = value }
					refresh()
				}
			}
		}

		private fun setting(slot: Int, item: Item, title: String, lore: List<String> = emptyList(), count: Int = 1, enchanted: Boolean = false, model: Identifier? = null, action: (ServerPlayer, Click) -> Unit) {
			button(slot, named(item, title, ChatFormatting.GOLD, lore, max(1, count), enchanted).apply { if (model != null) set(DataComponents.ITEM_MODEL, model) }) { player, click -> action(player, click); if (player.containerMenu === this) refresh() }
		}
	}

	class ManagerSettingsMenu(
		containerId: Int,
		inventory: Inventory,
		private val service: TaskTussleService,
		private val manager: String,
	) : ActionChestMenu(containerId, inventory, 2) {
		init { refresh() }

		private fun refresh() {
			clear(); fill()
			for (slot in 9 until 18) display(slot, emptySetting())
			val (item, title, color) = when (manager) {
				"obtain" -> Triple(Items.SHULKER_SHELL, "Obtain Task", ChatFormatting.AQUA)
				"advancements" -> Triple(Items.KNOWLEDGE_BOOK, "Advancements Task", ChatFormatting.AQUA)
				"click" -> Triple(Items.BELL, "Click Task", ChatFormatting.AQUA)
				else -> Triple(Items.FILLED_MAP, "Bingo", ChatFormatting.LIGHT_PURPLE)
			}
			button(4, named(item, title, color, listOf("Click: Go back"), enchanted = true)) { player, _ -> service.openSettings(player) }
			when (manager) {
				"obtain" -> obtainSettings()
				"advancements" -> fileSetting(9, false)
				"bingo" -> bingoSettings()
			}
			broadcastChanges()
		}

		private fun obtainSettings() {
			val settings = service.settings
			setting(9, Items.HOPPER, "Hand in items: ${if (settings.handInItems) "on" else "off"}", enchanted = settings.handInItems) { _, _ -> settings.handInItems = !settings.handInItems }
			fileSetting(10, true)
		}

		private fun bingoSettings() {
			val settings = service.settings
			setting(9, Blocks.DYED_CANDLE.lime().asItem(), "Succeed token: ${settings.succeedTokens}", count = max(1, settings.succeedTokens), enchanted = settings.succeedTokens > 0) { _, click -> settings.succeedTokens = simpleChange(settings.succeedTokens, click, 0, 100) }
			setting(10, Items.STICK, "Overdue: ${settings.overtime}", listOf("What happens when game time reaches zero"), enchanted = true) { _, _ -> settings.overtime = if (settings.overtime == "draw") "most tasks wins" else "draw" }
			setting(11, Items.ENDER_PEARL, "${if (settings.mutualTasks <= 25) "Mutual tasks" else "Task pool"}: ${settings.mutualTasks}", mutualLore, max(1, settings.mutualTasks), settings.mutualTasks > 0) { _, click ->
				settings.mutualTasks = when { click.left && click.shift -> 25; click.right && click.shift -> 0; click.right -> max(0, settings.mutualTasks - 1); else -> settings.mutualTasks + 1 }
			}
			setting(12, Items.AMETHYST_SHARD, "Win condition: ${settings.winningCondition}", enchanted = true) { player, _ -> service.openWinConditions(player) }
		}

		private fun fileSetting(slot: Int, blocks: Boolean) {
			val file = if (blocks) service.settings.blocksFile else service.settings.advancementsFile
			setting(slot, Items.NAME_TAG, "File name: $file", listOf("Click to change the file used to generate tasks"), enchanted = true) { player, _ -> service.openFilePicker(player, blocks) }
		}

		private fun setting(slot: Int, item: Item, title: String, lore: List<String> = emptyList(), count: Int = 1, enchanted: Boolean = false, action: (ServerPlayer, Click) -> Unit) {
			button(slot, named(item, title, ChatFormatting.GOLD, lore, max(1, count), enchanted)) { player, click -> action(player, click); if (player.containerMenu === this) refresh() }
		}
	}

	class TimeSettingsMenu(containerId: Int, inventory: Inventory, private val service: TaskTussleService) : ActionDispenserMenu(containerId, inventory) {
		init { refresh() }
		private fun refresh() {
			clear(); repeat(9) { display(it, separator()) }
			button(4, named(Items.CLOCK, "Game Time: ${if (service.settings.gameMinutes == 0) "disabled" else "${service.settings.gameMinutes} minutes"}", ChatFormatting.GOLD, listOf("Click to go back"), enchanted = service.settings.gameMinutes > 0)) { player, _ -> service.openSettings(player) }
			button(0, named(Blocks.DYED_CANDLE.red().asItem(), "-1", ChatFormatting.RED)) { _, _ -> service.settings.gameMinutes = max(0, service.settings.gameMinutes - 1); refresh() }
			button(6, named(Blocks.WOOL.red().asItem(), "-10", ChatFormatting.RED)) { _, _ -> service.settings.gameMinutes = max(0, service.settings.gameMinutes - 10); refresh() }
			button(2, named(Blocks.DYED_CANDLE.lime().asItem(), "+1", ChatFormatting.GREEN)) { _, _ -> service.settings.gameMinutes++; refresh() }
			button(8, named(Blocks.WOOL.lime().asItem(), "+10", ChatFormatting.GREEN)) { _, _ -> service.settings.gameMinutes += 10; refresh() }
			button(7, named(Items.BARRIER, "disable", ChatFormatting.RED)) { _, _ -> service.settings.gameMinutes = 0; refresh() }
			broadcastChanges()
		}
	}

	class WinConditionMenu(containerId: Int, inventory: Inventory, private val service: TaskTussleService) : ActionChestMenu(containerId, inventory, 1) {
		init {
			val options = listOf(
				Blocks.SMALL_AMETHYST_BUD.asItem() to "1 line", Blocks.MEDIUM_AMETHYST_BUD.asItem() to "2 lines",
				Blocks.LARGE_AMETHYST_BUD.asItem() to "3 lines", Blocks.AMETHYST_CLUSTER.asItem() to "4 lines",
				Blocks.AMETHYST_BLOCK.asItem() to "full card", null, Items.AMETHYST_SHARD to "horizontal line",
				Items.AMETHYST_SHARD to "vertical line", Items.AMETHYST_SHARD to "diagonal line",
			)
			options.forEachIndexed { slot, option ->
				if (option == null) display(slot, separator()) else button(slot, named(option.first, option.second, ChatFormatting.LIGHT_PURPLE, if (service.settings.winningCondition == option.second) listOf("current") else emptyList(), enchanted = service.settings.winningCondition == option.second)) { player, _ ->
					service.settings.winningCondition = option.second
					service.openManagerSettings(player, "bingo")
				}
			}
		}
	}

	class FilePickerMenu(
		containerId: Int,
		inventory: Inventory,
		private val service: TaskTussleService,
		private val blocks: Boolean,
		private val page: Int,
	) : ActionChestMenu(containerId, inventory, pickerRows(if (blocks) service.blockFiles().size else service.advancementFiles().size)) {
		init {
			fill()
			val files = if (blocks) service.blockFiles() else service.advancementFiles()
			val start = page * 45
			files.drop(start).take(45).forEachIndexed { index, file ->
				val current = file == if (blocks) service.settings.blocksFile else service.settings.advancementsFile
				button(index, named(if (index % 2 == 0) Items.PAPER else Items.MAP, file, ChatFormatting.YELLOW, if (current) listOf("current") else emptyList(), enchanted = current)) { player, _ ->
					if (service.selectCatalog(player, blocks, file)) service.openManagerSettings(player, if (blocks) "obtain" else "advancements")
				}
			}
			if (files.size > 45) {
				val totalPages = (files.size - 1) / 45 + 1
				display(49, named(Items.BOW, "${page + 1}/$totalPages", ChatFormatting.GRAY))
				if (page > 0) button(45, named(Items.ARROW, "Go Back", ChatFormatting.WHITE)) { player, _ -> service.openFilePicker(player, blocks, page - 1) }
				if (page + 1 < totalPages) button(53, named(Items.ARROW, "Go Next", ChatFormatting.WHITE)) { player, _ -> service.openFilePicker(player, blocks, page + 1) }
			}
		}
	}

	class TaskOptionsMenu(
		containerId: Int,
		inventory: Inventory,
		private val service: TaskTussleService,
		private val teamIndex: Int,
		private val taskIndex: Int,
	) : ActionHopperMenu(containerId, inventory) {
		init {
			val team = service.currentGame()?.teams?.find { it.index == teamIndex }
			val task = team?.tasks?.getOrNull(taskIndex)
			if (team != null && task != null) {
				button(0, named(taskItem(task), service.taskName(task), ChatFormatting.WHITE, listOf("Click: go back to the card"))) { player, _ -> service.openCard(player, teamIndex) }
				display(1, separator())
				for (slot in 2..4) display(slot, named(Blocks.DYED_CANDLE.lightGray().asItem(), "Empty token slot", ChatFormatting.GRAY))
				val available = team.succeedTokens > 0
				button(2, named(if (available) Blocks.DYED_CANDLE.lime().asItem() else Blocks.DYED_CANDLE.gray().asItem(), "Succeed token", if (available) ChatFormatting.GREEN else ChatFormatting.GRAY, listOf("${team.succeedTokens} left", "Complete the task instantly"))) { player, _ ->
					if (service.useSucceedToken(player, teamIndex, taskIndex)) service.openCard(player, teamIndex)
				}
			}
		}
	}

	class TeamMakerMenu(
		containerId: Int,
		inventory: Inventory,
		private val service: TaskTussleService,
		private val selection: TeamSelection,
	) : ActionChestMenu(containerId, inventory, teamRows(selection.teamCount)) {
		init { refresh() }
		fun refresh() {
			clear(); fill()
			for (index in 1..selection.teamCount) {
				val memberIds = selection.members.filterValues { it == index }.keys
				val players = checkNotNull(playerInventory.player.level().server).playerList.players.filter { it.uuid in memberIds }
				val occupied = players.isNotEmpty()
				val selected = selection.members[playerInventory.player.uuid] == index
				button(index - 1, named(teamItem(index, occupied), "Team $index", teamColor(index), if (occupied) listOf(players.joinToString { it.gameProfile.name }) else listOf("Empty"), max(1, players.size), selected)) { player, _ ->
					service.toggleTeam(player, index)
				}
			}
			broadcastChanges()
		}
	}

	private val TEAMS_MODEL = Identifier.fromNamespaceAndPath("tasktussle", "teams")
	private val TASK_COMPLETE_MODEL = Identifier.fromNamespaceAndPath("tasktussle", "task_complete")
	private val CLICK_MODELS = (1..3).map { Identifier.fromNamespaceAndPath("tasktussle", "click_me$it") }
	private val ratioLore = listOf("L Click: Increase ratio", "R Click: Decrease ratio", "Shift+R Click: Set ratio to 0", "A ratio of 0 turns it off")
	private val taskLore = listOf("Shift+L Click: Open details") + ratioLore
	private val mutualLore = listOf("L Click: Increase amount", "R Click: Decrease amount", "Shift+L Click: Set amount to 25", "Shift+R Click: Set amount to 0")

	private fun ratioChange(value: Int, click: Click): Int = when { click.right && click.shift -> 0; click.right -> max(0, value - 1); else -> minOf(100, value + 1) }
	private fun simpleChange(value: Int, click: Click, min: Int, max: Int): Int = (value + if (click.right) -1 else 1).coerceIn(min, max)

	private fun taskStack(service: TaskTussleService, task: BingoTask): ItemStack = when {
		task.completed -> named(Items.PAPER, service.taskName(task), ChatFormatting.GREEN, listOf("Completed") + task.contributors).apply {
			set(DataComponents.ITEM_MODEL, TASK_COMPLETE_MODEL)
		}
		task.failed -> named(Blocks.STAINED_GLASS_PANE.red().asItem(), service.taskName(task), ChatFormatting.RED, listOf("Failed"), enchanted = true)
		else -> named(Items.PAPER, service.taskName(task), ChatFormatting.WHITE).apply {
			set(DataComponents.ITEM_MODEL, if (task.template.kind == TaskKind.CLICK) CLICK_MODELS.random() else BuiltInRegistries.ITEM.getKey(taskItem(task)))
			val kind = when (task.template.kind) { TaskKind.BLOCK -> "Obtain Task"; TaskKind.ADVANCEMENT -> "Advancements Task"; TaskKind.CLICK -> "Click Task" }
			val description = when (task.template.kind) { TaskKind.BLOCK -> "obtain this block"; TaskKind.ADVANCEMENT -> "complete this advancement"; TaskKind.CLICK -> "click me" }
			set(DataComponents.LORE, ItemLore(listOf(
				taskLoreLine("Task", kind, ChatFormatting.AQUA),
				taskLoreLine("Description", description, ChatFormatting.WHITE),
			)))
		}
	}

	private fun taskLoreLine(label: String, value: String, valueColor: ChatFormatting): Component =
		Component.literal("$label: ").withStyle { it.withColor(ChatFormatting.GRAY).withItalic(false) }
			.append(Component.literal(value).withStyle { it.withColor(valueColor).withItalic(false) })

	private fun taskItem(task: BingoTask): Item = when (task.template.kind) {
		TaskKind.BLOCK -> BuiltInRegistries.BLOCK.getValue(task.template.id).asItem()
		TaskKind.ADVANCEMENT -> Items.KNOWLEDGE_BOOK
		TaskKind.CLICK -> Items.BELL
	}

	private fun separator(): ItemStack = named(Blocks.STAINED_GLASS_PANE.gray().asItem(), " ", ChatFormatting.DARK_GRAY).apply {
		set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("tasktussle", "empty_slot"))
	}
	private fun emptyTeamSlot(): ItemStack = named(Blocks.STAINED_GLASS.gray().asItem(), "Empty team slot", ChatFormatting.GRAY)
	private fun emptyOption(type: String): ItemStack = named(Blocks.STAINED_GLASS.lightGray().asItem(), "Empty $type slot", ChatFormatting.GRAY, listOf("A spot for a future $type"))
	private fun emptySetting(): ItemStack = named(Blocks.STAINED_GLASS.lightGray().asItem(), "Empty setting slot", ChatFormatting.GRAY, listOf("a spot for a future setting"))

	private fun named(item: Item, name: String, color: ChatFormatting, lore: List<String> = emptyList(), count: Int = 1, enchanted: Boolean = false): ItemStack = ItemStack(item).apply {
		val visualCount = count.coerceIn(1, 99)
		if (visualCount > maxStackSize) set(DataComponents.MAX_STACK_SIZE, visualCount)
		this.count = visualCount
		set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle { it.withColor(color).withItalic(false) })
		if (lore.isNotEmpty()) set(DataComponents.LORE, ItemLore(lore.filter(String::isNotBlank).map { Component.literal(it).withStyle { style -> style.withColor(ChatFormatting.GRAY).withItalic(false) } }))
		if (enchanted) set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
	}

	private fun teamItem(index: Int, solid: Boolean): Item = when ((index - 1) % 10) {
		0 -> if (solid) Blocks.CONCRETE.blue().asItem() else Blocks.STAINED_GLASS.blue().asItem()
		1 -> if (solid) Blocks.CONCRETE.red().asItem() else Blocks.STAINED_GLASS.red().asItem()
		2 -> if (solid) Blocks.CONCRETE.green().asItem() else Blocks.STAINED_GLASS.green().asItem()
		3 -> if (solid) Blocks.CONCRETE.yellow().asItem() else Blocks.STAINED_GLASS.yellow().asItem()
		4 -> if (solid) Blocks.CONCRETE.magenta().asItem() else Blocks.STAINED_GLASS.magenta().asItem()
		5 -> if (solid) Blocks.CONCRETE.orange().asItem() else Blocks.STAINED_GLASS.orange().asItem()
		6 -> if (solid) Blocks.CONCRETE.cyan().asItem() else Blocks.STAINED_GLASS.cyan().asItem()
		7 -> if (solid) Blocks.CONCRETE.green().asItem() else Blocks.STAINED_GLASS.green().asItem()
		8 -> if (solid) Blocks.CONCRETE.purple().asItem() else Blocks.STAINED_GLASS.purple().asItem()
		else -> if (solid) Blocks.CONCRETE.cyan().asItem() else Blocks.STAINED_GLASS.cyan().asItem()
	}

	private fun teamColor(index: Int) = listOf(ChatFormatting.BLUE, ChatFormatting.RED, ChatFormatting.GREEN, ChatFormatting.YELLOW, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD, ChatFormatting.AQUA, ChatFormatting.DARK_GREEN, ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_AQUA)[(index - 1) % 10]
	private fun teamRows(count: Int): Int = minOf(6, count / 9 + 1)
	private fun pickerRows(count: Int): Int = if (count > 45) 6 else 1 + max(1, (count - 1) / 9 + 1)
	private fun menuType(rows: Int): MenuType<*> = when (rows) {
		1 -> MenuType.GENERIC_9x1; 2 -> MenuType.GENERIC_9x2; 3 -> MenuType.GENERIC_9x3
		4 -> MenuType.GENERIC_9x4; 5 -> MenuType.GENERIC_9x5; 6 -> MenuType.GENERIC_9x6
		else -> error("Unsupported row count: $rows")
	}
}

internal enum class MenuClickSound { BUTTON, PAGE }

internal fun menuClickSound(pageChanged: Boolean, contentsChanged: Boolean): MenuClickSound? = when {
	pageChanged -> MenuClickSound.PAGE
	contentsChanged -> MenuClickSound.BUTTON
	else -> null
}
