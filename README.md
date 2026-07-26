# TaskTussle

TaskTussle is a server-side Fabric mod for Minecraft 26.2 that runs team bingo games with block, advancement, and manual click tasks.

Players do not need a client mod. TaskTussle serves its required resource pack from the server for its custom card, sounds, particles, and interface textures.

## Requirements

- Minecraft 26.2
- Java 25
- Fabric Loader 0.19.3 or newer
- Fabric API
- Fabric Language Kotlin

## Build

From the `source` directory:

```shell
./gradlew build
```

On Windows, use `gradlew.bat build`. The mod JAR is written to `source/build/libs/`.

## Install

1. Put the TaskTussle JAR, Fabric API, and Fabric Language Kotlin in the dedicated server's `mods` directory.
2. Package the contents of `resourcepack/` as `tasktussle-resource-pack.zip` in the server directory.
3. Start the server once to generate `config/tasktussle/tasktussle.json`.
4. Set `resource_pack_url` to the externally reachable HTTP URL for the pack. TaskTussle hosts the ZIP itself on the URL's configured port.

The default task catalogs are copied to `config/tasktussle/` on first start. The balanced block and advancement catalogs are the recommended alternatives.

Use `/tt help` in game for commands.

## Repository layout

- `source/` — Kotlin source, tests, Gradle wrapper, and Fabric metadata
- `resourcepack/` — deployable resource-pack source
- `assets/` — original editable image and audio assets

Local Minecraft server directories are intentionally excluded from Git because they contain generated files and private player/server data.