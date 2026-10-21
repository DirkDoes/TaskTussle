package me.wanttobee.tasktussle.tasks.statusEffectTask

import me.wanttobee.tasktussle.base.generic.TaskTussleSettings
import me.wanttobee.tasktussle.base.tasks.Difficulty
import me.wanttobee.tasktussle.base.tasks.ITaskFiles
import me.wanttobee.tasktussle.base.tasks.TaskPools
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object StatusEffectTaskFiles : ITaskFiles {
	override val prefix = "status_effects_"
	override val defaults = listOf("status_effects_default.yml")
	override val generatedFile = "status_effects_all_26_2.yml"
	override val browserName = "Status Effect Tasks"
	override val managerKey = "status_effect"
	override fun selectedFile(settings: TaskTussleSettings) = settings.statusEffectsFile
	override fun selectFile(settings: TaskTussleSettings, file: String) { settings.statusEffectsFile = file }
	override fun accepts(server: MinecraftServer, id: Identifier) = BuiltInRegistries.MOB_EFFECT.containsKey(id)
	override fun available(server: MinecraftServer) = effectIds()
	override fun name(server: MinecraftServer?, id: Identifier) = BuiltInRegistries.MOB_EFFECT.getValue(id)?.displayName?.string
		?: id.path.replace('_', ' ').replaceFirstChar(Char::uppercase)
	override fun icon(server: MinecraftServer?, settings: TaskTussleSettings, id: Identifier) = ItemStack(Items.PAPER).apply {
		set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("tasktussle", "status_effect/${id.path}"))
	}
	override fun description(server: MinecraftServer?, id: Identifier) = "Have this status effect applied to you"
	override fun generated(server: MinecraftServer) = pools()

	internal fun effectIds() = BuiltInRegistries.MOB_EFFECT.keySet().asSequence().sortedBy(Identifier::toString)
	internal fun pools(): TaskPools {
		val grouped = effectIds().groupBy { difficulty(it.path) }
		return TaskPools(grouped[Difficulty.EASY].orEmpty(), grouped[Difficulty.MEDIUM].orEmpty(), grouped[Difficulty.HARD].orEmpty())
	}

	internal fun difficulty(effect: String): Difficulty = when (effect) {
		// Common foods, mobs, suspicious stew, and straightforward brewing.
		"absorption", "blindness", "dolphins_grace", "hunger", "instant_health", "jump_boost", "nausea",
		"night_vision", "poison", "regeneration", "speed", "weakness" -> Difficulty.EASY
		"bad_omen", "breath_of_the_nautilus", "fire_resistance", "glowing", "infested", "instant_damage",
		"invisibility", "oozing", "raid_omen", "resistance", "slow_falling", "slowness", "strength",
		"trial_omen", "water_breathing", "weaving", "wind_charged" -> Difficulty.MEDIUM
		// Structures, bosses, beacon/conduit setup, and effects only available through commands.
		else -> Difficulty.HARD
	}
}
