package me.wanttobee.tasktussle

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket
import net.minecraft.server.level.ServerPlayer
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.HexFormat
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.io.path.exists
import kotlin.io.path.inputStream

object TaskTussleResourcePackServer {
	private val gameDirectory = FabricLoader.getInstance().gameDir
	private var server: HttpServer? = null
	private var executor: ExecutorService? = null
	private var packHash = ""

	var active = false
		private set

	fun start() {
		if (!TaskTussleConfig.resourcePackEnabled || server != null) return
		val pack = gameDirectory.resolve(PACK_FILE)
		if (!pack.exists()) {
			LOGGER.warn("Resource pack is enabled, but {} does not exist. Custom models stay disabled.", pack)
			return
		}
		val endpoint = runCatching { resourcePackEndpoint(TaskTussleConfig.resourcePackUrl) }.getOrElse { error ->
			LOGGER.error("Invalid TaskTussle resource-pack URL: {}", TaskTussleConfig.resourcePackUrl, error)
			return
		}

		runCatching {
			val address = endpoint.bindAddress?.let { InetSocketAddress(it, endpoint.port) } ?: InetSocketAddress(endpoint.port)
			val http = HttpServer.create(address, 0)
			val pool = Executors.newCachedThreadPool { task -> Thread(task, "TaskTussle resource pack HTTP").apply { isDaemon = true } }
			http.createContext(endpoint.path) { exchange -> serve(exchange, pack, endpoint.path) }
			http.executor = pool
			http.start()
			server = http
			executor = pool
			packHash = sha1(pack)
			active = true
			LOGGER.info("Serving the TaskTussle resource pack on port {} at {}.", endpoint.port, endpoint.path)
		}.onFailure { error ->
			LOGGER.error("Could not start the TaskTussle resource pack server on port {}.", endpoint.port, error)
		}
	}

	fun send(player: ServerPlayer) {
		if (!active) return
		player.connection.send(ClientboundResourcePackPushPacket(
			PACK_ID,
			TaskTussleConfig.resourcePackUrl,
			packHash,
			true,
			Optional.of(Component.literal("TaskTussle uses custom game textures.")),
		))
	}

	fun stop() {
		server?.stop(0)
		executor?.shutdownNow()
		server = null
		executor = null
		active = false
	}

	private fun serve(exchange: HttpExchange, pack: Path, path: String) {
		exchange.use {
			if (exchange.requestURI.path != path) {
				exchange.sendResponseHeaders(404, -1)
				return
			}
			if (exchange.requestMethod != "GET" && exchange.requestMethod != "HEAD") {
				exchange.responseHeaders.set("Allow", "GET, HEAD")
				exchange.sendResponseHeaders(405, -1)
				return
			}

			val size = Files.size(pack)
			exchange.responseHeaders.set("Content-Type", "application/zip")
			exchange.responseHeaders.set("Content-Length", size.toString())
			exchange.responseHeaders.set("Cache-Control", "no-cache")
			if (exchange.requestMethod == "HEAD") {
				exchange.sendResponseHeaders(200, -1)
				return
			}
			exchange.sendResponseHeaders(200, size)
			pack.inputStream().use { input -> input.copyTo(exchange.responseBody) }
		}
	}

	private fun sha1(path: Path): String {
		val digest = MessageDigest.getInstance("SHA-1")
		Files.newInputStream(path).use { input ->
			val buffer = ByteArray(8192)
			while (true) {
				val read = input.read(buffer)
				if (read < 0) break
				digest.update(buffer, 0, read)
			}
		}
		return HexFormat.of().formatHex(digest.digest())
	}

	private const val PACK_FILE = "tasktussle-resource-pack.zip"
	private val PACK_ID: UUID = UUID.nameUUIDFromBytes("tasktussle-resource-pack".toByteArray())
	private val LOGGER = LoggerFactory.getLogger(TaskTussleResourcePackServer::class.java)
}

internal data class ResourcePackEndpoint(val bindAddress: String?, val port: Int, val path: String)

internal fun resourcePackEndpoint(url: String): ResourcePackEndpoint {
	val uri = URI(url)
	require(uri.scheme.equals("http", ignoreCase = true)) { "The embedded host supports http:// URLs only." }
	require(!uri.host.isNullOrBlank()) { "The resource-pack URL needs a host." }
	val port = if (uri.port == -1) 80 else uri.port
	require(port in 1..65535) { "The resource-pack URL needs a valid port." }
	require(uri.path.isNotBlank() && uri.path != "/") { "The resource-pack URL needs a file path." }
	val bindAddress = "127.0.0.1".takeIf { uri.host == it || uri.host.equals("localhost", ignoreCase = true) }
	return ResourcePackEndpoint(bindAddress, port, uri.path)
}
