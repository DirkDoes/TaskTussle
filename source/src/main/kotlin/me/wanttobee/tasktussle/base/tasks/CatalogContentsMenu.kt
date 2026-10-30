package me.wanttobee.tasktussle.base.tasks

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.ActionChestMenu
import me.wanttobee.tasktussle.base.generic.named
import me.wanttobee.tasktussle.base.generic.separator
import me.wanttobee.tasktussle.util.wrapLore
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class CatalogContentsMenu(
	containerId: Int,
	inventory: Inventory,
	private val system: TaskTussleSystem,
	private val catalog: ITaskFiles,
	private val file: String,
	private var pools: TaskPools,
	private var removedTasks: List<Identifier>,
) : ActionChestMenu(containerId, inventory, 6) {
	private var offset = 0
	private var editing: Identifier? = null
	init { refresh() }

	private fun refresh() {
		clear()
		val tierOffsets = catalogTierOffsets(pools, removedTasks)
		val rows = buildList<List<Pair<ItemStack, Identifier?>>> {
			for (difficulty in Difficulty.entries) {
				add(List(8) { column -> tierLine(difficulty, column) to null })
				pools[difficulty].chunked(8).forEach { ids -> add(ids.map { catalogEntry(it) to it }) }
			}
			add(List(8) { column -> tierLine("removed", "Removed Items", ChatFormatting.GRAY, column) to null })
			removedTasks.chunked(8).forEach { ids -> add(ids.map { catalogEntry(it) to it }) }
		}
		val maxOffset = catalogMaxOffset(pools, removedTasks)
		offset = offset.coerceIn(0, maxOffset)
		val editable = system.menus.canEditCatalog(playerInventory.player as ServerPlayer, catalog, file)
		rows.drop(offset).take(6).forEachIndexed { row, cells ->
			cells.forEachIndexed { column, (stack, id) ->
				val slot = row * 9 + column
				if (editable && id != null) button(slot, stack) { _, _ -> editing = id; refresh() }
				else display(slot, stack)
			}
		}
		Difficulty.entries.forEachIndexed { index, difficulty ->
			button(17 + index * 9, tierDot(tierId(difficulty), "${difficulty.yamlName} Items", tierColor(difficulty))) { _, _ -> offset = tierOffsets[index]; refresh() }
		}
		button(44, tierDot("removed", "Removed Items", ChatFormatting.GRAY)) { _, _ -> offset = tierOffsets.last(); refresh() }
		if (offset > 0) button(8, arrow(up = true, enabled = true)) { _, click -> offset -= if (click.right) 2 else 1; refresh() }
		else display(8, arrow(up = true, enabled = false))
		if (offset < maxOffset) button(53, arrow(up = false, enabled = true)) { _, click -> offset += if (click.right) 2 else 1; refresh() }
		else display(53, arrow(up = false, enabled = false))
		editing?.let(::modal)
		broadcastChanges()
	}

	private fun modal(id: Identifier) {
		clearActions()
		for (column in 1..6) {
			display(9 + column, separator())
			display(36 + column, separator())
		}
		for (row in 2..3) {
			display(row * 9 + 1, separator())
			display(row * 9 + 6, separator())
		}
		display(20, separator())
		display(21, catalogEntry(id))
		button(22, named(Items.BARRIER, "Cancel", ChatFormatting.RED)) { _, _ -> editing = null; refresh() }
		display(23, separator())
		Difficulty.entries.forEachIndexed { index, difficulty ->
			button(29 + index, tierDot(tierId(difficulty), "Move to ${difficulty.yamlName}", tierColor(difficulty))) { player, _ -> move(player, id, difficulty) }
		}
		button(32, tierDot("removed", "Move to Removed", ChatFormatting.GRAY)) { player, _ -> move(player, id, null) }
	}

	private fun move(player: ServerPlayer, id: Identifier, target: Difficulty?) {
		system.menus.moveCatalogTask(player, catalog, file, id, target)?.let { (updated, removed) ->
			pools = updated
			removedTasks = removed
			editing = null
			refresh()
		}
	}

	override fun removed(player: Player) {
		super.removed(player)
		val serverPlayer = player as? ServerPlayer ?: return
		system.menus.returnToCatalogFiles(serverPlayer, catalog, file)
	}

	private fun tierLine(difficulty: Difficulty, column: Int) = tierLine(
		tierId(difficulty), "${difficulty.yamlName} Items", tierColor(difficulty), column,
	)

	private fun tierLine(tier: String, title: String, color: ChatFormatting, column: Int): ItemStack {
		val part = when (column) { 0 -> "start"; 7 -> "end"; else -> "straight" }
		return named(Items.PAPER, title, color).apply {
			set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("tasktussle", "tier_${tier}_$part"))
		}
	}

	private fun tierDot(tier: String, title: String, color: ChatFormatting) = named(Items.PAPER, title, color).apply {
		set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("tasktussle", "tier_${tier}_dot"))
	}

	private fun arrow(up: Boolean, enabled: Boolean): ItemStack = named(
		Items.PAPER, if (up) "Scroll up" else "Scroll down", if (enabled) ChatFormatting.WHITE else ChatFormatting.GRAY,
	).apply { set(DataComponents.ITEM_MODEL, if (up) ARROW_UP_MODEL else ARROW_DOWN_MODEL) }

	private fun catalogEntry(id: Identifier): ItemStack {
		val lore = catalog.description(system.server, id)?.let(::wrapLore).orEmpty()
		return named(catalog.icon(system.server, system.settings, id), catalog.name(system.server, id), ChatFormatting.WHITE, lore)
	}
}
