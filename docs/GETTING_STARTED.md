# Getting Started with OpenComputers

This guide will walk you through building your first computer and getting started with programming.

## Prerequisites

Before you begin, make sure you have:
- OpenComputers mod installed
- A source of power (any RF/FE compatible power source)
- Basic crafting materials (iron, redstone, gold)

## Step 1: Gather Basic Materials

### Essential Crafting Components

1. **Transistor** - The fundamental building block
   ```
   Recipe: 3 Iron Nuggets + 3 Redstone + 3 Sticks
   ```

2. **Microchip (Tier 1)**
   ```
   Recipe: 4 Iron Nuggets + 4 Redstone + 1 Redstone Torch
   ```

3. **Circuit Board**
   ```
   Recipe: Raw Circuit Board + Acid + Cutting Wire
   - Raw Circuit Board: Clay + Cactus Green
   - Acid: Gunpowder + Bone Meal + Slime Ball
   - Cutting Wire: 5 Sticks
   ```

4. **Printed Circuit Board** (Advanced)
   ```
   Recipe: Circuit Board + Gold Nuggets + Transistors
   ```

## Step 2: Build Your First Computer

### Required Components

1. **Computer Case (Tier 1)**
   - Provides slots for components
   - Tier 1 has 2 component slots

2. **CPU (Tier 1)**
   ```
   Recipe Pattern:
   Chip - Transistor - Nugget
   Chip - ALU - Chip
   Nugget - Transistor - Chip
   
   Where:
   - Chip = Microchip (Tier 1)
   - ALU = Arithmetic Logic Unit
   - Nugget = Iron Nugget
   ```

3. **Memory/RAM (Tier 1)** - 192 KB
   ```
   Recipe: Microchips + Transistors + Circuit Board
   ```

4. **Graphics Card (Tier 1)**
   - Max resolution: 50x16
   - 16 colors

5. **Hard Disk Drive (Tier 1)** - 1 MB storage
   ```
   Recipe: Disk Platter + Microchips + Circuit Board + Transistors
   ```

6. **EEPROM** (Alternative to HDD for basic systems)
   - Pre-programmed with Lua BIOS
   - Required for booting

### Peripherals

7. **Screen (Tier 1)** - Monochrome display
8. **Keyboard** - For input

## Step 3: Assembly

### Physical Setup

1. **Place the Computer Case** in the world
2. **Right-click the case** to open its GUI
3. **Install components** by dragging them into slots:
   - CPU (required)
   - Memory (at least one required)
   - Graphics Card (required for display)
   - HDD or EEPROM (required for boot)

4. **Place a Screen** adjacent to the case
5. **Place a Keyboard** adjacent to the screen

### Power Setup

6. **Place a Capacitor** next to the computer
7. **Connect power** to the capacitor (any RF/FE source)
8. **Wait for charging** - LEDs will indicate power status

## Step 4: First Boot

### Installing OpenOS

1. **Craft a Floppy Disk**
2. **Insert into Disk Drive** (or directly into computer if case has slots)
3. **Power on** the computer (right-click or redstone signal)
4. **Wait for boot** - You'll see:
   ```
   OpenOS 1.7.5
   Press any key to continue...
   ```

5. **Press Enter** - You'll see a shell prompt:
   ```
   / # _
   ```

6. **Install OpenOS to HDD:**
   ```bash
   install
   ```
   - Follow prompts to install to the internal HDD
   - Remove floppy disk
   - Reboot: `reboot`

## Step 5: Your First Program

### Hello World in Lua

1. **Create a new file:**
   ```bash
   edit hello.lua
   ```

2. **Write your program:**
   ```lua
   local component = require("component")
   local gpu = component.gpu
   
   gpu.set(1, 1, "Hello, World!")
   print("Hello from OpenComputers!")
   ```

3. **Save and exit:** `Ctrl+S`, then `Ctrl+W`

4. **Run your program:**
   ```bash
   hello
   ```

### Understanding the Code

- `component.gpu` - Access the graphics card
- `gpu.set(x, y, text)` - Draw text at coordinates
- `print()` - Standard Lua print function

## Step 6: Basic Shell Commands

### File Management

```bash
ls              # List files in current directory
ls /home        # List files in /home
cd /home        # Change to /home directory
pwd             # Show current directory
mkdir mydir     # Create directory
touch test.txt  # Create empty file
rm test.txt     # Delete file
cp file1 file2  # Copy file
mv file1 file2  # Move/rename file
```

### Text Viewing

