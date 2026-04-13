package li.cil.oc.server.fs

/**
 * Populates a VirtualFileSystem with a simplified OpenOS-compatible system.
 * 
 * This provides:
 * - /init.lua (boot entry)
 * - /lib/ (core libraries: filesystem, event, shell, term, component, etc.)
 * - /bin/ (commands: sh, ls, cat, cp, mv, rm, mkdir, pastebin, install, edit, etc.)
 * - /boot/ (boot scripts)
 * - /etc/ (config)
 * - /home/ (user home)
 * - /tmp/ (temp dir)
 */
object OpenOSContent {

    fun populate(fs: VirtualFileSystem) {
        fs.makeDirectory("bin")
        fs.makeDirectory("boot")
        fs.makeDirectory("etc")
        fs.makeDirectory("home")
        fs.makeDirectory("lib")
        fs.makeDirectory("lib/core")
        fs.makeDirectory("tmp")
        fs.makeDirectory("usr")
        fs.makeDirectory("usr/bin")

        fs.writeFile(".prop", """{"label":"OpenOS","reboot":true,"setboot":true}""")

        // ============ /init.lua ============
        fs.writeFile("init.lua", INIT_LUA)

        // ============ /lib/core/boot.lua ============
        fs.writeFile("lib/core/boot.lua", BOOT_LUA)

        // ============ Core libraries ============
        fs.writeFile("lib/event.lua", EVENT_LUA)
        fs.writeFile("lib/filesystem.lua", FILESYSTEM_LUA)
        fs.writeFile("lib/shell.lua", SHELL_LUA)
        fs.writeFile("lib/term.lua", TERM_LUA)
        fs.writeFile("lib/text.lua", TEXT_LUA)
        fs.writeFile("lib/io.lua", IO_LUA)

        // ============ Boot scripts ============
        fs.writeFile("boot/00_base.lua", BOOT_00_BASE)
        fs.writeFile("boot/01_term.lua", BOOT_01_TERM)
        fs.writeFile("boot/02_fs.lua", BOOT_02_FS)

        // ============ Programs ============
        fs.writeFile("bin/sh.lua", SH_LUA)
        fs.writeFile("bin/ls.lua", LS_LUA)
        fs.writeFile("bin/cat.lua", CAT_LUA)
        fs.writeFile("bin/cp.lua", CP_LUA)
        fs.writeFile("bin/mv.lua", MV_LUA)
        fs.writeFile("bin/rm.lua", RM_LUA)
        fs.writeFile("bin/mkdir.lua", MKDIR_LUA)
        fs.writeFile("bin/edit.lua", EDIT_LUA)
        fs.writeFile("bin/pastebin.lua", PASTEBIN_LUA)
        fs.writeFile("bin/install.lua", INSTALL_LUA)
        fs.writeFile("bin/reboot.lua", REBOOT_LUA)
        fs.writeFile("bin/clear.lua", CLEAR_LUA)
        fs.writeFile("bin/echo.lua", ECHO_LUA)
        fs.writeFile("bin/cd.lua", CD_LUA)
        fs.writeFile("bin/wget.lua", WGET_LUA)

        // ============ Config ============
        fs.writeFile("etc/profile.lua", PROFILE_LUA)
        fs.writeFile("etc/motd", "")

        fs.writeFile("home/.shrc", "")
    }

    // ================================================================
    // init.lua - Boot entry point (matches original OC)
    // ================================================================
    val INIT_LUA = """
do
  local addr, invoke = computer.getBootAddress(), component.invoke
  local function loadfile(file)
    local handle = assert(invoke(addr, "open", file))
    local buffer = ""
    repeat
      local data = invoke(addr, "read", handle, math.huge)
      buffer = buffer .. (data or "")
    until not data
    invoke(addr, "close", handle)
    return load(buffer, "=" .. file, "t", _G)
  end
  loadfile("/lib/core/boot.lua")(loadfile)
end

while true do
  local ok, err = pcall(function()
    local shell = require("shell")
    if shell and shell.getShell then
      shell.getShell()()
    else
      -- Fallback shell
      local term = require("term")
      while true do
        term.write(os.getenv("PS1") or "# ")
        local line = term.read()
        if line and #line > 0 then
          local fn, err = load(line, "=stdin")
          if fn then
            local ok2, err2 = pcall(fn)
            if not ok2 then print(err2) end
          else
            -- Try as command
            local parts = {}
            for w in line:gmatch("%S+") do parts[#parts+1] = w end
            local cmd = parts[1]
            local path = "/bin/" .. cmd .. ".lua"
            if require("filesystem").exists(path) then
              local f = loadfile(path)
              if f then
                local ok2, err2 = pcall(f, table.unpack(parts, 2))
                if not ok2 then print(err2) end
              end
            else
              print(cmd .. ": command not found")
            end
          end
        end
      end
    end
  end)
  if not ok then
    io.write(tostring(err) .. "\n")
    io.write("Press any key to continue.\n")
    computer.pullSignal(1)
  end
end
""".trimIndent()

