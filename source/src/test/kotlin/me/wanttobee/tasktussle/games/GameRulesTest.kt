package me.wanttobee.tasktussle.games

import java.util.UUID
import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.base.games.ITTCardLogic
import me.wanttobee.tasktussle.base.games.TimeoutRule
import me.wanttobee.tasktussle.base.games.TaskGridLayout
import me.wanttobee.tasktussle.base.games.TaskGridMenu
import me.wanttobee.tasktussle.base.generic.ActionChestMenu
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.generic.SettingsMenu
import me.wanttobee.tasktussle.base.tasks.Difficulty
import me.wanttobee.tasktussle.base.tasks.TaskKind
import me.wanttobee.tasktussle.base.tasks.TaskFactory
import me.wanttobee.tasktussle.base.tasks.TaskState
import me.wanttobee.tasktussle.base.tasks.TaskTemplate
import me.wanttobee.tasktussle.base.tasks.claim
import me.wanttobee.tasktussle.base.tasks.deliveryTask
import me.wanttobee.tasktussle.base.tasks.taskStack
import me.wanttobee.tasktussle.games.bingo.BingoCardLogic
import me.wanttobee.tasktussle.games.bingo.BingoGame
import me.wanttobee.tasktussle.games.bingo.BingoManager
import me.wanttobee.tasktussle.games.bingo.BingoTeam
import me.wanttobee.tasktussle.games.lockout.LockoutGame
import me.wanttobee.tasktussle.games.lockout.LockoutManager
import me.wanttobee.tasktussle.games.lockout.LockoutTeam
import me.wanttobee.tasktussle.games.lockout.LockoutSettingsMenu
import me.wanttobee.tasktussle.games.lockout.LockoutCardMenu
import me.wanttobee.tasktussle.teams.teamItem
import me.wanttobee.tasktussle.tasks.clickTask.ClickTaskManager
import me.wanttobee.tasktussle.tasks.obtainTask.ObtainTask
import me.wanttobee.tasktussle.teams.Team
import net.minecraft.SharedConstants
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.registries.VanillaRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.Items
import net.minecraft.world.entity.EntityEquipment
import net.minecraft.world.entity.player.Inventory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.assertNotSame

