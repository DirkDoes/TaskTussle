package me.wanttobee.tasktussle

import me.wanttobee.tasktussle.commands.TaskTussleCommands
import me.wanttobee.tasktussle.config.TaskTussleConfig
import me.wanttobee.tasktussle.resourcepack.TaskTussleResourcePackServer
import me.wanttobee.tasktussle.sounds.TaskTussleSounds
import me.wanttobee.tasktussle.world.LobbyService
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level

object TaskTussleMod : DedicatedServerModInitializer {
	private val system = TaskTussleSystem()
	private val lobby = LobbyService()

	override fun onInitializeServer() {
		TaskTussleConfig.load()
		CommandRegistrationCallback.EVENT.register { dispatcher, _, _ -> TaskTussleCommands.register(dispatcher, system, lobby) }
		ServerLifecycleEvents.SERVER_STARTING.register { TaskTussleResourcePackServer.start() }
		ServerLifecycleEvents.SERVER_STARTED.register(system::prepare)
		ServerLifecycleEvents.SERVER_STOPPING.register { server -> system.shutdown(server); lobby.clear(server); TaskTussleResourcePackServer.stop() }
		ServerTickEvents.END_SERVER_TICK.register { server -> system.tick(server); lobby.tick(server); TaskTussleSounds.tick(server) }
		ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
			system.onJoin(handler.player)
			if (lobby.active) lobby.apply(handler.player)
			TaskTussleResourcePackServer.send(handler.player)
		}
		ServerPlayConnectionEvents.DISCONNECT.register { handler, _ -> if (lobby.active) lobby.remove(handler.player) }
		ServerPlayerEvents.AFTER_RESPAWN.register { _, player, _ -> system.onJoin(player) }
		ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, _, amount -> entity !is ServerPlayer || !lobby.active || amount < entity.health }
		UseItemCallback.EVENT.register { player, world, hand -> useTool(player, world, hand) }
		UseBlockCallback.EVENT.register { player, world, hand, _ -> useTool(player, world, hand) }
		UseEntityCallback.EVENT.register { player, world, hand, _, _ -> useTool(player, world, hand) }
	}

	private fun useTool(player: Player, world: Level, hand: InteractionHand): InteractionResult {
		if (world.isClientSide || player !is ServerPlayer) return InteractionResult.PASS
		val item = player.getItemInHand(hand)
		when {
			system.items.isSettingsTool(item) -> system.menus.openReadOnlySettings(player)
			system.items.isTaskBrowser(item) -> system.menus.openTaskBrowser(player)
			system.items.isCard(item) -> system.openCard(player)
			system.items.isTeamNavigator(item) -> system.teams.openTeamMaker(player)
			system.items.isTeamFinisher(item) -> system.teams.finishTeamSelection(player)
			else -> return InteractionResult.PASS
		}
		player.containerMenu.broadcastFullState()
		return InteractionResult.SUCCESS_SERVER
	}

	internal fun isDeliverySpot(stack: net.minecraft.world.item.ItemStack) = system.items.isDeliverySpot(stack)
	internal fun clickDeliverySpot(player: ServerPlayer, stack: net.minecraft.world.item.ItemStack) = system.clickDeliverySpot(player, stack)
}