    // ================================================================
    // /lib/core/boot.lua - Kernel bootstrap
    // ================================================================
    val BOOT_LUA = """
local loadfile = ...
_OSVERSION = "OpenOS 1.8 (OpenCoudou)"

-- Bind GPU to screen
do
  local screen = component.list("screen")()
  local gpu = component.list("gpu")()
  if gpu and screen then
    component.invoke(gpu, "bind", screen)
  end
end

-- Status display
local y = 1
local function status(msg)
  local gpu = component.list("gpu")()
  if gpu then
    component.invoke(gpu, "set", 1, y, msg)
    y = y + 1
  end
end

status(_OSVERSION)
status("Booting...")

-- Minimal package system
local loaded = {}
local loading = {}

function require(name)
  if loaded[name] then return loaded[name] end
  if loading[name] then error("circular dependency: " .. name) end
  loading[name] = true
  
  -- Try lib paths
  local paths = {
    "/lib/" .. name .. ".lua",
    "/lib/" .. name .. "/init.lua",
    "/usr/lib/" .. name .. ".lua",
  }
  
  local addr = computer.getBootAddress()
  for _, path in ipairs(paths) do
    local ok, handle = pcall(component.invoke, addr, "open", path)
    if ok and handle then
      local buffer = ""
      repeat
        local data = component.invoke(addr, "read", handle, math.huge)
        buffer = buffer .. (data or "")
      until not data
      component.invoke(addr, "close", handle)
      local fn, err = load(buffer, "=" .. path, "t", _G)
      if fn then
        local result = fn()
        loaded[name] = result or true
        loading[name] = nil
        return loaded[name]
      end
    end
  end
  
  loading[name] = nil
  error("module not found: " .. name)
end

_G.require = require

-- Environment variables
local env = {
  HOME = "/home",
  PATH = "/bin:/usr/bin",
  PS1 = "/home # ",
  SHELL = "/bin/sh.lua",
  TMPDIR = "/tmp",
  TERM = "opencomputers",
}
os.getenv = function(k) return env[k] end
os.setenv = function(k, v) env[k] = v end

-- Basic print
_G.print = function(...)
  local args = {...}
  local gpu = component.list("gpu")()
  if not gpu then return end
  local s = ""
  for i = 1, #args do
    if i > 1 then s = s .. "\t" end
    s = s .. tostring(args[i])
  end
  local term = loaded["term"]
  if term and term.write then
    term.write(s .. "\n")
  else
    component.invoke(gpu, "set", 1, y, s)
    y = y + 1
  end
end

_G.loadfile = function(path)
  local addr = computer.getBootAddress()
  local ok, handle = pcall(component.invoke, addr, "open", path)
  if not ok or not handle then return nil, "file not found: " .. path end
  local buffer = ""
  repeat
    local data = component.invoke(addr, "read", handle, math.huge)
    buffer = buffer .. (data or "")
  until not data
  component.invoke(addr, "close", handle)
  return load(buffer, "=" .. path, "t", _G)
end

_G.dofile = function(path, ...)
  local fn, err = loadfile(path)
  if not fn then error(err) end
  return fn(...)
end

-- Run boot scripts
status("Running boot scripts...")
local addr = computer.getBootAddress()
local bootFiles = component.invoke(addr, "list", "boot")
if bootFiles then
  table.sort(bootFiles)
  for _, file in ipairs(bootFiles) do
    if file:match("%.lua$") then
      status("  " .. file)
      local ok, err = pcall(dofile, "/boot/" .. file)
      if not ok then status("  Error: " .. tostring(err)) end
    end
  end
end

status("Boot complete.")
status("")

-- Load term for proper display
pcall(require, "term")
local term = loaded["term"]
if term and term.clear then
  term.clear()
end
""".trimIndent()

    // ================================================================
    // /lib/event.lua - Event system
    // ================================================================
    val EVENT_LUA = """
local event = {}
local handlers = {}
local lastInterrupt = -math.huge

event.handlers = handlers

function event.register(key, callback, interval, times, opt_handlers)
  local handler = {
    key = key,
    times = times or 1,
    callback = callback,
    interval = interval or math.huge,
  }
  handler.timeout = computer.uptime() + handler.interval
  opt_handlers = opt_handlers or handlers
  local id = 0
  repeat
    id = id + 1
  until not opt_handlers[id]
  opt_handlers[id] = handler
  return id
end

local _pullSignal = computer.pullSignal
setmetatable(handlers, {__call = function(_, ...) return _pullSignal(...) end})

computer.pullSignal = function(seconds)
  checkArg(1, seconds, "number", "nil")
  seconds = seconds or math.huge
  local uptime = computer.uptime
  local deadline = uptime() + seconds
  repeat
    local closest = deadline
    for _, handler in pairs(handlers) do
      closest = math.min(handler.timeout, closest)
    end
    local event_data = table.pack(handlers(closest - uptime()))
    local signal = event_data[1]
    local copy = {}
    for id, handler in pairs(handlers) do
      copy[id] = handler
    end
    for id, handler in pairs(copy) do
      if (handler.key == nil or handler.key == signal) or uptime() >= handler.timeout then
        handler.times = handler.times - 1
        handler.timeout = handler.timeout + handler.interval
        if handler.times <= 0 and handlers[id] == handler then
          handlers[id] = nil
        end
        local result, message = pcall(handler.callback, table.unpack(event_data, 1, event_data.n))
        if not result then
          pcall(event.onError, message)
        elseif message == false and handlers[id] == handler then
          handlers[id] = nil
        end
      end
    end
    if signal then
      return table.unpack(event_data, 1, event_data.n)
    end
  until uptime() >= deadline
end

local function createPlainFilter(name, ...)
  local filter = table.pack(...)
  if name == nil and filter.n == 0 then
    return nil
  end
  return function(...)
    local signal = table.pack(...)
    if name and not (type(signal[1]) == "string" and signal[1]:match(name)) then
      return false
    end
    for i = 1, filter.n do
      if filter[i] ~= nil and filter[i] ~= signal[i + 1] then
        return false
      end
    end
    return true
  end
end

local function createMultipleFilter(...)
  local filter = table.pack(...)
  if filter.n == 0 then
    return nil
  end
  return function(...)
    local signal = table.pack(...)
    if type(signal[1]) ~= "string" then
      return false
    end
    for i = 1, filter.n do
      if filter[i] ~= nil and signal[1]:match(filter[i]) then
        return true
      end
    end
    return false
  end
end

function event.listen(name, callback)
  checkArg(1, name, "string")
  checkArg(2, callback, "function")
  for _, handler in pairs(handlers) do
    if handler.key == name and handler.callback == callback then
      return false
    end
  end
  return event.register(name, callback, math.huge, math.huge)
end

function event.ignore(name, callback)
  checkArg(1, name, "string")
  checkArg(2, callback, "function")
  for id, handler in pairs(handlers) do
    if handler.key == name and handler.callback == callback then
      handlers[id] = nil
      return true
    end
  end
  return false
end

function event.timer(interval, callback, times)
  checkArg(1, interval, "number")
  checkArg(2, callback, "function")
  checkArg(3, times, "number", "nil")
  return event.register(false, callback, interval, times)
end

function event.cancel(timerId)
  checkArg(1, timerId, "number")
  if handlers[timerId] then
    handlers[timerId] = nil
    return true
  end
  return false
end

function event.pull(...)
  local args = table.pack(...)
  if type(args[1]) == "string" then
    return event.pullFiltered(createPlainFilter(...))
  else
    checkArg(1, args[1], "number", "nil")
    checkArg(2, args[2], "string", "nil")
    return event.pullFiltered(args[1], createPlainFilter(select(2, ...)))
  end
end

function event.pullFiltered(...)
  local args = table.pack(...)
  local seconds, filter = math.huge
  if type(args[1]) == "function" then
    filter = args[1]
  else
    checkArg(1, args[1], "number", "nil")
    checkArg(2, args[2], "function", "nil")
    seconds = args[1]
    filter = args[2]
  end
  local deadline = computer.uptime() + (seconds or math.huge)
  repeat
    local waitTime = deadline - computer.uptime()
    if waitTime < 0 then
      break
    end
    local signal = table.pack(computer.pullSignal(waitTime))
    if signal.n > 0 then
      if not (seconds or filter) or filter == nil or filter(table.unpack(signal, 1, signal.n)) then
        return table.unpack(signal, 1, signal.n)
      end
    end
  until signal.n == 0
end

function event.pullMultiple(...)
  local seconds
  local args
  if type(...) == "number" then
    seconds = ...
    args = table.pack(select(2, ...))
    for i = 1, args.n do
      checkArg(i + 1, args[i], "string", "nil")
    end
  else
    args = table.pack(...)
    for i = 1, args.n do
      checkArg(i, args[i], "string", "nil")
    end
  end
  return event.pullFiltered(seconds, createMultipleFilter(table.unpack(args, 1, args.n)))
end

function event.onError(message)
  local ok, fio = pcall(require, "io")
  if ok and fio and fio.open then
    local log = fio.open("/tmp/event.log", "a")
    if log then
      pcall(log.write, log, tostring(message) .. "\n")
      log:close()
    end
  end
end

event.push = computer.pushSignal

return event
""".trimIndent()

