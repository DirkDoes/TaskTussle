package me.wanttobee.tasktussle.base.generic

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import net.minecraft.client.gui.font.providers.BitmapProvider
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.Identifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsMenuAssetsTest {
	@Test
	fun selectorUsesTheFullSizeBackgroundWithoutVisibleTitleText() {
		val pack = Path.of("..", "resourcepack", "assets", "tasktussle")
		val font = JsonParser.parseString(Files.readString(pack.resolve("font/game_selector.json"))).asJsonObject
		val providers = font["providers"].asJsonArray
		assertEquals(-8, providers[0].asJsonObject["advances"].asJsonObject["\uF800"].asInt)
		val bitmap = providers[1].asJsonObject
		assertEquals("tasktussle:gui/backgrounds/game_selector.png", bitmap["file"].asString)
		val definition = BitmapProvider.Definition.CODEC.codec().parse(JsonOps.INSTANCE, bitmap).orThrow
		val image = ImageIO.read(pack.resolve("textures/gui/backgrounds/game_selector.png").toFile())
		assertEquals(256, image.width)
		assertEquals(image.height, definition.height())
		assertEquals(13, definition.ascent())
		val title = SettingsMenu.title(true)
		assertEquals("\uF800\uF801", title.string)
		assertEquals(FontDescription.Resource(Identifier.parse("tasktussle:game_selector")), title.style.font)
		assertEquals("", SettingsMenu.title(false).string)
	}

	@Test
	fun markerUsesTheOriginalSixteenPixelTextureUnderTheStandardItemAtlasDirectory() {
		val assets = Path.of("..", "resourcepack", "assets")
		val item = JsonParser.parseString(Files.readString(assets.resolve("tasktussle/items/game_marker.json"))).asJsonObject
		assertEquals("tasktussle:item/ui/navigation/game_marker", item["model"].asJsonObject["model"].asString)
		val model = JsonParser.parseString(Files.readString(assets.resolve("tasktussle/models/item/ui/navigation/game_marker.json"))).asJsonObject
		assertEquals("minecraft:item/generated", model["parent"].asString)
		assertEquals("tasktussle:item/ui/navigation/game_marker", model["textures"].asJsonObject["layer0"].asString)
		val image = ImageIO.read(assets.resolve("tasktussle/textures/item/ui/navigation/game_marker.png").toFile())
		assertEquals(16, image.width)
		assertEquals(16, image.height)
	}

	@Test
	fun playAndCogUseTheirSuppliedTextures() {
		val assets = Path.of("..", "resourcepack", "assets")
		for ((name, texture) in listOf("play_button" to "item/ui/navigation/play_button", "cog" to "item/shared/cog")) {
			val item = JsonParser.parseString(Files.readString(assets.resolve("tasktussle/items/$name.json"))).asJsonObject
			assertEquals("tasktussle:$texture", item["model"].asJsonObject["model"].asString)
			val model = JsonParser.parseString(Files.readString(assets.resolve("tasktussle/models/$texture.json"))).asJsonObject
			assertEquals("minecraft:item/generated", model["parent"].asString)
			assertEquals("tasktussle:$texture", model["textures"].asJsonObject["layer0"].asString)
			val image = ImageIO.read(assets.resolve("tasktussle/textures/$texture.png").toFile())
			assertTrue(image.width > 0 && image.height > 0)
		}
	}
}
