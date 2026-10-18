package me.wanttobee.tasktussle.tasks.achievementTask

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.games.ITTGameTeam
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.ITaskManager
import me.wanttobee.tasktussle.base.tasks.TaskKind
import me.wanttobee.tasktussle.base.tasks.TaskTemplate
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items
import net.minecraft.world.level.gamerules.GameRules

object AdvancementsTaskManager : ITaskManager {
	override val kind = TaskKind.ADVANCEMENT
	override val name = "Advancements Task"
	override val icon get() = Items.KNOWLEDGE_BOOK
	override val files = AdvancementsFiles
	override fun weight(settings: TaskTussleSettings) = settings.advancementWeight
	override fun setWeight(settings: TaskTussleSettings, value: Int) { settings.advancementWeight = value }
	override fun create(template: TaskTemplate) = AdvancementsTask(template)
	override fun settingsMenu(id: Int, inventory: Inventory, system: TaskTussleSystem) = AdvancementSettingsMenu(id, inventory, system)
	override fun settingsLore(settings: TaskTussleSettings) = listOf("File: ${settings.advancementsFile}", "Icons: ${if (settings.customAdvancementIcons) "custom" else "recipe books"}")

	override fun prepare(server: MinecraftServer, teams: List<ITTGameTeam>, settings: TaskTussleSettings) {
		if (settings.cardVisibility != "visible") server.allLevels.forEach { level ->
			level.gameRules.set(GameRules.SHOW_ADVANCEMENT_MESSAGES, false, server)
		}
		for (player in server.playerList.players.filter { candidate -> teams.any { candidate.uuid in it.members } }) {
			for (advancement in server.advancements.allAdvancements) {
				val progress = player.advancements.getOrStartProgress(advancement)
				progress.completedCriteria.toList().forEach { player.advancements.revoke(advancement, it) }
			}
		}
	}
}