    // ================================================================
    // /lib/filesystem.lua - Filesystem library
    // ================================================================
    val FILESYSTEM_LUA = """
local filesystem = {}
local mounts = {}

function filesystem.mount(address, path)
  mounts[path] = address
  return true
end

function filesystem.umount(path)
  mounts[path] = nil
  return true
end

function filesystem.mounts()
  return pairs(mounts)
end

-- Resolve path to filesystem address and relative path
local function resolve(path)
  path = filesystem.canonical(path)
  local best = ""
  local bestAddr = nil
  for mpath, addr in pairs(mounts) do
    if path:sub(1, #mpath) == mpath and #mpath > #best then
      best = mpath
      bestAddr = addr
    end
  end
  if bestAddr then
    local rel = path:sub(#best + 1)
    if rel == "" then rel = "/" end
    return bestAddr, rel
  end
  -- Fallback to boot address
  return computer.getBootAddress(), path
end

function filesystem.canonical(path)
  local parts = {}
  for part in path:gmatch("[^/]+") do
    if part == ".." then
      if #parts > 0 then table.remove(parts) end
    elseif part ~= "." then
      parts[#parts+1] = part
    end
  end
  return "/" .. table.concat(parts, "/")
end

function filesystem.exists(path)
  local addr, rel = resolve(path)
  return component.invoke(addr, "exists", rel)
end

function filesystem.isDirectory(path)
  local addr, rel = resolve(path)
  return component.invoke(addr, "isDirectory", rel)
end

function filesystem.list(path)
  local addr, rel = resolve(path)
  local files = component.invoke(addr, "list", rel)
  if not files then return nil, "cannot list" end
  local i = 0
  return function()
    i = i + 1
    return files[i]
  end
end

function filesystem.size(path)
  local addr, rel = resolve(path)
  return component.invoke(addr, "size", rel)
end

function filesystem.open(path, mode)
  mode = mode or "r"
  local addr, rel = resolve(path)
  local handle, err = component.invoke(addr, "open", rel, mode)
  if not handle then return nil, err or "cannot open" end
  
  local f = {}
  f.handle = handle
  f.addr = addr
  
  function f:read(n)
    return component.invoke(self.addr, "read", self.handle, n or math.huge)
  end
  
  function f:write(data)
    return component.invoke(self.addr, "write", self.handle, data)
  end
  
  function f:close()
    return component.invoke(self.addr, "close", self.handle)
  end
  
  function f:seek(whence, offset)
    return component.invoke(self.addr, "seek", self.handle, whence, offset)
  end
  
  return f
end

function filesystem.makeDirectory(path)
  local addr, rel = resolve(path)
  return component.invoke(addr, "makeDirectory", rel)
end

function filesystem.remove(path)
  local addr, rel = resolve(path)
  return component.invoke(addr, "remove", rel)
end

function filesystem.rename(from, to)
  local addrF, relF = resolve(from)
  local addrT, relT = resolve(to)
  if addrF == addrT then
    return component.invoke(addrF, "rename", relF, relT)
  end
  return false, "cross-device rename"
end

function filesystem.copy(from, to)
  local fin = filesystem.open(from, "r")
  if not fin then return false, "source not found" end
  local fout = filesystem.open(to, "w")
  if not fout then fin:close(); return false, "cannot create target" end
  while true do
    local data = fin:read(4096)
    if not data then break end
    fout:write(data)
  end
  fin:close()
  fout:close()
  return true
end

function filesystem.get(path)
  local addr, rel = resolve(path)
  return component.proxy(addr), rel
end

function filesystem.isReadOnly(path)
  local addr = resolve(path)
  local ok, result = pcall(component.invoke, addr, "isReadOnly")
  return ok and result
end

function filesystem.getLabel(addr)
  local ok, result = pcall(component.invoke, addr, "getLabel")
  return ok and result or ""
end

function filesystem.spaceTotal(path)
  local addr = resolve(path)
  local ok, result = pcall(component.invoke, addr, "spaceTotal")
  return ok and result or 0
end

function filesystem.spaceUsed(path)
  local addr = resolve(path)
  local ok, result = pcall(component.invoke, addr, "spaceUsed")
  return ok and result or 0
end

return filesystem
""".trimIndent()

