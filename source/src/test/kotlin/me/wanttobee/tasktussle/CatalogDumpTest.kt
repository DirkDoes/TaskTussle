package me.wanttobee.tasktussle

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.Bootstrap
import net.minecraft.SharedConstants
import net.minecraft.world.item.Items
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue

class CatalogDumpTest {
	@Test
	fun minecraft262BlocksAreClassified() {
		SharedConstants.tryDetectVersion()
		Bootstrap.bootStrap()
		val ids = BuiltInRegistries.BLOCK.keySet().filter { id ->
			val item = BuiltInRegistries.BLOCK.getValue(id).asItem()
			item != Items.AIR && BuiltInRegistries.ITEM.getKey(item) == id && BlockDifficulty.isObtainable(id.path)
		}
		val output = java.nio.file.Path.of("build/generated-catalogs/blocks_balanced.yml")
		Files.createDirectories(output.parent)
		Files.writeString(output, buildString {
			appendLine("# Complete survival-obtainable Minecraft 26.2 block catalog, ranked for TaskTussle.")
			for (difficulty in Difficulty.entries) {
				appendLine("${difficulty.yamlName}:")
				ids.filter { BlockDifficulty.rank(it.path) == difficulty }.sortedBy { it.toString() }.forEach { appendLine("  - $it") }
			}
		})
		assertTrue(ids.size > 600, "Expected the full Minecraft 26.2 block registry, got ${ids.size}")
	}
}
