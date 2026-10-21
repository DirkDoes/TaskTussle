package me.wanttobee.tasktussle.tasks.statusEffectTask

import me.wanttobee.tasktussle.base.tasks.TaskEvent
import net.minecraft.resources.Identifier

data class StatusEffectEvent(val effect: Identifier) : TaskEvent
