package me.wanttobee.tasktussle.base.tasks

import java.util.UUID
import net.minecraft.resources.Identifier

internal fun moveTaskBetweenTiers(pools: TaskPools, id: Identifier, target: Difficulty?): TaskPools {
	fun tier(difficulty: Difficulty, values: List<Identifier>) = values.filterNot(id::equals) + if (target == difficulty) listOf(id) else emptyList()
	return TaskPools(tier(Difficulty.EASY, pools.easy), tier(Difficulty.MEDIUM, pools.medium), tier(Difficulty.HARD, pools.hard))
}

internal fun mayCreateCatalog(owned: Int, total: Int, operator: Boolean) = total < 36 && (operator || owned < 3)

internal fun mayEditCatalog(owner: UUID?, player: UUID, operator: Boolean) = operator || owner == player