    // ================================================================
    // /lib/shell.lua - Shell library
    // ================================================================
    val SHELL_LUA = """
local shell = {}

local aliases = {
  dir = "ls",
  copy = "cp",
  move = "mv",
  del = "rm",
  md = "mkdir",
}

function shell.resolve(cmd)
  if cmd:sub(1,1) == "/" then return cmd end
  local path = os.getenv("PATH") or "/bin"
  for dir in path:gmatch("[^:]+") do
    local full = dir .. "/" .. cmd .. ".lua"
    if require("filesystem").exists(full) then
      return full
    end
    -- Check alias
    if aliases[cmd] then
      full = dir .. "/" .. aliases[cmd] .. ".lua"
      if require("filesystem").exists(full) then
        return full
      end
    end
  end
  return nil
end

function shell.execute(cmd, ...)
  local path = shell.resolve(cmd)
  if not path then
    return false, cmd .. ": command not found"
  end
  local fn, err = loadfile(path)
  if not fn then return false, err end
  return pcall(fn, ...)
end

function shell.getShell()
  return function()
    local term = require("term")
    local fs = require("filesystem")
    
    -- Run profile
    if fs.exists("/etc/profile.lua") then
      pcall(dofile, "/etc/profile.lua")
    end
    
    -- Show MOTD
    if fs.exists("/etc/motd") then
      local f = fs.open("/etc/motd", "r")
      if f then
        local content = f:read(math.huge)
        f:close()
        if content and #content > 0 then print(content) end
      end
    end
    
    -- Main shell loop
    local shellHistory = {}
    while true do
      local cwd = os.getenv("CWD") or "/home"
      local prompt = cwd .. " # "
      term.write(prompt)
      local line = term.read(shellHistory)
      if line then
        line = line:match("^%s*(.-)%s*$") or ""
        if #line > 0 then
          -- Parse command and args
          local parts = {}
          for w in line:gmatch("%S+") do parts[#parts+1] = w end
          local cmd = parts[1]
          
          if cmd == "exit" then
            return
          end
          
          -- Try as shell command
          local ok, err = shell.execute(cmd, table.unpack(parts, 2))
          if not ok then
            -- Try as Lua expression
            local fn = load("return " .. line)
            if fn then
              local ok2, result = pcall(fn)
              if ok2 and result ~= nil then print(tostring(result)) end
            else
              print(err or (cmd .. ": command not found"))
            end
          end
        end
      end
    end
  end
end

function shell.getWorkingDirectory()
  return os.getenv("CWD") or "/home"
end

function shell.setWorkingDirectory(dir)
  os.setenv("CWD", dir)
end

return shell
""".trimIndent()

