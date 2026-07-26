package me.wanttobee.tasktussle

import net.minecraft.ChatFormatting
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import java.util.UUID
import kotlin.math.floor
import kotlin.random.Random

enum class TaskKind { BLOCK, ADVANCEMENT, CLICK }

data class TaskTemplate(
	val kind: TaskKind,
	val id: Identifier,
	val difficulty: Difficulty,
	val amount: Int = 1,
)

data class BingoTask(val template: TaskTemplate) {
	var completed = false
	var failed = false
	var obtained = 0
	val contributors = mutableSetOf<String>()
}

data class TussleTeam(val index: Int, val members: MutableSet<UUID>) {
	val tasks = mutableListOf<BingoTask>()
	var succeedTokens = 0
	var succeedTokensMax = 0
	val color: ChatFormatting = TEAM_COLORS[(index - 1) % TEAM_COLORS.size]
	val completed: Int get() = tasks.count(BingoTask::completed)

	fun displayName(): String = "Team $index"

	companion object {
		private val TEAM_COLORS = listOf(
			ChatFormatting.BLUE, ChatFormatting.RED, ChatFormatting.GREEN, ChatFormatting.YELLOW, ChatFormatting.LIGHT_PURPLE,
			ChatFormatting.GOLD, ChatFormatting.AQUA, ChatFormatting.DARK_GREEN, ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_AQUA,
		)
	}
}

data class TaskTussleSettings(
	var cardVisibility: String = "progress only",
	var chooseTeams: Boolean = false,
	var easyRatio: Int = 13,
	var mediumRatio: Int = 8,
	var hardRatio: Int = 4,
	var blockWeight: Int = 10,
	var advancementWeight: Int = 2,
	var clickWeight: Int = 0,
	var handInItems: Boolean = false,
	var easyObtainCount: Int = 1,
	var mediumObtainCount: Int = 1,
	var hardObtainCount: Int = 1,
	var mutualTasks: Int = 15,
	var winningCondition: String = "1 line",
	var succeedTokens: Int = 0,
	var gameMinutes: Int = 0,
	var overtime: String = "draw",
	var blocksFile: String = "blocks_default.yml",
	var advancementsFile: String = "advancements_default.yml",
)

data class TussleGame(
	val teams: List<TussleTeam>,
	var remainingSeconds: Int,
	var paused: Boolean = false,
	var finished: Boolean = false,
)

data class TeamSelection(
	val teamCount: Int,
	val starter: UUID,
	val members: MutableMap<UUID, Int> = mutableMapOf(),
	val startsGame: Boolean,
)

object TaskGenerator {
	private val clickId = Identifier.fromNamespaceAndPath("tasktussle", "click")

	fun generate(
		amount: Int,
		blockPools: TaskPools,
		advancementPools: TaskPools,
		difficultyRatios: Triple<Int, Int, Int>,
		kindWeights: Triple<Int, Int, Int>,
		obtainCounts: Triple<Int, Int, Int>,
		excluded: Set<Pair<TaskKind, Identifier>> = emptySet(),
		random: Random = Random.Default,
	): List<TaskTemplate>? {
		if (amount < 0 || difficultyRatios.toList().sum() <= 0 || kindWeights.toList().sum() <= 0) return null
		val kindCounts = ratioCounts(amount, kindWeights)
		val selected = mutableListOf<TaskTemplate>()

		selected += selectFromPools(TaskKind.BLOCK, blockPools, kindCounts[0], difficultyRatios, obtainCounts, excluded, random)
		selected += selectFromPools(TaskKind.ADVANCEMENT, advancementPools, kindCounts[1], difficultyRatios, Triple(1, 1, 1), excluded, random)
		repeat(kindCounts[2]) { selected += TaskTemplate(TaskKind.CLICK, clickId, Difficulty.EASY) }

		if (selected.size < amount) {
			val used = excluded + selected.map { it.kind to it.id }
			val remaining = amount - selected.size
			val fallback = buildList {
				if (kindWeights.first > 0) addAll(allTemplates(TaskKind.BLOCK, blockPools, obtainCounts))
				if (kindWeights.second > 0) addAll(allTemplates(TaskKind.ADVANCEMENT, advancementPools, Triple(1, 1, 1)))
			}.filter { (it.kind to it.id) !in used }.shuffled(random).take(remaining)
			selected += fallback
			if (selected.size < amount && kindWeights.third > 0) repeat(amount - selected.size) {
				selected += TaskTemplate(TaskKind.CLICK, clickId, Difficulty.EASY)
			}
		}
		return selected.takeIf { it.size == amount }?.shuffled(random)
	}

	internal fun ratioCounts(amount: Int, ratios: Triple<Int, Int, Int>): List<Int> {
		val total = ratios.toList().sum().toDouble()
		if (total <= 0) return listOf(0, 0, 0)
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
		counts: Triple<Int, Int, Int>,
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
				.map { TaskTemplate(kind, it, difficulty, counts.toList()[index]) }
		}
		if (selected.size < amount) {
			val used = selected.mapTo(mutableSetOf()) { it.id }
			selected += allTemplates(kind, pools, counts)
				.filter { it.id !in used && (kind to it.id) !in excluded }
				.shuffled(random)
				.take(amount - selected.size)
		}
		return selected
	}

	private fun allTemplates(kind: TaskKind, pools: TaskPools, counts: Triple<Int, Int, Int>) = Difficulty.entries.flatMapIndexed { index, difficulty ->
		pools[difficulty].map { TaskTemplate(kind, it, difficulty, counts.toList()[index]) }
	}
}

fun TussleTeam.onlinePlayers(players: Collection<ServerPlayer>): List<ServerPlayer> = players.filter { it.uuid in members }
