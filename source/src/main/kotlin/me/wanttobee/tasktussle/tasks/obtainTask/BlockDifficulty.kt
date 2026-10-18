package me.wanttobee.tasktussle.tasks.obtainTask

import me.wanttobee.tasktussle.base.tasks.Difficulty

object BlockDifficulty {
	private val unobtainable = setOf(
		"air", "cave_air", "void_air", "water", "lava", "bedrock", "barrier", "structure_void", "structure_block",
		"jigsaw", "light", "spawner", "trial_spawner", "end_portal_frame", "reinforced_deepslate", "budding_amethyst",
		"farmland", "dirt_path", "frosted_ice", "petrified_oak_slab", "suspicious_sand", "suspicious_gravel",
		"tall_grass", "large_fern", "frogspawn", "vault", "test_block", "test_instance_block",
	)
	private val hardWords = listOf(
		"ancient_debris", "netherite", "diamond_block", "emerald_block", "beacon", "conduit", "shulker", "purpur",
		"chorus", "end_stone", "dragon", "wither", "skull", "head", "sculk", "coral", "sponge", "sea_lantern",
		"ochre_froglight", "verdant_froglight", "pearlescent_froglight", "sniffer_egg", "heavy_core", "creaking_heart",
		"resin", "weathered", "oxidized",
	)
	private val mediumWords = listOf(
		"nether", "crimson", "warped", "blackstone", "basalt", "quartz", "soul_", "glowstone", "magma", "respawn_anchor",
		"deepslate", "tuff", "amethyst", "copper", "redstone", "lapis", "gold", "iron_block", "obsidian", "crying_obsidian",
		"prismarine", "ice", "snow", "mangrove", "cherry", "pale_oak", "bamboo", "mud", "moss", "dripstone",
		"bee", "honey", "slime", "terracotta", "concrete", "glazed", "packed_mud", "decorated_pot", "crafter",
	)

	fun isObtainable(path: String): Boolean = path !in unobtainable && path != "command_block" && !path.startsWith("infested_") && !path.endsWith("_command_block")

	fun rank(path: String): Difficulty = when {
		hardWords.any(path::contains) -> Difficulty.HARD
		mediumWords.any(path::contains) -> Difficulty.MEDIUM
		else -> Difficulty.EASY
	}
}
