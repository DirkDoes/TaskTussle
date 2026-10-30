package me.wanttobee.tasktussle.base.generic

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.tasks.CatalogContentsMenu
import me.wanttobee.tasktussle.base.tasks.CatalogCreateMenu
import me.wanttobee.tasktussle.base.tasks.CatalogFile
import me.wanttobee.tasktussle.base.tasks.CatalogTypeMenu
import me.wanttobee.tasktussle.base.tasks.Difficulty
import me.wanttobee.tasktussle.base.tasks.FilePickerMenu
import me.wanttobee.tasktussle.base.tasks.ITaskFiles
import me.wanttobee.tasktussle.base.tasks.TaskCatalog
import me.wanttobee.tasktussle.base.tasks.TaskPools
import me.wanttobee.tasktussle.base.tasks.mayCreateCatalog
import me.wanttobee.tasktussle.base.tasks.mayEditCatalog
import me.wanttobee.tasktussle.base.tasks.moveTaskBetweenTiers
import me.wanttobee.tasktussle.util.message
import me.wanttobee.tasktussle.resourcepack.TaskTussleResourcePackServer
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions
import net.minecraft.world.MenuProvider
import net.minecraft.world.SimpleMenuProvider

class TaskMenus(private val system: TaskTussleSystem) {
	private val settingsGames = mutableMapOf<java.util.UUID, String>()
	fun rememberSettingsGame(player: ServerPlayer, key: String) { settingsGames[player.uuid] = key }
	fun forgetSettingsGame(player: ServerPlayer) { settingsGames.remove(player.uuid) }
	private fun settingsGame(player: ServerPlayer) = settingsGames[player.uuid] ?: game?.manager?.key ?: system.gameManagers.first().key
	private val settings get() = system.settings
	private val game get() = system.currentGame()
	private val selection get() = system.teams.selection
	private val server get() = system.server

	fun canEditSettings(player: ServerPlayer): Boolean = isOperator(player)

	fun openSettings(player: ServerPlayer) = openMenu(player, SimpleMenuProvider(
		{ id, inventory, _ -> SettingsMenu(id, inventory, system, !canEditSettings(player), settingsGame(player)) }, SettingsMenu.title(TaskTussleResourcePackServer.active),
	))

	fun openManagerSettings(player: ServerPlayer, manager: String) = openMenu(player, SimpleMenuProvider(
		{ id, inventory, _ -> system.manager(manager).settingsMenu(id, inventory, system) },
		Component.literal("${manager.replaceFirstChar(Char::uppercase)} Settings"),
	))

	fun openTimeSettings(player: ServerPlayer) = openMenu(player, SimpleMenuProvider(
		{ id, inventory, _ -> TimeSettingsMenu(id, inventory, system) }, Component.literal("Time Settings"),
	))

	fun openTaskBrowser(player: ServerPlayer): Boolean {
		if (game != null || selection != null) return false
		openMenu(player, SimpleMenuProvider(
			{ id, inventory, _ -> CatalogTypeMenu(id, inventory, system) }, Component.literal("Task Browser"),
		))
		return true
	}

	fun openCatalogFiles(player: ServerPlayer, catalog: ITaskFiles, selected: String? = null): Boolean {
		if (game != null || selection != null) return false
		openMenu(player, SimpleMenuProvider(
			{ id, inventory, _ -> FilePickerMenu(id, inventory, system, catalog, true, selected) }, Component.literal("Task Files"),
		))
		return true
	}

	fun returnToCatalogFiles(player: ServerPlayer, catalog: ITaskFiles, selected: String) = system.after(player.level().server, 1) {
		if (player.containerMenu === player.inventoryMenu) openCatalogFiles(player, catalog, selected)
	}

	fun openFilePicker(player: ServerPlayer, catalog: ITaskFiles) = openMenu(player, SimpleMenuProvider(
		{ id, inventory, _ -> FilePickerMenu(id, inventory, system, catalog) }, Component.literal("File Name picker"),
	))

	fun openCatalogCreation(player: ServerPlayer, catalog: ITaskFiles): Boolean {
		if (game != null || selection != null) return false
		openMenu(player, SimpleMenuProvider(
			{ id, inventory, _ -> CatalogCreateMenu(id, inventory, system, catalog) }, Component.literal("Create Task File"),
		))
		return true
	}

