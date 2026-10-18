package me.wanttobee.tasktussle.items

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.resourcepack.TaskTussleResourcePackServer
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData

class ForcedItems(private val system: TaskTussleSystem) {
	private val settings get() = system.settings
	private val game get() = system.currentGame()
	private val selection get() = system.teams.selection
	internal val deliverySpotLearned = mutableSetOf<java.util.UUID>()

	fun isCard(stack: ItemStack): Boolean = hasMarker(stack, CARD_MARKER)

	fun isTeamNavigator(stack: ItemStack): Boolean = hasMarker(stack, TEAM_NAVIGATOR_MARKER)

	fun isTeamFinisher(stack: ItemStack): Boolean = hasMarker(stack, TEAM_FINISHER_MARKER)

	fun isDeliverySpot(stack: ItemStack): Boolean = hasMarker(stack, DELIVERY_SPOT_MARKER)

	fun isSettingsTool(stack: ItemStack): Boolean = hasMarker(stack, SETTINGS_MARKER)

	fun isTaskBrowser(stack: ItemStack): Boolean = hasMarker(stack, TASK_BROWSER_MARKER)

	fun giveCard(player: ServerPlayer) = giveAt(
		player, 8, Items.PAPER, "Task Tussle Card", ChatFormatting.GREEN, CARD_MARKER,
		BINGO_CARD_MODEL.takeIf { TaskTussleResourcePackServer.active },
	)

	fun giveTeamNavigator(player: ServerPlayer) = giveAt(
		player, 8, Items.PAPER, "Teams", ChatFormatting.BLUE, TEAM_NAVIGATOR_MARKER,
		TEAMS_MODEL.takeIf { TaskTussleResourcePackServer.active },
	)

	fun giveTeamFinisher(player: ServerPlayer) = giveAt(player, 7, Items.FIRE_CHARGE, "Detonate Current Teams", ChatFormatting.GOLD, TEAM_FINISHER_MARKER)

	fun giveSettingsTool(player: ServerPlayer) {
		val displaced = player.inventory.getItem(SETTINGS_SLOT).takeUnless(::isSettingsTool)?.takeUnless(ItemStack::isEmpty)?.copy()
		giveAt(player, SETTINGS_SLOT, Items.COMPASS, "Task Tussle Settings", ChatFormatting.AQUA, SETTINGS_MARKER)
		if (displaced != null && !player.inventory.add(displaced)) player.drop(displaced, false)
	}

	fun giveTaskBrowser(player: ServerPlayer) {
		val displaced = player.inventory.getItem(TASK_BROWSER_SLOT).takeUnless(::isTaskBrowser)?.takeUnless(ItemStack::isEmpty)?.copy()
		giveAt(player, TASK_BROWSER_SLOT, Items.MAP, "Task Browser", ChatFormatting.GREEN, TASK_BROWSER_MARKER)
		if (displaced != null && !player.inventory.add(displaced)) player.drop(displaced, false)
	}

	fun giveDeliverySpot(player: ServerPlayer) {
		val displaced = player.inventory.getItem(DELIVERY_SPOT_SLOT).takeUnless(::isDeliverySpot)?.takeUnless(ItemStack::isEmpty)?.copy()
		giveAt(
			player, DELIVERY_SPOT_SLOT, Items.PAPER, "Delivery spot", ChatFormatting.GOLD, DELIVERY_SPOT_MARKER,
			if (player.uuid in deliverySpotLearned) DELIVERY_SPOT_MODEL else DELIVERY_SPOT_SIGN_MODEL,
		)
		if (displaced != null && !player.inventory.add(displaced)) player.drop(displaced, false)
	}

	fun giveAt(player: ServerPlayer, slot: Int, item: net.minecraft.world.item.Item, name: String, color: ChatFormatting, marker: String, model: Identifier? = null) {
		for (index in 0 until player.inventory.containerSize) if (hasMarker(player.inventory.getItem(index), marker)) player.inventory.setItem(index, ItemStack.EMPTY)
		player.inventory.setItem(slot, ItemStack(item).apply {
			set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle { it.withColor(color).withItalic(false) })
			set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().apply { putString(TOOL_MARKER, marker) }))
			if (model != null) set(DataComponents.ITEM_MODEL, model)
		})
	}

	fun removeGameItems(player: ServerPlayer) {
		for (slot in 0 until player.inventory.containerSize) {
			val stack = player.inventory.getItem(slot)
			if (isCard(stack) || isDeliverySpot(stack)) player.inventory.setItem(slot, ItemStack.EMPTY)
		}
	}

	fun removeTeamItems(player: ServerPlayer) {
		for (slot in 0 until player.inventory.containerSize) {
			val stack = player.inventory.getItem(slot)
			if (isTeamNavigator(stack) || isTeamFinisher(stack)) player.inventory.setItem(slot, ItemStack.EMPTY)
		}
	}

	fun removeSettingsTool(player: ServerPlayer) {
		for (slot in 0 until player.inventory.containerSize) if (isSettingsTool(player.inventory.getItem(slot))) player.inventory.setItem(slot, ItemStack.EMPTY)
	}

	fun removeTaskBrowser(player: ServerPlayer) {
		for (slot in 0 until player.inventory.containerSize) if (isTaskBrowser(player.inventory.getItem(slot))) player.inventory.setItem(slot, ItemStack.EMPTY)
	}

	fun restoreForcedItems(server: MinecraftServer) {
		if (game == null && selection == null) server.playerList.players.filter { !hasMarker(it, SETTINGS_MARKER) }.forEach(::giveSettingsTool)
		if (game == null && selection == null) server.playerList.players.filter { !hasMarker(it, TASK_BROWSER_MARKER) }.forEach(::giveTaskBrowser)
		game?.teams?.flatMap { it.members }?.toSet()?.let { members ->
			server.playerList.players.filter { it.uuid in members && !hasMarker(it, CARD_MARKER) }.forEach(::giveCard)
			if (settings.handInItems) server.playerList.players.filter { it.uuid in members && !hasMarker(it, DELIVERY_SPOT_MARKER) }.forEach(::giveDeliverySpot)
		}
		selection?.let { current ->
			server.playerList.players.filter { !hasMarker(it, TEAM_NAVIGATOR_MARKER) }.forEach(::giveTeamNavigator)
			server.playerList.getPlayer(current.starter)?.takeIf { !hasMarker(it, TEAM_FINISHER_MARKER) }?.let(::giveTeamFinisher)
		}
	}

	private fun hasMarker(stack: ItemStack, marker: String): Boolean =
		stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr(TOOL_MARKER, "") == marker
	private fun hasMarker(player: ServerPlayer, marker: String): Boolean =
		(0 until player.inventory.containerSize).any { hasMarker(player.inventory.getItem(it), marker) }

	companion object {
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

		@JvmStatic
		fun isForcedItem(stack: ItemStack): Boolean = isForcedMarker(stack.get(DataComponents.CUSTOM_DATA)?.copyTag()?.getStringOr(TOOL_MARKER, ""))

		internal fun isForcedMarker(marker: String?) =
			marker == CARD_MARKER || marker == TEAM_NAVIGATOR_MARKER || marker == TEAM_FINISHER_MARKER || marker == DELIVERY_SPOT_MARKER || marker == SETTINGS_MARKER || marker == TASK_BROWSER_MARKER

		fun clearNonForcedItems(player: ServerPlayer) {
			for (slot in 0 until player.inventory.containerSize) {
				if (!isForcedItem(player.inventory.getItem(slot))) player.inventory.setItem(slot, ItemStack.EMPTY)
			}
		}
	}
}