    // ================================================================
    // /lib/term.lua - Terminal library
    // ================================================================
    val TERM_LUA = """
local term = {}

local cursorX = 1
local cursorY = 1
local gpuAddr = nil
local screenW = 80
local screenH = 25

local function getGpu()
  if not gpuAddr then gpuAddr = component.list("gpu")() end
  return gpuAddr
end

local function refreshSize()
  local gpu = getGpu()
  if gpu then
    screenW, screenH = component.invoke(gpu, "getResolution")
  end
end

function term.clear()
  local gpu = getGpu()
  if not gpu then return end
  refreshSize()
  component.invoke(gpu, "setBackground", 0x000000)
  component.invoke(gpu, "setForeground", 0xFFFFFF)
  component.invoke(gpu, "fill", 1, 1, screenW, screenH, " ")
  cursorX = 1
  cursorY = 1
end

function term.getCursor()
  return cursorX, cursorY
end

function term.setCursor(x, y)
  cursorX = x
  cursorY = y
end

local function scroll()
  local gpu = getGpu()
  if not gpu then return end
  component.invoke(gpu, "copy", 1, 2, screenW, screenH - 1, 0, -1)
  component.invoke(gpu, "fill", 1, screenH, screenW, 1, " ")
  cursorY = screenH
end

function term.write(text)
  local gpu = getGpu()
  if not gpu then return end
  refreshSize()
  
  for i = 1, #text do
    local ch = text:sub(i, i)
    if ch == "\n" then
      cursorX = 1
      cursorY = cursorY + 1
      if cursorY > screenH then scroll() end
    elseif ch == "\r" then
      cursorX = 1
    elseif ch == "\t" then
      cursorX = cursorX + (4 - ((cursorX - 1) % 4))
      if cursorX > screenW then
        cursorX = 1
        cursorY = cursorY + 1
        if cursorY > screenH then scroll() end
      end
    else
      if cursorX > screenW then
        cursorX = 1
        cursorY = cursorY + 1
        if cursorY > screenH then scroll() end
      end
      component.invoke(gpu, "set", cursorX, cursorY, ch)
      cursorX = cursorX + 1
    end
  end
end

function term.read(history, dobreak)
  local gpu = getGpu()
  if not gpu then return "" end
  refreshSize()
  
  history = history or {}
  local histIdx = #history + 1
  local buffer = ""
  local pos = 0 -- cursor position within buffer (0 = before first char)
  local startX = cursorX
  local startY = cursorY
  
  local function redraw()
    -- Clear the line from startX
    local clearLen = screenW - startX + 1
    component.invoke(gpu, "fill", startX, startY, clearLen, 1, " ")
    -- Draw buffer
    if #buffer > 0 then
      component.invoke(gpu, "set", startX, startY, buffer)
    end
    -- Position cursor
    cursorX = startX + pos
  end
  
  while true do
    redraw()
    -- Show cursor
    local cursorChar = pos < #buffer and buffer:sub(pos + 1, pos + 1) or "_"
    component.invoke(gpu, "set", startX + pos, startY, cursorChar)
    
    local sig, _, char, code = computer.pullSignal(0.5)
    
    if sig == "key_down" then
      if char == 13 or code == 257 then -- Enter
        redraw()
        cursorX = 1
        cursorY = cursorY + 1
        if cursorY > screenH then scroll() end
        if #buffer > 0 then
          history[#history + 1] = buffer
        end
        return buffer
      elseif char == 4 then -- Ctrl+D
        if #buffer == 0 then return nil end
      elseif char == 8 or code == 259 then -- Backspace
        if pos > 0 then
          buffer = buffer:sub(1, pos - 1) .. buffer:sub(pos + 1)
          pos = pos - 1
        end
      elseif code == 261 then -- Delete
        if pos < #buffer then
          buffer = buffer:sub(1, pos) .. buffer:sub(pos + 2)
        end
      elseif code == 263 then -- Left
        if pos > 0 then pos = pos - 1 end
      elseif code == 262 then -- Right
        if pos < #buffer then pos = pos + 1 end
      elseif code == 268 then -- Home
        pos = 0
      elseif code == 269 then -- End
        pos = #buffer
      elseif code == 265 then -- Up (history)
        if histIdx > 1 then
          histIdx = histIdx - 1
          buffer = history[histIdx] or ""
          pos = #buffer
        end
      elseif code == 264 then -- Down (history)
        if histIdx <= #history then
          histIdx = histIdx + 1
          buffer = history[histIdx] or ""
          pos = #buffer
        end
      elseif char >= 32 and char < 127 then
        buffer = buffer:sub(1, pos) .. string.char(char) .. buffer:sub(pos + 1)
        pos = pos + 1
        if startX + pos > screenW then
          cursorX = 1
          cursorY = cursorY + 1
          if cursorY > screenH then scroll() end
        end
      end
    elseif sig == "clipboard" then
      local text = char
      if type(text) == "string" then
        for i = 1, #text do
          local c = text:sub(i, i)
          if c ~= "\n" and c ~= "\r" then
            buffer = buffer:sub(1, pos) .. c .. buffer:sub(pos + 1)
            pos = pos + 1
          end
        end
      end
    end
  end
end

function term.getViewport()
  refreshSize()
  return screenW, screenH
end

function term.isAvailable()
  return getGpu() ~= nil
end

return term
""".trimIndent()

    // ================================================================
    // /lib/text.lua - Text utilities
    // ================================================================
    val TEXT_LUA = """
local text = {}

function text.trim(s)
  return s:match("^%s*(.-)%s*$")
end

function text.tokenize(s)
  local tokens = {}
  for token in s:gmatch("%S+") do
    tokens[#tokens+1] = token
  end
  return tokens
end

function text.padRight(s, len)
  return s .. string.rep(" ", math.max(0, len - #s))
end

return text
""".trimIndent()

    // ================================================================
    // /lib/io.lua - I/O library
    // ================================================================
    val IO_LUA = """
local io = _G.io or {}

local stdout = {}
function stdout:write(...)
  local term = require("term")
  for _, v in ipairs({...}) do
    term.write(tostring(v))
  end
  return self
end
function stdout:close() end
function stdout:flush() end

local stderr = {}
function stderr:write(...)
  local term = require("term")
  local gpu = component.list("gpu")()
  if gpu then
    local oldFg = component.invoke(gpu, "getForeground")
    component.invoke(gpu, "setForeground", 0xFF0000)
    for _, v in ipairs({...}) do
      term.write(tostring(v))
    end
    component.invoke(gpu, "setForeground", oldFg)
  end
  return self
end
function stderr:close() end

local stdin = {}
function stdin:read(mode)
  local term = require("term")
  if mode == "*l" or mode == "l" or mode == nil then
    return term.read()
  elseif mode == "*a" or mode == "a" then
    return term.read()
  end
  return term.read()
end
function stdin:close() end

io.stdout = stdout
io.stderr = stderr
io.stdin = stdin

function io.write(...)
  return stdout:write(...)
end

function io.read(mode)
  return stdin:read(mode)
end

function io.open(path, mode)
  return require("filesystem").open(path, mode)
end

_G.io = io
return io
""".trimIndent()

    // ================================================================
    // Boot scripts
    // ================================================================
    val BOOT_00_BASE = """
-- Base boot script: set up loadfile, dofile, print, os.sleep
os.sleep = function(seconds)
  checkArg(1, seconds, "number", "nil")
  local deadline = computer.uptime() + (seconds or 0)
  repeat
    computer.pullSignal(deadline - computer.uptime())
  until computer.uptime() >= deadline
end
""".trimIndent()

    val BOOT_01_TERM = """
-- Initialize terminal
local term = require("term")
if term.clear then
  term.clear()
end
""".trimIndent()

    val BOOT_02_FS = """
-- Mount boot filesystem
local fs = require("filesystem")
fs.mount(computer.getBootAddress(), "/")

-- Mount all other filesystems under /mnt/
for addr, ctype in component.list("filesystem") do
  if addr ~= computer.getBootAddress() and addr ~= computer.tmpAddress() then
    local label = ""
    pcall(function() label = component.invoke(addr, "getLabel") or "" end)
    local mpath = "/mnt/" .. addr:sub(1, 3)
    fs.mount(addr, mpath)
  end
end

-- Mount tmpfs
if computer.tmpAddress then
  local tmp = computer.tmpAddress()
  if tmp and #tmp > 0 then
    fs.mount(tmp, "/tmp")
  end
end

os.setenv("CWD", "/home")
""".trimIndent()

