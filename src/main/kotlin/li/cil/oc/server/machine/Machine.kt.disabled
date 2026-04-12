package li.cil.oc.server.machine

import li.cil.oc.server.component.*
import li.cil.oc.server.network.NetworkRegistry
import li.cil.oc.util.OCLogger
import org.luaj.vm2.*
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.*
import java.util.concurrent.*
import java.util.UUID

/**
 * The Machine executes Lua code and manages the computer's runtime.
 * This is the heart of the OpenComputers computer system.
 */
class Machine {
    
    enum class State {
        STOPPED,
        STARTING,
        RUNNING,
        PAUSED,
        CRASHED
    }
    
    var state = State.STOPPED
        private set
    
    // Component network
    val network = ComponentNetwork()
    
    // Signal queue (events from components)
    private val signals = ConcurrentLinkedQueue<Signal>()
    
    // Lua globals
    private var globals: Globals? = null
    private var mainCoroutine: LuaThread? = null
    
    // Uptime tracking
    private var startTime = 0L
    val uptime: Double get() = if (startTime > 0) (System.currentTimeMillis() - startTime) / 1000.0 else 0.0
    
    // World time in game ticks (updated by CaseBlockEntity)
    var worldTime: Long = 0
    
    // Computer address
    val address: String = UUID.randomUUID().toString()
    
    // Crash message
    var crashMessage: String? = null
        private set
    
    // EEPROM code to run
    private var biosCode: String? = null
    
    // Executor for running Lua in background
    private val executor = Executors.newSingleThreadExecutor()
    private var runningTask: Future<*>? = null
    
    // Creative mode flag - bypasses energy consumption
    var isCreative = false
    
    // Callback for reboot (set by CaseBlockEntity)
    var onReboot: (() -> Unit)? = null
    
    // Callback for beep (set by CaseBlockEntity) - frequency, duration
    var onBeep: ((Double, Double) -> Unit)? = null
    
    // Energy (simplified)
    var energy = 10000.0
        get() = if (isCreative) maxEnergy else field
        set(value) { if (!isCreative) field = value.coerceIn(0.0, maxEnergy) }
    var maxEnergy = 10000.0
    
    /**
     * Initialize the Lua VM with sandboxed environment.
     */
    private fun initLua(): Boolean {
        try {
            OCLogger.boot("INIT", "Creating Lua VM")
            
            // Create sandboxed globals (no file/os access)
            globals = Globals()
            globals!!.load(JseBaseLib())
            globals!!.load(PackageLib())
            globals!!.load(Bit32Lib())
            globals!!.load(TableLib())
            globals!!.load(StringLib())
            globals!!.load(CoroutineLib())
            globals!!.load(JseMathLib())
            
            // Install Lua compiler - REQUIRED for loading source code
            LoadState.install(globals)
            LuaC.install(globals)
            
            // Install our custom libraries
            installComputerLib()
            installComponentLib()
            installUnicodeLib()
            installSandboxedOsLib()
            installSandboxedDebugLib()
            
            // Add checkArg global (used by OpenOS libs)
            globals!!.load(globals!!.load("""
                function checkArg(n, have, ...)
                  have = type(have)
                  local function check(want, ...)
                    if not want then
                      return false
                    else
                      return have == want or check(...)
                    end
                  end
                  for i = 1, select('#', ...) do
                    if have == select(i, ...) then
                      return
                    end
                  end
                  local msg = string.format("bad argument #%d (%s expected, got %s)", n, table.concat({...}, " or "), have)
                  error(msg, 3)
                end
            """.trimIndent(), "=checkArg").call())
            
            // Sandbox: remove dangerous globals (like original OC)
            globals!!.set("dofile", LuaValue.NIL)
            globals!!.set("loadfile", LuaValue.NIL)
            
            OCLogger.boot("INIT", "Lua VM created successfully")
            return true
        } catch (e: Exception) {
            OCLogger.error("Failed to initialize Lua VM", e)
            crash("Lua initialization failed: ${e.message}")
            return false
        }
    }
    
