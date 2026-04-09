package li.cil.oc.server.machine.luac

import li.cil.oc.api.machine.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import org.luaj.vm2.*
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.*
import java.io.ByteArrayInputStream
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * LuaJ-based Lua 5.2/5.3 compatible architecture implementation.
 * Provides a sandboxed Lua environment for running computer programs.
 */
@ArchitectureName("Lua 5.4")
class LuaJArchitecture(override val machine: Machine) : Architecture {
    
    companion object {
        const val NAME = "Lua 5.4"
        
        // Memory limits per RAM tier
        val MEMORY_TIERS = mapOf(
            1 to 192 * 1024,  // Tier 1: 192 KB
            2 to 256 * 1024,  // Tier 2: 256 KB  
            3 to 384 * 1024,  // Tier 3: 384 KB
            4 to 512 * 1024,  // Tier 4: 512 KB
            5 to 768 * 1024,  // Tier 5: 768 KB
            6 to 1024 * 1024  // Tier 6: 1 MB
        )
    }
    
    private var globals: Globals? = null
    private var mainThread: LuaThread? = null
    private var bootCode: ByteArray? = null
    private val signalPending = AtomicBoolean(false)
    private var memoryLimit = 256 * 1024
    
    override val isInitialized: Boolean
        get() = globals != null
    
    override fun initialize(): Boolean {
        try {
            // Create sandboxed Lua globals
            globals = createSandboxedGlobals()
            
            // Set up APIs
            setupComponentAPI()
            setupComputerAPI()
            setupUnicodeAPI()
            
            // Load boot code
            bootCode = loadBootCode()
            
            return true
        } catch (e: Exception) {
            machine.crash("Failed to initialize Lua: ${e.message}")
            return false
        }
    }
    
    override fun close() {
        globals = null
        mainThread = null
        bootCode = null
    }
    
    override fun runThreaded(isSynchronizedReturn: Boolean): ExecutionResult {
        val g = globals ?: return ExecutionResult.Error("Architecture not initialized")
        
        try {
            if (mainThread == null) {
                // Start new thread with boot code
                val boot = bootCode ?: return ExecutionResult.Error("No boot code")
                val chunk = g.load(ByteArrayInputStream(boot), "=bios", "bt", g)
                mainThread = LuaThread(g, chunk)
            }
            
            // Resume execution
            val thread = mainThread!!
            
            // LuaJ coroutine handling - simplified version
            // LuaJ doesn't use typical coroutine semantics - just execute directly
            return try {
                // Just call the function/thread - LuaThread is a LuaFunction
                // Use methods from LuaFunction API  
                val result: Varargs = (thread as LuaFunction).call()
                // If we get here, thread completed
                // Check first return value - use arg1() method to avoid Kotlin invoke conflicts
                val nargs = result.narg()
                if (nargs == 0) {
                    ExecutionResult.Shutdown
                } else {
                    val firstValue: LuaValue = result.arg1()
                    if (firstValue.isnil()) {
                        ExecutionResult.Shutdown
                    } else {
                        // Thread returned a value, treat as error message
                        ExecutionResult.Error(firstValue.tojstring())
                    }
                }
            } catch (e: LuaError) {
                // Thread yielded or errored
                if (e.message?.contains("yield") == true) {
                    ExecutionResult.Sleep(1)
                } else {
                    ExecutionResult.Error(e.message ?: "Lua error")
                }
            }
        } catch (e: LuaError) {
            return ExecutionResult.Error(e.message ?: "Lua error")
        } catch (e: Exception) {
            return ExecutionResult.Error(e.message ?: "Unknown error")
        }
    }
    
    override fun runSynchronized() {
        // Execute any pending synchronized operations
    }
    
    override fun recomputeMemory(memory: Iterable<ItemStack>): Boolean {
        var totalMemory = 0
        
        for (stack in memory) {
            // Calculate memory based on item tier
            val tier = getMemoryTier(stack)
            if (tier > 0) {
                totalMemory += MEMORY_TIERS[tier] ?: 0
            }
        }
        
        memoryLimit = totalMemory.coerceAtLeast(192 * 1024)
        return totalMemory > 0
    }
    
