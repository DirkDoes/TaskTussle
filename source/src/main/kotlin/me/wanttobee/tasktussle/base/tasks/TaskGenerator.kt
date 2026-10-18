package me.wanttobee.tasktussle.base.tasks

import kotlin.math.floor
import kotlin.random.Random
import net.minecraft.resources.Identifier

object TaskGenerator {
	fun generate(
		amount: Int,
		sources: List<TaskSource>,
		difficultyRatios: Triple<Int, Int, Int>,
		excluded: Set<Pair<TaskKind, Identifier>> = emptySet(),
		random: Random = Random.Default,
	): List<TaskTemplate>? {
		if (amount < 0 || difficultyRatios.toList().sum() <= 0 || sources.sumOf { it.weight } <= 0) return null
		val kindCounts = ratioCounts(amount, sources.map { it.weight })
		val selected = mutableListOf<TaskTemplate>()
		sources.forEachIndexed { index, source ->
			if (source.pools != null) selected += selectFromPools(source.kind, source.pools, kindCounts[index], difficultyRatios, excluded, random)
			else source.repeatable?.let { template -> repeat(kindCounts[index]) { selected += template } }
		}
		if (selected.size < amount) {
			val used = excluded + selected.map { it.kind to it.id }
			val remaining = amount - selected.size
			val fallback = sources.filter { it.weight > 0 }.flatMap { source ->
				source.pools?.let { allTemplates(source.kind, it) }.orEmpty()
			}.filter { (it.kind to it.id) !in used }.shuffled(random).take(remaining)
			selected += fallback
			val repeatable = sources.firstOrNull { it.weight > 0 && it.repeatable != null }?.repeatable
			if (selected.size < amount && repeatable != null) repeat(amount - selected.size) { selected += repeatable }
		}
		return selected.takeIf { it.size == amount }?.shuffled(random)
	}
	internal fun ratioCounts(amount: Int, ratios: Triple<Int, Int, Int>): List<Int> = ratioCounts(amount, ratios.toList())

	internal fun ratioCounts(amount: Int, ratios: List<Int>): List<Int> {
		val total = ratios.toList().sum().toDouble()
		if (total <= 0) return ratios.map { 0 }
		val exact = ratios.toList().map { it / total * amount }
		val result = exact.map { floor(it).toInt() }.toMutableList()
		repeat(amount - result.sum()) {
			val index = exact.indices.maxBy { exact[it] - result[it] }
			result[index]++
		}
		return result
	}

	private fun selectFromPools(
		kind: TaskKind,
		pools: TaskPools,
		amount: Int,
		ratios: Triple<Int, Int, Int>,
		excluded: Set<Pair<TaskKind, Identifier>>,
		random: Random,
	): List<TaskTemplate> {
		val wanted = ratioCounts(amount, ratios)
		val selected = mutableListOf<TaskTemplate>()
		for ((index, difficulty) in Difficulty.entries.withIndex()) {
			selected += pools[difficulty]
				.filter { (kind to it) !in excluded }
				.shuffled(random)
				.take(wanted[index])
				.map { TaskTemplate(kind, it, difficulty) }
		}
		if (selected.size < amount) {
			val used = selected.mapTo(mutableSetOf()) { it.id }
			selected += allTemplates(kind, pools)
				.filter { it.id !in used && (kind to it.id) !in excluded }
				.shuffled(random)
				.take(amount - selected.size)
		}
		return selected
	}

	private fun allTemplates(kind: TaskKind, pools: TaskPools) = Difficulty.entries.flatMap { difficulty ->
		pools[difficulty].map { TaskTemplate(kind, it, difficulty) }
	}
}
