package me.wanttobee.tasktussle.sounds

internal enum class MenuClickSound { BUTTON, PAGE }

internal fun menuClickSound(pageChanged: Boolean, contentsChanged: Boolean): MenuClickSound? = when {
	pageChanged -> MenuClickSound.PAGE
	contentsChanged -> MenuClickSound.BUTTON
	else -> null
}
