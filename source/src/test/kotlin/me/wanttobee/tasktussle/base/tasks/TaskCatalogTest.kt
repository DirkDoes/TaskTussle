package me.wanttobee.tasktussle.base.tasks

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import me.wanttobee.tasktussle.tasks.obtainTask.BlockDifficulty
import net.minecraft.resources.Identifier

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

	@Test
	fun catalogScrollsByEightItemRows() {
		val ids = (1..9).map { Identifier.fromNamespaceAndPath("test", "item_$it") }
		val pools = TaskPools(ids, ids, ids)
		assertEquals(listOf(0, 3, 6, 9), catalogTierOffsets(pools, emptyList()))
		assertEquals(9, catalogMaxOffset(pools))
	}

	@Test
	fun catalogOwnershipAndLimits() {
		val owner = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
		assertEquals(owner, TaskCatalog.parseOwner(listOf("# file", "owner: $owner")))
		assertEquals(null, TaskCatalog.parseOwner(listOf("Easy:")))
		assertFalse(mayCreateCatalog(3, 3, false))
		assertEquals(true, mayCreateCatalog(3, 3, true))
		assertFalse(mayCreateCatalog(0, 36, true))
		assertEquals(true, mayEditCatalog(owner, owner, false))
		assertFalse(mayEditCatalog(null, owner, false))
		assertEquals(true, mayEditCatalog(null, owner, true))
	}

	@Test
	fun movesTasksBetweenTiersAndRemoved() {
		val id = Identifier.fromNamespaceAndPath("test", "task")
		val pools = TaskPools(listOf(id), emptyList(), emptyList())
		val hard = moveTaskBetweenTiers(pools, id, Difficulty.HARD)
		assertEquals(emptyList(), hard.easy)
		assertEquals(listOf(id), hard.hard)
		assertEquals(TaskPools(emptyList(), emptyList(), emptyList()), moveTaskBetweenTiers(hard, id, null))
	}
}
