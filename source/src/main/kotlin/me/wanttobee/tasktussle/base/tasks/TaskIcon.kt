package me.wanttobee.tasktussle.base.tasks

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.games.ITTGame
import me.wanttobee.tasktussle.base.generic.named
import me.wanttobee.tasktussle.util.wrapLore
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.level.block.Blocks

internal val TASK_COMPLETE_MODEL = Identifier.fromNamespaceAndPath("tasktussle", "task_complete")

internal fun taskStack(system: TaskTussleSystem, task: ITask, teamIndex: Int? = null, game: ITTGame? = system.currentGame()): ItemStack {
	val limit = game?.completionLimit ?: 1
	val claims = task.progress.completions
	val state = task.progress.state(teamIndex ?: if (game == null) claims.firstOrNull()?.teamIndex else null, limit)
	val claimLore = if (game?.sharedTasks == true) listOf("Claims: ${claims.size}/$limit") + claims.mapIndexed { index, claim ->
		"${index + 1}. Team ${claim.teamIndex}: ${claim.playerName}"
	} else claims.map { it.playerName }
	val advancementDescription = task.completedDescription(system.server)?.let(::wrapLore).orEmpty()
	return when {
		state == TaskState.COMPLETED || state == TaskState.LOCKED_OUT -> named(Items.PAPER, task.name(system.server), if (state == TaskState.COMPLETED) ChatFormatting.GREEN else ChatFormatting.GRAY,
			listOf(if (state == TaskState.COMPLETED) "Completed" else "Locked out") + advancementDescription + claimLore).apply {
			set(DataComponents.ITEM_MODEL, TASK_COMPLETE_MODEL)
		}
		state == TaskState.FAILED -> named(Blocks.STAINED_GLASS_PANE.red().asItem(), task.name(system.server), ChatFormatting.RED, listOf("Failed") + advancementDescription + claimLore, enchanted = true)
		else -> named(task.icon(system.server, system.settings), task.name(system.server), ChatFormatting.WHITE).apply {
			val kind = task.typeName
			val description = task.description(system.server)
			val descriptionLines = wrapLore(description)
			val descriptionLore = if (task.separateDescription) {
				listOf(Component.literal("Description:").withStyle { it.withColor(ChatFormatting.GRAY).withItalic(false) }) +
					descriptionLines.map { Component.literal(it).withStyle { style -> style.withColor(ChatFormatting.WHITE).withItalic(false) } }
			} else descriptionLines.mapIndexed { index, line ->
				if (index == 0) taskLoreLine("Description", line, ChatFormatting.WHITE)
				else Component.literal(line).withStyle { it.withColor(ChatFormatting.WHITE).withItalic(false) }
			}
			set(DataComponents.LORE, ItemLore(listOf(taskLoreLine("Task", kind, ChatFormatting.AQUA)) + descriptionLore + claimLore.map { Component.literal(it).withStyle(ChatFormatting.GRAY).withStyle { style -> style.withItalic(false) } }))
		}
	}
}

internal fun taskLoreLine(label: String, value: String, valueColor: ChatFormatting): Component =
	Component.literal("$label: ").withStyle { it.withColor(ChatFormatting.GRAY).withItalic(false) }
		.append(Component.literal(value).withStyle { it.withColor(valueColor).withItalic(false) })

