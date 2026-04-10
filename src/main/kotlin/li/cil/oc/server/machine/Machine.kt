package li.cil.oc.server.machine

import li.cil.oc.server.component.*
import li.cil.oc.util.OCLogger
import org.luaj.vm2.*
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
            globals!!.load(JseMathLib())
            
            // Install our custom libraries
            installComputerLib()
            installComponentLib()
            installUnicodeLib()
            
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
        
        computer.set("shutdown", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val reboot = args.optboolean(1, false)
                if (reboot) {
                    signals.add(Signal("reboot"))
                } else {
                    shutdown()
                }
                return LuaValue.NIL
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
                // Would play beep sound
                OCLogger.computer("BEEP", args.optjstring(1, "."))
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
                
                // Return iterator function
                val iter = object : VarArgFunction() {
                    val entries = result.keys().toList().iterator()
                    override fun invoke(args: Varargs): Varargs {
                        if (entries.hasNext()) {
                            val key = entries.next()
                            return LuaValue.varargsOf(key, result.get(key))
                        }
                        return LuaValue.NIL
                    }
                }
                return iter
            }
        })
        
        component.set("type", object : OneArgFunction() {
            override fun call(addr: LuaValue): LuaValue {
                val comp = network.get(addr.checkjstring()) 
                    ?: return LuaValue.NIL
                return LuaValue.valueOf(comp.componentType)
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
        
        globals!!.set("component", component)
    }
    
    /**
     * Install the 'unicode' library.
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
                return LuaValue.valueOf(s.substring(start, end))
            }
        })
        
        unicode.set("char", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val sb = StringBuilder()
                for (i in 1..args.narg()) {
                    sb.append(args.checkint(i).toChar())
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
        
        globals!!.set("unicode", unicode)
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
                OCLogger.boot("RUN", "Executing BIOS code")
                
                val chunk = globals!!.load(biosCode!!, "=bios")
                chunk.call()
                
                OCLogger.computer("FINISHED", "BIOS execution completed normally")
            } catch (e: LuaError) {
                OCLogger.error("Lua error: ${e.message}")
                crash(e.message ?: "Lua error")
            } catch (e: Exception) {
                OCLogger.error("Machine error", e)
                crash(e.message ?: "Unknown error")
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
        while (System.currentTimeMillis() < endTime) {
            val signal = signals.poll()
            if (signal != null) return signal
            Thread.sleep(10)
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
        state = State.STOPPED
        runningTask?.cancel(true)
        globals = null
        mainCoroutine = null
        signals.clear()
        startTime = 0
        crashMessage = null
    }
    
    /**
     * Check if machine is running.
     */
    fun isRunning(): Boolean = state == State.RUNNING || state == State.STARTING
    
    data class Signal(val name: String, val args: List<Any?> = emptyList())
}
