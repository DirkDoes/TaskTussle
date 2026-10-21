package me.wanttobee.tasktussle.base.generic

import kotlin.math.max
import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.tasks.ITaskManager
import me.wanttobee.tasktussle.pvp.PvpMode
import me.wanttobee.tasktussle.teams.TEAMS_MODEL
import me.wanttobee.tasktussle.util.message
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

class SettingsMenu(
	containerId: Int,
	inventory: Inventory,
	private val system: TaskTussleSystem,
	private val readOnly: Boolean = false,
) : ActionChestMenu(containerId, inventory, 5) {
	private var displayedSettings = system.settings.copy()
	init { refresh() }

	override fun broadcastChanges() {
		if (displayedSettings != system.settings) refresh()
		else super.broadcastChanges()
	}

	private fun refresh() {
		displayedSettings = system.settings.copy()
		clear(); fill()
		val settings = system.settings
		val visibilityItem = when (settings.cardVisibility) {
			"visible" -> Items.ENDER_EYE
			"hidden" -> Items.FIREWORK_STAR
			else -> Items.ENDER_PEARL
		}
		setting(0, visibilityItem, "Card Visibility: ${settings.cardVisibility}") { _, _ ->
			val options = listOf("progress only", "visible", "hidden")
			settings.cardVisibility = options[(options.indexOf(settings.cardVisibility) + 1) % options.size]
			system.refreshCards()
		}
		setting(1, Items.BRICK, "Easy Ratio: ${settings.easyRatio}", ratioLore, settings.easyRatio, settings.easyRatio > 0) { _, click -> settings.easyRatio = ratioChange(settings.easyRatio, click) }
		setting(2, Items.NETHER_BRICK, "Normal Ratio: ${settings.mediumRatio}", ratioLore, settings.mediumRatio, settings.mediumRatio > 0) { _, click -> settings.mediumRatio = ratioChange(settings.mediumRatio, click) }
		setting(3, Items.NETHERITE_INGOT, "Hard Ratio: ${settings.hardRatio}", ratioLore, settings.hardRatio, settings.hardRatio > 0) { _, click -> settings.hardRatio = ratioChange(settings.hardRatio, click) }
		setting(4, Items.PAPER, "Chose Teams: ${if (settings.chooseTeams) "on" else "off"}", enchanted = settings.chooseTeams, model = TEAMS_MODEL) { _, _ -> settings.chooseTeams = !settings.chooseTeams }
		setting(5, settings.pvpMode.icon, "PvP: ${settings.pvpMode.label}", listOf("Click: Change cross-team PvP", "Teammate damage is always enabled"), model = settings.pvpMode.iconModel) { player, _ ->
			settings.pvpMode = PvpMode.entries[(settings.pvpMode.ordinal + 1) % PvpMode.entries.size]
			system.pvp.refreshNames(player.level().server)
			player.level().server.playerList.players.forEach(system.items::syncPvpToggle)
		}
		setting(6, Items.CLOCK, "Game Time: ${if (settings.gameMinutes == 0) "disabled" else "${settings.gameMinutes} minutes"}", enchanted = settings.gameMinutes > 0) { player, _ -> system.menus.openTimeSettings(player) }
		val preset = named(Items.WRITABLE_BOOK, "Load a pre-set", ChatFormatting.GOLD)
		if (readOnly) display(8, preset) else button(8, preset) { player, _ -> player.sendSystemMessage(message("Does nothing yet", ChatFormatting.RED)) }

		for (slot in 18 until 27) display(slot, emptyOption("task type"))
		for (slot in 36 until 45) display(slot, emptyOption("game mode"))
		// shortcut: each manager row has nine slots; paginate when either registry exceeds nine entries.
		system.taskFactory.managers.forEachIndexed { index, manager -> taskManager(18 + index, manager) }
		system.gameManagers.forEachIndexed { index, manager ->
			val lore = if (readOnly) manager.settingsLore(settings) else listOf("Click: Open details")
			val stack = named(manager.icon, manager.name, ChatFormatting.LIGHT_PURPLE, lore)
			if (readOnly) display(36 + index, stack) else button(36 + index, stack) { player, _ -> system.menus.openManagerSettings(player, manager.key) }
		}
		super.broadcastChanges()
	}

	private fun taskManager(slot: Int, manager: ITaskManager) {
		val weight = manager.weight(system.settings)
		val lore = if (!readOnly) taskLore else manager.settingsLore(system.settings)
		val stack = named(manager.icon, "${manager.name} Ratio: ${if (weight == 0) "Disabled" else weight}", ChatFormatting.AQUA, lore, max(1, weight), weight > 0).apply {
			manager.iconModel?.let { set(DataComponents.ITEM_MODEL, it) }
		}
		if (readOnly) { display(slot, stack); return }
		button(slot, stack) { player, click ->
			if (click.left && click.shift) system.menus.openManagerSettings(player, manager.key)
			else {
				val value = when { click.right && click.shift -> 0; click.right -> max(0, weight - 1); else -> minOf(100, weight + 1) }
				manager.setWeight(system.settings, value)
				refresh()
			}
		}
	}

	private fun setting(slot: Int, item: Item, title: String, lore: List<String> = emptyList(), count: Int = 1, enchanted: Boolean = false, model: Identifier? = null, action: (ServerPlayer, Click) -> Unit = { _, _ -> }) {
		val stack = named(item, title, ChatFormatting.GOLD, if (readOnly) emptyList() else lore, max(1, count), enchanted).apply {
			remove(DataComponents.ATTRIBUTE_MODIFIERS)
			if (model != null) set(DataComponents.ITEM_MODEL, model)
		}
		if (readOnly) display(slot, stack) else button(slot, stack) { player, click -> action(player, click); if (player.containerMenu === this) refresh() }
	}
}
