package me.wanttobee.tasktussle.util

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

fun message(text: String, color: ChatFormatting): Component = Component.literal(text).withStyle(color)

fun tagged(text: String, color: ChatFormatting): Component = Component.literal("[").withStyle(ChatFormatting.GRAY)
	.append(Component.literal("Task Tussle").withStyle(ChatFormatting.GOLD))
	.append(Component.literal("] ").withStyle(ChatFormatting.GRAY))
	.append(Component.literal(text).withStyle(color))
