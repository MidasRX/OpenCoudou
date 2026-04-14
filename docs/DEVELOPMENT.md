# OpenCoudou — Full Developer & Contributor Documentation

> **OpenCoudou** is a ground-up rewrite of [OpenComputers](https://github.com/MightyPirates/OpenComputers) for **Minecraft 1.21.4** on **NeoForge 21.4.0-beta**, written in **Kotlin**.  
> It adds programmable, Lua-powered computers, screens, keyboards, robots, and drones to the game.

---

## Table of Contents

1. [What Is This Project?](#what-is-this-project)
2. [Game Version & Platform](#game-version--platform)
3. [Prerequisites & Installing Dependencies](#prerequisites--installing-dependencies)
4. [Building from Source](#building-from-source)
5. [Running the Mod (Dev / Production)](#running-the-mod)
6. [Project Structure — Where Everything Lives](#project-structure--where-everything-lives)
7. [Architecture — How It Works](#architecture--how-it-works)
8. [Data Flow — GPU Write to In-World Screen](#data-flow--gpu-write-to-in-world-screen)
9. [Known Bugs & Current Issues](#known-bugs--current-issues)
10. [Remaining TODOs](#remaining-todos)
11. [Debugging Guide — READ THIS FIRST](#debugging-guide--read-this-first)
12. [What Went Wrong & Lessons Learned](#what-went-wrong--lessons-learned)
13. [Contributing](#contributing)

---

## What Is This Project?

OpenCoudou is a **complete rewrite** of the classic OpenComputers Minecraft mod. The original was written in Scala for Minecraft 1.7–1.12. This version targets modern Minecraft (1.21.4) with NeoForge and Kotlin.

**What works right now (v3.0.0):**
- Computer Case block with inventory (CPU, RAM, GPU, HDD slots)
- Screen blocks (Tier 1–3) with in-world text rendering
- Keyboard input forwarded to Lua VM
- LuaJ-based Lua 5.4 runtime (sandboxed, timeout-protected)
- GPU component API (set, fill, copy, setForeground, setBackground, getResolution)
- Filesystem component with VFS (read/write files from HDD items)
- Built-in BIOS that auto-discovers bootable media
- Full OpenOS included (60+ shell commands, 20+ libraries)
- Internet component (HTTP GET/POST)
- Redstone component (per-side input/output)
- Network sync: screen buffer synced server→client via custom packets
- Power LED on computer case (green = on, red = off)
- Computer running sound loop
- Creative tab with all items
- Screen touch/drag/scroll events
- Keyboard clipboard paste support

**What does NOT work yet:**
- Robots / Drones (entities exist but logic is stubbed)
- 3D Printing
- Hologram projector
- Cable networking between computers
- Multi-block screens
- Several GUIs (Printer, Server Rack send no packets)
- Power system / energy consumption (no real drain)
- Proper component bus / node network (simplified direct approach used)
- MachineRegistry / NetworkHandler (commented out)

---

## Game Version & Platform

| Property | Value |
|----------|-------|
| Minecraft | **1.21.4** |
| Mod Loader | **NeoForge 21.4.0-beta** |
| Language | **Kotlin 2.1.0** (JVM target 21) |
| Java | **21** (required) |
| Lua Runtime | **LuaJ 3.0.1** (bundled in JAR via jarJar) |
| Mod ID | `opencomputers` |
| Mod Version | `3.0.0` |
| Gradle | Wrapper included, NeoForge ModDev plugin 2.0.28-beta |

---

## Prerequisites & Installing Dependencies

### For Players

1. Install **Java 21** (or higher) — [Adoptium](https://adoptium.net/)
2. Install **Minecraft 1.21.4**
3. Install the **NeoForge** loader for 1.21.4 — [NeoForge Downloads](https://neoforged.net/)
4. Drop the `opencomputers-3.0.0.jar` into your `.minecraft/mods/` folder
5. Launch Minecraft with your NeoForge profile

### For Developers

1. **Java 21 JDK** — `java -version` must show 21+
2. **Git** — to clone the repo
3. **IDE** — IntelliJ IDEA recommended (Kotlin support built-in)

```bash
git clone https://github.com/MidasRX/OpenCoudou.git
cd OpenCoudou
```

Gradle will automatically download all dependencies on first build:
- **NeoForge 21.4.0-beta** (Minecraft modding framework)
- **LuaJ 3.0.1** (Lua VM for JVM — bundled into the mod JAR)
- **Kotlin stdlib 2.x** (bundled into mod JAR)
- **Kotlin coroutines 1.8.0** (bundled into mod JAR)
- **Kotlin reflect 2.x** (bundled into mod JAR)
- **Gson** (JSON — already in Minecraft, not bundled)

No manual dependency installation is needed. Gradle handles everything.

---

## Building from Source

### Quick Build

```bash
# Windows
gradlew.bat build

# Linux/Mac
./gradlew build
```

Output JAR: `build/libs/opencomputers-3.0.0.jar`

### Deploy to Minecraft

```powershell
# Windows — copy to mods folder
copy build\libs\opencomputers-3.0.0.jar %APPDATA%\.minecraft\mods\
```

### Run in Dev Environment

```bash
# Client (opens Minecraft with mod loaded + hot reload)
gradlew.bat runClient

# Server
gradlew.bat runServer

# Data generation (recipes, models, loot tables)
gradlew.bat runData
```

### Gradle Properties

In `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4G    # 4GB heap for Gradle
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
```

If you run out of memory during build, increase `-Xmx`.

---

## Running the Mod

### In-Game Setup (Minimum Viable Computer)

1. Place a **Computer Case (Tier 1)**
2. Right-click it, insert: **CPU**, **RAM**, **HDD** (or EEPROM), **GPU**
3. Place a **Screen** block adjacent to the case (within 8 blocks)
4. Place a **Keyboard** adjacent to the screen
5. Press the **power button** in the Case GUI (or send a redstone signal)
6. The built-in BIOS boots → looks for `/init.lua` on the HDD → runs OpenOS
7. You get a shell prompt: `/ # _`

### Screen Detection

The computer scans for screens in a **±8 X/Z, ±4 Y** box around the case.
It connects to the **first screen found** (scan order: X ascending → Y ascending → Z ascending).
Only ONE screen is connected at a time.

---

## Project Structure — Where Everything Lives

```
OpenCoudou/
├── build.gradle.kts            # Build config, dependencies, NeoForge setup
├── gradle.properties           # JVM args, caching flags
├── settings.gradle.kts         # Project name, plugin repos
│
├── src/main/kotlin/li/cil/oc/
│   ├── OpenComputers.kt        # @Mod entry point, registry wiring
│   ├── Settings.kt             # Mod configuration values
│   │
│   ├── api/                    # Internal API interfaces
│   │   ├── driver/Driver.kt
│   │   ├── fs/FileSystem.kt
│   │   ├── machine/            # Architecture, Machine, MachineHost, Context
│   │   └── network/            # Component, Node, Network interfaces
│   │
│   ├── client/                 # CLIENT-SIDE ONLY
│   │   ├── ClientSetup.kt     # Registers renderers, screens, key bindings
│   │   ├── Sound.kt           # Computer running sound loop
│   │   ├── gui/               # 20+ GUI screens (Case, Robot, Disk, etc.)
│   │   ├── input/             # KeyboardInputHandler (key→packet)
│   │   └── renderer/
│   │       ├── SimpleScreenRenderer.kt   # ★ In-world text rendering
│   │       ├── CaseRenderer.kt           # Power LED on case face
│   │       ├── DroneRenderer.kt
│   │       ├── MicrocontrollerRenderer.kt
│   │       └── RobotRenderer.kt
│   │
│   ├── common/                 # SHARED (client + server)
│   │   ├── CommonSetup.kt
│   │   ├── Config.kt           # Configuration loading
│   │   ├── Tier.kt             # Tier 1–3 enum
│   │   ├── block/              # Block definitions (~20 blocks)
│   │   │   ├── CaseBlock.kt
│   │   │   ├── ScreenBlock.kt
│   │   │   ├── KeyboardBlock.kt
│   │   │   ├── CableBlock.kt
│   │   │   └── ...
│   │   ├── blockentity/        # Block entities (tile entities)
│   │   │   ├── CaseBlockEntity.kt    # ★ Computer host, runs machine
│   │   │   ├── ScreenBlockEntity.kt  # ★ TextBuffer, network sync
│   │   │   └── ...
│   │   ├── init/               # Deferred registries
│   │   │   ├── ModBlocks.kt
│   │   │   ├── ModItems.kt
│   │   │   ├── ModBlockEntities.kt
│   │   │   ├── ModMenus.kt
│   │   │   └── ...
│   │   ├── event/              # Game event handlers
│   │   └── item/               # Item definitions (components, materials, upgrades)
│   │
│   ├── server/                 # SERVER-SIDE ONLY
│   │   ├── machine/
│   │   │   ├── SimpleLuaArchitecture.kt  # ★★ 3700-line Lua VM (GPU, FS, net, etc.)
│   │   │   ├── SimpleMachine.kt          # Machine lifecycle, signal queue
│   │   │   └── InstalledComponents.kt    # Inventory→component mapping
│   │   ├── fs/
│   │   │   ├── VirtualFileSystem.kt      # In-memory VFS for HDD items
│   │   │   └── OpenOSContent.kt          # Loads OpenOS files from resources
│   │   └── entity/
│   │       └── RobotAndDroneEntities.kt  # Stubbed
│   │
│   ├── network/
│   │   └── ModPackets.kt       # ★ All packet definitions & handlers
│   │
│   ├── util/
│   │   ├── TextBuffer.kt       # ★ 2D char/fg/bg grid
│   │   ├── KeyboardKeys.kt     # Key code constants
│   │   └── OCLogger.kt         # Logging utility
│   │
│   ├── datagen/
│   │   └── DataGenerators.kt   # Recipe/model/loot data generators
│   │
│   └── integration/
│       ├── IntegrationManager.kt
│       └── SimpleComponent.kt
│
├── src/main/resources/
│   ├── META-INF/
│   │   └── neoforge.mods.toml  # Mod metadata (id, version, deps)
│   ├── pack.mcmeta             # Resource pack metadata
│   ├── assets/opencomputers/
│   │   ├── blockstates/        # Block state JSON files
│   │   ├── models/             # Block & item models
│   │   ├── textures/           # Textures
│   │   ├── sounds/             # Sound files
│   │   └── lua/                # ★★ Lua programs
│   │       ├── bios.lua        # Standalone BIOS
│   │       └── openos/         # Full OpenOS
│   │           ├── init.lua    # Boot entry
│   │           ├── bin/        # 60+ commands (ls, cat, edit, grep, ...)
│   │           ├── lib/        # 20+ libraries (event, component, term, ...)
│   │           └── etc/        # Config (motd, profile)
│   ├── config/
│   │   └── opencomputers.toml  # Default config
│   └── data/opencomputers/
│       ├── recipes/            # Crafting recipes
│       └── loot_tables/        # Loot tables
│
├── docs/                       # Documentation (you are here)
│   ├── API_REFERENCE.md        # Lua component API reference
│   ├── COMMANDS.md             # OpenOS shell commands
│   ├── GETTING_STARTED.md      # In-game quickstart
│   ├── PROGRAMMING.md          # Lua programming guide
│   └── DEVELOPMENT.md          # ★ This file
│
└── ~80+ .kt.disabled files     # Deferred features (original OC code, not yet ported)
```

### Key Files You'll Touch Most Often

| File | What It Does | Lines |
|------|-------------|-------|
| `SimpleLuaArchitecture.kt` | The entire Lua VM: GPU API, filesystem, internet, signals, BIOS | ~3700 |
| `SimpleScreenRenderer.kt` | Renders TextBuffer text on screen block faces in-world | ~140 |
| `ScreenBlockEntity.kt` | Screen state, TextBuffer, server→client sync packets | ~175 |
| `CaseBlockEntity.kt` | Computer case: inventory, machine lifecycle, screen detection | ~285 |
| `ModPackets.kt` | All network packets (keyboard, screen updates, power, touch) | ~600 |
| `TextBuffer.kt` | 2D character + color grid storage | ~270 |
| `SimpleMachine.kt` | Machine state machine (start/stop/pause), signal queue | ~200 |

---

## Architecture — How It Works

### The Lua VM

The heart of the mod is `SimpleLuaArchitecture.kt`. It creates a **LuaJ** sandbox per computer:

```
LuaJ (org.luaj.vm2) Globals
├── Libraries: Base, Package, Bit32, Table, String, Math, Coroutine, Debug
├── Timeout: 5 seconds max execution, checked every 10,000 instructions
├── APIs exposed to Lua:
│   ├── component.list()        — enumerate hardware
│   ├── component.invoke()      — call component methods
│   ├── computer.pushSignal()   — queue events
│   ├── computer.pullSignal()   — wait for events (yields coroutine)
│   ├── gpu.set/fill/copy/...   — GPU drawing (modifies TextBuffer)
│   ├── os.clock/date/time      — time functions
│   ├── internet.request()      — HTTP GET/POST
│   └── unicode.char/len/sub    — Unicode string ops
└── Built-in BIOS: finds GPU+screen, searches for bootable FS, runs /init.lua
```

### The Component System (Simplified)

Unlike original OpenComputers which had a full node-graph network, this rewrite uses a **simplified direct approach**:

1. `CaseBlockEntity` scans its inventory for items (CPU, RAM, GPU, HDD, etc.)
2. `SimpleMachine` maps these to virtual component addresses (UUIDs)
3. `SimpleLuaArchitecture.registerVirtualComponents()` creates Lua-callable components
4. GPU operations directly modify the `TextBuffer` on the nearby `ScreenBlockEntity`

### Tiers

| Tier | Screen Resolution | Color Depth | RAM |
|------|------------------|-------------|-----|
| 1 | 50×16 | 16 colors | 192 KB |
| 2 | 80×25 | 256 colors | 512 KB |
| 3 | 160×50 | 16.7M colors | 1024 KB |

---

## Data Flow — GPU Write to In-World Screen

This is the full pipeline from a Lua `gpu.set(1, 1, "Hello")` call to pixels on-screen:

```
1. Lua code calls gpu.set(x, y, text)
         │
         ▼
2. SimpleLuaArchitecture.kt
   GPU "set" handler (runs on server thread)
   Calls findNearbyScreen() → gets ScreenBlockEntity
   Writes to screen.buffer.set(x-1, y-1, char, fg)
   Sets screen.markForSync()
         │
         ▼
3. ScreenBlockEntity.serverTick()
   Checks needsSync flag
   Calls createFullSyncPacket()
   Serializes charData + fgData + bgData → ByteBuffer
   Sends ScreenUpdatePacket via PacketDistributor.sendToPlayersTrackingChunk()
   Debug log: "SCREEN TICK: sending packet for (x,y,z), nonSpaceChars=N"
         │
         ▼
4. ModPackets.handleScreenUpdate() [CLIENT SIDE]
   Receives packet, finds ScreenBlockEntity at pos
   Calls ScreenBlockEntity.applyScreenUpdate(data)
   Decodes ByteBuffer → sets buffer.charData/fgData/bgData via setRawData()
   Debug log: "CLIENT applied screen update: nonSpaceChars=N"
         │
         ▼
5. SimpleScreenRenderer.render() [CLIENT SIDE, every frame]
   Gets buffer from ScreenBlockEntity
   Determines block facing (NORTH/SOUTH/EAST/WEST)
   Applies pose transforms (translate + rotate) to position text on correct face
   Iterates all cells: skips char ≤ 32 (space), draws others via Font.drawInBatch()
   Uses Font.DisplayMode.SEE_THROUGH + endBatch() flush
   Debug log (every 200 frames): "RENDERER: buffer WxH, nonSpaceChars=N"
```

### Network Packet Summary

| Direction | Packet | Purpose |
|-----------|--------|---------|
| Server→Client | `ScreenUpdatePacket` | Full TextBuffer sync (chars+fg+bg) |
| Server→Client | `ComputerStatePacket` | Power on/off, energy, component count |
| Client→Server | `ComputerPowerPacket` | Toggle power button |
| Client→Server | `KeyboardInputPacket` | Key press/release + clipboard paste |
| Client→Server | `ScreenTouchPacket` | Touch/drag/drop/scroll at char coords |

---

## Known Bugs & Current Issues

### CRITICAL: Text Renders on Wrong Side of Screen

**Status:** Active bug, not yet fixed.

**Symptom:** Text is visible when looking at the **back** of the screen block, but invisible from the **front**. This is a classic backface culling problem.

**Root Cause:** The pose transforms in `SimpleScreenRenderer.kt` position text with normals facing **into** the block instead of **outward** toward the viewer. The `XP.rotationDegrees(180f)` rotation flips Y (correct — font draws downward) but also flips the Z normal (incorrect — makes text face inward).

**Where:** `SimpleScreenRenderer.kt` lines 66–100, the `when(facing)` block.

**How to fix:** The transforms need to produce text quads with normals pointing **out** of the block face, toward the viewer. Either restructure the rotation chain or add a compensating scale/rotation.

### Debug Logging Is Still Active

Diagnostic `LOGGER.info()` calls are present in three files and will spam the log:
- `ScreenBlockEntity.kt` — logs every sync packet sent
- `ModPackets.kt` — logs every screen update received on client
- `SimpleScreenRenderer.kt` — logs every 200 frames

**These should be removed or changed to `LOGGER.debug()` before release.**

### Screen Detection Is Simplistic

The computer connects to the **first** `ScreenBlockEntity` found in a ±8/±4 scan. If multiple screens exist nearby, only one works. Multi-block screens are not supported.

### No Real Power System

Computers don't consume energy. The power LED works but is cosmetic. Capacitors and Power Converters exist as blocks but don't do anything functional.

---

## Remaining TODOs

These are `TODO` comments found directly in the source code:

| File | TODO | Priority |
|------|------|----------|
| `OpenComputers.kt` | Re-enable `NetworkHandler` and `MachineRegistry` | Medium |
| `CommonSetup.kt` | Re-enable when config system is ready | Low |
| `ClientSetup.kt` | Re-enable additional renderers (hologram, rack, etc.) | Low |
| `PrinterBlock.kt` | Open printer GUI on right-click | Low |
| `ServerRackBlock.kt` | Open server rack GUI on right-click | Low |
| `RackScreen.kt` | Send packet to server (GUI has no backend) | Low |
| `DriveScreen.kt` | Send packet to server (label rename, 2 places) | Low |
| `InputEventHandler.kt` | Send network packets for various input events (6 places) | Medium |
| `ModItems.kt` | Implement DroneItem class | Low |
| `ModItems.kt` | Proper upgrade item implementation | Low |
| `RobotEventHandler.kt` | Implement when robot entity exists | Low |

### Features Not Yet Ported (`.kt.disabled` Files)

There are **~80+ disabled Kotlin files** from the original OC port. These represent deferred features:
- Full node-graph networking (`network/`)
- Original component system with visibility/reachability
- Many block entities (charger, assembler, hologram, etc.)
- Original renderers (hologram, cable, rack, etc.)
- Multi-block screen merging
- Robot/Drone AI and movement 
- Loot disk system (partially ported)
- Forge energy integration

---

## Debugging Guide — READ THIS FIRST

If you're working on this mod and something isn't working, here's the debugging playbook that was developed through painful trial and error.

### 1. Check the Logs

The game log is your best friend. Look at:
- **Latest log:** `run/logs/latest.log` (dev environment) or `.minecraft/logs/latest.log` (production)
- **Search for:** `SCREEN TICK`, `CLIENT handleScreenUpdate`, `RENDERER:`

If you see `SCREEN TICK: nonSpaceChars=N` with N > 0, the server has text data.  
If you see `CLIENT applied screen update: nonSpaceChars=N` with N > 0, the client received it.  
If you see `RENDERER: nonSpaceChars=N` with N > 0, the renderer has the data — it's a rendering problem.

### 2. Trace The Data Flow

If the screen is blank, figure out WHERE data stops:

```
Lua GPU call → TextBuffer on server → Packet sent? → Packet received? → Buffer on client → Renderer sees it?
```

Add `println()` or `LOGGER.info()` at each stage. Don't guess — **verify**.

### 3. Common Rendering Problems

| Symptom | Likely Cause |
|---------|-------------|
| Completely invisible text | Z-fighting: text at same depth as block face. Increase `Z_OFFSET`. |
| Text visible from behind, not front | Backface culling: quad normals face wrong direction. Fix pose transforms. |
| Text flickers | Z-fighting or batch ordering. Use `SEE_THROUGH` display mode + `endBatch()`. |
| Text in wrong position | Transform math is wrong. Check translate/rotate per facing direction. |
| Text on wrong face | `FACING` property not matching expected direction. Check blockstate. |

### 4. Common Data Flow Problems

| Symptom | Likely Cause |
|---------|-------------|
| Server has data, client doesn't | Packet not being sent. Check `needsSync` flag and `serverTick()`. |
| Client receives packet, buffer empty | `applyScreenUpdate()` is decoding wrong. Check ByteBuffer order. |
| GPU calls do nothing | `findNearbyScreen()` returns null. No screen connected. Check scan range. |
| Multiple screens, wrong one lights up | Screen detection picks FIRST found. Scan order: X→Y→Z ascending. |
| Computer won't boot | Missing CPU or RAM in case inventory. Check `scanInventory()` validation. |

### 5. Useful Debug Techniques

**Add temporary logging in SimpleLuaArchitecture:**
```kotlin
// In the GPU "set" handler:
OpenComputers.LOGGER.info("GPU SET: x=$x, y=$y, text=$text, screen=${cachedScreen?.blockPos}")
```

**Check what components are registered:**
```kotlin
// In SimpleLuaArchitecture.registerVirtualComponents():
OpenComputers.LOGGER.info("Registered component: $type @ $address")
```

**Verify screen connection:**
```kotlin
// In CaseBlockEntity.connectNearbyScreens():
OpenComputers.LOGGER.info("Connected screen at ${screen.blockPos}, address=${screen.address}")
```

**Dump TextBuffer contents:**
```kotlin
// Anywhere you have access to a TextBuffer:
val lines = buffer.getLines()
lines.forEachIndexed { i, line -> 
    if (line.isNotBlank()) OpenComputers.LOGGER.info("Buffer line $i: '$line'")
}
```

### 6. Build & Test Cycle

```powershell
# Build the JAR
.\gradlew.bat jar

# Copy to mods folder
copy build\libs\opencomputers-3.0.0.jar $env:APPDATA\.minecraft\mods\ -Force

# Launch Minecraft, check logs at:
# %APPDATA%\.minecraft\logs\latest.log

# OR just use the dev client:
.\gradlew.bat runClient
```

**Pro tip:** Use `runClient` for faster iteration — you don't need to copy JARs.

### 7. Understanding Minecraft Rendering

Key concepts for screen rendering:

- **PoseStack:** A transformation stack (like OpenGL matrix stack). `pushPose()` / `popPose()` to save/restore.
- **Block entity origin:** (0,0,0) = bottom-north-west corner of the block. (1,1,1) = top-south-east corner.
- **Font.drawInBatch():** Draws text in the XY plane. +X = right, +Y = down. Quad faces toward +Z.
- **Facing directions:**
  - NORTH face: at z=0, viewer at z < 0 looking toward +Z
  - SOUTH face: at z=1, viewer at z > 1 looking toward -Z
  - EAST face: at x=1, viewer at x > 1 looking toward -X
  - WEST face: at x=0, viewer at x < 0 looking toward +X
- **SEE_THROUGH display mode:** Ignores depth buffer → text always visible over block faces. Needs `endBatch()` to force correct draw order.
- **Backface culling:** If a quad's normal points away from the camera, it's invisible. This is why text direction matters.

---

## What Went Wrong & Lessons Learned

This section documents bugs that were found and fixed (or partially fixed) during development, so future contributors don't repeat the same mistakes.

### Bug 1: Wrong Screen Connected

**Problem:** GPU wrote to the first screen found, but `connectNearbyScreens()` marked ALL nearby screens as connected. The first screen (by scan order) got GPU writes, but a different screen might have been "the" connected one from the case's perspective.

**Fix:** Changed `connectNearbyScreens()` to only connect the **primary** screen (same one `findAndCacheNearbyScreen()` would return) and explicitly disconnect all others.

**Lesson:** When two systems independently scan for blocks (GPU finding a screen + Case connecting to screens), they MUST use the **same scan order and selection logic** or they'll pick different blocks.

### Bug 2: Flickering HDD LED

**Problem:** The `CaseRenderer` had a "hard drive activity" LED that blinked using `((gameTime * 7) % 13) < 6`. This created a rapid, distracting strobe effect.

**Fix:** Removed the HDD activity LED entirely. Only the power LED remains.

**Lesson:** Don't simulate hardware activity LEDs with pseudorandom patterns — they look terrible in-game.

### Bug 3: Text Invisible (Depth / Draw Order)

**Problem:** Text was being drawn at the same depth as the block face, causing Z-fighting. Additionally, Minecraft's batch rendering system could draw the block face AFTER the text, completely overwriting it.

**Fix:** Three changes combined:
1. Increased `Z_OFFSET` from 0.005f to 0.02f (more depth separation)
2. Switched from `Font.DisplayMode.NORMAL` to `SEE_THROUGH` (ignores depth test)
3. Added `endBatch()` call after text rendering to force immediate flush

**Lesson:** In Minecraft's rendering pipeline, you can't rely on draw call order. `SEE_THROUGH` + `endBatch()` is the reliable pattern for overlaid text.

### Bug 4: Text Faces Inward (CURRENT)

**Problem:** Text is visible from behind the screen but not from the front. The `XP.rotationDegrees(180f)` rotation flips both Y (desired for font) and Z-normal (causes backface culling from front).

**Status:** Not yet fixed. The pose transforms in `SimpleScreenRenderer.kt` need rework.

**Lesson:** When applying rotations for text rendering, remember that rotating around X by 180° flips BOTH Y and Z. If you only want to flip Y (so text goes downward), you need a different approach — either use `scale(1, -1, 1)` or counteract the Z flip with another transform.

---

## Contributing

### Code Style

- Kotlin with JVM target 21
- `freeCompilerArgs = -Xjvm-default=all` (interface default methods)
- No specific formatter enforced — just be consistent with surrounding code
- Keep `SimpleLuaArchitecture.kt` organized by API section (GPU, FS, Internet, etc.)

### Before Submitting a PR

1. Build successfully: `gradlew.bat build`
2. Test in-game: boot a computer, run `edit test.lua`, verify screen works
3. Check for log spam: remove or downgrade any debug logging you added
4. Update this documentation if you changed architecture or fixed a bug

### Git

```bash
git remote -v
# origin: https://github.com/MidasRX/OpenCoudou.git

git checkout main
git pull
# Make changes
git add -A
git commit -m "description of change"
git push origin main
```

---

*Last updated: April 2026 — OpenCoudou v3.0.0*
