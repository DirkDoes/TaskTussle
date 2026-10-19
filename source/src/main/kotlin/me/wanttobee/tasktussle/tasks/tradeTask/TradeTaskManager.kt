package me.wanttobee.tasktussle.tasks.tradeTask

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.ITaskManager
import me.wanttobee.tasktussle.base.tasks.TaskKind
import me.wanttobee.tasktussle.base.tasks.TaskTemplate
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items

object TradeTaskManager : ITaskManager {
	override val kind = TaskKind("trade")
	override val name = "Trade Task"
	override val icon get() = Items.EMERALD
	override val iconModel = Identifier.fromNamespaceAndPath("tasktussle", "trade_task")
	override val files = TradeTaskFiles
	override fun weight(settings: TaskTussleSettings) = settings.tradeWeight
	override fun setWeight(settings: TaskTussleSettings, value: Int) { settings.tradeWeight = value }
	override fun create(template: TaskTemplate) = TradeTask(template)
	override fun settingsMenu(id: Int, inventory: Inventory, system: TaskTussleSystem) = TradeSettingsMenu(id, inventory, system)
	override fun settingsLore(settings: TaskTussleSettings) = listOf("File: ${settings.tradesFile}")
	override fun explanation(settings: TaskTussleSettings) = if (settings.tradeWeight <= 0) null else
		"(Trade Task) Complete any trade that buys or sells the named item. Wandering traders also count."
}
