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
        fs.writeFile("lib/keyboard.lua", KEYBOARD_LUA)
        fs.writeFile("lib/serialization.lua", SERIALIZATION_LUA)
        fs.writeFile("lib/sides.lua", SIDES_LUA)
        fs.writeFile("lib/colors.lua", COLORS_LUA)
        fs.writeFile("lib/uuid.lua", UUID_LUA)
        fs.writeFile("lib/buffer.lua", BUFFER_LUA)
        fs.writeFile("lib/process.lua", PROCESS_LUA)
        fs.writeFile("lib/thread.lua", THREAD_LUA)
        fs.writeFile("lib/rc.lua", RC_LUA)
        fs.writeFile("lib/devfs.lua", DEVFS_LUA)
        fs.writeFile("lib/note.lua", NOTE_LUA)
        fs.writeFile("lib/internet.lua", INTERNET_LUA)
        fs.writeFile("lib/transforms.lua", TRANSFORMS_LUA)
        fs.writeFile("lib/tty.lua", TTY_LUA)
        fs.writeFile("lib/sh.lua", SH_LIB_LUA)
        fs.writeFile("lib/vt100.lua", VT100_LUA)
        fs.writeFile("lib/pipe.lua", PIPE_LUA)
        fs.writeFile("lib/bit32.lua", BIT32_LUA)

        // ============ Boot scripts ============
        fs.writeFile("boot/00_base.lua", BOOT_00_BASE)
        fs.writeFile("boot/01_term.lua", BOOT_01_TERM)
        fs.writeFile("boot/02_fs.lua", BOOT_02_FS)
        fs.writeFile("boot/03_keyboard.lua", BOOT_03_KEYBOARD)
        fs.writeFile("boot/04_component.lua", BOOT_04_COMPONENT)
        fs.writeFile("boot/90_filesystem.lua", BOOT_90_FILESYSTEM)
        fs.writeFile("boot/91_gpu.lua", BOOT_91_GPU)
        fs.writeFile("boot/05_devfs.lua", BOOT_05_DEVFS)
        fs.writeFile("boot/89_rc.lua", BOOT_89_RC)

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
        fs.writeFile("bin/pwd.lua", PWD_LUA)
        fs.writeFile("bin/set.lua", SET_LUA)
        fs.writeFile("bin/which.lua", WHICH_LUA)
        fs.writeFile("bin/hostname.lua", HOSTNAME_LUA)
        fs.writeFile("bin/components.lua", COMPONENTS_LUA)
        fs.writeFile("bin/label.lua", LABEL_LUA)
        fs.writeFile("bin/free.lua", FREE_LUA)
        fs.writeFile("bin/df.lua", DF_LUA)
        fs.writeFile("bin/sleep.lua", SLEEP_LUA)
        fs.writeFile("bin/wget.lua", WGET_LUA)
        fs.writeFile("bin/lua.lua", LUA_LUA)
        fs.writeFile("bin/grep.lua", GREP_LUA)
        fs.writeFile("bin/more.lua", MORE_LUA)
        fs.writeFile("bin/dmesg.lua", DMESG_LUA)
        fs.writeFile("bin/mount.lua", MOUNT_LUA)
        fs.writeFile("bin/umount.lua", UMOUNT_LUA)
        fs.writeFile("bin/date.lua", DATE_LUA)
        fs.writeFile("bin/touch.lua", TOUCH_LUA)
        fs.writeFile("bin/alias.lua", ALIAS_LUA)
        fs.writeFile("bin/unalias.lua", UNALIAS_LUA)
        fs.writeFile("bin/head.lua", HEAD_LUA)
        fs.writeFile("bin/shutdown.lua", SHUTDOWN_LUA)
        fs.writeFile("bin/wc.lua", WC_LUA)
        fs.writeFile("bin/tail.lua", TAIL_LUA)
        fs.writeFile("bin/tee.lua", TEE_LUA)
        fs.writeFile("bin/rev.lua", REV_LUA)
        fs.writeFile("bin/type.lua", TYPE_LUA)
        fs.writeFile("bin/env.lua", ENV_LUA)
        fs.writeFile("bin/true.lua", TRUE_LUA)
        fs.writeFile("bin/false.lua", FALSE_LUA)
        fs.writeFile("bin/rc.lua", RC_CMD_LUA)
        fs.writeFile("bin/address.lua", ADDRESS_LUA)
        fs.writeFile("bin/resolution.lua", RESOLUTION_LUA)
        fs.writeFile("bin/flash.lua", FLASH_LUA)
        fs.writeFile("bin/primary.lua", PRIMARY_LUA)
        fs.writeFile("bin/uptime.lua", UPTIME_LUA)
        fs.writeFile("bin/redstone.lua", REDSTONE_LUA)
        fs.writeFile("bin/time.lua", TIME_LUA)
        fs.writeFile("bin/source.lua", SOURCE_LUA)
        fs.writeFile("bin/du.lua", DU_LUA)
        fs.writeFile("bin/find.lua", FIND_LUA)
        fs.writeFile("bin/less.lua", LESS_LUA)
        fs.writeFile("bin/list.lua", LIST_LUA)
        fs.writeFile("bin/ln.lua", LN_LUA)
        fs.writeFile("bin/lshw.lua", LSHW_LUA)
        fs.writeFile("bin/man.lua", MAN_LUA)
        fs.writeFile("bin/mktmp.lua", MKTMP_LUA)
        fs.writeFile("bin/ps.lua", PS_LUA)
        fs.writeFile("bin/rmdir.lua", RMDIR_LUA)
        fs.writeFile("bin/tree.lua", TREE_LUA)
        fs.writeFile("bin/unset.lua", UNSET_LUA)
        fs.writeFile("bin/useradd.lua", USERADD_LUA)
        fs.writeFile("bin/userdel.lua", USERDEL_LUA)
        fs.writeFile("bin/yes.lua", YES_LUA)

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
        if line then
          line = line:gsub("[\r\n]+${'$'}", "")
        end
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
  -- Check package.preload
  if _G.package and _G.package.preload and _G.package.preload[name] then
    loading[name] = true
    local result = _G.package.preload[name](name)
    loaded[name] = result or true
    loading[name] = nil
    return loaded[name]
  end
  loading[name] = true
  
  -- Build search paths
  local cwd = os.getenv and os.getenv("PWD") or "/"
  local paths = {
    "/lib/" .. name .. ".lua",
    "/lib/" .. name .. "/init.lua",
    "/usr/lib/" .. name .. ".lua",
    "/usr/lib/" .. name .. "/init.lua",
    "/home/lib/" .. name .. ".lua",
    "/home/lib/" .. name .. "/init.lua",
    cwd .. "/" .. name .. ".lua",
    cwd .. "/" .. name .. "/init.lua",
  }
  
  local addr = computer.getBootAddress()
  local tryLoad = function(path)
    -- Use filesystem lib if available
    local fs = loaded["filesystem"]
    if fs and fs.open then
      local f = fs.open(path, "r")
      if not f then return nil end
      local buffer = ""
      repeat
        local data = f:read(math.huge)
        buffer = buffer .. (data or "")
      until not data
      f:close()
      return load(buffer, "=" .. path, "t", _G)
    else
      -- Fallback during early boot
      local ok, handle = pcall(component.invoke, addr, "open", path)
      if not ok or not handle then return nil end
      local buffer = ""
      repeat
        local data = component.invoke(addr, "read", handle, math.huge)
        buffer = buffer .. (data or "")
      until not data
      component.invoke(addr, "close", handle)
      return load(buffer, "=" .. path, "t", _G)
    end
  end
  
  for _, path in ipairs(paths) do
    local fn = tryLoad(path)
    if fn then
      local result = fn()
      loaded[name] = result or true
      loading[name] = nil
      return loaded[name]
    end
  end
  
  loading[name] = nil
  error("module not found: " .. name)
end

_G.require = require

