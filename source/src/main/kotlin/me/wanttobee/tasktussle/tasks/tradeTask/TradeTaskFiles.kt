package me.wanttobee.tasktussle.tasks.tradeTask

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.Difficulty
import me.wanttobee.tasktussle.base.tasks.ITaskFiles
import me.wanttobee.tasktussle.base.tasks.TaskPools
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.trading.VillagerTrade

object TradeTaskFiles : ITaskFiles {
	override val prefix = "trades_"
	override val defaults = listOf("trades_default.yml")
	override val generatedFile = "trades_all_26_2.yml"
	override val browserName = "Trade Tasks"
	override val managerKey = "trade"
	override fun selectedFile(settings: TaskTussleSettings) = settings.tradesFile
	override fun selectFile(settings: TaskTussleSettings, file: String) { settings.tradesFile = file }
	override fun accepts(server: MinecraftServer, id: Identifier) = BuiltInRegistries.ITEM.containsKey(id) && BuiltInRegistries.ITEM.getValue(id) != Items.AIR
	override fun available(server: MinecraftServer) = itemLevels(server).keys.asSequence()
	override fun name(server: MinecraftServer?, id: Identifier) = ItemStack(BuiltInRegistries.ITEM.getValue(id)).hoverName.string
	override fun icon(server: MinecraftServer?, settings: TaskTussleSettings, id: Identifier) = ItemStack(BuiltInRegistries.ITEM.getValue(id))
	override fun description(server: MinecraftServer?, id: Identifier) = "Buy or sell this item in any completed trade"

	override fun generated(server: MinecraftServer) = poolsForLevels(itemLevels(server))

	private fun itemLevels(server: MinecraftServer): Map<Identifier, Int> {
		val registries = server.reloadableRegistries().lookup()
		val sets = registries.lookupOrThrow(Registries.TRADE_SET)
		val ops = registries.createSerializationContext(JsonOps.INSTANCE)
		val levels = mutableMapOf<Identifier, Int>()
		// Follow profession trade sets, so wandering-trader-only offers never enter the pool.
		for (profession in BuiltInRegistries.VILLAGER_PROFESSION) {
			for (entry in profession.tradeSetsByLevel().int2ObjectEntrySet()) {
				for (trade in sets.getOrThrow(entry.value).value().trades) {
					val json = VillagerTrade.CODEC.encodeStart(ops, trade.value()).orThrow.asJsonObject
					for (item in tradeItems(json)) levels.merge(item, entry.intKey, ::minOf)
				}
			}
		}
		return levels
	}

	internal fun poolsForLevels(levels: Map<Identifier, Int>): TaskPools {
		val grouped = levels.keys.sortedBy(Identifier::toString).groupBy { id ->
			when (levels.getValue(id)) { 1 -> Difficulty.EASY; 2 -> Difficulty.MEDIUM; 3 -> Difficulty.HARD; else -> null }
		}
		return TaskPools(grouped[Difficulty.EASY].orEmpty(), grouped[Difficulty.MEDIUM].orEmpty(), grouped[Difficulty.HARD].orEmpty())
	}

	internal fun tradeItems(trade: JsonObject): Set<Identifier> = buildSet {
		for (field in listOf("wants", "additional_wants")) {
			trade.getAsJsonObject(field)?.get("id")?.asString?.let { add(Identifier.parse(it)) }
		}
		var result = Identifier.parse(trade.getAsJsonObject("gives").get("id").asString)
		for (modifier in trade.getAsJsonArray("given_item_modifiers") ?: JsonArray()) {
			// These vanilla modifiers change the item type, not just its components.
			when (modifier.asJsonObject.get("function").asString) {
				"minecraft:exploration_map" -> if (result == Identifier.withDefaultNamespace("map")) result = Identifier.withDefaultNamespace("filled_map")
				"minecraft:enchant_randomly", "minecraft:enchant_with_levels" -> if (result == Identifier.withDefaultNamespace("book")) result = Identifier.withDefaultNamespace("enchanted_book")
			}
		}
		add(result)
	}
}
