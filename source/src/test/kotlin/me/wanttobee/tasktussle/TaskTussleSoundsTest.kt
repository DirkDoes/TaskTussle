package me.wanttobee.tasktussle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskTussleSoundsTest {
	@Test
	fun selectsMenuSoundFromActionOutcome() {
		assertEquals(MenuClickSound.PAGE, menuClickSound(pageChanged = true, contentsChanged = true))
		assertEquals(MenuClickSound.BUTTON, menuClickSound(pageChanged = false, contentsChanged = true))
		assertNull(menuClickSound(pageChanged = false, contentsChanged = false))
	}

	@Test
	fun limitsEmitterToVanillaUnitSphere() {
		assertTrue(isTotemParticleOffset(0.0, 0.0, 0.0))
		assertTrue(isTotemParticleOffset(1.0, 0.0, 0.0))
		assertFalse(isTotemParticleOffset(1.0, 1.0, 0.0))
	}
}