    // ================================================================
    // Programs
    // ================================================================
    val SH_LUA = """
-- Shell - handled by shell.getShell()
local shell = require("shell")
local sh = shell.getShell()
sh()
""".trimIndent()

    val LS_LUA = """
local fs = require("filesystem")
local term = require("term")
local args = {...}
local path = args[1] or (os.getenv("CWD") or "/")
if path:sub(1,1) ~= "/" then
  path = (os.getenv("CWD") or "/") .. "/" .. path
end
path = fs.canonical(path)

if not fs.exists(path) then
  print("ls: cannot access '" .. path .. "': No such file or directory")
  return
end

if not fs.isDirectory(path) then
  print(path)
  return
end

local gpu = component.list("gpu")()
for name in fs.list(path) do
  if name:sub(-1) == "/" then
    -- Directory in blue
    if gpu then component.invoke(gpu, "setForeground", 0x5555FF) end
    term.write(name)
    if gpu then component.invoke(gpu, "setForeground", 0xFFFFFF) end
    term.write("\n")
  else
    term.write(name .. "\n")
  end
end
""".trimIndent()

    val CAT_LUA = """
local fs = require("filesystem")
local args = {...}
if #args == 0 then
  print("Usage: cat <file>")
  return
end
local path = args[1]
if path:sub(1,1) ~= "/" then
  path = (os.getenv("CWD") or "/") .. "/" .. path
end
path = fs.canonical(path)
local f, err = fs.open(path, "r")
if not f then
  print("cat: " .. (err or "cannot open file"))
  return
end
while true do
  local data = f:read(4096)
  if not data then break end
  io.write(data)
end
f:close()
""".trimIndent()

    val CP_LUA = """
local fs = require("filesystem")
local args = {...}
if #args < 2 then
  print("Usage: cp <source> <target>")
  return
end
local function resolve(p)
  if p:sub(1,1) ~= "/" then p = (os.getenv("CWD") or "/") .. "/" .. p end
  return fs.canonical(p)
end
local ok, err = fs.copy(resolve(args[1]), resolve(args[2]))
if not ok then print("cp: " .. (err or "failed")) end
""".trimIndent()

    val MV_LUA = """
local fs = require("filesystem")
local args = {...}
if #args < 2 then
  print("Usage: mv <source> <target>")
  return
end
local function resolve(p)
  if p:sub(1,1) ~= "/" then p = (os.getenv("CWD") or "/") .. "/" .. p end
  return fs.canonical(p)
end
local ok, err = fs.rename(resolve(args[1]), resolve(args[2]))
if not ok then
  -- Try copy+delete
  ok, err = fs.copy(resolve(args[1]), resolve(args[2]))
  if ok then fs.remove(resolve(args[1])) end
end
if not ok then print("mv: " .. (err or "failed")) end
""".trimIndent()

    val RM_LUA = """
local fs = require("filesystem")
local args = {...}
if #args == 0 then
  print("Usage: rm <path>")
  return
end
local function resolve(p)
  if p:sub(1,1) ~= "/" then p = (os.getenv("CWD") or "/") .. "/" .. p end
  return fs.canonical(p)
end
for _, p in ipairs(args) do
  if not fs.remove(resolve(p)) then
    print("rm: cannot remove '" .. p .. "'")
  end
end
""".trimIndent()

    val MKDIR_LUA = """
local fs = require("filesystem")
local args = {...}
if #args == 0 then
  print("Usage: mkdir <path>")
  return
end
local function resolve(p)
  if p:sub(1,1) ~= "/" then p = (os.getenv("CWD") or "/") .. "/" .. p end
  return fs.canonical(p)
end
for _, p in ipairs(args) do
  fs.makeDirectory(resolve(p))
end
""".trimIndent()

    val EDIT_LUA = """
local fs = require("filesystem")
local term = require("term")
local args = {...}
if #args == 0 then
  print("Usage: edit <file>")
  return
end
local function resolve(p)
  if p:sub(1,1) ~= "/" then p = (os.getenv("CWD") or "/") .. "/" .. p end
  return fs.canonical(p)
end
local path = resolve(args[1])

-- Load file content
local lines = {""}
if fs.exists(path) then
  local f = fs.open(path, "r")
  if f then
    local content = ""
    while true do
      local data = f:read(4096)
      if not data then break end
      content = content .. data
    end
    f:close()
    lines = {}
    for line in (content .. "\n"):gmatch("(.-)\n") do
      lines[#lines+1] = line
    end
    if #lines == 0 then lines = {""} end
  end
end

local gpu = component.list("gpu")()
local w, h = component.invoke(gpu, "getResolution")
local scrollY = 0
local curLine = 1
local curCol = 1

local function draw()
  component.invoke(gpu, "setBackground", 0x000000)
  component.invoke(gpu, "fill", 1, 1, w, h, " ")
  -- Title bar
  component.invoke(gpu, "setBackground", 0x002244)
  component.invoke(gpu, "fill", 1, 1, w, 1, " ")
  component.invoke(gpu, "setForeground", 0x55CCFF)
  component.invoke(gpu, "set", 1, 1, " EDIT: " .. path .. " | Ctrl+S save | Ctrl+Q quit")
  component.invoke(gpu, "setBackground", 0x000000)
  component.invoke(gpu, "setForeground", 0xFFFFFF)
  
  for i = 1, h - 2 do
    local lineIdx = scrollY + i
    if lineIdx <= #lines then
      component.invoke(gpu, "set", 1, i + 1, lines[lineIdx]:sub(1, w))
    end
  end
  
  -- Status bar
  component.invoke(gpu, "setBackground", 0x222222)
  component.invoke(gpu, "fill", 1, h, w, 1, " ")
  component.invoke(gpu, "set", 1, h, " Line " .. curLine .. "/" .. #lines .. " Col " .. curCol)
  component.invoke(gpu, "setBackground", 0x000000)
end

draw()
while true do
  local sig, _, char, code = computer.pullSignal(0.5)
  if sig == "key_down" then
    if code == 17 and char == 17 then -- Ctrl+Q
      break
    elseif code == 31 and char == 19 then -- Ctrl+S
      local f = fs.open(path, "w")
      if f then
        f:write(table.concat(lines, "\n"))
        f:close()
        component.invoke(gpu, "set", 1, h, " Saved!                    ")
        computer.pullSignal(0.5)
      end
    elseif char == 13 or code == 257 then -- Enter
      local rest = lines[curLine]:sub(curCol)
      lines[curLine] = lines[curLine]:sub(1, curCol - 1)
      table.insert(lines, curLine + 1, rest)
      curLine = curLine + 1
      curCol = 1
      draw()
    elseif char == 8 or code == 259 then -- Backspace
      if curCol > 1 then
        lines[curLine] = lines[curLine]:sub(1, curCol - 2) .. lines[curLine]:sub(curCol)
        curCol = curCol - 1
        draw()
      elseif curLine > 1 then
        curCol = #lines[curLine - 1] + 1
        lines[curLine - 1] = lines[curLine - 1] .. lines[curLine]
        table.remove(lines, curLine)
        curLine = curLine - 1
        draw()
      end
    elseif char >= 32 and char < 127 then
      lines[curLine] = lines[curLine]:sub(1, curCol - 1) .. string.char(char) .. lines[curLine]:sub(curCol)
      curCol = curCol + 1
      draw()
    end
  end
end

-- Restore
term.clear()
""".trimIndent()