```bash
cat file.txt    # Show entire file
more file.txt   # Page through file
head file.txt   # Show first 10 lines
tail file.txt   # Show last 10 lines
```

### System Information

```bash
df              # Disk space usage
free            # Memory usage
components      # List connected components
```

## Step 7: Component Access

### Listing Components

```bash
components
```

Output example:
```
gpu             gpu_component_address
screen          screen_component_address
filesystem      filesystem_component_address
redstone        redstone_component_address
```

### Using Components in Lua

```lua
local component = require("component")

-- Access specific component types
local gpu = component.gpu
local rs = component.redstone

-- Get component address
print(gpu.address)

-- List all components
for address, type in component.list() do
  print(type .. ": " .. address)
end
```

## Step 8: Redstone Control

### Basic Redstone Output

Create `redstone_test.lua`:

```lua
local component = require("component")
local sides = require("sides")
local rs = component.redstone

-- Turn on redstone on all sides
for i = 0, 5 do
  rs.setOutput(i, 15)
end

-- Wait 2 seconds
os.sleep(2)

-- Turn off redstone
for i = 0, 5 do
  rs.setOutput(i, 0)
end
```

### Reading Redstone Input

```lua
local component = require("component")
local sides = require("sides")
local rs = component.redstone

-- Read input from south side
local strength = rs.getInput(sides.south)
print("Redstone strength: " .. strength)
```

## Step 9: Networking

### Basic Network Setup

1. **Craft Network Cards** for each computer
2. **Install in component slots**
3. **Connect with Cables** or use **Wireless Cards**

### Sending Messages

**Computer 1 (Sender):**
```lua
local component = require("component")
local modem = component.modem

modem.open(123)  -- Open port
modem.broadcast(123, "Hello from Computer 1!")
```

**Computer 2 (Receiver):**
```lua
local component = require("component")
local event = require("event")
local modem = component.modem

modem.open(123)  -- Open same port

print("Listening for messages...")
while true do
  local _, _, from, port, _, message = event.pull("modem_message")
  print("Received: " .. message .. " from " .. from)
end
```

## Step 10: Saving & Loading Data

### Writing to Files

```lua
local filesystem = require("filesystem")

-- Write to file
local file = io.open("/home/data.txt", "w")
file:write("Hello from Lua!\n")
file:write("Line 2\n")
file:close()

-- Read from file
local file = io.open("/home/data.txt", "r")
local content = file:read("*a")
file:close()
print(content)
```

### Serialization

```lua
local serialization = require("serialization")

-- Serialize a table
local data = {
  name = "MyComputer",
  x = 100,
  y = 64,
  z = 200,
  inventory = {"iron", "gold", "diamond"}
}

local file = io.open("/home/config.txt", "w")
file:write(serialization.serialize(data))
file:close()

-- Deserialize
local file = io.open("/home/config.txt", "r")
local content = file:read("*a")
file:close()
local loaded = serialization.unserialize(content)
print(loaded.name)  -- "MyComputer"
```

## Common Issues & Solutions

### Computer Won't Boot

**Problem:** Black screen, no response
**Solutions:**
- Check power connection
- Verify all required components are installed (CPU, RAM, GPU, HDD/EEPROM)
- Ensure screen is adjacent to case
- Try rebooting: Break and replace the case

### "Not enough memory" Error

**Problem:** Programs crash with memory error
**Solutions:**
- Install more RAM modules
- Upgrade to higher tier RAM
- Optimize your code (close unused handles, clear unused variables)

### Components Not Detected

**Problem:** `component.gpu` returns nil
**Solutions:**
- Verify component is installed in case
- Check component slot limits (Tier 1 case = 2 slots)
- Try: `component.list()` to see all components

### Slow Performance

**Problem:** Computer runs slowly
**Solutions:**
- Upgrade CPU to higher tier
- Reduce `computer.pullSignal()` timeout
- Optimize loops and algorithms
- Add more RAM for better buffering

## Next Steps

Now that you have a working computer:

1. **Explore the filesystem:**
   - `/bin/` - System programs
   - `/home/` - User files
   - `/lib/` - Lua libraries
   - `/usr/` - User programs

2. **Learn Lua programming:**
   - See [PROGRAMMING.md](PROGRAMMING.md)
   - Check out example programs in `/home/examples/`

3. **Build advanced systems:**
   - Multi-computer networks
   - Automated farms
   - Security systems
   - Data centers with servers and racks

4. **Try robots:**
   - See [ROBOTS.md](ROBOTS.md)
   - Build and program mobile robots

Happy computing! 🖥️
