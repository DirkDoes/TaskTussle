package me.wanttobee.tasktussle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TaskCatalogTest {
	@Test
	fun parsesFabricIdentifiersAndLegacyNormalHeading() {
		val pools = TaskCatalog.parseYaml(listOf(
			"Easy:", "- minecraft:stone # common", "Normal:", "- minecraft:nether_bricks", "Hard:", "- not valid!",
		))
		assertEquals("minecraft:stone", pools.easy.single().toString())
		assertEquals("minecraft:nether_bricks", pools.medium.single().toString())
		assertEquals(emptyList(), pools.hard)
	}

	@Test
	fun filtersTechnicalBlocks() {
		assertFalse(BlockDifficulty.isObtainable("bedrock"))
		assertFalse(BlockDifficulty.isObtainable("infested_stone"))
		assertEquals(Difficulty.HARD, BlockDifficulty.rank("oxidized_cut_copper"))
	}
}
