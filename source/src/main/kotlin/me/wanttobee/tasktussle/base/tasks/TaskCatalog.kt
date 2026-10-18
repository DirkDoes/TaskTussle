package me.wanttobee.tasktussle.base.tasks

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer

object TaskCatalog {
	private val directory: Path by lazy { FabricLoader.getInstance().configDir.resolve("tasktussle") }

	fun prepare(server: MinecraftServer, catalogs: List<ITaskFiles>) {
		Files.createDirectories(directory)
		catalogs.flatMap { it.defaults }.forEach(::copyDefault)
		catalogs.forEach { writeYaml(path(it, it.generatedFile), it.generated(server)) }
	}

	fun load(server: MinecraftServer, catalog: ITaskFiles, fileName: String): TaskPools =
		readYaml(path(catalog, fileName)).filter { catalog.accepts(server, it) }

	fun removed(server: MinecraftServer, catalog: ITaskFiles, pools: TaskPools): List<Identifier> {
		val included = Difficulty.entries.flatMap(pools::get).toSet()
		return catalog.available(server).filterNot(included::contains).sortedBy(Identifier::toString).toList()
	}

	fun catalogFiles(catalog: ITaskFiles): List<CatalogFile> = files(catalog.prefix).take(MAX_FILES).map { CatalogFile(it, owner(catalog, it)) }

	fun owner(catalog: ITaskFiles, fileName: String): UUID? = parseOwner(Files.readAllLines(path(catalog, fileName)))

	fun totalFiles(catalog: ITaskFiles): Int = files(catalog.prefix).size

	fun ownedFiles(owner: UUID, catalogs: List<ITaskFiles>): Int = catalogs.sumOf { catalog -> files(catalog.prefix).count { owner(catalog, it) == owner } }

	fun create(catalog: ITaskFiles, owner: UUID, ownerName: String, pools: TaskPools): String {
		require(totalFiles(catalog) < MAX_FILES) { "This task list already has $MAX_FILES files." }
		val stem = ownerName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifEmpty { "player" }
		val existing = files(catalog.prefix).toSet()
		val name = generateSequence(1) { it + 1 }.map { "${catalog.prefix}${stem}_$it.yml" }.first { it !in existing }
		writeYaml(path(catalog, name), pools, owner)
		return name
	}

	fun delete(catalog: ITaskFiles, fileName: String) = Files.delete(path(catalog, fileName))

	fun update(catalog: ITaskFiles, fileName: String, pools: TaskPools) = writeYaml(path(catalog, fileName), pools, owner(catalog, fileName))

	internal fun parseOwner(lines: List<String>): UUID? {
		val value = lines.asSequence().map { it.substringBefore('#').trim() }
			.firstOrNull { it.startsWith("owner:", ignoreCase = true) }?.substringAfter(':')?.trim()
		return value?.takeIf(String::isNotEmpty)?.let { runCatching { UUID.fromString(it) }.getOrNull() }
	}

	internal fun parseYaml(lines: List<String>): TaskPools {
		val values = Difficulty.entries.associateWith { mutableListOf<Identifier>() }
		var section: Difficulty? = null
		for (raw in lines) {
			val line = raw.substringBefore('#').trim()
			if (line.isEmpty()) continue
			if (line.endsWith(':')) {
				section = when (line.dropLast(1).trim().lowercase()) {
					"easy" -> Difficulty.EASY
					"medium", "normal" -> Difficulty.MEDIUM
					"hard" -> Difficulty.HARD
					else -> null
				}
				continue
			}
			if (!line.startsWith('-') || section == null) continue
			Identifier.tryParse(line.drop(1).trim())?.let(values.getValue(section)::add)
		}
		return TaskPools(values.getValue(Difficulty.EASY), values.getValue(Difficulty.MEDIUM), values.getValue(Difficulty.HARD))
	}

	private fun readYaml(path: Path): TaskPools {
		require(path.startsWith(directory) && Files.isRegularFile(path)) { "Task file does not exist: ${path.fileName}" }
		return parseYaml(Files.readAllLines(path))
	}

	private fun safeName(name: String): String {
		val fileName = if (name.endsWith(".yml")) name else "$name.yml"
		require(fileName == Path.of(fileName).fileName.toString()) { "Task file must be inside config/tasktussle" }
		return fileName
	}

	private fun path(catalog: ITaskFiles, fileName: String): Path {
		val safe = safeName(fileName)
		require(safe.startsWith(catalog.prefix)) { "Task file has the wrong type." }
		return directory.resolve(safe)
	}

	private fun files(prefix: String): List<String> = Files.list(directory).use { paths ->
		paths.map { it.fileName.toString() }
			.filter { it.startsWith(prefix) && it.endsWith(".yml") && !it.contains("_all_") }
			.sorted()
			.toList()
	}

	private fun copyDefault(name: String) {
		val target = directory.resolve(name)
		if (Files.exists(target)) return
		checkNotNull(javaClass.getResourceAsStream("/$name")) { "Missing bundled task list: $name" }.use {
			Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
		}
	}

	private fun writeYaml(path: Path, pools: TaskPools, owner: UUID? = null) {
		val text = buildString {
			appendLine("# TaskTussle task catalog.")
			if (owner != null) appendLine("owner: $owner")
			for (difficulty in Difficulty.entries) {
				appendLine("${difficulty.yamlName}:")
				pools[difficulty].forEach { appendLine("  - $it") }
			}
		}
		Files.writeString(path, text)
	}

	private fun TaskPools.filter(predicate: (Identifier) -> Boolean) = TaskPools(
		easy.filter(predicate), medium.filter(predicate), hard.filter(predicate),
	)

	private const val MAX_FILES = 36
}
