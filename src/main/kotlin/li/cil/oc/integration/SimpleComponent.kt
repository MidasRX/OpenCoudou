package li.cil.oc.integration

import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Reachability
import li.cil.oc.api.network.NodeBuilder
import net.minecraft.nbt.CompoundTag

/**
 * Simple base implementation of a component environment.
 * Used by integration drivers to expose external blocks to computers.
 */
open class SimpleComponent(
    val componentType: String
) : ManagedEnvironment {
    
    private var _node: Node? = null
    
    override val node: Node?
        get() {
            if (_node == null) {
                _node = NodeBuilder(this)
                    .withComponent(componentType)
                    .withReachability(Reachability.NETWORK)
                    .create()
            }
            return _node
        }
    
    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message): Any? = null
    
    override fun canUpdate(): Boolean = false
    override fun update() {}
    
    override fun loadData(tag: CompoundTag) {
        node?.load(tag.getCompound("node"))
    }
    
    override fun saveData(tag: CompoundTag) {
        node?.let { n ->
            val nodeTag = CompoundTag()
            n.save(nodeTag)
            tag.put("node", nodeTag)
        }
    }
    
    /**
     * Get methods exposed by this component.
     * Subclasses should override to expose @Callback annotated methods.
     */
    open fun methods(): Map<String, (Context, Arguments) -> Array<Any?>> = emptyMap()
    
    /**
     * Helper to convert ItemStack to a Lua table.
     */
    protected fun stackToTable(stack: net.minecraft.world.item.ItemStack): Map<String, Any?> {
        if (stack.isEmpty) return emptyMap()
        
        return mapOf(
            "name" to stack.item.descriptionId,
            "label" to stack.displayName.string,
            "count" to stack.count,
            "maxCount" to stack.maxStackSize,
            "damage" to stack.damageValue,
            "maxDamage" to stack.maxDamage,
            "hasTag" to !stack.componentsPatch.isEmpty
        )
    }
    
    /**
     * Helper to convert BlockPos to a Lua table.
     */
    protected fun posToTable(pos: net.minecraft.core.BlockPos): Map<String, Any> {
        return mapOf(
            "x" to pos.x,
            "y" to pos.y,
            "z" to pos.z
        )
    }
    
    /**
     * Helper to convert FluidStack to a Lua table.
     */
    protected fun fluidToTable(stack: net.neoforged.neoforge.fluids.FluidStack): Map<String, Any?> {
        if (stack.isEmpty) return emptyMap()
        
        return mapOf(
            "name" to stack.fluid.fluidType.descriptionId,
            "amount" to stack.amount,
            "hasTag" to !stack.componentsPatch.isEmpty
        )
    }
}
