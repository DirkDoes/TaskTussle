package me.wanttobee.tasktussle.resourcepack

import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import net.minecraft.SharedConstants
import net.minecraft.client.gui.font.providers.BitmapProvider
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.Identifier
import net.minecraft.server.Bootstrap
import net.minecraft.world.inventory.MenuType

class ChestMenuTitleTest {
	@Test
	fun restoresEveryChestSizeWithoutChangingNamesOrOtherMenuTypes() {
		SharedConstants.tryDetectVersion()
		Bootstrap.bootStrap()
		val assets = Path.of("..", "resourcepack", "assets")
		val providers = JsonParser.parseString(Files.readString(assets.resolve("tasktussle/font/chest.json"))).asJsonObject.getAsJsonArray("providers")
		val advances = providers[0].asJsonObject.getAsJsonObject("advances")
		assertEquals(-8, advances.get("\uF800").asInt)
		assertEquals(-169, advances.get("\uF807").asInt)
		val types = listOf(MenuType.GENERIC_9x1, MenuType.GENERIC_9x2, MenuType.GENERIC_9x3, MenuType.GENERIC_9x4, MenuType.GENERIC_9x5, MenuType.GENERIC_9x6)
		for ((index, type) in types.withIndex()) {
			val rows = index + 1
			val provider = providers[rows].asJsonObject
			for (name in listOf(Component.translatable("container.chest"), Component.literal("My named chest"))) {
				val title = chestMenuTitle(name, type)
				assertSame(name, title.siblings.last())
				val background = title.siblings.first()
				assertEquals(FontDescription.Resource(Identifier.parse("tasktussle:chest")), background.style.font)
				assertEquals(0xFFFFFF, background.style.color?.value)
				assertEquals("\uF800" + provider.getAsJsonArray("chars")[0].asString + "\uF807", background.string)
			}
			val definition = BitmapProvider.Definition.CODEC.codec().parse(JsonOps.INSTANCE, provider).orThrow
			val texture = provider.get("file").asString.removePrefix("tasktussle:")
			val image = ImageIO.read(assets.resolve("tasktussle/textures/$texture").toFile())
			assertEquals(176, image.width)
			assertEquals(114 + rows * 18, image.height)
			assertEquals(image.height, definition.height())
			assertEquals(13, definition.ascent())
		}
		val name = Component.literal("Other menu")
		for (type in listOf(MenuType.HOPPER, MenuType.GENERIC_3x3, MenuType.SHULKER_BOX, MenuType.FURNACE)) assertSame(name, chestMenuTitle(name, type))
		val transparent = ImageIO.read(assets.resolve("minecraft/textures/gui/container/generic_54.png").toFile())
		assertTrue(transparent.getRGB(0, 0, transparent.width, transparent.height, null, 0, transparent.width).all { it ushr 24 == 0 })
		Files.list(assets.resolve("minecraft/lang")).use { languages ->
			languages.forEach { assertEquals("", JsonParser.parseString(Files.readString(it)).asJsonObject.get("container.inventory").asString) }
		}
	}
}
