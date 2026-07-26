package me.wanttobee.tasktussle

import net.minecraft.resources.Identifier
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TaskGeneratorTest {
	@Test
	fun generatesExactUniqueCardWithRatios() {
		fun ids(prefix: String, count: Int) = (1..count).map { Identifier.fromNamespaceAndPath("test", "$prefix$it") }
		val blocks = TaskPools(ids("easy", 30), ids("medium", 30), ids("hard", 30))
		val advancements = TaskPools(emptyList(), emptyList(), emptyList())
		val tasks = assertNotNull(TaskGenerator.generate(25, blocks, advancements, Triple(13, 8, 4), Triple(10, 0, 0), Triple(1, 1, 1), random = Random(1)))
		assertEquals(25, tasks.size)
		assertEquals(25, tasks.map { it.kind to it.id }.toSet().size)
		assertEquals(listOf(13, 8, 4), Difficulty.entries.map { difficulty -> tasks.count { it.difficulty == difficulty } })
	}
}
