package me.wanttobee.tasktussle.commands

import com.mojang.brigadier.CommandDispatcher
import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.world.LobbyService
import net.minecraft.SharedConstants
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.Bootstrap
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class TaskTussleCommandsTest {
	@Test
	fun startIsRemovedWhileSettingsStopAndAliasesRemain() {
		SharedConstants.tryDetectVersion()
		Bootstrap.bootStrap()
		val dispatcher = CommandDispatcher<CommandSourceStack>()
		TaskTussleCommands.register(dispatcher, TaskTussleSystem(), LobbyService())
		val root = assertNotNull(dispatcher.root.getChild("taskTussle"))
		assertNull(root.getChild("start"))
		assertNotNull(root.getChild("settings")?.command)
		assertNotNull(root.getChild("stop")?.command)
		assertSame(root, dispatcher.root.getChild("tt").redirect)
		assertSame(root, dispatcher.root.getChild("tasktussle").redirect)
	}
}
