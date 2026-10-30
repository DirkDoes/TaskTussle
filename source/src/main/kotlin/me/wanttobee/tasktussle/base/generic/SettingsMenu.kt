package me.wanttobee.tasktussle.base.generic

import kotlin.math.max
import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.pvp.PvpMode
import me.wanttobee.tasktussle.teams.TEAMS_MODEL
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.item.component.ItemLore
import java.util.LinkedHashSet

class SettingsMenu(
	containerId: Int,
	inventory: Inventory,
	private val system: TaskTussleSystem,
	private var readOnly: Boolean = false,
	private var gameKey: String = system.currentGame()?.manager?.key ?: system.gameManagers.first().key,
) : ActionChestMenu(containerId, inventory, 6) {
	override val customBackground = true
	private var displayedSettings = system.settings.copy()
	private var displayedCanStart = false
	init { refresh() }

	override fun broadcastChanges() {
		val viewer = playerInventory.player as? ServerPlayer
		val nextReadOnly = viewer?.let { !system.menus.canEditSettings(it) } ?: readOnly
		if (readOnly != nextReadOnly || displayedSettings != system.settings || displayedCanStart != canStart()) {
			readOnly = nextReadOnly
			refresh()
		}
		else super.broadcastChanges()
	}

	private fun canStart() = !readOnly && system.currentGame() == null && system.teams.selection == null

	internal fun selectGame(key: String) {
		if (system.gameManagers.none { it.key == key }) return
		gameKey = key
		refresh()
	}

	private fun refresh() {
		displayedSettings = system.settings.copy()
		displayedCanStart = canStart()
		clear()
		val settings = system.settings
		// shortcut: six game rows are visible; add sidebar pagination when more are registered.
		val visibleGames = system.gameManagers.take(6)
		val manager = visibleGames.firstOrNull { it.key == gameKey } ?: visibleGames.first()
		visibleGames.forEachIndexed { row, game ->
			val selected = game == manager
			val tab = named(game.icon, game.name, ChatFormatting.LIGHT_PURPLE, enchanted = selected).apply {
				game.iconModel?.let { set(DataComponents.ITEM_MODEL, it) }
				set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.MAP_ID, true))
			}
			val select: (ServerPlayer, Click) -> Unit = { player, _ ->
				selectGame(game.key)
				system.menus.rememberSettingsGame(player, game.key)
			}
			button(row * 9, tab, select)
			val marker = if (selected) named(Items.PAPER, " ", ChatFormatting.WHITE).apply {
				set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("tasktussle", "game_marker"))
				set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay(true, LinkedHashSet()))
			} else ItemStack.EMPTY
			button(row * 9 + 1, marker, select)
		}
		setting(2, Items.PAPER, "Choose teams: ${if (settings.chooseTeams) "on" else "off"}", listOf("On: Players choose their team", "Off: Teams are assigned randomly"), enchanted = settings.chooseTeams, model = TEAMS_MODEL) { _, _ -> settings.chooseTeams = !settings.chooseTeams }
		setting(3, settings.pvpMode.icon, "PvP: ${settings.pvpMode.label}", listOf("Click: Change cross-team PvP", "Teammate damage is always enabled"), model = settings.pvpMode.iconModel) { player, _ ->
			settings.pvpMode = PvpMode.entries[(settings.pvpMode.ordinal + 1) % PvpMode.entries.size]
			system.pvp.refreshNames(player.level().server)
			player.level().server.playerList.players.forEach(system.items::syncPvpToggle)
		}
		setting(4, Items.CLOCK, "Game Time: ${if (settings.gameMinutes == 0) "disabled" else "${settings.gameMinutes} minutes"}", listOf("Click: Adjust minutes"), enchanted = settings.gameMinutes > 0) { player, _ -> system.menus.openTimeSettings(player) }
		setting(5, Items.PAPER, "Teams: ${settings.teamCount}", listOf("L Click: Add a team", "R Click: Remove a team", "${manager.teamRange.first}–${manager.teamRange.last} teams"), settings.teamCount, model = TEAMS_MODEL) { _, click ->
			settings.adjustTeamCount(if (click.right) -1 else 1, manager.teamRange)
		}
		if (displayedCanStart) button(8, named(Items.PAPER, "Play ${manager.name}", ChatFormatting.GREEN,
			listOf("${settings.teamCount} teams", "Click: Start game")).apply {
			set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("tasktussle", "play_button"))
		}) { player, _ ->
			if (!system.menus.canEditSettings(player) || !canStart()) return@button
			val started = system.start(player.level().server, player, settings.teamCount, manager)
			if (started && player.containerMenu === this) player.closeContainer()
		}
		val ruleSlots = (11..17).toList() + (29..35).toList() + (38..44).toList()
		manager.settingsControls(system).zip(ruleSlots).forEach { (control, slot) -> renderControl(slot, control) }
		setting(20, Items.BRICK, "Easy Ratio: ${settings.easyRatio}", ratioLore, settings.easyRatio, settings.easyRatio > 0) { _, click -> settings.easyRatio = ratioChange(settings.easyRatio, click) }
		setting(21, Items.NETHER_BRICK, "Normal Ratio: ${settings.mediumRatio}", ratioLore, settings.mediumRatio, settings.mediumRatio > 0) { _, click -> settings.mediumRatio = ratioChange(settings.mediumRatio, click) }
		setting(22, Items.NETHERITE_INGOT, "Hard Ratio: ${settings.hardRatio}", ratioLore, settings.hardRatio, settings.hardRatio > 0) { _, click -> settings.hardRatio = ratioChange(settings.hardRatio, click) }
		// shortcut: seven task types fit beside the sidebar; add paging if that row fills up.
		system.taskFactory.managers.take(7).forEachIndexed { index, task ->
			val weight = task.weight(settings)
			val stack = named(task.icon, "${task.name} Ratio: $weight", ChatFormatting.AQUA,
				listOf(if (weight == 0) "Disabled" else "Weight: $weight") + task.settingsLore(settings) + if (readOnly) emptyList() else listOf("Click: Edit this task type"),
				max(1, weight), weight > 0).apply { task.iconModel?.let { set(DataComponents.ITEM_MODEL, it) } }
			if (readOnly) display(47 + index, stack) else button(47 + index, stack) { player, _ ->
				if (system.menus.canEditSettings(player)) system.menus.openManagerSettings(player, task.key)
			}
		}
		super.broadcastChanges()
	}

	private fun setting(slot: Int, item: Item, title: String, lore: List<String> = emptyList(), count: Int = 1, enchanted: Boolean = false, model: Identifier? = null, action: (ServerPlayer, Click) -> Unit = { _, _ -> }) {
		renderControl(slot, settingControl(item, title, lore, count, enchanted, model, action))
	}

	private fun renderControl(slot: Int, control: SettingControl) {
		if (readOnly) {
			val stack = control.stack.copy()
			stack.get(DataComponents.LORE)?.let { lore ->
				stack.set(DataComponents.LORE, ItemLore(lore.lines.filterNot { it.string.contains("click", ignoreCase = true) }))
			}
			display(slot, stack)
		} else button(slot, control.stack) { player, click ->
			if (!system.menus.canEditSettings(player)) return@button
			control.action(player, click)
			if (player.containerMenu === this) refresh()
		}
	}

	companion object {
		fun title(textured: Boolean): Component = if (!textured) Component.empty() else
			Component.literal("\uF800\uF801").withStyle {
				it.withFont(FontDescription.Resource(Identifier.fromNamespaceAndPath("tasktussle", "game_selector")))
					.withColor(ChatFormatting.WHITE).withItalic(false)
			}
	}
}