	fun openCatalogContents(player: ServerPlayer, catalog: ITaskFiles, file: String): Boolean {
		if (game != null || selection != null) return false
		val pools = runCatching { catalog.load(player.level().server, file) }
			.getOrElse { return system.fail(player, it.message ?: "Invalid task file") }
		val removed = TaskCatalog.removed(player.level().server, catalog, pools)
		openMenu(player, SimpleMenuProvider(
			{ id, inventory, _ -> CatalogContentsMenu(id, inventory, system, catalog, file, pools, removed) }, Component.literal(file),
		))
		return true
	}

	fun catalogFiles(catalog: ITaskFiles): List<CatalogFile> = TaskCatalog.catalogFiles(catalog)

	fun canEditCatalog(player: ServerPlayer, catalog: ITaskFiles, file: String): Boolean = runCatching {
		mayEditCatalog(TaskCatalog.owner(catalog, file), player.uuid, isOperator(player))
	}.getOrDefault(false)

	fun createCatalog(player: ServerPlayer, catalog: ITaskFiles, generated: Boolean): Boolean {
		val pools = if (generated) catalog.generated(player.level().server) else TaskPools(emptyList(), emptyList(), emptyList())
		val file = saveCatalog(player, catalog, pools) ?: return false
		return openCatalogFiles(player, catalog, file)
	}

	fun duplicateCatalog(player: ServerPlayer, catalog: ITaskFiles, source: String): String? {
		val pools = runCatching { catalog.load(player.level().server, source) }
			.getOrElse { system.fail(player, it.message ?: "Could not duplicate task file"); return null }
		return saveCatalog(player, catalog, pools)
	}

	fun moveCatalogTask(player: ServerPlayer, catalog: ITaskFiles, file: String, id: Identifier, target: Difficulty?): Pair<TaskPools, List<Identifier>>? {
		if (!canEditCatalog(player, catalog, file)) { system.fail(player, "You cannot edit this task file."); return null }
		val current = runCatching { catalog.load(player.level().server, file) }
			.getOrElse { system.fail(player, it.message ?: "Could not edit task file"); return null }
		val updated = moveTaskBetweenTiers(current, id, target)
		runCatching { TaskCatalog.update(catalog, file, updated) }.getOrElse { system.fail(player, it.message ?: "Could not save task file"); return null }
		if (file == catalog.selectedFile(settings)) runCatching(system::reloadPools)
		return updated to TaskCatalog.removed(player.level().server, catalog, updated)
	}

	fun deleteCatalog(player: ServerPlayer, catalog: ITaskFiles, file: String): Boolean {
		if (!canEditCatalog(player, catalog, file)) return system.fail(player, "You do not own this task file.")
		runCatching { TaskCatalog.delete(catalog, file) }.getOrElse { return system.fail(player, it.message ?: "Could not delete task file") }
		val current = catalog.selectedFile(settings)
		if (current == file) TaskCatalog.catalogFiles(catalog).firstOrNull()?.name?.let { replacement ->
			catalog.selectFile(settings, replacement)
			runCatching(system::reloadPools)
		}
		return true
	}

	private fun saveCatalog(player: ServerPlayer, catalog: ITaskFiles, pools: TaskPools): String? {
		val operator = isOperator(player)
		val owned = TaskCatalog.ownedFiles(player.uuid, system.taskFactory.managers.mapNotNull { it.files })
		val total = TaskCatalog.totalFiles(catalog)
		if (!mayCreateCatalog(owned, total, operator)) {
			system.fail(player, if (total >= 36) "This task list already has 36 files." else "You can create at most 3 task files.")
			return null
		}
		return runCatching { TaskCatalog.create(catalog, player.uuid, player.gameProfile.name, pools) }
			.getOrElse { system.fail(player, it.message ?: "Could not create task file"); null }
	}

	private fun isOperator(player: ServerPlayer) = player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)

	fun selectCatalog(player: ServerPlayer, catalog: ITaskFiles, file: String): Boolean {
		if (!canEditSettings(player)) return false
		val old = catalog.selectedFile(settings)
		catalog.selectFile(settings, file)
		return runCatching(system::reloadPools).fold(
			onSuccess = { true },
			onFailure = {
				catalog.selectFile(settings, old)
				system.fail(player, it.message ?: "Invalid task file")
			},
		)
	}

	internal fun openMenu(player: ServerPlayer, provider: MenuProvider) = player.containerMenu.takeUnless { it === player.inventoryMenu }?.let { previous ->
		player.containerMenu = player.inventoryMenu
		player.openMenu(provider).also { opened ->
			if (opened.isEmpty) player.containerMenu = previous
			else { previous.removed(player); player.inventoryMenu.transferState(previous) }
		}
	} ?: player.openMenu(provider)
}
