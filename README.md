# Project VoidWalk

Project VoidWalk is a custom Fabric 1.21.10 Minecraft server-side PvP and Purge-event mod. The mod is designed to create high-risk player interactions through dynamic bounties, world events, supply drops, loot caches, and player-driven conflict systems.

## Technical Environment

- **Platform:** Fabric 1.21.10
- **Mappings:** Yarn
- **Package Root:** `io.github.dashtiss.voidwalk`

## Core Systems

### 💰 Bounty System
Players place bounties using physical diamonds.
- **Features:** UUID-based tracking, persistent storage (`voidwalk_bounties.json`), immediate save-on-update.
- **Claiming:** Bounty removed upon kill, items deposited directly to killer's inventory.

### 📦 Supply Drop System
Managed by `SupplyDropManager`.
- **Flow:** Dropship spawns 80 blocks above target, descends with particles, and lands as a locked loot container.
- **Capture:** Locked upon landing; requires player interaction/proximity to unlock (PvP hotspot creation).

### 🗺️ Loot Cache Events
Randomized world events spawned 500-750 blocks from the center of player activity (requires >= 5 players online).
- **Purpose:** Pull players away from bases and encourage exploration.

### 🕵️ Vanish & God Systems
- **Vanish:** Admin stealth (requires client-side VoidWalk mod).
- **God Framework:** Restricted to `dashtiss`. Provides utility functions (heal, speed, item spawning).

### ⚔️ Kill Tracking
Tracks player statistics to power future bounty rankings, heat systems, and leaderboards.

---

## Command Reference

| Command | Description |
| :--- | :--- |
| `/voidwalk IsModded` | Verify client-side mod installation |
| `/voidwalk drop` | Trigger supply drop |
| `/voidwalk vanish` | Toggle administrative stealth |
| `/voidwalk god [heal/speed/give]` | Developer utilities |
| `/voidwalk bounty [toggle/reset]` | Bounty system management |
| `/voidwalk cache` | Trigger loot cache event |

*Planned: `/wanted`, `/topbounties`, `/stats`, `/heat`, `/intel`, `/event`.*

---

## Development Constraints (1.21.10)

- **Server Access:** Use `player.getEntityWorld().getServer()`.
- **Inventory Access:** Use `inventory.getStack(i)` and `inventory.setStack(i, ItemStack.EMPTY)`. Do not access private fields.
- **User Lookup:** Cast `server.getApiServices().nameToIdCache()` to `net.minecraft.util.UserCache`.
- **Permissions:** Commands must use `.requires(source -> source.hasPermissionLevel(4))`.

## Design Philosophy

VoidWalk prioritizes organic conflict over traditional survival. Every system is built to drive players toward high-value objectives, forcing them to become visible, valuable, and vulnerable targets.

---

*This project is built for high-stakes PvP and emergent storytelling.*
