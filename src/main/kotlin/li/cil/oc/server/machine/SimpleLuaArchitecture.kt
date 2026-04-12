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

        g.set("dofile", LuaValue.NIL)
        g.set("loadfile", LuaValue.NIL)
        g.set("print", createPrintFunction())

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

        // component.list([filter], [exact]) → iterator
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
                val iter = comps.entries.iterator()
                return object : VarArgFunction() {
                    override fun invoke(args: Varargs): Varargs {
                        return if (iter.hasNext()) {
                            val (addr, type) = iter.next()
                            LuaValue.varargsOf(arrayOf(LuaValue.valueOf(addr), LuaValue.valueOf(type)))
                        } else LuaValue.NONE
                    }
                }
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
                                for (i in 1..args.narg()) invokeArgs.add(args.arg(i))
                                return compInvoke.invoke(LuaValue.varargsOf(invokeArgs.toTypedArray()))
                            }
                        }
                    }
                })
                proxy.setmetatable(mt)
                return proxy
            }
        })

        // component.methods(address)
        comp.set("methods", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaTable() // stub
            }
        })

        // component.doc(address, method)
        comp.set("doc", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue = LuaValue.NIL
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
            override fun call(): LuaValue = LuaValue.valueOf(machine.totalMemory.toDouble())
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
                return LuaValue.NONE
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
            override fun call(): LuaValue = LuaTable()
        })

        comp.set("addUser", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = LuaValue.TRUE
        })

        comp.set("removeUser", object : OneArgFunction() {
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
                val time = if (args.narg() > 1) args.arg(2).checklong() * 1000L else System.currentTimeMillis()
                return LuaValue.valueOf(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(time)))
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

    private fun setupGpuAPI() {
        val g = globals ?: return
        val gpu = LuaTable()

        gpu.set("bind", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                boundScreenAddr = args.arg1().tojstring()
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
                return LuaValue.varargsOf(arrayOf(LuaValue.valueOf(160), LuaValue.valueOf(50)))
            }
        })

        gpu.set("setResolution", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val w = args.arg1().checkint()
                val h = args.arg(2).checkint()
                val screen = findNearbyScreen()
                screen?.buffer?.resize(w, h)
                return LuaValue.TRUE
            }
        })

        gpu.set("getDepth", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(8)
        })

        gpu.set("maxDepth", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(8)
        })

        gpu.set("setDepth", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = LuaValue.TRUE
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
                return LuaValue.TRUE
            }
        })

        gpu.set("get", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val x = args.arg1().checkint()
                val y = args.arg(2).checkint()
                val screen = findNearbyScreen() ?: return LuaValue.valueOf(" ")
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
                    LuaValue.valueOf(bg)
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
                return LuaValue.TRUE
            }
        })

        gpu.set("setForeground", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val color = args.arg1().checkint()
                val screen = findNearbyScreen()
                val old = screen?.buffer?.foreground ?: 0xFFFFFF
                if (screen != null) screen.buffer.foreground = color
                return LuaValue.valueOf(old)
            }
        })

        gpu.set("setBackground", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val color = args.arg1().checkint()
                val screen = findNearbyScreen()
                val old = screen?.buffer?.background ?: 0x000000
                if (screen != null) screen.buffer.background = color
                return LuaValue.valueOf(old)
            }
        })

        gpu.set("getForeground", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val screen = findNearbyScreen()
                return LuaValue.valueOf(screen?.buffer?.foreground ?: 0xFFFFFF)
            }
        })

        gpu.set("getBackground", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val screen = findNearbyScreen()
                return LuaValue.valueOf(screen?.buffer?.background ?: 0x000000)
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

    private fun handleScreenInvoke(method: String, args: List<Any?>): Varargs {
        val screen = findNearbyScreen() ?: return LuaValue.NIL
        return when (method) {
            "getAspectRatio" -> LuaValue.varargsOf(arrayOf(LuaValue.valueOf(1), LuaValue.valueOf(1)))
            "getKeyboards" -> {
                val t = LuaTable()
                // Return keyboard address if screen has one
                screen.keyboardAddress?.let { t.set(1, LuaValue.valueOf(it)) }
                t
            }
            "turnOn" -> LuaValue.TRUE
            "turnOff" -> LuaValue.TRUE
            "isOn" -> LuaValue.TRUE
            "setPrecise" -> LuaValue.TRUE
            "isPrecise" -> LuaValue.FALSE
            "setTouchModeInverted" -> LuaValue.TRUE
            "isTouchModeInverted" -> LuaValue.FALSE
            else -> LuaValue.NIL
        }
    }

    private fun handleEepromInvoke(method: String, args: List<Any?>): Varargs {
        return when (method) {
            "get" -> LuaValue.valueOf("") // return empty BIOS code
            "set" -> LuaValue.TRUE
            "getLabel" -> LuaValue.valueOf("EEPROM")
            "setLabel" -> LuaValue.TRUE
            "getData" -> LuaValue.valueOf(bootAddress ?: "")
            "setData" -> {
                bootAddress = args.getOrNull(0)?.toString()
                LuaValue.TRUE
            }
            "getSize" -> LuaValue.valueOf(4096)
            "getDataSize" -> LuaValue.valueOf(256)
            "getChecksum" -> LuaValue.valueOf("00000000")
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
                    val handle = (args.getOrNull(0) as? Number)?.toInt() ?: return LuaValue.NIL
                    val count = (args.getOrNull(1) as? Number)?.toInt() ?: Int.MAX_VALUE
                    val data = vfs.read(handle, count)
                    if (data != null && data.isNotEmpty()) {
                        LuaValue.valueOf(String(data))
                    } else {
                        LuaValue.NIL
                    }
                }
                "write" -> {
                    val handle = (args.getOrNull(0) as? Number)?.toInt() ?: return LuaValue.FALSE
                    val data = args.getOrNull(1)?.toString() ?: return LuaValue.FALSE
                    LuaValue.valueOf(vfs.write(handle, data.toByteArray()))
                }
                "seek" -> {
                    val handle = (args.getOrNull(0) as? Number)?.toInt() ?: return LuaValue.NIL
                    val whence = args.getOrNull(1)?.toString() ?: "cur"
                    val offset = (args.getOrNull(2) as? Number)?.toLong() ?: 0L
                    val pos = vfs.seek(handle, whence, offset)
                    LuaValue.valueOf((pos ?: 0L).toDouble())
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

        // internet.request(url, [postData], [headers]) → handle
        inet.set("request", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val url = args.arg1().checkjstring()
                val postData = if (args.narg() >= 2 && !args.arg(2).isnil()) args.arg(2).tojstring() else null

                // Return a response handle table
                val handle = LuaTable()
                var responseData: String? = null
                var error: String? = null
                var finished = false

                // Perform HTTP request on a background thread
                val thread = Thread({
                    try {
                        val conn = URI(url).toURL().openConnection() as HttpURLConnection
                        conn.connectTimeout = 10000
                        conn.readTimeout = 10000
                        conn.setRequestProperty("User-Agent", "OpenCoudou/3.0")
                        if (postData != null) {
                            conn.requestMethod = "POST"
                            conn.doOutput = true
                            conn.outputStream.use { it.write(postData.toByteArray()) }
                        }
                        val reader = BufferedReader(InputStreamReader(conn.inputStream))
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

                // handle.read([n]) → data or nil
                handle.set("read", object : VarArgFunction() {
                    private var offset = 0
                    override fun invoke(args: Varargs): Varargs {
                        if (!finished) return LuaValue.valueOf("")
                        val data = responseData ?: return LuaValue.NIL
                        if (offset >= data.length) return LuaValue.NIL
                        val n = if (args.narg() > 0 && !args.arg1().isnil()) {
                            val v = args.arg1()
                            if (v.isnumber()) v.checkint() else data.length
                        } else data.length
                        val chunk = data.substring(offset, minOf(offset + n, data.length))
                        offset += chunk.length
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
                            LuaValue.valueOf(200),
                            LuaValue.valueOf("OK"),
                            LuaTable()
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
            override fun call(arg: LuaValue): LuaValue = LuaValue.valueOf(arg.checkjstring().length)
        })
        uni.set("sub", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val s = args.arg1().checkjstring()
                val i = args.arg(2).checkint()
                val j = if (args.narg() >= 3) args.arg(3).checkint() else s.length
                val start = if (i < 0) maxOf(0, s.length + i) else maxOf(0, i - 1)
                val end = if (j < 0) maxOf(0, s.length + j + 1) else minOf(s.length, j)
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
            override fun call(arg: LuaValue): LuaValue = LuaValue.valueOf(arg.checkjstring().length)
        })
        uni.set("wtrunc", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val s = arg1.checkjstring()
                val n = arg2.checkint()
                return LuaValue.valueOf(s.take(n))
            }
        })
        uni.set("isWide", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = LuaValue.FALSE
        })
        uni.set("charWidth", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue = LuaValue.valueOf(1)
        })
        g.set("unicode", uni)
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
    private var tmpFsAddress = java.util.UUID.randomUUID().toString().take(8)
    private var hardDriveAddress = java.util.UUID.randomUUID().toString()
    private var lootDiskAddress = java.util.UUID.randomUUID().toString()
    private val filesystems = mutableMapOf<String, VirtualFileSystem>()

    override fun recomputeMemory(memory: Iterable<ItemStack>): Boolean = true

    override fun close() {
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

                val biosCode = BIOS_CODE
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

            val result = co.resume(resumeArgs)
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

            // Check for shutdown/reboot signals
            val sm = machine as? SimpleMachine
            val signal = sm?.pollSignal()
            if (signal != null) {
                if (signal.name == "__shutdown") return ExecutionResult.Shutdown
                if (signal.name == "__reboot") return ExecutionResult.Shutdown // handle as restart

                // Convert signal to Lua values
                val sigArgs = mutableListOf<LuaValue>(LuaValue.valueOf(signal.name))
                signal.args.forEach { sigArgs.add(convertToLua(it)) }
                pendingSignal = LuaValue.varargsOf(sigArgs.toTypedArray())
            } else {
                // No signal - sleep briefly then resume with nil (timeout)
                val sleepMs = (timeout * 1000).toLong().coerceIn(1, 1000)
                Thread.sleep(minOf(sleepMs, 50))
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
        // Register virtual gpu, screen, internet, eeprom components so Lua can find them
        val sm = machine as? SimpleMachine ?: return
        val screen = findNearbyScreen()
        val gpuAddr = "gpu-" + java.util.UUID.randomUUID().toString().take(4)
        sm.registerComponent(gpuAddr, "gpu")
        if (screen != null) {
            sm.registerComponent(screen.address, "screen")
        }
        val inetAddr = "inet-" + java.util.UUID.randomUUID().toString().take(4)
        sm.registerComponent(inetAddr, "internet")
        val eepromAddr = "eeprom-" + java.util.UUID.randomUUID().toString().take(4)
        sm.registerComponent(eepromAddr, "eeprom")

        // Filesystem components
        // 1. tmpfs - small volatile storage (only create if not restored from save)
        if (!filesystems.containsKey(tmpFsAddress)) {
            filesystems[tmpFsAddress] = VirtualFileSystem(capacity = 64 * 1024, readOnly = false, label = "tmpfs")
        }
        sm.registerComponent(tmpFsAddress, "filesystem")

        // 2. Hard drive - writable persistent storage (only create if not restored from save)
        if (!filesystems.containsKey(hardDriveAddress)) {
            val hdd = VirtualFileSystem(capacity = 2 * 1024 * 1024, readOnly = false, label = "")
            hdd.makeDirectory("home")
            hdd.makeDirectory("tmp")
            filesystems[hardDriveAddress] = hdd
        }
        sm.registerComponent(hardDriveAddress, "filesystem")

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
    }

    override fun runSynchronized() {}
    override fun onSignal() {}

    fun saveData(tag: CompoundTag) {}
    fun loadData(tag: CompoundTag) {}

    override fun save(tag: CompoundTag) {
        // Persist addresses
        tag.putString("bootAddress", bootAddress ?: "")
        tag.putString("tmpFsAddress", tmpFsAddress)
        tag.putString("hardDriveAddress", hardDriveAddress)
        tag.putString("lootDiskAddress", lootDiskAddress)

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
