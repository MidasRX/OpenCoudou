# OpenComputers Rewrite

A complete rewrite of the [OpenComputers](https://github.com/MightyPirates/OpenComputers) mod for Minecraft 1.21.4 using NeoForge and Kotlin.

## 📖 About

OpenComputers is a Minecraft mod that adds programmable computers and robots to the game. This is a ground-up rewrite for modern Minecraft versions, maintaining the core functionality while leveraging modern modding practices.

### Key Features

- **Programmable Computers** - Full Lua 5.4 environment via LuaJ
- **Computer Components** - CPUs, RAM, HDDs, GPUs, and more
- **Networking** - Cables, switches, access points for inter-computer communication
- **Robots** - Mobile, programmable entities that can interact with the world
- **Drones** - Flying programmable devices
- **3D Printing** - Create custom blocks with the Chamelium material
- **OpenOS** - Full Unix-like operating system included with 69+ Lua programs

## 🚀 Quick Start

### Requirements

- Java 21 or higher
- Minecraft 1.21.4
- NeoForge (compatible version)

### Installation

1. Download the latest `.jar` from [Releases](https://github.com/TomGousseau/OpenCoudou/releases)
2. Place it in your Minecraft `mods/` folder
3. Launch Minecraft with NeoForge

### Building from Source

```bash
git clone https://github.com/TomGousseau/OpenCoudou.git
cd OpenCoudou
./gradlew build
```

The compiled mod will be in `build/libs/`.

## 🎮 Getting Started In-Game

### Your First Computer

1. **Craft a Computer Case** (Tier 1)
   - Use Iron, Redstone, and Circuit Boards
   
2. **Add Components:**
   - CPU (Tier 1+)
   - Memory/RAM (at least Tier 1)
   - Hard Disk Drive or EEPROM
   - Graphics Card
   - Screen and Keyboard

3. **Power It:**
   - Connect to a power source (Capacitor or Power Converter)
   - OpenComputers uses Energy (compatible with most power systems)

4. **Boot Up:**
   - Right-click the computer case
   - Install OpenOS from the included floppy disk
   - Use `install` command in the shell

### Basic Commands

Once OpenOS is installed, you can use standard Unix-like commands:

```bash
ls          # List files
cd <dir>    # Change directory
edit <file> # Edit a file
lua         # Start Lua interpreter
help        # Get help
man <cmd>   # Manual pages
```

## 🏗️ Project Structure

```
OpenComputersRewrite/
├── src/main/
│   ├── kotlin/li/cil/oc/          # Mod source code
│   │   ├── common/
│   │   │   ├── block/             # Block definitions
│   │   │   ├── blockentity/       # Block entities (tile entities)
│   │   │   ├── item/              # Items and components
│   │   │   └── init/              # Registry initialization
│   │   ├── datagen/               # Data generation (recipes, models, etc.)
│   │   └── OpenComputers.kt       # Main mod class
│   └── resources/
│       ├── assets/opencomputers/  # Client-side assets
│       │   ├── blockstates/       # Block state definitions
│       │   ├── models/            # 3D models (block & item)
│       │   ├── textures/          # Textures
│       │   └── lua/               # Lua operating system & programs
│       └── data/opencomputers/    # Server-side data
│           ├── loot_tables/       # Loot table definitions
│           └── recipes/           # Crafting recipes
└── build.gradle.kts               # Build configuration
```

## 🔧 Development

### Setting Up the Dev Environment

1. **Clone the repository:**
   ```bash
   git clone https://github.com/TomGousseau/OpenCoudou.git
   cd OpenCoudou
   ```

2. **Generate IDE files:**
   ```bash
   # For IntelliJ IDEA
   ./gradlew idea
   
   # For Eclipse
   ./gradlew eclipse
   ```

3. **Run in development:**
   ```bash
   ./gradlew runClient  # Launch game client
   ./gradlew runServer  # Launch dedicated server
   ```

### Code Structure

**Kotlin Packages:**
- `li.cil.oc.common.block.*` - All block types (machines, network devices)
- `li.cil.oc.common.blockentity.*` - Block entity logic
- `li.cil.oc.common.item.*` - Items and components
- `li.cil.oc.common.init.*` - Mod registry (blocks, items, block entities)

**Key Files:**
- `OpenComputers.kt` - Main mod entrypoint
- `ModBlocks.kt` - Block registration
- `ModItems.kt` - Item registration
- `ModBlockEntities.kt` - Block entity registration

### Adding New Components

1. **Create the item class** in `common/item/`
2. **Register it** in `ModItems.kt`
3. **Add textures** in `resources/assets/opencomputers/textures/item/`
4. **Create model JSON** in `resources/assets/opencomputers/models/item/`
5. **Add recipe** in `resources/data/opencomputers/recipes/`

## 📦 Components & Blocks

### Computer Components
- **CPUs** - 3 tiers (speed & component support)
- **Memory (RAM)** - 6 tiers (192KB to 2MB)
- **Graphics Cards** - 3 tiers (resolution & color depth)
- **Hard Drives** - 3 tiers (1MB to 4MB)
- **Network Cards** - Wired and Wireless
- **Redstone Cards** - Advanced redstone I/O

### Machines
- **Computer Case** - 3 tiers + Creative
- **Server** - Rack-mountable computers (3 tiers + Creative)
- **Rack** - Houses up to 4 servers
- **Assembler** - Builds robots and drones
- **Disassembler** - Breaks down components
- **3D Printer** - Prints custom blocks

### Network Devices
- **Cable** - Basic network connectivity
- **Switch** - Network packet routing with bandwidth limits
- **Access Point** - Wireless network bridge
- **Relay** - Network packet relay

### Peripherals
- **Screen** - 3 tiers (monochrome to full color)
- **Keyboard** - Input device
- **Disk Drive** - Read/write floppies
- **RAID** - Multi-disk array
- **Hologram** - 2 tiers (48³ to 96³ voxels)

## 🤖 Programming

### Lua Environment

OpenComputers uses **LuaJ** for Lua 5.4 compatibility. All computers run in sandboxed environments with resource limits.

### Example: Hello World

```lua
local component = require("component")
local gpu = component.gpu

gpu.set(1, 1, "Hello, World!")
```

### Example: Redstone Control

```lua
local component = require("component")
local rs = component.redstone

-- Set redstone output on south side
rs.setOutput(3, 15)  -- sides.south, strength 15
```

### Example: Network Communication

```lua
local component = require("component")
local modem = component.modem

modem.open(123)  -- Open port 123
modem.broadcast(123, "Hello network!")
```

### Available APIs

- `computer` - Computer information and control
- `component` - Component access
- `filesystem` - File I/O operations
- `os` - Operating system functions
- `event` - Event handling
- `thread` - Multitasking support
- `serialization` - Data serialization

## 📚 OpenOS Programs

The mod includes **69+ Lua programs** providing a full Unix-like shell:

**File Management:** `ls`, `cd`, `cp`, `mv`, `rm`, `mkdir`, `touch`, `cat`, `more`  
**Text Processing:** `grep`, `sed`, `awk`, `cut`, `sort`, `uniq`, `wc`, `head`, `tail`  
**System Tools:** `df`, `du`, `ps`, `kill`, `sleep`, `shutdown`, `reboot`  
**Networking:** `wget`, `ping`, `ifconfig`  
**Development:** `edit`, `lua`, `sh`  
**Utilities:** `find`, `which`, `man`, `help`, `tree`

Full command list: [docs/COMMANDS.md](docs/COMMANDS.md)

## 🔌 Power System

OpenComputers requires energy to operate. The mod is compatible with most power systems through the **Power Converter**.

**Energy Storage:**
- Capacitor - Basic energy buffer
- Upgraded capacitors available

**Power Sources:**
- Power Converter - Converts RF/FE to OC energy
- Power Distributor - Distributes power efficiently
- Solar Generator Upgrade - Renewable robot power

## 🛠️ Crafting Progression

1. **Raw Materials:**
   - Transistor (base component)
   - Circuit Board → Printed Circuit Board
   - Microchips (Tier 1-3)

2. **Basic Components:**
   - ALU + Control Unit → CPU
   - Memory modules
   - Disk Platter → Hard Drive

3. **Advanced Components:**
   - Graphics Cards
   - Network Cards
   - Expansion Cards

4. **Machines:**
   - Computer Case
   - Peripherals (Screen, Keyboard)
   - Specialization (Robots, Servers)

## 📝 License

This project maintains compatibility with the original OpenComputers licensing:

- **Code:** MIT License
- **Assets:** Assets are derived from the original OpenComputers mod
- **LuaJ:** Uses LuaJ library (MIT License)

See [LICENSE](LICENSE) for full details.

## 🙏 Credits

- **Original OpenComputers:** [Sangar/MightyPirates](https://github.com/MightyPirates/OpenComputers)
- **LuaJ:** Lua implementation for Java
- **NeoForge:** Modern Minecraft modding framework
- **Contributors:** All contributors to this rewrite

## 🐛 Reporting Issues

Found a bug? [Open an issue](https://github.com/TomGousseau/OpenCoudou/issues) with:
- Minecraft version
- NeoForge version
- Mod version
- Steps to reproduce
- Crash logs (if applicable)

## 🤝 Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📞 Support & Community

- **Discord:** [Join our server](#) *(coming soon)*
- **Wiki:** [Documentation](https://github.com/TomGousseau/OpenCoudou/wiki) *(coming soon)*
- **Issues:** [GitHub Issues](https://github.com/TomGousseau/OpenCoudou/issues)

## 🗺️ Roadmap

- [x] Core computer functionality
- [x] Component system
- [x] OpenOS implementation
- [x] Network system
- [x] Robot support
- [ ] Drone implementation
- [ ] Tablet functionality
- [ ] Microcontroller support
- [ ] 3D Printer functionality
- [ ] Creative tools (Debug card, etc.)
- [ ] Internet card support
- [ ] Full API documentation

---

**Made with ❤️ for the Minecraft modding community**
