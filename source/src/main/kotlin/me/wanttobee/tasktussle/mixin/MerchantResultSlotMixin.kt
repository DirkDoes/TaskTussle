package me.wanttobee.tasktussle.mixin

import me.wanttobee.tasktussle.TaskTussleMod
import me.wanttobee.tasktussle.tasks.tradeTask.TradeEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MerchantContainer
import net.minecraft.world.inventory.MerchantResultSlot
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Final
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(MerchantResultSlot::class)
abstract class MerchantResultSlotMixin {
	@Shadow @Final private lateinit var slots: MerchantContainer
	@Unique private var completedTrade: TradeEvent? = null

	@Inject(method = ["onTake"], at = [At("HEAD")])
	private fun resetTrade(player: Player, stack: ItemStack, callback: CallbackInfo) { completedTrade = null }

	@Inject(method = ["onTake"], at = [At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/Merchant;notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V")])
	private fun captureTrade(player: Player, stack: ItemStack, callback: CallbackInfo) {
		if (player is ServerPlayer) completedTrade = slots.activeOffer?.let(::TradeEvent)
	}

	@Inject(method = ["onTake"], at = [At("RETURN")])
	private fun finishTrade(player: Player, stack: ItemStack, callback: CallbackInfo) {
		val event = completedTrade ?: return
		completedTrade = null
		// Completing a winning task can close the menu; finish the trade's slot updates first.
		if (player is ServerPlayer) TaskTussleMod.onTaskEvent(player, event)
	}
}
