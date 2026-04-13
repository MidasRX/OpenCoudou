package li.cil.oc.server.machine

import li.cil.oc.api.machine.*
import li.cil.oc.server.fs.VirtualFileSystem
import li.cil.oc.server.fs.OpenOSContent
import li.cil.oc.util.OCLogger
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import org.luaj.vm2.*
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * LuaJ-based architecture for running Lua code on OpenCoudou computers.
 * Implements full OpenComputers-compatible APIs:
 * - component.list, component.type, component.invoke, component.proxy
 * - computer.address, computer.uptime, computer.pullSignal, computer.pushSignal, computer.shutdown, computer.beep
 * - gpu.* (set, fill, setForeground, setBackground, getResolution, copy, get)
 * - internet.request (HTTP GET for Pastebin etc)
 * - os.clock, os.time
 * - unicode.* basics
 */
class SimpleLuaArchitecture(override val machine: Machine) : Architecture {

    private var globals: Globals? = null
    private var mainCoroutine: LuaThread? = null
    private var initialized = false
    // Signal delivered to pullSignal
    private var pendingSignal: Varargs? = null
    private var lastResumeTime: Long = System.currentTimeMillis()
    private val MAX_EXECUTION_TIME_MS = 5000L // 5 seconds without yielding

    override val isInitialized: Boolean get() = initialized

    override fun initialize(): Boolean {
        try {
            globals = createSandboxedGlobals()
            setupComponentAPI()
            setupComputerAPI()
            setupOSAPI()
            setupGpuAPI()
            setupInternetAPI()
            setupUnicodeAPI()
            initialized = true
            return true
        } catch (e: Exception) {
            machine.crash("Lua init failed: ${e.message}")
            return false
        }
    }

