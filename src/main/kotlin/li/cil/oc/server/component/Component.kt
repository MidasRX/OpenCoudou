package li.cil.oc.server.component

import java.util.UUID

/**
 * Base interface for all components that can be connected to a computer.
 */
interface Component {
    /** Unique address for this component */
    val address: String
    
    /** Component type name (e.g., "gpu", "screen", "keyboard") */
    val componentType: String
    
    /** Get methods callable from Lua */
    fun methods(): Map<String, ComponentMethod>
}

/**
 * A method that can be called from Lua on a component.
 */
data class ComponentMethod(
    val name: String,
    val direct: Boolean = true, // Can be called directly (no yield)
    val doc: String = "",
    val invoke: (args: Array<Any?>) -> Array<Any?>
)

/**
 * Base implementation of Component with common functionality.
 */
abstract class AbstractComponent(
    override val componentType: String
) : Component {
    override val address: String = UUID.randomUUID().toString()
    
    private val methodMap = mutableMapOf<String, ComponentMethod>()
    
    protected fun registerMethod(name: String, direct: Boolean = true, doc: String = "", 
                                  invoke: (Array<Any?>) -> Array<Any?>) {
        methodMap[name] = ComponentMethod(name, direct, doc, invoke)
    }
    
    override fun methods(): Map<String, ComponentMethod> = methodMap
}
