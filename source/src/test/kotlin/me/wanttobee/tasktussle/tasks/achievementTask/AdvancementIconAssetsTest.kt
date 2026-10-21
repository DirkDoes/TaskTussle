package me.wanttobee.tasktussle.tasks.achievementTask

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdvancementIconAssetsTest {
	@Test
	fun advancementModelsCoverAllIconsAndCompletionDisplays() {
		val pack = Path.of("..", "resourcepack", "assets", "tasktussle")
		val model = JsonParser.parseString(Files.readString(pack.resolve("items/advancement_icon.json"))).asJsonObject["model"].asJsonObject
		assertEquals("minecraft:composite", model["type"].asString)
		val layers = model["models"].asJsonArray
		assertEquals(2, layers.size())
		assertEquals(0, layers[0].asJsonObject["index"].asInt)
		val guiItems = layers[1].asJsonObject["fallback"].asJsonObject
		assertEquals(1, guiItems["index"].asInt)
		val items = guiItems["cases"].asJsonArray.flatMap {
			val keys = it.asJsonObject["when"]
			if (keys.isJsonArray) keys.asJsonArray.map { key -> key.asString } else listOf(keys.asString)
		}
		assertEquals(items.size, items.toSet().size)
		for ((id, style) in advancementIcons) {
			assertTrue(style.item in items, "$id selects a missing item")
			assertTrue(Files.exists(pack.resolve("models/item/advancement_${style.plate}.json")))
			// The completion animation uses FIXED, while the card uses GUI.
			for (context in listOf("gui", "fixed")) {
				val icon = resolve(layers[1].asJsonObject, style.plate, style.item, context)
				assertTrue(icon["type"].asString != "minecraft:empty")
				val plate = resolve(layers[0].asJsonObject, style.plate, style.item, context)
				assertTrue(plate.toString().contains("tasktussle:item/advancement_${style.plate}"))
			}
		}
	}

	private fun resolve(node: JsonObject, plate: String, item: String, context: String): JsonObject {
		if (node["type"].asString != "minecraft:select") return node
		val value = when (node["property"].asString) {
			"minecraft:display_context" -> context
			"minecraft:custom_model_data" -> if (node["index"].asInt == 0) plate else item
			else -> return node
		}
		val selected = node["cases"].asJsonArray.firstOrNull {
			val keys = it.asJsonObject["when"]
			if (keys.isJsonArray) keys.asJsonArray.any { key -> key.asString == value } else keys.asString == value
		}?.asJsonObject?.get("model") ?: node["fallback"]
		return resolve(selected.asJsonObject, plate, item, context)
	}
}
