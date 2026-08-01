package me.wanttobee.tasktussle

import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdvancementIconAssetsTest {
	@Test
	fun compositeIconsUseSquareFramesBehindNativeIcons() {
		assertTrue(TaskTussleSettings().customAdvancementIcons)
		val pack = Path.of("..", "resourcepack", "assets", "tasktussle")
		val definition = json(pack.resolve("items/advancement_icon.json"))
		assertFalse(definition.has("oversized_in_gui"))
		val cases = definition["model"].asJsonObject["cases"].asJsonArray
		assertEquals(126, cases.size())
		val loweredSpecialIcons = setOf(
			"minecraft:end/kill_dragon", "minecraft:nether/loot_bastion",
			"minecraft:adventure/craft_decorated_pot_using_only_sherds", "minecraft:nether/get_wither_skull",
		)
		for (case in cases) {
			val layers = case.asJsonObject["model"].asJsonObject["models"].asJsonArray
			assertEquals("minecraft:composite", case.asJsonObject["model"].asJsonObject["type"].asString)
			assertEquals(2, layers.size())
			val first = layers[0].asJsonObject
			val blockIcon = first["type"]?.asString == "minecraft:composite"
			if (blockIcon) {
				assertEquals(listOf(0.0, 0.0, -2.0), first["transformation"].asJsonObject["translation"].asJsonArray.map { it.asDouble })
				assertTrue(first["models"].asJsonArray.single().asJsonObject["model"].asString.startsWith("tasktussle:item/advancement_"))
				val icon = layers[1].asJsonObject
				assertEquals(listOf(0.75, 0.75, 0.75), icon["transformation"].asJsonObject["scale"].asJsonArray.map { it.asDouble })
				val expectedTranslation = when (case.asJsonObject["when"].asString) {
					in loweredSpecialIcons -> listOf(0.25, 0.0, 0.0)
					"minecraft:adventure/sleep_in_bed" -> listOf(0.1875, 0.1875, 0.0)
					else -> listOf(0.25, 0.25, 0.0)
				}
				assertEquals(expectedTranslation, icon["transformation"].asJsonObject["translation"].asJsonArray.map { it.asDouble })
			} else {
				assertTrue(first["model"].asString.startsWith("tasktussle:item/advancement_"))
				val icon = layers[1].asJsonObject
				assertEquals(listOf(0.75, 0.75, 0.75), icon["transformation"].asJsonObject["scale"].asJsonArray.map { it.asDouble })
				assertEquals(listOf(0.125, 0.125, 1.0), icon["transformation"].asJsonObject["translation"].asJsonArray.map { it.asDouble })
			}
		}
		val byId = cases.associate { it.asJsonObject["when"].asString to it.asJsonObject["model"].asJsonObject["models"].asJsonArray }
		val blockPlate = byId.getValue("minecraft:adventure/bullseye")[0].asJsonObject
		assertEquals(listOf(0.0, 0.0, -2.0), blockPlate["transformation"].asJsonObject["translation"].asJsonArray.map { it.asDouble })
		assertEquals(listOf(0.75, 0.75, 0.75), byId.getValue("minecraft:adventure/bullseye")[1].asJsonObject["transformation"].asJsonObject["scale"].asJsonArray.map { it.asDouble })
		assertTrue(byId.getValue("minecraft:adventure/kill_a_mob")[0].asJsonObject["model"].asString.startsWith("tasktussle:item/advancement_"))
		assertTrue(byId.getValue("minecraft:husbandry/plant_seed")[0].asJsonObject["model"].asString.startsWith("tasktussle:item/advancement_"))
		assertEquals(1.0, byId.getValue("minecraft:husbandry/plant_seed")[1].asJsonObject["transformation"].asJsonObject["translation"].asJsonArray[2].asDouble)
		assertTrue(byId.getValue("minecraft:husbandry/obtain_sniffer_egg")[0].asJsonObject["model"].asString.startsWith("tasktussle:item/advancement_"))
		assertEquals(byId.getValue("minecraft:husbandry/plant_seed"), byId.getValue("minecraft:husbandry/breed_an_animal"))
		for (frame in listOf("task", "goal", "challenge")) {
			assertFalse(json(pack.resolve("models/item/advancement_$frame.json")).has("display"))
			val image = ImageIO.read(pack.resolve("textures/item/ui_items/advancements/advancement_$frame.png").toFile())
			assertTrue(image.width > 0)
			assertEquals(image.width, image.height)
		}
	}

	@Test
	fun descriptionsWrapAtWordBoundaries() {
		assertEquals(listOf("One two", "three", "oversizedword"), wrapLore("One two three oversizedword", 9))
		assertEquals(emptyList(), wrapLore("   ", 9))
	}
	private fun json(path: Path) = JsonParser.parseString(Files.readString(path)).asJsonObject
}
