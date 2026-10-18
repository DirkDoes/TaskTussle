package me.wanttobee.tasktussle.tasks.achievementTask

import me.wanttobee.tasktussle.base.tasks.Difficulty

object AdvancementDifficulty {
	private val hard = setOf(
		"nether/all_effects", "nether/all_potions", "nether/create_full_beacon", "nether/uneasy_alliance", "nether/explore_nether",
		"nether/netherite_armor", "nether/summon_wither", "nether/ride_strider_in_overworld_lava",
		"adventure/adventuring_time", "adventure/kill_all_mobs", "adventure/arbalistic", "adventure/two_birds_one_arrow", "adventure/overoverkill",
		"adventure/very_very_frightening", "adventure/blowback", "adventure/heart_transplanter", "adventure/spear_many_mobs", "adventure/who_needs_rockets",
		"adventure/trim_with_all_exclusive_armor_patterns", "husbandry/bred_all_animals", "husbandry/balanced_diet",
		"husbandry/complete_catalogue", "husbandry/leash_all_frog_variants", "husbandry/froglights", "husbandry/whole_pack",
	)
	private val mediumWords = listOf(
		"nether/", "end/", "diamond", "enchant", "village", "bastion", "fortress", "ancient", "trial", "raid", "sniffer",
		"totem", "trident", "sniper", "bullseye", "world_height", "sculk", "exclusive", "allay", "axolotl", "goat", "wolf_armor",
	)

	fun rank(path: String): Difficulty = when {
		path in hard || path.startsWith("end/") -> Difficulty.HARD
		mediumWords.any(path::contains) -> Difficulty.MEDIUM
		else -> Difficulty.EASY
	}
}
