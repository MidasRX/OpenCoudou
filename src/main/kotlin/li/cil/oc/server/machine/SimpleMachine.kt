package li.cil.oc.server.machine

import li.cil.oc.OpenComputers
import li.cil.oc.api.machine.*
import li.cil.oc.api.network.Component
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Network
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Reachability
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import org.luaj.vm2.*
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.*
import java.io.ByteArrayInputStream
import java.util.concurrent.*

/**
 * Simplified Machine implementation for basic computer operation.
 */
class SimpleMachine(override val host: MachineHost) : Machine {
    
    companion object {
        private val executor = Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "OpenComputers-Machine").apply { isDaemon = true }
        }
    }
    
    override var state: MachineState = MachineState.STOPPED
        private set
    
    override var architecture: Architecture? = null
        private set
    
    override var hasCrashed: Boolean = false
        private set
    
    override var crashMessage: String? = null
        private set
    
    private val _components = mutableMapOf<String, String>()
    override val components: Map<String, String> get() = _components.toMap()
    
    override var totalMemory: Int = 256 * 1024
        private set
    
    // Components installed in the case (set by CaseBlockEntity before start)
    var installedComponents: InstalledComponents = InstalledComponents()
    
    // Flag set by architecture when Lua calls computer.shutdown(true)
    var pendingReboot: Boolean = false

    // Flag for server-tick reboot (set in executor, consumed in update)
    @Volatile
    var needsReboot: Boolean = false
    
    private var _cpuTime: Double = 0.0
    override val cpuTime: Double get() = _cpuTime
    
    private var _callBudget: Int = 1000
    override val callBudget: Int get() = _callBudget
    
    private val _users = mutableListOf<String>()
    override val users: List<String> get() = _users.toList()
    
    override var lastStartTime: Long = 0
        private set
    
    override val uptime: Double
        get() {
            val level = host.world() ?: return 0.0
            return if (state.isRunning) {
                (level.gameTime - lastStartTime) / 20.0
            } else 0.0
        }
    
    private val signalQueue = ConcurrentLinkedQueue<Signal>()
    private var _node: Node? = null
    private var nodeAddress: String = java.util.UUID.randomUUID().toString()
    private var executorFuture: Future<*>? = null

    /**
     * Minimal Node implementation for the machine - just provides an address.
     */
    private inner class SimpleNode(override val address: String) : Node {
        override val network: Network? get() = null
        override val host: Environment get() = object : Environment {
            override val node: Node? get() = this@SimpleNode
            override fun onConnect(node: Node) {}
            override fun onDisconnect(node: Node) {}
            override fun onMessage(message: Message): Any? = null
        }
        override val reachability: Reachability get() = Reachability.NETWORK
        override fun connect(other: Node): Boolean = false
        override fun disconnect(other: Node): Boolean = false
        override fun remove() {}
        override fun neighbors(): Collection<Node> = emptyList()
        override fun reachableNodes(): Collection<Node> = emptyList()
        override fun sendToAddress(target: String, name: String, vararg args: Any?): Any? = null
        override fun sendToNeighbors(name: String, vararg args: Any?) {}
        override fun sendToReachable(name: String, vararg args: Any?) {}
        override fun sendToVisible(name: String, vararg args: Any?) {}
        override fun load(tag: CompoundTag) {}
        override fun save(tag: CompoundTag) {}
    }
    
    data class Signal(val name: String, val args: Array<out Any?>) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Signal) return false
            return name == other.name && args.contentEquals(other.args)
        }
        override fun hashCode(): Int = 31 * name.hashCode() + args.contentHashCode()
    }
    
    // Context implementation
    override fun node(): Node = _node ?: throw IllegalStateException("Machine has no node")
    
    override fun signal(name: String, vararg args: Any?): Boolean {
        if (signalQueue.size >= 256) return false
        signalQueue.offer(Signal(name, args))
        architecture?.onSignal()
        return true
    }

    fun pollSignal(): Signal? = signalQueue.poll()
    
    override fun canInteract(player: String): Boolean {
        if (_users.isEmpty()) return true
        return _users.contains(player)
    }
    
    override fun addUser(name: String): Boolean {
        if (_users.contains(name)) return false
        _users.add(name)
        return true
    }
    
    override fun removeUser(name: String): Boolean {
        return _users.remove(name)
    }
    
    override fun consumeCallBudget(cost: Int): Boolean {
        if (_callBudget >= cost) {
            _callBudget -= cost
            return true
        }
        return false
    }
    
    // Machine control
    override fun start(): Boolean {
        if (state != MachineState.STOPPED && state != MachineState.CRASHED) {
            return false
        }
        
        hasCrashed = false
        crashMessage = null
        signalQueue.clear()
        
        // Ensure node exists with a stable address
        if (_node == null) {
            _node = SimpleNode(nodeAddress)
        }
        
        // Use actual memory from installed components
        totalMemory = if (installedComponents.totalMemory > 0) installedComponents.totalMemory else 256 * 1024
        
        // Create LuaJ architecture
        val arch = SimpleLuaArchitecture(this)
        architecture = arch
        
        state = MachineState.STARTING
        host.onMachineStateChanged(state)
        
        if (!arch.initialize()) {
            crash("Failed to initialize Lua")
            return false
        }
        
        lastStartTime = host.world()?.gameTime ?: 0
        state = MachineState.RUNNING
        host.onMachineStateChanged(state)
        
        // Notify about components
        for ((address, name) in _components) {
            signal("component_added", address, name)
        }
        
        OpenComputers.LOGGER.debug("Machine started at {}", host.hostPosition())
        return true
    }
    
    override fun pause(duration: Double): Boolean {
        if (!state.isRunning) return false
        state = MachineState.PAUSED
        host.onMachineStateChanged(state)
        return true
    }
    
    override fun stop(): Boolean {
        if (state == MachineState.STOPPED) return false
        
        state = MachineState.STOPPING
        host.onMachineStateChanged(state)
        
        executorFuture?.cancel(true)
        executorFuture = null
        
        architecture?.close()
        architecture = null
        
        signalQueue.clear()
        state = MachineState.STOPPED
        host.onMachineStateChanged(state)
        
        OpenComputers.LOGGER.debug("Machine stopped at {}", host.hostPosition())
        return true
    }
    
    override fun crash(message: String) {
        OpenComputers.LOGGER.warn("Machine crashed at {}: {}", host.hostPosition(), message)
        
        hasCrashed = true
        crashMessage = message
        
        executorFuture?.cancel(true)
        executorFuture = null
        
        architecture?.close()
        architecture = null
        
        signalQueue.clear()
        state = MachineState.CRASHED
        host.onMachineStateChanged(state)
    }
    
    override fun update(): Boolean {
        if (!host.canRun()) {
            if (state.isRunning) pause(0.0)
            return false
        }
        
        _callBudget = 1000
        
        // Handle pending reboot on server thread
        if (needsReboot && state == MachineState.STOPPED) {
            needsReboot = false
            start()
            return state.isRunning
        }
        
        when (state) {
            MachineState.STOPPED, MachineState.CRASHED -> return false
            MachineState.RUNNING -> {
                val future = executorFuture
                if (future == null || future.isDone) {
                    scheduleExecution()
                }
            }
            else -> {}
        }
        
        return state.isRunning
    }
    
    private fun scheduleExecution() {
        val arch = architecture ?: return
        
        executorFuture = executor.submit {
            val startTime = System.nanoTime()
            try {
                when (val result = arch.runThreaded(false)) {
                    is ExecutionResult.Shutdown -> {
                        if (pendingReboot) {
                            pendingReboot = false
                            stop()
                            needsReboot = true
                        } else {
                            stop()
                        }
                    }
                    is ExecutionResult.Error -> crash(result.message)
                    else -> {}
                }
            } catch (e: Exception) {
                OpenComputers.LOGGER.error("Error executing machine", e)
                crash(e.message ?: "Unknown error")
            } finally {
                _cpuTime += (System.nanoTime() - startTime) / 1_000_000_000.0
            }
        }
    }
    
    override fun onSynchronizedCall() {}
    
    fun registerComponent(address: String, type: String) {
        _components[address] = type
        if (state.isRunning) {
            signal("component_added", address, type)
        }
    }
    
    fun unregisterComponent(address: String) {
        val type = _components.remove(address) ?: return
        if (state.isRunning) {
            signal("component_removed", address, type)
        }
    }
    
    fun saveData(tag: CompoundTag) {
        tag.putInt("state", state.ordinal)
        tag.putBoolean("crashed", hasCrashed)
        tag.putString("nodeAddress", nodeAddress)
        crashMessage?.let { tag.putString("crashMessage", it) }
        // Save architecture state (filesystems, boot address, etc.)
        val archTag = CompoundTag()
        architecture?.save(archTag)
        tag.put("architecture", archTag)
    }
    
    fun loadData(tag: CompoundTag) {
        state = MachineState.entries.getOrNull(tag.getInt("state")) ?: MachineState.STOPPED
        hasCrashed = tag.getBoolean("crashed")
        if (tag.contains("nodeAddress")) nodeAddress = tag.getString("nodeAddress")
        _node = SimpleNode(nodeAddress)
        crashMessage = if (tag.contains("crashMessage")) tag.getString("crashMessage") else null
        // Load architecture state (filesystems, boot address, etc.)
        if (tag.contains("architecture")) {
            if (architecture == null) {
                architecture = SimpleLuaArchitecture(this)
            }
            architecture?.load(tag.getCompound("architecture"))
        }
    }
    
    // Populates machine with BIOS/RAM from host inventory
    override fun recomputeMemory(memory: Iterable<ItemStack>): Boolean {
        totalMemory = memory.fold(0) { acc: Int, stack: ItemStack ->
            // Each RAM item adds some memory
            acc + if (stack.isEmpty) 0 else 64 * 1024 
        }
        return true
    }
    
    override fun component(address: String): Component? {
        // Look through connected components to find one with this address
        val node = host.machine()?.node() ?: return null
        return findComponentInNetwork(node, address)
    }
    
    override fun componentsOf(type: String): List<Component> {
        // Find all components of the given type
        val node = host.machine()?.node() ?: return emptyList()
        return findComponentsOfTypeInNetwork(node, type)
    }
    
    override fun invoke(address: String, method: String, vararg args: Any?): Array<Any?> {
        // Find the component and invoke the method
        val comp = component(address) ?: throw Exception("no such component")
        // For now, return empty - real implementation would call actual component methods
        return arrayOf()
    }
    
    override fun invokeAsync(address: String, method: String, vararg args: Any?): Iterator<Array<Any?>> {
        // Async invocation - returns an iterator that yields when ready
        return object : Iterator<Array<Any?>> {
            private var consumed = false
            override fun hasNext(): Boolean = !consumed
            override fun next(): Array<Any?> {
                consumed = true
                return invoke(address, method, *args)
            }
        }
    }
    
    private fun findComponentInNetwork(node: Node, address: String): Component? {
        // Simple DFS to find component by address
        if (node is Component && node.address == address) {
            return node
        }
        // In a full implementation, we'd traverse the network graph
        return null
    }
    
    private fun findComponentsOfTypeInNetwork(node: Node, type: String): List<Component> {
        // In a full implementation, we'd traverse the network graph
        // For now return components from our registered list
        return _components.entries.filter { it.value == type }
            .mapNotNull { findComponentInNetwork(node, it.key) }
    }
}