    /**
     * Install the 'computer' library.
     */
    private fun installComputerLib() {
        val computer = LuaTable()
        
        computer.set("address", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(address)
        })
        
        computer.set("uptime", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(uptime)
        })
        
        computer.set("energy", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(energy)
        })
        
        computer.set("maxEnergy", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(maxEnergy)
        })
        
        computer.set("freeMemory", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(65536) // Simplified
        })
        
        computer.set("totalMemory", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(131072) // Simplified
        })
        
        computer.set("realTime", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf(System.currentTimeMillis() / 1000.0)
        })
        
        computer.set("tmpAddress", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                // Return address of the tmpfs component specifically
                for (comp in network.all()) {
                    if (comp is FilesystemComponent && comp.label == "tmpfs") {
                        return LuaValue.valueOf(comp.address)
                    }
                }
                val tmpfs = network.getFirst("filesystem")
                return if (tmpfs != null) LuaValue.valueOf(tmpfs.address) else LuaValue.NIL
            }
        })
        
        computer.set("getBootAddress", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                // Read boot address from EEPROM data
                val eeprom = network.getFirst("eeprom")
                if (eeprom != null) {
                    val getData = eeprom.methods()["getData"]
                    if (getData != null) {
                        val result = getData.invoke(emptyArray())
                        val data = result.getOrNull(0)?.toString() ?: ""
                        val trimmed = data.trimEnd('\u0000')
                        if (trimmed.isNotEmpty()) return LuaValue.valueOf(trimmed)
                    }
                }
                val fs = network.getFirst("filesystem")
                return if (fs != null) LuaValue.valueOf(fs.address) else LuaValue.valueOf("")
            }
        })
        
        computer.set("setBootAddress", object : OneArgFunction() {
            override fun call(addr: LuaValue): LuaValue {
                // Save boot address to EEPROM data
                val eeprom = network.getFirst("eeprom")
                if (eeprom != null) {
                    val setData = eeprom.methods()["setData"]
                    if (setData != null) {
                        setData.invoke(arrayOf(addr.optjstring("")))
                    }
                }
                return LuaValue.NIL
            }
        })
        
        computer.set("users", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaTable()
        })
        
        computer.set("addUser", object : OneArgFunction() {
            override fun call(name: LuaValue): LuaValue = LuaValue.TRUE
        })
        
        computer.set("removeUser", object : OneArgFunction() {
            override fun call(name: LuaValue): LuaValue = LuaValue.TRUE
        })
        
        computer.set("isRobot", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.FALSE
        })
        
        computer.set("getArchitectures", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val t = LuaTable()
                t.set(1, LuaValue.valueOf("Lua 5.2"))
                return t
            }
        })
        
        computer.set("getArchitecture", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.valueOf("Lua 5.2")
        })
        
        computer.set("setArchitecture", object : OneArgFunction() {
            override fun call(arch: LuaValue): LuaValue = LuaValue.TRUE
        })
        
        computer.set("error", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return if (crashMessage != null) LuaValue.valueOf(crashMessage) else LuaValue.NIL
            }
        })
        
        computer.set("getProgramLocations", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaTable()
        })
        
        computer.set("getDeviceInfo", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                // Returns a table of address -> {class, description, product, vendor}
                val result = LuaTable()
                for (comp in network.all()) {
                    val info = LuaTable()
                    info.set("class", comp.componentType)
                    info.set("description", comp.componentType)
                    info.set("product", "OpenComputers ${comp.componentType}")
                    info.set("vendor", "MightyPirates")
                    result.set(comp.address, info)
                }
                return result
            }
        })
        
        computer.set("shutdown", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val reboot = args.optboolean(1, false)
                if (reboot) {
                    // Signal the machine to reboot
                    onReboot?.invoke()
                }
                // Set state to stopped and throw to halt execution
                state = State.STOPPED
                throw LuaError("machine shutdown")
            }
        })
        
        computer.set("pushSignal", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val name = args.checkjstring(1)
                val signalArgs = mutableListOf<Any?>()
                for (i in 2..args.narg()) {
                    signalArgs.add(luaToJava(args.arg(i)))
                }
                pushSignal(name, *signalArgs.toTypedArray())
                return LuaValue.TRUE
            }
        })
        
        computer.set("pullSignal", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val timeout = args.optdouble(1, Double.MAX_VALUE)
                val signal = pullSignal(timeout)
                if (signal != null) {
                    val result = mutableListOf<LuaValue>()
                    result.add(LuaValue.valueOf(signal.name))
                    signal.args.forEach { result.add(javaToLua(it)) }
                    return LuaValue.varargsOf(result.toTypedArray())
                }
                return LuaValue.NIL
            }
        })
        
        computer.set("beep", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val freq = args.optdouble(1, 440.0).coerceIn(20.0, 2000.0)
                val duration = args.optdouble(2, 0.1).coerceIn(0.05, 5.0)
                OCLogger.computer("BEEP", "freq=$freq, duration=$duration")
                onBeep?.invoke(freq, duration)
                return LuaValue.NIL
            }
        })
        
        computer.set("log", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val msg = args.optjstring(1, "")
                OCLogger.computer("LUA_LOG", msg ?: "")
                return LuaValue.NIL
            }
        })
        
        globals!!.set("computer", computer)
    }
    
    /**
     * Install the 'component' library.
     */
    private fun installComponentLib() {
        val component = LuaTable()
        
        component.set("list", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val filter = args.optjstring(1, null)
                val exact = args.optboolean(2, false)
                
                val result = LuaTable()
                for (comp in network.all()) {
                    if (filter == null || 
                        (exact && comp.componentType == filter) ||
                        (!exact && comp.componentType.contains(filter))) {
                        result.set(comp.address, comp.componentType)
                    }
                }
                
                // Make it work as both pairs(t) and for k in t do
                // by setting a __call metamethod so t() returns next key,value
                val meta = LuaTable()
                val keys = mutableListOf<LuaValue>()
                var k: LuaValue = LuaValue.NIL
                while (true) {
                    val n = result.next(k)
                    if (n.arg1().isnil()) break
                    keys.add(n.arg1())
                    k = n.arg1()
                }
                var keyIdx = 0
                meta.set("__call", object : VarArgFunction() {
                    override fun invoke(args: Varargs): Varargs {
                        if (keyIdx < keys.size) {
                            val key = keys[keyIdx++]
                            return LuaValue.varargsOf(key, result.get(key))
                        }
                        return LuaValue.NIL
                    }
                })
                // Support pairs() - returns iterator function, table, nil
                meta.set("__pairs", object : VarArgFunction() {
                    override fun invoke(args: Varargs): Varargs {
                        var pairIdx = 0
                        val iterFunc = object : VarArgFunction() {
                            override fun invoke(args: Varargs): Varargs {
                                if (pairIdx < keys.size) {
                                    val key = keys[pairIdx++]
                                    return LuaValue.varargsOf(key, result.get(key))
                                }
                                return LuaValue.NIL
                            }
                        }
                        return LuaValue.varargsOf(iterFunc, result, LuaValue.NIL)
                    }
                })
                result.setmetatable(meta)
                
                return result
            }
        })
        
        component.set("type", object : OneArgFunction() {
            override fun call(addr: LuaValue): LuaValue {
                val comp = network.get(addr.checkjstring()) 
                    ?: return LuaValue.NIL
                return LuaValue.valueOf(comp.componentType)
            }
        })
        
        // component.get(partialAddress) - returns full address matching partial
        component.set("get", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val partial = args.checkjstring(1)
                val componentType = args.optjstring(2, null)
                
                var found: li.cil.oc.server.component.Component? = null
                var ambiguous = false
                
                for (comp in network.all()) {
                    if (comp.address.startsWith(partial)) {
                        if (componentType != null && comp.componentType != componentType) continue
                        if (found != null) {
                            ambiguous = true
                            break
                        }
                        found = comp
                    }
                }
                
                if (ambiguous) {
                    return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("ambiguous address"))
                }
                if (found != null) {
                    return LuaValue.varargsOf(LuaValue.valueOf(found.address), LuaValue.valueOf(found.componentType))
                }
                return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such component"))
            }
        })
        
        component.set("methods", object : OneArgFunction() {
            override fun call(addr: LuaValue): LuaValue {
                val comp = network.get(addr.checkjstring())
                    ?: return LuaValue.NIL
                val result = LuaTable()
                for ((name, method) in comp.methods()) {
                    val info = LuaTable()
                    info.set("direct", LuaValue.valueOf(method.direct))
                    info.set("doc", LuaValue.valueOf(method.doc))
                    result.set(name, info)
                }
                return result
            }
        })
        
        component.set("invoke", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val addr = args.checkjstring(1)
                val method = args.checkjstring(2)
                
                val comp = network.get(addr)
                    ?: return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such component"))
                
                val m = comp.methods()[method]
                    ?: return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such method"))
                
                // Convert args
                val methodArgs = mutableListOf<Any?>()
                for (i in 3..args.narg()) {
                    methodArgs.add(luaToJava(args.arg(i)))
                }
                
                try {
                    val result = m.invoke(methodArgs.toTypedArray())
                    if (result.isEmpty()) return LuaValue.NONE
                    val luaResult = result.map { javaToLua(it) }.toTypedArray()
                    return LuaValue.varargsOf(luaResult)
                } catch (e: Exception) {
                    return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(e.message ?: "error"))
                }
            }
        })
        
        component.set("proxy", object : OneArgFunction() {
            override fun call(addr: LuaValue): LuaValue {
                val address = addr.checkjstring()
                val comp = network.get(address)
                    ?: return LuaValue.NIL
                
                // Create proxy table with all methods
                val proxy = LuaTable()
                proxy.set("address", address)
                proxy.set("type", comp.componentType)
                
                for ((name, method) in comp.methods()) {
                    proxy.set(name, object : VarArgFunction() {
                        override fun invoke(args: Varargs): Varargs {
                            val methodArgs = mutableListOf<Any?>()
                            for (i in 1..args.narg()) {
                                methodArgs.add(luaToJava(args.arg(i)))
                            }
                            try {
                                val result = method.invoke(methodArgs.toTypedArray())
                                if (result.isEmpty()) return LuaValue.NONE
                                val luaResult = result.map { javaToLua(it) }.toTypedArray()
                                return LuaValue.varargsOf(luaResult)
                            } catch (e: Exception) {
                                return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(e.message ?: "error"))
                            }
                        }
                    })
                }
                
                return proxy
            }
        })
        
        component.set("doc", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val addr = args.checkjstring(1)
                val method = args.checkjstring(2)
                val comp = network.get(addr)
                    ?: return LuaValue.NIL
                val m = comp.methods()[method]
                    ?: return LuaValue.NIL
                return LuaValue.valueOf(m.doc)
            }
        })
        
        component.set("slot", object : OneArgFunction() {
            override fun call(addr: LuaValue): LuaValue = LuaValue.valueOf(-1)
        })
        
        component.set("fields", object : OneArgFunction() {
            override fun call(addr: LuaValue): LuaValue = LuaTable()
        })
        
        globals!!.set("component", component)
    }
    
    /**
     * Install the 'unicode' library.
     * Matches original OC: char, len, lower, upper, reverse, sub, isWide, charWidth, wlen, wtrunc
     */
    private fun installUnicodeLib() {
        val unicode = LuaTable()
        
        unicode.set("len", object : OneArgFunction() {
            override fun call(s: LuaValue): LuaValue {
                return LuaValue.valueOf(s.checkjstring().length)
            }
        })
        
        unicode.set("sub", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val s = args.checkjstring(1)
                val i = args.checkint(2)
                val j = args.optint(3, s.length)
                val start = if (i < 0) maxOf(0, s.length + i) else minOf(s.length, i - 1)
                val end = if (j < 0) maxOf(0, s.length + j + 1) else minOf(s.length, j)
                return LuaValue.valueOf(if (start < end) s.substring(start, end) else "")
            }
        })
        
        unicode.set("char", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val sb = StringBuilder()
                for (i in 1..args.narg()) {
                    sb.appendCodePoint(args.checkint(i))
                }
                return LuaValue.valueOf(sb.toString())
            }
        })
        
        unicode.set("upper", object : OneArgFunction() {
            override fun call(s: LuaValue): LuaValue {
                return LuaValue.valueOf(s.checkjstring().uppercase())
            }
        })
        
        unicode.set("lower", object : OneArgFunction() {
            override fun call(s: LuaValue): LuaValue {
                return LuaValue.valueOf(s.checkjstring().lowercase())
            }
        })
        
        unicode.set("reverse", object : OneArgFunction() {
            override fun call(s: LuaValue): LuaValue {
                return LuaValue.valueOf(s.checkjstring().reversed())
            }
        })
        
        unicode.set("isWide", object : OneArgFunction() {
            override fun call(s: LuaValue): LuaValue {
                val str = s.checkjstring()
                if (str.isEmpty()) return LuaValue.FALSE
                val cp = str.codePointAt(0)
                return LuaValue.valueOf(isWideChar(cp))
            }
        })
        
        unicode.set("charWidth", object : OneArgFunction() {
            override fun call(s: LuaValue): LuaValue {
                val str = s.checkjstring()
                if (str.isEmpty()) return LuaValue.valueOf(0)
                val cp = str.codePointAt(0)
                return LuaValue.valueOf(if (isWideChar(cp)) 2 else 1)
            }
        })
        
        unicode.set("wlen", object : OneArgFunction() {
            override fun call(s: LuaValue): LuaValue {
                val str = s.checkjstring()
                var width = 0
                var i = 0
                while (i < str.length) {
                    val cp = str.codePointAt(i)
                    width += if (isWideChar(cp)) 2 else 1
                    i += Character.charCount(cp)
                }
                return LuaValue.valueOf(width)
            }
        })
        
        unicode.set("wtrunc", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val str = args.checkjstring(1)
                val count = args.checkint(2)
                val sb = StringBuilder()
                var width = 0
                var i = 0
                while (i < str.length) {
                    val cp = str.codePointAt(i)
                    val cw = if (isWideChar(cp)) 2 else 1
                    if (width + cw > count) break
                    sb.appendCodePoint(cp)
                    width += cw
                    i += Character.charCount(cp)
                }
                return LuaValue.valueOf(sb.toString())
            }
        })
        
        globals!!.set("unicode", unicode)
    }
    
    /**
     * Returns true if the Unicode codepoint is a wide (fullwidth) character.
     * Uses East Asian Width property approximation.
     */
    private fun isWideChar(cp: Int): Boolean {
        // CJK Unified Ideographs, CJK Compatibility, Fullwidth Forms, etc.
        return (cp in 0x1100..0x115F) ||   // Hangul Jamo
               (cp in 0x2E80..0x303E) ||   // CJK Radicals, Kangxi, CJK Symbols
               (cp in 0x3041..0x33BF) ||   // Hiragana, Katakana, Bopomofo, CJK Compatibility
               (cp in 0x3400..0x4DBF) ||   // CJK Unified Ext A
               (cp in 0x4E00..0x9FFF) ||   // CJK Unified Ideographs
               (cp in 0xA000..0xA4CF) ||   // Yi
               (cp in 0xAC00..0xD7AF) ||   // Hangul Syllables
               (cp in 0xF900..0xFAFF) ||   // CJK Compatibility Ideographs
               (cp in 0xFE30..0xFE6F) ||   // CJK Compatibility Forms
               (cp in 0xFF01..0xFF60) ||   // Fullwidth Forms
               (cp in 0xFFE0..0xFFE6) ||   // Fullwidth Signs
               (cp in 0x20000..0x2FA1F)    // CJK Unified Ext B-F, Supplement
    }
    
    /**
     * Install sandboxed 'os' library. Original OC only exposes clock/time/date.
     */
    private fun installSandboxedOsLib() {
        val os = LuaTable()
        
        os.set("clock", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(uptime)
            }
        })
        
        os.set("time", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() == 0 || args.isnil(1)) {
                    // Return in-game world time (Minecraft time). 
                    // Game time is in ticks, 24000 per day. Minecraft starts at 6 AM (tick 0).
                    // Convert: timestamp = (time + 6000) * 60 * 60 / 1000
                    return LuaValue.valueOf(((worldTime + 6000) * 60.0 * 60.0) / 1000.0)
                } else if (args.istable(1)) {
                    // Convert table {year,month,day,hour,min,sec} to timestamp
                    val tbl = args.checktable(1)
                    val year = tbl.get("year").optint(-1)
                    val month = tbl.get("month").optint(-1)
                    val day = tbl.get("day").optint(-1)
                    val hour = tbl.get("hour").optint(12)
                    val min = tbl.get("min").optint(0)
                    val sec = tbl.get("sec").optint(0)
                    
                    if (year < 0 || month < 0 || day < 0) {
                        return LuaValue.NIL
                    }
                    
                    try {
                        val dt = java.time.LocalDateTime.of(year, month, day, hour, min, sec)
                        val epoch = dt.toEpochSecond(java.time.ZoneOffset.UTC)
                        return LuaValue.valueOf(epoch.toDouble())
                    } catch (e: Exception) {
                        return LuaValue.NIL
                    }
                }
                return LuaValue.valueOf(System.currentTimeMillis() / 1000.0)
            }
        })
        
        os.set("date", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                var format = args.optjstring(1, "%c")
                val time = args.optnumber(2, null)
                
                // Check for "*t" table format
                if (format == "*t" || format == "!*t") {
                    val dt = if (time != null) {
                        java.time.Instant.ofEpochSecond(time.tolong())
                            .atZone(java.time.ZoneOffset.UTC).toLocalDateTime()
                    } else {
                        java.time.LocalDateTime.now()
                    }
                    val tbl = LuaTable()
                    tbl.set("year", dt.year)
                    tbl.set("month", dt.monthValue)
                    tbl.set("day", dt.dayOfMonth)
                    tbl.set("hour", dt.hour)
                    tbl.set("min", dt.minute)
                    tbl.set("sec", dt.second)
                    tbl.set("wday", dt.dayOfWeek.value % 7 + 1) // Lua weekday: Sunday = 1
                    tbl.set("yday", dt.dayOfYear)
                    tbl.set("isdst", LuaValue.FALSE)
                    return tbl
                }
                
                // Handle "!" prefix (UTC/real time) - just strip it for Minecraft
                val useRealTime = format.startsWith("!")
                if (useRealTime) {
                    format = format.substring(1)
                }
                
                // Get DateTime based on time argument or current
                val dt = if (time != null) {
                    java.time.Instant.ofEpochSecond(time.tolong())
                        .atZone(java.time.ZoneOffset.UTC).toLocalDateTime()
                } else if (useRealTime) {
                    java.time.LocalDateTime.now()
                } else {
                    // In-game time - convert from ticks
                    val totalMinutes = ((worldTime + 6000) * 60 / 1000).toInt()
                    val hours = (totalMinutes / 60) % 24
                    val minutes = totalMinutes % 60
                    java.time.LocalDateTime.of(2000, 1, 1, hours, minutes, 0)
                }
                
                // Format string substitutions
                val result = format
                    .replace("%Y", String.format("%04d", dt.year))
                    .replace("%y", String.format("%02d", dt.year % 100))
                    .replace("%m", String.format("%02d", dt.monthValue))
                    .replace("%d", String.format("%02d", dt.dayOfMonth))
                    .replace("%H", String.format("%02d", dt.hour))
                    .replace("%I", String.format("%02d", ((dt.hour + 11) % 12) + 1))
                    .replace("%M", String.format("%02d", dt.minute))
                    .replace("%S", String.format("%02d", dt.second))
                    .replace("%p", if (dt.hour < 12) "AM" else "PM")
                    .replace("%a", dt.dayOfWeek.toString().take(3).lowercase().replaceFirstChar { it.uppercase() })
                    .replace("%A", dt.dayOfWeek.toString().lowercase().replaceFirstChar { it.uppercase() })
                    .replace("%b", dt.month.toString().take(3).lowercase().replaceFirstChar { it.uppercase() })
                    .replace("%B", dt.month.toString().lowercase().replaceFirstChar { it.uppercase() })
                    .replace("%j", String.format("%03d", dt.dayOfYear))
                    .replace("%w", (dt.dayOfWeek.value % 7).toString())
                    .replace("%c", dt.toString())
                    .replace("%x", "${dt.monthValue}/${dt.dayOfMonth}/${dt.year % 100}")
                    .replace("%X", "${String.format("%02d", dt.hour)}:${String.format("%02d", dt.minute)}:${String.format("%02d", dt.second)}")
                    .replace("%%", "%")
                    
                return LuaValue.valueOf(result)
            }
        })
        
        os.set("tmpname", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf("/tmp/" + java.util.UUID.randomUUID().toString().take(8))
            }
        })
        
        globals!!.set("os", os)
    }
    
    /**
     * Install sandboxed 'debug' library. Only exposes traceback and getinfo (limited).
     */
    private fun installSandboxedDebugLib() {
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
                // Return minimal info table
                val info = LuaTable()
                info.set("source", LuaValue.valueOf("=?"))
                info.set("short_src", LuaValue.valueOf("?"))
                info.set("what", LuaValue.valueOf("Lua"))
                return info
            }
        })
        
        globals!!.set("debug", debug)
    }
    
    // Helper to convert Lua values to Java
    private fun luaToJava(v: LuaValue): Any? = when {
        v.isnil() -> null
        v.isboolean() -> v.toboolean()
        v.isint() -> v.toint()
        v.isnumber() -> v.todouble()
        v.isstring() -> v.tojstring()
        v.istable() -> {
            val map = mutableMapOf<Any, Any?>()
            var k = LuaValue.NIL
            while (true) {
                val n = v.next(k)
                if (n.arg1().isnil()) break
                k = n.arg1()
                map[luaToJava(k)!!] = luaToJava(n.arg(2))
            }
            map
        }
        else -> v.tojstring()
    }
    
    // Helper to convert Java values to Lua
    private fun javaToLua(v: Any?): LuaValue = when (v) {
        null -> LuaValue.NIL
        is Boolean -> LuaValue.valueOf(v)
        is Int -> LuaValue.valueOf(v)
        is Long -> LuaValue.valueOf(v.toDouble())
        is Double -> LuaValue.valueOf(v)
        is Float -> LuaValue.valueOf(v.toDouble())
        is String -> LuaValue.valueOf(v)
        is Array<*> -> {
            val t = LuaTable()
            v.forEachIndexed { i, e -> t.set(i + 1, javaToLua(e)) }
            t
        }
        is List<*> -> {
            val t = LuaTable()
            v.forEachIndexed { i, e -> t.set(i + 1, javaToLua(e)) }
            t
        }
        is Map<*, *> -> {
            val t = LuaTable()
            v.forEach { (k, e) -> t.set(javaToLua(k), javaToLua(e)) }
            t
        }
        else -> LuaValue.valueOf(v.toString())
    }
    
    /**
     * Start the machine with given BIOS code.
     */
    fun start(bios: String): Boolean {
        if (state != State.STOPPED) {
            OCLogger.boot("START", "Cannot start - already running")
            return false
        }
        
        OCLogger.boot("START", "Starting machine")
        biosCode = bios
        
        if (!initLua()) {
            return false
        }
        
        state = State.STARTING
        startTime = System.currentTimeMillis()
        
        // Run BIOS in background thread
        runningTask = executor.submit {
            try {
                state = State.RUNNING
                
                // Push component_added signals for all components (like original OC)
                for (comp in network.all()) {
                    pushSignal("component_added", comp.address, comp.componentType)
                }
                
                OCLogger.boot("RUN", "Executing BIOS code (${network.all().size} components)")
                
                val chunk = globals!!.load(biosCode!!, "=bios")
                chunk.call()
                
                OCLogger.computer("FINISHED", "BIOS execution completed normally")
            } catch (e: InterruptedException) {
                // Expected during shutdown - don't log as error
                OCLogger.computer("SHUTDOWN", "Machine interrupted")
                Thread.currentThread().interrupt()
            } catch (e: LuaError) {
                // Check if this is actually an interrupt wrapped in a LuaError
                if (e.cause is InterruptedException || Thread.currentThread().isInterrupted) {
                    OCLogger.computer("SHUTDOWN", "Machine interrupted")
                } else if (e.message == "machine shutdown") {
                    OCLogger.computer("SHUTDOWN", "Machine shutdown requested")
                } else {
                    OCLogger.error("Lua error: ${e.message}")
                    crash(e.message ?: "Lua error")
                }
            } catch (e: Exception) {
                if (e is InterruptedException || e.cause is InterruptedException || Thread.currentThread().isInterrupted) {
                    OCLogger.computer("SHUTDOWN", "Machine interrupted")
                } else {
                    OCLogger.error("Machine error", e)
                    crash(e.message ?: "Unknown error")
                }
            }
        }
        
        return true
    }
    
    /**
     * Push a signal to the queue.
     */
    fun pushSignal(name: String, vararg args: Any?) {
        signals.add(Signal(name, args.toList()))
        OCLogger.debug("Signal pushed: $name")
    }
    
    /**
     * Pull a signal from the queue, waiting up to timeout seconds.
     */
    fun pullSignal(timeout: Double): Signal? {
        val endTime = System.currentTimeMillis() + (timeout * 1000).toLong()
        while (System.currentTimeMillis() < endTime && state == State.RUNNING) {
            val signal = signals.poll()
            if (signal != null) return signal
            try {
                Thread.sleep(10)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return null
            }
        }
        return null
    }
    
    /**
     * Crash the machine with an error message.
     */
    fun crash(message: String) {
        OCLogger.error("Machine crashed: $message")
        crashMessage = message
        state = State.CRASHED
        runningTask?.cancel(true)
    }
    
    /**
     * Shutdown the machine.
     */
    fun shutdown() {
        OCLogger.computer("SHUTDOWN", "Machine shutting down")
        
        // Unregister all network cards belonging to this machine
        NetworkRegistry.unregisterAll(this)
        
        state = State.STOPPED
        runningTask?.cancel(true)
        globals = null
        mainCoroutine = null
        signals.clear()
        startTime = 0
        crashMessage = null
        executor.shutdownNow()
    }
    
    /**
     * Check if machine is running.
     */
    fun isRunning(): Boolean = state == State.RUNNING || state == State.STARTING
    
    data class Signal(val name: String, val args: List<Any?> = emptyList())
}
