package me.wanttobee.tasktussle.games.bingo

import me.wanttobee.tasktussle.base.games.TaskGridLayout
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.minecraft.client.gui.font.providers.BitmapProvider
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.Identifier

class BingoCardLayoutTest {
	@Test
	fun sixRowsFitTheCardTwelveTeamsAndTheViewedTeamWithoutOverlapping() {
		val layout = TaskGridLayout()
		assertEquals(6, layout.rows)
		assertEquals(1..12, BingoManager.teamRange)
		assertEquals(25, layout.taskSlots.toSet().size)
		assertEquals(listOf(0, 9, 18, 27, 36, 45, 8, 17, 26, 35, 44, 53), layout.teamSlots)
		assertEquals(listOf(11, 12, 13, 14, 15), layout.taskSlots.take(5))
		assertEquals(listOf(47, 48, 49, 50, 51), layout.taskSlots.takeLast(5))
		assertEquals(4, layout.viewedTeamSlot)
		val occupied = layout.taskSlots + layout.teamSlots + layout.viewedTeamSlot
		assertEquals(occupied.size, occupied.toSet().size)
		assertTrue(occupied.all { it in 0 until layout.rows * 9 })
		assertEquals("", layout.title(false).string)
	}

	@Test
	fun titleUsesOnlyTheBigTextureAtItsOriginalPixelSize() {
		val pack = Path.of("..", "resourcepack", "assets", "tasktussle")
		val providers = JsonParser.parseString(Files.readString(pack.resolve("font/bingo_card.json"))).asJsonObject.getAsJsonArray("providers")
		assertEquals(-8, providers[0].asJsonObject.getAsJsonObject("advances").get("\uF800").asInt)
		assertEquals(2, providers.size())
		val title = TaskGridLayout().title(true)
		val provider = providers[1].asJsonObject
		assertEquals(FontDescription.Resource(Identifier.parse("tasktussle:bingo_card")), title.style.font)
		assertEquals(0xFFFFFF, title.style.color?.value)
		assertEquals("\uF800" + provider.getAsJsonArray("chars")[0].asString, title.string)
		assertEquals("tasktussle:gui/bingo_card_big.png", provider.get("file").asString)
		val definition = BitmapProvider.Definition.CODEC.codec().parse(JsonOps.INSTANCE, provider).orThrow
		val image = ImageIO.read(pack.resolve("textures/gui/bingo_card_big.png").toFile())
		assertEquals(256, image.width)
		assertEquals(image.height, definition.height())
		assertEquals(13, definition.ascent())
		assertEquals(0, image.getRGB(35, 20) ushr 24)
	}

	@Test
	fun lockoutHasAnIndependentFontAndTextureWithTheSameLayout() {
		val pack = Path.of("..", "resourcepack", "assets", "tasktussle")
		val layout = TaskGridLayout(Identifier.parse("tasktussle:lockout_card"))
		assertEquals(FontDescription.Resource(Identifier.parse("tasktussle:lockout_card")), layout.title(true).style.font)
		val providers = JsonParser.parseString(Files.readString(pack.resolve("font/lockout_card.json"))).asJsonObject.getAsJsonArray("providers")
		assertEquals("tasktussle:gui/lockout_card_big.png", providers[1].asJsonObject["file"].asString)
		val image = ImageIO.read(pack.resolve("textures/gui/lockout_card_big.png").toFile())
		assertEquals(256, image.width)
		assertEquals(image.height, providers[1].asJsonObject["height"].asInt)
		assertTrue(Files.exists(pack.resolve("items/lockout_card.json")))
		assertTrue(Files.exists(pack.resolve("textures/item/lockout_card.png")))
	}
}
