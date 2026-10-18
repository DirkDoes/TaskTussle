package me.wanttobee.tasktussle.base.tasks

import net.minecraft.resources.Identifier

data class TaskPools(
	val easy: List<Identifier>,
	val medium: List<Identifier>,
	val hard: List<Identifier>,
) {
	operator fun get(difficulty: Difficulty): List<Identifier> = when (difficulty) {
		Difficulty.EASY -> easy
		Difficulty.MEDIUM -> medium
		Difficulty.HARD -> hard
	}
}
