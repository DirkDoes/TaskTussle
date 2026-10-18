package me.wanttobee.tasktussle.pvp

import net.minecraft.resources.Identifier
import net.minecraft.world.item.Items

enum class PvpMode(val label: String) {
	ON("On"), OPT_IN_OUT("Opt-in/out"), OFF("Off");

	val icon get() = when (this) {
		ON -> Items.IRON_SWORD
		OPT_IN_OUT -> Items.WOODEN_SWORD
		OFF -> Items.PAPER
	}
	val iconModel get() = if (this == OFF) Identifier.fromNamespaceAndPath("tasktussle", "peacefull") else null

	fun allowsDamage(sameTeam: Boolean, attackerOptedIn: Boolean, targetOptedIn: Boolean): Boolean =
		sameTeam || when (this) {
			ON -> true
			OPT_IN_OUT -> attackerOptedIn && targetOptedIn
			OFF -> false
		}
}
