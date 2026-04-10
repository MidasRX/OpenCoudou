package li.cil.oc.server.component

/**
 * Network of connected components. Components in the same network can communicate.
 */
class ComponentNetwork {
    private val components = mutableMapOf<String, Component>()
    private val listeners = mutableListOf<NetworkListener>()
    
    fun add(component: Component) {
        if (component.address !in components) {
            components[component.address] = component
            listeners.forEach { it.onComponentAdded(component) }
        }
    }
    
    fun remove(component: Component) {
        if (components.remove(component.address) != null) {
            listeners.forEach { it.onComponentRemoved(component) }
        }
    }
    
    fun remove(address: String) {
        components[address]?.let { remove(it) }
    }
    
    fun get(address: String): Component? = components[address]
    
    fun getByType(type: String): List<Component> = 
        components.values.filter { it.componentType == type }
    
    fun getFirst(type: String): Component? = 
        components.values.firstOrNull { it.componentType == type }
    
    fun all(): Collection<Component> = components.values
    
    fun addListener(listener: NetworkListener) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: NetworkListener) {
        listeners.remove(listener)
    }
    
    fun clear() {
        val toRemove = components.values.toList()
        toRemove.forEach { remove(it) }
    }
}

interface NetworkListener {
    fun onComponentAdded(component: Component)
    fun onComponentRemoved(component: Component)
}