    private fun getMemoryTier(stack: ItemStack): Int {
        // Check item registry name for tier
        val name = stack.item.descriptionId
        return when {
            name.contains("memory") || name.contains("ram") -> {
                when {
                    name.contains("tier1") || name.contains("1") -> 1
                    name.contains("tier2") || name.contains("2") -> 2
                    name.contains("tier3") || name.contains("3") -> 3
                    name.contains("tier4") || name.contains("4") -> 4
                    name.contains("tier5") || name.contains("5") -> 5
                    name.contains("tier6") || name.contains("6") -> 6
                    else -> 1
                }
            }
            else -> 0
        }
    }
    
    override fun onSignal() {
        signalPending.set(true)
    }
    
    override fun load(tag: CompoundTag) {
        // Load state
        if (tag.contains("memory")) {
            memoryLimit = tag.getInt("memory")
        }
    }
    
    override fun save(tag: CompoundTag) {
        tag.putInt("memory", memoryLimit)
    }
    
    // === Lua Environment Setup ===
    
    private fun createSandboxedGlobals(): Globals {
        val globals = Globals()
        
        // Load standard libraries
        globals.load(JseBaseLib())
        globals.load(PackageLib())
        globals.load(Bit32Lib())
        globals.load(TableLib())
        globals.load(StringLib())
        globals.load(CoroutineLib())
        globals.load(JseMathLib())
        globals.load(JseOsLib())
        
        // Install compiler
        LuaC.install(globals)
        
        // Sandbox dangerous functions
        removeDangerous(globals)
        
        // Add custom print
        globals.set("print", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val sb = StringBuilder()
                for (i in 1..args.narg()) {
                    if (i > 1) sb.append("\t")
                    sb.append(args.arg(i).tojstring())
                }
                machine.signal("print", sb.toString())
                return LuaValue.NIL
            }
        })
        
        // Add checkArg
        globals.set("checkArg", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val n = args.checkint(1)
                val have = args.arg(2).typename()
                
                for (i in 3..args.narg()) {
                    val want = args.checkjstring(i)
                    if (have == want || (want == "nil" && args.arg(2).isnil())) {
                        return LuaValue.NIL
                    }
                }
                
                val expected = (3..args.narg()).map { args.checkjstring(it) }.joinToString(" or ")
                throw LuaError("bad argument #$n ($expected expected, got $have)")
            }
        })
        
        return globals
    }
    
    private fun removeDangerous(globals: Globals) {
        globals.set("collectgarbage", LuaValue.NIL)
        globals.set("dofile", LuaValue.NIL)
        globals.set("loadfile", LuaValue.NIL)
        globals.set("module", LuaValue.NIL)
        globals.set("debug", LuaValue.NIL)
        globals.set("rawequal", LuaValue.NIL)
        globals.set("rawget", LuaValue.NIL)
        globals.set("rawlen", LuaValue.NIL)
        globals.set("rawset", LuaValue.NIL)
        globals.set("getfenv", LuaValue.NIL)
        globals.set("setfenv", LuaValue.NIL)
        
        // Sandbox os
        val os = globals.get("os")
        if (os.istable()) {
            val safeOs = LuaTable()
            listOf("clock", "date", "difftime", "time").forEach { fname ->
                val f = os.get(fname)
                if (!f.isnil()) safeOs.set(fname, f)
            }
            globals.set("os", safeOs)
        }
        
        // Sandbox string
        val string = globals.get("string")
        if (string.istable()) {
            string.checktable().set("dump", LuaValue.NIL)
        }
    }
    
    private fun setupComponentAPI() {
        val g = globals ?: return
        val componentTable = LuaTable()
        
        // component.list
        componentTable.set("list", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val filter = if (args.narg() >= 1 && !args.arg(1).isnil()) args.checkjstring(1) else null
                val exact = args.optboolean(2, false)
                
                val components = machine.components
                val filtered = components.filter { (_, type) ->
                    when {
                        filter == null -> true
                        exact -> type == filter
                        else -> type.contains(filter)
                    }
                }
                
                val iter = filtered.iterator()
                return LuaValue.varargsOf(arrayOf(
                    object : VarArgFunction() {
                        override fun invoke(args: Varargs): Varargs {
                            return if (iter.hasNext()) {
                                val (address, type) = iter.next()
                                LuaValue.varargsOf(LuaValue.valueOf(address), LuaValue.valueOf(type))
                            } else {
                                LuaValue.NIL
                            }
                        }
                    }
                ))
            }
        })
        
        // component.invoke
        componentTable.set("invoke", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val address = args.checkjstring(1)
                val method = args.checkjstring(2)
                
                val luaArgs = mutableListOf<Any?>()
                for (i in 3..args.narg()) {
                    luaArgs.add(fromLuaValue(args.arg(i)))
                }
                
                return try {
                    val result = machine.invoke(address, method, *luaArgs.toTypedArray())
                    toVarargs(result)
                } catch (e: Exception) {
                    LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(e.message ?: "error"))
                }
            }
        })
        
        // component.type
        componentTable.set("type", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val address = args.checkjstring(1)
                val type = machine.components[address]
                return if (type != null) LuaValue.valueOf(type) else LuaValue.NIL
            }
        })
        
        // component.proxy
        componentTable.set("proxy", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val address = args.checkjstring(1)
                val type = machine.components[address] 
                    ?: return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("no such component"))
                
                val proxy = LuaTable()
                proxy.set("address", LuaValue.valueOf(address))
                proxy.set("type", LuaValue.valueOf(type))
                
                // Methods would be added here based on component type
                return proxy
            }
        })
        
        // component.get
        componentTable.set("get", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val partial = args.checkjstring(1)
                val componentType = if (args.narg() >= 2 && !args.arg(2).isnil()) args.checkjstring(2) else null
                
                for ((address, type) in machine.components) {
                    if (address.startsWith(partial)) {
                        if (componentType == null || type == componentType) {
                            return LuaValue.valueOf(address)
                        }
                    }
                }
                return LuaValue.NIL
            }
        })
        
        g.set("component", componentTable)
    }
    
    private fun setupComputerAPI() {
        val g = globals ?: return
        val computerTable = LuaTable()
        
        // computer.address
        computerTable.set("address", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(machine.node()?.address ?: "")
            }
        })
        
        // computer.uptime
        computerTable.set("uptime", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(machine.uptime)
            }
        })
        
        // computer.freeMemory
        computerTable.set("freeMemory", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(memoryLimit / 2) // Approximate
            }
        })
        
        // computer.totalMemory
        computerTable.set("totalMemory", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(memoryLimit)
            }
        })
        
        // computer.energy
        computerTable.set("energy", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(10000.0) // TODO: get from power system
            }
        })
        
        // computer.maxEnergy
        computerTable.set("maxEnergy", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(10000.0)
            }
        })
        
        // computer.pushSignal
        computerTable.set("pushSignal", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val name = args.checkjstring(1)
                val signalArgs = mutableListOf<Any?>()
                for (i in 2..args.narg()) {
                    signalArgs.add(fromLuaValue(args.arg(i)))
                }
                machine.signal(name, *signalArgs.toTypedArray())
                return LuaValue.TRUE
            }
        })
        
        // computer.pullSignal
        computerTable.set("pullSignal", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                // This is handled by coroutine yield
                val timeout = args.optdouble(1, Double.MAX_VALUE)
                return globals!!.yield(LuaValue.valueOf(timeout))
            }
        })
        
        // computer.shutdown
        computerTable.set("shutdown", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val reboot = args.optboolean(1, false)
                if (reboot) {
                    machine.crash("reboot")
                } else {
                    machine.stop()
                }
                return LuaValue.NIL
            }
        })
        
        // computer.users
        computerTable.set("users", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val users = machine.users
                return toLuaTable(users)
            }
        })
        
        g.set("computer", computerTable)
    }
    
    private fun setupUnicodeAPI() {
        val g = globals ?: return
        val unicodeTable = LuaTable()
        
        // unicode.char
        unicodeTable.set("char", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val sb = StringBuilder()
                for (i in 1..args.narg()) {
                    sb.appendCodePoint(args.checkint(i))
                }
                return LuaValue.valueOf(sb.toString())
            }
        })
        
        // unicode.len
        unicodeTable.set("len", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaValue.valueOf(arg.checkjstring().codePointCount(0, arg.checkjstring().length))
            }
        })
        
        // unicode.sub
        unicodeTable.set("sub", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val s = args.checkjstring(1)
                val codePoints = s.codePoints().toArray()
                
                var start = args.checkint(2) - 1
                var end = args.optint(3, codePoints.size) - 1
                
                if (start < 0) start = codePoints.size + start + 1
                if (end < 0) end = codePoints.size + end + 1
                
                start = start.coerceIn(0, codePoints.size)
                end = end.coerceIn(0, codePoints.size)
                
                if (start > end) return LuaValue.valueOf("")
                
                val sb = StringBuilder()
                for (i in start..end) {
                    sb.appendCodePoint(codePoints[i])
                }
                return LuaValue.valueOf(sb.toString())
            }
        })
        
        // unicode.upper
        unicodeTable.set("upper", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaValue.valueOf(arg.checkjstring().uppercase())
            }
        })
        
        // unicode.lower
        unicodeTable.set("lower", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaValue.valueOf(arg.checkjstring().lowercase())
            }
        })
        
        g.set("unicode", unicodeTable)
    }
    
    // === Helper Methods ===
    
    private fun loadBootCode(): ByteArray {
        // Try to get from EEPROM component
        for ((address, type) in machine.components) {
            if (type == "eeprom") {
                try {
                    val result = machine.invoke(address, "get")
                    if (result.isNotEmpty() && result[0] is ByteArray) {
                        return result[0] as ByteArray
                    }
                    if (result.isNotEmpty() && result[0] is String) {
                        return (result[0] as String).toByteArray()
                    }
                } catch (e: Exception) {
                    // Continue trying
                }
            }
        }
        
        // Default minimal boot code
        return """
            -- Minimal boot code
            local computer = computer
            local component = component
            
            -- Find filesystem
            local fs = component.list("filesystem")()
            if fs then
                local proxy = component.proxy(fs)
                if proxy and proxy.exists and proxy.exists("init.lua") then
                    local handle = proxy.open("init.lua", "r")
                    if handle then
                        local code = ""
                        repeat
                            local data = proxy.read(handle, 8192)
                            if data then code = code .. data end
                        until not data
                        proxy.close(handle)
                        local fn, err = load(code, "=init.lua")
                        if fn then
                            fn()
                        else
                            error("Failed to load init.lua: " .. tostring(err))
                        end
                    end
                end
            end
            
            while true do
                computer.pullSignal(1)
            end
        """.trimIndent().toByteArray()
    }
    
    private fun fromLuaValue(value: LuaValue): Any? = when {
        value.isnil() -> null
        value.isboolean() -> value.toboolean()
        value.isint() -> value.toint()
        value.isnumber() -> value.todouble()
        value.isstring() -> value.tojstring()
        value.istable() -> {
            val table = value.checktable()
            val map = mutableMapOf<Any?, Any?>()
            var k = LuaValue.NIL
            while (true) {
                val n = table.next(k)
                k = n.arg1()
                if (k.isnil()) break
                map[fromLuaValue(k)] = fromLuaValue(n.arg(2))
            }
            map
        }
        else -> value.tojstring()
    }
    
    private fun toLuaValue(value: Any?): LuaValue = when (value) {
        null -> LuaValue.NIL
        is Boolean -> LuaValue.valueOf(value)
        is Int -> LuaValue.valueOf(value)
        is Long -> LuaValue.valueOf(value.toDouble())
        is Float -> LuaValue.valueOf(value.toDouble())
        is Double -> LuaValue.valueOf(value)
        is String -> LuaValue.valueOf(value)
        is ByteArray -> LuaValue.valueOf(String(value))
        is Array<*> -> {
            val table = LuaTable()
            value.forEachIndexed { i, v -> table.set(i + 1, toLuaValue(v)) }
            table
        }
        is List<*> -> {
            val table = LuaTable()
            value.forEachIndexed { i, v -> table.set(i + 1, toLuaValue(v)) }
            table
        }
        is Map<*, *> -> {
            val table = LuaTable()
            value.forEach { (k, v) -> table.set(toLuaValue(k), toLuaValue(v)) }
            table
        }
        else -> LuaValue.valueOf(value.toString())
    }
    
    private fun toLuaTable(list: List<*>): LuaTable {
        val table = LuaTable()
        list.forEachIndexed { i, v -> table.set(i + 1, toLuaValue(v)) }
        return table
    }
    
    private fun toVarargs(values: Array<Any?>?): Varargs {
        if (values == null || values.isEmpty()) return LuaValue.NIL
        return LuaValue.varargsOf(values.map { toLuaValue(it) }.toTypedArray())
    }
}
