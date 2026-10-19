package me.wanttobee.tasktussle.games.bingo

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
	fun bothLayoutsFitEveryTaskAndTeamWithoutOverlapping() {
		for (count in 1..15) {
			val layout = BingoCardLayout(count)
			assertEquals(25, layout.taskSlots.toSet().size)
			assertEquals(if (count <= 10) 10 else 15, layout.teamSlots.toSet().size)
			assertTrue(layout.teamSlots.size >= count)
			assertTrue((layout.taskSlots + layout.teamSlots).all { it in 0..44 })
			assertTrue(layout.taskSlots.intersect(layout.teamSlots.toSet()).isEmpty())
			assertEquals("Bingo", layout.title(false).string)
		}
		assertEquals(listOf(0, 9, 18, 27, 36, 8, 17, 26, 35, 44), BingoCardLayout(10).teamSlots)
		assertEquals(listOf(6, 7, 8, 15, 16, 17, 24, 25, 26, 33, 34, 35, 42, 43, 44), BingoCardLayout(11).teamSlots)
		assertEquals(listOf(2, 3, 4, 5, 6), BingoCardLayout(10).taskSlots.take(5))
		assertEquals(listOf(0, 1, 2, 3, 4), BingoCardLayout(11).taskSlots.take(5))
	}

	@Test
	fun titlesSelectTheMatchingTextureAtItsOriginalPixelSize() {
		val pack = Path.of("..", "resourcepack", "assets", "tasktussle")
		val providers = JsonParser.parseString(Files.readString(pack.resolve("font/bingo_card.json"))).asJsonObject.getAsJsonArray("providers")
		assertEquals(-8, providers[0].asJsonObject.getAsJsonObject("advances").get("\uF800").asInt)
		assertEquals(-169, providers[0].asJsonObject.getAsJsonObject("advances").get("\uF803").asInt)
		for ((count, texture) in listOf(10 to 1, 11 to 2, 15 to 2)) {
			val title = BingoCardLayout(count).title(true)
			val background = title.siblings.first()
			val provider = providers[texture].asJsonObject
			assertEquals(FontDescription.Resource(Identifier.parse("tasktussle:bingo_card")), background.style.font)
			assertEquals(0xFFFFFF, background.style.color?.value)
			assertEquals("\uF800" + provider.getAsJsonArray("chars")[0].asString + "\uF803Bingo", title.string)
			assertEquals("tasktussle:gui/bingo_card$texture.png", provider.get("file").asString)
			val definition = BitmapProvider.Definition.CODEC.codec().parse(JsonOps.INSTANCE, provider).orThrow
			val image = ImageIO.read(pack.resolve("textures/gui/bingo_card$texture.png").toFile())
			assertEquals(256, image.width)
			assertEquals(image.height, definition.height())
			assertEquals(13, definition.ascent())
		}
	}
}
