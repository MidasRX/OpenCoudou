# OpenComputers Lua Programming Guide

Complete guide to programming computers, robots, and drones in OpenComputers using Lua.

## Table of Contents

1. [Lua Basics](#lua-basics)
2. [Component API](#component-api)
3. [Event System](#event-system)
4. [Filesystem API](#filesystem-api)
5. [Graphics Programming](#graphics-programming)
6. [Redstone Control](#redstone-control)
7. [Networking](#networking)
8. [Robot Programming](#robot-programming)
9. [Advanced Topics](#advanced-topics)

## Lua Basics

### Lua Environment

OpenComputers uses **LuaJ** which implements Lua 5.4. Each computer runs in a sandboxed environment with:
- Limited memory (based on RAM tier)
- CPU time limits (based on CPU tier)
- Restricted file system access
- Component access controls

### Hello World

```lua
print("Hello, World!")
```

### Variables and Types

```lua
-- Numbers
local x = 10
local y = 3.14

-- Strings
local name = "Computer"
local message = 'Hello'

-- Booleans
local running = true
local stopped = false

-- Tables (arrays and dictionaries)
local array = {1, 2, 3, 4, 5}
local dict = {name = "PC1", x = 10, y = 20}

-- Nil (no value)
local empty = nil
```

### Functions

```lua
-- Function definition
function greet(name)
  return "Hello, " .. name
end

-- Local function
local function add(a, b)
  return a + b
end

-- Anonymous function
local square = function(x)
  return x * x
end

-- Calling functions
print(greet("World"))
local result = add(5, 3)
```

### Control Flow

```lua
-- If/else
if x > 10 then
  print("Large")
elseif x > 5 then
  print("Medium")
else
  print("Small")
end

-- While loop
local i = 1
while i <= 10 do
  print(i)
  i = i + 1
end

-- For loop (numeric)
for i = 1, 10 do
  print(i)
end

-- For loop (step)
for i = 0, 100, 10 do
  print(i)  -- 0, 10, 20, ...
end

-- For loop (tables)
for index, value in ipairs(array) do
  print(index, value)
end

-- For loop (dictionary)
for key, value in pairs(dict) do
  print(key, value)
end
```

## Component API

### Accessing Components

```lua
local component = require("component")

-- Get primary component of type
local gpu = component.gpu
local screen = component.screen
local redstone = component.redstone

-- Get component by address
local gpu = component.proxy("gpu-address-here")

-- Check if component type exists
if component.isAvailable("redstone") then
  print("Redstone card found")
end

-- List all components
for address, type in component.list() do
  print(type .. ": " .. address)
end

-- List specific type
for address in component.list("gpu") do
  print("GPU: " .. address)
end
```

### Component Methods

```lua
-- Get component methods
local methods = component.methods("gpu")
for name, direct in pairs(methods) do
  print(name)
end

-- Get component address
print(gpu.address)

-- Get component type
print(gpu.type)

-- Component documentation
component.doc("gpu", "set")  -- Documentation for gpu.set
```

## Event System

### Basic Event Handling

```lua
local event = require("event")

-- Pull next event (wait forever)
local eventName, param1, param2 = event.pull()

-- Pull with timeout (5 seconds)
local eventName = event.pull(5)

-- Pull specific event type
local _, address, char, code = event.pull("key_down")

-- Listen for multiple events
while true do
  local eventName, param1, param2 = event.pull()
  
  if eventName == "key_down" then
    print("Key pressed: " .. param2)
  elseif eventName == "touch" then
    print("Screen touched")
  end
end
```

### Event Listeners

```lua
local event = require("event")

-- Register event listener
local function onKeyPress(eventName, address, char, code, player)
  print("Key: " .. code)
end

event.listen("key_down", onKeyPress)

-- Unregister listener
event.ignore("key_down", onKeyPress)

-- Timer (one-shot)
local timerID = event.timer(5, function()
  print("5 seconds passed!")
end)

-- Cancel timer
event.cancel(timerID)
```

### Common Events

```lua
-- Keyboard events
event.listen("key_down", function(_, _, char, code, player)
  print("Key down: " .. code)
end)

event.listen("key_up", function(_, _, char, code, player)
  print("Key up: " .. code)
end)

-- Screen events
event.listen("touch", function(_, address, x, y, button, player)
  print("Touch at: " .. x .. ", " .. y)
end)

event.listen("drag", function (_, address, x, y, button, player)
  print("Drag at: " .. x .. ", " .. y)
end)

-- Redstone events
event.listen("redstone_changed", function(_, address, side, oldValue, newValue)
  print("Redstone changed on side " .. side)
end)

-- Component events
event.listen("component_added", function(_, address, componentType)
  print("Component added: " .. componentType)
end)

event.listen("component_removed", function(_, address, componentType)
  print("Component removed: " .. componentType)
end)

-- Network events
event.listen("modem_message", function(_, localAddress, remoteAddress, port, distance, message)
  print("Received: " .. tostring(message))
end)
```

## Filesystem API

### Basic File Operations

```lua
local filesystem = require("filesystem")

-- Check if file exists
if filesystem.exists("/home/file.txt") then
  print("File exists")
end

-- Check if directory
if filesystem.isDirectory("/home") then
  print("Is directory")
end

-- Get file size
local size = filesystem.size("/home/file.txt")

-- List directory
for file in filesystem.list("/home") do
  print(file)
end

-- Make directory
filesystem.makeDirectory("/home/mydir")

-- Remove file
filesystem.remove("/home/file.txt")

-- Rename/move
filesystem.rename("/home/old.txt", "/home/new.txt")

-- Copy file
filesystem.copy("/home/file.txt", "/home/backup.txt")
```

### Reading Files

```lua
-- Read entire file
local file = io.open("/home/data.txt", "r")
if file then
  local content = file:read("*a")
  file:close()
  print(content)
end

-- Read line by line
local file = io.open("/home/data.txt", "r")
if file then
  for line in file:lines() do
    print(line)
  end
  file:close()
end

-- Read specific number of bytes
local file = io.open("/home/data.txt", "r")
if file then
  local chunk = file:read(100)  -- Read 100 bytes
  file:close()
end
```

### Writing Files

```lua
-- Write to file (overwrite)
local file = io.open("/home/output.txt", "w")
if file then
  file:write("Hello\n")
  file:write("World\n")
  file:close()
end

-- Append to file
local file = io.open("/home/log.txt", "a")
if file then
  file:write(os.date() .. ": Event logged\n")
  file:close()
end
```

### Serialization

```lua
local serialization = require("serialization")

-- Serialize table to string
local data = {
  name = "Computer1",
  x = 100,
  y = 64,
  z = 200,
  items = {"iron", "gold", "diamond"}
}

local serialized = serialization.serialize(data)

-- Save to file
local file = io.open("/home/config.txt", "w")
file:write(serialized)
file:close()

-- Load and deserialize
local file = io.open("/home/config.txt", "r")
local content = file:read("*a")
file:close()
local loaded = serialization.unserialize(content)

print(loaded.name)  -- "Computer1"
```

## Graphics Programming

### Basic Graphics

```lua
local component = require("component")
local gpu = component.gpu

-- Get screen resolution
local width, height = gpu.getResolution()

-- Set resolution (if supported)
gpu.setResolution(80, 25)

-- Clear screen
gpu.fill(1, 1, width, height, " ")

-- Set colors
gpu.setBackground(0x000000)  -- Black
gpu.setForeground(0xFFFFFF)  -- White

-- Draw text
gpu.set(1, 1, "Hello, World!")
gpu.set(10, 10, "X: 10, Y: 10")

-- Fill area with character
gpu.fill(5, 5, 10, 5, "#")  -- 10x5 rectangle filled with '#'

-- Copy region
gpu.copy(1, 1, 10, 10, 5, 0)  -- Copy 10x10 area, move 5 right
```

### Colors

```lua
-- Color values (24-bit RGB)
local colors = {
  black = 0x000000,
  white = 0xFFFFFF,
  red = 0xFF0000,
  green = 0x00FF00,
  blue = 0x0000FF,
  yellow = 0xFFFF00,
  cyan = 0x00FFFF,
  magenta = 0xFF00FF,
  orange = 0xFFA500,
  purple = 0x800080
}

-- Set colors
gpu.setBackground(colors.blue)
gpu.setForeground(colors.white)

-- Get current colors
local bg = gpu.getBackground()
local fg = gpu.getForeground()

-- Get color depth
local depth = gpu.getDepth()  -- 1, 4, or 8 (bits per pixel)

-- Set color depth
gpu.setDepth(8)  -- 256 colors
```

### Drawing UI Elements

```lua
-- Draw box
local function drawBox(x, y, width, height, color)
  gpu.setBackground(color)
  gpu.fill(x, y, width, height, " ")
  gpu.setBackground(0x000000)
end

-- Draw border
local function drawBorder(x, y, width, height)
  gpu.set(x, y, "┌" .. string.rep("─", width-2) .. "┐")
  for i = 1, height-2 do
    gpu.set(x, y+i, "│")
    gpu.set(x+width-1, y+i, "│")
  end
  gpu.set(x, y+height-1, "└" .. string.rep("─", width-2) .. "┘")
end

-- Draw progress bar
local function drawProgressBar(x, y, width, value, max)
  local filled = math.floor((value / max) * width)
  gpu.setBackground(0x00FF00)
  gpu.fill(x, y, filled, 1, " ")
  gpu.setBackground(0x555555)
  gpu.fill(x + filled, y, width - filled, 1, " ")
  gpu.setBackground(0x000000)
end

-- Usage
drawBox(10, 5, 30, 10, 0x0000FF)
drawBorder(10, 5, 30, 10)
drawProgressBar(12, 10, 26, 50, 100)
```

## Redstone Control

### Basic Redstone I/O

```lua
local component = require("component")
local sides = require("sides")
local rs = component.redstone

-- Output signal
rs.setOutput(sides.south, 15)  -- Max strength
rs.setOutput(sides.north, 0)   -- Off

-- Read input
local strength = rs.getInput(sides.east)
print("Input strength: " .. strength)

-- Read all sides
for i = 0, 5 do
  local input = rs.getInput(i)
  print("Side " .. i .. ": " .. input)
end
```

### Side Constants

```lua
local sides = require("sides")

-- Side numbering:
-- 0 = bottom (down)
-- 1 = top (up)
-- 2 = north (back in GUI)
-- 3 = south (front in GUI)
-- 4 = west (left)
-- 5 = east (right)

local side = sides.south  -- = 3
```

### Redstone Automation Example

```lua
local component = require("component")
local event = require("event")
local sides = require("sides")
local rs = component.redstone

-- Monitor redstone input and trigger output
while true do
  local _, address, side, oldValue, newValue = event.pull("redstone_changed")
  
  if side == sides.north and newValue > 0 then
    print("Input detected! Activating output...")
    rs.setOutput(sides.south, 15)
    os.sleep(2)
    rs.setOutput(sides.south, 0)
  end
end
```

## Networking

### Basic Networking

```lua
local component = require("component")
local event = require("event")
local modem = component.modem

-- Open port
modem.open(123)

-- Broadcast message (all computers in range)
modem.broadcast(123, "Hello everyone!")

-- Send to specific address
modem.send("address-here", 123, "Direct message")

-- Close port
modem.close(123)
```

### Receiving Messages

```lua
local component = require("component")
local event = require("event")
local modem = component.modem

modem.open(123)

print("Listening on port 123...")
while true do
  local _, localAddress, remoteAddress, port, distance, message = event.pull("modem_message")
  print("From " .. remoteAddress .. ": " .. tostring(message))
end
```

### Request-Response Pattern

**Server:**
```lua
local component = require("component")
local event = require("event")
local modem = component.modem

modem.open(1000)  -- Listen on port 1000

print("Server listening...")
while true do
  local _, _, from, port, _, request = event.pull("modem_message")
  
  if request == "PING" then
    modem.send(from, port, "PONG")
    print("Responded to " .. from)
  elseif request == "TIME" then
    modem.send(from, port, os.time())
  end
end
```

**Client:**
```lua
local component = require("component")
local event = require("event")
local modem = component.modem

local serverAddress = "server-address-here"
modem.open(2000)  -- Our response port

-- Send request
modem.send(serverAddress, 1000, "PING")

-- Wait for response (5 second timeout)
local _, _, from, port, _, response = event.pull(5, "modem_message")
if response then
  print("Server response: " .. response)
else
  print("No response (timeout)")
end

modem.close(2000)
```

## Robot Programming

### Robot Movement

```lua
local robot = require("robot")

-- Movement
robot.forward()   -- Move forward
robot.back()      -- Move backward
robot.up()        -- Move up
robot.down()      -- Move down
robot.turnLeft()  -- Turn left
robot.turnRight() -- Turn right

-- All movement functions return:
-- true on success
-- false, "error message" on failure
```

### Robot Interaction

```lua
local robot = require("robot")

-- Mining/digging
robot.swing()       -- Swing tool (mine block in front)
robot.swingUp()     -- Mine above
robot.swingDown()   -- Mine below

-- Placing blocks
robot.place()       -- Place block in front
robot.placeUp()     -- Place above
robot.placeDown()   -- Place below

-- Detecting/scanning
robot.detect()      -- Check if block in front
robot.detectUp()    -- Check above
robot.detectDown()  -- Check below

-- Using items
robot.use()         -- Use item in front
robot.useUp()       -- Use item above
robot.useDown()     -- Use item below
```

### Robot Inventory

```lua
local robot = require("robot")

-- Select slot (1-16 typically)
robot.select(1)

-- Get selected slot
local slot = robot.select()

-- Transfer items
robot.transferTo(2, 10)  -- Move 10 items from current to slot 2

-- Get item count
local count = robot.count(1)  -- Items in slot 1

-- Get item space
local space = robot.space(1)  -- Free space in slot 1

-- Drop items
robot.drop()       -- Drop items from selected slot in front
robot.dropUp()     -- Drop above
robot.dropDown()   -- Drop below

-- Suck items
robot.suck()       -- Pick up items in front
robot.suckUp()     -- Pick up above
robot.suckDown()   -- Pick up below
```

### Mining Robot Example

```lua
local robot = require("robot")
local component = require("component")

-- Simple mining program
local function mineForward(distance)
  for i = 1, distance do
    -- Mine block ahead
    while robot.detect() do
      robot.swing()
    end
    
    -- Move forward
    if not robot.forward() then
      print("Cannot move forward!")
      return false
    end
  end
  return true
end

-- Mine 10 blocks forward
mineForward(10)
```

## Advanced Topics

### Multi-threading with Coroutines

```lua
local thread = require("thread")

-- Create thread
local t = thread.create(function()
  while true do
    print("Background task running...")
    os.sleep(1)
  end
end)

-- Detach thread (run in background)
t:detach()

-- Main program continues
print("Main program running")
os.sleep(5)
```

### Computer API

```lua
local computer = require("computer")

-- Get computer info
print("Address: " .. computer.address())
print("Total memory: " .. computer.totalMemory())
print("Free memory: " .. computer.freeMemory())
print("Energy: " .. computer.energy())
print("Max energy: " .. computer.maxEnergy())
print("Uptime: " .. computer.uptime())

-- Control computer
computer.beep(1000, 0.5)  -- Beep at 1000Hz for 0.5 seconds
computer.shutdown()       -- Shutdown computer
computer.shutdown(true)   -- Reboot computer
```

### Error Handling

```lua
-- pcall (protected call)
local success, result = pcall(function()
  return dangerousFunction()
end)

if success then
  print("Result: " .. result)
else
  print("Error: " .. result)
end

-- xpcall (extended protected call with error handler)
local function errorHandler(err)
  print("Error caught: " .. tostring(err))
  print(debug.traceback())
end

xpcall(function()
  dangerousFunction()
end, errorHandler)

-- assert
local file = assert(io.open("/home/file.txt", "r"), "Failed to open file")
```

### Performance Tips

1. **Cache component proxies:**
```lua
-- Bad: Looking up component every call
for i = 1, 1000 do
  component.gpu.set(1, 1, "test")
end

-- Good: Cache proxy once
local gpu = component.gpu
for i = 1, 1000 do
  gpu.set(1, 1, "test")
end
```

2. **Batch GPU operations:**
```lua
-- Use gpu.fill instead of multiple gpu.set calls
gpu.fill(1, 1, 80, 25, " ")  -- Clear screen in one call
```

3. **Limit event polling frequency:**
```lua
-- Add timeout to event.pull to yield CPU
event.pull(0.05)  -- 50ms timeout = 20 ticks/sec max
```

4. **Close unused files:**
```lua
local file = io.open("/home/file.txt", "r")
-- ... use file ...
file:close()  -- Always close when done
```

---

For more examples, check `/home/examples/` on your computer or visit the [OpenComputers Wiki](https://ocdoc.cil.li/).
