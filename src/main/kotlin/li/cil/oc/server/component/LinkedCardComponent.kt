package li.cil.oc.server.component

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Linked Card component - Quantum-entangled network communication.
 * 
 * Linked cards come in pairs and provide instant cross-dimensional,
 * cross-world communication. Unlike regular network cards or wireless
 * cards, linked cards have no range limit and work across dimensions.
 * 
 * The cards are paired during crafting and share a unique tunnel/channel
 * identifier that allows them to communicate exclusively with each other.
 * 
 * Creative-tier: Unlimited energy, unlimited range.
 */
class LinkedCardComponent(
    private val channel: String = UUID.randomUUID().toString()
) : AbstractComponent("tunnel") {
    
    private var energy = 10000.0
    private var wakeMessage: String? = null
    
    companion object {
        // Global registry of all linked card endpoints for quantum communication
        private val endpoints = ConcurrentHashMap<String, MutableSet<LinkedCardComponent>>()
        
        // Message queue for each endpoint (address -> messages)
        private val messageQueues = ConcurrentHashMap<String, MutableList<QueuedMessage>>()
        
        fun register(component: LinkedCardComponent) {
            endpoints.computeIfAbsent(component.channel) { ConcurrentHashMap.newKeySet() }
                .add(component)
        }
        
        fun unregister(component: LinkedCardComponent) {
            endpoints[component.channel]?.remove(component)
            if (endpoints[component.channel]?.isEmpty() == true) {
                endpoints.remove(component.channel)
            }
        }
        
        fun getPartners(component: LinkedCardComponent): Set<LinkedCardComponent> {
            return endpoints[component.channel]?.filter { it != component }?.toSet() ?: emptySet()
        }
    }
    
    data class QueuedMessage(
        val sourceAddress: String,
        val port: Int,
        val data: Array<Any?>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is QueuedMessage) return false
            return sourceAddress == other.sourceAddress && port == other.port && data.contentEquals(other.data)
        }
        
        override fun hashCode(): Int {
            return 31 * (31 * sourceAddress.hashCode() + port) + data.contentHashCode()
        }
    }
    
    private val receivedMessages = mutableListOf<QueuedMessage>()
    
    init {
        register(this)
        
        // Send message to linked partner
        registerMethod("send", false, "function(data...) -- Sends data to the linked card partner.") { args ->
            val partners = getPartners(this)
            
            if (partners.isEmpty()) {
                return@registerMethod arrayOf(false, "no linked partner found")
            }
            
            // Energy cost calculation (minimal for linked cards)
            val packetSize = args.sumOf { (it?.toString()?.length ?: 0) }
            val energyCost = packetSize / 32.0 + 50.0 // Base cost + size-based cost
            
            if (energy < energyCost) {
                return@registerMethod arrayOf(null, "not enough energy")
            }
            
            energy -= energyCost
            
            // Send to all partners
            val message = QueuedMessage(address, 0, args)
            for (partner in partners) {
                partner.receivePacket(message)
            }
            
            arrayOf(true)
        }
        
        registerMethod("maxPacketSize", true, "function():number -- Gets the maximum packet size.") { _ ->
            // Linked cards support larger packets than regular network cards
            arrayOf(8192)
        }
        
        registerMethod("getChannel", true, "function():string -- Gets this linked card's channel address.") { _ ->
            arrayOf(channel)
        }
        
        registerMethod("getWakeMessage", true, "function():string -- Gets the wake message pattern, if set.") { _ ->
            arrayOf(wakeMessage)
        }
        
        registerMethod("setWakeMessage", false, "function([pattern:string]):string -- Sets the wake message pattern. Returns old pattern.") { args ->
            val oldPattern = wakeMessage
            wakeMessage = args.getOrNull(0) as? String
            arrayOf(oldPattern)
        }
        
        // Check if there are pending messages
        registerMethod("hasMessage", true, "function():boolean -- Check if there are pending messages.") { _ ->
            arrayOf(receivedMessages.isNotEmpty())
        }
        
        registerMethod("getMessage", false, "function():table|nil -- Get the next pending message, if any.") { _ ->
            val message = synchronized(receivedMessages) {
                if (receivedMessages.isNotEmpty()) {
                    receivedMessages.removeAt(0)
                } else {
                    null
                }
            }
            
            if (message != null) {
                arrayOf(mapOf(
                    "source" to message.sourceAddress,
                    "port" to message.port,
                    "data" to message.data.toList()
                ))
            } else {
                arrayOf(null)
            }
        }
    }
    
    fun destroy() {
        unregister(this)
    }
    
    /**
     * Returns whether this is a creative-tier component.
     * Linked cards are creative-tier and bypass most energy costs.
     */
    fun isCreative(): Boolean = true
    
    /**
     * Receives a packet from a linked partner.
     * This queues the message for the computer to process.
     */
    fun receivePacket(packet: QueuedMessage) {
        synchronized(receivedMessages) {
            receivedMessages.add(packet)
            // Keep only last 256 messages to prevent memory issues
            while (receivedMessages.size > 256) {
                receivedMessages.removeAt(0)
            }
        }
        
        // Check wake message
        if (wakeMessage != null && packet.data.isNotEmpty()) {
            val firstArg = packet.data[0]?.toString() ?: ""
            if (firstArg.contains(wakeMessage!!)) {
                // Would trigger computer wake
            }
        }
    }
}

/**
 * Linked Card Tunnel Registry - Global manager for linked card pairs.
 * 
 * This manages the quantum network that allows paired linked cards
 * to communicate across any distance and dimension.
 */
object LinkedCardRegistry {
    // Map of tunnel ID -> set of linked cards on that tunnel
    private val tunnels = ConcurrentHashMap<String, MutableSet<LinkedCardComponent>>()
    
    /**
     * Creates a new tunnel ID for a pair of linked cards.
     */
    fun createTunnel(): String {
        return UUID.randomUUID().toString()
    }
    
    /**
     * Registers a linked card to its tunnel.
     */
    fun register(card: LinkedCardComponent, tunnel: String) {
        tunnels.computeIfAbsent(tunnel) { ConcurrentHashMap.newKeySet() }
            .add(card)
    }
    
    /**
     * Unregisters a linked card from its tunnel.
     */
    fun unregister(card: LinkedCardComponent, tunnel: String) {
        tunnels[tunnel]?.remove(card)
        if (tunnels[tunnel]?.isEmpty() == true) {
            tunnels.remove(tunnel)
        }
    }
    
    /**
     * Gets all endpoints on a tunnel except the sender.
     */
    fun getEndpoints(tunnel: String, except: LinkedCardComponent? = null): Set<LinkedCardComponent> {
        val endpoints = tunnels[tunnel] ?: return emptySet()
        return if (except != null) {
            endpoints.filter { it != except }.toSet()
        } else {
            endpoints.toSet()
        }
    }
    
    /**
     * Sends a packet to all endpoints on a tunnel except the sender.
     */
    fun broadcast(tunnel: String, sender: LinkedCardComponent, message: LinkedCardComponent.QueuedMessage) {
        for (endpoint in getEndpoints(tunnel, sender)) {
            endpoint.receivePacket(message)
        }
    }
}
