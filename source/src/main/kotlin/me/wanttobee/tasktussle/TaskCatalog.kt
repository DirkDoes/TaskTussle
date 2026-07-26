package me.wanttobee.tasktussle

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.Items
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

enum class Difficulty(val yamlName: String) { EASY("Easy"), MEDIUM("Medium"), HARD("Hard") }

data class TaskPools(
	val easy: List<Identifier>,
	val medium: List<Identifier>,
	val hard: List<Identifier>,
) {
	operator fun get(difficulty: Difficulty): List<Identifier> = when (difficulty) {
		Difficulty.EASY -> easy
		Difficulty.MEDIUM -> medium
		Difficulty.HARD -> hard
	}
}

object TaskCatalog {
	private val directory: Path by lazy { FabricLoader.getInstance().configDir.resolve("tasktussle") }
	private val defaults = listOf(
		"blocks_default.yml",
		"blocks_balanced.yml",
		"advancements_default.yml",
		"advancements_balanced.yml",
	)

	fun prepare(server: MinecraftServer) {
		Files.createDirectories(directory)
		defaults.forEach(::copyDefault)
		writeYaml(directory.resolve("blocks_all_26_2.yml"), allBlocks())
		writeYaml(directory.resolve("advancements_all_26_2.yml"), allAdvancements(server))
	}

	fun blocks(fileName: String): TaskPools = readYaml(directory.resolve(safeName(fileName))).filter { id ->
		BuiltInRegistries.BLOCK.containsKey(id) && BuiltInRegistries.BLOCK.getValue(id).asItem() != Items.AIR
	}

	fun advancements(server: MinecraftServer, fileName: String): TaskPools = readYaml(directory.resolve(safeName(fileName))).filter {
		server.advancements.get(it)?.value?.display?.isPresent == true
	}

	fun blockFiles(): List<String> = files("blocks_")

	fun advancementFiles(): List<String> = files("advancements_")

	internal fun parseYaml(lines: List<String>): TaskPools {
		val values = Difficulty.entries.associateWith { mutableListOf<Identifier>() }
		var section: Difficulty? = null
		for (raw in lines) {
			val line = raw.substringBefore('#').trim()
			if (line.isEmpty()) continue
			if (line.endsWith(':')) {
				section = when (line.dropLast(1).trim().lowercase()) {
					"easy" -> Difficulty.EASY
					"medium", "normal" -> Difficulty.MEDIUM
					"hard" -> Difficulty.HARD
					else -> null
				}
				continue
			}
			if (!line.startsWith('-') || section == null) continue
			Identifier.tryParse(line.drop(1).trim())?.let(values.getValue(section)::add)
		}
		return TaskPools(values.getValue(Difficulty.EASY), values.getValue(Difficulty.MEDIUM), values.getValue(Difficulty.HARD))
	}

	private fun readYaml(path: Path): TaskPools {
		require(path.startsWith(directory) && Files.isRegularFile(path)) { "Task file does not exist: ${path.fileName}" }
		return parseYaml(Files.readAllLines(path))
	}

	private fun safeName(name: String): String {
		val fileName = if (name.endsWith(".yml")) name else "$name.yml"
		require(fileName == Path.of(fileName).fileName.toString()) { "Task file must be inside config/tasktussle" }
		return fileName
	}

	private fun files(prefix: String): List<String> = Files.list(directory).use { paths ->
		paths.map { it.fileName.toString() }
			.filter { it.startsWith(prefix) && it.endsWith(".yml") && !it.contains("_all_") }
			.sorted()
			.toList()
	}

	private fun copyDefault(name: String) {
		val target = directory.resolve(name)
		if (Files.exists(target)) return
		checkNotNull(javaClass.getResourceAsStream("/$name")) { "Missing bundled task list: $name" }.use {
			Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
		}
	}

	private fun allBlocks(): TaskPools {
		val grouped = BuiltInRegistries.BLOCK.keySet()
			.asSequence()
			.filter { id ->
				val item = BuiltInRegistries.BLOCK.getValue(id).asItem()
				item != Items.AIR && BuiltInRegistries.ITEM.getKey(item) == id && BlockDifficulty.isObtainable(id.path)
			}
			.sortedBy(Identifier::toString)
			.groupBy { BlockDifficulty.rank(it.path) }
		return TaskPools(
			grouped[Difficulty.EASY].orEmpty(),
			grouped[Difficulty.MEDIUM].orEmpty(),
			grouped[Difficulty.HARD].orEmpty(),
		)
	}

	private fun allAdvancements(server: MinecraftServer): TaskPools {
		val grouped = server.advancements.allAdvancements
			.asSequence()
			.filter { it.value.display.isPresent }
			.map { it.id }
			.filter { it.namespace == "minecraft" }
			.sortedBy(Identifier::toString)
			.groupBy { AdvancementDifficulty.rank(it.path) }
		return TaskPools(
			grouped[Difficulty.EASY].orEmpty(),
			grouped[Difficulty.MEDIUM].orEmpty(),
			grouped[Difficulty.HARD].orEmpty(),
		)
	}

	private fun writeYaml(path: Path, pools: TaskPools) {
		val text = buildString {
			appendLine("# Generated from the Minecraft 26.2 registry. Copy this file before editing it.")
			for (difficulty in Difficulty.entries) {
				appendLine("${difficulty.yamlName}:")
				pools[difficulty].forEach { appendLine("  - $it") }
			}
		}
		Files.writeString(path, text)
	}

	private fun TaskPools.filter(predicate: (Identifier) -> Boolean) = TaskPools(
		easy.filter(predicate), medium.filter(predicate), hard.filter(predicate),
	)
}

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
