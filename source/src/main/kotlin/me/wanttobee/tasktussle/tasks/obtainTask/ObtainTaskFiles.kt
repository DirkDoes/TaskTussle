package me.wanttobee.tasktussle.tasks.obtainTask

import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.Difficulty
import me.wanttobee.tasktussle.base.tasks.ITaskFiles
import me.wanttobee.tasktussle.base.tasks.TaskPools
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object ObtainTaskFiles : ITaskFiles {
	override val prefix = "blocks_"
	override val defaults = listOf("blocks_default.yml", "blocks_balanced.yml")
	override val generatedFile = "blocks_all_26_2.yml"
	override val browserName = "Obtain Tasks"
	override val managerKey = "obtain"
	override fun selectedFile(settings: TaskTussleSettings) = settings.blocksFile
	override fun selectFile(settings: TaskTussleSettings, file: String) { settings.blocksFile = file }
	override fun accepts(server: MinecraftServer, id: Identifier) =
		BuiltInRegistries.BLOCK.containsKey(id) && BuiltInRegistries.BLOCK.getValue(id).asItem() != Items.AIR
	override fun available(server: MinecraftServer) = BuiltInRegistries.BLOCK.keySet().asSequence().filter { id ->
		val item = BuiltInRegistries.BLOCK.getValue(id).asItem()
		item != Items.AIR && BuiltInRegistries.ITEM.getKey(item) == id
	}
	override fun name(server: MinecraftServer?, id: Identifier) = BuiltInRegistries.BLOCK.getValue(id).name.string
	override fun icon(server: MinecraftServer?, settings: TaskTussleSettings, id: Identifier) = ItemStack(Items.PAPER).apply {
		set(DataComponents.ITEM_MODEL, BuiltInRegistries.ITEM.getKey(BuiltInRegistries.BLOCK.getValue(id).asItem()))
	}

	override fun generated(server: MinecraftServer): TaskPools {
		val grouped = BuiltInRegistries.BLOCK.keySet()
			.asSequence()
			.filter { id ->
				val item = BuiltInRegistries.BLOCK.getValue(id).asItem()
				item != Items.AIR && BuiltInRegistries.ITEM.getKey(item) == id && BlockDifficulty.isObtainable(id.path)
			}
			.sortedBy(Identifier::toString)
			.groupBy { BlockDifficulty.rank(it.path) }
		return TaskPools(
			grouped[Difficulty.EASY].orEmpty(),
			grouped[Difficulty.MEDIUM].orEmpty(),
			grouped[Difficulty.HARD].orEmpty(),
		)
	}
}