-- Package library
_G.package = {
  loaded = loaded,
  path = "/lib/?.lua;/lib/?/init.lua;/usr/lib/?.lua;/usr/lib/?/init.lua;/home/lib/?.lua;/home/lib/?/init.lua;./?.lua;./?/init.lua",
  preload = {},
  
  searchpath = function(name, path, sep, rep)
    sep = sep or "."
    rep = rep or "/"
    name = name:gsub("%" .. sep, rep)
    local errors = {}
    for pattern in path:gmatch("[^;]+") do
      local filepath = pattern:gsub("%?", name)
      local addr = computer.getBootAddress()
      local fs = loaded["filesystem"]
      local exists = false
      if fs and fs.exists then
        exists = fs.exists(filepath)
      else
        local ok, handle = pcall(component.invoke, addr, "open", filepath)
        if ok and handle then
          component.invoke(addr, "close", handle)
          exists = true
        end
      end
      if exists then
        return filepath
      end
      errors[#errors+1] = "\n\tno file '" .. filepath .. "'"
    end
    return nil, table.concat(errors)
  end,
}

-- Environment variables
local env = {
  HOME = "/home",
  PATH = "/bin:/usr/bin",
  PWD = "/home",
  PS1 = "/home # ",
  SHELL = "/bin/sh.lua",
  TMPDIR = "/tmp",
  TERM = "opencomputers",
}
os.getenv = function(k) return env[k] end
os.setenv = function(k, v) env[k] = v end

os.execute = function(cmd)
  if not cmd then return true end
  local shell = loaded["shell"] or require("shell")
  return shell.execute(cmd)
end

os.exit = function(code)
  error({reason = "terminated", code = code or 0})
end

os.remove = function(path)
  local fs = loaded["filesystem"] or require("filesystem")
  return fs.remove(path)
end

os.rename = function(from, to)
  local fs = loaded["filesystem"] or require("filesystem")
  return fs.rename(from, to)
end

local tmpCounter = 0
os.tmpname = function()
  tmpCounter = tmpCounter + 1
  return (env.TMPDIR or "/tmp") .. "/lua_" .. computer.address():sub(1,8) .. "_" .. tmpCounter
end

-- Basic print
_G.print = function(...)
  local args = table.pack(...)
  local ioLib = loaded["io"]
  if ioLib and ioLib.stdout then
    local pre = ""
    for i = 1, args.n do
      ioLib.stdout:write(pre .. tostring(args[i]))
      pre = "\t"
    end
    ioLib.stdout:write("\n")
    if ioLib.stdout.flush then ioLib.stdout:flush() end
  else
    local s = ""
    for i = 1, args.n do
      if i > 1 then s = s .. "\t" end
      s = s .. tostring(args[i])
    end
    local term = loaded["term"]
    if term and term.write then
      term.write(s .. "\n")
    else
      local gpu = component.list("gpu")()
      if gpu then
        component.invoke(gpu, "set", 1, y, s)
        y = y + 1
      end
    end
  end
end

_G.loadfile = function(path, mode, env)
  -- Resolve relative paths
  if path:sub(1, 1) ~= "/" then
    path = (os.getenv("PWD") or "/") .. "/" .. path
  end
  -- Canonicalize path
  local parts = {}
  for part in path:gmatch("[^/]+") do
    if part == ".." then
      if #parts > 0 then table.remove(parts) end
    elseif part ~= "." then
      parts[#parts+1] = part
    end
  end
  path = "/" .. table.concat(parts, "/")

  -- Try loading through filesystem if available
  local fs = loaded and loaded["filesystem"]
  if fs and fs.open then
    local f, err = fs.open(path, "r")
    if not f then return nil, "file not found: " .. path end
    local buffer = ""
    repeat
      local data = f:read(math.huge)
      buffer = buffer .. (data or "")
    until not data
    f:close()
    return load(buffer, "=" .. path, mode or "t", env or _G)
  end

  -- Fallback to direct component access (during early boot)
  local addr = computer.getBootAddress()
  local ok, handle = pcall(component.invoke, addr, "open", path)
  if not ok or not handle then return nil, "file not found: " .. path end
  local buffer = ""
  repeat
    local data = component.invoke(addr, "read", handle, math.huge)
    buffer = buffer .. (data or "")
  until not data
  component.invoke(addr, "close", handle)
  return load(buffer, "=" .. path, mode or "t", env or _G)
end

_G.dofile = function(path, ...)
  local fn, err = loadfile(path)
  if not fn then error(err) end
  return fn(...)
end

-- Wrap computer.shutdown to fire shutdown signal
do
  local shutdown = computer.shutdown
  _G.runlevel = "S"
  computer.runlevel = function() return _G.runlevel or 1 end
  computer.shutdown = function(reboot)
    _G.runlevel = reboot and 6 or 0
    if os.sleep then
      computer.pushSignal("shutdown")
      os.sleep(0.1)
    end
    shutdown(reboot)
  end
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

-- Fire component_added for all existing components
for addr, ctype in component.list() do
  computer.pushSignal("component_added", addr, ctype)
end
computer.pushSignal("init")
-- Process the init signal to trigger handlers
pcall(function()
  local event = require("event")
  event.pull(1, "init")
end)
_G.runlevel = 1

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
    
    -- Ctrl+C interrupt check
    if uptime() - lastInterrupt > 1 then
      local ok, kb = pcall(require, "keyboard")
      if ok and kb and kb.isControlDown and kb.isControlDown() and kb.isKeyDown and kb.isKeyDown(0x2E) then
        lastInterrupt = uptime()
        computer.pushSignal("interrupted", uptime())
      end
    end
    
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

local function segments(path)
  local parts = {}
  for part in path:gmatch("[^\\/]+") do
    local current, up = part:find("^%.?%.${'$'}")
    if current then
      if up == 2 then
        table.remove(parts)
      end
    else
      table.insert(parts, part)
    end
  end
  return parts
end

function filesystem.canonical(path)
  local result = table.concat(segments(path), "/")
  if path:sub(1, 1) == "/" then
    return "/" .. result
  else
    return result
  end
end

function filesystem.segments(path)
  return segments(path)
end

function filesystem.concat(...)
  local set = table.pack(...)
  for index, value in ipairs(set) do
    if type(value) ~= "string" then
      error("bad argument #" .. index .. " (string expected, got " .. type(value) .. ")")
    end
  end
  return filesystem.canonical(table.concat(set, "/"))
end

function filesystem.path(path)
  local parts = segments(path)
  local result = table.concat(parts, "/", 1, #parts - 1) .. "/"
  if path:sub(1, 1) == "/" and result:sub(1, 1) ~= "/" then
    return "/" .. result
  else
    return result
  end
end

function filesystem.name(path)
  local parts = segments(path)
  return parts[#parts]
end

function filesystem.mount(address, path)
  mounts[path] = address
  return true
end

function filesystem.umount(pathOrAddr)
  if mounts[pathOrAddr] then
    mounts[pathOrAddr] = nil
    return true
  end
  for path, addr in pairs(mounts) do
    if addr == pathOrAddr then
      mounts[path] = nil
      return true
    end
  end
  return false
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
  -- Check if source is a directory (filesystem.copy only works on files)
  if filesystem.isDirectory(from) then
    return false, "cannot copy a directory"
  end
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
    return component.proxy(bestAddr), best
  end
  return component.proxy(computer.getBootAddress()), "/"
end

function filesystem.proxy(filter)
  checkArg(1, filter, "string")
  for addr in component.list("filesystem") do
    if addr == filter or addr:sub(1, #filter) == filter then
      return component.proxy(addr)
    end
  end
  return nil, "no such filesystem"
end

function filesystem.isReadOnly(path)
  local addr = resolve(path)
  local ok, result = pcall(component.invoke, addr, "isReadOnly")
  return ok and result
end

function filesystem.lastModified(path)
  local addr, rel = resolve(path)
  local ok, result = pcall(component.invoke, addr, "lastModified", rel)
  return ok and result or 0
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

-- Symbolic link stubs (not fully supported in simplified OpenCoudou)
function filesystem.isLink(path)
  -- Always return false - no symlink support in simplified VFS
  return false
end

function filesystem.link(target, linkpath)
  -- Symlinks not supported in simplified VFS
  return nil, "symbolic links not supported"
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

function shell.getAlias(name)
  return aliases[name]
end

function shell.setAlias(name, value)
  aliases[name] = value
end

function shell.aliases()
  return pairs(aliases)
end

function shell.getPath()
  return os.getenv("PATH")
end

function shell.setPath(value)
  os.setenv("PATH", value)
end

function shell.parse(...)
  local params = table.pack(...)
  local args = {}
  local options = {}
  local doneWithOptions = false
  for i = 1, params.n do
    local param = params[i]
    if not doneWithOptions and type(param) == "string" then
      if param == "--" then
        doneWithOptions = true
      elseif param:sub(1, 2) == "--" then
        local key, value = param:match("%-%-(.-)=(.*)")
        if not key then
          key, value = param:sub(3), true
        end
        options[key] = value
      elseif param:sub(1, 1) == "-" and param ~= "-" then
        for j = 2, #param do
          options[param:sub(j, j)] = true
        end
      else
        table.insert(args, param)
      end
    else
      table.insert(args, param)
    end
  end
  return args, options
end

function shell.resolve(cmd, ext)
  if cmd:sub(1, 1) == "/" then
    if require("filesystem").exists(cmd) then return cmd end
    if ext and not cmd:match("%.%w+${'$'}") then
      local full = cmd .. (ext or ".lua")
      if require("filesystem").exists(full) then return full end
    end
    return cmd
  end
  
  -- Check relative to working directory first
  local fs = require("filesystem")
  local cwd = shell.getWorkingDirectory()
  local relPath = fs.concat(cwd, cmd)
  if fs.exists(relPath) then return relPath end
  if not cmd:match("%.%w+${'$'}") then
    local relLua = relPath .. ".lua"
    if fs.exists(relLua) then return relLua end
  end
  
  -- Check alias
  local alias = aliases[cmd]
  if alias then
    return shell.resolve(alias, ext)
  end
  
  -- Search PATH
  local path = os.getenv("PATH") or "/bin"
  for dir in path:gmatch("[^:]+") do
    local full = fs.concat(dir, cmd)
    if fs.exists(full) then return full end
    if not cmd:match("%.%w+${'$'}") then
      local fullLua = full .. ".lua"
      if fs.exists(fullLua) then return fullLua end
    end
  end
  
  return nil
end

-- Tokenize a line respecting quotes
local function tokenize(line)
  local tokens = {}
  local current = ""
  local inQuote = nil
  local hasQuote = false
  local i = 1
  while i <= #line do
    local c = line:sub(i, i)
    if inQuote then
      if c == inQuote then
        inQuote = nil
      elseif c == "\\" and i < #line then
        i = i + 1
        current = current .. line:sub(i, i)
      else
        current = current .. c
      end
    else
      if c == '"' or c == "'" then
        inQuote = c
        hasQuote = true
      elseif c == " " or c == "\t" then
        if #current > 0 or hasQuote then
          table.insert(tokens, current)
          current = ""
          hasQuote = false
        end
      elseif c == "\\" and i < #line then
        i = i + 1
        current = current .. line:sub(i, i)
      else
        current = current .. c
      end
    end
    i = i + 1
  end
  if #current > 0 or hasQuote then
    table.insert(tokens, current)
  end
  return tokens
end

function shell.execute(cmd, ...)
  local cmdArgs = {...}
  
  -- Check for output redirection in arguments
  local redirectFile = nil
  local redirectAppend = false
  local cleanArgs = {}
  local i = 1
  while i <= #cmdArgs do
    local a = tostring(cmdArgs[i])
    if a == ">>" then
      redirectAppend = true
      i = i + 1
      if i <= #cmdArgs then redirectFile = tostring(cmdArgs[i]) end
    elseif a == ">" then
      redirectAppend = false
      i = i + 1
      if i <= #cmdArgs then redirectFile = tostring(cmdArgs[i]) end
    elseif a:sub(1, 2) == ">>" then
      redirectAppend = true
      redirectFile = a:sub(3)
    elseif a:sub(1, 1) == ">" then
      redirectAppend = false
      redirectFile = a:sub(2)
    else
      cleanArgs[#cleanArgs + 1] = cmdArgs[i]
    end
    i = i + 1
  end
  
  local path = shell.resolve(cmd)
  if not path then
    return false, cmd .. ": command not found"
  end
  local env = setmetatable({}, {__index = _G})
  env._G = env
  local fn, err = loadfile(path, "t", env)
  if not fn then return false, err end
  
  if redirectFile then
    -- Resolve redirect path
    local fs = require("filesystem")
    if redirectFile:sub(1, 1) ~= "/" then
      redirectFile = fs.concat(shell.getWorkingDirectory(), redirectFile)
    end
    redirectFile = fs.canonical(redirectFile)
    
    -- Capture output by temporarily replacing print, io.write, term.write
    local captured = {}
    local oldPrint = _G.print
    local term = require("term")
    local oldTermWrite = term.write
    local ioLib = require("io")
    local oldIoWrite = ioLib and ioLib.write
    
    local function capture(text)
      captured[#captured + 1] = tostring(text)
    end
    
    _G.print = function(...)
      local args = table.pack(...)
      local s = ""
      for j = 1, args.n do
        if j > 1 then s = s .. "\t" end
        s = s .. tostring(args[j])
      end
      capture(s .. "\n")
    end
    term.write = function(text) capture(text) end
    if ioLib then ioLib.write = function(text) capture(text) end end
    
    local ok, err2 = pcall(fn, table.unpack(cleanArgs))
    
    -- Restore
    _G.print = oldPrint
    term.write = oldTermWrite
    if ioLib and oldIoWrite then ioLib.write = oldIoWrite end
    
    -- Write captured output to file
    local mode = redirectAppend and "a" or "w"
    local f = fs.open(redirectFile, mode)
    if f then
      f:write(table.concat(captured))
      f:close()
    end
    
    -- Handle os.exit() terminated errors
    if not ok and type(err2) == "table" and err2.reason == "terminated" then
      return true, nil, err2.code or 0
    end
    return ok, err2
  end
  
  -- Run without redirection
  local ok, err2 = pcall(fn, table.unpack(cleanArgs))
  
  -- Handle os.exit() terminated errors
  if not ok and type(err2) == "table" and err2.reason == "terminated" then
    return true, nil, err2.code or 0
  end
  return ok, err2
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
      local cwd = os.getenv("PWD") or "/home"
      local prompt = cwd .. " # "
      term.write(prompt)
      local line = term.read(shellHistory)
      if line == nil then
        -- Ctrl+D: exit shell
        return
      end
      if line then
        -- Trim trailing newline/whitespace
        line = line:gsub("[\r\n]+${'$'}", "")
        line = line:match("^%s*(.-)%s*${'$'}") or ""
        if #line > 0 then
          if line:match("^%s*exit%s*${'$'}") then return end
          
          -- Split by pipes (respecting quotes)
          local pipeSegments = {}
          local pipeCur = ""
          local pipeQ = nil
          for ci = 1, #line do
            local ch = line:sub(ci, ci)
            if pipeQ then
              if ch == pipeQ then pipeQ = nil end
              pipeCur = pipeCur .. ch
            elseif ch == '"' or ch == "'" then
              pipeQ = ch
              pipeCur = pipeCur .. ch
            elseif ch == "|" then
              pipeSegments[#pipeSegments+1] = pipeCur
              pipeCur = ""
            else
              pipeCur = pipeCur .. ch
            end
          end
          pipeSegments[#pipeSegments+1] = pipeCur
          
          if #pipeSegments > 1 then
            -- Pipeline execution
            local pipeData = nil
            for pi, segment in ipairs(pipeSegments) do
              segment = segment:match("^%s*(.-)%s*${'$'}") or ""
              if #segment == 0 then break end
              local segParts = tokenize(segment)
              if #segParts == 0 then break end
              local segCmd = segParts[1]
              local isLast = (pi == #pipeSegments)
              
              -- Set up stdin from previous pipe
              local oldStdin = nil
              local ioLib = require("io")
              if pipeData and pi > 1 then
                oldStdin = ioLib.stdin
                local pStr = pipeData
                local pPos = 1
                local function pipeRead(self, mode)
                  mode = mode or "*l"
                  if mode == "*a" or mode == "a" then
                    if pPos > #pStr then return nil end
                    local rest = pStr:sub(pPos)
                    pPos = #pStr + 1
                    return rest
                  end
                  if pPos > #pStr then return nil end
                  local nl = pStr:find("\n", pPos, true)
                  if nl then
                    local ln = pStr:sub(pPos, nl - 1)
                    pPos = nl + 1
                    return ln
                  end
                  local ln = pStr:sub(pPos)
                  pPos = #pStr + 1
                  return #ln > 0 and ln or nil
                end
                ioLib.stdin = {
                  read = pipeRead,
                  close = function() end,
                  flush = function() end,
                  write = function() return nil, "not writable" end,
                  seek = function() return nil, "not seekable" end,
                  setvbuf = function() end,
                  lines = function(self)
                    return function() return pipeRead(self, "*l") end
                  end,
                }
              end
              
              if isLast then
                shell.execute(segCmd, table.unpack(segParts, 2))
              else
                local captured = {}
                local oldPrint = _G.print
                local termLib = require("term")
                local oldTermWrite = termLib.write
                local oldIoWrite = ioLib.write
                _G.print = function(...)
                  local pa = table.pack(...)
                  local s = ""
                  for j = 1, pa.n do
                    if j > 1 then s = s .. "\t" end
                    s = s .. tostring(pa[j])
                  end
                  captured[#captured+1] = s .. "\n"
                end
                termLib.write = function(text) captured[#captured+1] = tostring(text) end
                ioLib.write = function(text) captured[#captured+1] = tostring(text) end
                shell.execute(segCmd, table.unpack(segParts, 2))
                _G.print = oldPrint
                termLib.write = oldTermWrite
                ioLib.write = oldIoWrite
                pipeData = table.concat(captured)
              end
              if oldStdin then ioLib.stdin = oldStdin end
            end
          else
            -- Single command
            local parts = tokenize(line)
            local cmd = parts[1]
            if cmd == "exit" then return end
            local ok, err = shell.execute(cmd, table.unpack(parts, 2))
            if not ok then
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
end

function shell.getWorkingDirectory()
  return os.getenv("PWD") or "/home"
end

function shell.setWorkingDirectory(dir)
  os.setenv("PWD", dir)
end

-- Get info about currently running program
local currentProgram = nil
function shell.running(level)
  return currentProgram
end

function shell.setRunning(path)
  currentProgram = path
end

-- List available programs in PATH
function shell.programs(includeHidden)
  local result = {}
  local seen = {}
  local fs = require("filesystem")
  local path = os.getenv("PATH") or "/bin"
  for dir in path:gmatch("[^:]+") do
    if fs.isDirectory(dir) then
      for file in fs.list(dir) do
        local name = file:gsub("%.lua${'$'}", "")
        if not seen[name] then
          if includeHidden or not name:match("^%.") then
            result[#result + 1] = name
            seen[name] = true
          end
        end
      end
    end
  end
  table.sort(result)
  return result
end

-- Save aliases to file
function shell.saveAliases()
  local fs = require("filesystem")
  local path = "/home/.aliases"
  local f = fs.open(path, "w")
  if f then
    for k, v in pairs(aliases) do
      f:write(k .. "=" .. v .. "\n")
    end
    f:close()
    return true
  end
  return false
end

-- Load aliases from file
function shell.loadAliases()
  local fs = require("filesystem")
  local path = "/home/.aliases"
  if not fs.exists(path) then return end
  local f = fs.open(path, "r")
  if f then
    local content = ""
    repeat
      local data = f:read(math.huge)
      content = content .. (data or "")
    until not data
    f:close()
    for line in content:gmatch("[^\r\n]+") do
      local k, v = line:match("^([^=]+)=(.+)${'$'}")
      if k and v then
        aliases[k] = v
      end
    end
  end
end

return shell
""".trimIndent()

    // ================================================================
    // /lib/term.lua - Terminal library with VT100/ANSI support
    // ================================================================
    val TERM_LUA = """
local term = {}

local cursorX = 1
local cursorY = 1
local gpuAddr = nil
local screenW = 80
local screenH = 25
local currentFg = 0xFFFFFF
local currentBg = 0x000000

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

-- ANSI color code to RGB mapping
local ansiColors = {
  [0] = 0x000000,  -- black
  [1] = 0xCC0000,  -- red
  [2] = 0x00CC00,  -- green
  [3] = 0xCCCC00,  -- yellow
  [4] = 0x0000CC,  -- blue
  [5] = 0xCC00CC,  -- magenta
  [6] = 0x00CCCC,  -- cyan
  [7] = 0xCCCCCC,  -- white
  -- Bright colors (90-97 / 100-107)
  [8] = 0x555555,  -- bright black (gray)
  [9] = 0xFF5555,  -- bright red
  [10] = 0x55FF55, -- bright green
  [11] = 0xFFFF55, -- bright yellow
  [12] = 0x5555FF, -- bright blue
  [13] = 0xFF55FF, -- bright magenta
  [14] = 0x55FFFF, -- bright cyan
  [15] = 0xFFFFFF, -- bright white
}

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

-- Handle ANSI/VT100 escape sequences
local function handleEscape(seq, gpu)
  -- CSI sequences: ESC [ <params> <command>
  -- Common sequences:
  -- ESC[nA - cursor up n
  -- ESC[nB - cursor down n
  -- ESC[nC - cursor forward n
  -- ESC[nD - cursor back n
  -- ESC[n;mH - cursor position (row;col)
  -- ESC[2J - clear screen
  -- ESC[K - clear to end of line
  -- ESC[nm - SGR (colors)
  
  if seq:sub(1, 1) ~= "[" then return end
  local body = seq:sub(2)
  local cmd = body:sub(-1)
  local params = body:sub(1, -2)
  
  if cmd == "A" then
    local n = tonumber(params) or 1
    cursorY = math.max(1, cursorY - n)
  elseif cmd == "B" then
    local n = tonumber(params) or 1
    cursorY = math.min(screenH, cursorY + n)
  elseif cmd == "C" then
    local n = tonumber(params) or 1
    cursorX = math.min(screenW, cursorX + n)
  elseif cmd == "D" then
    local n = tonumber(params) or 1
    cursorX = math.max(1, cursorX - n)
  elseif cmd == "H" or cmd == "f" then
    local row, col = params:match("(%d*);?(%d*)")
    cursorY = tonumber(row) or 1
    cursorX = tonumber(col) or 1
    cursorY = math.max(1, math.min(screenH, cursorY))
    cursorX = math.max(1, math.min(screenW, cursorX))
  elseif cmd == "J" then
    local n = tonumber(params) or 0
    if n == 2 then
      component.invoke(gpu, "fill", 1, 1, screenW, screenH, " ")
      cursorX = 1
      cursorY = 1
    elseif n == 0 then
      -- Clear from cursor to end of screen
      component.invoke(gpu, "fill", cursorX, cursorY, screenW - cursorX + 1, 1, " ")
      if cursorY < screenH then
        component.invoke(gpu, "fill", 1, cursorY + 1, screenW, screenH - cursorY, " ")
      end
    elseif n == 1 then
      -- Clear from start to cursor
      component.invoke(gpu, "fill", 1, 1, screenW, cursorY - 1, " ")
      component.invoke(gpu, "fill", 1, cursorY, cursorX, 1, " ")
    end
  elseif cmd == "K" then
    local n = tonumber(params) or 0
    if n == 0 then
      component.invoke(gpu, "fill", cursorX, cursorY, screenW - cursorX + 1, 1, " ")
    elseif n == 1 then
      component.invoke(gpu, "fill", 1, cursorY, cursorX, 1, " ")
    elseif n == 2 then
      component.invoke(gpu, "fill", 1, cursorY, screenW, 1, " ")
    end
  elseif cmd == "m" then
    -- SGR - Select Graphic Rendition (colors)
    local codes = {}
    for code in (params .. ";"):gmatch("(%d*);") do
      codes[#codes + 1] = tonumber(code) or 0
    end
    if #codes == 0 then codes = {0} end
    
    for _, code in ipairs(codes) do
      if code == 0 then
        currentFg = 0xFFFFFF
        currentBg = 0x000000
        component.invoke(gpu, "setForeground", currentFg)
        component.invoke(gpu, "setBackground", currentBg)
      elseif code >= 30 and code <= 37 then
        currentFg = ansiColors[code - 30]
        component.invoke(gpu, "setForeground", currentFg)
      elseif code >= 40 and code <= 47 then
        currentBg = ansiColors[code - 40]
        component.invoke(gpu, "setBackground", currentBg)
      elseif code >= 90 and code <= 97 then
        currentFg = ansiColors[code - 90 + 8]
        component.invoke(gpu, "setForeground", currentFg)
      elseif code >= 100 and code <= 107 then
        currentBg = ansiColors[code - 100 + 8]
        component.invoke(gpu, "setBackground", currentBg)
      elseif code == 39 then
        currentFg = 0xFFFFFF
        component.invoke(gpu, "setForeground", currentFg)
      elseif code == 49 then
        currentBg = 0x000000
        component.invoke(gpu, "setBackground", currentBg)
      end
    end
  elseif cmd == "s" then
    -- Save cursor position (not fully implemented, simple store)
    term._savedX = cursorX
    term._savedY = cursorY
  elseif cmd == "u" then
    -- Restore cursor position
    cursorX = term._savedX or 1
    cursorY = term._savedY or 1
  end
end

function term.write(text)
  local gpu = getGpu()
  if not gpu then return end
  refreshSize()
  
  local i = 1
  while i <= #text do
    local ch = text:sub(i, i)
    
    -- Check for escape sequence
    if ch == "\27" then
      local escEnd = text:find("[A-Za-z]", i + 1)
      if escEnd then
        local seq = text:sub(i + 1, escEnd)
        handleEscape(seq, gpu)
        i = escEnd + 1
      else
        i = i + 1
      end
    elseif ch == "\n" then
      cursorX = 1
      cursorY = cursorY + 1
      if cursorY > screenH then scroll() end
      i = i + 1
    elseif ch == "\r" then
      cursorX = 1
      i = i + 1
    elseif ch == "\b" then
      if cursorX > 1 then
        cursorX = cursorX - 1
      end
      i = i + 1
    elseif ch == "\t" then
      cursorX = cursorX + (4 - ((cursorX - 1) % 4))
      if cursorX > screenW then
        cursorX = 1
        cursorY = cursorY + 1
        if cursorY > screenH then scroll() end
      end
      i = i + 1
    else
      if cursorX > screenW then
        cursorX = 1
        cursorY = cursorY + 1
        if cursorY > screenH then scroll() end
      end
      component.invoke(gpu, "set", cursorX, cursorY, ch)
      cursorX = cursorX + 1
      i = i + 1
    end
  end
end

function term.read(history, dobreak, hint, pwchar)
  local gpu = getGpu()
  if not gpu then return "" end
  refreshSize()
  
  if dobreak == nil then dobreak = true end
  history = history or {}
  local histIdx = #history + 1
  local buffer = ""
  local pos = 0 -- cursor position within buffer (0 = before first char)
  local startX = cursorX
  local startY = cursorY
  
  local scrollOffset = 0
  local function redraw()
    local maxVisible = screenW - startX + 1
    if #buffer <= maxVisible then
      scrollOffset = 0
    elseif pos >= scrollOffset + maxVisible then
      scrollOffset = pos - maxVisible + 1
    elseif pos < scrollOffset then
      scrollOffset = pos
    end
    component.invoke(gpu, "fill", startX, startY, maxVisible, 1, " ")
    local displayBuf = pwchar and string.rep(pwchar, #buffer) or buffer
    local visible = displayBuf:sub(scrollOffset + 1, scrollOffset + maxVisible)
    if #visible > 0 then
      component.invoke(gpu, "set", startX, startY, visible)
    end
    cursorX = startX + pos - scrollOffset
  end
  
  while true do
    redraw()
    -- Show cursor
    local cursorScreenPos = startX + pos - scrollOffset
    local displayBuf = pwchar and string.rep(pwchar, #buffer) or buffer
    local cursorChar = pos < #displayBuf and displayBuf:sub(pos + 1, pos + 1) or "_"
    if cursorScreenPos >= 1 and cursorScreenPos <= screenW then
      component.invoke(gpu, "set", cursorScreenPos, startY, cursorChar)
    end
    
    local sig, _, char, code = computer.pullSignal(0.5)
    
    if sig == "key_down" then
      if char == 13 or code == 28 then -- Enter
        redraw()
        if dobreak then
          cursorX = 1
          cursorY = cursorY + 1
          if cursorY > screenH then scroll() end
        end
        if #buffer > 0 then
          history[#history + 1] = buffer
        end
        return buffer
      elseif char == 4 then -- Ctrl+D
        if #buffer == 0 then return nil end
      elseif char == 9 and hint then -- Tab completion
        local hints = hint(buffer, pos)
        if hints and #hints > 0 then
          local completion = hints[1]
          if type(completion) == "string" then
            buffer = buffer:sub(1, pos) .. completion .. buffer:sub(pos + 1)
            pos = pos + #completion
          end
        end
      elseif char == 8 or code == 14 then -- Backspace
        if pos > 0 then
          buffer = buffer:sub(1, pos - 1) .. buffer:sub(pos + 1)
          pos = pos - 1
        end
      elseif code == 211 then -- Delete
        if pos < #buffer then
          buffer = buffer:sub(1, pos) .. buffer:sub(pos + 2)
        end
      elseif code == 203 then -- Left
        if pos > 0 then pos = pos - 1 end
      elseif code == 205 then -- Right
        if pos < #buffer then pos = pos + 1 end
      elseif code == 199 then -- Home
        pos = 0
      elseif code == 207 then -- End
        pos = #buffer
      elseif code == 200 then -- Up (history)
        if histIdx > 1 then
          histIdx = histIdx - 1
          buffer = history[histIdx] or ""
          pos = #buffer
        end
      elseif code == 208 then -- Down (history)
        if histIdx <= #history then
          histIdx = histIdx + 1
          buffer = history[histIdx] or ""
          pos = #buffer
        end
      elseif char >= 32 and char < 127 then
        buffer = buffer:sub(1, pos) .. string.char(char) .. buffer:sub(pos + 1)
        pos = pos + 1
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

function term.scroll(lines)
  lines = lines or 1
  local gpu = getGpu()
  if not gpu then return end
  refreshSize()
  if lines > 0 then
    local n = math.min(lines, screenH)
    component.invoke(gpu, "copy", 1, 1 + n, screenW, screenH - n, 0, -n)
    component.invoke(gpu, "fill", 1, screenH - n + 1, screenW, n, " ")
  elseif lines < 0 then
    local n = math.min(-lines, screenH)
    component.invoke(gpu, "copy", 1, 1, screenW, screenH - n, 0, n)
    component.invoke(gpu, "fill", 1, 1, screenW, n, " ")
  end
end

function term.clearLine()
  local gpu = getGpu()
  if not gpu then return end
  refreshSize()
  component.invoke(gpu, "fill", 1, cursorY, screenW, 1, " ")
  cursorX = 1
end

function term.getGlobalArea()
  refreshSize()
  return 1, 1, screenW, screenH
end

function term.pull(...)
  return require("event").pull(...)
end

function term.setCursorBlink(enabled)
  -- Cursor blink is handled in term.read, this is a no-op for compatibility
end

function term.getCursorBlink()
  return true
end

function term.gpu()
  local addr = getGpu()
  if addr then return component.proxy(addr) end
  return nil
end

function term.screen()
  local gpu = getGpu()
  if not gpu then return nil end
  local ok, screen = pcall(component.invoke, gpu, "getScreen")
  return ok and screen or nil
end

function term.setForeground(color)
  local gpu = getGpu()
  if not gpu then return end
  currentFg = color
  component.invoke(gpu, "setForeground", color)
end

function term.setBackground(color)
  local gpu = getGpu()
  if not gpu then return end
  currentBg = color
  component.invoke(gpu, "setBackground", color)
end

function term.getForeground()
  return currentFg
end

function term.getBackground()
  return currentBg
end

return term
""".trimIndent()

    // ================================================================
    // /lib/text.lua - Text utilities
    // ================================================================
    val TEXT_LUA = """
local text = {}

function text.trim(s)
  checkArg(1, s, "string")
  local from = s:match("^%s*()")
  return from > #s and "" or s:match(".*%S", from)
end

function text.tokenize(s, delimiters)
  checkArg(1, s, "string")
  delimiters = delimiters or "%s"
  local tokens = {}
  for token in s:gmatch("[^" .. delimiters .. "]+") do
    tokens[#tokens+1] = token
  end
  return tokens
end

function text.padRight(s, len, char)
  checkArg(1, s, "string")
  checkArg(2, len, "number")
  char = char or " "
  return s .. string.rep(char, math.max(0, len - #s))
end

function text.padLeft(s, len, char)
  checkArg(1, s, "string")
  checkArg(2, len, "number")
  char = char or " "
  return string.rep(char, math.max(0, len - #s)) .. s
end

function text.wrap(value, width, maxWidth)
  checkArg(1, value, "string")
  checkArg(2, width, "number")
  checkArg(3, maxWidth, "number", "nil")
  maxWidth = maxWidth or width
  
  local lines = {}
  local line = ""
  
  -- Split by existing newlines first
  for segment in (value .. "\n"):gmatch("(.-)\n") do
    -- Wrap within segment
    for word in segment:gmatch("%S+") do
      if #line + #word + 1 > width then
        if #line > 0 then
          lines[#lines+1] = line
        end
        -- Handle words longer than width
        while #word > maxWidth do
          lines[#lines+1] = word:sub(1, maxWidth)
          word = word:sub(maxWidth + 1)
        end
        line = word
      elseif #line > 0 then
        line = line .. " " .. word
      else
        line = word
      end
    end
    if #line > 0 or #segment == 0 then
      lines[#lines+1] = line
      line = ""
    end
  end
  
  return lines
end

function text.detab(value, tabWidth)
  checkArg(1, value, "string")
  tabWidth = tabWidth or 8
  local result = ""
  local col = 0
  for i = 1, #value do
    local c = value:sub(i, i)
    if c == "\t" then
      local spaces = tabWidth - (col % tabWidth)
      result = result .. string.rep(" ", spaces)
      col = col + spaces
    elseif c == "\n" then
      result = result .. c
      col = 0
    else
      result = result .. c
      col = col + 1
    end
  end
  return result
end

-- Escape magic pattern characters
function text.escapeMagic(txt)
  return txt:gsub('[%(%)%.%%%+%-%*%?%[%^%$]', '%%%1')
end

function text.removeEscapes(txt)
  return txt:gsub("%%([%(%)%.%%%+%-%*%?%[%^%$])","%1")
end

-- Split a string by a separator
function text.split(s, sep)
  checkArg(1, s, "string")
  sep = sep or "%s"
  local parts = {}
  local pattern = string.format("([^%s]+)", sep)
  for part in s:gmatch(pattern) do
    parts[#parts+1] = part
  end
  return parts
end

-- Join an array with separator
function text.join(arr, sep)
  checkArg(1, arr, "table")
  sep = sep or " "
  return table.concat(arr, sep)
end

-- Check if string starts with prefix
function text.startsWith(s, prefix)
  return s:sub(1, #prefix) == prefix
end

-- Check if string ends with suffix
function text.endsWith(s, suffix)
  return suffix == "" or s:sub(-#suffix) == suffix
end

return text
""".trimIndent()

    // ================================================================
    // /lib/io.lua - I/O library
    // ================================================================
    val IO_LUA = """
local io = _G.io or {}
local fs = require("filesystem")

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
function stdout:lines() return function() return nil end end
function stdout:read() return nil end
function stdout:seek() return nil, "not seekable" end
function stdout:setvbuf() end

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
function stderr:flush() end
function stderr:lines() return function() return nil end end
function stderr:read() return nil end
function stderr:seek() return nil, "not seekable" end
function stderr:setvbuf() end

local stdin = {}
function stdin:read(mode)
  local term = require("term")
  if mode == "*l" or mode == "l" or mode == nil then
    return term.read()
  elseif mode == "*L" or mode == "L" then
    local line = term.read()
    if line then return line .. "\n" end
    return nil
  elseif mode == "*a" or mode == "a" then
    -- Read all lines until Ctrl+D (nil return)
    local chunks = {}
    while true do
      local line = term.read()
      if not line then break end
      chunks[#chunks+1] = line
      chunks[#chunks+1] = "\n"
    end
    return #chunks > 0 and table.concat(chunks) or nil
  elseif mode == "*n" or mode == "n" then
    local s = term.read()
    return tonumber(s)
  elseif type(mode) == "number" then
    -- Read exactly N characters (reads one line, returns up to N chars)
    local s = term.read()
    if s then return s:sub(1, mode) end
    return nil
  end
  return term.read()
end
function stdin:close() end
function stdin:flush() end
function stdin:lines()
  return function()
    return self:read("*l")
  end
end
function stdin:write() return nil, "not writable" end
function stdin:seek() return nil, "not seekable" end
function stdin:setvbuf() end

io.stdout = stdout
io.stderr = stderr
io.stdin = stdin

-- Wrap a raw filesystem handle into a proper io file handle
local function wrapHandle(rawHandle, mode)
  local handle = {}
  handle._raw = rawHandle
  handle._closed = false
  handle._buf = ""

  local function fillBuf(self)
    local data = self._raw:read(4096)
    if data then
      self._buf = self._buf .. data
      return true
    end
    return false
  end

  function handle:read(...)
    if self._closed then return nil, "closed file" end
    local args = table.pack(...)
    if args.n == 0 then args = {"*l"}; args.n = 1 end
    
    local function readOne(fmt)
      fmt = fmt or "*l"
      if fmt == "*a" or fmt == "a" then
        local chunks = {}
        if #self._buf > 0 then
          chunks[#chunks+1] = self._buf
          self._buf = ""
        end
        while true do
          local data = self._raw:read(4096)
          if not data then break end
          chunks[#chunks+1] = data
        end
        return #chunks > 0 and table.concat(chunks) or nil
      elseif fmt == "*l" or fmt == "l" then
        while true do
          local nl = self._buf:find("\n")
          if nl then
            local line = self._buf:sub(1, nl - 1)
            -- Strip \r if present (CRLF)
            if line:sub(-1) == "\r" then line = line:sub(1, -2) end
            self._buf = self._buf:sub(nl + 1)
            return line
          end
          if not fillBuf(self) then
            if #self._buf > 0 then
              local line = self._buf
              self._buf = ""
              return line
            end
            return nil
          end
        end
      elseif fmt == "*L" or fmt == "L" then
        -- Like *l but keeps the newline
        while true do
          local nl = self._buf:find("\n")
          if nl then
            local line = self._buf:sub(1, nl)
            self._buf = self._buf:sub(nl + 1)
            return line
          end
          if not fillBuf(self) then
            if #self._buf > 0 then
              local line = self._buf
              self._buf = ""
              return line
            end
            return nil
          end
        end
      elseif fmt == "*n" or fmt == "n" then
        while #self._buf == 0 do
          if not fillBuf(self) then return nil end
        end
        local s, e = self._buf:find("^%s*[%-]?%d+%.?%d*[eE]?[%-+]?%d*")
        if not s then return nil end
        local numStr = self._buf:sub(s, e)
        self._buf = self._buf:sub(e + 1)
        return tonumber(numStr)
      elseif type(fmt) == "number" then
        while #self._buf < fmt do
          if not fillBuf(self) then break end
        end
        if #self._buf == 0 then return nil end
        local n = math.min(fmt, #self._buf)
        local data = self._buf:sub(1, n)
        self._buf = self._buf:sub(n + 1)
        return data
      end
      return self._raw:read(4096)
    end
    
    -- Support multiple format arguments
    local results = {}
    for i = 1, args.n do
      local val = readOne(args[i])
      if val == nil and i == 1 then return nil end
      results[i] = val
    end
    return table.unpack(results, 1, args.n)
  end

  function handle:write(...)
    if self._closed then return nil, "closed file" end
    for _, v in ipairs({...}) do
      self._raw:write(tostring(v))
    end
    return self
  end

  function handle:close()
    if self._closed then return end
    self._closed = true
    return self._raw:close()
  end

  function handle:flush()
    return self
  end

  function handle:seek(whence, offset)
    if self._closed then return nil, "closed file" end
    return self._raw:seek(whence or "cur", offset or 0)
  end

  function handle:lines(...)
    local args = table.pack(...)
    if args.n == 0 then args = {"*l"}; args.n = 1 end
    return function()
      local result = table.pack(self:read(table.unpack(args, 1, args.n)))
      if not result[1] then return nil end
      return table.unpack(result, 1, result.n)
    end
  end

  function handle:setvbuf() end

  return handle
end

function io.write(...)
  return stdout:write(...)
end

function io.read(...)
  return io.stdin:read(...)
end

function io.open(path, mode)
  mode = mode or "r"
  -- Resolve relative paths
  if path:sub(1, 1) ~= "/" then
    local shell = require("shell")
    path = fs.concat(shell.getWorkingDirectory(), path)
  end
  local rawHandle, err = fs.open(path, mode)
  if not rawHandle then return nil, err end
  return wrapHandle(rawHandle, mode)
end

function io.close(file)
  if file then
    return file:close()
  end
  return nil, "no file to close"
end

function io.flush()
  return stdout:flush()
end

function io.lines(filename, ...)
  if filename then
    local f, err = io.open(filename, "r")
    if not f then error(err, 2) end
    local args = table.pack(...)
    if args.n == 0 then args = {"*l"}; args.n = 1 end
    return function()
      local result = table.pack(f:read(table.unpack(args, 1, args.n)))
      if not result[1] then
        if result[2] then error(result[2], 2) end
        f:close()
        return nil
      end
      return table.unpack(result, 1, result.n)
    end
  else
    return io.stdin:lines()
  end
end

function io.type(obj)
  if type(obj) ~= "table" then return nil end
  if obj == stdout or obj == stderr or obj == stdin then return "file" end
  if obj._raw then
    if obj._closed then return "closed file" end
    return "file"
  end
  return nil
end

function io.input(file)
  if file then
    if type(file) == "string" then
      io.stdin = io.open(file, "r")
    else
      io.stdin = file
    end
  end
  return io.stdin
end

function io.output(file)
  if file then
    if type(file) == "string" then
      io.stdout = io.open(file, "w")
    else
      io.stdout = file
    end
  end
  return io.stdout
end

function io.tmpfile()
  return io.open("/tmp/.tmpfile_" .. math.floor(computer.uptime() * 1000), "a")
end

function io.popen() return nil, "not supported" end

function io.error(file)
  if file then
    if type(file) == "string" then
      io.stderr = io.open(file, "w")
    else
      io.stderr = file
    end
  end
  return io.stderr
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
  seconds = seconds or 0
  local deadline = computer.uptime() + seconds
  local event = package and package.loaded and package.loaded["event"]
  if event and event.pull then
    repeat
      event.pull(math.max(0, deadline - computer.uptime()))
    until computer.uptime() >= deadline
  else
    repeat
      local remaining = math.max(0, deadline - computer.uptime())
      local signal = table.pack(computer.pullSignal(remaining))
      if signal.n > 0 then
        computer.pushSignal(table.unpack(signal, 1, signal.n))
      end
    until computer.uptime() >= deadline
  end
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

os.setenv("PWD", "/home")
""".trimIndent()

    val BOOT_03_KEYBOARD = """
-- Track keyboard pressed state
local keyboard = require("keyboard")
local event = require("event")

event.listen("key_down", function(_, _, char, code)
  if type(char) == "number" and char > 0 then
    keyboard.pressedChars[char] = true
  end
  if type(code) == "number" then
    keyboard.pressedCodes[code] = true
  end
end)

event.listen("key_up", function(_, _, char, code)
  if type(char) == "number" then
    keyboard.pressedChars[char] = nil
  end
  if type(code) == "number" then
    keyboard.pressedCodes[code] = nil
  end
end)
""".trimIndent()

    val BOOT_04_COMPONENT = """
-- Set up component shorthand: component.gpu, component.modem, etc.
local primaries = {}

setmetatable(component, {
  __index = function(_, key)
    if primaries[key] then
      return primaries[key]
    end
    local addr = component.list(key)()
    if addr then
      primaries[key] = component.proxy(addr)
      return primaries[key]
    end
    return nil
  end
})

-- Listen for component changes to invalidate cache
local event = require("event")
event.listen("component_added", function(_, addr, ctype)
  primaries[ctype] = nil
end)
event.listen("component_removed", function(_, addr, ctype)
  primaries[ctype] = nil
end)
""".trimIndent()

    val BOOT_90_FILESYSTEM = """
-- Hot-plug filesystem auto-mount/unmount
local fs = require("filesystem")
local event = require("event")

event.listen("component_added", function(_, addr, ctype)
  if ctype == "filesystem" then
    local label = ""
    pcall(function() label = component.invoke(addr, "getLabel") or "" end)
    local mountPoint = "/mnt/" .. addr:sub(1, 3)
    pcall(fs.mount, addr, mountPoint)
    -- Run autorun if present
    pcall(function()
      if component.invoke(addr, "exists", "autorun.lua") then
        dofile(mountPoint .. "/autorun.lua")
      end
    end)
  end
end)

event.listen("component_removed", function(_, addr, ctype)
  if ctype == "filesystem" then
    pcall(fs.umount, addr)
  end
end)
""".trimIndent()

    val BOOT_91_GPU = """
-- GPU auto-bind on component changes
local event = require("event")

local function onComponentAvailable(_, componentType)
  if (componentType == "screen" and component.list("gpu")()) or
     (componentType == "gpu" and component.list("screen")())
  then
    local gpu = component.list("gpu")()
    local screen = component.list("screen")()
    if gpu and screen then
      local currentScreen = component.invoke(gpu, "getScreen")
      if currentScreen ~= screen then
        component.invoke(gpu, "bind", screen)
      end
      local depth = math.floor(2^(component.invoke(gpu, "getDepth")))
      os.setenv("TERM", "term-" .. depth .. "color")
    end
  end
end

event.listen("component_available", onComponentAvailable)
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
local shell = require("shell")
local term = require("term")
local args, options = shell.parse(...)
local path = args[1] or shell.getWorkingDirectory()
if path:sub(1,1) ~= "/" then
  path = fs.concat(shell.getWorkingDirectory(), path)
end
path = fs.canonical(path)

if not fs.exists(path) then
  io.stderr:write("ls: cannot access '" .. path .. "': No such file or directory\n")
  return 1
end

if not fs.isDirectory(path) then
  if options.l then
    local size = fs.size(path) or 0
    local modified = fs.lastModified(path) or 0
    local dateStr = tostring(modified)
    pcall(function() dateStr = os.date("%Y-%m-%d %H:%M", math.floor(modified / 1000)) end)
    print(string.format("%10d %s %s", size, dateStr, path))
  else
    print(path)
  end
  return
end

local gpu = component.list("gpu")()
local dirs = {}
local files = {}
for name in fs.list(path) do
  if name:sub(-1) == "/" then
    dirs[#dirs+1] = name
  else
    files[#files+1] = name
  end
end
table.sort(dirs)
table.sort(files)
local entries = {}
for _, d in ipairs(dirs) do entries[#entries+1] = d end
for _, f in ipairs(files) do entries[#entries+1] = f end

for _, name in ipairs(entries) do
  if not options.a and name:sub(1,1) == "." then
    -- skip hidden files
  else
    if options.l then
      local fullPath = fs.concat(path, name)
      local size = 0
      if name:sub(-1) ~= "/" then
        size = fs.size(fullPath) or 0
      end
      local modified = fs.lastModified(fullPath) or 0
      local dateStr = tostring(modified)
      pcall(function() dateStr = os.date("%Y-%m-%d %H:%M", math.floor(modified / 1000)) end)
      local typeChar = name:sub(-1) == "/" and "d" or "-"
      if name:sub(-1) == "/" then
        if gpu then component.invoke(gpu, "setForeground", 0x5555FF) end
        io.write(string.format("%s %10d %s %s", typeChar, size, dateStr, name))
        if gpu then component.invoke(gpu, "setForeground", 0xFFFFFF) end
        io.write("\n")
      else
        print(string.format("%s %10d %s %s", typeChar, size, dateStr, name))
      end
    else
      if name:sub(-1) == "/" then
        if gpu then component.invoke(gpu, "setForeground", 0x5555FF) end
        term.write(name)
        if gpu then component.invoke(gpu, "setForeground", 0xFFFFFF) end
        term.write("\n")
      else
        term.write(name .. "\n")
      end
    end
  end
end
""".trimIndent()

    val CAT_LUA = """
local fs = require("filesystem")
local shell = require("shell")
local args, options = shell.parse(...)
local exitCode = 0
if #args == 0 then
  -- Read from stdin
  local lineNum = 0
  while true do
    local line = io.read("*l")
    if not line then break end
    lineNum = lineNum + 1
    if options.n then
      io.write(string.format("%6d\t", lineNum))
    end
    print(line)
  end
  return 0
end
for _, file in ipairs(args) do
  local path = file
  if path:sub(1,1) ~= "/" then
    path = fs.concat(shell.getWorkingDirectory(), path)
  end
  path = fs.canonical(path)
  local f, err = fs.open(path, "r")
  if not f then
    io.stderr:write("cat: " .. file .. ": " .. (err or "No such file or directory") .. "\n")
    exitCode = 1
  else
    if options.n then
      local content = ""
      while true do
        local data = f:read(4096)
        if not data then break end
        content = content .. data
      end
      f:close()
      local lineNum = 0
      for line in content:gmatch("[^\n]+") do
        lineNum = lineNum + 1
        io.write(string.format("%6d\t", lineNum))
        print(line)
      end
    else
      while true do
        local data = f:read(4096)
        if not data then break end
        io.write(data)
      end
      f:close()
    end
  end
end
return exitCode
""".trimIndent()

    val CP_LUA = """
local fs = require("filesystem")
local shell = require("shell")
local args, options = shell.parse(...)
if #args < 2 then
  io.stderr:write("Usage: cp [-r] <source> <target>\n")
  return 1
end
local function resolve(p)
  if p:sub(1,1) ~= "/" then p = fs.concat(shell.getWorkingDirectory(), p) end
  return fs.canonical(p)
end

local function copyRecursive(src, dst)
  if fs.isDirectory(src) then
    if not fs.exists(dst) then
      fs.makeDirectory(dst)
    end
    for file in fs.list(src) do
      local srcPath = fs.concat(src, file)
      local dstPath = fs.concat(dst, file)
      local ok, err = copyRecursive(srcPath, dstPath)
      if not ok then return nil, err end
    end
    return true
  else
    return fs.copy(src, dst)
  end
end

local src = resolve(args[1])
local dst = resolve(args[2])

if not fs.exists(src) then
  io.stderr:write("cp: cannot stat '" .. args[1] .. "': No such file or directory\n")
  return 1
end

if fs.isDirectory(src) then
  if not options.r and not options.R then
    io.stderr:write("cp: -r not specified; omitting directory '" .. args[1] .. "'\n")
    return 1
  end
  local ok, err = copyRecursive(src, dst)
  if not ok then
    io.stderr:write("cp: " .. (err or "failed to copy") .. "\n")
    return 1
  end
else
  local ok, err = fs.copy(src, dst)
  if not ok then
    io.stderr:write("cp: " .. (err or "failed to copy") .. "\n")
    return 1
  end
end
return 0
""".trimIndent()

    val MV_LUA = """
local fs = require("filesystem")
local args = {...}
if #args < 2 then
  io.stderr:write("Usage: mv <source> <target>\n")
  return 1
end
local function resolve(p)
  if p:sub(1,1) ~= "/" then p = require("filesystem").concat(require("shell").getWorkingDirectory(), p) end
  return fs.canonical(p)
end
local src = resolve(args[1])
local dst = resolve(args[2])
if not fs.exists(src) then
  io.stderr:write("mv: cannot stat '" .. args[1] .. "': No such file or directory\n")
  return 1
end
local ok, err = fs.rename(src, dst)
if not ok then
  -- Try copy+delete
  ok, err = fs.copy(src, dst)
  if ok then fs.remove(src) end
end
if not ok then
  io.stderr:write("mv: " .. (err or "failed to move") .. "\n")
  return 1
end
return 0
""".trimIndent()

    val RM_LUA = """
local fs = require("filesystem")
local shell = require("shell")
local args, options = shell.parse(...)
if #args == 0 then
  io.stderr:write("Usage: rm [-rf] <path> [...]\n")
  return 1
end
local function resolve(p)
  if p:sub(1,1) ~= "/" then p = fs.concat(shell.getWorkingDirectory(), p) end
  return fs.canonical(p)
end
local exitCode = 0
for _, p in ipairs(args) do
  local path = resolve(p)
  if not fs.exists(path) then
    if not options.f then
      io.stderr:write("rm: cannot remove '" .. p .. "': No such file or directory\n")
      exitCode = 1
    end
  elseif not fs.remove(path) then
    io.stderr:write("rm: cannot remove '" .. p .. "': Permission denied\n")
    exitCode = 1
  end
end
return exitCode
""".trimIndent()

    val MKDIR_LUA = """
local fs = require("filesystem")
local shell = require("shell")
local args, options = shell.parse(...)
if #args == 0 then
  io.stderr:write("Usage: mkdir [-p] <path> [...]\n")
  return 1
end
local function resolve(p)
  if p:sub(1,1) ~= "/" then p = fs.concat(shell.getWorkingDirectory(), p) end
  return fs.canonical(p)
end
local exitCode = 0
for _, p in ipairs(args) do
  local path = resolve(p)
  if fs.exists(path) then
    if not options.p then
      io.stderr:write("mkdir: cannot create directory '" .. p .. "': File exists\n")
      exitCode = 1
    end
  elseif not fs.makeDirectory(path) then
    io.stderr:write("mkdir: cannot create directory '" .. p .. "': Permission denied\n")
    exitCode = 1
  end
end
return exitCode
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
  if p:sub(1,1) ~= "/" then p = require("filesystem").concat(require("shell").getWorkingDirectory(), p) end
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
  component.invoke(gpu, "set", 1, 1, " EDIT: " .. path .. " | Ctrl+S save | Ctrl+W/Q quit")
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
    if code == 16 and char == 17 then -- Ctrl+Q (KEY_Q=16, ctrl char=17)
      break
    elseif code == 17 and char == 23 then -- Ctrl+W (KEY_W=17, ctrl char=23)
      break
    elseif code == 31 and char == 19 then -- Ctrl+S
      local f, saveErr = fs.open(path, "w")
      if f then
        f:write(table.concat(lines, "\n"))
        f:close()
        component.invoke(gpu, "setBackground", 0x222222)
        component.invoke(gpu, "fill", 1, h, w, 1, " ")
        component.invoke(gpu, "set", 1, h, " Saved!                    ")
        component.invoke(gpu, "setBackground", 0x000000)
        computer.pullSignal(0.5)
      else
        component.invoke(gpu, "setBackground", 0x222222)
        component.invoke(gpu, "fill", 1, h, w, 1, " ")
        component.invoke(gpu, "setForeground", 0xFF5555)
        component.invoke(gpu, "set", 1, h, " Save failed: " .. tostring(saveErr or "unknown error"))
        component.invoke(gpu, "setForeground", 0xFFFFFF)
        component.invoke(gpu, "setBackground", 0x000000)
        computer.pullSignal(1.5)
      end
    elseif char == 13 or code == 28 then -- Enter
      local rest = lines[curLine]:sub(curCol)
      lines[curLine] = lines[curLine]:sub(1, curCol - 1)
      table.insert(lines, curLine + 1, rest)
      curLine = curLine + 1
      curCol = 1
      if curLine > scrollY + h - 2 then scrollY = curLine - (h - 2) end
      draw()
    elseif char == 8 or code == 14 then -- Backspace
      if curCol > 1 then
        lines[curLine] = lines[curLine]:sub(1, curCol - 2) .. lines[curLine]:sub(curCol)
        curCol = curCol - 1
        draw()
      elseif curLine > 1 then
        curCol = #lines[curLine - 1] + 1
        lines[curLine - 1] = lines[curLine - 1] .. lines[curLine]
        table.remove(lines, curLine)
        curLine = curLine - 1
        if curLine - 1 < scrollY then scrollY = curLine - 1 end
        draw()
      end
    elseif code == 203 then -- Left
      if curCol > 1 then curCol = curCol - 1; draw() end
    elseif code == 205 then -- Right
      if curCol <= #lines[curLine] then curCol = curCol + 1; draw() end
    elseif code == 200 then -- Up
      if curLine > 1 then
        curLine = curLine - 1
        curCol = math.min(curCol, #lines[curLine] + 1)
        if curLine - 1 < scrollY then scrollY = curLine - 1 end
        draw()
      end
    elseif code == 208 then -- Down
      if curLine < #lines then
        curLine = curLine + 1
        curCol = math.min(curCol, #lines[curLine] + 1)
        if curLine > scrollY + h - 2 then scrollY = curLine - (h - 2) end
        draw()
      end
    elseif code == 199 then -- Home
      curCol = 1; draw()
    elseif code == 207 then -- End
      curCol = #lines[curLine] + 1; draw()
    elseif code == 211 then -- Delete
      if curCol <= #lines[curLine] then
        lines[curLine] = lines[curLine]:sub(1, curCol - 1) .. lines[curLine]:sub(curCol + 1)
        draw()
      elseif curLine < #lines then
        lines[curLine] = lines[curLine] .. lines[curLine + 1]
        table.remove(lines, curLine + 1)
        draw()
      end
    elseif code == 201 then -- Page Up
      curLine = math.max(1, curLine - (h - 2))
      curCol = math.min(curCol, #lines[curLine] + 1)
      scrollY = math.max(0, curLine - 1)
      draw()
    elseif code == 209 then -- Page Down
      curLine = math.min(#lines, curLine + (h - 2))
      curCol = math.min(curCol, #lines[curLine] + 1)
      scrollY = math.max(0, curLine - (h - 2))
      if scrollY > #lines - (h - 2) then scrollY = math.max(0, #lines - (h - 2)) end
      draw()
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
  if p:sub(1,1) ~= "/" then p = require("filesystem").concat(require("shell").getWorkingDirectory(), p) end
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
        local wh = component.invoke(dstAddr, "open", fullPath, "w")
        if wh then
          local total = 0
          while true do
            local chunk = component.invoke(srcAddr, "read", handle, 4096)
            if not chunk then break end
            component.invoke(dstAddr, "write", wh, chunk)
            total = total + #chunk
          end
          component.invoke(dstAddr, "close", wh)
          print("  " .. fullPath .. " (" .. total .. " bytes)")
        end
        component.invoke(srcAddr, "close", handle)
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
local shell = require("shell")
local args = {...}
local path = args[1] or os.getenv("HOME") or "/"
if path:sub(1, 1) ~= "/" then
  path = fs.concat(shell.getWorkingDirectory(), path)
end
path = fs.canonical(path)
if not fs.exists(path) then
  io.stderr:write("cd: no such file or directory: " .. (args[1] or "") .. "\n")
  return 1
end
if not fs.isDirectory(path) then
  io.stderr:write("cd: not a directory: " .. (args[1] or "") .. "\n")
  return 1
end
os.setenv("PWD", path)
os.setenv("PS1", path .. " # ")
return 0
""".trimIndent()

    val WGET_LUA = """
local fs = require("filesystem")
local args = {...}

if #args < 2 then
  io.stderr:write("Usage: wget <url> <file>\n")
  return 1
end

local url = args[1]
local file = args[2]
if file:sub(1,1) ~= "/" then
  file = require("filesystem").concat(require("shell").getWorkingDirectory(), file)
end
file = require("filesystem").canonical(file)

local inet = component.list("internet")()
if not inet then
  io.stderr:write("wget: no internet card\n")
  return 1
end

io.write("Downloading " .. url .. " ...\n")
local handle = component.invoke(inet, "request", url)
if not handle then
  io.stderr:write("wget: request failed\n")
  return 1
end

local timeout = 0
while true do
  local ok, err = handle.finishConnect()
  if ok then break end
  if ok == nil then
    io.stderr:write("wget: " .. (err or "connection failed") .. "\n")
    return 1
  end
  timeout = timeout + 1
  if timeout > 200 then
    io.stderr:write("wget: connection timeout\n")
    return 1
  end
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
  return 0
else
  io.stderr:write("wget: cannot write to " .. file .. "\n")
  return 1
end
""".trimIndent()

    // ================================================================
    // /bin/lua.lua - Interactive Lua REPL
    // ================================================================
    val LUA_LUA = """
local term = require("term")
local serialization = require("serialization")
local history = {}

local function serialize(val)
  if type(val) == "table" then
    local ok, result = pcall(serialization.serialize, val, true)
    if ok then return result end
  end
  return tostring(val)
end

-- Create environment with auto-require access
local env = setmetatable({}, {
  __index = function(self, key)
    local v = _G[key]
    if v ~= nil then return v end
    -- Try to auto-require the module
    local ok, lib = pcall(require, key)
    if ok then
      rawset(self, key, lib)
      return lib
    end
    return nil
  end
})

print("Lua 5.3 interpreter. Type 'exit' to quit.")
while true do
  io.write("lua> ")
  local line = term.read(history)
  if not line then break end
  line = line:match("^%s*(.-)%s*${'$'}") or ""
  if line == "exit" or line == "quit" then break end
  if #line > 0 then
    -- Support =expression shortcut
    local evalLine = line
    if line:sub(1, 1) == "=" then
      evalLine = "return " .. line:sub(2)
    end
    -- Try as expression first
    local fn, err = load("return " .. evalLine, "=stdin", "t", env)
    if not fn then
      fn, err = load(evalLine, "=stdin", "t", env)
    end
    if fn then
      local results = table.pack(xpcall(fn, debug.traceback))
      if results[1] then
        if results.n > 1 then
          local parts = {}
          for i = 2, results.n do
            parts[#parts+1] = serialize(results[i])
          end
          print(table.concat(parts, "\t"))
        end
      else
        io.stderr:write(tostring(results[2]) .. "\n")
      end
    else
      io.stderr:write(tostring(err) .. "\n")
    end
  end
end
""".trimIndent()

    // ================================================================
    // /bin/grep.lua - Text search in files
    // ================================================================
    val GREP_LUA = """
local fs = require("filesystem")
local shell = require("shell")
local args, options = shell.parse(...)
if #args < 1 then
  io.stderr:write("Usage: grep [-invc] <pattern> [file...]\n")
  return 2
end
local pattern = args[1]
local files = {}
for i = 2, #args do files[#files+1] = args[i] end

local function testMatch(line, pat)
  local testLine = line
  local testPat = pat
  if options.i then
    testLine = line:lower()
    testPat = pat:lower()
  end
  local found = testLine:find(testPat)
  if options.v then found = not found end
  return found
end

local totalMatches = 0
local hasErrors = false

-- If no files given, read from stdin
if #files == 0 then
  local count = 0
  local lineNum = 0
  while true do
    local line = io.read("*l")
    if not line then break end
    lineNum = lineNum + 1
    if testMatch(line, pattern) then
      count = count + 1
      totalMatches = totalMatches + 1
      if not options.c then
        if options.n then
          io.write(tostring(lineNum) .. ":")
        end
        print(line)
      end
    end
  end
  if options.c then print(count) end
  return totalMatches > 0 and 0 or 1
end

local showFilename = #files > 1
for _, file in ipairs(files) do
  local path = file
  if path:sub(1,1) ~= "/" then
    path = fs.concat(shell.getWorkingDirectory(), path)
  end
  local f = io.open(path, "r")
  if not f then
    io.stderr:write("grep: " .. file .. ": No such file or directory\n")
    hasErrors = true
  else
    local lineNum = 0
    local count = 0
    for line in f:lines() do
      lineNum = lineNum + 1
      if testMatch(line, pattern) then
        count = count + 1
        totalMatches = totalMatches + 1
        if not options.c then
          if showFilename then
            io.write(file .. ":")
          end
          if options.n then
            io.write(tostring(lineNum) .. ":")
          end
          print(line)
        end
      end
    end
    if options.c then
      if showFilename then io.write(file .. ":") end
      print(count)
    end
    f:close()
  end
end
if hasErrors then return 2 end
return totalMatches > 0 and 0 or 1
""".trimIndent()

    // ================================================================
    // /bin/more.lua - Pager
    // ================================================================
    val MORE_LUA = """
local fs = require("filesystem")
local shell = require("shell")
local term = require("term")
local args = {...}
local gpu = component.list("gpu")()
local _, screenH = 25, 25
if gpu then
  _, screenH = component.invoke(gpu, "getResolution")
end
local pageSize = screenH - 1

local function showPage(lines, start)
  for i = start, math.min(start + pageSize - 1, #lines) do
    print(lines[i])
  end
end

local allLines = {}
if #args > 0 then
  local path = args[1]
  if path:sub(1,1) ~= "/" then
    path = fs.concat(shell.getWorkingDirectory(), path)
  end
  local f = io.open(path, "r")
  if not f then
    io.stderr:write("more: " .. args[1] .. ": No such file or directory\n")
    return 1
  end
  for line in f:lines() do
    allLines[#allLines+1] = line
  end
  f:close()
else
  -- Read from stdin
  while true do
    local line = io.read("*l")
    if not line then break end
    allLines[#allLines+1] = line
  end
end

if #allLines <= pageSize then
  for _, line in ipairs(allLines) do print(line) end
  return
end

local pos = 1
while pos <= #allLines do
  showPage(allLines, pos)
  pos = pos + pageSize
  if pos <= #allLines then
    io.write("-- More (" .. math.floor(pos / #allLines * 100) .. "%) --")
    local event = require("event")
    while true do
      local _, _, _, code = event.pull("key_down")
      if code == 28 or code == 57 then break end  -- Enter or Space
      if code == 16 then return end  -- Q
    end
    print()
  end
end
""".trimIndent()

    // ================================================================
    // /etc/profile.lua
    // ================================================================
    val DMESG_LUA = """
local event = require("event")
local args = {...}

local function dmesg()
  print("Press Ctrl+C to exit")
  while true do
    local data = table.pack(event.pull())
    if data[1] then
      local parts = {}
      for i = 1, data.n do
        parts[#parts+1] = tostring(data[i])
      end
      print("[" .. string.format("%.2f", computer.uptime()) .. "] " .. table.concat(parts, " "))
    end
  end
end

local ok, err = pcall(dmesg)
if not ok and err ~= "interrupted" then
  error(err)
end
""".trimIndent()

    val MOUNT_LUA = """
local fs = require("filesystem")
local args = {...}
if #args == 0 then
  for path, addr in fs.mounts() do
    print(addr:sub(1, 8) .. "... on " .. path)
  end
elseif #args == 2 then
  local addr, path = args[1], args[2]
  for a in component.list("filesystem") do
    if a:sub(1, #addr) == addr then
      fs.mount(a, path)
      print("Mounted " .. a:sub(1, 8) .. "... on " .. path)
      return
    end
  end
  print("No filesystem found matching: " .. addr)
else
  print("Usage: mount [address path]")
end
""".trimIndent()

    val UMOUNT_LUA = """
local fs = require("filesystem")
local args = {...}
if #args ~= 1 then
  print("Usage: umount <path>")
  return
end
if fs.umount(args[1]) then
  print("Unmounted " .. args[1])
else
  print("Not mounted: " .. args[1])
end
""".trimIndent()

    val DATE_LUA = """
local time = os.date and os.date() or os.time and tostring(os.time()) or "unknown"
if computer.realTime then
  local t = computer.realTime()
  local secs = math.floor(t)
  local date = os.date and os.date("%Y-%m-%d %H:%M:%S", secs)
  if date then
    print(date)
  else
    print("Epoch: " .. secs)
  end
else
  print(time)
end
""".trimIndent()

    val TOUCH_LUA = """
local fs = require("filesystem")
local shell = require("shell")
local args = {...}
if #args == 0 then
  print("Usage: touch <file> [...]")
  return
end
for _, path in ipairs(args) do
  if path:sub(1, 1) ~= "/" then
    path = fs.concat(shell.getWorkingDirectory(), path)
  end
  if not fs.exists(path) then
    local f = fs.open(path, "w")
    if f then f:close() end
  end
end
""".trimIndent()

    val ALIAS_LUA = """
local shell = require("shell")
local args = {...}
if #args == 0 then
  for k, v in shell.aliases() do
    print(k .. "=" .. v)
  end
elseif #args == 1 then
  local v = shell.getAlias(args[1])
  if v then print(args[1] .. "=" .. v) else print("No alias: " .. args[1]) end
elseif #args >= 2 then
  shell.setAlias(args[1], args[2])
  -- Persist aliases to disk
  shell.saveAliases()
end
""".trimIndent()

    val UNALIAS_LUA = """
local shell = require("shell")
local args = {...}
if #args == 0 then
  print("Usage: unalias <name>")
  return
end
shell.setAlias(args[1], nil)
-- Persist aliases to disk
shell.saveAliases()
""".trimIndent()

    val HEAD_LUA = """
local fs = require("filesystem")
local shell = require("shell")
local args = {...}
local n = 10
local files = {}
local i = 1
while i <= #args do
  local a = args[i]
  local num = a:match("^%-(%d+)$")
  if num then
    n = tonumber(num) or 10
  elseif a == "-n" and i < #args then
    i = i + 1
    n = tonumber(args[i]) or 10
  else
    files[#files+1] = a
  end
  i = i + 1
end
if #files == 0 then
  -- Read from stdin
  local count = 0
  while count < n do
    local line = io.read("*l")
    if not line then break end
    print(line)
    count = count + 1
  end
  return 0
end
local exitCode = 0
for idx, file in ipairs(files) do
  local path = file
  if path:sub(1,1) ~= "/" then
    path = fs.concat(shell.getWorkingDirectory(), path)
  end
  local f = io.open(path, "r")
  if not f then
    io.stderr:write("head: " .. file .. ": No such file or directory\n")
    exitCode = 1
  else
    if #files > 1 then
      if idx > 1 then io.write("\n") end
      io.write("==> " .. file .. " <==")
      io.write("\n")
    end
    local count = 0
    for line in f:lines() do
      print(line)
      count = count + 1
      if count >= n then break end
    end
    f:close()
  end
end
return exitCode
""".trimIndent()

    val SHUTDOWN_LUA = """
computer.shutdown(false)
""".trimIndent()

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
os.setenv("PWD", "/home")

-- Load user aliases
local shell = require("shell")
if shell.loadAliases then
  pcall(shell.loadAliases)
end
""".trimIndent()

    // ================================================================
    // /lib/keyboard.lua - Keyboard constants
    // ================================================================
    val KEYBOARD_LUA = """
local keyboard = {pressedChars = {}, pressedCodes = {}}

keyboard.keys = {
  a               = 0x1E,
  b               = 0x30,
  c               = 0x2E,
  d               = 0x20,
  e               = 0x12,
  f               = 0x21,
  g               = 0x22,
  h               = 0x23,
  i               = 0x17,
  j               = 0x24,
  k               = 0x25,
  l               = 0x26,
  m               = 0x32,
  n               = 0x31,
  o               = 0x18,
  p               = 0x19,
  q               = 0x10,
  r               = 0x13,
  s               = 0x1F,
  t               = 0x14,
  u               = 0x16,
  v               = 0x2F,
  w               = 0x11,
  x               = 0x2D,
  y               = 0x15,
  z               = 0x2C,
  ["1"]           = 0x02,
  ["2"]           = 0x03,
  ["3"]           = 0x04,
  ["4"]           = 0x05,
  ["5"]           = 0x06,
  ["6"]           = 0x07,
  ["7"]           = 0x08,
  ["8"]           = 0x09,
  ["9"]           = 0x0A,
  ["0"]           = 0x0B,
  escape          = 0x01,
  back            = 0x0E,
  tab             = 0x0F,
  enter           = 0x1C,
  lcontrol        = 0x1D,
  lshift          = 0x2A,
  rshift          = 0x36,
  lmenu           = 0x38,
  space           = 0x39,
  capital         = 0x3A,
  up              = 0xC8,
  down            = 0xD0,
  left            = 0xCB,
  right           = 0xCD,
  home            = 0xC7,
  ["end"]         = 0xCF,
  pageUp          = 0xC9,
  pageDown        = 0xD1,
  insert          = 0xD2,
  delete          = 0xD3,
  rcontrol        = 0x9D,
  rmenu           = 0xB8,
  numpadenter     = 0x9C,
  minus           = 0x0C,
  equals          = 0x0D,
  lbracket        = 0x1A,
  rbracket        = 0x1B,
  semicolon       = 0x27,
  apostrophe      = 0x28,
  grave           = 0x29,
  backslash       = 0x2B,
  comma           = 0x33,
  period          = 0x34,
  slash           = 0x35,
  numlock         = 0x45,
  scroll          = 0x46,
  numpad0         = 0x52,
  numpad1         = 0x4F,
  numpad2         = 0x50,
  numpad3         = 0x51,
  numpad4         = 0x4B,
  numpad5         = 0x4C,
  numpad6         = 0x4D,
  numpad7         = 0x47,
  numpad8         = 0x48,
  numpad9         = 0x49,
  numpadsub       = 0x4A,
  numpadadd       = 0x4E,
  numpaddecimal   = 0x53,
  numpadmul       = 0x37,
  numpaddiv       = 0xB5,
  F1              = 0x3B,
  F2              = 0x3C,
  F3              = 0x3D,
  F4              = 0x3E,
  F5              = 0x3F,
  F6              = 0x40,
  F7              = 0x41,
  F8              = 0x42,
  F9              = 0x43,
  F10             = 0x44,
  F11             = 0x57,
  F12             = 0x58,
}

function keyboard.isAltDown()
  return keyboard.pressedCodes[keyboard.keys.lmenu] or keyboard.pressedCodes[keyboard.keys.rmenu]
end

function keyboard.isControl(char)
  return type(char) == "number" and (char < 0x20 or (char >= 0x7F and char <= 0x9F))
end

function keyboard.isControlDown()
  return keyboard.pressedCodes[keyboard.keys.lcontrol] or keyboard.pressedCodes[keyboard.keys.rcontrol]
end

function keyboard.isKeyDown(charOrCode)
  checkArg(1, charOrCode, "string", "number")
  if type(charOrCode) == "string" then
    return keyboard.pressedChars[charOrCode:byte()]
  elseif type(charOrCode) == "number" then
    return keyboard.pressedCodes[charOrCode]
  end
end

function keyboard.isShiftDown()
  return keyboard.pressedCodes[keyboard.keys.lshift] or keyboard.pressedCodes[keyboard.keys.rshift]
end

return keyboard
""".trimIndent()

    // ================================================================
    // /lib/serialization.lua
    // ================================================================
    val SERIALIZATION_LUA = """
local serialization = {}

function serialization.serialize(value, pretty)
  local seen = {}
  local function ser(v, indent)
    local t = type(v)
    if t == "nil" then return "nil"
    elseif t == "boolean" then return tostring(v)
    elseif t == "number" then return tostring(v)
    elseif t == "string" then return string.format("%q", v)
    elseif t == "table" then
      if seen[v] then return "{...}" end
      seen[v] = true
      local parts = {}
      local indent2 = (indent or "") .. "  "
      local isArray = #v > 0
      if isArray then
        for i = 1, #v do
          parts[#parts + 1] = ser(v[i], indent2)
        end
      end
      for k, val in pairs(v) do
        if not (isArray and type(k) == "number" and k >= 1 and k <= #v) then
          local key
          if type(k) == "string" and k:match("^[%a_][%w_]*${'$'}") then
            key = k
          else
            key = "[" .. ser(k, indent2) .. "]"
          end
          parts[#parts + 1] = key .. "=" .. ser(val, indent2)
        end
      end
      if pretty then
        return "{\n" .. indent2 .. table.concat(parts, ",\n" .. indent2) .. "\n" .. (indent or "") .. "}"
      else
        return "{" .. table.concat(parts, ",") .. "}"
      end
    else
      return tostring(v)
    end
  end
  return ser(value, "")
end

function serialization.unserialize(str)
  local fn, err = load("return " .. str, "=unserialize", "t", {})
  if fn then
    local ok, result = pcall(fn)
    if ok then return result end
  end
  return nil, err
end

return serialization
""".trimIndent()

    // ================================================================
    // /lib/sides.lua
    // ================================================================
    val SIDES_LUA = """
local sides = {
  bottom = 0, down  = 0, negy = 0,
  top    = 1, up    = 1, posy = 1,
  back   = 2, north = 2, negz = 2,
  front  = 3, south = 3, posz = 3,
  right  = 4, west  = 4, negx = 4,
  left   = 5, east  = 5, posx = 5,
}
sides[0] = "bottom"
sides[1] = "top"
sides[2] = "back"
sides[3] = "front"
sides[4] = "right"
sides[5] = "left"
return sides
""".trimIndent()

    // ================================================================
    // /lib/colors.lua
    // ================================================================
    val COLORS_LUA = """
local colors = {
  white     = 0, orange    = 1, magenta   = 2, lightblue = 3,
  yellow    = 4, lime      = 5, pink      = 6, gray      = 7,
  silver    = 8, cyan      = 9, purple    = 10, blue      = 11,
  brown     = 12, green     = 13, red       = 14, black     = 15,
}
colors[0]  = "white"
colors[1]  = "orange"
colors[2]  = "magenta"
colors[3]  = "lightblue"
colors[4]  = "yellow"
colors[5]  = "lime"
colors[6]  = "pink"
colors[7]  = "gray"
colors[8]  = "silver"
colors[9]  = "cyan"
colors[10] = "purple"
colors[11] = "blue"
colors[12] = "brown"
colors[13] = "green"
colors[14] = "red"
colors[15] = "black"
return colors
""".trimIndent()

    // ================================================================
    // Additional commands
    // ================================================================
    val PWD_LUA = """
print(os.getenv("PWD") or "/")
""".trimIndent()

    val SET_LUA = """
local args = {...}
if #args == 0 then
  -- print all env vars (not possible to enumerate, just print known ones)
  local known = {"HOME", "PATH", "PS1", "SHELL", "TMPDIR", "TERM", "PWD"}
  for _, k in ipairs(known) do
    local v = os.getenv(k)
    if v then print(k .. "=" .. v) end
  end
elseif #args == 1 then
  local k, v = args[1]:match("([^=]+)=(.*)")
  if k then
    os.setenv(k, v)
  else
    local val = os.getenv(args[1])
    if val then print(args[1] .. "=" .. val) else print(args[1] .. " not set") end
  end
end
""".trimIndent()

    val WHICH_LUA = """
local shell = require("shell")
local args = {...}
if #args == 0 then print("Usage: which <command>"); return end
local path = shell.resolve(args[1])
if path then print(path) else print(args[1] .. ": not found") end
""".trimIndent()

    val HOSTNAME_LUA = """
local args = {...}
if #args > 0 then
  os.setenv("HOSTNAME", args[1])
else
  print(os.getenv("HOSTNAME") or "localhost")
end
""".trimIndent()

    val COMPONENTS_LUA = """
for addr, ctype in component.list() do
  print(addr:sub(1, 8) .. "... " .. ctype)
end
""".trimIndent()

    val LABEL_LUA = """
local args = {...}
local fs = require("filesystem")
if #args == 0 then
  -- Show label of boot drive
  local addr = computer.getBootAddress()
  local label = ""
  pcall(function() label = component.invoke(addr, "getLabel") or "" end)
  print(addr:sub(1, 8) .. ": " .. (label ~= "" and label or "(no label)"))
elseif #args == 2 then
  local addr = args[1]
  local label = args[2]
  -- Find full address
  for a, t in component.list("filesystem") do
    if a:sub(1, #addr) == addr then
      pcall(function() component.invoke(a, "setLabel", label) end)
      print("Label set to: " .. label)
      return
    end
  end
  print("No filesystem found matching: " .. addr)
else
  print("Usage: label [<address> <name>]")
end
""".trimIndent()

    val FREE_LUA = """
local total = computer.totalMemory()
local free = computer.freeMemory()
local used = total - free
print(string.format("Total: %d bytes", total))
print(string.format("Used:  %d bytes (%.1f%%)", used, used / total * 100))
print(string.format("Free:  %d bytes (%.1f%%)", free, free / total * 100))
""".trimIndent()

    val DF_LUA = """
local fs = require("filesystem")
print(string.format("%-12s %-10s %-10s %-10s %s", "Filesystem", "Total", "Used", "Free", "Mounted on"))
for path, addr in fs.mounts() do
  local total = fs.spaceTotal(path)
  local used = fs.spaceUsed(path)
  local free = total - used
  print(string.format("%-12s %-10d %-10d %-10d %s", addr:sub(1, 8) .. "..", total, used, free, path))
end
""".trimIndent()

    val SLEEP_LUA = """
local args = {...}
local seconds = tonumber(args[1]) or 1
os.sleep(seconds)
""".trimIndent()

    // ================================================================
    // /bin/wc.lua - Word/line/char count
    // ================================================================
    val WC_LUA = """
local fs = require("filesystem")
local shell = require("shell")
local args = {...}

local function countContent(content)
  local lines = 0
  local words = 0
  local chars = #content
  for _ in content:gmatch("\n") do lines = lines + 1 end
  if #content > 0 and content:sub(-1) ~= "\n" then lines = lines + 1 end
  for _ in content:gmatch("%S+") do words = words + 1 end
  return lines, words, chars
end

if #args == 0 then
  local content = ""
  while true do
    local line = io.read("*l")
    if not line then break end
    content = content .. line .. "\n"
  end
  local l, w, c = countContent(content)
  print(string.format("%8d %8d %8d", l, w, c))
  return 0
end

local exitCode = 0
local totalL, totalW, totalC = 0, 0, 0
for _, file in ipairs(args) do
  local path = file
  if path:sub(1,1) ~= "/" then
    path = fs.concat(shell.getWorkingDirectory(), path)
  end
  path = fs.canonical(path)
  local f = fs.open(path, "r")
  if not f then
    io.stderr:write("wc: " .. file .. ": No such file or directory\n")
    exitCode = 1
  else
    local content = ""
    while true do
      local data = f:read(4096)
      if not data then break end
      content = content .. data
    end
    f:close()
    local l, w, c = countContent(content)
    totalL = totalL + l
    totalW = totalW + w
    totalC = totalC + c
    print(string.format("%8d %8d %8d %s", l, w, c, file))
  end
end
if #args > 1 then
  print(string.format("%8d %8d %8d total", totalL, totalW, totalC))
end
return exitCode
""".trimIndent()

    // ================================================================
    // /bin/tail.lua - Show last N lines
    // ================================================================
    val TAIL_LUA = """
local fs = require("filesystem")
local shell = require("shell")
local args = {...}
local n = 10
local files = {}
local i = 1
while i <= #args do
  if args[i] == "-n" and i < #args then
    n = tonumber(args[i+1]) or 10
    i = i + 2
  elseif args[i]:match("^%-(%d+)${'$'}") then
    n = tonumber(args[i]:match("^%-(%d+)${'$'}")) or 10
    i = i + 1
  else
    files[#files+1] = args[i]
    i = i + 1
  end
end
if #files == 0 then
  -- Read from stdin
  local lines = {}
  while true do
    local line = io.read("*l")
    if not line then break end
    lines[#lines+1] = line
  end
  local start = math.max(1, #lines - n + 1)
  for j = start, #lines do
    print(lines[j])
  end
  return 0
end
local exitCode = 0
for idx, file in ipairs(files) do
  local path = file
  if path:sub(1,1) ~= "/" then
    path = fs.concat(shell.getWorkingDirectory(), path)
  end
  path = fs.canonical(path)
  local f = io.open(path, "r")
  if not f then
    io.stderr:write("tail: " .. file .. ": No such file or directory\n")
    exitCode = 1
  else
    if #files > 1 then
      if idx > 1 then io.write("\n") end
      io.write("==> " .. file .. " <==")
      io.write("\n")
    end
    local lines = {}
    for line in f:lines() do
      lines[#lines+1] = line
    end
    f:close()
    local start = math.max(1, #lines - n + 1)
    for j = start, #lines do
      print(lines[j])
    end
  end
end
return exitCode
""".trimIndent()

    // ================================================================
    // /bin/tee.lua - Write to file and stdout
    // ================================================================
    val TEE_LUA = """
local fs = require("filesystem")
local shell = require("shell")
local args, options = shell.parse(...)
if #args == 0 then
  print("Usage: tee [-a] <file> [...]")
  return
end
local mode = options.a and "a" or "w"
local handles = {}
for _, file in ipairs(args) do
  local path = file
  if path:sub(1,1) ~= "/" then
    path = fs.concat(shell.getWorkingDirectory(), path)
  end
  local f = fs.open(path, mode)
  if f then handles[#handles+1] = f end
end
while true do
  local line = io.read("*l")
  if not line then break end
  print(line)
  for _, f in ipairs(handles) do
    f:write(line .. "\n")
  end
end
for _, f in ipairs(handles) do
  f:close()
end
""".trimIndent()

    // ================================================================
    // /bin/rev.lua - Reverse lines
    // ================================================================
    val REV_LUA = """
local args = {...}
local function reverseLine(s)
  local chars = {}
  for j = #s, 1, -1 do
    chars[#chars+1] = s:sub(j, j)
  end
  return table.concat(chars)
end
if #args == 0 then
  while true do
    local line = io.read("*l")
    if not line then break end
    print(reverseLine(line))
  end
else
  for _, file in ipairs(args) do
    local f = io.open(file, "r")
    if not f then
      io.stderr:write("rev: " .. file .. ": No such file\n")
    else
      for line in f:lines() do
        print(reverseLine(line))
      end
      f:close()
    end
  end
end
""".trimIndent()

    // ================================================================
    // /bin/type.lua - Show type of a command (builtin, alias, or program)
    // ================================================================
    val TYPE_LUA = """
local shell = require("shell")
local args = {...}
if #args == 0 then
  io.stderr:write("Usage: type <name>\n")
  return 1
end
for _, name in ipairs(args) do
  local aliases = shell.aliases()
  if aliases[name] then
    print(name .. " is aliased to '" .. aliases[name] .. "'")
  else
    local path = shell.resolve(name)
    if path then
      print(name .. " is " .. path)
    else
      io.stderr:write("type: " .. name .. ": not found\n")
    end
  end
end
""".trimIndent()

    // ================================================================
    // /bin/env.lua - Display all environment variables
    // ================================================================
    val ENV_LUA = """
local shell = require("shell")
local env = shell.getenv()
if type(env) == "table" then
  for k, v in pairs(env) do
    print(k .. "=" .. tostring(v))
  end
else
  -- Fallback: show common variables
  for _, name in ipairs({"PATH", "HOME", "PWD", "PS1", "SHELL", "TERM"}) do
    local val = os.getenv(name)
    if val then
      print(name .. "=" .. val)
    end
  end
end
""".trimIndent()

    // ================================================================
    // /bin/true.lua - Exit with success
    // ================================================================
    val TRUE_LUA = """
return 0
""".trimIndent()

    // ================================================================
    // /bin/false.lua - Exit with failure
    // ================================================================
    val FALSE_LUA = """
return 1
""".trimIndent()

    // ================================================================
    // /bin/rc.lua - Service management command
    // ================================================================
    val RC_CMD_LUA = """
local rc = require("rc")
local args = {...}

if #args == 0 then
  print("Usage: rc <command> [service]")
  print("Commands:")
  print("  list     - list loaded services")
  print("  start    - start a service")
  print("  stop     - stop a service")
  print("  restart  - restart a service")
  print("  status   - show service status")
  print("  enable   - enable service at boot")
  print("  disable  - disable service at boot")
  return
end

local cmd = args[1]
local service = args[2]

if cmd == "list" then
  local loaded = rc.loaded()
  if #loaded == 0 then
    print("No services loaded")
  else
    print("Loaded services:")
    for _, name in ipairs(loaded) do
      print("  " .. name .. ": " .. rc.status(name))
    end
  end
elseif cmd == "start" then
  if not service then
    print("Usage: rc start <service>")
    return
  end
  local ok, err = rc.start(service)
  if ok then
    print("Started: " .. service)
  else
    print("Failed to start " .. service .. ": " .. tostring(err))
  end
elseif cmd == "stop" then
  if not service then
    print("Usage: rc stop <service>")
    return
  end
  local ok, err = rc.stop(service)
  if ok then
    print("Stopped: " .. service)
  else
    print("Failed to stop " .. service .. ": " .. tostring(err))
  end
elseif cmd == "restart" then
  if not service then
    print("Usage: rc restart <service>")
    return
  end
  local ok, err = rc.restart(service)
  if ok then
    print("Restarted: " .. service)
  else
    print("Failed to restart " .. service .. ": " .. tostring(err))
  end
elseif cmd == "status" then
  if not service then
    -- Show all
    local loaded = rc.loaded()
    for _, name in ipairs(loaded) do
      print(name .. ": " .. rc.status(name))
    end
  else
    print(service .. ": " .. rc.status(service))
  end
elseif cmd == "enable" or cmd == "disable" then
  print("Enable/disable requires editing /etc/rc.cfg")
else
  print("Unknown command: " .. cmd)
end
""".trimIndent()

    // ================================================================
    // /lib/uuid.lua - UUID generation
    // ================================================================
    val UUID_LUA = """
local uuid = {}

function uuid.next()
  -- Generate UUID v4 format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
  -- where y is one of 8, 9, a, b
  local template = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'
  return string.gsub(template, '[xy]', function(c)
    local v = (c == 'x') and math.random(0, 0xf) or math.random(8, 0xb)
    return string.format('%x', v)
  end)
end

return uuid
""".trimIndent()

    // ================================================================
    // /lib/buffer.lua - Stream buffering wrapper
    // ================================================================
    val BUFFER_LUA = """
local buffer = {}
local metatable = {
  __index = buffer,
  __metatable = "file"
}

function buffer.new(mode, stream)
  local result = {
    closed = false,
    tty = false,
    mode = {},
    stream = stream,
    bufferRead = "",
    bufferWrite = "",
    bufferSize = 512,
    bufferMode = "full",
    readTimeout = math.huge,
  }
  mode = mode or "r"
  for i = 1, #mode do
    result.mode[mode:sub(i, i)] = true
  end
  return setmetatable(result, metatable)
end

function buffer:close()
  if self.mode.w or self.mode.a then
    self:flush()
  end
  self.closed = true
  if self.stream and self.stream.close then
    return self.stream:close()
  end
  return true
end

function buffer:flush()
  if #self.bufferWrite > 0 then
    local tmp = self.bufferWrite
    self.bufferWrite = ""
    if self.stream and self.stream.write then
      local result, reason = self.stream:write(tmp)
      if not result then
        return nil, reason or "bad file descriptor"
      end
    end
  end
  return self
end

function buffer:lines(...)
  local args = table.pack(...)
  return function()
    local result = table.pack(self:read(table.unpack(args, 1, args.n)))
    if not result[1] and result[2] then
      error(result[2])
    end
    return table.unpack(result, 1, result.n)
  end
end

local function readChunk(self)
  if not self.stream or not self.stream.read then
    return nil, "no stream"
  end
  local result, reason = self.stream:read(math.max(1, self.bufferSize))
  if result then
    self.bufferRead = self.bufferRead .. result
    return self
  else
    return result, reason
  end
end

function buffer:read(...)
  if not self.mode.r then
    return nil, "read mode was not enabled for this stream"
  end
  if self.mode.w or self.mode.a then
    self:flush()
  end
  
  local args = table.pack(...)
  if args.n == 0 then
    args = {"*l"}
    args.n = 1
  end
  
  local results = {}
  for i = 1, args.n do
    local fmt = args[i]
    local result
    
    if fmt == "*l" or fmt == "l" then
      -- Read line without newline
      while true do
        local nl = self.bufferRead:find("\n")
        if nl then
          result = self.bufferRead:sub(1, nl - 1)
          if result:sub(-1) == "\r" then result = result:sub(1, -2) end
          self.bufferRead = self.bufferRead:sub(nl + 1)
          break
        end
        local ok = readChunk(self)
        if not ok then
          if #self.bufferRead > 0 then
            result = self.bufferRead
            self.bufferRead = ""
          end
          break
        end
      end
    elseif fmt == "*L" or fmt == "L" then
      -- Read line with newline
      while true do
        local nl = self.bufferRead:find("\n")
        if nl then
          result = self.bufferRead:sub(1, nl)
          self.bufferRead = self.bufferRead:sub(nl + 1)
          break
        end
        local ok = readChunk(self)
        if not ok then
          if #self.bufferRead > 0 then
            result = self.bufferRead
            self.bufferRead = ""
          end
          break
        end
      end
    elseif fmt == "*a" or fmt == "a" then
      -- Read all
      while readChunk(self) do end
      if #self.bufferRead > 0 then
        result = self.bufferRead
        self.bufferRead = ""
      end
    elseif fmt == "*n" or fmt == "n" then
      -- Read number
      while #self.bufferRead < 64 do
        if not readChunk(self) then break end
      end
      local s, e = self.bufferRead:find("^%s*[%-]?%d+%.?%d*[eE]?[%-+]?%d*")
      if s then
        local numStr = self.bufferRead:sub(s, e)
        self.bufferRead = self.bufferRead:sub(e + 1)
        result = tonumber(numStr)
      end
    elseif type(fmt) == "number" then
      -- Read N bytes
      while #self.bufferRead < fmt do
        if not readChunk(self) then break end
      end
      if #self.bufferRead > 0 then
        local n = math.min(fmt, #self.bufferRead)
        result = self.bufferRead:sub(1, n)
        self.bufferRead = self.bufferRead:sub(n + 1)
      end
    end
    
    results[i] = result
  end
  
  return table.unpack(results, 1, args.n)
end

function buffer:setvbuf(mode, size)
  mode = mode or self.bufferMode
  size = size or self.bufferSize
  self.bufferMode = mode
  self.bufferSize = size
  return self.bufferMode, self.bufferSize
end

function buffer:write(...)
  if self.closed then
    return nil, "bad file descriptor"
  end
  if not self.mode.w and not self.mode.a then
    return nil, "write mode was not enabled for this stream"
  end
  
  local args = table.pack(...)
  for i = 1, args.n do
    local arg = tostring(args[i])
    if self.bufferMode == "no" then
      if self.stream and self.stream.write then
        local result, reason = self.stream:write(arg)
        if not result then
          return nil, reason
        end
      end
    else
      self.bufferWrite = self.bufferWrite .. arg
      if self.bufferMode == "line" and arg:find("\n") then
        self:flush()
      elseif #self.bufferWrite >= self.bufferSize then
        self:flush()
      end
    end
  end
  
  return self
end

function buffer:seek(whence, offset)
  if self.stream and self.stream.seek then
    self:flush()
    self.bufferRead = ""
    return self.stream:seek(whence, offset)
  end
  return nil, "not seekable"
end

return buffer
""".trimIndent()

    // ================================================================
    // /lib/process.lua - Process management (simplified)
    // ================================================================
    val PROCESS_LUA = """
local process = {}
local processes = {}
local currentProcess = nil

function process.load(path, env, init, name)
  checkArg(1, path, "string", "function")
  checkArg(2, env, "table", "nil")
  checkArg(3, init, "function", "nil")
  checkArg(4, name, "string", "nil")
  
  env = env or _G
  local code
  if type(path) == "string" then
    local shell = require("shell")
    local program = shell.resolve(path, "lua")
    if not program then
      return nil, "program not found: " .. path
    end
    local fn, err = loadfile(program, "t", env)
    if not fn then return nil, err end
    code = fn
  else
    code = path
  end
  
  local thread = coroutine.create(function(...)
    if init then
      return code(init(...))
    else
      return code(...)
    end
  end)
  
  processes[thread] = {
    path = path,
    command = name or tostring(path),
    env = env,
    data = {
      handles = {},
      io = {},
    },
  }
  
  return thread
end

function process.info(levelOrThread)
  checkArg(1, levelOrThread, "thread", "number", "nil")
  
  if type(levelOrThread) == "thread" then
    return processes[levelOrThread]
  end
  
  -- Return info about current process
  local co = coroutine.running()
  if co and processes[co] then
    return processes[co]
  end
  
  -- Return a default process info
  return {
    path = "/bin/sh.lua",
    command = "sh",
    env = _G,
    data = {
      handles = {},
      io = {},
    },
  }
end

function process.running(level)
  local info = process.info(level)
  if info then
    return info.path, info.env, info.command
  end
end

function process.findProcess(co)
  co = co or coroutine.running()
  return processes[co]
end

return process
""".trimIndent()

    // ================================================================
    // /lib/thread.lua - Thread library (stub)
    // ================================================================
    val THREAD_LUA = """
-- Thread library stub for compatibility
-- Full threading requires coroutine management not available in simplified OpenOS
local thread = {}

function thread.create(fn, ...)
  checkArg(1, fn, "function")
  return nil, "threading not supported in simplified OpenOS"
end

function thread.waitForAll(threads, timeout)
  return nil, "threading not supported in simplified OpenOS"
end

function thread.waitForAny(threads, timeout)
  return nil, "threading not supported in simplified OpenOS"
end

function thread.current()
  return nil
end

return thread
""".trimIndent()

    // ================================================================
    // /lib/rc.lua - Run control (services)
    // ================================================================
    val RC_LUA = """
-- RC (run control) library for managing services
local rc = {}
local fs = require("filesystem")
local running = {}

rc.path = "/etc/rc.d/"

function rc.loaded()
  local result = {}
  for name in pairs(running) do
    result[#result + 1] = name
  end
  return result
end

function rc.load(name, reason)
  if running[name] then
    return true -- already loaded
  end
  
  local path = rc.path .. name .. ".lua"
  if not fs.exists(path) then
    return nil, "rc script not found: " .. name
  end
  
  local fn, err = loadfile(path)
  if not fn then
    return nil, err
  end
  
  local service = {}
  local env = setmetatable({}, {__index = _G})
  setfenv(fn, env)
  
  local ok, err2 = pcall(fn)
  if not ok then
    return nil, err2
  end
  
  service.start = env.start
  service.stop = env.stop
  service.status = env.status
  
  running[name] = service
  
  if service.start and reason ~= "load" then
    local ok2, err3 = pcall(service.start)
    if not ok2 then
      return nil, "failed to start: " .. tostring(err3)
    end
  end
  
  return true
end

function rc.unload(name)
  local service = running[name]
  if not service then
    return nil, "not loaded: " .. name
  end
  
  if service.stop then
    pcall(service.stop)
  end
  
  running[name] = nil
  return true
end

function rc.start(name)
  local service = running[name]
  if not service then
    local ok, err = rc.load(name, "start")
    if not ok then return nil, err end
    return true
  end
  
  if service.start then
    local ok, err = pcall(service.start)
    if not ok then return nil, err end
  end
  return true
end

function rc.stop(name)
  local service = running[name]
  if not service then
    return nil, "not loaded: " .. name
  end
  
  if service.stop then
    local ok, err = pcall(service.stop)
    if not ok then return nil, err end
  end
  return true
end

function rc.restart(name)
  rc.stop(name)
  return rc.start(name)
end

function rc.status(name)
  local service = running[name]
  if not service then
    return "not loaded"
  end
  
  if service.status then
    local ok, result = pcall(service.status)
    if ok then return result end
  end
  
  return "running"
end

return rc
""".trimIndent()

    // ================================================================
    // /lib/devfs.lua - Virtual /dev filesystem
    // ================================================================
    val DEVFS_LUA = """
-- Virtual device filesystem
local devfs = {}

devfs.devices = {}

-- /dev/null - discards all writes, returns EOF on read
devfs.devices["null"] = {
  read = function() return nil end,
  write = function(data) return #data end,
}

-- /dev/zero - returns infinite zeros
devfs.devices["zero"] = {
  read = function(count)
    count = math.min(count or 1, 4096)
    return string.rep("\0", count)
  end,
  write = function(data) return #data end,
}

-- /dev/random - returns random bytes
devfs.devices["random"] = {
  read = function(count)
    count = math.min(count or 1, 4096)
    local bytes = {}
    for i = 1, count do
      bytes[i] = string.char(math.random(0, 255))
    end
    return table.concat(bytes)
  end,
  write = function(data) return #data end,
}

-- /dev/urandom - same as random for our purposes
devfs.devices["urandom"] = devfs.devices["random"]

-- /dev/full - returns EOF on read, returns ENOSPC on write
devfs.devices["full"] = {
  read = function() return nil end,
  write = function() return nil, "no space left on device" end,
}

function devfs.open(device, mode)
  local dev = devfs.devices[device]
  if not dev then
    return nil, "no such device: " .. device
  end
  
  return {
    device = device,
    dev = dev,
    read = function(self, n)
      if self.dev.read then
        return self.dev.read(n)
      end
      return nil
    end,
    write = function(self, data)
      if self.dev.write then
        return self.dev.write(data)
      end
      return nil, "not writable"
    end,
    close = function() return true end,
    seek = function() return nil, "not seekable" end,
  }
end

function devfs.list()
  local result = {}
  for name in pairs(devfs.devices) do
    result[#result + 1] = name
  end
  table.sort(result)
  return result
end

function devfs.exists(device)
  return devfs.devices[device] ~= nil
end

return devfs
""".trimIndent()

    // ================================================================
    // /boot/05_devfs.lua - Initialize devfs
    // ================================================================
    val BOOT_05_DEVFS = """
-- Set up devfs access via filesystem hooks
local fs = require("filesystem")
local devfs = require("devfs")

-- Patch filesystem.open to handle /dev/ paths
local originalOpen = fs.open
fs.open = function(path, mode)
  path = fs.canonical(path)
  if path:sub(1, 5) == "/dev/" then
    local device = path:sub(6)
    return devfs.open(device, mode)
  end
  return originalOpen(path, mode)
end

-- Patch filesystem.exists to handle /dev/ paths
local originalExists = fs.exists
fs.exists = function(path)
  path = fs.canonical(path)
  if path == "/dev" then return true end
  if path:sub(1, 5) == "/dev/" then
    local device = path:sub(6)
    return devfs.exists(device)
  end
  return originalExists(path)
end

-- Patch filesystem.isDirectory to handle /dev/
local originalIsDir = fs.isDirectory
fs.isDirectory = function(path)
  path = fs.canonical(path)
  if path == "/dev" then return true end
  if path:sub(1, 5) == "/dev/" then return false end
  return originalIsDir(path)
end

-- Patch filesystem.list to handle /dev/
local originalList = fs.list
fs.list = function(path)
  path = fs.canonical(path)
  if path == "/dev" or path == "/dev/" then
    local devices = devfs.list()
    local i = 0
    return function()
      i = i + 1
      return devices[i]
    end
  end
  return originalList(path)
end
""".trimIndent()

    // ================================================================
    // /boot/89_rc.lua - Load RC services
    // ================================================================
    val BOOT_89_RC = """
-- RC boot script - load enabled services
local event = require("event")

event.listen("init", function()
  local fs = require("filesystem")
  local rc = require("rc")
  
  -- Create rc.d directory if it doesn't exist
  if not fs.exists("/etc/rc.d") then
    fs.makeDirectory("/etc/rc.d")
  end
  
  -- Load enabled services from /etc/rc.cfg if it exists
  if fs.exists("/etc/rc.cfg") then
    local f = fs.open("/etc/rc.cfg", "r")
    if f then
      local content = ""
      repeat
        local data = f:read(math.huge)
        content = content .. (data or "")
      until not data
      f:close()
      
      local cfg = load("return " .. content)
      if cfg then
        local ok, services = pcall(cfg)
        if ok and type(services) == "table" and services.enabled then
          for _, name in ipairs(services.enabled) do
            pcall(rc.load, name)
          end
        end
      end
    end
  end
  
  return false -- unregister this listener
end)
""".trimIndent()

    // ================================================================
    // /bin/address.lua - Show component address by type
    // ================================================================
    val ADDRESS_LUA = """
local args = {...}
if #args == 0 then
  -- Show computer address by default
  io.write(computer.address() .. "\n")
else
  local ctype = args[1]
  local addr = component.list(ctype)()
  if addr then
    io.write(addr .. "\n")
  else
    io.stderr:write("no component of type: " .. ctype .. "\n")
    return 1
  end
end
""".trimIndent()

    // ================================================================
    // /bin/resolution.lua - Get/set GPU resolution
    // ================================================================
    val RESOLUTION_LUA = """
local shell = require("shell")
local term = require("term")
local args = shell.parse(...)

local gpu = component.list("gpu")()
if not gpu then
  io.stderr:write("no GPU available\n")
  return 1
end

if #args == 0 then
  local w, h = component.invoke(gpu, "getResolution")
  io.write(w .. " " .. h .. "\n")
  return
end

if #args ~= 2 then
  io.write("Usage: resolution [<width> <height>]\n")
  return 1
end

local w = tonumber(args[1])
local h = tonumber(args[2])
if not w or not h then
  io.stderr:write("invalid width or height\n")
  return 1
end

local ok, reason = component.invoke(gpu, "setResolution", w, h)
if not ok then
  if reason then
    io.stderr:write(reason .. "\n")
  end
  return 1
end
term.clear()
""".trimIndent()

    // ================================================================
    // /bin/flash.lua - Flash EEPROM
    // ================================================================
    val FLASH_LUA = """
local shell = require("shell")
local fs = require("filesystem")
local args, options = shell.parse(...)

if #args < 1 and not options.l and not options.r then
  io.write("Usage: flash [-qlr] [<bios.lua>] [label]\n")
  io.write(" q: quiet mode, don't ask questions.\n")
  io.write(" l: print current contents of installed EEPROM.\n")
  io.write(" r: save the current contents of installed EEPROM to file.\n")
  return
end

local eeprom = component.list("eeprom")()
if not eeprom then
  io.stderr:write("no EEPROM found\n")
  return 1
end

if options.l then
  -- Print EEPROM contents
  local data = component.invoke(eeprom, "get")
  io.write(data or "")
  return
end

if options.r then
  -- Read EEPROM to file
  if #args < 1 then
    io.stderr:write("specify output file\n")
    return 1
  end
  local fileName = shell.resolve(args[1]) or args[1]
  if not options.q then
    if fs.exists(fileName) then
      io.write("Are you sure you want to overwrite " .. fileName .. "?\n")
      io.write("Type 'y' to confirm: ")
      local response = io.read()
      if not response or response:lower():sub(1,1) ~= "y" then
        return
      end
    end
    io.write("Reading EEPROM " .. eeprom .. "\n")
  end
  local data = component.invoke(eeprom, "get")
  local f = fs.open(fileName, "w")
  if not f then
    io.stderr:write("cannot open file for writing\n")
    return 1
  end
  f:write(data or "")
  f:close()
  if not options.q then
    local label = component.invoke(eeprom, "getLabel") or ""
    io.write("All done! The label is '" .. label .. "'.\n")
  end
  return
end

-- Flash file to EEPROM
local fileName = shell.resolve(args[1]) or args[1]
if not fs.exists(fileName) then
  io.stderr:write("file not found: " .. fileName .. "\n")
  return 1
end

local f = fs.open(fileName, "r")
if not f then
  io.stderr:write("cannot open file\n")
  return 1
end
local data = ""
repeat
  local chunk = f:read(math.huge)
  data = data .. (chunk or "")
until not chunk
f:close()

if not options.q then
  io.write("Insert the EEPROM you would like to flash.\n")
  io.write("Type 'y' to confirm: ")
  local response = io.read()
  if not response or response:lower():sub(1,1) ~= "y" then
    return
  end
  io.write("Flashing EEPROM " .. eeprom .. "\n")
  io.write("Please do NOT power down or restart during this operation!\n")
end

local ok, reason = component.invoke(eeprom, "set", data)
if reason then
  io.stderr:write("flash failed: " .. reason .. "\n")
  return 1
end

local label = args[2]
if not options.q and not label then
  io.write("Enter new label (blank to keep current): ")
  label = io.read()
end
if label and #label > 0 then
  component.invoke(eeprom, "setLabel", label)
  if not options.q then
    io.write("Set label to '" .. component.invoke(eeprom, "getLabel") .. "'.\n")
  end
end

if not options.q then
  io.write("All done!\n")
end
""".trimIndent()

    // ================================================================
    // /bin/primary.lua - Set primary component for a type
    // ================================================================
    val PRIMARY_LUA = """
local shell = require("shell")
local args = shell.parse(...)

if #args == 0 then
  io.write("Usage: primary <type> [<address>]\n")
  io.write("Note that the address may be abbreviated.\n")
  return 1
end

local componentType = args[1]

if #args > 1 then
  local address = args[2]
  -- Find matching component
  local found = nil
  for addr in component.list(componentType) do
    if addr:sub(1, #address) == address then
      found = addr
      break
    end
  end
  if not found then
    io.stderr:write("no component with this address\n")
    return 1
  end
  component.setPrimary(componentType, found)
  os.sleep(0.1) -- allow signals to be processed
end

-- Show current primary
local primary = component.list(componentType)()
if primary then
  io.write(primary .. "\n")
else
  io.stderr:write("no primary component for this type\n")
  return 1
end
""".trimIndent()

    // ================================================================
    // /bin/uptime.lua - Show system uptime
    // ================================================================
    val UPTIME_LUA = """
local seconds = math.floor(computer.uptime())
local minutes, hours = 0, 0
if seconds >= 60 then
  minutes = math.floor(seconds / 60)
  seconds = seconds % 60
end
if minutes >= 60 then
  hours = math.floor(minutes / 60)
  minutes = minutes % 60
end
io.write(string.format("%02d:%02d:%02d\n", hours, minutes, seconds))
""".trimIndent()

    // ================================================================
    // /bin/redstone.lua - Redstone control
    // ================================================================
    val REDSTONE_LUA = """
local colors = require("colors")
local sides = require("sides")
local shell = require("shell")
local args, options = shell.parse(...)

local rs = component.list("redstone")()
if not rs then
  io.stderr:write("This program requires a redstone card or redstone I/O block.\n")
  return 1
end

if #args == 0 and not options.w and not options.f then
  io.write("Usage:\n")
  io.write("  redstone <side> [<value>]\n")
  local hasBundled = pcall(component.invoke, rs, "getBundledInput", 0, 0)
  if hasBundled then
    io.write("  redstone -b <side> <color> [<value>]\n")
  end
  local hasWireless = pcall(component.invoke, rs, "getWirelessInput")
  if hasWireless then
    io.write("  redstone -w [<value>]\n")
    io.write("  redstone -f [<frequency>]\n")
  end
  return
end

if options.w then
  -- Wireless redstone
  local hasWireless = pcall(component.invoke, rs, "setWirelessOutput", false)
  if not hasWireless then
    io.stderr:write("wireless redstone not available\n")
    return 1
  end
  if #args > 0 then
    local value = args[1]
    if tonumber(value) then
      value = tonumber(value) > 0
    else
      value = ({["true"]=true, ["on"]=true, ["yes"]=true})[value:lower()] ~= nil
    end
    component.invoke(rs, "setWirelessOutput", value)
  end
  local inVal = component.invoke(rs, "getWirelessInput")
  local outVal = component.invoke(rs, "getWirelessOutput")
  io.write("in: " .. tostring(inVal) .. "\n")
  io.write("out: " .. tostring(outVal) .. "\n")
  return
end

if options.f then
  -- Wireless frequency
  local ok = pcall(component.invoke, rs, "getWirelessFrequency")
  if not ok then
    io.stderr:write("wireless redstone not available\n")
    return 1
  end
  if #args > 0 then
    local value = tonumber(args[1])
    if not value then
      io.stderr:write("invalid frequency\n")
      return 1
    end
    component.invoke(rs, "setWirelessFrequency", value)
  end
  local freq = component.invoke(rs, "getWirelessFrequency")
  io.write("freq: " .. tostring(freq) .. "\n")
  return
end

-- Normal redstone
local side = sides[args[1]]
if not side then
  io.stderr:write("invalid side\n")
  return 1
end
if type(side) == "string" then
  side = sides[side]
end

if options.b then
  -- Bundled redstone
  local ok = pcall(component.invoke, rs, "getBundledInput", side, 0)
  if not ok then
    io.stderr:write("bundled redstone not available\n")
    return 1
  end
  local color = colors[args[2]]
  if not color then
    io.stderr:write("invalid color\n")
    return 1
  end
  if type(color) == "string" then
    color = colors[color]
  end
  if #args > 2 then
    local value = tonumber(args[3]) or 0
    component.invoke(rs, "setBundledOutput", side, color, value)
  end
  local inVal = component.invoke(rs, "getBundledInput", side, color)
  local outVal = component.invoke(rs, "getBundledOutput", side, color)
  io.write("in: " .. tostring(inVal) .. "\n")
  io.write("out: " .. tostring(outVal) .. "\n")
else
  -- Simple redstone
  if #args > 1 then
    local value = tonumber(args[2]) or 0
    component.invoke(rs, "setOutput", side, value)
  end
  local inVal = component.invoke(rs, "getInput", side)
  local outVal = component.invoke(rs, "getOutput", side)
  io.write("in: " .. tostring(inVal) .. "\n")
  io.write("out: " .. tostring(outVal) .. "\n")
end
""".trimIndent()

    // ================================================================
    // /bin/time.lua - Time a command's execution
    // ================================================================
    val TIME_LUA = """
local shell = require("shell")
local args = {...}

if #args == 0 then
  io.write("Usage: time <command> [args...]\n")
  return
end

local realBefore = computer.uptime()
local cpuBefore = os.clock()

-- Execute the command
local cmd = args[1]
local cmdArgs = {}
for i = 2, #args do
  cmdArgs[#cmdArgs + 1] = args[i]
end
shell.execute(cmd, table.unpack(cmdArgs))

local realAfter = computer.uptime()
local cpuAfter = os.clock()

local realDiff = realAfter - realBefore
local cpuDiff = cpuAfter - cpuBefore

print(string.format("real %5dm%.3fs", math.floor(realDiff / 60), realDiff % 60))
print(string.format("cpu  %5dm%.3fs", math.floor(cpuDiff / 60), cpuDiff % 60))
""".trimIndent()

    // ================================================================
    // /bin/source.lua - Source a script file
    // ================================================================
    val SOURCE_LUA = """
local shell = require("shell")
local fs = require("filesystem")
local args, options = shell.parse(...)

if #args ~= 1 then
  io.stderr:write("specify a single file to source\n")
  return 1
end

local path = shell.resolve(args[1]) or args[1]
if not fs.exists(path) then
  if not options.q then
    io.stderr:write("file not found: " .. args[1] .. "\n")
  end
  return 1
end

local f = fs.open(path, "r")
if not f then
  if not options.q then
    io.stderr:write("cannot open file: " .. args[1] .. "\n")
  end
  return 1
end

local content = ""
repeat
  local data = f:read(math.huge)
  content = content .. (data or "")
until not data
f:close()

-- Execute each line as a shell command
for line in content:gmatch("[^\r\n]+") do
  line = line:match("^%s*(.-)%s*${'$'}") or ""
  -- Skip empty lines and comments
  if #line > 0 and line:sub(1, 1) ~= "#" then
    shell.execute(line)
  end
end
""".trimIndent()

    // ================================================================
    // /bin/du.lua - Disk usage
    // ================================================================
    val DU_LUA = """
local shell = require("shell")
local fs = require("filesystem")
local args, options = shell.parse(...)

if options.help then
  io.write("Usage: du [-sh] [path...]\n")
  io.write("  -s, --summarize  display only total for each argument\n")
  io.write("  -h, --human-readable  print sizes in human readable format\n")
  return
end

local function humanSize(bytes)
  if bytes < 1024 then return bytes .. "B" end
  if bytes < 1024 * 1024 then return string.format("%.1fK", bytes / 1024) end
  if bytes < 1024 * 1024 * 1024 then return string.format("%.1fM", bytes / (1024 * 1024)) end
  return string.format("%.1fG", bytes / (1024 * 1024 * 1024))
end

local human = options.h or options["human-readable"]
local summarize = options.s or options.summarize

local function getSize(path, printAll)
  local total = 0
  if fs.isDirectory(path) then
    for file in fs.list(path) do
      local full = fs.concat(path, file)
      local sub = getSize(full, printAll and not summarize)
      total = total + sub
    end
    if printAll then
      local sz = human and humanSize(total) or total
      io.write(sz .. "\t" .. path .. "\n")
    end
  else
    local sz = fs.size(path) or 0
    total = sz
    if printAll then
      local szStr = human and humanSize(sz) or sz
      io.write(szStr .. "\t" .. path .. "\n")
    end
  end
  return total
end

if #args == 0 then args[1] = "." end
for _, arg in ipairs(args) do
  local path = shell.resolve(arg) or arg
  if not fs.exists(path) then
    io.stderr:write("du: " .. arg .. ": No such file or directory\n")
  else
    getSize(path, true)
  end
end
""".trimIndent()

    // ================================================================
    // /bin/find.lua - Find files
    // ================================================================
    val FIND_LUA = """
local shell = require("shell")
local fs = require("filesystem")
local args, options = shell.parse(...)

if options.help then
  io.write("Usage: find [path] [--name=PATTERN] [--type=d|f]\n")
  io.write("  --name=PATTERN  file name pattern (supports * wildcard)\n")
  io.write("  --type=d        find only directories\n")
  io.write("  --type=f        find only files\n")
  return
end

local path = args[1] or "."
path = shell.resolve(path) or path

local namePattern = options.name or options.iname
local typeFilter = options.type
local caseInsensitive = options.iname ~= nil

local function patternMatch(name, pattern)
  if not pattern then return true end
  local p = pattern:gsub("%*", ".*"):gsub("%?", ".")
  if caseInsensitive then
    return name:lower():match("^" .. p:lower() .. "${'$'}")
  end
  return name:match("^" .. p .. "${'$'}")
end

local function search(dir)
  if not fs.exists(dir) then return end
  if not fs.isDirectory(dir) then
    -- It's a file
    if (not typeFilter or typeFilter == "f") and patternMatch(fs.name(dir), namePattern) then
      io.write(dir .. "\n")
    end
    return
  end

  -- Check the directory itself
  if (not typeFilter or typeFilter == "d") and patternMatch(fs.name(dir), namePattern) then
    if dir ~= path then -- don't print starting path
      io.write(dir .. "\n")
    end
  end

  for file in fs.list(dir) do
    local full = fs.concat(dir, file)
    if fs.isDirectory(full) then
      if (not typeFilter or typeFilter == "d") and patternMatch(file:gsub("/$", ""), namePattern) then
        io.write(full .. "\n")
      end
      search(full)
    else
      if (not typeFilter or typeFilter == "f") and patternMatch(file, namePattern) then
        io.write(full .. "\n")
      end
    end
  end
end

if not fs.exists(path) then
  io.stderr:write("find: " .. path .. ": No such file or directory\n")
  return 1
end
search(path)
""".trimIndent()

    // ================================================================
    // /bin/less.lua - Pager (alias for more)
    // ================================================================
    val LESS_LUA = """
-- less is similar to more (simplified pager)
local shell = require("shell")
local fs = require("filesystem")
local term = require("term")
local args = shell.parse(...)

if #args == 0 then
  io.write("Usage: less <file>\n")
  return
end

local path = shell.resolve(args[1]) or args[1]
if not fs.exists(path) then
  io.stderr:write("less: " .. args[1] .. ": No such file\n")
  return 1
end

local f = fs.open(path, "r")
if not f then
  io.stderr:write("less: cannot open " .. args[1] .. "\n")
  return 1
end

local content = ""
repeat
  local data = f:read(math.huge)
  content = content .. (data or "")
until not data
f:close()

local lines = {}
for line in content:gmatch("([^\n]*)\n?") do
  lines[#lines + 1] = line
end

local w, h = term.getViewport()
h = h - 1

local offset = 0
local function display()
  term.clear()
  for i = 1, h do
    local ln = lines[offset + i]
    if ln then
      term.write(ln:sub(1, w) .. "\n")
    end
  end
  term.write("-- " .. (offset + 1) .. "-" .. math.min(offset + h, #lines) .. "/" .. #lines .. " --")
end

display()
while true do
  local _, _, char, code = event.pull("key_down")
  if char == 113 or code == 0x10 then -- q
    term.clear()
    return
  elseif code == 0xC8 or code == 0x17 or char == 107 then -- up/w/k
    offset = math.max(0, offset - 1)
    display()
  elseif code == 0xD0 or code == 0x1F or char == 106 then -- down/s/j
    offset = math.min(#lines - h, offset + 1)
    if offset < 0 then offset = 0 end
    display()
  elseif code == 0xC9 then -- page up
    offset = math.max(0, offset - h)
    display()
  elseif code == 0xD1 or char == 32 then -- page down / space
    offset = math.min(#lines - h, offset + h)
    if offset < 0 then offset = 0 end
    display()
  elseif code == 0x147 then -- home
    offset = 0
    display()
  elseif code == 0x14F then -- end
    offset = math.max(0, #lines - h)
    display()
  end
end
""".trimIndent()

    // ================================================================
    // /bin/list.lua - Simple directory listing
    // ================================================================
    val LIST_LUA = """
local fs = require("filesystem")
local shell = require("shell")
local args, options = shell.parse(...)

if options.help then
  io.write("Usage: list [path]\n")
  io.write("  Displays a list of files with no added formatting\n")
  io.write("  Intended for low memory systems\n")
  return
end

local path = args[1] or "."
path = shell.resolve(path) or path

if not fs.exists(path) then
  io.stderr:write("cannot access '" .. path .. "': No such file or directory\n")
  return 1
end

if not fs.isDirectory(path) then
  io.write(fs.name(path) .. "\n")
  return
end

for item in fs.list(path) do
  io.write(item .. "\n")
end
""".trimIndent()

    // ================================================================
    // /bin/ln.lua - Create symbolic links (not supported in simplified VFS)
    // ================================================================
    val LN_LUA = """
local shell = require("shell")
local fs = require("filesystem")
local args = shell.parse(...)

if #args == 0 then
  io.write("Usage: ln <target> [<name>]\n")
  return 1
end

local target = args[1]
local linkpath = args[2]

if not linkpath then
  linkpath = fs.name(target)
end

-- Resolve paths
local resolvedTarget = shell.resolve(target) or target
if not fs.exists(resolvedTarget) then
  io.stderr:write("ln: failed to access '" .. target .. "': No such file or directory\n")
  return 1
end

local resolvedLink = shell.resolve(linkpath) or fs.concat(shell.getWorkingDirectory(), linkpath)

local result, reason = fs.link(resolvedTarget, resolvedLink)
if not result then
  io.stderr:write((reason or "symbolic links not supported") .. "\n")
  return 1
end
""".trimIndent()

    // ================================================================
    // /bin/lshw.lua - List hardware
    // ================================================================
    val LSHW_LUA = """
local shell = require("shell")
local args, options = shell.parse(...)

if options.help then
  io.write("Usage: lshw [-tdpvcsw]\n")
  io.write("List hardware information.\n")
  io.write("  -t  show component type (class)\n")
  io.write("  -d  show description\n")
  io.write("  -p  show product name\n")
  io.write("  -v  show vendor\n")
  io.write("  -c  show capacity\n")
  io.write("  -w  show width\n")
  io.write("  -s  show clock speed\n")
  return
end

-- Default columns
if not options.t and not options.d and not options.p and not options.v and not options.c and not options.w and not options.s then
  options.t = true
  options.d = true
  options.p = true
end

local columns = {}
if options.t then table.insert(columns, {name = "Class", field = "class"}) end
if options.d then table.insert(columns, {name = "Descr", field = "description"}) end
if options.p then table.insert(columns, {name = "Product", field = "product"}) end
if options.v then table.insert(columns, {name = "Vendor", field = "vendor"}) end
if options.c then table.insert(columns, {name = "Capacity", field = "capacity"}) end
if options.w then table.insert(columns, {name = "Width", field = "width"}) end
if options.s then table.insert(columns, {name = "Clock", field = "clock"}) end

-- Get device info if available
local devices = {}
local ok, devInfo = pcall(computer.getDeviceInfo)
if ok and devInfo then
  devices = devInfo
else
  -- Fallback: list components
  for addr, ctype in component.list() do
    devices[addr] = {class = ctype, description = ctype, product = "-", vendor = "-"}
  end
end

-- Calculate column widths
local widths = {}
for i, col in ipairs(columns) do widths[i] = #col.name end
for addr, info in pairs(devices) do
  for i, col in ipairs(columns) do
    local val = info[col.field] or "-"
    widths[i] = math.max(widths[i], #tostring(val))
  end
end

-- Print header
io.write(string.format("%-10s", "Address"))
for i, col in ipairs(columns) do
  io.write(string.format("  %-" .. widths[i] .. "s", col.name))
end
io.write("\n")

-- Print devices
for addr, info in pairs(devices) do
  io.write(string.format("%-10s", addr:sub(1, 5) .. "..."))
  for i, col in ipairs(columns) do
    local val = info[col.field] or "-"
    io.write(string.format("  %-" .. widths[i] .. "s", tostring(val)))
  end
  io.write("\n")
end
""".trimIndent()

    // ================================================================
    // /bin/man.lua - Manual pages (stub)
    // ================================================================
    val MAN_LUA = """
local args = {...}

if #args == 0 then
  io.write("Usage: man <command>\n")
  io.write("Display manual page for a command.\n")
  return 1
end

local cmd = args[1]

-- Built-in help for common commands
local manPages = {
  ls = "ls [options] [path] - List directory contents\n  -l  long format\n  -a  show hidden files\n  -h  human-readable sizes",
  cd = "cd <path> - Change current directory",
  pwd = "pwd - Print working directory",
  cat = "cat <file>... - Concatenate and print files",
  cp = "cp <source> <dest> - Copy files",
  mv = "mv <source> <dest> - Move/rename files",
  rm = "rm [-rf] <file>... - Remove files\n  -r  recursive\n  -f  force",
  mkdir = "mkdir [-p] <dir> - Create directory\n  -p  create parent directories",
  echo = "echo [text] - Print text",
  grep = "grep <pattern> <file>... - Search for pattern in files",
  find = "find [path] [--name=X] [--type=d|f] - Find files",
  wget = "wget <url> [file] - Download file from URL",
  edit = "edit <file> - Text editor",
  components = "components - List components",
  reboot = "reboot - Restart computer",
  shutdown = "shutdown - Power off computer",
}

local page = manPages[cmd]
if page then
  io.write(page .. "\n")
else
  io.write("No manual entry for " .. cmd .. "\n")
  io.write("Try '" .. cmd .. " --help' or 'help " .. cmd .. "'\n")
  return 1
end
""".trimIndent()

    // ================================================================
    // /bin/mktmp.lua - Create temporary file
    // ================================================================
    val MKTMP_LUA = """
local shell = require("shell")
local fs = require("filesystem")
local args, options = shell.parse(...)

if options.help then
  io.write("Usage: mktmp [-dqv] [path]\n")
  io.write("  -d  create a directory instead of a file\n")
  io.write("  -q  quiet mode\n")
  io.write("  -v  verbose mode\n")
  return
end

local tmpdir = args[1] or os.getenv("TMPDIR") or "/tmp"
local isDir = options.d
local quiet = options.q
local verbose = options.v

-- Generate random name
local function randomName()
  local chars = "abcdefghijklmnopqrstuvwxyz0123456789"
  local name = "tmp."
  for i = 1, 8 do
    local idx = math.random(1, #chars)
    name = name .. chars:sub(idx, idx)
  end
  return name
end

-- Ensure tmp directory exists
if not fs.exists(tmpdir) then
  fs.makeDirectory(tmpdir)
end

-- Create unique temp file/dir
local attempts = 0
local path
repeat
  path = fs.concat(tmpdir, randomName())
  attempts = attempts + 1
until not fs.exists(path) or attempts > 100

if fs.exists(path) then
  io.stderr:write("mktmp: failed to create temp file\n")
  return 1
end

if isDir then
  fs.makeDirectory(path)
else
  local f = fs.open(path, "w")
  if f then f:close() end
end

if verbose or (not quiet and io.stdout.tty) then
  io.write(path .. "\n")
end
""".trimIndent()

    // ================================================================
    // /bin/ps.lua - Process list (simplified)
    // ================================================================
    val PS_LUA = """
local shell = require("shell")
local args, options = shell.parse(...)

if options.help then
  io.write("Usage: ps\n")
  io.write("List running processes (simplified).\n")
  return
end

io.write("PID\tNAME\n")

-- In simplified OpenCoudou, we don't have a full process system
-- Show basic info about current execution context
local pid = "1"
local name = os.getenv("_") or "/bin/sh.lua"

io.write(pid .. "\t" .. name .. "\n")

-- If process library is available with list
local ok, proc = pcall(require, "process")
if ok and proc and proc.list then
  for k, v in pairs(proc.list) do
    local pname = v.path or v.name or "unknown"
    io.write(tostring(k):sub(1, 8) .. "\t" .. pname .. "\n")
  end
end
""".trimIndent()

    // ================================================================
    // /bin/rmdir.lua - Remove empty directory
    // ================================================================
    val RMDIR_LUA = """
local shell = require("shell")
local fs = require("filesystem")
local args, options = shell.parse(...)

if options.help then
  io.write("Usage: rmdir [-pqv] <directory>...\n")
  io.write("  -p, --parents  remove directory and its empty ancestors\n")
  io.write("  -q  ignore failures due to non-empty directories\n")
  io.write("  -v, --verbose  output a diagnostic for each directory\n")
  return
end

if #args == 0 then
  io.stderr:write("rmdir: missing operand\n")
  return 1
end

local parents = options.p or options.parents
local quiet = options.q
local verbose = options.v or options.verbose

local function isEmpty(path)
  for _ in fs.list(path) do
    return false
  end
  return true
end

local function removeDir(path)
  if not fs.exists(path) then
    if not quiet then
      io.stderr:write("rmdir: " .. path .. ": No such file or directory\n")
    end
    return false
  end
  if not fs.isDirectory(path) then
    io.stderr:write("rmdir: " .. path .. ": Not a directory\n")
    return false
  end
  if not isEmpty(path) then
    if not quiet then
      io.stderr:write("rmdir: " .. path .. ": Directory not empty\n")
    end
    return false
  end
  
  if verbose then
    io.write("rmdir: removing directory, " .. path .. "\n")
  end
  return fs.remove(path)
end

local exitCode = 0
for _, arg in ipairs(args) do
  local path = shell.resolve(arg) or arg
  if not removeDir(path) then
    exitCode = 1
  elseif parents then
    -- Remove parent directories
    local parent = fs.path(path):gsub("/$", "")
    while parent and #parent > 1 do
      if not isEmpty(parent) then break end
      if not removeDir(parent) then break end
      parent = fs.path(parent):gsub("/$", "")
    end
  end
end
return exitCode
""".trimIndent()

    // ================================================================
    // /bin/tree.lua - Directory tree
    // ================================================================
    val TREE_LUA = """
local shell = require("shell")
local fs = require("filesystem")
local args, options = shell.parse(...)

if options.help then
  io.write("Usage: tree [-ahlp] [--level=N] [path]\n")
  io.write("  -a  show hidden files\n")
  io.write("  -l  use long listing format (show sizes)\n")
  io.write("  -h  human-readable sizes (with -l)\n")
  io.write("  -p  append '/' to directories\n")
  io.write("  --level=N  descend only N levels deep\n")
  return
end

local showHidden = options.a
local longFormat = options.l
local humanSizes = options.h
local showSlash = options.p
local maxLevel = tonumber(options.level) or math.huge

local path = args[1] or "."
path = shell.resolve(path) or path

if not fs.exists(path) then
  io.stderr:write("tree: " .. path .. ": No such file or directory\n")
  return 1
end

local function humanSize(bytes)
  if bytes < 1024 then return bytes .. "" end
  if bytes < 1024 * 1024 then return string.format("%.1fK", bytes / 1024) end
  return string.format("%.1fM", bytes / (1024 * 1024))
end

local dirCount, fileCount = 0, 0

local function printTree(dir, prefix, level)
  if level > maxLevel then return end
  
  local entries = {}
  for file in fs.list(dir) do
    if showHidden or file:sub(1, 1) ~= "." then
      entries[#entries + 1] = file
    end
  end
  table.sort(entries)
  
  for i, file in ipairs(entries) do
    local isLast = (i == #entries)
    local branch = isLast and "└── " or "├── "
    local full = fs.concat(dir, file)
    local isDir = fs.isDirectory(full)
    local name = file
    
    if isDir then
      dirCount = dirCount + 1
      if showSlash then name = name:gsub("/$", "") .. "/" end
    else
      fileCount = fileCount + 1
    end
    
    local line = prefix .. branch .. name
    if longFormat and not isDir then
      local sz = fs.size(full) or 0
      local szStr = humanSizes and humanSize(sz) or tostring(sz)
      line = prefix .. branch .. "[" .. szStr .. "]  " .. name
    end
    io.write(line .. "\n")
    
    if isDir then
      local newPrefix = prefix .. (isLast and "    " or "│   ")
      printTree(full, newPrefix, level + 1)
    end
  end
end

io.write(path .. "\n")
if fs.isDirectory(path) then
  printTree(path, "", 1)
end
io.write("\n" .. dirCount .. " directories, " .. fileCount .. " files\n")
""".trimIndent()

    // ================================================================
    // /bin/unset.lua - Unset environment variable
    // ================================================================
    val UNSET_LUA = """
local args = {...}

if #args == 0 then
  io.write("Usage: unset <varname> [<varname2>...]\n")
  return 1
end

for _, k in ipairs(args) do
  os.setenv(k, nil)
end
""".trimIndent()

    // ================================================================
    // /bin/useradd.lua - Add user (stub - not supported)
    // ================================================================
    val USERADD_LUA = """
local args = {...}

if #args == 0 then
  io.write("Usage: useradd <username>\n")
  return 1
end

-- User management is not supported in simplified OpenCoudou
-- This is a stub for compatibility
io.stderr:write("useradd: user management not supported\n")
io.stderr:write("OpenCoudou runs in single-user mode\n")
return 1
""".trimIndent()

    // ================================================================
    // /bin/userdel.lua - Delete user (stub - not supported)
    // ================================================================
    val USERDEL_LUA = """
local args = {...}

if #args == 0 then
  io.write("Usage: userdel <username>\n")
  return 1
end

-- User management is not supported in simplified OpenCoudou
-- This is a stub for compatibility
io.stderr:write("userdel: user management not supported\n")
io.stderr:write("OpenCoudou runs in single-user mode\n")
return 1
""".trimIndent()

    // ================================================================
    // /bin/yes.lua - Repeatedly output a string
    // ================================================================
    val YES_LUA = """
local shell = require("shell")
local args, options = shell.parse(...)

if options.help or options.h then
  io.write("Usage: yes [string]...\n")
  io.write("Repeatedly output a line with STRING, or 'y'.\n")
  return
end

if options.version or options.V then
  io.write("yes (OpenCoudou) 1.0\n")
  return
end

local msg = #args > 0 and table.concat(args, " ") or "y"
msg = msg .. "\n"

while io.write(msg) do
  if io.stdout.tty then
    os.sleep(0)
  end
end
""".trimIndent()

    // ================================================================
    // /lib/note.lua - Music note frequencies for computer.beep
    // ================================================================
    val NOTE_LUA = """
-- Provides all music notes in range of computer.beep in MIDI and frequency form
-- Author: Vexatos
local computer = require("computer")

local note = {}
-- The table that maps note names to their respective MIDI codes
local notes = {}
-- The reversed table "notes"
local reverseNotes = {}

do
  -- All the base notes
  local tempNotes = {
    "c", "c#", "d", "d#", "e", "f", "f#", "g", "g#", "a", "a#", "b"
  }
  -- The table containing all the standard notes and # semitones in correct order
  local sNotes = {}
  -- The table containing all the b semitones
  local bNotes = {}

  -- Registers all possible notes in order
  do
    table.insert(sNotes, "a0")
    table.insert(sNotes, "a#0")
    table.insert(bNotes, "bb0")
    table.insert(sNotes, "b0")
    for i = 1, 6 do
      for _, v in ipairs(tempNotes) do
        table.insert(sNotes, v .. tostring(i))
        if #v == 1 and v ~= "c" and v ~= "f" then
          table.insert(bNotes, v .. "b" .. tostring(i))
        end
      end
    end
  end
  for i = 21, 95 do
    notes[sNotes[i - 20]] = tostring(i)
  end

  -- Reversing the whole table in reverseNotes, used for note.get
  do
    for k, v in pairs(notes) do
      reverseNotes[tonumber(v)] = k
    end
  end

  -- This is registered after reverseNotes to avoid conflicts
  for k, v in ipairs(bNotes) do
    notes[v] = tostring(notes[string.gsub(v, "(.)b(.)", "%1%2")] - 1)
  end
end

-- Converts string or frequency into MIDI code
function note.midi(n)
  if type(n) == "string" then
    n = string.lower(n)
    if tonumber(notes[n]) ~= nil then
      return tonumber(notes[n])
    else
      error("Wrong input " .. tostring(n) .. " given to note.midi, needs to be <note>[semitone sign]<octave>, e.g. A#0 or Gb4")
    end
  elseif type(n) == "number" then
    return math.floor((12 * math.log(n / 440, 2)) + 69)
  else
    error("Wrong input " .. tostring(n) .. " given to note.midi, needs to be a number or a string")
  end
end

-- Converts String or MIDI code into frequency
function note.freq(n)
  if type(n) == "string" then
    n = string.lower(n)
    if tonumber(notes[n]) ~= nil then
      return math.pow(2, (tonumber(notes[n]) - 69) / 12) * 440
    else
      error("Wrong input " .. tostring(n) .. " given to note.freq, needs to be <note>[semitone sign]<octave>, e.g. A#0 or Gb4", 2)
    end
  elseif type(n) == "number" then
    return math.pow(2, (n - 69) / 12) * 440
  else
    error("Wrong input " .. tostring(n) .. " given to note.freq, needs to be a number or a string", 2)
  end
end

-- Converts a MIDI value back into a string
function note.name(n)
  n = tonumber(n)
  if reverseNotes[n] then
    return string.upper(string.match(reverseNotes[n], "^(.)")) .. string.gsub(reverseNotes[n], "^.(.*)", "%1")
  else
    error("Attempt to get a note for a non-existing MIDI code", 2)
  end
end

-- Converts Note block ticks (0-24) to MIDI code (34-58) and vice-versa
function note.ticks(n)
  if type(n) == "number" then
    if n >= 0 and n <= 24 then
      return n + 34
    elseif n >= 34 and n <= 58 then
      return n - 34
    else
      error("Wrong input " .. tostring(n) .. " given to note.ticks, needs to be a number [0-24 or 34-58]", 2)
    end
  else
    error("Wrong input " .. tostring(n) .. " given to note.ticks, needs to be a number", 2)
  end
end

-- Plays a tone, input is either the note as a string or the MIDI code as well as the duration of the tone
function note.play(tone, duration)
  computer.beep(note.freq(tone), duration)
end

return note
""".trimIndent()

    // ================================================================
    // /lib/internet.lua - High-level internet wrapper
    // ================================================================
    val INTERNET_LUA = """
local buffer = require("buffer")
local component = require("component")

local internet = {}

-------------------------------------------------------------------------------

function internet.request(url, data, headers, method)
  checkArg(1, url, "string")
  checkArg(2, data, "string", "table", "nil")
  checkArg(3, headers, "table", "nil")
  checkArg(4, method, "string", "nil")

  if not component.isAvailable("internet") then
    error("no primary internet card found", 2)
  end
  local inet = component.internet

  local post
  if type(data) == "string" then
    post = data
  elseif type(data) == "table" then
    for k, v in pairs(data) do
      post = post and (post .. "&") or ""
      post = post .. tostring(k) .. "=" .. tostring(v)
    end
  end

  local request, reason = inet.request(url, post, headers, method)
  if not request then
    error(reason, 2)
  end

  return setmetatable(
  {
    ["()"] = "function():string -- Tries to read data from the socket stream and return the read byte array.",
    close = setmetatable({}, {
      __call = request.close,
      __tostring = function() return "function() -- closes the connection" end
    })
  },
  {
    __call = function()
      while true do
        local data, reason = request.read()
        if not data then
          request.close()
          if reason then
            error(reason, 2)
          else
            return nil -- eof
          end
        elseif #data > 0 then
          return data
        end
        -- else: no data, block
        os.sleep(0)
      end
    end,
    __index = request,
  })
end

-------------------------------------------------------------------------------

local socketStream = {}

function socketStream:close()
  if self.socket then
    self.socket.close()
    self.socket = nil
  end
end

function socketStream:seek()
  return nil, "bad file descriptor"
end

function socketStream:read(n)
  if not self.socket then
    return nil, "connection is closed"
  end
  return self.socket.read(n)
end

function socketStream:write(value)
  if not self.socket then
    return nil, "connection is closed"
  end
  while #value > 0 do
    local written, reason = self.socket.write(value)
    if not written then
      return nil, reason
    end
    value = string.sub(value, written + 1)
  end
  return true
end

function internet.socket(address, port)
  checkArg(1, address, "string")
  checkArg(2, port, "number", "nil")
  if port then
    address = address .. ":" .. port
  end

  local inet = component.internet
  local socket, reason = inet.connect(address)
  if not socket then
    return nil, reason
  end

  local stream = {inet = inet, socket = socket}
  local metatable = {__index = socketStream, __metatable = "socketstream"}
  return setmetatable(stream, metatable)
end

function internet.open(address, port)
  local stream, reason = internet.socket(address, port)
  if not stream then
    return nil, reason
  end
  return buffer.new("rwb", stream)
end

-------------------------------------------------------------------------------

return internet
""".trimIndent()

    // ================================================================
    // /lib/transforms.lua - Data transformation utilities
    // ================================================================
    val TRANSFORMS_LUA = """
local lib = {}
lib.internal = {}

function lib.internal.range_adjust(f, l, s)
  checkArg(1, f, 'number', 'nil')
  checkArg(2, l, 'number', 'nil')
  checkArg(3, s, 'number')
  if f == nil then f = 1 elseif f < 0 then f = s + f + 1 end
  if l == nil then l = s elseif l < 0 then l = s + l + 1 end
  return f, l
end

function lib.internal.table_view(tbl, f, l)
  return setmetatable({}, {
    __index = function(_, key)
      return (type(key) ~= 'number' or (key >= f and key <= l)) and tbl[key] or nil
    end,
    __len = function(_)
      return l
    end,
  })
end

local adjust = lib.internal.range_adjust
local view = lib.internal.table_view

-- first(p1,p2) searches for the first range in p1 that satisfies p2
function lib.first(tbl, pred, f, l)
  checkArg(1, tbl, 'table')
  checkArg(2, pred, 'function', 'table')
  if type(pred) == 'table' then
    local set; set, pred = pred, function(e, fi, tbl)
      for vi = 1, #set do
        local v = set[vi]
        if lib.begins(tbl, v, fi) then return true, #v end
      end
    end
  end
  local s = #tbl
  f, l = adjust(f, l, s)
  tbl = view(tbl, f, l)
  for i = f, l do
    local si, ei = pred(tbl[i], i, tbl)
    if si then
      return i, i + (ei or 1) - 1
    end
  end
end

-- returns true if p1 at first p3 equals element for element p2
function lib.begins(tbl, v, f, l)
  checkArg(1, tbl, 'table')
  checkArg(2, v, 'table')
  local vs = #v
  f, l = adjust(f, l, #tbl)
  if vs > (l - f + 1) then return end
  for i = 1, vs do
    if tbl[f + i - 1] ~= v[i] then return end
  end
  return true
end

function lib.concat(...)
  local r, rn, k = {}, 0
  for _, tbl in ipairs({...}) do
    if type(tbl) ~= 'table' then
      return nil, 'parameter ' .. tostring(_) .. ' to concat is not a table'
    end
    local n = tbl.n or #tbl
    k = k or tbl.n
    for i = 1, n do
      rn = rn + 1; r[rn] = tbl[i]
    end
  end
  r.n = k and rn or nil
  return r
end

-- partition splits a table by a separator
function lib.partition(tbl, sep, f, l)
  checkArg(1, tbl, 'table')
  checkArg(2, sep, 'table', 'function')
  local s = #tbl
  f, l = adjust(f, l, s)
  local result = {}
  local current = {}
  for i = f, l do
    local is_sep = false
    if type(sep) == 'table' then
      for _, v in ipairs(sep) do
        if tbl[i] == v then is_sep = true; break end
      end
    else
      is_sep = sep(tbl[i], i, tbl)
    end
    if is_sep then
      if #current > 0 then
        result[#result + 1] = current
        current = {}
      end
    else
      current[#current + 1] = tbl[i]
    end
  end
  if #current > 0 then
    result[#result + 1] = current
  end
  return result
end

-- sub returns a sub-table
function lib.sub(tbl, f, l)
  checkArg(1, tbl, 'table')
  local s = #tbl
  f, l = adjust(f, l, s)
  local result = {}
  for i = f, l do
    result[#result + 1] = tbl[i]
  end
  return result
end

return lib
""".trimIndent()

    // ================================================================
    // /lib/tty.lua - Terminal type utilities
    // ================================================================
    val TTY_LUA = """
local event = require("event")
local component = require("component")
local computer = require("computer")

local tty = {}
tty.window = {
  fullscreen = true,
  blink = true,
  dx = 0,
  dy = 0,
  x = 1,
  y = 1,
  output_buffer = "",
}

tty.stream = {}

local screen_cache = {}
local function screen_reset(gpu, addr)
  screen_cache[addr or gpu.getScreen() or false] = nil
end

event.listen("screen_resized", screen_reset)

function tty.getViewport()
  local window = tty.window
  local screen = tty.screen()
  if window.fullscreen and screen and not screen_cache[screen] then
    screen_cache[screen] = true
    window.width, window.height = window.gpu.getViewport()
  end
  return window.width, window.height, window.dx, window.dy, window.x, window.y
end

function tty.setViewport(width, height, dx, dy, x, y)
  checkArg(1, width, "number")
  checkArg(2, height, "number")
  local window = tty.window
  dx, dy, x, y = dx or 0, dy or 0, x or 1, y or 1
  window.width, window.height, window.dx, window.dy, window.x, window.y = width, height, dx, dy, x, y
end

function tty.gpu()
  return tty.window.gpu
end

function tty.clear()
  tty.stream.scroll(math.huge)
  tty.setCursor(1, 1)
end

function tty.isAvailable()
  local gpu = tty.gpu()
  return not not (gpu and gpu.getScreen())
end

function tty.stream.read()
  -- Simplified read implementation
  local buffer = ""
  while true do
    local ev, _, char, code = event.pull("key_down")
    if char == 13 then -- Enter
      io.write("\n")
      return buffer .. "\n"
    elseif char == 8 then -- Backspace
      if #buffer > 0 then
        buffer = buffer:sub(1, -2)
        local x, y = tty.getCursor()
        tty.setCursor(x - 1, y)
        tty.gpu().set(x - 1 + tty.window.dx, y + tty.window.dy, " ")
      end
    elseif char >= 32 and char < 127 then
      buffer = buffer .. string.char(char)
      io.write(string.char(char))
    end
  end
end

function tty.stream:write(value)
  local gpu = tty.gpu()
  if not gpu then
    return
  end
  local window = tty.window
  
  for i = 1, #value do
    local char = value:sub(i, i)
    if char == "\n" then
      window.x = 1
      window.y = window.y + 1
      if window.y > window.height then
        self.scroll(1)
        window.y = window.height
      end
    elseif char == "\r" then
      window.x = 1
    elseif char == "\t" then
      window.x = ((window.x - 1) - ((window.x - 1) % 8)) + 9
    elseif char == "\b" then
      window.x = math.max(1, window.x - 1)
    elseif char == "\a" then
      computer.beep()
    else
      gpu.set(window.x + window.dx, window.y + window.dy, char)
      window.x = window.x + 1
      if window.x > window.width then
        window.x = 1
        window.y = window.y + 1
        if window.y > window.height then
          self.scroll(1)
          window.y = window.height
        end
      end
    end
  end
  return 0
end

function tty.getCursor()
  local window = tty.window
  return window.x, window.y
end

function tty.setCursor(x, y)
  checkArg(1, x, "number")
  checkArg(2, y, "number")
  local window = tty.window
  window.x, window.y = x, y
end

local gpu_intercept = {}
function tty.bind(gpu)
  checkArg(1, gpu, "table")
  if not gpu_intercept[gpu] then
    gpu_intercept[gpu] = true
    local setr, setv = gpu.setResolution, gpu.setViewport
    gpu.setResolution = function(...)
      screen_reset(gpu)
      return setr(...)
    end
    gpu.setViewport = function(...)
      screen_reset(gpu)
      return setv(...)
    end
  end
  local window = tty.window
  if window.gpu ~= gpu then
    window.gpu = gpu
    window.keyboard = nil
    tty.getViewport()
  end
  screen_reset(gpu)
end

function tty.keyboard()
  local window = tty.window
  if window.keyboard then
    return window.keyboard
  end
  local system_keyboard = component.isAvailable("keyboard") and component.keyboard
  system_keyboard = system_keyboard and system_keyboard.address or "no_system_keyboard"
  local screen = tty.screen()
  if not screen then
    return system_keyboard
  end
  if component.isAvailable("screen") and component.screen.address == screen then
    window.keyboard = system_keyboard
  else
    window.keyboard = component.invoke(screen, "getKeyboards")[1] or system_keyboard
  end
  return window.keyboard
end

function tty.screen()
  local gpu = tty.gpu()
  if not gpu then
    return nil
  end
  return gpu.getScreen()
end

function tty.stream.scroll(lines)
  local gpu = tty.gpu()
  if not gpu then
    return 0
  end
  local width, height, dx, dy, x, y = tty.getViewport()
  if not lines then
    if y < 1 then
      lines = y - 1
    elseif y > height then
      lines = y - height
    else
      return 0
    end
  end
  lines = math.min(lines, height)
  lines = math.max(lines, -height)
  local abs_lines = math.abs(lines)
  local box_height = height - abs_lines
  local fill_top = dy + 1 + (lines < 0 and 0 or box_height)
  gpu.copy(dx + 1, dy + 1 + math.max(0, lines), width, box_height, 0, -lines)
  gpu.fill(dx + 1, fill_top, width, abs_lines, ' ')
  tty.setCursor(x, math.max(1, math.min(y, height)))
  return lines
end

local function bfd() return nil, "tty: invalid operation" end
tty.stream.close = bfd
tty.stream.seek = bfd
tty.stream.handle = "tty"

return tty
""".trimIndent()

    // ================================================================
    // /lib/sh.lua - Shell utilities library
    // ================================================================
    val SH_LIB_LUA = """
local process = require("process")
local shell = require("shell")
local text = require("text")

local sh = {}
sh.internal = {}

function sh.internal.isWordOf(w, vs)
  if not w or #w ~= 1 or w[1].qr then return false end
  for _, v in ipairs(vs) do
    if type(v) == "table" and #v == 1 and v[1] == w[1].txt then
      return true
    elseif type(v) == "string" and v == w[1].txt then
      return true
    end
  end
  return false
end

local isWordOf = sh.internal.isWordOf

-------------------------------------------------------------------------------
-- SH API

sh.internal.ec = {}
sh.internal.ec.parseCommand = 127
sh.internal.ec.last = 0

function sh.getLastExitCode()
  return sh.internal.ec.last
end

function sh.internal.command_result_as_code(ec, reason)
  local code
  if ec == false then
    code = 1
  elseif ec == nil or ec == true then
    code = 0
  elseif type(ec) ~= "number" then
    code = 2
  else
    code = ec
  end
  if reason and code ~= 0 then io.stderr:write(reason, "\n") end
  return code
end

function sh.internal.resolveActions(input, resolved)
  resolved = resolved or {}
  local processed = {}
  local prev_was_delim = true
  
  -- Simple tokenization
  local words = {}
  for word in input:gmatch("%S+") do
    words[#words + 1] = {{txt = word, qr = false}}
  end
  
  while #words > 0 do
    local next = table.remove(words, 1)
    if isWordOf(next, {";", "&&", "||", "|"}) then
      prev_was_delim = true
      resolved = {}
    elseif prev_was_delim then
      prev_was_delim = false
      if next and #next == 1 and not next[1].qr then
        local key = next[1].txt
        if key == "!" then
          prev_was_delim = true
        elseif not resolved[key] then
          resolved[key] = shell.getAlias(key)
          local value = resolved[key]
          if value and key ~= value then
            local replacement_tokens = sh.internal.resolveActions(value, resolved)
            if replacement_tokens then
              for i = #replacement_tokens, 1, -1 do
                table.insert(words, 1, replacement_tokens[i])
              end
              next = table.remove(words, 1)
            end
          end
        end
      end
    end
    table.insert(processed, next)
  end
  return processed
end

function sh.internal.isIdentifier(key)
  if type(key) ~= "string" then
    return false
  end
  return key:match("^[%a_][%w_]*${'$'}") == key
end

function sh.expand(value)
  local expanded = value
    :gsub("%${'$'}([_%w%?]+)", function(key)
      if key == "?" then
        return tostring(sh.getLastExitCode())
      end
      return os.getenv(key) or ''
    end)
    :gsub("%${'$'}{(.*)}", function(key)
      if sh.internal.isIdentifier(key) then
        return os.getenv(key) or ''
      end
      io.stderr:write("${'$'}{" .. key .. "}: bad substitution\n")
      os.exit(1)
    end)
  return expanded
end

function sh.execute(env, command, ...)
  checkArg(2, command, "string")
  if command:find("^%s*#") then return true, 0 end
  
  local words = sh.internal.resolveActions(command)
  if type(words) ~= "table" then
    return words
  elseif #words == 0 then
    return true
  end
  
  -- Simple execution - extract command and args
  local args = {}
  for _, word in ipairs(words) do
    if word and #word > 0 and word[1].txt then
      args[#args + 1] = sh.expand(word[1].txt)
    end
  end
  
  if #args == 0 then return true end
  
  local cmd = table.remove(args, 1)
  local path = shell.resolve(cmd)
  
  if path then
    local result, reason = shell.execute(path, env, table.unpack(args))
    sh.internal.ec.last = sh.internal.command_result_as_code(result, reason)
    return result, reason
  else
    io.stderr:write(cmd .. ": command not found\n")
    sh.internal.ec.last = 127
    return false, "command not found"
  end
end

function sh.hintHandler(full_line, cursor)
  return nil -- Simplified: no tab completion
end

return sh
""".trimIndent()

    // ================================================================
    // /lib/vt100.lua - VT100 escape sequence parsing
    // ================================================================
    val VT100_LUA = """
local text = require("text")

local rules = {}
local vt100 = {rules = rules}

-- colors, blinking, and reverse
-- [%d+;%d+;..%d+m
rules[{"%[", "[%d;]*", "m"}] = function(window, _, number_text)
  local colors = {0x0, 0xff0000, 0x00ff00, 0xffff00, 0x0000ff, 0xff00ff, 0x00B6ff, 0xffffff}
  local fg, bg = window.gpu.setForeground, window.gpu.setBackground
  if window.flip then
    fg, bg = bg, fg
  end
  number_text = " _ " .. number_text:gsub("^;${'$'}", ""):gsub(";", " _ ") .. " _ "
  
  for num_str in number_text:gmatch("%d+") do
    local num = tonumber(num_str)
    local flip = num == 7
    if flip then
      if not window.flip then
        local rgb, pal = bg(window.gpu.getForeground())
        fg(pal or rgb, not not pal)
        fg, bg = bg, fg
      end
    elseif num == 5 then
      window.blink = true
    elseif num == 0 then
      bg(colors[1])
      fg(colors[8])
    elseif num then
      num = num - 29
      local set = fg
      if num > 10 then
        num = num - 10
        set = bg
      end
      local color = colors[num]
      if color then
        set(color)
      end
    end
    window.flip = flip
  end
end

local function save_attributes(window, seven, s)
  if seven == "7" or s == "s" then
    window.saved = {
      window.x,
      window.y,
      {window.gpu.getBackground()},
      {window.gpu.getForeground()},
      window.flip,
      window.blink
    }
  else
    local data = window.saved or {1, 1, {0x0}, {0xffffff}, window.flip, window.blink}
    window.x = data[1]
    window.y = data[2]
    window.gpu.setBackground(table.unpack(data[3]))
    window.gpu.setForeground(table.unpack(data[4]))
    window.flip = data[5]
    window.blink = data[6]
  end
end

-- 7 save cursor position and attributes
-- 8 restore cursor position and attributes
rules[{"[78]"}] = save_attributes

-- s save cursor position
-- u restore cursor position
rules[{"%[", "[su]"}] = save_attributes

-- Cursor movement
rules[{"%[", "%d*", "A"}] = function(window, _, n)
  window.y = math.max(1, window.y - (tonumber(n) or 1))
end

rules[{"%[", "%d*", "B"}] = function(window, _, n)
  window.y = window.y + (tonumber(n) or 1)
end

rules[{"%[", "%d*", "C"}] = function(window, _, n)
  window.x = window.x + (tonumber(n) or 1)
end

rules[{"%[", "%d*", "D"}] = function(window, _, n)
  window.x = math.max(1, window.x - (tonumber(n) or 1))
end

-- Cursor position
rules[{"%[", "%d*;%d*", "H"}] = function(window, _, pos)
  local y, x = pos:match("(%d*);?(%d*)")
  window.y = tonumber(y) or 1
  window.x = tonumber(x) or 1
end

rules[{"%[", "%d*;%d*", "f"}] = rules[{"%[", "%d*;%d*", "H"}]

-- Clear screen
rules[{"%[", "%d*", "J"}] = function(window, _, n)
  n = tonumber(n) or 0
  local gpu = window.gpu
  local w, h = gpu.getViewport()
  if n == 0 then
    gpu.fill(window.x, window.y, w - window.x + 1, 1, " ")
    if window.y < h then
      gpu.fill(1, window.y + 1, w, h - window.y, " ")
    end
  elseif n == 1 then
    gpu.fill(1, 1, w, window.y - 1, " ")
    gpu.fill(1, window.y, window.x, 1, " ")
  elseif n == 2 then
    gpu.fill(1, 1, w, h, " ")
  end
end

-- Clear line
rules[{"%[", "%d*", "K"}] = function(window, _, n)
  n = tonumber(n) or 0
  local gpu = window.gpu
  local w = gpu.getViewport()
  if n == 0 then
    gpu.fill(window.x, window.y, w - window.x + 1, 1, " ")
  elseif n == 1 then
    gpu.fill(1, window.y, window.x, 1, " ")
  elseif n == 2 then
    gpu.fill(1, window.y, w, 1, " ")
  end
end

-- returns: anything that failed to parse
function vt100.parse(window)
  if window.output_buffer:sub(1, 1) ~= "\27" then
    return ""
  end
  local any_valid

  for rule, action in pairs(rules) do
    local last_index = 1
    local captures = {}
    for _, pattern in ipairs(rule) do
      if last_index >= #window.output_buffer then
        any_valid = true
        break
      end
      local si, ei, capture = window.output_buffer:find("^(" .. pattern .. ")", last_index + 1)
      if not si then
        break
      end
      captures[#captures + 1] = capture
      last_index = ei
    end

    if #captures == #rule then
      action(window, table.unpack(captures))
      window.output_buffer = window.output_buffer:sub(last_index + 1)
      return ""
    end
  end

  if not any_valid then
    window.output_buffer = window.output_buffer:sub(2)
    return "\27"
  end
end

return vt100
""".trimIndent()

    // ================================================================
    // /lib/pipe.lua - Pipe utilities
    // ================================================================
    val PIPE_LUA = """
local process = require("process")
local buffer = require("buffer")

local pipe = {}

-- Simple pipe stream for connecting processes
local pipe_stream = {
  continue = function(self, exit)
    if self.closed then
      return self
    end
    if self.next and coroutine.status(self.next) == "dead" then
      self:close()
    end
    return self
  end,
  
  close = function(self)
    self.closed = true
  end,
  
  seek = function()
    return nil, "bad file descriptor"
  end,
  
  write = function(self, value)
    if self.closed then
      return nil, "attempt to use a closed stream"
    end
    self.buffer = (self.buffer or "") .. value
    return self
  end,
  
  read = function(self, n)
    if self.closed and (not self.buffer or #self.buffer == 0) then
      return nil
    end
    if not self.buffer or #self.buffer == 0 then
      self.read_mode = true
      return ""
    end
    self.read_mode = false
    if n then
      local result = self.buffer:sub(1, n)
      self.buffer = self.buffer:sub(n + 1)
      return result
    else
      local result = self.buffer
      self.buffer = ""
      return result
    end
  end
}

function pipe.create()
  local stream = {
    buffer = "",
    closed = false,
    read_mode = false
  }
  return setmetatable(stream, {__index = pipe_stream})
end

function pipe.createCoroutineStack(root, env, name)
  checkArg(1, root, "thread", "function")
  
  if type(root) == "function" then
    root = coroutine.create(root)
  end
  
  local pco = {root = root}
  
  function pco.yield(...)
    return coroutine.yield(nil, ...)
  end
  
  function pco.yield_past(co, ...)
    return coroutine.yield(co, ...)
  end
  
  function pco.resume(co, ...)
    checkArg(1, co, "thread")
    return coroutine.resume(co, ...)
  end
  
  return pco
end

function pipe.buildPipeChain(threads)
  if #threads < 2 then return end
  
  for i = 1, #threads - 1 do
    local p = pipe.create()
    p.next = threads[i + 1]
    -- Connect stdout of thread i to stdin of thread i+1
  end
end

return pipe
""".trimIndent()

    // ================================================================
    // /lib/bit32.lua - Bit manipulation (Lua 5.3 compatibility)
    // ================================================================
    val BIT32_LUA = """
-- Backwards compat for Lua 5.3; provides bit32 API
-- In Lua 5.2, bit32 is built-in

local bit32 = {}

-------------------------------------------------------------------------------

local function fold(init, op, ...)
  local result = init
  local args = table.pack(...)
  for i = 1, args.n do
    result = op(result, args[i])
  end
  return result
end

local function trim(n)
  return n & 0xFFFFFFFF
end

local function mask(w)
  return ~(0xFFFFFFFF << w)
end

function bit32.arshift(x, disp)
  return x // (2 ^ disp)
end

function bit32.band(...)
  return fold(0xFFFFFFFF, function(a, b) return a & b end, ...)
end

function bit32.bnot(x)
  return ~x & 0xFFFFFFFF
end

function bit32.bor(...)
  return fold(0, function(a, b) return a | b end, ...)
end

function bit32.btest(...)
  return bit32.band(...) ~= 0
end

function bit32.bxor(...)
  return fold(0, function(a, b) return a ~ b end, ...)
end

local function fieldargs(f, w)
  w = w or 1
  assert(f >= 0, "field cannot be negative")
  assert(w > 0, "width must be positive")
  assert(f + w <= 32, "trying to access non-existent bits")
  return f, w
end

function bit32.extract(n, field, width)
  local f, w = fieldargs(field, width)
  return (n >> f) & mask(w)
end

function bit32.replace(n, v, field, width)
  local f, w = fieldargs(field, width)
  local m = mask(w)
  return (n & ~(m << f)) | ((v & m) << f)
end

function bit32.lrotate(x, disp)
  if disp == 0 then
    return x
  elseif disp < 0 then
    return bit32.rrotate(x, -disp)
  else
    disp = disp & 31
    x = trim(x)
    return trim((x << disp) | (x >> (32 - disp)))
  end
end

function bit32.lshift(x, disp)
  return trim(x << disp)
end

function bit32.rrotate(x, disp)
  if disp == 0 then
    return x
  elseif disp < 0 then
    return bit32.lrotate(x, -disp)
  else
    disp = disp & 31
    x = trim(x)
    return trim((x >> disp) | (x << (32 - disp)))
  end
end

function bit32.rshift(x, disp)
  return trim(x >> disp)
end

-------------------------------------------------------------------------------

return bit32
""".trimIndent()

}
