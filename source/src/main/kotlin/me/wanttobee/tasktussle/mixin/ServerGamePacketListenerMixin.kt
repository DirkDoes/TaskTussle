package me.wanttobee.tasktussle.mixin

import me.wanttobee.tasktussle.items.ForcedItems
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ServerGamePacketListenerImpl::class)
abstract class ServerGamePacketListenerMixin {
	@Shadow lateinit var player: ServerPlayer

	@Inject(method = ["handlePlayerAction"], at = [At("HEAD")], cancellable = true)
	private fun preventForcedItemAction(packet: ServerboundPlayerActionPacket, callback: CallbackInfo) {
		val locked = when (packet.action) {
			ServerboundPlayerActionPacket.Action.DROP_ITEM,
			ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS -> ForcedItems.isForcedItem(player.mainHandItem)
			ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND ->
				ForcedItems.isForcedItem(player.mainHandItem) || ForcedItems.isForcedItem(player.offhandItem)
			else -> false
		}
		if (locked) reject(callback)
	}

	@Inject(method = ["handleSetCreativeModeSlot"], at = [At("HEAD")], cancellable = true)
	private fun preventCreativeForcedItemMove(packet: ServerboundSetCreativeModeSlotPacket, callback: CallbackInfo) {
		val slot = packet.slotNum().toInt()
		if (
			ForcedItems.isForcedItem(packet.itemStack()) ||
			slot in 1..45 && ForcedItems.isForcedItem(player.inventoryMenu.getSlot(slot).item)
		) reject(callback)
	}

	private fun reject(callback: CallbackInfo) {
		player.level().server.execute { player.containerMenu.broadcastFullState() }
		callback.cancel()
	}
}
