package li.cil.oc.server.network

import li.cil.oc.server.component.NetworkCardComponent
import li.cil.oc.server.machine.Machine
import li.cil.oc.util.OCLogger
import net.minecraft.core.BlockPos
import java.util.concurrent.ConcurrentHashMap

/**
 * Global registry for all network cards. Enables inter-computer communication.
 * Thread-safe for concurrent access from multiple machine threads.
 */
object NetworkRegistry {
    
    /**
     * Entry for a registered network card
     */
    data class NetworkEntry(
        val card: NetworkCardComponent,
        val machine: Machine,
        val worldPos: BlockPos?
    )
    
    // All registered network cards by address
    private val cards = ConcurrentHashMap<String, NetworkEntry>()
    
    /**
     * Register a network card with its owning machine.
     */
    fun register(card: NetworkCardComponent, machine: Machine, pos: BlockPos? = null) {
        cards[card.address] = NetworkEntry(card, machine, pos)
        OCLogger.network("REGISTRY", "Registered card ${card.address} (wireless=${card.isWireless})")
    }
    
    /**
     * Unregister a network card.
     */
    fun unregister(card: NetworkCardComponent) {
        cards.remove(card.address)
        OCLogger.network("REGISTRY", "Unregistered card ${card.address}")
    }
    
    /**
     * Unregister all cards belonging to a machine.
     */
    fun unregisterAll(machine: Machine) {
        val toRemove = cards.values.filter { it.machine === machine }.map { it.card.address }
        toRemove.forEach { cards.remove(it) }
        if (toRemove.isNotEmpty()) {
            OCLogger.network("REGISTRY", "Unregistered ${toRemove.size} cards for machine")
        }
    }
    
    /**
     * Send a message to a specific address.
     * @return true if the message was delivered
     */
    fun send(
        senderCard: NetworkCardComponent,
        targetAddress: String,
        port: Int,
        vararg data: Any?
    ): Boolean {
        val sender = cards[senderCard.address] ?: return false
        val target = cards[targetAddress] ?: return false
        
        // Check if target has the port open
        if (!target.card.isPortOpen(port)) {
            return false
        }
        
        // Calculate distance (0 for wired)
        val distance = calculateDistance(sender, target)
        
        // Check wireless range
        if (senderCard.isWireless && distance > senderCard.getSignalStrength()) {
            return false
        }
        
        // Deliver the message
        deliverMessage(target, senderCard.address, port, distance, *data)
        return true
    }
    
    /**
     * Broadcast a message to all cards with port open.
     * @return number of cards that received the message
     */
    fun broadcast(
        senderCard: NetworkCardComponent,
        port: Int,
        vararg data: Any?
    ): Int {
        val sender = cards[senderCard.address] ?: return 0
        var delivered = 0
        
        for ((addr, entry) in cards) {
            // Don't send to self
            if (addr == senderCard.address) continue
            
            // Check if port is open
            if (!entry.card.isPortOpen(port)) continue
            
            // Calculate distance
            val distance = calculateDistance(sender, entry)
            
            // Check wireless range
            if (senderCard.isWireless && distance > senderCard.getSignalStrength()) {
                continue
            }
            
            // For wired cards, they must be in same network (simplified: same dimension)
            // In real OC, this would check cable connections
            if (!senderCard.isWireless && !entry.card.isWireless) {
                // Wired networks: for now, assume all wired cards in same world are connected
                // This is a simplification - real OC uses cable routing
            }
            
            // Deliver
            deliverMessage(entry, senderCard.address, port, distance, *data)
            delivered++
        }
        
        OCLogger.network("BROADCAST", "Delivered to $delivered receivers on port $port")
        return delivered
    }
    
    private fun calculateDistance(sender: NetworkEntry, receiver: NetworkEntry): Double {
        // Wired is always 0 distance
        if (!sender.card.isWireless && !receiver.card.isWireless) {
            return 0.0
        }
        
        val senderPos = sender.worldPos
        val receiverPos = receiver.worldPos
        
        if (senderPos != null && receiverPos != null) {
            val dx = (senderPos.x - receiverPos.x).toDouble()
            val dy = (senderPos.y - receiverPos.y).toDouble()
            val dz = (senderPos.z - receiverPos.z).toDouble()
            return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
        }
        
        return 0.0
    }
    
    private fun deliverMessage(
        target: NetworkEntry,
        senderAddress: String,
        port: Int,
        distance: Double,
        vararg data: Any?
    ) {
        // Build signal args: modem_message, localAddress, senderAddress, port, distance, ...data
        val args = mutableListOf<Any?>(
            target.card.address,  // local address (receiver)
            senderAddress,        // remote address (sender)
            port,
            distance
        )
        args.addAll(data)
        
        // Push signal to the target machine
        target.machine.pushSignal("modem_message", *args.toTypedArray())
        
        OCLogger.network("DELIVER", "modem_message to ${target.card.address} from $senderAddress port $port")
    }
    
    /**
     * Get all registered card addresses (for debugging)
     */
    fun getAllAddresses(): List<String> = cards.keys.toList()
}
