# Backrooms Mod - AI Agent Guide

## Project Overview
Fabric mod for Minecraft 1.21.10 implementing a "Ghost Mode" vanish feature with client-server verification. Package: `io.github.dashtiss.backrooms`

## Architecture
- **Dual entrypoints**: `Backrooms` (server/main) + `BackroomsClient` (client)
- **Networking**: Custom payloads via Fabric Networking API v1
- **State tracking**: Server-side `Set<UUID>` for authenticated clients and vanished players
- **Commands**: Brigadier-based `/backrooms vanish|list`

## Key Files
| File | Purpose |
|------|---------|
| `src/main/java/io/github/dashtiss/backrooms/Backrooms.java` | Main initializer, packet handlers, vanish logic, server tick particles |
| `src/main/java/io/github/dashtiss/backrooms/client/BackroomsClient.java` | Client init, keybind (V), handshake on join |
| `src/main/java/io/github/dashtiss/backrooms/commands/BackroomsCommand.java` | `/backrooms vanish` (toggles armor/invisibility), `/backrooms list` (OP only) |
| `src/main/java/io/github/dashtiss/backrooms/HandshakePayload.java` | C2S/S2C handshake record (empty payload) |
| `src/main/java/io/github/dashtiss/backrooms/VanishTogglePayload.java` | C2S vanish toggle record |
| `src/main/resources/fabric.mod.json` | Mod metadata, entrypoints, dependencies |

## Developer Workflows
```bash
# Build
./gradlew build

# Run client (dev environment)
./gradlew runClient

# Run server
./gradlew runServer

# Generate sources for IDE
./gradlew genSources
```

## Conventions & Patterns
- **Java 21**, UTF-8 encoding enforced in `build.gradle`
- **Records** for network payloads (immutable, auto-codec via `PacketCodec.unit`)
- **Static sets** in main class for global state (`AUTHENTICATED_CLIENTS`, `VANISHED_PLAYERS`)
- **Server-thread execution**: `context.server().execute(() -> { ... })` for packet handlers
- **Keybind category**: Custom `Identifier.of("backrooms", "category")`
- **Lang keys**: `key.backrooms.vanish`, `category.backrooms.general` in `assets/backrooms/lang/en_us.json`

## Networking Flow
1. Client joins → `ClientPlayConnectionEvents.JOIN` → sends `HandshakePayload`
2. Server receives → adds player UUID to `AUTHENTICATED_CLIENTS`
3. Client presses **V** → sends `VanishTogglePayload`
4. Server toggles vanish via `Backrooms.toggleVanish()` (unloads entity, broadcasts REMOVE_PLAYER packet, spawns SOUL_FIRE_FLAME particles each tick)

## Adding Features
- **New payload**: Create record implementing `CustomPayload` with `ID` and `CODEC`, register in `Backrooms.onInitialize()`
- **New command**: Add subcommand in `BackroomsCommand.register()`, use `Backrooms.AUTHENTICATED_CLIENTS` for mod-check
- **Client-only logic**: Implement in `BackroomsClient.onInitializeClient()`

## Dependencies (gradle.properties)
- Minecraft: 1.21.10
- Yarn mappings: 1.21.10+build.3
- Fabric Loader: 0.19.3
- Fabric API: 0.138.4+1.21.10
- DevAuth (modRuntimeOnly): 1.2.2
