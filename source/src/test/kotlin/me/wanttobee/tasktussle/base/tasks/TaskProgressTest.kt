package me.wanttobee.tasktussle.base.tasks

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskProgressTest {
	private fun completion(team: Int, player: String, tick: Int = 0) = TaskCompletion(team, UUID.nameUUIDFromBytes(player.toByteArray()), player, tick)

	@Test
	fun singleClaimClosesTaskForOthersAndRejectsRepeatedCallbacks() {
		val progress = TaskProgress()
		assertTrue(progress.complete(completion(2, "Bob"), 1))
		assertEquals(TaskState.COMPLETED, progress.state(2, 1))
		assertEquals(TaskState.LOCKED_OUT, progress.state(1, 1))
		assertFalse(progress.complete(completion(2, "Another teammate"), 1))
		assertFalse(progress.complete(completion(1, "Alice"), 1))
		assertEquals(1, progress.completions.size)
	}

	@Test
	fun firstTwoDistinctTeamsClaimInOrderEvenOnTheSameTick() {
		val progress = TaskProgress()
		assertTrue(progress.complete(completion(3, "Cara", 20), 2))
		assertEquals(TaskState.ACTIVE, progress.state(1, 2))
		assertEquals(TaskState.COMPLETED, progress.state(3, 2))
		assertFalse(progress.complete(completion(3, "Cara's teammate", 20), 2))
		assertTrue(progress.complete(completion(1, "Alice", 20), 2))
		assertEquals(listOf(3, 1), progress.completions.map { it.teamIndex })
		assertEquals(listOf("Cara", "Alice"), progress.completions.map { it.playerName })
		assertEquals(TaskState.LOCKED_OUT, progress.state(2, 2))
		assertFalse(progress.complete(completion(2, "Bob", 21), 2))
		assertTrue(progress.closed(2))
	}

	@Test
	fun capacityThreeAndFailedTasksUseTheSameRules() {
		val progress = TaskProgress()
		for (team in 1..3) assertTrue(progress.complete(completion(team, "Player $team"), 3))
		assertFalse(progress.complete(completion(4, "Fourth"), 3))
		val failed = TaskProgress()
		failed.fail()
		assertTrue(failed.closed(3))
		assertEquals(TaskState.FAILED, failed.state(1, 3))
		assertFalse(failed.complete(completion(1, "Alice"), 3))
	}
}
