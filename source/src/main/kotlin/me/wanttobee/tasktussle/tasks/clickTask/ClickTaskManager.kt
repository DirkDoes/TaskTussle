package me.wanttobee.tasktussle.tasks.clickTask

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.Difficulty
import me.wanttobee.tasktussle.base.tasks.ITaskManager
import me.wanttobee.tasktussle.base.tasks.TaskKind
import me.wanttobee.tasktussle.base.tasks.TaskTemplate
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items

object ClickTaskManager : ITaskManager {
	override val kind = TaskKind.CLICK
	override val name = "Click Task"
	override val icon get() = Items.PAPER
	override val settingsIcon get() = Items.BELL
	override val iconModel = Identifier.fromNamespaceAndPath("tasktussle", "click_me1")
	override fun weight(settings: TaskTussleSettings) = settings.clickWeight
	override fun setWeight(settings: TaskTussleSettings, value: Int) { settings.clickWeight = value }
	override fun create(template: TaskTemplate) = ClickTask(template)
	override fun repeatableTemplate() = TaskTemplate(kind, Identifier.fromNamespaceAndPath("tasktussle", "click"), Difficulty.EASY)
	override fun settingsMenu(id: Int, inventory: Inventory, system: TaskTussleSystem) = ClickSettingsMenu(id, inventory, system)
}
