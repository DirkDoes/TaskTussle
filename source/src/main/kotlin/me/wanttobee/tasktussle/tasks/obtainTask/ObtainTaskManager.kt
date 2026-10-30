package me.wanttobee.tasktussle.tasks.obtainTask

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.ITaskManager
import me.wanttobee.tasktussle.base.tasks.TaskKind
import me.wanttobee.tasktussle.base.tasks.TaskTemplate
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items

object ObtainTaskManager : ITaskManager {
	override val kind = TaskKind.BLOCK
	override val name = "Obtain Task"
	override val icon get() = Items.SHULKER_SHELL
	override val files = ObtainTaskFiles
	override fun weight(settings: TaskTussleSettings) = settings.blockWeight
	override fun setWeight(settings: TaskTussleSettings, value: Int) { settings.blockWeight = value }
	override fun create(template: TaskTemplate) = ObtainTask(template)
	override fun settingsMenu(id: Int, inventory: Inventory, system: TaskTussleSystem) = ObtainSettingsMenu(id, inventory, system)
	override fun settingsLore(settings: TaskTussleSettings) = listOf("File: ${settings.blocksFile}")
	override fun explanation(settings: TaskTussleSettings): String? = if (settings.blockWeight <= 0) null else
		"(Obtain Task) Obtain the shown block."
}