    // ================================================================
    // pastebin.lua - Download/upload from Pastebin
    // ================================================================
    val PASTEBIN_LUA = """
local fs = require("filesystem")
local term = require("term")
local args = {...}

local function resolve(p)
  if p:sub(1,1) ~= "/" then p = (os.getenv("CWD") or "/") .. "/" .. p end
  return fs.canonical(p)
end

local function httpGet(url)
  local inet = component.list("internet")()
  if not inet then
    return nil, "no internet card"
  end
  local handle = component.invoke(inet, "request", url)
  if not handle then return nil, "request failed" end
  
  -- Wait for connection
  local timeout = 0
  while true do
    local ok, err = handle.finishConnect()
    if ok then break end
    if ok == nil then return nil, err or "connection failed" end
    timeout = timeout + 1
    if timeout > 200 then return nil, "timeout" end
    computer.pullSignal(0.05)
  end
  
  local data = ""
  while true do
    local chunk = handle.read(math.huge)
    if not chunk then break end
    data = data .. chunk
  end
  handle.close()
  return data
end

if #args == 0 then
  print("Usage:")
  print("  pastebin get <code> <file>")
  print("  pastebin run <code> [args...]")
  print("  pastebin put <file>")
  return
end

local cmd = args[1]

if cmd == "get" then
  if #args < 3 then
    print("Usage: pastebin get <code> <file>")
    return
  end
  local code = args[2]
  local file = resolve(args[3])
  local url = "https://pastebin.com/raw/" .. code
  
  print("Downloading " .. url .. " ...")
  local data, err = httpGet(url)
  if data then
    -- Convert \r\n to \n
    data = data:gsub("\r\n", "\n")
    local f = fs.open(file, "w")
    if f then
      f:write(data)
      f:close()
      print("Saved to " .. file .. " (" .. #data .. " bytes)")
    else
      print("Error: cannot write to " .. file)
    end
  else
    print("Error: " .. (err or "download failed"))
  end

elseif cmd == "run" then
  if #args < 2 then
    print("Usage: pastebin run <code> [args...]")
    return
  end
  local code = args[2]
  local url = "https://pastebin.com/raw/" .. code
  
  print("Downloading " .. url .. " ...")
  local data, err = httpGet(url)
  if data then
    data = data:gsub("\r\n", "\n")
    print("Running (" .. #data .. " bytes)...")
    local fn, lerr = load(data, "=" .. code, "t", _G)
    if fn then
      local extraArgs = {}
      for i = 3, #args do extraArgs[#extraArgs+1] = args[i] end
      local ok, rerr = pcall(fn, table.unpack(extraArgs))
      if not ok then print("Error: " .. tostring(rerr)) end
    else
      print("Load error: " .. tostring(lerr))
    end
  else
    print("Error: " .. (err or "download failed"))
  end

elseif cmd == "put" then
  print("Upload not supported in OpenCoudou")
  
else
  print("Unknown command: " .. cmd)
  print("Usage: pastebin get|run|put ...")
end
""".trimIndent()

