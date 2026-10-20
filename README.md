# TaskTussle

TaskTussle is a server-side Fabric mod for Minecraft 26.2. Games share a generic task system, so new games and task types can be added independently. Players only need the server's resource pack; no client mod is required.

## Games

| Game | Summary |
| --- | --- |
| Bingo | Complete tasks on a 5×5 card to meet the chosen win condition. Supports 1–12 teams. |
| More games | Planned, using the same task system. |

## Tasks

| Task | Summary |
| --- | --- |
| Obtain | Collect the requested block, or hand it in when delivery mode is enabled. |
| Advancement | Earn the requested Minecraft advancement. |
| Click | Click the task on your card to complete it. |
| Trade | Complete a trade that buys or sells the requested item. Any merchant counts, including wandering traders. |

## Other features

- Random or player-selected teams, with shared task progress.
- Editable task catalogs, difficulty ratios, game timers, and card visibility settings.
- Cross-team PvP modes: on, off, or mutual opt-in. Teammate PvP stays enabled.
- Custom menus, task displays, sounds, and an optional lobby with parkour.

## Server setup

1. Set up a Minecraft **26.2** server with **Java 25** and **Fabric Loader 0.19.3+**.
2. Put the **TaskTussle**, **Fabric API**, and **Fabric Language Kotlin** JARs in the server's `mods/` folder. Use dependency versions compatible with Minecraft 26.2.
3. Zip the contents of `resourcepack/` as **`tasktussle-resource-pack.zip`**, with `pack.mcmeta` and `assets/` directly inside the ZIP. Place it in the server's root folder.
4. Start and stop the server once. In `config/tasktussle/tasktussle.json`, keep `resource_pack_enabled` set to `true` and set `resource_pack_url` to `http://YOUR_SERVER_ADDRESS:25566/tasktussle-resource-pack.zip`. Allow TCP port **25566** through your firewall/router so players can download it. The default `127.0.0.1` address is for testing on the same computer.
5. Restart the server. TaskTussle hosts and sends the pack automatically. As an operator, use `/tt settings`, then `/tt start bingo 2` to start a two-team game.

Task catalogs are generated in `config/tasktussle/` and can be managed through the in-game Task Browser.

To build the mod yourself, run `.\gradlew.bat build` from `source/` (`./gradlew build` on Linux/macOS). The server mod is `source/build/libs/tasktussle-<version>.jar`.

## Commands

All commands require operator permissions except `/card` and `/display`. `/tt` also accepts `/taskTussle` and `/tasktussle`. Arguments in `<brackets>` are required.

| Command | Purpose |
| --- | --- |
| `/tt help` | Show game and settings commands. |
| `/tt settings` | Open editable game settings. |
| `/tt start bingo <teams>` | Start Bingo with 1–12 teams. |
| `/tt stop` | Stop and clear the current game. |
| `/tt finish draw` | End the game as a draw. |
| `/tt finish out_of_time` | End the game using its time-limit rules. |
| `/card` | Open your team's card. |
| `/display` | Show the item you are holding in the task display. |
| `/teams help` | Show team commands. |
| `/teams list` | List current teams. |
| `/teams generateTeams <amount>` | Create random teams. |
| `/teams teamMaker <amount>` | Start manual team selection. |
| `/teams clearAll` | Clear teams and stop any running game. |
| `/tt worldsetup help` | Show lobby commands. |
| `/tt worldsetup start` | Enable the lobby and freeze world time at dawn. |
| `/tt worldsetup clear` | Clear the lobby and resume world time. |
| `/tt worldsetup create_parkour` | Add lobby parkour. |
| `/tt worldsetup remove_parkour` | Remove lobby parkour. |
| `/tt debug status` | Show current game status. |
| `/tt debug log` | Toggle console logging. |
| `/debug_tt help` | Show diagnostic commands. |
| `/debug_tt <topic>` | Inspect `task_events`, `hotbar_items`, `interactive_inventories`, `teams`, or `timers`. |
