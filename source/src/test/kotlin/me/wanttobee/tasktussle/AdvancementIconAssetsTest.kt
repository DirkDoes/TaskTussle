package me.wanttobee.tasktussle

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdvancementIconAssetsTest {
	private val pack = Path.of("..", "resourcepack", "assets", "tasktussle")
	private val definition = json(pack.resolve("items/advancement_icon.json"))
	private val layers = definition["model"].asJsonObject["models"].asJsonArray

	@Test
	fun genericSelectorsCoverEveryAdvancementAndAllowIndependentChoices() {
		assertTrue(TaskTussleSettings().customAdvancementIcons)
		assertFalse(definition.has("oversized_in_gui"))
		assertEquals("minecraft:composite", definition["model"].asJsonObject["type"].asString)
		assertEquals(2, layers.size())
		assertEquals(0, layers[0].asJsonObject["index"].asInt)
		assertEquals(1, layers[1].asJsonObject["index"].asInt)
		val items = layers[1].asJsonObject["cases"].asJsonArray.map { it.asJsonObject["when"].asString }
		assertTrue(items.size > 1500)
		assertEquals(items.size, items.toSet().size)
		assertEquals(126, advancementIcons.size)
		val encoded = Files.readString(pack.resolve("items/advancement_icon.json"))
		for ((id, style) in advancementIcons) {
			assertTrue(style.item in items, "$id selects a missing item")
			assertTrue(style.plate in listOf("task", "goal", "challenge"))
			assertFalse(encoded.contains(id), "Advancement choices belong in Kotlin")
		}
		for (item in listOf("minecraft:diamond", "minecraft:grass_block", "minecraft:dragon_head")) {
			for (context in listOf("gui", "fixed", "firstperson_righthand")) {
				val expected = icon("task", item, context)
				assertEquals(expected, icon("goal", item, context))
				assertEquals(expected, icon("challenge", item, context))
			}
		}
		assertEquals(icon("task", "minecraft:knowledge_book", "gui"), icon("task", "missing:item", "gui"))
		assertEquals(plate("task", "minecraft:diamond", "gui"), plate("missing", "minecraft:diamond", "gui"))
	}

	@Test
	fun inventoryPresetsAndFrameTexturesStayUnchanged() {
		val positions = mapOf(
			"minecraft:blaze_rod" to listOf(0.125, 0.125, 1.0),
			"minecraft:grass_block" to listOf(0.25, 0.25, 0.0),
			"minecraft:carved_pumpkin" to listOf(0.25, 0.25, 0.0),
			"minecraft:dragon_head" to listOf(0.25, 0.0, 0.0),
			"minecraft:red_bed" to listOf(0.1875, 0.1875, 0.0),
			"minecraft:shield" to listOf(0.125, 0.125, 1.0),
		)
		for ((item, expected) in positions) {
			val t = icon("task", item, "gui")["transformation"].asJsonObject
			assertEquals(expected, t["translation"].asJsonArray.map { it.asDouble })
			assertEquals(listOf(0.75, 0.75, 0.75), t["scale"].asJsonArray.map { it.asDouble })
			val frame = plate("task", item, "gui")
			if (expected[2] == 0.0) {
				assertEquals(-2.0, frame["transformation"].asJsonObject["translation"].asJsonArray[2].asDouble)
			} else {
				assertFalse(frame.has("transformation"))
			}
		}
		for (frame in listOf("task", "goal", "challenge")) {
			assertFalse(json(pack.resolve("models/item/advancement_$frame.json")).has("display"))
			val image = ImageIO.read(pack.resolve("textures/item/ui_items/advancements/advancement_$frame.png").toFile())
			assertTrue(image.width > 0)
			assertEquals(image.width, image.height)
		}
	}

	@Test
	fun fixedViewMountsIconsOnThickerPlatesAndShowsPumpkinFront() {
		// ScreenEffectRenderer uses FIXED for the totem animation triggered by /display.
		val common = Matrix4f().rotateY(kotlin.math.PI.toFloat()).translate(-0.5f, -0.5f, -0.5f)
		val blockFixed = Matrix4f().scale(0.5f).translate(-0.5f, -0.5f, -0.5f)
		// Rear vertices and native transforms from vanilla items, blocks and DragonHeadModel.
		val samples = listOf(
			Triple("minecraft:blaze_rod", Vector3f(0f, 0f, 7.5f / 16), Matrix4f(common)),
			Triple("minecraft:grass_block", Vector3f(0f, 0f, 1f), Matrix4f(blockFixed)),
			Triple("minecraft:carved_pumpkin", Vector3f(0f, 0f, 1f), Matrix4f(blockFixed)),
			Triple("minecraft:dragon_head", Vector3f(0.375f, 0.00062503125f, 0.28125f), Matrix4f().translate(0f, 0.25f, 0f).mul(common)),
		)
		for ((item, vertex, native) in samples) {
			val frame = matrix(plate("task", item, "fixed"))
			assertEquals(0.4375f, frame.transformPosition(Vector3f(0.5f, 0.5f, 7.5f / 16)).z, 0.000001f)
			assertEquals(0.5625f, frame.transformPosition(Vector3f(0.5f, 0.5f, 8.5f / 16)).z, 0.000001f)
			val leaf = icon("task", item, "fixed")["models"].asJsonArray.single().asJsonObject
			val placed = Matrix4f(common).invert().mul(native).mul(matrix(leaf)).transformPosition(vertex)
			assertEquals(9f / 16 + 1f / 128, placed.z, 0.000001f, "$item must meet the plate without clipping")
		}
		val pumpkin = icon("goal", "minecraft:carved_pumpkin", "fixed")["models"].asJsonArray.single().asJsonObject
		val front = Matrix4f(common).invert().mul(blockFixed).mul(matrix(pumpkin)).transformDirection(Vector3f(0f, 0f, -1f))
		assertTrue(front.x > 0 && front.z > 0, "The pumpkin's north face must point toward the viewer, as in its inventory pose")
	}

	@Test
	fun descriptionsWrapAtWordBoundaries() {
		assertEquals(listOf("One two", "three", "oversizedword"), wrapLore("One two three oversizedword", 9))
		assertEquals(emptyList(), wrapLore("   ", 9))
	}

	private fun icon(plate: String, item: String, context: String) = resolve(layers[1].asJsonObject, plate, item, context)
	private fun plate(plate: String, item: String, context: String) = resolve(layers[0].asJsonObject, plate, item, context)
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

	private fun matrix(node: JsonObject): Matrix4f {
		val t = node["transformation"]
		if (t.isJsonArray) return Matrix4f().set(t.asJsonArray.map { it.asFloat }.toFloatArray()).transpose()
		val o = t.asJsonObject
		val p = o["translation"].asJsonArray.map { it.asFloat }
		val q = o["left_rotation"].asJsonArray.map { it.asFloat }
		val s = o["scale"].asJsonArray.map { it.asFloat }
		return Matrix4f().translation(p[0], p[1], p[2]).rotate(Quaternionf(q[0], q[1], q[2], q[3])).scale(s[0], s[1], s[2])
	}

	private fun json(path: Path) = JsonParser.parseString(Files.readString(path)).asJsonObject
}
