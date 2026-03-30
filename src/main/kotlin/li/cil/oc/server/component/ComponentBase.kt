package li.cil.oc.server.component

import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ComponentVisibility
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Reachability
import net.minecraft.nbt.CompoundTag
import java.util.UUID

/**
 * Simple node implementation for components.
 */
class SimpleNode(
    override val address: String = UUID.randomUUID().toString(),
    override val host: Environment,
    override val reachability: Reachability = Reachability.NEIGHBORS
) : Node {
    
    private var _network: li.cil.oc.api.network.Network? = null
    
    override val network: li.cil.oc.api.network.Network?
        get() = _network
    
    override fun connect(other: Node): Boolean {
        // Simplified implementation
        return true
    }
    
    override fun disconnect(other: Node): Boolean {
        // Simplified implementation
        return true
    }
    
    override fun remove() {
        _network = null
    }
    
    override fun neighbors(): Collection<Node> = emptyList()
    
    override fun reachableNodes(): Collection<Node> = emptyList()
    
    override fun sendToAddress(target: String, name: String, vararg data: Any?) {
        // Simplified implementation
    }
    
    override fun sendToNeighbors(name: String, vararg data: Any?) {
        // Simplified implementation
    }
    
    override fun sendToReachable(name: String, vararg data: Any?) {
        // Simplified implementation
    }
    
    override fun sendToVisible(name: String, vararg data: Any?) {
        // Simplified implementation
    }
    
    override fun save(tag: CompoundTag) {
        tag.putString("address", address)
    }
    
    override fun load(tag: CompoundTag) {
        // Address is immutable in this implementation
    }
}

/**
 * Base class for all Lua-callable components in OpenComputers.
 * 
 * This provides the core functionality needed for a component to be
 * visible to Lua programs and respond to method calls.
 *
 * @param name The component name as seen by Lua programs (e.g., "gpu", "filesystem")
 * @param visibility How this component is visible on the network
 */
abstract class ComponentBase(
    val componentName: String,
    val visibility: ComponentVisibility = ComponentVisibility.NETWORK
) : Environment {
    
    /**
     * The network node for this component.
     */
    override val node: Node? by lazy { SimpleNode(host = this) }
    
    /**
     * The unique address of this component on the network.
     */
    val address: String
        get() = node?.address ?: ""
    
    /**
     * Returns a map of method name to method implementation.
     * 
     * Override this in subclasses to declare Lua-callable methods.
     * Each entry maps a method name (String) to a function that takes
     * a Context and vararg Any? arguments and returns an array of results.
     *
     * Example:
     * ```kotlin
     * override fun methods() = mapOf(
     *     "doSomething" to ::doSomething,
     *     "getValue" to ::getValue
     * )
     * 
     * fun doSomething(context: Context, args: Array<Any?>): Array<Any?> {
     *     // implementation
     *     return arrayOf(true)
     * }
     * ```
     */
    abstract fun methods(): Map<String, (Context, Array<Any?>) -> Array<Any?>>
    
    /**
     * Called when this component connects to a network node.
     */
    override fun onConnect(node: Node) {
        // Override in subclasses if needed
    }
    
    /**
     * Called when this component disconnects from a network node.
     */
    override fun onDisconnect(node: Node) {
        // Override in subclasses if needed
    }
    
    /**
     * Called when a network message is received.
     */
    override fun onMessage(message: Message): Any? {
        // Override in subclasses if needed
        return null
    }
    
    /**
     * Saves component state to NBT.
     * Override to save custom data.
     */
    open fun save(tag: CompoundTag) {
        node?.save(tag)
    }
    
    /**
     * Loads component state from NBT.
     * Override to load custom data.
     */
    open fun load(tag: CompoundTag) {
        node?.load(tag)
    }
    
    /**
     * Queues a signal to be sent to connected computers.
     * 
     * @param name The signal name
     * @param args The signal arguments
     */
    protected fun queueSignal(name: String, vararg args: Any?) {
        node?.sendToReachable("computer.signal", name, *args)
    }
    
    /**
     * Helper to get a required argument from Lua call.
     */
    protected inline fun <reified T> getArg(args: Array<Any?>, index: Int): T {
        return args.getOrNull(index) as? T
            ?: throw IllegalArgumentException("Argument $index must be a ${T::class.simpleName}")
    }
    
    /**
     * Helper to get an optional argument from Lua call.
     */
    protected inline fun <reified T> getOptionalArg(args: Array<Any?>, index: Int, default: T): T {
        return args.getOrNull(index) as? T ?: default
    }
    
    /**
     * Helper to check argument count.
     */
    protected fun checkArgCount(args: Array<Any?>, minCount: Int, maxCount: Int = minCount) {
        if (args.size < minCount || args.size > maxCount) {
            val expected = if (minCount == maxCount) "$minCount" else "$minCount-$maxCount"
            throw IllegalArgumentException("Expected $expected arguments, got ${args.size}")
        }
    }
}
