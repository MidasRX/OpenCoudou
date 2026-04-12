# OpenComputers Component API Reference

Complete reference for all component APIs available in OpenComputers.

## Component System

### Accessing Components

```lua
local component = require("component")

-- Get a specific component by type
local gpu = component.gpu
local screen = component.screen

-- Get component by address
local c = component.proxy("abc123...")

-- List all components
for address, type in component.list() do
  print(type, address)
end

-- Check if component is available
if component.isAvailable("internet") then
  print("Internet card installed!")
end
```

## GPU API

The GPU (Graphics Processing Unit) handles all screen rendering.

### Basic Methods

```lua
local gpu = component.gpu

-- Bind to a screen
gpu.bind(screenAddress)

-- Get resolution
local w, h = gpu.getResolution()

-- Set resolution
gpu.setResolution(80, 25)

-- Get max resolution
local maxW, maxH = gpu.maxResolution()

-- Get color depth
local depth = gpu.getDepth()

-- Set color depth
gpu.setDepth(8) -- 1, 4, or 8 bits
```

### Colors

```lua
-- Set foreground (text) color
gpu.setForeground(0xFFFFFF) -- White

-- Set background color
gpu.setBackground(0x000000) -- Black

-- Get current colors
local fg = gpu.getForeground()
local bg = gpu.getBackground()

-- Set palette colors (tier 3+)
gpu.setPaletteColor(0, 0xFF0000) -- Set palette 0 to red
local color = gpu.getPaletteColor(0)
```

### Drawing

```lua
-- Set a single character
gpu.set(x, y, "A")

-- Set a string
gpu.set(10, 5, "Hello World")

-- Fill an area
gpu.fill(1, 1, 80, 25, " ") -- Clear screen

-- Copy an area
gpu.copy(1, 1, 40, 12, 40, 0) -- Copy left half to right

-- Get a character
local char, fg, bg = gpu.get(10, 5)
```

## Screen API

```lua
local screen = component.screen

-- Turn screen on/off
screen.turnOn()
screen.turnOff()
local isOn = screen.isOn()

-- Get screen aspect ratio
local w, h = screen.getAspectRatio()

-- Get keyboard addresses
for _, kbd in pairs(screen.getKeyboards()) do
  print("Keyboard:", kbd)
end

-- Set touch mode (for touch screens)
-- true = inverts touch coordinates
screen.setTouchModeInverted(false)
```

## Redstone Card API

```lua
local rs = component.redstone

-- Get input on a side
local input = rs.getInput(sides.front)

-- Set output on a side
rs.setOutput(sides.back, 15)

-- Get bundled input (color)
local colors = rs.getBundledInput(sides.left)
local red = colors[colors.red]

-- Set bundled output
rs.setBundledOutput(sides.right, {
  [colors.red] = 15,
  [colors.blue] = 8
})

-- Get comparator input
local comp = rs.getComparatorInput(sides.down)

-- Wake threshold (wireless wakeup)
rs.setWakeThreshold(1)
local threshold = rs.getWakeThreshold()
```

## Filesystem API

```lua
local fs = require("filesystem")

-- Check if path exists
if fs.exists("/home/file.txt") then
  print("File exists")
end

-- Check if path is directory
local isDir = fs.isDirectory("/home")

-- List directory contents
for file in fs.list("/home") do
  print(file)
end

-- Make directory
fs.makeDirectory("/home/newdir")

-- Remove file/directory
fs.remove("/home/oldfile.txt")

-- Copy file
fs.copy("/home/src.txt", "/home/dst.txt")

-- Rename/move file
fs.rename("/old.txt", "/new.txt")

-- Get file size
local size = fs.size("/home/file.txt")

-- Get free space
local freeSpace = fs.spaceTotal("/") - fs.spaceUsed("/")
```

### File Operations

```lua
-- Open file for reading
local file = io.open("/home/data.txt", "r")
local contents = file:read("*a") -- Read all
file:close()

-- Open file for writing
local file = io.open("/home/output.txt", "w")
file:write("Hello World\n")
file:close()

-- Append to file
local file = io.open("/home/log.txt", "a")
file:write("New log entry\n")
file:close()

-- Read line by line
for line in io.lines("/home/data.txt") do
  print(line)
end
```

## Internet Card API

```lua
local internet = component.internet

-- HTTP GET request
local handle = internet.request("https://api.example.com/data")
local response = handle.read()
handle.close()

-- HTTP POST request
local handle = internet.request("https://api.example.com/post", "data=value")
local response = handle.read()
handle.close()

-- Check if URL is allowed
local allowed = internet.isHttpEnabled()

-- TCP connection
local socket = internet.connect("example.com", 80)
if socket then
  socket.finishConnect()
  socket.write("GET / HTTP/1.1\r\nHost: example.com\r\n\r\n")
  local response = socket.read()
  socket.close()
end
```

## Robot API

