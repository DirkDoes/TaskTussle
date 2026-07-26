package me.wanttobee.tasktussle

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.Commands.argument
import net.minecraft.network.chat.Component
import net.minecraft.server.permissions.Permissions

object TaskTussleCommands {
	fun register(dispatcher: CommandDispatcher<CommandSourceStack>, service: TaskTussleService, lobby: LobbyService) {
		val mainHelp = CommandHelp("/taskTussle", "to start a game or change settings before starting the game")
		val worldHelp = CommandHelp("/taskTussle worldsetup", "to prepare or clear the Task Tussle lobby")

		val taskTussle = dispatcher.register(
			literal("taskTussle").requires(::admin)
				.executes { context -> mainHelp.show(context.source) }
				.then(mainHelp.helpCommand())
				.then(mainHelp.branch("start", "to start one of the games").then(literal("bingo")
					.executes { context -> fail(context.source, "you must specify the amount of teams you want to play with") }
					.then(argument("teams", integer(1, 15)).executes { context ->
						if (service.start(context.source.server, player(context.source), getInteger(context, "teams"))) 1 else 0
					})))
				.then(mainHelp.branch("debug", "check information about the current running game")
					.then(literal("status").executes { context -> player(context.source)?.let { service.debugStatus(it); 1 } ?: 0 })
					.then(literal("log").executes { context ->
						service.logEnabled = !service.logEnabled
						context.source.sendSuccess({ Component.literal("look at the server console") }, false)
						println("[Task Tussle] ${if (service.logEnabled) "Started" else "Stopped"} Logging")
						1
					}))
				.then(mainHelp.branch("stop", "to stop and clear the current running game").executes { context -> if (service.stop(context.source.server, player(context.source))) 1 else 0 })
				.then(mainHelp.branch("finish", "to finish the game early")
					.then(literal("draw").executes { context -> if (service.draw(context.source.server)) 1 else 0 })
					.then(literal("out_of_time").executes { context -> if (service.timeUp(context.source.server)) 1 else 0 }))
				.then(mainHelp.branch("settings", "to view or change settings").executes { context -> player(context.source)?.let {
					service.openSettings(it)
					if (service.currentGame() != null) it.sendSystemMessage(TaskTussleService.tagged("There is already a game running, most settings will not effect a game that is already running", ChatFormatting.RED))
					1
				} ?: 0 })
				.then(mainHelp.branch("worldsetup", "to prepare or clear the Task Tussle lobby")
					.executes { context -> worldHelp.show(context.source) }
					.then(worldHelp.helpCommand())
					.then(worldHelp.branch("start", "to build the lobby in this world").executes { context -> player(context.source)?.let { lobby.start(it); 1 } ?: 0 })
					.then(worldHelp.branch("clear", "to remove the generated lobby").executes { context -> lobby.clear(context.source.server); 1 })
					.then(worldHelp.branch("create_parkour", "to add the parkour course").executes { context -> player(context.source)?.let { if (lobby.createParkour(it)) 1 else 0 } ?: 0 })
					.then(worldHelp.branch("remove_parkour", "to remove the parkour course").executes { if (lobby.removeParkour()) 1 else 0 })),
		)
		dispatcher.register(literal("tasktussle").requires(::admin).executes { context -> mainHelp.show(context.source) }.redirect(taskTussle))
		dispatcher.register(literal("tt").requires(::admin).executes { context -> mainHelp.show(context.source) }.redirect(taskTussle))
		dispatcher.register(literal("card").executes { context -> player(context.source)?.let { if (service.openCard(it)) 1 else 0 } ?: 0 })
		dispatcher.register(literal("display").executes { context -> player(context.source)?.let { service.displayHeldItem(it); 1 } ?: 0 })

		val teamsHelp = CommandHelp("/teams", "to inspect or create teams")
		dispatcher.register(
			literal("teams").requires(::admin)
				.executes { context -> teamsHelp.show(context.source) }
				.then(teamsHelp.helpCommand())
				.then(teamsHelp.branch("list", "to list the current teams").executes { context -> player(context.source)?.let { service.teamStatus(it); 1 } ?: 0 })
				.then(teamsHelp.branch("clearAll", "to clear all teams").executes { context -> service.clearTeams(context.source.server); 1 })
				.then(teamsHelp.branch("generateTeams", "to randomly generate teams").then(argument("amount", integer(1)).executes { context ->
					player(context.source)?.let { if (service.generateTeams(it, getInteger(context, "amount"))) 1 else 0 } ?: 0
				}))
				.then(teamsHelp.branch("teamMaker", "to open the manual team maker").then(argument("amount", integer(2, 54)).executes { context ->
					player(context.source)?.let { if (service.beginTeamSelection(it, getInteger(context, "amount"), false)) 1 else 0 } ?: 0
				})),
		)

		val debugHelp = CommandHelp("/debug_tt", "to inspect Task Tussle internals")
		dispatcher.register(
			literal("debug_tt").requires(::admin)
				.executes { context -> debugHelp.show(context.source) }
				.then(debugHelp.helpCommand())
				.then(debugHelp.branch("task_events", "to inspect registered task events").executes { context -> player(context.source)?.let { service.debugStatus(it); 1 } ?: 0 })
				.then(debugHelp.branch("hotbar_items", "to inspect Task Tussle hotbar items").executes { context -> player(context.source)?.let {
					it.sendSystemMessage(TaskTussleService.tagged("hotbar items: ${if (service.currentGame() == null && service.currentSelection() == null) 0 else 1}", ChatFormatting.YELLOW)); 1
				} ?: 0 })
				.then(debugHelp.branch("interactive_inventories", "to inspect interactive inventories").executes { context -> player(context.source)?.let {
					it.sendSystemMessage(TaskTussleService.tagged("interactive inventories are created natively when opened", ChatFormatting.YELLOW)); 1
				} ?: 0 })
				.then(debugHelp.branch("teams", "to inspect team state").executes { context -> player(context.source)?.let { service.teamStatus(it); 1 } ?: 0 })
				.then(debugHelp.branch("timers", "to inspect active timers").executes { context -> player(context.source)?.let {
					it.sendSystemMessage(TaskTussleService.tagged("active timers: ${if (service.currentGame()?.remainingSeconds?.let { seconds -> seconds > 0 } == true) 1 else 0}", ChatFormatting.YELLOW)); 1
				} ?: 0 }),
		)
	}