    // ===========================
    // Globals
    // ===========================
    private fun createSandboxedGlobals(): Globals {
        val g = Globals()
        g.load(JseBaseLib())
        g.load(PackageLib())
        g.load(Bit32Lib())
        g.load(TableLib())
        g.load(StringLib())
        g.load(JseMathLib())
        g.load(CoroutineLib())
        g.load(DebugLib())  // For instruction count hooks (execution timeout)

        // Install execution timeout hook - prevents infinite loops from freezing the server
        val hookFunction = object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val elapsed = System.currentTimeMillis() - lastResumeTime
                if (elapsed > MAX_EXECUTION_TIME_MS) {
                    LuaValue.error("too long without yielding")
                }
                return LuaValue.NONE
            }
        }
        // Check every 10000 instructions
        g.get("debug").get("sethook").invoke(LuaValue.varargsOf(arrayOf(
            hookFunction, LuaValue.valueOf(""), LuaValue.valueOf(10000)
        )))

        // Sandboxed debug library (only traceback and getinfo)
        val debug = LuaTable()
        debug.set("traceback", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val message = args.optjstring(1, "")
                val level = args.optint(2, 1)
                return LuaValue.valueOf(message ?: "")
            }
        })
        debug.set("getinfo", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val info = LuaTable()
                info.set("source", LuaValue.valueOf("=?"))
                info.set("short_src", LuaValue.valueOf("?"))
                info.set("what", LuaValue.valueOf("Lua"))
                return info
            }
        })
        g.set("debug", debug)

        g.set("dofile", LuaValue.NIL)
        g.set("loadfile", LuaValue.NIL)
        // Remove PackageLib's native loader for sandbox safety
        val pkg = g.get("package")
        if (!pkg.isnil()) {
            pkg.set("loadlib", LuaValue.NIL)
            pkg.set("searchpath", LuaValue.NIL)
        }

        // Remove string.dump (sandbox risk - can dump bytecode)
        val str = g.get("string")
        if (!str.isnil()) {
            str.set("dump", LuaValue.NIL)
        }

        // Add Lua 5.3 table functions missing from LuaJ
        val tbl = g.get("table")
        if (!tbl.isnil()) {
            // table.pack(...)
            tbl.set("pack", object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs {
                    val t = LuaTable()
                    for (i in 1..args.narg()) {
                        t.set(i, args.arg(i))
                    }
                    t.set("n", LuaValue.valueOf(args.narg()))
                    return t
                }
            })
            // table.unpack(list [, i [, j]])
            tbl.set("unpack", object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs {
                    val list = args.arg1().checktable()
                    val i = args.optint(2, 1)
                    val j = args.optint(3, list.rawlen())
                    if (i > j) return LuaValue.NONE
                    val vals = Array(j - i + 1) { idx -> list.get(i + idx) }
                    return LuaValue.varargsOf(vals)
                }
            })
            // table.move(a1, f, e, t [, a2])
            tbl.set("move", object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs {
                    val a1 = args.arg1().checktable()
                    val f = args.arg(2).checkint()
                    val e = args.arg(3).checkint()
                    val t = args.arg(4).checkint()
                    val a2 = if (args.narg() >= 5 && !args.arg(5).isnil()) args.arg(5).checktable() else a1
                    if (f <= e) {
                        if (t <= f || a2 !== a1) {
                            for (idx in 0..(e - f)) a2.set(t + idx, a1.get(f + idx))
                        } else {
                            for (idx in (e - f) downTo 0) a2.set(t + idx, a1.get(f + idx))
                        }
                    }
                    return a2
                }
            })
        }

        // Add Lua 5.3 math constants/functions missing from LuaJ
        val math = g.get("math")
        if (!math.isnil()) {
            math.set("maxinteger", LuaValue.valueOf(Long.MAX_VALUE.toDouble()))
            math.set("mininteger", LuaValue.valueOf(Long.MIN_VALUE.toDouble()))
            // math.tointeger(x) - returns integer if x is an integer, else nil
            math.set("tointeger", object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue {
                    if (arg.isint()) return arg
                    if (arg.isnumber()) {
                        val d = arg.todouble()
                        val l = d.toLong()
                        if (d == l.toDouble()) return LuaValue.valueOf(l.toInt())
                    }
                    return LuaValue.NIL
                }
            })
            // math.type(x) - "integer", "float", or false
            math.set("type", object : OneArgFunction() {
                override fun call(arg: LuaValue): LuaValue {
                    return when {
                        arg.isint() || arg.islong() -> LuaValue.valueOf("integer")
                        arg.isnumber() -> LuaValue.valueOf("float")
                        else -> LuaValue.FALSE
                    }
                }
            })
        }

        g.set("print", createPrintFunction())

        // Lua 5.2+ xpcall with extra arguments support (LuaJ only supports xpcall(f, handler))
        g.set("xpcall", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val f = args.arg1().checkfunction()
                val handler = args.arg(2).checkfunction()
                val fArgs = if (args.narg() > 2) args.subargs(3) else LuaValue.NONE
                return try {
                    val result = f.invoke(fArgs)
                    LuaValue.varargsOf(LuaValue.TRUE, result)
                } catch (e: LuaError) {
                    val handled = handler.call(LuaValue.valueOf(e.message ?: "error"))
                    LuaValue.varargsOf(LuaValue.FALSE, handled)
                }
            }
        })

        // coroutine.isyieldable() - Lua 5.3 (missing from LuaJ's CoroutineLib)
        val co = g.get("coroutine")
        if (!co.isnil()) {
            co.set("isyieldable", object : ZeroArgFunction() {
                override fun call(): LuaValue {
                    // In OC, code always runs inside a coroutine, so always yieldable
                    val running = co.get("running").invoke(LuaValue.NONE)
                    // coroutine.running() returns (co, isMain) - yieldable if not main
                    val isMain = running.narg() >= 2 && running.arg(2).toboolean()
                    return LuaValue.valueOf(!isMain)
                }
            })
        }

        // utf8 library (Lua 5.3)
        val utf8lib = LuaTable()
        utf8lib.set("charpattern", LuaValue.valueOf("[\u0000-\u007F\u00C2-\u00FD][\u0080-\u00BF]*"))

        // utf8.char(...) - receives zero or more integers, converts to UTF-8 string
        utf8lib.set("char", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val sb = StringBuilder()
                for (i in 1..args.narg()) sb.appendCodePoint(args.arg(i).checkint())
                return LuaValue.valueOf(sb.toString())
            }
        })

        // utf8.codepoint(s [, i [, j]]) - returns codepoints of characters in s between positions i and j
        utf8lib.set("codepoint", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val s = args.arg1().checkjstring()
                val len = s.codePointCount(0, s.length)
                val i = args.optint(2, 1)
                val j = args.optint(3, i)
                val startIdx = if (i > 0) i - 1 else len + i
                val endIdx = if (j > 0) j - 1 else len + j
                if (startIdx < 0 || endIdx < 0 || startIdx >= len || endIdx >= len || startIdx > endIdx) {
                    return LuaValue.NONE
                }
                val vals = mutableListOf<LuaValue>()
                var offset = s.offsetByCodePoints(0, startIdx)
                for (idx in startIdx..endIdx) {
                    val cp = s.codePointAt(offset)
                    vals.add(LuaValue.valueOf(cp))
                    offset += Character.charCount(cp)
                }
                return LuaValue.varargsOf(vals.toTypedArray())
            }
        })

        // utf8.len(s [, i [, j]]) - returns number of UTF-8 characters in s between positions i and j (byte positions)
        utf8lib.set("len", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val s = args.arg1().checkjstring()
                val bytes = s.toByteArray(Charsets.UTF_8)
                val i = args.optint(2, 1) - 1 // convert to 0-based
                val j = args.optint(3, bytes.size) // default to end
                val sub = try {
                    String(bytes, i, j - i, Charsets.UTF_8)
                } catch (e: Exception) {
                    return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf(i + 1)))
                }
                return LuaValue.valueOf(sub.codePointCount(0, sub.length))
            }
        })

        // utf8.codes(s) - returns an iterator function
        utf8lib.set("codes", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val s = arg.checkjstring()
                return object : VarArgFunction() {
                    private var bytePos = 0
                    override fun invoke(args: Varargs): Varargs {
                        if (bytePos >= s.length) return LuaValue.NIL
                        val cp = s.codePointAt(bytePos)
                        val pos = bytePos + 1 // 1-based
                        bytePos += Character.charCount(cp)
                        return LuaValue.varargsOf(arrayOf(
                            LuaValue.valueOf(pos),
                            LuaValue.valueOf(cp)
                        ))
                    }
                }
            }
        })

        // utf8.offset(s, n [, i]) - returns byte position of the n-th character
        utf8lib.set("offset", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val s = args.arg1().checkjstring()
                val n = args.arg(2).checkint()
                val i = args.optint(3, if (n >= 0) 1 else s.length + 1) - 1 // 0-based
                var pos = i.coerceIn(0, s.length)
                if (n > 0) {
                    var remaining = n
                    // For n >= 1, move forward n-1 characters from pos
                    remaining -= 1 // offset with n=1 at position i returns i itself
                    while (remaining > 0 && pos < s.length) {
                        val cp = s.codePointAt(pos)
                        pos += Character.charCount(cp)
                        remaining--
                    }
                    if (remaining > 0) return LuaValue.NIL
                } else if (n < 0) {
                    var remaining = -n
                    while (remaining > 0 && pos > 0) {
                        pos--
                        // skip continuation bytes
                        while (pos > 0 && (s[pos].code and 0xC0) == 0x80) pos--
                        remaining--
                    }
                    if (remaining > 0) return LuaValue.NIL
                }
                return LuaValue.valueOf(pos + 1) // 1-based
            }
        })

        g.set("utf8", utf8lib)

        // checkArg helper used by many OC programs
        g.set("checkArg", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val n = args.arg1().checkint()
                val have = args.arg(2)
                for (i in 3..args.narg()) {
                    val want = args.arg(i).checkjstring()
                    if (have.typename() == want) return LuaValue.NONE
                }
                val expected = (3..args.narg()).joinToString(" or ") { args.arg(it).tojstring() }
                LuaValue.error("bad argument #$n ($expected expected, got ${have.typename()})")
                return LuaValue.NONE
            }
        })

        // Remove global unpack (Lua 5.2+ uses table.unpack instead)
        // This matches original OC behavior
        g.set("unpack", LuaValue.NIL)

        // rawlen(v) - Lua 5.2 addition, returns length without invoking __len metamethod
        g.set("rawlen", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return when {
                    arg.isstring() -> LuaValue.valueOf(arg.checkjstring().length)
                    arg.istable() -> LuaValue.valueOf(arg.checktable().rawlen())
                    else -> LuaValue.error("table or string expected")
                }
            }
        })

        // collectgarbage([opt [, arg]]) - Limited OC support (only "count" really works)
        g.set("collectgarbage", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val opt = args.optjstring(1, "collect")
                return when (opt) {
                    "count" -> {
                        // Return approximate memory usage in KB
                        val runtime = Runtime.getRuntime()
                        val usedBytes = runtime.totalMemory() - runtime.freeMemory()
                        LuaValue.valueOf(usedBytes / 1024.0)
                    }
                    "collect", "step" -> {
                        System.gc()
                        LuaValue.valueOf(0)
                    }
                    "stop", "restart", "isrunning" -> {
                        // No-op in OC, GC is managed by JVM
                        LuaValue.valueOf(0)
                    }
                    "setpause", "setstepmul" -> {
                        // Return previous value (stub returns 0)
                        LuaValue.valueOf(0)
                    }
                    else -> LuaValue.valueOf(0)
                }
            }
        })

        // io library stubs for io.tmpfile() and io.popen()
        val ioLib = g.get("io")
        if (ioLib.isnil()) {
            // Create io table if it doesn't exist
            g.set("io", LuaTable())
        }
        val io = g.get("io")
        
        // io.tmpfile() - Returns a handle for a temp file (stub - not practically usable)
        io.set("tmpfile", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                // In real OC, tmpfile() works within the virtual filesystem
                // For now, return nil with error as files need proper fs integration
                return LuaValue.NIL
            }
        })

        // io.popen() - Not supported in OC (security risk)
        io.set("popen", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("io.popen not supported")))
            }
        })

        // string.pack, string.unpack, string.packsize - Lua 5.3 features
        // LuaJ doesn't natively support these, provide stubs that error
        val stringLib = g.get("string")
        if (!stringLib.isnil()) {
            stringLib.set("pack", object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs {
                    return LuaValue.error("string.pack not available (Lua 5.3 feature not supported by this runtime)")
                }
            })
            stringLib.set("unpack", object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs {
                    return LuaValue.error("string.unpack not available (Lua 5.3 feature not supported by this runtime)")
                }
            })
            stringLib.set("packsize", object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs {
                    return LuaValue.error("string.packsize not available (Lua 5.3 feature not supported by this runtime)")
                }
            })
        }

        return g
    }

    private fun createPrintFunction(): LuaValue = object : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val sb = StringBuilder()
            for (i in 1..args.narg()) {
                if (i > 1) sb.append("\t")
                sb.append(args.arg(i).tojstring())
            }
            OCLogger.info("[OC-Lua] $sb")
            return LuaValue.NONE
        }
    }

    // ===========================
    // component API
    // ===========================
    private fun setupComponentAPI() {
        val g = globals ?: return
        val comp = LuaTable()

        // component.list([filter], [exact]) → callable table (like original OC)
        comp.set("list", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val filter = if (args.narg() > 0 && !args.arg1().isnil()) args.arg1().tojstring() else null
                val exact = args.narg() > 1 && args.arg(2).toboolean()
                val comps = machine.components.filter { (_, type) ->
                    when {
                        filter == null -> true
                        exact -> type == filter
                        else -> type.contains(filter, ignoreCase = true)
                    }
                }
                // Build a table with all matching components (address → type)
                val table = LuaTable()
                for ((addr, type) in comps) {
                    table.set(addr, LuaValue.valueOf(type))
                }
                // Add __call metamethod so the table can be used as an iterator
                val mt = LuaTable()
                val keys = comps.keys.toList()
                mt.set("__call", object : VarArgFunction() {
                    private var index = 0
                    override fun invoke(args: Varargs): Varargs {
                        return if (index < keys.size) {
                            val addr = keys[index++]
                            val type = comps[addr] ?: ""
                            LuaValue.varargsOf(arrayOf(LuaValue.valueOf(addr), LuaValue.valueOf(type)))
                        } else LuaValue.NONE
                    }
                })
                // Protect metatable from getmetatable() (returns "component" instead of real mt)
                mt.set("__metatable", LuaValue.valueOf("component"))
                table.setmetatable(mt)
                return table
            }
        })

        // component.type(address)
        comp.set("type", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val addr = arg.checkjstring()
                return machine.components[addr]?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
            }
        })

        // component.slot(address)
        comp.set("slot", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = LuaValue.valueOf(-1)
        })

        // component.invoke(address, method, ...) → results
        comp.set("invoke", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val addr = args.arg1().checkjstring()
                val method = args.arg(2).checkjstring()
                val luaArgs = mutableListOf<Any?>()
                for (i in 3..args.narg()) luaArgs.add(convertFromLua(args.arg(i)))

                // Direct GPU handling
                val compType = machine.components[addr]
                if (compType == "gpu") {
                    return handleGpuInvoke(method, luaArgs)
                }
                if (compType == "screen") {
                    return handleScreenInvoke(method, luaArgs)
                }
                if (compType == "internet") {
                    return handleInternetInvoke(method, luaArgs)
                }
                if (compType == "eeprom") {
                    return handleEepromInvoke(method, luaArgs)
                }
                if (compType == "computer") {
                    return handleComputerComponentInvoke(method, luaArgs)
                }
                if (compType == "filesystem") {
                    return handleFilesystemInvoke(addr, method, luaArgs)
                }
                if (compType == "redstone") {
                    return handleRedstoneInvoke(method, luaArgs)
                }
                // Stub handlers for components not yet implemented
                if (compType == "robot") {
                    return handleRobotStub(method, luaArgs)
                }
                if (compType == "drone") {
                    return handleDroneStub(method, luaArgs)
                }
                if (compType == "hologram") {
                    return handleHologramStub(method, luaArgs)
                }
                if (compType == "modem" || compType == "tunnel") {
                    return handleModemStub(method, luaArgs)
                }
                if (compType == "data") {
                    return handleDataCardStub(method, luaArgs)
                }
                if (compType == "debug") {
                    return handleDebugCardStub(method, luaArgs)
                }
                if (compType == "inventory_controller") {
                    return handleInventoryControllerStub(method, luaArgs)
                }
                if (compType == "tank_controller") {
                    return handleTankControllerStub(method, luaArgs)
                }
                if (compType == "experience") {
                    return handleExperienceStub(method, luaArgs)
                }
                if (compType == "generator") {
                    return handleGeneratorStub(method, luaArgs)
                }
                if (compType == "crafting") {
                    return handleCraftingStub(method, luaArgs)
                }
                if (compType == "geolyzer") {
                    return handleGeolyzerStub(method, luaArgs)
                }
                if (compType == "navigation") {
                    return handleNavigationStub(method, luaArgs)
                }
                if (compType == "keyboard") {
                    return handleKeyboardInvoke(method, luaArgs)
                }

                // Try machine's component invoke
                return try {
                    val result = machine.invoke(addr, method, *luaArgs.toTypedArray())
                    convertToLuaVarargs(result)
                } catch (e: Exception) {
                    LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf(e.message ?: "error")))
                }
            }
        })

        // component.proxy(address) → table with methods
        comp.set("proxy", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val addr = arg.checkjstring()
                val type = machine.components[addr] ?: return LuaValue.NIL
                val proxy = LuaTable()
                proxy.set("address", LuaValue.valueOf(addr))
                proxy.set("type", LuaValue.valueOf(type))

                // Metatable that forwards calls to component.invoke
                val mt = LuaTable()
                mt.set("__index", object : TwoArgFunction() {
                    override fun call(table: LuaValue, key: LuaValue): LuaValue {
                        val methodName = key.tojstring()
                        return object : VarArgFunction() {
                            override fun invoke(args: Varargs): Varargs {
                                val compInvoke = globals?.get("component")?.get("invoke") ?: return LuaValue.NIL
                                val invokeArgs = arrayListOf<LuaValue>(LuaValue.valueOf(addr), LuaValue.valueOf(methodName))
                                // Skip first arg if it's the proxy table itself (colon syntax: proxy:method())
                                val startIdx = if (args.narg() > 0 && args.arg(1).raweq(proxy)) 2 else 1
                                for (i in startIdx..args.narg()) invokeArgs.add(args.arg(i))
                                return compInvoke.invoke(LuaValue.varargsOf(invokeArgs.toTypedArray()))
                            }
                        }
                    }
                })
                // Protect metatable from getmetatable() (returns "component" instead of real mt)
                mt.set("__metatable", LuaValue.valueOf("component"))
                proxy.setmetatable(mt)
                return proxy
            }
        })

        // component.methods(address)
        comp.set("methods", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val addr = arg.checkjstring()
                val type = machine.components[addr] ?: return LuaValue.NIL
                val t = LuaTable()
                val methods = when (type) {
                    "gpu" -> listOf("bind", "getScreen", "getResolution", "setResolution", "maxResolution",
                        "getDepth", "setDepth", "maxDepth", "get", "set", "fill", "copy", "getBackground",
                        "setBackground", "getForeground", "setForeground", "getPaletteColor", "setPaletteColor",
                        "getViewport", "setViewport",
                        // VRAM buffer methods (OC 1.7+)
                        "getActiveBuffer", "setActiveBuffer", "buffers", "allocateBuffer", "freeBuffer",
                        "freeAllBuffers", "getBufferSize", "totalMemory", "freeMemory", "bitblt")
                    "screen" -> listOf("isOn", "turnOn", "turnOff", "getAspectRatio", "getKeyboards",
                        "setPrecise", "isPrecise", "setTouchModeInverted", "isTouchModeInverted")
                    "filesystem" -> listOf("open", "close", "read", "write", "seek", "exists",
                        "isDirectory", "list", "makeDirectory", "remove", "rename", "size",
                        "lastModified", "spaceTotal", "spaceUsed", "isReadOnly", "getLabel", "setLabel",
                        "isLink", "link")
                    "computer" -> listOf("start", "stop", "isRunning", "beep")
                    "eeprom" -> listOf("get", "set", "getLabel", "setLabel", "getData", "setData",
                        "getSize", "getDataSize", "getChecksum")
                    "internet" -> listOf("isHttpEnabled", "isTcpEnabled", "request", "connect")
                    "redstone" -> listOf("getInput", "getOutput", "setOutput", "getBundledInput", "getBundledOutput", "setBundledOutput",
                        "getComparatorInput", "setWakeThreshold", "getWakeThreshold", "setWirelessFrequency", "getWirelessFrequency", 
                        "getWirelessInput", "getWirelessOutput", "setWirelessOutput")
                    "keyboard" -> listOf("getLayoutName", "setLayoutName", "getPressedKeys", "getPressedCodes",
                        "isAltDown", "isControl", "isControlDown", "isKeyDown", "isShiftDown")
                    "robot" -> listOf("move", "turn", "name", "detect", "use", "swing", "place", "drop", "suck",
                        "select", "count", "space", "compareTo", "compareFluid", "compareFluidTo", "transferFluid",
                        "inventorySize", "level", "lightColor", "tankCount", "selectTank", "tankLevel", "tankSpace",
                        "drain", "fill", "durability")
                    "drone" -> listOf("move", "name", "offset", "setStatusText", "getStatusText", "setLightColor", "getLightColor",
                        "setAcceleration", "getAcceleration", "getMaxVelocity", "getVelocity", "getPosition")
                    "hologram" -> listOf("clear", "get", "set", "fill", "copy", "maxDepth", "setScale", "getScale",
                        "setTranslation", "getTranslation", "setRotation", "getRotation", "setRotationSpeed", "getRotationSpeed",
                        "setPaletteColor", "getPaletteColor")
                    "modem" -> listOf("isWireless", "isWired", "getWakeMessage", "setWakeMessage", "getStrength",
                        "setStrength", "maxPacketSize", "broadcast", "send", "open", "close", "isOpen")
                    "tunnel" -> listOf("send", "getWakeMessage", "setWakeMessage", "maxPacketSize", "getChannel")
                    "data" -> listOf("crc32", "md5", "sha256", "deflate", "inflate", "encode64", "decode64",
                        "random", "generateKeyPair", "ecdsa", "ecdh", "serialize", "deserialize")
                    "debug" -> listOf("getWorld", "setWorld", "getPlayer", "getPlayers", "isModLoaded",
                        "runCommand", "getScoreboard", "getX", "getY", "getZ", "sendToDebugCard", "sendToClipboard",
                        "changeBuffer", "connectToBlock", "test")
                    "inventory_controller" -> listOf("getInventorySize", "getStackInSlot", "getSlotMaxStackSize",
                        "getSlotStackSize", "compareStacks", "getItemInventorySize", "getStackInInternalSlot",
                        "dropIntoSlot", "suckFromSlot", "equip", "store", "storeInternal")
                    "tank_controller" -> listOf("getTankCount", "getFluidInTank", "getTankCapacity",
                        "getFluidInInternalTank", "drain", "fill")
                    "experience" -> listOf("level")
                    "generator" -> listOf("count", "insert", "remove")
                    "crafting" -> listOf("craft")
                    "geolyzer" -> listOf("scan", "analyze", "store", "detect")
                    "navigation" -> listOf("getPosition", "getFacing", "getRange", "findWaypoints")
                    "sign" -> listOf("getValue", "setValue")
                    "piston" -> listOf("push")
                    "tractor_beam" -> listOf("suck")
                    "leash" -> listOf("leash", "unleash")
                    else -> listOf()
                }
                for (m in methods) {
                    val info = LuaTable()
                    info.set("direct", LuaValue.TRUE)
                    info.set("getter", LuaValue.FALSE)
                    info.set("setter", LuaValue.FALSE)
                    t.set(m, info)
                }
                return t
            }
        })

        // component.fields(address)
        comp.set("fields", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = LuaTable()
        })

        // component.doc(address, method) - Return documentation strings for methods
        comp.set("doc", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val methodName = arg2.checkjstring()
                val doc = COMPONENT_DOCS[methodName]
                return if (doc != null) LuaValue.valueOf(doc) else LuaValue.NIL
            }
        })

        // component.getPrimary(type) → proxy for first component of that type
        comp.set("getPrimary", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val type = arg.checkjstring()
                val addr = machine.components.entries.firstOrNull { it.value == type }?.key
                    ?: return LuaValue.NIL
                return comp.get("proxy").call(LuaValue.valueOf(addr))
            }
        })

        // component.isAvailable(type) → boolean
        comp.set("isAvailable", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val type = arg.checkjstring()
                return LuaValue.valueOf(machine.components.values.any { it == type })
            }
        })

        // component.get(address, [componentType]) → fullAddress or (nil, error)
        // Matches partial addresses (prefix matching), returns just the address like original OC
        comp.set("get", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val partialAddr = args.arg1().checkjstring()
                val filterType = if (args.narg() > 1 && !args.arg(2).isnil()) args.arg(2).tojstring() else null
                
                for ((addr, type) in machine.components) {
                    if (addr.startsWith(partialAddr) && (filterType == null || type == filterType)) {
                        return LuaValue.valueOf(addr)
                    }
                }
                return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("no such component")))
            }
        })

        g.set("component", comp)
    }

    // ===========================
    // computer API
    // ===========================
    private fun setupComputerAPI() {
        val g = globals ?: return
        val comp = LuaTable()

        comp.set("address", object : ZeroArgFunction() {
            override fun call(): LuaValue = try {
                LuaValue.valueOf(machine.node().address)
            } catch (e: Exception) {
                LuaValue.valueOf("computer-0")
            }
        })

        comp.set("uptime", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(machine.uptime)
        })

        comp.set("totalMemory", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(machine.totalMemory.toDouble())
        })

        comp.set("freeMemory", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val g = globals ?: return LuaValue.valueOf(machine.totalMemory.toDouble())
                // Estimate Lua memory usage from LuaJ's global table size
                System.gc()
                val runtime = Runtime.getRuntime()
                val jvmUsed = runtime.totalMemory() - runtime.freeMemory()
                // Rough estimate: attribute a fraction to this Lua instance
                val estimate = (machine.totalMemory - (jvmUsed / 8).coerceAtMost(machine.totalMemory.toLong()))
                    .coerceAtLeast(0).toDouble()
                return LuaValue.valueOf(estimate)
            }
        })

        comp.set("energy", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(10000.0)
        })

        comp.set("maxEnergy", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(10000.0)
        })

        // computer.pullSignal([timeout]) → name, ...args
        // This is the critical event system - yields the coroutine
        comp.set("pullSignal", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val timeout = if (args.narg() > 0 && !args.arg1().isnil()) args.arg1().todouble() else 5.0
                return globals!!.yield(LuaValue.varargsOf(arrayOf(LuaValue.valueOf(timeout))))
            }
        })

        comp.set("pushSignal", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1) return LuaValue.FALSE
                val name = args.arg1().checkjstring()
                val signalArgs = (2..args.narg()).map { convertFromLua(args.arg(it)) }.toTypedArray()
                return LuaValue.valueOf(machine.signal(name, *signalArgs))
            }
        })

        comp.set("shutdown", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val reboot = args.narg() > 0 && args.arg1().toboolean()
                if (reboot) {
                    machine.signal("__reboot")
                } else {
                    machine.signal("__shutdown")
                }
                // Yield immediately like original OC (coroutine.yield(reboot))
                return globals!!.yield(LuaValue.valueOf(reboot))
            }
        })

        comp.set("beep", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                // Play beep sound – stub
                return LuaValue.NONE
            }
        })

        comp.set("getBootAddress", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(bootAddress ?: "")
        })

        comp.set("setBootAddress", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                bootAddress = if (arg.isnil()) null else arg.tojstring()
                return LuaValue.NONE
            }
        })

        comp.set("tmpAddress", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(tmpFsAddress)
        })

        comp.set("users", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val t = LuaTable()
                computerUsers.forEachIndexed { i, user -> t.set(i + 1, LuaValue.valueOf(user)) }
                return t
            }
        })

        comp.set("addUser", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val name = args.arg1().checkjstring()
                if (computerUsers.size >= maxUsers) {
                    return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("too many users")))
                }
                if (computerUsers.contains(name)) {
                    return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("user exists")))
                }
                if (name.length > 64) {
                    return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("username too long")))
                }
                computerUsers.add(name)
                return LuaValue.TRUE
            }
        })

        comp.set("removeUser", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val name = arg.checkjstring()
                return LuaValue.valueOf(computerUsers.remove(name))
            }
        })

        comp.set("realTime", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(System.currentTimeMillis() / 1000.0)
        })

        comp.set("isRobot", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.FALSE
        })

        comp.set("getDeviceInfo", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val result = LuaTable()
                for ((addr, type) in machine.components) {
                    val info = LuaTable()
                    when (type) {
                        "gpu" -> {
                            info.set("class", LuaValue.valueOf("display"))
                            info.set("description", LuaValue.valueOf("Graphics controller"))
                            info.set("vendor", LuaValue.valueOf("MightyPirates GmbH & Co. KG"))
                            info.set("product", LuaValue.valueOf("ATI Accelerated GPU"))
                            info.set("capacity", LuaValue.valueOf("8"))
                            info.set("width", LuaValue.valueOf("3"))
                        }
                        "screen" -> {
                            info.set("class", LuaValue.valueOf("display"))
                            info.set("description", LuaValue.valueOf("Text screen"))
                            info.set("vendor", LuaValue.valueOf("MightyPirates GmbH & Co. KG"))
                            info.set("product", LuaValue.valueOf("Hard-Mond T3"))
                            info.set("width", LuaValue.valueOf("3"))
                        }
                        "filesystem" -> {
                            info.set("class", LuaValue.valueOf("volume"))
                            info.set("description", LuaValue.valueOf("Disk drive"))
                            info.set("vendor", LuaValue.valueOf("MightyPirates GmbH & Co. KG"))
                            info.set("product", LuaValue.valueOf("MightyHDD"))
                            info.set("capacity", LuaValue.valueOf("2097152"))
                        }
                        "keyboard" -> {
                            info.set("class", LuaValue.valueOf("input"))
                            info.set("description", LuaValue.valueOf("Keyboard"))
                            info.set("vendor", LuaValue.valueOf("MightyPirates GmbH & Co. KG"))
                            info.set("product", LuaValue.valueOf("Keyboard"))
                        }
                        "computer" -> {
                            info.set("class", LuaValue.valueOf("system"))
                            info.set("description", LuaValue.valueOf("Computer"))
                            info.set("vendor", LuaValue.valueOf("MightyPirates GmbH & Co. KG"))
                            info.set("product", LuaValue.valueOf("Blocker T3"))
                        }
                        "internet" -> {
                            info.set("class", LuaValue.valueOf("network"))
                            info.set("description", LuaValue.valueOf("Internet card"))
                            info.set("vendor", LuaValue.valueOf("MightyPirates GmbH & Co. KG"))
                            info.set("product", LuaValue.valueOf("MPNet"))
                        }
                        "eeprom" -> {
                            info.set("class", LuaValue.valueOf("memory"))
                            info.set("description", LuaValue.valueOf("EEPROM"))
                            info.set("vendor", LuaValue.valueOf("MightyPirates GmbH & Co. KG"))
                            info.set("product", LuaValue.valueOf("FlashStick2k"))
                            info.set("capacity", LuaValue.valueOf("4096"))
                        }
                        "redstone" -> {
                            info.set("class", LuaValue.valueOf("redstone"))
                            info.set("description", LuaValue.valueOf("Redstone I/O"))
                            info.set("vendor", LuaValue.valueOf("MightyPirates GmbH & Co. KG"))
                            info.set("product", LuaValue.valueOf("RS"))
                        }
                        else -> {
                            info.set("class", LuaValue.valueOf("generic"))
                            info.set("description", LuaValue.valueOf(type))
                            info.set("vendor", LuaValue.valueOf("unknown"))
                            info.set("product", LuaValue.valueOf(type))
                        }
                    }
                    result.set(addr, info)
                }
                return result
            }
        })

        comp.set("getProgramLocations", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaTable()
        })

        comp.set("getArchitectures", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val t = LuaTable()
                t.set(1, LuaValue.valueOf("Lua 5.3"))
                return t
            }
        })

        comp.set("getArchitecture", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf("Lua 5.3")
        })

        comp.set("setArchitecture", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = LuaValue.TRUE
        })

        g.set("computer", comp)
    }

    // ===========================
    // os API
    // ===========================
    private fun setupOSAPI() {
        val g = globals ?: return
        val os = LuaTable()
        os.set("clock", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(machine.cpuTime)
        })
        os.set("time", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(System.currentTimeMillis() / 1000.0)
        })
        os.set("date", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val format = if (args.narg() > 0 && args.arg1().isstring()) args.arg1().tojstring() else "%c"
                val time = if (args.narg() > 1 && args.arg(2).isnumber()) args.arg(2).checklong() * 1000L else System.currentTimeMillis()
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = time
                
                if (format == "*t") {
                    val t = LuaTable()
                    t.set("year", LuaValue.valueOf(cal.get(java.util.Calendar.YEAR)))
                    t.set("month", LuaValue.valueOf(cal.get(java.util.Calendar.MONTH) + 1))
                    t.set("day", LuaValue.valueOf(cal.get(java.util.Calendar.DAY_OF_MONTH)))
                    t.set("hour", LuaValue.valueOf(cal.get(java.util.Calendar.HOUR_OF_DAY)))
                    t.set("min", LuaValue.valueOf(cal.get(java.util.Calendar.MINUTE)))
                    t.set("sec", LuaValue.valueOf(cal.get(java.util.Calendar.SECOND)))
                    t.set("wday", LuaValue.valueOf(((cal.get(java.util.Calendar.DAY_OF_WEEK) - 1 + 6) % 7) + 1))
                    t.set("yday", LuaValue.valueOf(cal.get(java.util.Calendar.DAY_OF_YEAR)))
                    t.set("isdst", LuaValue.valueOf(cal.timeZone.inDaylightTime(cal.time)))
                    return t
                }
                
                // Convert C-style strftime format to Java SimpleDateFormat
                val javaFmt = format
                    .replace("%Y", "yyyy").replace("%y", "yy")
                    .replace("%m", "MM").replace("%d", "dd")
                    .replace("%H", "HH").replace("%M", "mm").replace("%S", "ss")
                    .replace("%p", "a").replace("%I", "hh")
                    .replace("%A", "EEEE").replace("%a", "EEE")
                    .replace("%B", "MMMM").replace("%b", "MMM")
                    .replace("%c", "EEE MMM dd HH:mm:ss yyyy")
                    .replace("%x", "MM/dd/yy").replace("%X", "HH:mm:ss")
                    .replace("%Z", "zzz").replace("%j", "DDD")
                    .replace("%w", "F").replace("%%", "'%'")
                return LuaValue.valueOf(java.text.SimpleDateFormat(javaFmt).format(java.util.Date(time)))
            }
        })
        g.set("os", os)
    }

    // ===========================
    // GPU API (direct access + component.invoke)
    // ===========================
    private fun findNearbyScreen(): li.cil.oc.common.blockentity.ScreenBlockEntity? {
        val level = machine.host.world() ?: return null
        val pos = machine.host.hostPosition()
        for (dx in -8..8) for (dy in -2..2) for (dz in -8..8) {
            val be = level.getBlockEntity(pos.offset(dx, dy, dz))
            if (be is li.cil.oc.common.blockentity.ScreenBlockEntity) return be
        }
        return null
    }

    private var boundScreenAddr: String? = null
    private var gpuDepth: Int = 8
    private var viewportW: Int = -1  // -1 means same as resolution
    private var viewportH: Int = -1
    private val gpuPalette = intArrayOf(
        0x000000, 0x000040, 0x004000, 0x004040, 0x400000, 0x400040, 0x404000, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    )

    // GPU VRAM buffer system (OC 1.7+)
    private var activeBuffer: Int = 0  // 0 = screen, 1+ = VRAM buffer index
    private data class VRAMBuffer(val width: Int, val height: Int, val data: IntArray, val fg: IntArray, val bg: IntArray)
    private val vramBuffers = mutableMapOf<Int, VRAMBuffer>()
    private var nextBufferId = 1
    private val totalVRAM = 2 * 1024 * 1024  // 2MB VRAM (T3 GPU)

    /**
     * Clamp a color to the current GPU depth.
     * Depth 1: monochrome (black or white)
     * Depth 4: nearest of the 16 palette colors
     * Depth 8: full 24-bit (no change)
     */
    private fun clampColor(color: Int): Int {
        return when (gpuDepth) {
            1 -> {
                // Monochrome: use luminance to decide black or white
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                if (r + g + b >= 383) 0xFFFFFF else 0x000000
            }
            4 -> {
                // Find nearest palette color (Euclidean distance in RGB)
                var bestIdx = 0
                var bestDist = Int.MAX_VALUE
                val cr = (color shr 16) and 0xFF
                val cg = (color shr 8) and 0xFF
                val cb = color and 0xFF
                for (i in gpuPalette.indices) {
                    val pr = (gpuPalette[i] shr 16) and 0xFF
                    val pg = (gpuPalette[i] shr 8) and 0xFF
                    val pb = gpuPalette[i] and 0xFF
                    val dist = (cr - pr) * (cr - pr) + (cg - pg) * (cg - pg) + (cb - pb) * (cb - pb)
                    if (dist < bestDist) { bestDist = dist; bestIdx = i }
                }
                gpuPalette[bestIdx]
            }
            else -> color and 0xFFFFFF
        }
    }

    private fun setupGpuAPI() {
        val g = globals ?: return
        val gpu = LuaTable()

        gpu.set("bind", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                boundScreenAddr = args.arg1().tojstring()
                val reset = if (args.narg() >= 2) args.arg(2).toboolean() else true
                if (reset) {
                    val screen = findNearbyScreen()
                    if (screen != null) {
                        screen.buffer.foreground = 0xFFFFFF
                        screen.buffer.background = 0x000000
                        gpuDepth = 8
                        viewportW = -1
                        viewportH = -1
                    }
                }
                return LuaValue.varargsOf(arrayOf(LuaValue.TRUE))
            }
        })

        gpu.set("getScreen", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(boundScreenAddr ?: "")
        })

        gpu.set("getResolution", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val screen = findNearbyScreen()
                return LuaValue.varargsOf(arrayOf(
                    LuaValue.valueOf(screen?.buffer?.width ?: 80),
                    LuaValue.valueOf(screen?.buffer?.height ?: 25)
                ))
            }
        })

        gpu.set("maxResolution", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                // Return hardware maximum, not current resolution
                // GPU T3 max is 160x50, screen T3 max is 160x50
                return LuaValue.varargsOf(arrayOf(
                    LuaValue.valueOf(160),
                    LuaValue.valueOf(50)
                ))
            }
        })

        gpu.set("setResolution", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val w = args.arg1().checkint()
                val h = args.arg(2).checkint()
                if (w < 1 || h < 1 || w > 160 || h > 50) {
                    return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("unsupported resolution")))
                }
                val screen = findNearbyScreen()
                if (screen != null) {
                    val oldW = screen.buffer.width
                    val oldH = screen.buffer.height
                    if (w != oldW || h != oldH) {
                        screen.buffer.resize(w, h)
                        viewportW = -1
                        viewportH = -1
                        screen.setChanged()
                        screen.markForSync()
                        machine.signal("screen_resized", boundScreenAddr ?: "", w, h)
                        return LuaValue.TRUE
                    }
                }
                return LuaValue.FALSE
            }
        })

        gpu.set("getDepth", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(gpuDepth)
        })

        gpu.set("maxDepth", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                // Max depth based on GPU tier: T1=1, T2=4, T3=8
                val sm = machine as? SimpleMachine
                val gpuTier = sm?.installedComponents?.gpuTier ?: 2  // default T3 if unknown
                return LuaValue.valueOf(when (gpuTier) {
                    0 -> 1  // Tier 1
                    1 -> 4  // Tier 2
                    else -> 8  // Tier 3
                })
            }
        })

        gpu.set("setDepth", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val depth = arg.checkint()
                // Check valid depth values
                if (depth != 1 && depth != 4 && depth != 8) {
                    LuaValue.error("unsupported depth")
                }
                // Check against max depth for GPU tier
                val sm = machine as? SimpleMachine
                val gpuTier = sm?.installedComponents?.gpuTier ?: 2
                val maxAllowed = when (gpuTier) {
                    0 -> 1  // Tier 1
                    1 -> 4  // Tier 2
                    else -> 8  // Tier 3
                }
                if (depth > maxAllowed) {
                    LuaValue.error("unsupported depth")
                }
                val old = gpuDepth
                gpuDepth = depth
                return LuaValue.valueOf(old)
            }
        })

        gpu.set("getPaletteColor", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val index = arg.checkint()
                return if (index in 0..15) LuaValue.valueOf(gpuPalette[index]) else LuaValue.valueOf(0)
            }
        })

        gpu.set("setPaletteColor", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val index = arg1.checkint()
                val color = arg2.checkint()
                if (index in 0..15) {
                    val old = gpuPalette[index]
                    gpuPalette[index] = color
                    return LuaValue.valueOf(old)
                }
                return LuaValue.valueOf(0)
            }
        })

        gpu.set("set", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val x = args.arg1().checkint()
                val y = args.arg(2).checkint()
                val text = args.arg(3).checkjstring()
                val vertical = args.narg() >= 4 && args.arg(4).toboolean()
                val screen = findNearbyScreen() ?: return LuaValue.FALSE
                screen.buffer.set(x - 1, y - 1, text, vertical)
                screen.setChanged()
                screen.markForSync()
                return LuaValue.TRUE
            }
        })

        gpu.set("get", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val x = args.arg1().checkint()
                val y = args.arg(2).checkint()
                val screen = findNearbyScreen() ?: return LuaValue.varargsOf(arrayOf(
                    LuaValue.valueOf(" "),
                    LuaValue.valueOf(0xFFFFFF),
                    LuaValue.valueOf(0x000000),
                    LuaValue.NIL,
                    LuaValue.NIL
                ))
                val buf = screen.buffer
                val bx = x - 1
                val by = y - 1
                val char = if (bx in 0 until buf.width && by in 0 until buf.height)
                    String(Character.toChars(buf.charData[by * buf.width + bx])) else " "
                val fg = if (bx in 0 until buf.width && by in 0 until buf.height)
                    buf.fgData[by * buf.width + bx] else 0xFFFFFF
                val bg = if (bx in 0 until buf.width && by in 0 until buf.height)
                    buf.bgData[by * buf.width + bx] else 0x000000
                return LuaValue.varargsOf(arrayOf(
                    LuaValue.valueOf(char),
                    LuaValue.valueOf(fg),
                    LuaValue.valueOf(bg),
                    LuaValue.NIL,
                    LuaValue.NIL
                ))
            }
        })

        gpu.set("fill", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val x = args.arg1().checkint()
                val y = args.arg(2).checkint()
                val w = args.arg(3).checkint()
                val h = args.arg(4).checkint()
                val char = if (args.narg() >= 5) args.arg(5).checkjstring().firstOrNull()?.code ?: ' '.code else ' '.code
                val screen = findNearbyScreen() ?: return LuaValue.FALSE
                screen.buffer.fill(x - 1, y - 1, w, h, char)
                screen.setChanged()
                screen.markForSync()
                return LuaValue.TRUE
            }
        })

        gpu.set("copy", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val x = args.arg1().checkint()
                val y = args.arg(2).checkint()
                val w = args.arg(3).checkint()
                val h = args.arg(4).checkint()
                val tx = args.arg(5).checkint()
                val ty = args.arg(6).checkint()
                val screen = findNearbyScreen() ?: return LuaValue.FALSE
                screen.buffer.copy(x - 1, y - 1, w, h, tx, ty)
                screen.setChanged()
                screen.markForSync()
                return LuaValue.TRUE
            }
        })

        gpu.set("setForeground", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val color = args.arg1().checkint()
                val isPalette = args.narg() >= 2 && args.arg(2).toboolean()
                val screen = findNearbyScreen()
                val old = screen?.buffer?.foreground ?: 0xFFFFFF
                if (screen != null) {
                    screen.buffer.foreground = if (isPalette) {
                        if (color in 0..15) gpuPalette[color] else clampColor(color)
                    } else clampColor(color)
                }
                return LuaValue.varargsOf(arrayOf(LuaValue.valueOf(old), LuaValue.FALSE))
            }
        })

        gpu.set("setBackground", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val color = args.arg1().checkint()
                val isPalette = args.narg() >= 2 && args.arg(2).toboolean()
                val screen = findNearbyScreen()
                val old = screen?.buffer?.background ?: 0x000000
                if (screen != null) {
                    screen.buffer.background = if (isPalette) {
                        if (color in 0..15) gpuPalette[color] else clampColor(color)
                    } else clampColor(color)
                }
                return LuaValue.varargsOf(arrayOf(LuaValue.valueOf(old), LuaValue.FALSE))
            }
        })

        gpu.set("getForeground", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val screen = findNearbyScreen()
                return LuaValue.varargsOf(arrayOf(
                    LuaValue.valueOf(screen?.buffer?.foreground ?: 0xFFFFFF),
                    LuaValue.FALSE
                ))
            }
        })

        gpu.set("getBackground", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val screen = findNearbyScreen()
                return LuaValue.varargsOf(arrayOf(
                    LuaValue.valueOf(screen?.buffer?.background ?: 0x000000),
                    LuaValue.FALSE
                ))
            }
        })

        gpu.set("getViewport", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val screen = findNearbyScreen()
                val w = if (viewportW > 0) viewportW else (screen?.buffer?.width ?: 80)
                val h = if (viewportH > 0) viewportH else (screen?.buffer?.height ?: 25)
                return LuaValue.varargsOf(arrayOf(
                    LuaValue.valueOf(w),
                    LuaValue.valueOf(h)
                ))
            }
        })

        gpu.set("setViewport", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val w = args.arg1().checkint()
                val h = args.arg(2).checkint()
                val screen = findNearbyScreen()
                val resW = screen?.buffer?.width ?: 80
                val resH = screen?.buffer?.height ?: 25
                if (w < 1 || h < 1 || w > resW || h > resH) {
                    return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("unsupported viewport size")))
                }
                val changed = (viewportW != w || viewportH != h)
                viewportW = w
                viewportH = h
                return LuaValue.valueOf(changed)
            }
        })

        // ===== GPU VRAM Buffer Methods (OC 1.7+) =====
        
        gpu.set("getActiveBuffer", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(activeBuffer)
        })

        gpu.set("setActiveBuffer", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val index = arg.checkint()
                if (index != 0 && !vramBuffers.containsKey(index)) {
                    return LuaValue.NIL  // Invalid buffer index
                }
                val prev = activeBuffer
                activeBuffer = index
                return LuaValue.valueOf(prev)
            }
        })

        gpu.set("buffers", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val t = LuaTable()
                var i = 1
                for (idx in vramBuffers.keys.sorted()) {
                    t.set(i++, LuaValue.valueOf(idx))
                }
                return t
            }
        })

        gpu.set("allocateBuffer", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val w = if (args.narg() >= 1 && !args.arg1().isnil()) args.arg1().checkint() else 160
                val h = if (args.narg() >= 2 && !args.arg(2).isnil()) args.arg(2).checkint() else 50
                if (w <= 0 || h <= 0) {
                    return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("invalid buffer dimensions")))
                }
                val size = w * h * 4  // Rough memory estimate per cell (char + fg + bg)
                val usedMem = vramBuffers.values.sumOf { it.width * it.height * 4 }
                if (usedMem + size > totalVRAM) {
                    return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("not enough video memory")))
                }
                val id = nextBufferId++
                vramBuffers[id] = VRAMBuffer(
                    width = w, height = h,
                    data = IntArray(w * h) { ' '.code },
                    fg = IntArray(w * h) { 0xFFFFFF },
                    bg = IntArray(w * h) { 0x000000 }
                )
                return LuaValue.valueOf(id)
            }
        })

        gpu.set("freeBuffer", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val index = if (args.narg() >= 1 && !args.arg1().isnil()) args.arg1().checkint() else activeBuffer
                if (index == 0) return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("cannot free screen buffer")))
                val removed = vramBuffers.remove(index) != null
                if (removed && activeBuffer == index) {
                    activeBuffer = 0  // Fall back to screen
                }
                return if (removed) LuaValue.TRUE else LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("no buffer at index")))
            }
        })

        gpu.set("freeAllBuffers", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val count = vramBuffers.size
                vramBuffers.clear()
                nextBufferId = 1
                activeBuffer = 0
                return LuaValue.valueOf(count)
            }
        })

        gpu.set("getBufferSize", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val index = if (args.narg() >= 1 && !args.arg1().isnil()) args.arg1().checkint() else activeBuffer
                if (index == 0) {
                    val screen = findNearbyScreen()
                    return LuaValue.varargsOf(arrayOf(
                        LuaValue.valueOf(screen?.buffer?.width ?: 80),
                        LuaValue.valueOf(screen?.buffer?.height ?: 25)
                    ))
                }
                val buf = vramBuffers[index] ?: return LuaValue.NIL
                return LuaValue.varargsOf(arrayOf(LuaValue.valueOf(buf.width), LuaValue.valueOf(buf.height)))
            }
        })

        gpu.set("totalMemory", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(totalVRAM)
        })

        // GPU freeMemory (VRAM free, not computer RAM)
        gpu.set("freeMemory", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val usedMem = vramBuffers.values.sumOf { it.width * it.height * 4 }
                return LuaValue.valueOf(totalVRAM - usedMem)
            }
        })

        gpu.set("bitblt", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                // bitblt([dst: number, col: number, row: number, width: number, height: number, src: number, fromCol: number, fromRow: number])
                val dstIdx = if (args.narg() >= 1 && !args.arg1().isnil()) args.arg1().checkint() else 0
                val screen = findNearbyScreen()
                val dstW = if (dstIdx == 0) (screen?.buffer?.width ?: 80) else (vramBuffers[dstIdx]?.width ?: return LuaValue.NIL)
                val dstH = if (dstIdx == 0) (screen?.buffer?.height ?: 25) else (vramBuffers[dstIdx]?.height ?: return LuaValue.NIL)
                
                val col = if (args.narg() >= 2 && !args.arg(2).isnil()) args.arg(2).checkint() else 1
                val row = if (args.narg() >= 3 && !args.arg(3).isnil()) args.arg(3).checkint() else 1
                val w = if (args.narg() >= 4 && !args.arg(4).isnil()) args.arg(4).checkint() else dstW
                val h = if (args.narg() >= 5 && !args.arg(5).isnil()) args.arg(5).checkint() else dstH
                val srcIdx = if (args.narg() >= 6 && !args.arg(6).isnil()) args.arg(6).checkint() else activeBuffer
                val fromCol = if (args.narg() >= 7 && !args.arg(7).isnil()) args.arg(7).checkint() else 1
                val fromRow = if (args.narg() >= 8 && !args.arg(8).isnil()) args.arg(8).checkint() else 1

                // Get source buffer
                val srcBuf = if (srcIdx == 0) null else vramBuffers[srcIdx]
                if (srcIdx != 0 && srcBuf == null) return LuaValue.NIL

                // Perform blit
                for (dy in 0 until h) {
                    for (dx in 0 until w) {
                        val sx = fromCol - 1 + dx
                        val sy = fromRow - 1 + dy
                        val dx2 = col - 1 + dx
                        val dy2 = row - 1 + dy
                        
                        // Read from source
                        val (ch, fg, bg) = if (srcIdx == 0 && screen != null) {
                            val buf = screen.buffer
                            if (sx in 0 until buf.width && sy in 0 until buf.height) {
                                val idx = sy * buf.width + sx
                                Triple(buf.charData[idx], buf.fgData[idx], buf.bgData[idx])
                            } else Triple(' '.code, 0xFFFFFF, 0x000000)
                        } else if (srcBuf != null && sx in 0 until srcBuf.width && sy in 0 until srcBuf.height) {
                            val idx = sy * srcBuf.width + sx
                            Triple(srcBuf.data[idx], srcBuf.fg[idx], srcBuf.bg[idx])
                        } else continue

                        // Write to destination
                        if (dstIdx == 0 && screen != null) {
                            val buf = screen.buffer
                            if (dx2 in 0 until buf.width && dy2 in 0 until buf.height) {
                                val idx = dy2 * buf.width + dx2
                                buf.charData[idx] = ch
                                buf.fgData[idx] = fg
                                buf.bgData[idx] = bg
                            }
                        } else {
                            val dstBuf = vramBuffers[dstIdx] ?: continue
                            if (dx2 in 0 until dstBuf.width && dy2 in 0 until dstBuf.height) {
                                val idx = dy2 * dstBuf.width + dx2
                                dstBuf.data[idx] = ch
                                dstBuf.fg[idx] = fg
                                dstBuf.bg[idx] = bg
                            }
                        }
                    }
                }
                
                if (dstIdx == 0 && screen != null) {
                    screen.setChanged()
                    screen.markForSync()
                }
                return LuaValue.TRUE
            }
        })

        g.set("gpu", gpu)
    }

    // Handle gpu invocations via component.invoke(gpuAddress, method, ...)
    private fun handleGpuInvoke(method: String, args: List<Any?>): Varargs {
        val gpu = globals?.get("gpu") ?: return LuaValue.NIL
        val fn = gpu.get(method)
        if (fn.isnil()) return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("no such method: $method")))
        val luaArgs = args.map { convertToLua(it) }.toTypedArray()
        return fn.invoke(LuaValue.varargsOf(luaArgs))
    }

    private var screenIsOn = true

    private fun handleScreenInvoke(method: String, args: List<Any?>): Varargs {
        val screen = findNearbyScreen() ?: return LuaValue.NIL
        return when (method) {
            "getAspectRatio" -> LuaValue.varargsOf(arrayOf(LuaValue.valueOf(1), LuaValue.valueOf(1)))
            "getKeyboards" -> {
                val t = LuaTable()
                screen.keyboardAddress?.let { t.set(1, LuaValue.valueOf(it)) }
                t
            }
            "turnOn" -> {
                val changed = !screenIsOn
                screenIsOn = true
                LuaValue.varargsOf(arrayOf(LuaValue.valueOf(changed), LuaValue.valueOf(screenIsOn)))
            }
            "turnOff" -> {
                val changed = screenIsOn
                screenIsOn = false
                LuaValue.varargsOf(arrayOf(LuaValue.valueOf(changed), LuaValue.valueOf(screenIsOn)))
            }
            "isOn" -> LuaValue.valueOf(screenIsOn)
            "setPrecise" -> {
                // Return old value (always false since we don't track it)
                LuaValue.FALSE
            }
            "isPrecise" -> LuaValue.FALSE
            "setTouchModeInverted" -> {
                LuaValue.FALSE
            }
            "isTouchModeInverted" -> LuaValue.FALSE
            else -> LuaValue.NIL
        }
    }

    private var eepromCode: String = BIOS_CODE
    private var eepromLabel: String = "EEPROM"

    private fun handleEepromInvoke(method: String, args: List<Any?>): Varargs {
        return when (method) {
            "get" -> LuaValue.valueOf(eepromCode)
            "set" -> {
                eepromCode = args.getOrNull(0)?.toString() ?: ""
                LuaValue.NONE
            }
            "getLabel" -> LuaValue.valueOf(eepromLabel)
            "setLabel" -> {
                eepromLabel = (args.getOrNull(0)?.toString() ?: "EEPROM").trim().take(24).ifEmpty { "EEPROM" }
                LuaValue.valueOf(eepromLabel)
            }
            "getData" -> LuaString.valueOf(eepromDataBytes)
            "setData" -> {
                val data = when (val raw = args.getOrNull(0)) {
                    is ByteArray -> raw
                    is String -> raw.toByteArray(Charsets.ISO_8859_1)
                    else -> ByteArray(0)
                }
                if (data.size <= 256) eepromDataBytes = data
                LuaValue.NONE
            }
            "getSize" -> LuaValue.valueOf(4096)
            "getDataSize" -> LuaValue.valueOf(256)
            "getChecksum" -> {
                val crc = java.util.zip.CRC32()
                crc.update(eepromCode.toByteArray())
                LuaValue.valueOf(String.format("%08x", crc.value))
            }
            "makeReadonly" -> LuaValue.TRUE
            else -> LuaValue.NIL
        }
    }

    private fun handleComputerComponentInvoke(method: String, args: List<Any?>): Varargs {
        return when (method) {
            "beep" -> LuaValue.NONE
            "start" -> LuaValue.TRUE
            "stop" -> LuaValue.TRUE
            "isRunning" -> LuaValue.TRUE
            else -> LuaValue.NIL
        }
    }

    // ===========================
    // Redstone Component API
    // ===========================
    private val redstoneOutput = IntArray(6) { 0 }
    private val bundledOutput = Array(6) { IntArray(16) { 0 } }  // 6 sides x 16 colors
    private var wakeThreshold = 0
    private var wirelessFrequency = 0
    private var wirelessOutput = false

    private fun handleRedstoneInvoke(method: String, args: List<Any?>): Varargs {
        return when (method) {
            "getInput" -> {
                val side = args.getOrNull(0) as? Number
                if (side != null) {
                    val s = side.toInt()
                    val level = machine.host.world()
                    val pos = machine.host.hostPosition()
                    if (level != null && s in 0..5) {
                        val dir = net.minecraft.core.Direction.values()[s]
                        val signal = level.getSignal(pos.relative(dir), dir)
                        LuaValue.valueOf(signal)
                    } else LuaValue.valueOf(0)
                } else {
                    // Return table of all sides
                    val t = LuaTable()
                    val level = machine.host.world()
                    val pos = machine.host.hostPosition()
                    if (level != null) {
                        for (s in 0..5) {
                            val dir = net.minecraft.core.Direction.values()[s]
                            t.set(s, LuaValue.valueOf(level.getSignal(pos.relative(dir), dir)))
                        }
                    }
                    t
                }
            }
            "getOutput" -> {
                val side = args.getOrNull(0) as? Number
                if (side != null) {
                    val s = side.toInt()
                    LuaValue.valueOf(if (s in 0..5) redstoneOutput[s] else 0)
                } else {
                    val t = LuaTable()
                    for (s in 0..5) t.set(s, LuaValue.valueOf(redstoneOutput[s]))
                    t
                }
            }
            "setOutput" -> {
                val side = args.getOrNull(0) as? Number
                val value = args.getOrNull(1) as? Number
                if (side != null && value != null) {
                    val s = side.toInt()
                    if (s in 0..5) {
                        val old = redstoneOutput[s]
                        redstoneOutput[s] = value.toInt().coerceIn(0, 15)
                        // Fire redstone_changed signal
                        if (old != redstoneOutput[s]) {
                            machine.signal("redstone_changed", s, old, redstoneOutput[s])
                        }
                        LuaValue.valueOf(old)
                    } else LuaValue.valueOf(0)
                } else LuaValue.valueOf(0)
            }
            // Bundled redstone (for Project Red, etc.) - stubbed
            "getBundledInput" -> {
                val side = args.getOrNull(0) as? Number
                val color = args.getOrNull(1) as? Number
                when {
                    side != null && color != null -> {
                        // getBundledInput(side, color) -> number
                        LuaValue.valueOf(0)  // Bundled cables not implemented
                    }
                    side != null -> {
                        // getBundledInput(side) -> table of 16 colors
                        val t = LuaTable()
                        for (c in 0..15) t.set(c, LuaValue.valueOf(0))
                        t
                    }
                    else -> {
                        // getBundledInput() -> table of 6 sides, each with 16 colors
                        val t = LuaTable()
                        for (s in 0..5) {
                            val sideTable = LuaTable()
                            for (c in 0..15) sideTable.set(c, LuaValue.valueOf(0))
                            t.set(s, sideTable)
                        }
                        t
                    }
                }
            }
            "getBundledOutput" -> {
                val side = args.getOrNull(0) as? Number
                val color = args.getOrNull(1) as? Number
                when {
                    side != null && color != null -> {
                        val s = side.toInt()
                        val c = color.toInt()
                        if (s in 0..5 && c in 0..15) {
                            LuaValue.valueOf(bundledOutput[s][c])
                        } else LuaValue.valueOf(0)
                    }
                    side != null -> {
                        val s = side.toInt()
                        val t = LuaTable()
                        if (s in 0..5) {
                            for (c in 0..15) t.set(c, LuaValue.valueOf(bundledOutput[s][c]))
                        }
                        t
                    }
                    else -> {
                        val t = LuaTable()
                        for (s in 0..5) {
                            val sideTable = LuaTable()
                            for (c in 0..15) sideTable.set(c, LuaValue.valueOf(bundledOutput[s][c]))
                            t.set(s, sideTable)
                        }
                        t
                    }
                }
            }
            "setBundledOutput" -> {
                val side = args.getOrNull(0) as? Number
                val color = args.getOrNull(1) as? Number
                val value = args.getOrNull(2) as? Number
                if (side != null && color != null && value != null) {
                    val s = side.toInt()
                    val c = color.toInt()
                    if (s in 0..5 && c in 0..15) {
                        val old = bundledOutput[s][c]
                        bundledOutput[s][c] = value.toInt().coerceIn(0, 255)
                        if (old != bundledOutput[s][c]) {
                            machine.signal("redstone_changed", s, old, bundledOutput[s][c], c)
                        }
                        LuaValue.valueOf(old)
                    } else LuaValue.valueOf(0)
                } else LuaValue.valueOf(0)
            }
            // Comparator input
            "getComparatorInput" -> {
                val side = args.getOrNull(0) as? Number
                if (side != null) {
                    val s = side.toInt()
                    val level = machine.host.world()
                    val pos = machine.host.hostPosition()
                    if (level != null && s in 0..5) {
                        val dir = net.minecraft.core.Direction.values()[s]
                        val neighborPos = pos.relative(dir)
                        val state = level.getBlockState(neighborPos)
                        // Get analog (comparator) output from the neighbor block
                        val signal = state.getAnalogOutputSignal(level, neighborPos)
                        LuaValue.valueOf(signal)
                    } else LuaValue.valueOf(0)
                } else LuaValue.valueOf(0)
            }
            // Wake threshold - triggers computer wakeup when redstone crosses threshold
            "getWakeThreshold" -> LuaValue.valueOf(wakeThreshold)
            "setWakeThreshold" -> {
                val threshold = args.getOrNull(0) as? Number
                if (threshold != null) {
                    val old = wakeThreshold
                    wakeThreshold = threshold.toInt().coerceIn(0, 15)
                    LuaValue.valueOf(old)
                } else LuaValue.valueOf(wakeThreshold)
            }
            // Wireless redstone (for WR-CBE, etc.) - stubbed
            "getWirelessInput" -> LuaValue.valueOf(false)  // No wireless mod support
            "getWirelessOutput" -> LuaValue.valueOf(wirelessOutput)
            "setWirelessOutput" -> {
                val value = args.getOrNull(0)
                val newValue = when (value) {
                    is Boolean -> value
                    is Number -> value.toInt() != 0
                    else -> false
                }
                val old = wirelessOutput
                wirelessOutput = newValue
                LuaValue.valueOf(old)
            }
            "getWirelessFrequency" -> LuaValue.valueOf(wirelessFrequency)
            "setWirelessFrequency" -> {
                val freq = args.getOrNull(0) as? Number
                if (freq != null) {
                    val old = wirelessFrequency
                    wirelessFrequency = freq.toInt()
                    LuaValue.valueOf(old)
                } else LuaValue.valueOf(wirelessFrequency)
            }
            else -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("no such method: $method")))
        }
    }

    // ===========================
    // Stub Component Handlers (for programs expecting these components)
    // ===========================
    
    // Robot component stub - robots not implemented yet
    private fun handleRobotStub(method: String, args: List<Any?>): Varargs {
        return when (method) {
            "name" -> LuaValue.valueOf("Robot")
            "detect" -> LuaValue.varargsOf(arrayOf(LuaValue.FALSE, LuaValue.valueOf("air")))
            "detectUp" -> LuaValue.varargsOf(arrayOf(LuaValue.FALSE, LuaValue.valueOf("air")))
            "detectDown" -> LuaValue.varargsOf(arrayOf(LuaValue.FALSE, LuaValue.valueOf("air")))
            "select" -> LuaValue.valueOf(1)
            "inventorySize" -> LuaValue.valueOf(0)
            "count" -> LuaValue.valueOf(0)
            "space" -> LuaValue.valueOf(64)
            "tankCount" -> LuaValue.valueOf(0)
            "selectTank" -> LuaValue.valueOf(1)
            "tankLevel" -> LuaValue.valueOf(0)
            "tankSpace" -> LuaValue.valueOf(0)
            "lightColor" -> LuaValue.valueOf(0x66CC00)
            "level" -> LuaValue.valueOf(0)
            "durability" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("no tool equipped")))
            "move", "forward", "back", "up", "down" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("robot not implemented")))
            "turn", "turnLeft", "turnRight", "turnAround" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("robot not implemented")))
            "use", "useUp", "useDown" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("robot not implemented")))
            "swing", "swingUp", "swingDown" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("robot not implemented")))
            "place", "placeUp", "placeDown" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("robot not implemented")))
            "drop", "dropUp", "dropDown" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("robot not implemented")))
            "suck", "suckUp", "suckDown" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("robot not implemented")))
            "compareTo" -> LuaValue.FALSE
            "compareFluid", "compareFluidTo" -> LuaValue.FALSE
            "transferFluid" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("robot not implemented")))
            "drain", "fill" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("robot not implemented")))
            else -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("method not supported: $method")))
        }
    }

    // Drone component stub - drones not implemented yet
    private fun handleDroneStub(method: String, args: List<Any?>): Varargs {
        return when (method) {
            "name" -> LuaValue.valueOf("Drone")
            "getStatusText" -> LuaValue.valueOf("")
            "setStatusText" -> LuaValue.TRUE
            "getLightColor" -> LuaValue.valueOf(0x66CC00)
            "setLightColor" -> LuaValue.valueOf(0x66CC00)
            "getAcceleration" -> LuaValue.valueOf(0.0)
            "setAcceleration" -> LuaValue.valueOf(0.0)
            "getMaxVelocity" -> LuaValue.valueOf(0.0)
            "getVelocity" -> LuaValue.varargsOf(arrayOf(LuaValue.valueOf(0.0), LuaValue.valueOf(0.0), LuaValue.valueOf(0.0)))
            "getPosition" -> LuaValue.varargsOf(arrayOf(LuaValue.valueOf(0.0), LuaValue.valueOf(0.0), LuaValue.valueOf(0.0)))
            "getOffset" -> LuaValue.varargsOf(arrayOf(LuaValue.valueOf(0.0), LuaValue.valueOf(0.0), LuaValue.valueOf(0.0)))
            "move" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("drone not implemented")))
            else -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("method not supported: $method")))
        }
    }

    // Hologram component stub - holograms not implemented yet
    private fun handleHologramStub(method: String, args: List<Any?>): Varargs {
        return when (method) {
            "clear" -> LuaValue.NONE
            "get" -> LuaValue.valueOf(0)
            "set" -> LuaValue.NONE
            "fill" -> LuaValue.NONE
            "copy" -> LuaValue.NONE
            "maxDepth" -> LuaValue.valueOf(1)
            "getScale" -> LuaValue.valueOf(1.0)
            "setScale" -> LuaValue.NONE
            "getTranslation" -> LuaValue.varargsOf(arrayOf(LuaValue.valueOf(0.0), LuaValue.valueOf(0.0), LuaValue.valueOf(0.0)))
            "setTranslation" -> LuaValue.NONE
            "getRotation" -> LuaValue.varargsOf(arrayOf(LuaValue.valueOf(0.0), LuaValue.valueOf(0.0), LuaValue.valueOf(0.0), LuaValue.valueOf(0.0)))
            "setRotation" -> LuaValue.NONE
            "getRotationSpeed" -> LuaValue.varargsOf(arrayOf(LuaValue.valueOf(0.0), LuaValue.valueOf(0.0)))
            "setRotationSpeed" -> LuaValue.NONE
            "getPaletteColor" -> LuaValue.valueOf(0xFFFFFF)
            "setPaletteColor" -> LuaValue.valueOf(0xFFFFFF)
            else -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("method not supported: $method")))
        }
    }

    // Modem/Network component stub - networking not implemented yet
    private val modemOpenPorts = mutableSetOf<Int>()
    private fun handleModemStub(method: String, args: List<Any?>): Varargs {
        return when (method) {
            "isWireless" -> LuaValue.FALSE
            "isWired" -> LuaValue.TRUE
            "getWakeMessage" -> LuaValue.valueOf("")
            "setWakeMessage" -> LuaValue.valueOf("")
            "getStrength" -> LuaValue.valueOf(0)
            "setStrength" -> LuaValue.valueOf(0)
            "maxPacketSize" -> LuaValue.valueOf(8192)
            "open" -> {
                val port = (args.getOrNull(0) as? Number)?.toInt() ?: 0
                if (port in 1..65535) {
                    modemOpenPorts.add(port)
                    LuaValue.TRUE
                } else LuaValue.FALSE
            }
            "close" -> {
                val port = args.getOrNull(0) as? Number
                if (port != null) {
                    modemOpenPorts.remove(port.toInt())
                } else {
                    modemOpenPorts.clear()
                }
                LuaValue.TRUE
            }
            "isOpen" -> {
                val port = (args.getOrNull(0) as? Number)?.toInt() ?: 0
                LuaValue.valueOf(modemOpenPorts.contains(port))
            }
            "send" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("network not implemented")))
            "broadcast" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("network not implemented")))
            "getChannel" -> LuaValue.valueOf("") // For tunnel component
            else -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("method not supported: $method")))
        }
    }

    // Data card stub - provides hashing/crypto functions
    private fun handleDataCardStub(method: String, args: List<Any?>): Varargs {
        val data = args.getOrNull(0)?.toString()?.toByteArray() ?: ByteArray(0)
        return when (method) {
            "crc32" -> {
                val crc = java.util.zip.CRC32()
                crc.update(data)
                LuaValue.valueOf(crc.value.toInt())
            }
            "md5" -> {
                val digest = java.security.MessageDigest.getInstance("MD5").digest(data)
                LuaString.valueOf(digest)
            }
            "sha256" -> {
                val digest = java.security.MessageDigest.getInstance("SHA-256").digest(data)
                LuaString.valueOf(digest)
            }
            "encode64" -> {
                LuaValue.valueOf(java.util.Base64.getEncoder().encodeToString(data))
            }
            "decode64" -> {
                try {
                    val decoded = java.util.Base64.getDecoder().decode(data)
                    LuaString.valueOf(decoded)
                } catch (e: Exception) {
                    LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("invalid base64")))
                }
            }
            "deflate" -> {
                try {
                    val output = java.io.ByteArrayOutputStream()
                    val deflater = java.util.zip.DeflaterOutputStream(output)
                    deflater.write(data)
                    deflater.close()
                    LuaString.valueOf(output.toByteArray())
                } catch (e: Exception) {
                    LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("compression failed")))
                }
            }
            "inflate" -> {
                try {
                    val inflater = java.util.zip.InflaterInputStream(java.io.ByteArrayInputStream(data))
                    val output = inflater.readBytes()
                    inflater.close()
                    LuaString.valueOf(output)
                } catch (e: Exception) {
                    LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("decompression failed")))
                }
            }
            "random" -> {
                val count = (args.getOrNull(0) as? Number)?.toInt() ?: 1
                val bytes = ByteArray(count.coerceIn(1, 1024))
                java.security.SecureRandom().nextBytes(bytes)
                LuaString.valueOf(bytes)
            }
            "generateKeyPair", "ecdsa", "ecdh" -> {
                LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("elliptic curve crypto not implemented")))
            }
            "serialize", "deserialize" -> {
                LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("serialization not implemented")))
            }
            else -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("method not supported: $method")))
        }
    }

    // Debug card stub - creative-only OP component
    private fun handleDebugCardStub(method: String, args: List<Any?>): Varargs {
        return when (method) {
            "getWorld" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("debug card: not available in survival")))
            "setWorld" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("debug card: not available in survival")))
            "getPlayer" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("debug card: not available in survival")))
            "getPlayers" -> LuaTable()
            "isModLoaded" -> LuaValue.FALSE
            "runCommand" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("debug card: not available in survival")))
            "getScoreboard" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("debug card: not available in survival")))
            "getX", "getY", "getZ" -> LuaValue.valueOf(0.0)
            "sendToDebugCard" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("debug card: not available in survival")))
            "sendToClipboard" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("debug card: not available in survival")))
            "changeBuffer" -> LuaValue.valueOf(0.0)
            "connectToBlock" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("debug card: not available in survival")))
            "test" -> LuaValue.valueOf("debug card test")
            else -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("method not supported: $method")))
        }
    }

    // Inventory Controller stub - inventory interaction not implemented
    private fun handleInventoryControllerStub(method: String, args: List<Any?>): Varargs {
        return when (method) {
            "getInventorySize" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("no inventory")))
            "getStackInSlot" -> LuaValue.NIL
            "getSlotMaxStackSize" -> LuaValue.valueOf(64)
            "getSlotStackSize" -> LuaValue.valueOf(0)
            "compareStacks" -> LuaValue.FALSE
            "getItemInventorySize" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("no item")))
            "getStackInInternalSlot" -> LuaValue.NIL
            "dropIntoSlot" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("inventory controller not implemented")))
            "suckFromSlot" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("inventory controller not implemented")))
            "equip" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("inventory controller not implemented")))
            "store" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("inventory controller not implemented")))
            "storeInternal" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("inventory controller not implemented")))
            else -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("method not supported: $method")))
        }
    }

    // Tank Controller stub - fluid interaction not implemented
    private fun handleTankControllerStub(method: String, args: List<Any?>): Varargs {
        return when (method) {
            "getTankCount" -> LuaValue.valueOf(0)
            "getFluidInTank" -> LuaValue.NIL
            "getTankCapacity" -> LuaValue.valueOf(0)
            "getFluidInInternalTank" -> LuaValue.NIL
            "drain" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("tank controller not implemented")))
            "fill" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("tank controller not implemented")))
            else -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("method not supported: $method")))
        }
    }

    // Experience upgrade stub
    private fun handleExperienceStub(method: String, args: List<Any?>): Varargs {
        return when (method) {
            "level" -> LuaValue.valueOf(0.0)
            else -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("method not supported: $method")))
        }
    }

    // Generator upgrade stub
    private fun handleGeneratorStub(method: String, args: List<Any?>): Varargs {
        return when (method) {
            "count" -> LuaValue.valueOf(0)
            "insert" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("generator not implemented")))
            "remove" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("generator not implemented")))
            else -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("method not supported: $method")))
        }
    }

    // Crafting upgrade stub
    private fun handleCraftingStub(method: String, args: List<Any?>): Varargs {
        return when (method) {
            "craft" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("crafting not implemented")))
            else -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("method not supported: $method")))
        }
    }

    // Geolyzer stub
    private fun handleGeolyzerStub(method: String, args: List<Any?>): Varargs {
        return when (method) {
            "scan" -> {
                // Return empty scan result
                val t = LuaTable()
                t.set("n", LuaValue.valueOf(0))
                t
            }
            "analyze" -> LuaValue.NIL
            "store" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("geolyzer not implemented")))
            "detect" -> LuaValue.varargsOf(arrayOf(LuaValue.FALSE, LuaValue.valueOf("air")))
            else -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("method not supported: $method")))
        }
    }

    // Navigation upgrade stub
    private fun handleNavigationStub(method: String, args: List<Any?>): Varargs {
        return when (method) {
            "getPosition" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("navigation not implemented")))
            "getFacing" -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("navigation not implemented")))
            "getRange" -> LuaValue.valueOf(0)
            "findWaypoints" -> LuaTable()
            else -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("method not supported: $method")))
        }
    }

    // ===========================
    // Keyboard Component API
    // ===========================
    private var keyboardLayoutName: String = "en_US"
    private val pressedKeys = mutableMapOf<Int, Int>()  // code -> char
    
    private fun handleKeyboardInvoke(method: String, args: List<Any?>): Varargs {
        return when (method) {
            "getLayoutName" -> LuaValue.valueOf(keyboardLayoutName)
            "setLayoutName" -> {
                val name = args.getOrNull(0)?.toString() ?: "en_US"
                keyboardLayoutName = name
                LuaValue.valueOf(name)
            }
            "getPressedKeys" -> {
                // Returns a table of currently pressed keys: code -> char
                val t = LuaTable()
                for ((code, char) in pressedKeys) {
                    t.set(code, LuaValue.valueOf(char))
                }
                t
            }
            "getPressedCodes" -> {
                // Returns an array of currently pressed key codes
                val t = LuaTable()
                pressedKeys.keys.forEachIndexed { i, code -> t.set(i + 1, LuaValue.valueOf(code)) }
                t
            }
            "isAltDown" -> LuaValue.FALSE  // Would need actual input tracking
            "isControl" -> {
                val code = (args.getOrNull(0) as? Number)?.toInt() ?: 0
                LuaValue.valueOf(code in 0..31)
            }
            "isControlDown" -> LuaValue.FALSE  // Would need actual input tracking
            "isKeyDown" -> {
                val code = (args.getOrNull(0) as? Number)?.toInt() ?: 0
                LuaValue.valueOf(pressedKeys.containsKey(code))
            }
            "isShiftDown" -> LuaValue.FALSE  // Would need actual input tracking
            else -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("method not supported: $method")))
        }
    }

    // ===========================
    // Filesystem Component API
    // ===========================
    private fun handleFilesystemInvoke(address: String, method: String, args: List<Any?>): Varargs {
        val vfs = filesystems[address]
            ?: return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("no such filesystem")))

        return try {
            when (method) {
                "exists" -> {
                    val path = args.getOrNull(0)?.toString() ?: "/"
                    LuaValue.valueOf(vfs.exists(path))
                }
                "isDirectory" -> {
                    val path = args.getOrNull(0)?.toString() ?: "/"
                    LuaValue.valueOf(vfs.isDirectory(path))
                }
                "list" -> {
                    val path = args.getOrNull(0)?.toString() ?: "/"
                    val entries = vfs.list(path)
                    if (entries != null) {
                        val t = LuaTable()
                        entries.forEachIndexed { i, name -> t.set(i + 1, LuaValue.valueOf(name)) }
                        t
                    } else {
                        LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("no such directory")))
                    }
                }
                "size" -> {
                    val path = args.getOrNull(0)?.toString() ?: "/"
                    LuaValue.valueOf(vfs.size(path).toDouble())
                }
                "lastModified" -> {
                    val path = args.getOrNull(0)?.toString() ?: "/"
                    LuaValue.valueOf(vfs.lastModified(path).toDouble())
                }
                "makeDirectory" -> {
                    val path = args.getOrNull(0)?.toString() ?: return LuaValue.FALSE
                    LuaValue.valueOf(vfs.makeDirectory(path))
                }
                "remove" -> {
                    val path = args.getOrNull(0)?.toString() ?: return LuaValue.FALSE
                    LuaValue.valueOf(vfs.remove(path))
                }
                "rename" -> {
                    val from = args.getOrNull(0)?.toString() ?: return LuaValue.FALSE
                    val to = args.getOrNull(1)?.toString() ?: return LuaValue.FALSE
                    LuaValue.valueOf(vfs.rename(from, to))
                }
                "open" -> {
                    val path = args.getOrNull(0)?.toString() ?: return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("path required")))
                    val mode = args.getOrNull(1)?.toString() ?: "r"
                    val handle = vfs.open(path, mode)
                    if (handle != null && handle >= 0) {
                        LuaValue.valueOf(handle)
                    } else {
                        LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("cannot open file")))
                    }
                }
                "read" -> {
                    val handle = (args.getOrNull(0) as? Number)?.toInt()
                        ?: return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("bad file descriptor")))
                    val rawCount = (args.getOrNull(1) as? Number)?.toDouble() ?: Double.MAX_VALUE
                    val count = rawCount.coerceAtMost(Int.MAX_VALUE.toDouble()).toInt().coerceAtLeast(0)
                    val data = vfs.read(handle, count)
                    if (data != null && data.isNotEmpty()) {
                        LuaString.valueOf(data)
                    } else {
                        LuaValue.NIL
                    }
                }
                "write" -> {
                    val handle = (args.getOrNull(0) as? Number)?.toInt()
                        ?: return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("bad file descriptor")))
                    val data = args.getOrNull(1)?.toString()
                        ?: return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("bad argument")))
                    LuaValue.valueOf(vfs.write(handle, data.toByteArray(Charsets.ISO_8859_1)))
                }
                "seek" -> {
                    val handle = (args.getOrNull(0) as? Number)?.toInt()
                        ?: return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("bad file descriptor")))
                    val whence = args.getOrNull(1)?.toString() ?: "cur"
                    val offset = (args.getOrNull(2) as? Number)?.toLong() ?: 0L
                    val pos = vfs.seek(handle, whence, offset)
                    if (pos != null) {
                        LuaValue.valueOf(pos.toDouble())
                    } else {
                        LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("bad file descriptor")))
                    }
                }
                "close" -> {
                    val handle = (args.getOrNull(0) as? Number)?.toInt() ?: return LuaValue.NONE
                    vfs.close(handle)
                    LuaValue.NONE
                }
                "spaceTotal" -> LuaValue.valueOf(vfs.capacity.toDouble())
                "spaceUsed" -> LuaValue.valueOf(vfs.spaceUsed.toDouble())
                "isReadOnly" -> LuaValue.valueOf(vfs.readOnly)
                "getLabel" -> LuaValue.valueOf(vfs.label)
                "setLabel" -> {
                    val label = args.getOrNull(0)?.toString() ?: ""
                    vfs.label = label
                    LuaValue.valueOf(label)
                }
                "isLink" -> {
                    // Symlinks not supported - always return false
                    LuaValue.FALSE
                }
                "link" -> {
                    // Symlinks not supported
                    LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("symbolic links not supported")))
                }
                else -> LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("no such method: $method")))
            }
        } catch (e: Exception) {
            LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf(e.message ?: "filesystem error")))
        }
    }

    // ===========================
    // Internet API
    // ===========================
    private fun setupInternetAPI() {
        val g = globals ?: return
        val inet = LuaTable()

        // internet.request(url, [postData], [headers], [method]) → handle
        inet.set("request", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val url = args.arg1().checkjstring()
                val postData = if (args.narg() >= 2 && !args.arg(2).isnil()) args.arg(2).tojstring() else null
                val headers = mutableMapOf<String, String>()
                if (args.narg() >= 3 && args.arg(3).istable()) {
                    val t = args.arg(3).checktable()
                    var key: LuaValue = LuaValue.NIL
                    while (true) {
                        val n = t.next(key)
                        if (n.arg1().isnil()) break
                        key = n.arg1()
                        headers[key.tojstring()] = n.arg(2).tojstring()
                    }
                }
                val httpMethod = if (args.narg() >= 4 && !args.arg(4).isnil()) args.arg(4).tojstring() else null

                // Return a response handle table
                val handle = LuaTable()
                var responseData: String? = null
                var error: String? = null
                var finished = false
                var respCode = 0
                var respMessage = ""
                val respHeaders = LuaTable()

                // Perform HTTP request on a background thread
                val thread = Thread({
                    try {
                        val conn = URI(url).toURL().openConnection() as HttpURLConnection
                        conn.connectTimeout = 10000
                        conn.readTimeout = 10000
                        conn.setRequestProperty("User-Agent", "OpenCoudou/3.0")
                        for ((k, v) in headers) conn.setRequestProperty(k, v)
                        if (httpMethod != null) conn.requestMethod = httpMethod
                        if (postData != null) {
                            if (httpMethod == null) conn.requestMethod = "POST"
                            conn.doOutput = true
                            conn.outputStream.use { it.write(postData.toByteArray()) }
                        }
                        respCode = conn.responseCode
                        respMessage = conn.responseMessage ?: ""
                        conn.headerFields?.forEach { (k, v) ->
                            if (k != null && v != null) {
                                respHeaders.set(k, LuaValue.valueOf(v.joinToString(", ")))
                            }
                        }
                        val stream = if (respCode in 200..299) conn.inputStream else conn.errorStream
                        val reader = BufferedReader(InputStreamReader(stream ?: conn.inputStream))
                        responseData = reader.readText()
                        reader.close()
                        finished = true
                    } catch (e: Exception) {
                        error = e.message
                        finished = true
                    }
                }, "OC-HTTP-${url.take(30)}")
                thread.isDaemon = true
                thread.start()

                // handle.finishConnect() → true when done
                handle.set("finishConnect", object : VarArgFunction() {
                    override fun invoke(args: Varargs): Varargs {
                        if (!finished) return LuaValue.FALSE
                        if (error != null) return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf(error)))
                        return LuaValue.TRUE
                    }
                })

                // handle.read([n or mode]) → data or nil
                // - read(n) - read up to n bytes
                // - read("*a") - read all remaining
                // - read("*l") - read line (without newline)
                handle.set("read", object : VarArgFunction() {
                    private var offset = 0
                    override fun invoke(args: Varargs): Varargs {
                        if (!finished) return LuaValue.valueOf("")
                        val data = responseData ?: return LuaValue.NIL
                        if (offset >= data.length) return LuaValue.NIL
                        
                        val chunk: String
                        if (args.narg() > 0 && !args.arg1().isnil()) {
                            val v = args.arg1()
                            if (v.isstring()) {
                                val mode = v.tojstring()
                                when (mode) {
                                    "*a" -> {
                                        // Read all remaining data
                                        chunk = data.substring(offset)
                                        offset = data.length
                                    }
                                    "*l" -> {
                                        // Read one line (without newline)
                                        val newlineIdx = data.indexOf('\n', offset)
                                        if (newlineIdx != -1) {
                                            val endIdx = if (newlineIdx > offset && data[newlineIdx - 1] == '\r') newlineIdx - 1 else newlineIdx
                                            chunk = data.substring(offset, endIdx)
                                            offset = newlineIdx + 1
                                        } else {
                                            chunk = data.substring(offset)
                                            offset = data.length
                                        }
                                    }
                                    else -> {
                                        // Unknown mode, read all
                                        chunk = data.substring(offset)
                                        offset = data.length
                                    }
                                }
                            } else {
                                // Read n bytes
                                val n = v.checkint()
                                chunk = data.substring(offset, minOf(offset + n, data.length))
                                offset += chunk.length
                            }
                        } else {
                            // No argument: read all
                            chunk = data.substring(offset)
                            offset = data.length
                        }
                        return if (chunk.isEmpty()) LuaValue.NIL else LuaValue.valueOf(chunk)
                    }
                })

                // handle.response() → code, message, headers
                handle.set("response", object : VarArgFunction() {
                    override fun invoke(args: Varargs): Varargs {
                        if (!finished) return LuaValue.NIL
                        if (error != null) return LuaValue.varargsOf(arrayOf(
                            LuaValue.NIL, LuaValue.valueOf(error)
                        ))
                        return LuaValue.varargsOf(arrayOf(
                            LuaValue.valueOf(respCode),
                            LuaValue.valueOf(respMessage),
                            respHeaders
                        ))
                    }
                })

                handle.set("close", object : ZeroArgFunction() {
                    override fun call(): LuaValue = LuaValue.NONE
                })

                return handle
            }
        })

        // Simple synchronous version for convenience
        inet.set("isHttpEnabled", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.TRUE
        })

        inet.set("isTcpEnabled", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.FALSE
        })

        inet.set("connect", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                return LuaValue.varargsOf(arrayOf(LuaValue.NIL, LuaValue.valueOf("tcp connections are not available")))
            }
        })

        g.set("internet", inet)
    }

    private fun handleInternetInvoke(method: String, args: List<Any?>): Varargs {
        val inet = globals?.get("internet") ?: return LuaValue.NIL
        val fn = inet.get(method)
        if (fn.isnil()) return LuaValue.NIL
        val luaArgs = args.map { convertToLua(it) }.toTypedArray()
        return fn.invoke(LuaValue.varargsOf(luaArgs))
    }

    // ===========================
    // unicode API
    // ===========================
    private fun setupUnicodeAPI() {
        val g = globals ?: return
        val uni = LuaTable()
        uni.set("len", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val s = arg.checkjstring()
                return LuaValue.valueOf(s.codePointCount(0, s.length))
            }
        })
        uni.set("sub", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val s = args.arg1().checkjstring()
                val sLength = s.codePointCount(0, s.length)
                val i = args.arg(2).checkint()
                val j = if (args.narg() >= 3) args.arg(3).checkint() else sLength
                val start = when {
                    i < 0 -> s.offsetByCodePoints(s.length, maxOf(i, -sLength))
                    i == 0 -> 0
                    else -> s.offsetByCodePoints(0, minOf(i - 1, sLength))
                }
                val end = when {
                    j < 0 -> s.offsetByCodePoints(s.length, maxOf(j + 1, -sLength))
                    else -> s.offsetByCodePoints(0, minOf(j, sLength))
                }
                return if (start < end) LuaValue.valueOf(s.substring(start, end)) else LuaValue.valueOf("")
            }
        })
        uni.set("char", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val sb = StringBuilder()
                for (i in 1..args.narg()) sb.appendCodePoint(args.arg(i).checkint())
                return LuaValue.valueOf(sb.toString())
            }
        })
        uni.set("upper", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = LuaValue.valueOf(arg.checkjstring().uppercase())
        })
        uni.set("lower", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = LuaValue.valueOf(arg.checkjstring().lowercase())
        })
        uni.set("wlen", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val s = arg.checkjstring()
                var width = 0
                var i = 0
                while (i < s.length) {
                    val cp = s.codePointAt(i)
                    width += maxOf(1, wcwidth(cp))
                    i += Character.charCount(cp)
                }
                return LuaValue.valueOf(width)
            }
        })
        uni.set("wtrunc", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val s = arg1.checkjstring()
                val count = arg2.checkint()
                var width = 0
                var end = 0
                while (end < s.length && width < count) {
                    val cp = s.codePointAt(end)
                    val cw = maxOf(1, wcwidth(cp))
                    if (width + cw > count) break
                    width += cw
                    end += Character.charCount(cp)
                }
                return LuaValue.valueOf(s.substring(0, end))
            }
        })
        uni.set("reverse", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = LuaValue.valueOf(arg.checkjstring().reversed())
        })
        uni.set("isWide", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val s = arg.checkjstring()
                if (s.isEmpty()) return LuaValue.FALSE
                return LuaValue.valueOf(wcwidth(s.codePointAt(0)) > 1)
            }
        })
        uni.set("charWidth", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val s = arg.checkjstring()
                if (s.isEmpty()) return LuaValue.valueOf(1)
                return LuaValue.valueOf(wcwidth(s.codePointAt(0)))
            }
        })
        g.set("unicode", uni)
    }

    /**
     * Simplified wcwidth: returns the display width of a Unicode code point.
     * CJK characters occupy 2 columns, most others 1.
     */
    private fun wcwidth(cp: Int): Int {
        // Control characters
        if (cp < 32 || cp == 0x7F) return 0
        if (cp in 0x0300..0x036F) return 0 // Combining diacriticals
        if (cp in 0x1AB0..0x1AFF) return 0 // Combining diacriticals extended
        if (cp in 0x1DC0..0x1DFF) return 0 // Combining diacriticals supplement
        if (cp in 0x20D0..0x20FF) return 0 // Combining marks for symbols
        if (cp in 0xFE20..0xFE2F) return 0 // Combining half marks
        // CJK ranges
        if (cp in 0x1100..0x115F) return 2 // Hangul Jamo
        if (cp in 0x2E80..0x303E) return 2 // CJK radicals, Kangxi, ideographic description, CJK symbols
        if (cp in 0x3040..0x33BF) return 2 // Hiragana, Katakana, Bopomofo, Hangul compat jamo, Kanbun, CJK strokes
        if (cp in 0x33C0..0x33FF) return 2 // CJK compat ideographs
        if (cp in 0x3400..0x4DBF) return 2 // CJK unified ideographs extension A
        if (cp in 0x4E00..0x9FFF) return 2 // CJK unified ideographs
        if (cp in 0xA000..0xA4CF) return 2 // Yi
        if (cp in 0xAC00..0xD7AF) return 2 // Hangul syllables
        if (cp in 0xF900..0xFAFF) return 2 // CJK compatibility ideographs
        if (cp in 0xFE10..0xFE19) return 2 // Vertical forms
        if (cp in 0xFE30..0xFE6F) return 2 // CJK compatibility forms
        if (cp in 0xFF01..0xFF60) return 2 // Fullwidth forms
        if (cp in 0xFFE0..0xFFE6) return 2 // Fullwidth signs
        if (cp in 0x20000..0x2FFFF) return 2 // CJK unified ideographs ext B-F
        if (cp in 0x30000..0x3FFFF) return 2 // CJK unified ideographs ext G+
        return 1
    }

    // ===========================
    // Conversion helpers
    // ===========================
    private fun convertFromLua(value: LuaValue): Any? = when {
        value.isnil() -> null
        value.isboolean() -> value.toboolean()
        value.isint() -> value.toint()
        value.isnumber() -> value.todouble()
        value.isstring() -> value.tojstring()
        value.istable() -> {
            val table = value.checktable()
            val map = mutableMapOf<Any?, Any?>()
            var key: LuaValue = LuaValue.NIL
            while (true) {
                val n = table.next(key)
                if (n.arg1().isnil()) break
                key = n.arg1()
                map[convertFromLua(key)] = convertFromLua(n.arg(2))
            }
            map
        }
        else -> value.tojstring()
    }

    private fun convertToLua(value: Any?): LuaValue = when (value) {
        null -> LuaValue.NIL
        is Boolean -> LuaValue.valueOf(value)
        is Int -> LuaValue.valueOf(value)
        is Long -> LuaValue.valueOf(value.toDouble())
        is Double -> LuaValue.valueOf(value)
        is Float -> LuaValue.valueOf(value.toDouble())
        is String -> LuaValue.valueOf(value)
        is ByteArray -> LuaValue.valueOf(String(value))
        is Array<*> -> {
            val t = LuaTable()
            value.forEachIndexed { i, v -> t.set(i + 1, convertToLua(v)) }
            t
        }
        is Map<*, *> -> {
            val t = LuaTable()
            value.forEach { (k, v) -> t.set(convertToLua(k), convertToLua(v)) }
            t
        }
        else -> LuaValue.valueOf(value.toString())
    }

    private fun convertToLuaVarargs(result: Array<Any?>?): Varargs {
        if (result == null || result.isEmpty()) return LuaValue.NONE
        return LuaValue.varargsOf(result.map { convertToLua(it) }.toTypedArray())
    }

    // ===========================
    // State
    // ===========================
    private var bootAddress: String? = null
    private var eepromDataBytes: ByteArray = ByteArray(0)  // Separate 256-byte user-writable EEPROM data area (binary-safe)
    private val computerUsers = mutableSetOf<String>()  // Authorized users for this computer
    private val maxUsers = 16  // Maximum number of users
    private var tmpFsAddress = java.util.UUID.randomUUID().toString().take(8)
    private var hardDriveAddress = java.util.UUID.randomUUID().toString()
    private var lootDiskAddress = java.util.UUID.randomUUID().toString()
    // Virtual component addresses (persisted so they survive world save/load)
    private var gpuAddress: String = ""
    private var internetAddress: String = ""
    private var redstoneAddress: String = ""
    private var eepromAddress: String = ""
    private val signalSemaphore = java.util.concurrent.Semaphore(0)
    private val filesystems = mutableMapOf<String, VirtualFileSystem>()

    override fun recomputeMemory(memory: Iterable<ItemStack>): Boolean = true

    override fun close() {
        // Close all open file handles to prevent leaks on shutdown/crash
        for ((_, vfs) in filesystems) {
            vfs.closeAllHandles()
        }
        globals = null
        mainCoroutine = null
        initialized = false
    }

    // ===========================
    // Execution - coroutine based event loop
    // ===========================
    override fun runThreaded(isSynchronizedReturn: Boolean): ExecutionResult {
        val g = globals ?: return ExecutionResult.Error("Not initialized")

        try {
            if (mainCoroutine == null) {
                // Register built-in virtual components
                registerVirtualComponents()

                // Load BIOS from EEPROM content (like original OC)
                val biosCode = eepromCode.ifEmpty { BIOS_CODE }
                val chunk = g.load(biosCode, "=bios")
                mainCoroutine = LuaThread(g, chunk)
            }

            val co = mainCoroutine!!

            // Resume coroutine with pending signal (or nothing if first call)
            val resumeArgs = if (pendingSignal != null) {
                val sig = pendingSignal!!
                pendingSignal = null
                sig
            } else {
                LuaValue.NONE
            }

            val result: Varargs
            lastResumeTime = System.currentTimeMillis()
            result = co.resume(resumeArgs)
            val status = result.arg1().toboolean()

            if (!status) {
                // Coroutine error
                val err = result.arg(2).tojstring()
                return ExecutionResult.Error(err)
            }

            if (co.status == "dead") {
                return ExecutionResult.Shutdown
            }

            // Coroutine yielded - it called computer.pullSignal(timeout)
            val timeout = if (result.narg() >= 2 && result.arg(2).isnumber())
                result.arg(2).todouble() else 1.0

            // Wait for signals with proper timeout (woken immediately by onSignal)
            val sm = machine as? SimpleMachine
            val sleepMs = (timeout * 1000).toLong().coerceIn(0, 5000)
            val deadline = System.currentTimeMillis() + sleepMs
            var delivered = false
            signalSemaphore.drainPermits()

            do {
                val signal = sm?.pollSignal()
                if (signal != null) {
                    if (signal.name == "__shutdown") return ExecutionResult.Shutdown
                    if (signal.name == "__reboot") {
                        sm?.pendingReboot = true
                        return ExecutionResult.Shutdown
                    }
                    val sigArgs = mutableListOf<LuaValue>(LuaValue.valueOf(signal.name))
                    signal.args.forEach { sigArgs.add(convertToLua(it)) }
                    pendingSignal = LuaValue.varargsOf(sigArgs.toTypedArray())
                    delivered = true
                    break
                }
                val remaining = deadline - System.currentTimeMillis()
                if (remaining <= 0) break
                try {
                    signalSemaphore.tryAcquire(minOf(remaining, 50), TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return ExecutionResult.Shutdown
                }
            } while (true)

            if (!delivered) {
                pendingSignal = LuaValue.NONE
            }

            return ExecutionResult.Sleep(0)

        } catch (e: LuaError) {
            OCLogger.error("Lua error: ${e.message}")
            return ExecutionResult.Error(e.message ?: "Lua error")
        } catch (e: Exception) {
            OCLogger.error("Machine error: ${e.message}")
            return ExecutionResult.Error(e.message ?: "Unknown error")
        }
    }

    private fun registerVirtualComponents() {
        // Register components based on what's actually installed in the case
        val sm = machine as? SimpleMachine ?: return
        val installed = sm.installedComponents
        val screen = findNearbyScreen()

        // GPU - only if a graphics card is installed
        if (installed.gpuTier >= 0) {
            if (gpuAddress.isEmpty()) gpuAddress = "gpu-" + java.util.UUID.randomUUID().toString().take(8)
            sm.registerComponent(gpuAddress, "gpu")
        }

        // Screen - if one is nearby
        if (screen != null) {
            sm.registerComponent(screen.address, "screen")
            // Keyboard - associated with the screen
            val kbAddr = screen.keyboardAddress ?: ("kb-" + screen.address)
            sm.registerComponent(kbAddr, "keyboard")
        }

        // Internet card is always available for now
        if (internetAddress.isEmpty()) internetAddress = "inet-" + java.util.UUID.randomUUID().toString().take(8)
        sm.registerComponent(internetAddress, "internet")

        // Redstone - basic vanilla redstone I/O
        if (redstoneAddress.isEmpty()) redstoneAddress = "redstone-" + java.util.UUID.randomUUID().toString().take(8)
        sm.registerComponent(redstoneAddress, "redstone")

        // EEPROM - only if installed
        if (installed.hasEEPROM) {
            if (eepromAddress.isEmpty()) eepromAddress = "eeprom-" + java.util.UUID.randomUUID().toString().take(8)
            sm.registerComponent(eepromAddress, "eeprom")
        }

        // Filesystem components
        // 1. tmpfs - small volatile storage (always available)
        if (!filesystems.containsKey(tmpFsAddress)) {
            filesystems[tmpFsAddress] = VirtualFileSystem(capacity = 64 * 1024, readOnly = false, label = "tmpfs")
        }
        sm.registerComponent(tmpFsAddress, "filesystem")

        // 2. Hard drive - only if HDD is installed
        if (installed.hddTiers.isNotEmpty()) {
            val hddCapacity = installed.hddTiers.maxOrNull()?.let { tier ->
                when (tier) {
                    0 -> 1L * 1024 * 1024   // 1 MB
                    1 -> 2L * 1024 * 1024   // 2 MB
                    2 -> 4L * 1024 * 1024   // 4 MB
                    else -> 2L * 1024 * 1024
                }
            } ?: (2L * 1024 * 1024)
            if (!filesystems.containsKey(hardDriveAddress)) {
                val hdd = VirtualFileSystem(capacity = hddCapacity, readOnly = false, label = "")
                hdd.makeDirectory("home")
                hdd.makeDirectory("tmp")
                filesystems[hardDriveAddress] = hdd
            }
            sm.registerComponent(hardDriveAddress, "filesystem")
        }

        // 3. Loot disk - read-only OpenOS floppy
        val lootDisk = VirtualFileSystem(capacity = 2 * 1024 * 1024, readOnly = false, label = "OpenOS")
        OpenOSContent.populate(lootDisk)
        lootDisk.readOnly = true  // Make read-only after populating
        filesystems[lootDiskAddress] = lootDisk
        sm.registerComponent(lootDiskAddress, "filesystem")

        // Auto-set boot address to loot disk if not set
        if (bootAddress == null) {
            bootAddress = lootDiskAddress
        }
        // Initialize EEPROM data to boot address if empty (matches OC default behavior)
        if (eepromDataBytes.isEmpty()) {
            eepromDataBytes = (bootAddress ?: "").toByteArray(Charsets.ISO_8859_1)
        }
    }

    override fun runSynchronized() {}
    override fun onSignal() {
        signalSemaphore.release()
    }

    fun saveData(tag: CompoundTag) {}
    fun loadData(tag: CompoundTag) {}

    override fun save(tag: CompoundTag) {
        // Persist addresses
        tag.putString("bootAddress", bootAddress ?: "")
        tag.putString("tmpFsAddress", tmpFsAddress)
        tag.putString("hardDriveAddress", hardDriveAddress)
        tag.putString("lootDiskAddress", lootDiskAddress)

        // Persist virtual component addresses (so they survive restart)
        tag.putString("gpuAddress", gpuAddress)
        tag.putString("internetAddress", internetAddress)
        tag.putString("redstoneAddress", redstoneAddress)
        tag.putString("eepromAddress", eepromAddress)

        // Persist EEPROM state (user's BIOS code and label)
        tag.putString("eepromCode", eepromCode)
        tag.putString("eepromLabel", eepromLabel)
        tag.putByteArray("eepromDataBytes", eepromDataBytes)

        // Persist GPU/screen state
        tag.putInt("gpuDepth", gpuDepth)
        tag.putBoolean("screenIsOn", screenIsOn)
        tag.putString("boundScreenAddr", boundScreenAddr ?: "")

        // Persist redstone output levels
        tag.putIntArray("redstoneOutput", redstoneOutput)

        // Persist the hard drive filesystem (the user's data)
        val hdd = filesystems[hardDriveAddress]
        if (hdd != null) {
            tag.put("hardDriveData", hdd.saveToTag())
        }

        // Persist tmpfs too (volatile in original OC, but nice to keep across saves)
        val tmp = filesystems[tmpFsAddress]
        if (tmp != null) {
            tag.put("tmpFsData", tmp.saveToTag())
        }
    }

    override fun load(tag: CompoundTag) {
        // Restore addresses
        val ba = tag.getString("bootAddress")
        if (ba.isNotEmpty()) bootAddress = ba
        val ta = tag.getString("tmpFsAddress")
        if (ta.isNotEmpty()) tmpFsAddress = ta
        val ha = tag.getString("hardDriveAddress")
        if (ha.isNotEmpty()) hardDriveAddress = ha
        val la = tag.getString("lootDiskAddress")
        if (la.isNotEmpty()) lootDiskAddress = la

        // Restore virtual component addresses
        val ga = tag.getString("gpuAddress")
        if (ga.isNotEmpty()) gpuAddress = ga
        val ia = tag.getString("internetAddress")
        if (ia.isNotEmpty()) internetAddress = ia
        val ra = tag.getString("redstoneAddress")
        if (ra.isNotEmpty()) redstoneAddress = ra
        val ea = tag.getString("eepromAddress")
        if (ea.isNotEmpty()) eepromAddress = ea

        // Restore EEPROM state
        if (tag.contains("eepromCode")) eepromCode = tag.getString("eepromCode")
        if (tag.contains("eepromLabel")) eepromLabel = tag.getString("eepromLabel")
        if (tag.contains("eepromDataBytes")) eepromDataBytes = tag.getByteArray("eepromDataBytes")
        // Backwards compatibility: load old string format if present
        else if (tag.contains("eepromData")) eepromDataBytes = tag.getString("eepromData").toByteArray(Charsets.ISO_8859_1)

        // Restore GPU/screen state
        if (tag.contains("gpuDepth")) gpuDepth = tag.getInt("gpuDepth")
        if (tag.contains("screenIsOn")) screenIsOn = tag.getBoolean("screenIsOn")
        val bsa = tag.getString("boundScreenAddr")
        if (bsa.isNotEmpty()) boundScreenAddr = bsa

        // Restore redstone output
        if (tag.contains("redstoneOutput")) {
            val saved = tag.getIntArray("redstoneOutput")
            if (saved.size == 6) saved.copyInto(redstoneOutput)
        }

        // Restore hard drive filesystem
        if (tag.contains("hardDriveData")) {
            filesystems[hardDriveAddress] = VirtualFileSystem.loadFromTag(tag.getCompound("hardDriveData"))
        }

        // Restore tmpfs
        if (tag.contains("tmpFsData")) {
            filesystems[tmpFsAddress] = VirtualFileSystem.loadFromTag(tag.getCompound("tmpFsData"))
        }
    }

    // ===========================
    // BIOS CODE
    // ===========================
    companion object {
        /**
         * Documentation strings for component methods.
         * Used by component.doc(address, method) to return method documentation.
         * Format matches original OpenComputers: "function(args):returnType -- Description"
         */
        val COMPONENT_DOCS = mapOf(
            // GPU methods
            "bind" to "function(address:string[, reset:boolean]):boolean -- Binds a screen. Reset will also reset the palette.",
            "getScreen" to "function():string -- Returns the address of the screen it is currently bound to.",
            "getBackground" to "function():number, boolean -- Returns the current background color and if it's from the palette.",
            "setBackground" to "function(color:number[, isPaletteIndex:boolean]):number, boolean -- Sets the background color.",
            "getForeground" to "function():number, boolean -- Returns the current foreground color and if it's from the palette.",
            "setForeground" to "function(color:number[, isPaletteIndex:boolean]):number, boolean -- Sets the foreground color.",
            "getPaletteColor" to "function(index:number):number -- Gets the palette color at the specified index.",
            "setPaletteColor" to "function(index:number, value:number):number -- Sets the palette color at the specified index.",
            "maxDepth" to "function():number -- Returns the maximum supported color depth.",
            "getDepth" to "function():number -- Returns the current color depth.",
            "setDepth" to "function(depth:number):number -- Sets the color depth. Returns the old depth.",
            "maxResolution" to "function():number, number -- Returns the maximum resolution.",
            "getResolution" to "function():number, number -- Returns the current resolution.",
            "setResolution" to "function(width:number, height:number):boolean -- Sets the resolution. Returns true if changed.",
            "getViewport" to "function():number, number -- Returns the current viewport resolution.",
            "setViewport" to "function(width:number, height:number):boolean -- Sets the viewport resolution.",
            "get" to "function(x:number, y:number):string, number, number, number or nil, number or nil -- Gets the character and colors at a position.",
            "set" to "function(x:number, y:number, value:string[, vertical:boolean]):boolean -- Sets text at a position.",
            "copy" to "function(x:number, y:number, width:number, height:number, tx:number, ty:number):boolean -- Copies a region.",
            "fill" to "function(x:number, y:number, width:number, height:number, char:string):boolean -- Fills a region with a character.",
            
            // GPU VRAM buffer methods (OC 1.7+)
            "getActiveBuffer" to "function():number -- Returns the index of the currently selected buffer. 0 is the screen.",
            "setActiveBuffer" to "function(index:number):number -- Sets the active buffer. 0 is the screen, 1+ are VRAM buffers.",
            "buffers" to "function():table -- Returns an array of indexes of allocated buffers.",
            "allocateBuffer" to "function([width:number, height:number]):number -- Allocates a new VRAM buffer. Returns index or nil if out of memory.",
            "freeBuffer" to "function([index:number]):boolean -- Frees the buffer at index. Returns true if freed.",
            "freeAllBuffers" to "function():number -- Frees all buffers and returns the count.",
            "getBufferSize" to "function([index:number]):number, number -- Returns buffer size (width, height). Index 0 is the screen.",
            "totalMemory" to "function():number -- Returns the total VRAM size in bytes.",
            "bitblt" to "function([dst:number, col:number, row:number, width:number, height:number, src:number, fromCol:number, fromRow:number]):boolean -- Copies pixels between buffers.",
            
            // Filesystem methods
            "open" to "function(path:string[, mode:string]):number or nil, string -- Opens a file. Returns handle or nil and error.",
            "read" to "function(handle:number, count:number):string or nil -- Reads from an open file.",
            "write" to "function(handle:number, value:string):boolean -- Writes to an open file.",
            "close" to "function(handle:number) -- Closes an open file handle.",
            "seek" to "function(handle:number, whence:string, offset:number):number -- Seeks in an open file.",
            "exists" to "function(path:string):boolean -- Returns whether a path exists.",
            "isDirectory" to "function(path:string):boolean -- Returns whether a path is a directory.",
            "isReadOnly" to "function():boolean -- Returns whether the filesystem is read-only.",
            "list" to "function(path:string):table or nil, string -- Returns a table of entries in a directory.",
            "size" to "function(path:string):number -- Returns the size of a file in bytes.",
            "lastModified" to "function(path:string):number -- Returns the last modified time of a file.",
            "makeDirectory" to "function(path:string):boolean -- Creates a directory.",
            "remove" to "function(path:string):boolean -- Removes a file or empty directory.",
            "rename" to "function(from:string, to:string):boolean -- Renames a file or directory.",
            "spaceTotal" to "function():number -- Returns the total space of the filesystem.",
            "spaceUsed" to "function():number -- Returns the used space of the filesystem.",
            "getLabel" to "function():string -- Returns the filesystem label.",
            "setLabel" to "function(label:string):string -- Sets the filesystem label.",
            "isLink" to "function(path:string):boolean -- Returns false (symlinks not supported).",
            "link" to "function(target:string, linkpath:string):boolean, string -- Creates a symbolic link (not supported).",
            
            // Computer methods
            "start" to "function():boolean -- Starts the computer.",
            "stop" to "function():boolean -- Stops the computer.",
            "isRunning" to "function():boolean -- Returns whether the computer is running.",
            "beep" to "function([frequency:number[, duration:number]]) -- Plays a beep.",
            
            // Screen methods
            "isOn" to "function():boolean -- Returns whether the screen is on.",
            "turnOn" to "function():boolean -- Turns the screen on.",
            "turnOff" to "function():boolean -- Turns the screen off.",
            "getAspectRatio" to "function():number, number -- Returns the screen's aspect ratio.",
            "getKeyboards" to "function():table -- Returns the addresses of attached keyboards.",
            "setPrecise" to "function(enabled:boolean):boolean -- Enables or disables precise click mode.",
            "isPrecise" to "function():boolean -- Returns whether precise mode is enabled.",
            "setTouchModeInverted" to "function(enabled:boolean):boolean -- Inverts touch mode.",
            "isTouchModeInverted" to "function():boolean -- Returns whether touch mode is inverted.",
            
            // EEPROM methods
            "getData" to "function():string -- Returns the EEPROM data section.",
            "setData" to "function(data:string) -- Sets the EEPROM data section.",
            "getSize" to "function():number -- Returns the EEPROM code size.",
            "getDataSize" to "function():number -- Returns the EEPROM data size.",
            "getChecksum" to "function():string -- Returns the EEPROM checksum.",
            "makeReadonly" to "function():boolean -- Makes the EEPROM read-only.",
            
            // Internet methods
            "isHttpEnabled" to "function():boolean -- Returns whether HTTP is enabled.",
            "isTcpEnabled" to "function():boolean -- Returns whether TCP is enabled.",
            "request" to "function(url:string[, postData:string[, headers:table]]):table -- Performs an HTTP request.",
            "connect" to "function(address:string, port:number):table -- Opens a TCP connection.",
            
            // Redstone methods
            "getInput" to "function([side:number]):number or table -- Returns the redstone input.",
            "getOutput" to "function([side:number]):number or table -- Returns the redstone output.",
            "setOutput" to "function(side:number, value:number):number -- Sets the redstone output.",
            "getBundledInput" to "function([side:number[, color:number]]):number or table -- Returns bundled redstone input.",
            "getBundledOutput" to "function([side:number[, color:number]]):number or table -- Returns bundled redstone output.",
            "setBundledOutput" to "function(side:number, color:number, value:number):number -- Sets bundled redstone output.",
            "getComparatorInput" to "function(side:number):number -- Returns comparator input from a side.",
            "getWakeThreshold" to "function():number -- Returns the wake threshold.",
            "setWakeThreshold" to "function(threshold:number):number -- Sets the wake threshold. Returns old value.",
            "getWirelessInput" to "function():boolean -- Returns wireless redstone input.",
            "getWirelessOutput" to "function():boolean -- Returns wireless redstone output.",
            "setWirelessOutput" to "function(value:boolean):boolean -- Sets wireless redstone output. Returns old value.",
            "getWirelessFrequency" to "function():number -- Returns wireless redstone frequency.",
            "setWirelessFrequency" to "function(frequency:number):number -- Sets wireless frequency. Returns old value.",
            
            // Modem methods
            "isWireless" to "function():boolean -- Returns whether this is a wireless modem.",
            "isWired" to "function():boolean -- Returns whether this is a wired modem.",
            "maxPacketSize" to "function():number -- Returns the maximum packet size.",
            "send" to "function(address:string, port:number, ...):boolean -- Sends a packet to an address.",
            "broadcast" to "function(port:number, ...):boolean -- Broadcasts a packet on a port.",
            
            // Robot methods
            "name" to "function():string -- Returns the robot's name.",
            "detect" to "function():boolean, string -- Detects a block in front.",
            "detectUp" to "function():boolean, string -- Detects a block above.",
            "detectDown" to "function():boolean, string -- Detects a block below.",
            "select" to "function([slot:number]):number -- Selects or returns the selected slot.",
            "inventorySize" to "function():number -- Returns the robot's inventory size.",
            "count" to "function([slot:number]):number -- Returns the item count in a slot.",
            "space" to "function([slot:number]):number -- Returns the remaining space in a slot.",
            "move" to "function(side:number):boolean -- Moves in a direction.",
            "turn" to "function(clockwise:boolean):boolean -- Turns left or right.",
            "swing" to "function([side:number[, sneaky:boolean]]):boolean, string -- Swings the equipped tool.",
            "use" to "function([side:number[, sneaky:boolean[, duration:number]]]):boolean, string -- Uses the equipped item.",
            "place" to "function([side:number[, sneaky:boolean]]):boolean, string -- Places a block.",
            "drop" to "function([count:number]):boolean -- Drops items from the selected slot.",
            "suck" to "function([count:number]):boolean -- Picks up items.",
            "durability" to "function():number, number, string -- Returns tool durability.",
            
            // Data card methods
            "crc32" to "function(data:string):number -- Computes CRC-32 hash.",
            "md5" to "function(data:string):string -- Computes MD5 hash.",
            "sha256" to "function(data:string):string -- Computes SHA-256 hash.",
            "encode64" to "function(data:string):string -- Encodes data as base64.",
            "decode64" to "function(data:string):string -- Decodes base64 data.",
            "deflate" to "function(data:string):string -- Compresses data.",
            "inflate" to "function(data:string):string -- Decompresses data.",
            "random" to "function(count:number):string -- Generates random bytes.",
            
            // Hologram methods
            "clear" to "function() -- Clears the hologram.",
            "maxDepth" to "function():number -- Returns the maximum color depth.",
            "getScale" to "function():number -- Returns the hologram scale.",
            "setScale" to "function(scale:number) -- Sets the hologram scale.",
            "getTranslation" to "function():number, number, number -- Returns the translation offset.",
            "setTranslation" to "function(x:number, y:number, z:number) -- Sets the translation offset.",
        )

        val BIOS_CODE = """
-- =============================================
-- OpenCoudou BIOS (OC-compatible)
-- Boots from filesystem like original OpenComputers
-- =============================================

-- Bind GPU to screen
local gpu = component.list("gpu")()
local screen = component.list("screen")()
if gpu and screen then
  component.invoke(gpu, "bind", screen)
end

-- Set up gpu proxy in global for basic output before OS loads
if gpu then
  local w, h = component.invoke(gpu, "getResolution")
  component.invoke(gpu, "setBackground", 0x000000)
  component.invoke(gpu, "setForeground", 0xFFFFFF)
  component.invoke(gpu, "fill", 1, 1, w, h, " ")
end

local y = 1
local function status(msg)
  if gpu then
    component.invoke(gpu, "set", 1, y, msg)
    y = y + 1
  end
end

-- Try loading init.lua from a filesystem
local function tryLoadFrom(address)
  local ok, handle = pcall(component.invoke, address, "open", "/init.lua")
  if not ok or not handle then return nil end
  local buffer = ""
  repeat
    local data, err = component.invoke(address, "read", handle, math.huge)
    buffer = buffer .. (data or "")
  until not data
  component.invoke(address, "close", handle)
  return load(buffer, "=init", "t", _G)
end

status("OpenCoudou BIOS")
status("Booting...")

-- Try boot address first (set by EEPROM data)
local bootAddr = computer.getBootAddress()
local init

if bootAddr and #bootAddr > 0 then
  status("Trying boot address: " .. bootAddr:sub(1, 8) .. "...")
  init = tryLoadFrom(bootAddr)
end

-- If no init found, try all filesystems
if not init then
  status("Scanning filesystems...")
  for addr, ctype in component.list("filesystem") do
    status("  Checking " .. addr:sub(1, 8) .. "...")
    init = tryLoadFrom(addr)
    if init then
      computer.setBootAddress(addr)
      status("  Found init.lua!")
      break
    end
  end
end

if init then
  -- Boot from init.lua
  local ok, err = xpcall(init, function(e)
    return tostring(e) .. "\n" .. debug.traceback()
  end)
  if not ok then
    status("")
    status("Boot error:")
    -- Split error message into lines
    for line in tostring(err):gmatch("[^\n]+") do
      status("  " .. line)
    end
  end
else
  status("")
  status("No bootable medium found.")
  status("Insert a disk with init.lua or install OpenOS.")
  status("")
  
  -- List available components
  status("Components:")
  for addr, ctype in component.list() do
    status("  " .. ctype .. " [" .. addr:sub(1, 8) .. "]")
  end
end

-- Halt
status("")
status("Press any key to reboot...")
while true do
  local sig = computer.pullSignal(1)
  if sig == "key_down" then
    computer.shutdown(true)
  end
end
""".trimIndent()
    }
}
