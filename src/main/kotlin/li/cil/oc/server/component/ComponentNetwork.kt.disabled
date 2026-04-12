package li.cil.oc.server.component

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Network of connected components. Components in the same network can communicate.
 * Thread-safe: Lua VM runs in a background thread while Minecraft ticks on the main thread.
 */
class ComponentNetwork {
    private val components = ConcurrentHashMap<String, Component>()
    private val listeners = CopyOnWriteArrayList<NetworkListener>()
    
    fun add(component: Component) {
        if (components.putIfAbsent(component.address, component) == null) {
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
    
    fun all(): Collection<Component> = components.values.toList()
    
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
