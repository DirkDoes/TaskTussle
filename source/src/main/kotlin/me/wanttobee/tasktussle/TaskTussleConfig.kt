package me.wanttobee.tasktussle

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import net.fabricmc.loader.api.FabricLoader
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object TaskTussleConfig {
	private const val DEFAULT_URL = "http://127.0.0.1:25566/tasktussle-resource-pack.zip"
	private val gson = GsonBuilder().setPrettyPrinting().create()
	private val path by lazy { FabricLoader.getInstance().configDir.resolve("tasktussle/tasktussle.json") }

	var resourcePackEnabled = true
		private set
	var resourcePackUrl = DEFAULT_URL
		private set

	fun load() {
		path.parent.createDirectories()
		val values = if (path.exists()) runCatching { gson.fromJson(path.readText(), Values::class.java) }.getOrNull() ?: Values() else Values()
		resourcePackEnabled = values.resourcePackEnabled
		resourcePackUrl = values.resourcePackUrl.trim().ifBlank { DEFAULT_URL }
		path.writeText(gson.toJson(Values(resourcePackEnabled, resourcePackUrl)))
	}

	private data class Values(
		@SerializedName("resource_pack_enabled")
		val resourcePackEnabled: Boolean = true,
		@SerializedName("resource_pack_url")
		val resourcePackUrl: String = DEFAULT_URL,
	)
}
