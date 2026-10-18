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

The PvP setting between team selection and game time controls damage across teams: on, opt-in/out, or off. Teammates can always damage each other. In opt-in/out mode, players start opted in and can toggle the sword/peace icon beside their settings compass. This hotbar toggle remains available during team selection. The Teams item occupies the center hotbar slot, and the starter's detonator sits in the far-right slot. The PvP-off setting also uses the peace icon. Choices lock and the toggle disappears when a game starts; both players must be opted in for cross-team melee or player-fired projectile damage. Environmental damage such as lava is unchanged.

The compass opens **Task Tussle Settings Preview**; `/tt settings` opens the editable menu. `/tt worldsetup start` sets time to 0 and freezes it, and `/tt worldsetup clear` resumes time.

In opt-in/out mode, names above players and in the player list show an iron sword for opted-in players or the resource pack's `peacefull.png` for opted-out players. Icons update with each choice, remain during the game, and disappear in the other PvP modes.

## Repository layout

- `source/` — Kotlin source, tests, Gradle wrapper, and Fabric metadata
- `resourcepack/` — deployable resource-pack source
- `assets/` — original editable image and audio assets

### Code structure

The Fabric implementation follows the extension boundaries of TaskTussleLegacy. The implementation and game rules remain those of the Fabric version; Bukkit APIs and obsolete legacy task states are not carried over.

Packages under `source/src/main/kotlin/me/wanttobee/tasktussle/`:

| Package | Responsibility |
| --- | --- |
| Root | `TaskTussleMod` wires Fabric events, `TaskTussleSystem` coordinates the running game, and `TaskTussleGrouper` registers implementations. |
| `base/games` | `ITTGame`, `ITTGameManager`, `ITTGameTeam`, `ITTCardLogic`, and `ITTCardGui` define the game and card contracts. |
| `base/tasks` | `ITask`, `ITaskManager`, and `ITaskFiles`, plus task generation, catalog storage, task icons, and catalog menus. |
| `base/generic` | Shared manager contracts, settings, menu navigation, and interactive container base classes. |
| `games/bingo` | Bingo's card, team, game, generation rules, win conditions, and settings. |
| `tasks/obtainTask`, `tasks/achievementTask`, `tasks/clickTask` | Each task type's checks, presentation, manager, and settings; file-backed types also supply their catalog content. |
| `teams` | Team membership, manual selection, and team icons. |
| `pvp` | Cross-team damage rules and players' pre-game opt-in/out choices. |
| `commands`, `items`, `world`, `sounds`, `resourcepack`, `config`, `util`, `mixin` | Fabric integration and supporting services. |

To add a task type, extend `ITask`, implement `ITaskManager`, and register its manager in `TaskTussleGrouper.taskManagers`. Each generated task is a fresh instance, even when teams share the same template. Implement `ITaskFiles` for a file-backed task type, or supply a repeatable template for a task such as Click. Managers supply their own weight/settings accessors and settings menu; the shared generator, completion loop, and catalog browser dispatch through those contracts.

To add a game, implement `ITTGameManager`, `ITTGame`, and the card/team contracts, then register the manager in `TaskTussleGrouper.gameManagers`. The manager creates its game, supplies its card menu, and decides wins and overtime results. Start commands and the settings picker use the registry. Common timing, completion notifications, inventory tools, and lifecycle handling stay in `TaskTussleSystem` and its supporting services.

The current settings screen reserves nine task-type slots and nine game-mode slots; the catalog type picker has five slots. A larger registry will also need a larger or paginated picker.

Tests mirror the production packages under `source/src/test/kotlin`. Run `gradlew.bat test` or `gradlew.bat build` from `source` with Java 25. The focused suite covers task generation, win rules, item presentation and delivery, live settings views, catalog editing, and resource-pack assets and configuration. Multiplayer behavior still requires an in-game play-test.

Local Minecraft server directories are intentionally excluded from Git because they contain generated files and private player/server data.