```lua
local robot = require("robot")

-- Movement
robot.forward()
robot.back()
robot.up()
robot.down()
robot.turnLeft()
robot.turnRight()
robot.turnAround()

-- Interaction
robot.swing()        -- Attack/dig forward
robot.swingUp()
robot.swingDown()
robot.use()          -- Use item forward
robot.useUp()
robot.useDown()

-- Block detection
local hasBlock = robot.detect()
local hasBlockUp = robot.detectUp()
local hasBlockDown = robot.detectDown()

-- Inventory
robot.select(5)              -- Select slot 5
local count = robot.count()  -- Items in current slot
local space = robot.space()  -- Free space in current slot
robot.place()                -- Place block from current slot
robot.drop()                 -- Drop items forward
robot.suck()                 -- Pick up items forward

-- Tank (fluid)
robot.drain()                -- Drain fluid forward
robot.fill()                 -- Fill fluid forward
local mb = robot.tankLevel() -- Current tank level
```

## Drone API

```lua
local drone = component.drone

-- Movement
drone.move(dx, dy, dz)  -- Move relative
local x, y, z = drone.getOffset()  -- Get current offset

-- Velocity limits
drone.setMaxVelocity(0.5)

-- Actions
drone.swing()
drone.use()

-- Status
local name = drone.name()
drone.setLightColor(0xFF0000)  -- Set LED color
local color = drone.getLightColor()

-- Inventory
drone.select(1)
drone.count()
drone.drop(count)
drone.suck(count)
```

## Data Card API

```lua
local data = component.data

-- Hashing
local md5 = data.md5("Hello")
local sha256 = data.sha256("Hello")

-- Base64
local encoded = data.encode64("Hello")
local decoded = data.decode64(encoded)

-- Random data
local bytes = data.random(32)

-- Deflate/Inflate (compression)
local compressed = data.deflate("Long text data...")
local original = data.inflate(compressed)

-- AES encryption (tier 2+)
local key = data.random(16)
local iv = data.random(16)
local encrypted = data.encrypt(data, key, iv)
local decrypted = data.decrypt(encrypted, key, iv)

-- RSA (tier 3)
local pubKey, privKey = data.generateKeyPair(2048)
local encrypted = data.encrypt(data, pubKey)
local decrypted = data.decrypt(encrypted, privKey)
```

## Computer API

```lua
local computer = require("computer")

-- System info
local totalMem = computer.totalMemory()
local freeMem = computer.freeMemory()
local energy = computer.energy()
local maxEnergy = computer.maxEnergy()
local uptime = computer.uptime()
local realTime = computer.realTime()

-- Computer address
local address = computer.address()

-- Shutdown/Reboot
computer.shutdown()
computer.shutdown(true)  -- Reboot

-- Beep
computer.beep(440, 0.5)  -- 440Hz for 0.5 seconds

-- Push signal
computer.pushSignal("custom_event", arg1, arg2)

-- Users
local users = computer.users()
computer.addUser("player")
computer.removeUser("player")
```

## Keyboard Constants

```lua
local keyboard = require("keyboard")

-- Check if key is pressed
if keyboard.isKeyDown(keyboard.keys.w) then
  robot.forward()
end

-- Common keys
keyboard.keys.enter
keyboard.keys.space
keyboard.keys.back
keyboard.keys.tab
keyboard.keys.lshift
keyboard.keys.lcontrol
keyboard.keys.lmenu  -- Alt
keyboard.keys.a through keyboard.keys.z
keyboard.keys.numpad0 through keyboard.keys.numpad9
keyboard.keys.up, .down, .left, .right

-- Check modifiers
if keyboard.isShiftDown() then
  print("Shift pressed")
end
```

## Sides Library

```lua
local sides = require("sides")

-- Side constants
sides.bottom  -- 0
sides.top     -- 1
sides.back    -- 2
sides.front   -- 3
sides.right   -- 4
sides.left    -- 5

-- Also available as:
sides.down    -- 0
sides.up      -- 1
sides.north   -- 2
sides.south   -- 3
sides.west    -- 4
sides.east    -- 5

-- Conversion
local name = sides.names[0]  -- "bottom"
```

## Colors Library

```lua
local colors = require("colors")

-- Color constants
colors.white   -- 0
colors.orange  -- 1
colors.magenta -- 2
colors.lightblue -- 3
colors.yellow  -- 4
colors.lime    -- 5
colors.pink    -- 6
colors.gray    -- 7
colors.silver  -- 8
colors.cyan    -- 9
colors.purple  -- 10
colors.blue    -- 11
colors.brown   -- 12
colors.green   -- 13
colors.red     -- 14
colors.black   -- 15
```

## Serialization

```lua
local serialization = require("serialization")

-- Serialize table to string
local data = {name = "Test", values = {1, 2, 3}}
local str = serialization.serialize(data)

-- Deserialize string to table
local restored = serialization.unserialize(str)
print(restored.name)  -- "Test"
```

## Text Utilities

```lua
local text = require("text")

-- Trim whitespace
local trimmed = text.trim("  hello  ")

-- Word wrap
local lines = text.wrap("Long text...", 40)

-- Tokenize
local tokens = text.tokenize("hello world")

-- Pad strings
local padded = text.padLeft("5", 3, "0")  -- "005"
local padded = text.padRight("x", 5)      -- "x    "
```