class GameRulesTest {
	private fun tasks() = (0 until 25).map {
		ObtainTask(TaskTemplate(TaskKind.BLOCK, Identifier.parse("minecraft:stone"), Difficulty.EASY))
	}
	private fun team(index: Int, name: String) = Team(index, mutableSetOf(UUID.nameUUIDFromBytes(name.toByteArray())))
	private fun bingoTeam(index: Int, completed: Set<Int>) = BingoTeam(team(index, "Player $index")).apply {
		tasks += this@GameRulesTest.tasks()
		completed.forEach { tasks[it].claim(index, "Player $index") }
	}
	private fun lockout(limit: Int = 1): LockoutGame {
		val card = ITTCardLogic().apply { tasks += this@GameRulesTest.tasks() }
		return LockoutGame(listOf(LockoutTeam(team(1, "Alice"), card), LockoutTeam(team(2, "Bob"), card), LockoutTeam(team(3, "Cara"), card)), 600, limit)
	}
	private fun bootstrap() {
		SharedConstants.tryDetectVersion()
		Bootstrap.bootStrap()
		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup()).forEach { it.apply() }
	}

	private fun hasAction(menu: ActionChestMenu, slot: Int): Boolean =
		(ActionChestMenu::class.java.getDeclaredField("actions").apply { isAccessible = true }.get(menu) as Map<*, *>).containsKey(slot)

	@Test
	fun bingoHighlightsOnlyTheSelectedSidebarTeamAndLeavesHeaderPlain() {
		bootstrap()
		val system = TaskTussleSystem().apply { settings.cardVisibility = "visible" }
		val inventory = Inventory::class.java.constructors.single().newInstance(null, EntityEquipment()) as Inventory
		val first = bingoTeam(1, emptySet())
		val selected = bingoTeam(2, emptySet())
		val game = BingoGame(listOf(first, selected), 0)
		val layout = TaskGridLayout()
		val menu = TaskGridMenu(1, inventory, system, game, selected, first, layout, false)
		assertEquals(true, menu.getSlot(layout.teamSlots[1]).item.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
		assertNull(menu.getSlot(layout.teamSlots[0]).item.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
		assertNull(menu.getSlot(layout.viewedTeamSlot).item.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
		assertEquals(teamItem(1, true), menu.getSlot(layout.teamSlots[0]).item.item)
		assertTrue(hasAction(menu, layout.teamSlots[0]))
		assertFalse(hasAction(menu, layout.teamSlots[1]))
	}

	@Test
	fun lockoutSidebarsStayGlassAndReadOnlyWithPublicScoresInSeparateMenus() {
		bootstrap()
		val system = TaskTussleSystem()
		val inventory = Inventory::class.java.constructors.single().newInstance(null, EntityEquipment()) as Inventory
		val game = lockout()
		game.teams[1].tasks[0].claim(2, "Bob")
		game.teams[1].tasks[1].claim(2, "Bob")
		val layout = TaskGridLayout(Identifier.parse("tasktussle:lockout_card"))
		for (visibility in listOf("hidden", "progress only", "visible")) for (finished in listOf(false, true)) {
			system.settings.cardVisibility = visibility
			game.finished = finished
			val alice = LockoutCardMenu(1, inventory, system, game, game.teams[0], game.teams[0], layout, false)
			val bob = LockoutCardMenu(2, inventory, system, game, game.teams[1], game.teams[1], layout, false)
			assertNotSame(alice.getSlot(layout.taskSlots[0]).container, bob.getSlot(layout.taskSlots[0]).container)
			assertEquals(teamItem(1, true), alice.getSlot(layout.viewedTeamSlot).item.item)
			assertEquals(teamItem(2, true), bob.getSlot(layout.viewedTeamSlot).item.item)
			assertNull(alice.getSlot(layout.viewedTeamSlot).item.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
			val other = alice.getSlot(layout.teamSlots[1]).item
			assertEquals(teamItem(2, false), other.item)
			assertEquals(2, other.count)
			assertTrue(other.get(DataComponents.LORE)!!.lines.any { it.string == "2/25" })
			assertEquals(true, alice.getSlot(layout.teamSlots[0]).item.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
			assertNull(other.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
			for (slot in layout.teamSlots) assertFalse(hasAction(alice, slot))
		}
		assertEquals(Identifier.parse("tasktussle:lockout_card"), LockoutManager.cardItemModel)
	}

	@Test
	fun managersGenerateSharedLockoutTasksButSeparateBingoProgressAndSnapshotLimits() {
		val settings = TaskTussleSettings(clickWeight = 10, mutualTasks = 25, gameMinutes = 10, sharedTaskCompletionLimit = 12)
		val factory = TaskFactory(listOf(ClickTaskManager))
		// Seed an already-loaded repeatable source without starting a server or reading catalogs.
		factory.javaClass.getDeclaredField("pools").apply { isAccessible = true }.set(factory, mapOf(TaskKind.CLICK to null))
		val teams = listOf(team(1, "Alice"), team(2, "Bob"), Team(3, mutableSetOf()))
		val shared = checkNotNull(LockoutManager.create(teams, settings, factory))
		assertEquals(25, shared.teams[0].tasks.size)
		assertEquals(2, shared.completionLimit)
		assertEquals(600, shared.remainingSeconds)
		assertSame(shared.teams[0].card, shared.teams[1].card)
		shared.teams[0].tasks[0].claim(1, "Alice", shared.completionLimit)
		assertEquals(1, shared.teams[0].completed)
		assertEquals(0, shared.teams[1].completed)
		settings.sharedTaskCompletionLimit = 1
		settings.gameMinutes = 0
		assertEquals(2, shared.completionLimit)
		assertEquals(600, shared.remainingSeconds)
		val separate = checkNotNull(BingoManager.create(teams.take(2), settings, factory))
		assertEquals(0, separate.remainingSeconds)
		assertNotSame(separate.teams[0].tasks[0], separate.teams[1].tasks[0])
		separate.teams[0].tasks[0].claim()
		assertEquals(1, separate.teams[0].completed)
		assertEquals(0, separate.teams[1].completed)
	}

	@Test
	fun settingsExposeLockoutAndItsSharedClaimLimitSeparatelyFromBingo() {
		bootstrap()
		val system = TaskTussleSystem()
		val inventory = Inventory::class.java.constructors.single().newInstance(null, EntityEquipment()) as Inventory
		val settings = SettingsMenu(1, inventory, system, true)
		assertEquals("Bingo", settings.getSlot(36).item.get(DataComponents.CUSTOM_NAME)?.string)
		assertEquals("Lockout", settings.getSlot(37).item.get(DataComponents.CUSTOM_NAME)?.string)
		val lockout = LockoutSettingsMenu(2, inventory, system)
		assertEquals("Teams per task: 1", lockout.getSlot(9).item.get(DataComponents.CUSTOM_NAME)?.string)
	}

	@Test
	fun bingoDefaultsToLinesThenTasksThenDraw() {
		val settings = TaskTussleSettings()
		assertEquals(TimeoutRule.LINES_THEN_TASKS, settings.bingoTimeoutRule)
		val line = bingoTeam(1, (0..4).toSet())
		val scattered = bingoTeam(2, setOf(0, 1, 5, 6, 10, 11))
		assertSame(line, BingoManager.timeUpWinner(BingoGame(listOf(line, scattered), 0), settings))
		val biggerLine = bingoTeam(2, (0..5).toSet())
		assertSame(biggerLine, BingoManager.timeUpWinner(BingoGame(listOf(line, biggerLine), 0), settings))
		val identical = bingoTeam(2, (0..4).toSet())
		assertNull(BingoManager.timeUpWinner(BingoGame(listOf(line, identical), 0), settings))
		settings.bingoTimeoutRule = TimeoutRule.MOST_TASKS
		assertSame(scattered, BingoManager.timeUpWinner(BingoGame(listOf(line, scattered), 0), settings))
		settings.bingoTimeoutRule = TimeoutRule.DRAW
		assertNull(BingoManager.timeUpWinner(BingoGame(listOf(line, scattered), 0), settings))
	}

	@Test
	fun bingoCountsAllTwelveLinesAndDoesNotShareProgress() {
		val full = bingoTeam(1, (0..24).toSet())
		assertEquals(12, BingoCardLogic.completedLines(full.tasks, 1))
		assertEquals(0, BingoCardLogic.completedLines(full.tasks, 2))
		val empty = bingoTeam(2, emptySet())
		assertEquals(0, empty.completed)
		val settings = TaskTussleSettings()
		assertFalse(BingoGame(listOf(full, empty), 0).sharedTasks)
		assertTrue(BingoManager.isFinished(BingoGame(listOf(full, empty), 0), full, settings))
		assertFalse(BingoManager.isFinished(BingoGame(listOf(full, empty), 0), empty, settings))
	}

	@Test
	fun lastClaimEndsLockoutButHighestScoreWinsRatherThanLastClaimant() {
		val game = lockout()
		val settings = TaskTussleSettings()
		val (alice, bob, _) = game.teams
		assertSame(alice.tasks[0], bob.tasks[0])
		for (index in 0..19) alice.tasks[index].claim(1, "Alice")
		for (index in 20..23) bob.tasks[index].claim(2, "Bob")
		assertFalse(LockoutManager.isFinished(game, alice, settings))
		bob.tasks[24].claim(2, "Bob")
		assertTrue(LockoutManager.isFinished(game, bob, settings))
		assertEquals(20, alice.completed)
		assertEquals(5, bob.completed)
		assertSame(alice, LockoutManager.completionWinner(game, bob, settings))
	}

	@Test
	fun multiClaimCardsWaitForEverySlotAndTiedScoresDraw() {
		val game = lockout(2)
		val settings = TaskTussleSettings()
		val (alice, bob, cara) = game.teams
		for (task in alice.tasks) task.claim(1, "Alice", 2)
		assertFalse(LockoutManager.isFinished(game, alice, settings))
		for (task in bob.tasks) task.claim(2, "Bob", 2)
		assertTrue(LockoutManager.isFinished(game, bob, settings))
		assertEquals(25, alice.completed)
		assertEquals(25, bob.completed)
		assertEquals(0, cara.completed)
		assertNull(LockoutManager.completionWinner(game, bob, settings))
		assertNull(LockoutManager.timeUpWinner(game, settings))
	}

	@Test
	fun lockoutTimeLimitUsesClaimsAndCanBeConfiguredToDraw() {
		val game = lockout(3)
		val settings = TaskTussleSettings()
		game.teams[1].tasks[0].claim(2, "Bob", 3)
		assertSame(game.teams[1], LockoutManager.timeUpWinner(game, settings))
		assertFalse(LockoutManager.isFinished(game, game.teams[1], settings))
		settings.lockoutTimeoutRule = TimeoutRule.DRAW
		assertNull(LockoutManager.timeUpWinner(game, settings))
	}

	@Test
	fun contributionPercentagesUsePlayerIdentityAndOnlyTheirTeamsClaims() {
		val game = lockout(2)
		game.teams[0].tasks[0].claim(1, "Alice", 2)
		game.teams[0].tasks[1].claim(1, "Alice", 2)
		game.teams[0].tasks[2].claim(1, "Ally", 2)
		game.teams[1].tasks[0].claim(2, "Bob", 2)
		assertEquals(listOf("Alice = 66.66%", "Ally = 33.33%"), game.teams[0].contributions())
		assertEquals(listOf("Bob = 100.0%"), game.teams[1].contributions())
	}

	@Test
	fun deliveriesCannotConsumeItemsForAlreadyClaimedOrLockedTasks() {
		bootstrap()
		val game = lockout(2)
		val task = game.teams[0].tasks[0]
		task.claim(1, "Alice", 2)
		assertNull(deliveryTask(listOf(task), Items.STONE, 1, 2))
		assertSame(task, deliveryTask(listOf(task), Items.STONE, 2, 2))
		task.claim(2, "Bob", 2)
		assertNull(deliveryTask(listOf(task), Items.STONE, 3, 2))
	}

	@Test
	fun sharedLoreShowsOrderedOwnersWhileOtherTeamsCanStillClaim() {
		bootstrap()
		val game = lockout(2)
		val task = game.teams[0].tasks[0]
		val system = TaskTussleSystem()
		task.claim(1, "Alice", 2)
		val open = taskStack(system, task, 2, game)
		assertTrue(open.get(DataComponents.LORE)!!.lines.any { it.string == "1. Team 1: Alice" })
		assertEquals(Identifier.parse("minecraft:stone"), open.get(DataComponents.ITEM_MODEL))
		assertEquals(Identifier.parse("tasktussle:task_complete"), taskStack(system, task, 1, game).get(DataComponents.ITEM_MODEL))
		task.claim(2, "Bob", 2)
		val locked = taskStack(system, task, 3, game)
		assertEquals("Locked out", locked.get(DataComponents.LORE)!!.lines.first().string)
		assertTrue(locked.get(DataComponents.LORE)!!.lines.any { it.string == "2. Team 2: Bob" })
		assertEquals(TaskState.LOCKED_OUT, task.progress.state(3, 2))
	}

	@Test
	fun gameTimeCanIncreaseDecreaseDisableAndNeverOverflow() {
		val settings = TaskTussleSettings()
		assertEquals(0, settings.gameSeconds)
		settings.adjustGameTime(10)
		settings.adjustGameTime(1)
		assertEquals(660, settings.gameSeconds)
		settings.adjustGameTime(-1)
		assertEquals(600, settings.gameSeconds)
		settings.adjustGameTime(-10)
		settings.adjustGameTime(-10)
		assertEquals(0, settings.gameSeconds)
		settings.adjustGameTime(Int.MAX_VALUE)
		assertTrue(settings.gameSeconds > 0)
		settings.adjustGameTime(10)
		assertTrue(settings.gameSeconds > 0)
	}
}
