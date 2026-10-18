package me.wanttobee.tasktussle.base.generic

import me.wanttobee.tasktussle.TaskTussleSystem
import me.wanttobee.tasktussle.pvp.PvpMode
import net.minecraft.SharedConstants
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.registries.VanillaRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.Bootstrap
import net.minecraft.world.entity.EntityEquipment
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.Items
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SettingsMenuTest {
	@Test
	fun openViewersFollowSharedSettings() {
		SharedConstants.tryDetectVersion()
		Bootstrap.bootStrap()
		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup()).forEach { it.apply() }
		val system = TaskTussleSystem()
		// Rendering and broadcasting these menus do not access the inventory's owner.
		val viewers = (1..2).map {
			val inventory = Inventory::class.java.constructors.single().newInstance(null, EntityEquipment()) as Inventory
			SettingsMenu(it, inventory, system, true)
		}
		system.settings.hardRatio = 9
		system.settings.blockWeight = 7
		system.settings.handInItems = true
		viewers.forEach { menu ->
			menu.broadcastChanges()
			assertEquals("Hard Ratio: 9", menu.getSlot(3).item.get(DataComponents.CUSTOM_NAME)?.string)
			assertEquals(9, menu.getSlot(3).item.count)
			assertEquals("Obtain Task Ratio: 7", menu.getSlot(18).item.get(DataComponents.CUSTOM_NAME)?.string)
			assertEquals("Hand in items: on", menu.getSlot(18).item.get(DataComponents.LORE)?.lines?.first()?.string)
			val unchanged = menu.getSlot(3).item
			menu.broadcastChanges()
			assertSame(unchanged, menu.getSlot(3).item)
		}
		system.settings.hardRatio = 0
		viewers.forEach { menu ->
			menu.broadcastChanges()
			assertEquals("Hard Ratio: 0", menu.getSlot(3).item.get(DataComponents.CUSTOM_NAME)?.string)
			assertEquals(null, menu.getSlot(3).item.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))
		}
		for ((mode, icon) in listOf(PvpMode.ON to Items.IRON_SWORD, PvpMode.OPT_IN_OUT to Items.WOODEN_SWORD, PvpMode.OFF to Items.PAPER)) {
			system.settings.pvpMode = mode
			viewers.forEach { menu ->
				menu.broadcastChanges()
				assertEquals("Chose Teams: off", menu.getSlot(4).item.get(DataComponents.CUSTOM_NAME)?.string)
				assertEquals(icon, menu.getSlot(5).item.item)
				if (mode == PvpMode.OFF) assertEquals(Identifier.parse("tasktussle:peacefull"), menu.getSlot(5).item.get(DataComponents.ITEM_MODEL))
				assertEquals("PvP: ${mode.label}", menu.getSlot(5).item.get(DataComponents.CUSTOM_NAME)?.string)
				assertEquals(Items.CLOCK, menu.getSlot(6).item.item)
			}
		}
	}
}
