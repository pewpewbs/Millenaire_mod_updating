# Millénaire 8.1.2 → Minecraft 1.18.2 / Forge 40.3.0

## Status: NOT A PLAYABLE PORT

The requested scope is the full original mod, preserving all mechanics. No subsystem has been removed from the main compilation source set. The Java game integration is still for 1.12.2 and must be rewritten; metadata changes alone do not port it.

## Completed preparation

- Official Forge 40.3.0 MDK build configuration, Gradle wrapper, local Java 17.
- All 324 decompiled Java source files and original resources copied from the user's source folder.
- Recovered six syntactically invalid CFR files using Vineflower 1.11.1 from the same original JAR. CFR originals preserved under migration/original-cfr.
- All 324 source files pass Java 17 syntax parsing (21 syntax errors before recovery, zero after). This does not check types, Minecraft APIs, or behavior.
- Two independent pathfinding classes compile with Java 17; their integration and in-game behavior have not been tested.
- Converted 23 packaged language files to JSON, retaining existing keys and adding modern block/item aliases. Kept custom culture translation files unchanged.
- Added modern mod metadata and resource pack format 8. Excluded legacy metadata from processed resources.

## Required next work

1. Make the full Java compilation pass: Forge lifecycle/events, registry construction, Minecraft class/member mappings, and remaining decompiler type errors.
2. Port blocks/items, metadata variants, models, recipes, loot, inventories, and menus together so old culture definitions still resolve correctly.
3. Port villagers, navigation/AI, synchronized entity state, client rendering and animations.
4. Port village generation, terrain sampling including negative world heights, building construction, world saving and chunk loading.
5. Port packets and server validation, trading, quests, reputation, diplomacy, raids, children, player-controlled villages, books and interfaces.
6. Verify each culture and mechanic in a new client world and dedicated server. Verify save/reload. Existing 1.12 worlds are not claimed compatible.

## Reproduce checks

Run `./build-local.ps1` in PowerShell to compile all sources. Local JDK and Gradle caches do not replace system Java. The generated build is not ready to run in a game.

`tools/ParseSources.java` parses Java syntax without resolving dependencies. Run it with local JDK 17 and the `src/main/java` directory as the argument.

See migration-audit.json for the initial 80,472-line source inventory and 327 distinct Minecraft/Forge imports. See migration/recovered-files.json for the recovery list. Build logs record actual build failures; no successful full build or game run is claimed.