    // ================================================================
    // install.lua - Install from disk
    // ================================================================
    val INSTALL_LUA = """
local fs = require("filesystem")
local term = require("term")
local args = {...}

-- Find installable sources (filesystems with files)
local sources = {}
local targets = {}
local bootAddr = computer.getBootAddress()

for addr, ctype in component.list("filesystem") do
  local files = component.invoke(addr, "list", "/")
  local hasFiles = files and #files > 0
  if hasFiles then
    local ro = false
    pcall(function() ro = component.invoke(addr, "isReadOnly") end)
    local label = ""
    pcall(function() label = component.invoke(addr, "getLabel") or "" end)
    if #label == 0 then label = addr:sub(1, 8) .. "..." end
    
    if ro then
      sources[#sources+1] = {addr = addr, label = label, ro = true}
    else
      targets[#targets+1] = {addr = addr, label = label}
      if not ro then
        -- Also a potential source
        local hasInit = component.invoke(addr, "exists", "/init.lua") or false
        if hasInit then
          sources[#sources+1] = {addr = addr, label = label, ro = false}
        end
      end
    end
  end
end

if #sources == 0 then
  print("No installable source found.")
  print("Insert a floppy disk with an OS.")
  return
end

-- Select source
print("Select source to install from:")
for i, s in ipairs(sources) do
  local ro = s.ro and " [ro]" or " [rw]"
  print("  " .. i .. ") " .. s.label .. ro)
end

local srcIdx
if #sources == 1 then
  srcIdx = 1
  print(sources[1].label .. " selected for install")
else
  term.write("Enter number: ")
  local input = term.read()
  srcIdx = tonumber(input)
  if not srcIdx or srcIdx < 1 or srcIdx > #sources then
    print("Invalid selection.")
    return
  end
end

local source = sources[srcIdx]

-- Select target
if #targets == 0 then
  print("No writable filesystem found for installation target.")
  return
end

print("\nWhere do you want to install to?")
for i, t in ipairs(targets) do
  local ro = ""
  pcall(function() ro = component.invoke(t.addr, "isReadOnly") and " [ro]" or " [rw]" end)
  print("  " .. i .. ") " .. t.addr .. " at /mnt/" .. t.addr:sub(1, 3) .. ro)
end

local tgtIdx
if #targets == 1 then
  tgtIdx = 1
else
  term.write("Please enter a number between 1 and " .. #targets .. "\n")
  term.write("Enter 'q' to cancel the installation: ")
  local input = term.read()
  if input == "q" then return end
  tgtIdx = tonumber(input)
  if not tgtIdx or tgtIdx < 1 or tgtIdx > #targets then
    print("Invalid selection.")
    return
  end
end

local target = targets[tgtIdx]

print("\nInstall " .. source.label .. " to /mnt/" .. target.addr:sub(1, 3) .. "? [Y/n] ")
local confirm = term.read()
if confirm and confirm:lower():sub(1,1) == "n" then
  print("Cancelled.")
  return
end

-- Copy all files recursively
local function copyRecursive(srcAddr, dstAddr, path)
  local files = component.invoke(srcAddr, "list", path)
  if not files then return end
  for _, name in ipairs(files) do
    local fullPath = path .. name
    if name:sub(-1) == "/" then
      -- Directory
      component.invoke(dstAddr, "makeDirectory", fullPath)
      print("  " .. fullPath)
      copyRecursive(srcAddr, dstAddr, fullPath)
    else
      -- File
      local handle = component.invoke(srcAddr, "open", fullPath, "r")
      if handle then
        local data = ""
        repeat
          local chunk = component.invoke(srcAddr, "read", handle, math.huge)
          data = data .. (chunk or "")
        until not chunk
        component.invoke(srcAddr, "close", handle)
        
        local wh = component.invoke(dstAddr, "open", fullPath, "w")
        if wh then
          component.invoke(dstAddr, "write", wh, data)
          component.invoke(dstAddr, "close", wh)
          print("  " .. fullPath .. " (" .. #data .. " bytes)")
        end
      end
    end
  end
end

print("\nInstalling...")
copyRecursive(source.addr, target.addr, "/")
print("\nInstallation complete!")

-- Set boot address
term.write("Set as boot device? [Y/n] ")
local setBoot = term.read()
if not setBoot or setBoot:lower():sub(1,1) ~= "n" then
  computer.setBootAddress(target.addr)
  print("Boot address set to " .. target.addr)
end

term.write("Reboot now? [Y/n] ")
local reboot = term.read()
if not reboot or reboot:lower():sub(1,1) ~= "n" then
  computer.shutdown(true)
end
""".trimIndent()

    val REBOOT_LUA = """
computer.shutdown(true)
""".trimIndent()

    val CLEAR_LUA = """
require("term").clear()
""".trimIndent()

    val ECHO_LUA = """
local args = {...}
print(table.concat(args, " "))
""".trimIndent()

    val CD_LUA = """
local fs = require("filesystem")
local args = {...}
local path = args[1] or os.getenv("HOME") or "/"
if path:sub(1, 1) ~= "/" then
  path = (os.getenv("CWD") or "/") .. "/" .. path
end
path = fs.canonical(path)
if not fs.isDirectory(path) then
  io.stderr:write("cd: no such directory: " .. path .. "\n")
  return
end
os.setenv("CWD", path)
""".trimIndent()

    val WGET_LUA = """
local fs = require("filesystem")
local args = {...}

if #args < 2 then
  print("Usage: wget <url> <file>")
  return
end

local url = args[1]
local file = args[2]
if file:sub(1,1) ~= "/" then
  file = (os.getenv("CWD") or "/") .. "/" .. file
end
file = fs.canonical(file)

local inet = component.list("internet")()
if not inet then
  print("Error: no internet card")
  return
end

print("Downloading " .. url .. " ...")
local handle = component.invoke(inet, "request", url)
if not handle then
  print("Error: request failed")
  return
end

local timeout = 0
while true do
  local ok, err = handle.finishConnect()
  if ok then break end
  if ok == nil then print("Error: " .. (err or "failed")); return end
  timeout = timeout + 1
  if timeout > 200 then print("Error: timeout"); return end
  computer.pullSignal(0.05)
end

local data = ""
while true do
  local chunk = handle.read(math.huge)
  if not chunk then break end
  data = data .. chunk
end
handle.close()

data = data:gsub("\r\n", "\n")
local f = fs.open(file, "w")
if f then
  f:write(data)
  f:close()
  print("Saved to " .. file .. " (" .. #data .. " bytes)")
else
  print("Error: cannot write to " .. file)
end
""".trimIndent()

    // ================================================================
    // /etc/profile.lua
    // ================================================================
    val PROFILE_LUA = """
-- Profile
os.setenv("PS1", "/home # ")
local term = require("term")
local gpu = component.list("gpu")()
if gpu then
  component.invoke(gpu, "setForeground", 0x55FF55)
end
print(_OSVERSION or "OpenCoudou")
if gpu then
  component.invoke(gpu, "setForeground", 0xAAAAAA)
end
print("Note: Your home directory is readonly. Run 'install' and reboot.")
if gpu then
  component.invoke(gpu, "setForeground", 0xFFFFFF)
end
os.setenv("CWD", "/home")
""".trimIndent()

}
