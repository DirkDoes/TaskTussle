package me.wanttobee.tasktussle.tasks.statusEffectTask

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.ITaskManager
import me.wanttobee.tasktussle.base.tasks.TaskKind
import me.wanttobee.tasktussle.base.tasks.TaskTemplate
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items

object StatusEffectTaskManager : ITaskManager {
	override val kind = TaskKind("status_effect")
	override val name = "Status Effect Task"
	override val icon get() = Items.PAPER
	override val iconModel = Identifier.fromNamespaceAndPath("tasktussle", "status_effect/speed")
	override val files = StatusEffectTaskFiles
	override fun weight(settings: TaskTussleSettings) = settings.statusEffectWeight
	override fun setWeight(settings: TaskTussleSettings, value: Int) { settings.statusEffectWeight = value }
	override fun create(template: TaskTemplate) = StatusEffectTask(template)
	override fun settingsMenu(id: Int, inventory: Inventory, system: TaskTussleSystem) = StatusEffectSettingsMenu(id, inventory, system)
	override fun settingsLore(settings: TaskTussleSettings) = listOf("File: ${settings.statusEffectsFile}")
	override fun explanation(settings: TaskTussleSettings) = if (settings.statusEffectWeight <= 0) null else
		"(Status Effect Task) Have the named effect applied to you. Any strength or duration counts."
}
