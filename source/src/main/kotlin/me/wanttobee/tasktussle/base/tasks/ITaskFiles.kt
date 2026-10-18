package me.wanttobee.tasktussle.base.tasks

import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.ItemStack

/** File formats and storage are shared; each task type supplies its own catalog content. */
interface ITaskFiles {
	val prefix: String
	val defaults: List<String>
	val generatedFile: String
	val browserName: String
	val managerKey: String
	fun selectedFile(settings: TaskTussleSettings): String
	fun selectFile(settings: TaskTussleSettings, file: String)
	fun accepts(server: MinecraftServer, id: Identifier): Boolean
	fun available(server: MinecraftServer): Sequence<Identifier>
	fun generated(server: MinecraftServer): TaskPools
	fun name(server: MinecraftServer?, id: Identifier): String
	fun icon(server: MinecraftServer?, settings: TaskTussleSettings, id: Identifier): ItemStack
	fun description(server: MinecraftServer?, id: Identifier): String? = null
	fun load(server: MinecraftServer, file: String): TaskPools = TaskCatalog.load(server, this, file)
}
