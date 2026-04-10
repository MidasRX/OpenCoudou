package li.cil.oc.server.component

/**
 * Relay component - forwards network messages between connected networks.
 * Acts as a network bridge and can filter messages.
 */
class RelayComponent : AbstractComponent("relay") {
    
    companion object {
        const val DEFAULT_STRENGTH = 16
        const val MAX_QUEUE_SIZE = 20
    }
    
    private var signalStrength: Int = DEFAULT_STRENGTH
    private var isEnabled: Boolean = true
    
    // Packet queues for each side
    private val messageQueue = mutableListOf<NetworkMessage>()
    
    data class NetworkMessage(
        val source: String,
        val destination: String,
        val port: Int,
        val data: Array<Any?>
    )
    
    init {
        registerMethod("getStrength", true, "getStrength():number -- Get signal strength") { _ ->
            arrayOf(signalStrength)
        }
        
        registerMethod("setStrength", false, "setStrength(strength:number):number -- Set signal strength") { args ->
            val newStrength = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 400) ?: signalStrength
            val oldStrength = signalStrength
            signalStrength = newStrength
            arrayOf(oldStrength)
        }
        
        registerMethod("isEnabled", true, "isEnabled():boolean -- Check if relay is enabled") { _ ->
            arrayOf(isEnabled)
        }
        
        registerMethod("setEnabled", false, "setEnabled(enabled:boolean):boolean -- Enable/disable relay") { args ->
            val enabled = args.getOrNull(0) as? Boolean ?: isEnabled
            val oldEnabled = isEnabled
            isEnabled = enabled
            arrayOf(oldEnabled)
        }
    }
    
    /**
     * Forward a message through the relay.
     */
    fun forwardMessage(source: String, destination: String, port: Int, data: Array<Any?>) {
        if (!isEnabled) return
        if (messageQueue.size >= MAX_QUEUE_SIZE) return
        
        messageQueue.add(NetworkMessage(source, destination, port, data))
    }
    
    /**
     * Process queued messages.
     */
    fun tick(): List<NetworkMessage> {
        if (!isEnabled || messageQueue.isEmpty()) return emptyList()
        
        val messages = messageQueue.toList()
        messageQueue.clear()
        return messages
    }
    
    fun getSignalStrength(): Int = signalStrength
}
