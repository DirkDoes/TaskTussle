package me.wanttobee.tasktussle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TaskTussleResourcePackServerTest {
	@Test
	fun readsResourcePackHostPortAndPath() {
		assertEquals(ResourcePackEndpoint(null, 25566, "/tasktussle-resource-pack.zip"), resourcePackEndpoint("http://play.example.com:25566/tasktussle-resource-pack.zip"))
		assertEquals(ResourcePackEndpoint("127.0.0.1", 25566, "/pack.zip"), resourcePackEndpoint("http://127.0.0.1:25566/pack.zip"))
		assertFailsWith<IllegalArgumentException> { resourcePackEndpoint("https://play.example.com/pack.zip") }
	}
}
