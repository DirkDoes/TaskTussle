package me.wanttobee.tasktussle.base.tasks

import kotlin.math.max
import net.minecraft.ChatFormatting
import net.minecraft.resources.Identifier

internal val ARROW_UP_MODEL = Identifier.fromNamespaceAndPath("tasktussle", "arrow_up")

internal val ARROW_DOWN_MODEL = Identifier.fromNamespaceAndPath("tasktussle", "arrow_down")

internal fun catalogTierOffsets(pools: TaskPools, removed: List<Identifier>): List<Int> =
	listOf(pools.easy, pools.medium, pools.hard, removed).runningFold(0) { offset, tier -> offset + 1 + (tier.size + 7) / 8 }.dropLast(1)

internal fun catalogMaxOffset(pools: TaskPools, removed: List<Identifier> = emptyList()): Int {
	val offsets = catalogTierOffsets(pools, removed)
	return max(offsets.last(), offsets.last() + 1 + (removed.size + 7) / 8 - 6)
}

internal fun tierId(difficulty: Difficulty) = if (difficulty == Difficulty.MEDIUM) "normal" else difficulty.name.lowercase()

internal fun tierColor(difficulty: Difficulty) = when (difficulty) {
	Difficulty.EASY -> ChatFormatting.GREEN
	Difficulty.MEDIUM -> ChatFormatting.YELLOW
	Difficulty.HARD -> ChatFormatting.RED
}

internal fun pickerRows(count: Int): Int = if (count > 45) 6 else 1 + max(1, (count - 1) / 9 + 1)