	private class CommandHelp(private val command: String, private val description: String) {
		private val entries = mutableListOf<Pair<String, String>>()

		fun branch(name: String, description: String): LiteralArgumentBuilder<CommandSourceStack> {
			entries += name to description
			return literal(name)
		}

		fun helpCommand(): LiteralArgumentBuilder<CommandSourceStack> = literal("help").executes { context -> show(context.source) }

		fun show(source: CommandSourceStack): Int {
			val divider = Component.literal("================ 1/1 =================").withStyle(ChatFormatting.DARK_GRAY)
			source.sendSuccess({ divider }, false)
			source.sendSuccess({
				Component.literal("(Com)").withStyle(ChatFormatting.GREEN)
					.append(Component.literal("[Task Tussle]").withStyle(ChatFormatting.GOLD))
					.append(Component.literal(" $command $description").withStyle(ChatFormatting.WHITE))
			}, false)
			entries.forEach { (name, details) ->
				source.sendSuccess({
					Component.literal("- ").withStyle(ChatFormatting.GRAY)
						.append(Component.literal(name).withStyle(ChatFormatting.YELLOW))
						.append(Component.literal(": $details").withStyle(ChatFormatting.WHITE))
				}, false)
			}
			source.sendSuccess({ divider }, false)
			return 1
		}
	}

	private fun fail(source: CommandSourceStack, text: String): Int {
		source.sendFailure(Component.literal(text))
		return 0
	}

	private fun player(source: CommandSourceStack) = source.player.also {
		if (it == null) source.sendFailure(Component.literal("This command requires a player."))
	}

	private fun admin(source: CommandSourceStack): Boolean = source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
}
