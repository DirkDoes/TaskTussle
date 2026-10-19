package me.wanttobee.tasktussle.tasks.tradeTask

import me.wanttobee.tasktussle.base.tasks.TaskEvent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.trading.MerchantOffer

/** Snapshot only a completed offer, including both payments and its result. */
data class TradeEvent(val items: Set<Identifier>) : TaskEvent {
	constructor(offer: MerchantOffer) : this(
		listOf(offer.costA, offer.costB, offer.result).filterNot { it.isEmpty }
			.mapTo(mutableSetOf()) { BuiltInRegistries.ITEM.getKey(it.item) },
	)
}
