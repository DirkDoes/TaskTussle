package me.wanttobee.tasktussle.tasks.achievementTask

import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.Difficulty
import me.wanttobee.tasktussle.base.tasks.ITaskFiles
import me.wanttobee.tasktussle.base.tasks.TaskPools
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer

object AdvancementsFiles : ITaskFiles {
	override val prefix = "advancements_"
	override val defaults = listOf("advancements_default.yml", "advancements_balanced.yml")
	override val generatedFile = "advancements_all_26_2.yml"
	override val browserName = "Advancement Tasks"
	override val managerKey = "advancements"
	override fun selectedFile(settings: TaskTussleSettings) = settings.advancementsFile
	override fun selectFile(settings: TaskTussleSettings, file: String) { settings.advancementsFile = file }
	override fun accepts(server: MinecraftServer, id: Identifier) = server.advancements.get(id)?.value?.display?.isPresent == true
	override fun available(server: MinecraftServer) = server.advancements.allAdvancements.asSequence().filter { it.value.display.isPresent }.map { it.id }
	override fun name(server: MinecraftServer?, id: Identifier) = advancementName(server, id)
	override fun icon(server: MinecraftServer?, settings: TaskTussleSettings, id: Identifier) = advancementIcon(server, id)
	override fun description(server: MinecraftServer?, id: Identifier) = advancementDescription(server, id)

	override fun generated(server: MinecraftServer): TaskPools {
		val grouped = server.advancements.allAdvancements
			.asSequence()
			.filter { it.value.display.isPresent }
			.map { it.id }
			.filter { it.namespace == "minecraft" }
			.sortedBy(Identifier::toString)
			.groupBy { AdvancementDifficulty.rank(it.path) }
		return TaskPools(
			grouped[Difficulty.EASY].orEmpty(),
			grouped[Difficulty.MEDIUM].orEmpty(),
			grouped[Difficulty.HARD].orEmpty(),
		)
	}
}
