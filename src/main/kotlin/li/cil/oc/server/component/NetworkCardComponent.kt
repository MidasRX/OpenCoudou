package li.cil.oc.server.component

import li.cil.oc.util.OCLogger
import java.util.UUID

/**
 * Network card component for inter-computer communication.
 * Provides wired and wireless networking capability.
 */
class NetworkCardComponent(
    private val tier: Int = 1
) : AbstractComponent("modem") {
    
    // Maximum wireless range based on tier (0 = wired only)
    private val maxWirelessRange = when (tier) {
        0 -> 0 // wired-only card
        1 -> 16
        2 -> 64
        3 -> 400
        else -> 0
    }
    
    // Whether this is a wireless card
    val isWireless = maxWirelessRange > 0
    
    // Current wake message (for wake-on-LAN)
    private var wakeMessage: String = ""
    
    // Open ports for listening
    private val openPorts = mutableSetOf<Int>()
    
    init {
        registerMethod("isWireless", true, "isWireless():boolean -- Whether this is a wireless card") { _ ->
            arrayOf(isWireless)
        }
        
        registerMethod("maxPacketSize", true, "maxPacketSize():number -- Maximum packet size") { _ ->
            // Max packet size based on tier
            arrayOf(when (tier) {
                0 -> 8192
                1 -> 8192
                2 -> 16384
                3 -> 65536
                else -> 8192
            })
        }
        
        registerMethod("open", false, "open(port:number):boolean -- Open a port for listening") { args ->
            val port = (args.getOrNull(0) as? Number)?.toInt() 
                ?: return@registerMethod arrayOf(false, "invalid port")
            
            if (port < 1 || port > 65535) {
                return@registerMethod arrayOf(false, "port out of range")
            }
            
            if (openPorts.size >= 128) {
                return@registerMethod arrayOf(false, "too many open ports")
            }
            
            if (openPorts.contains(port)) {
                return@registerMethod arrayOf(false, "port already open")
            }
            
            openPorts.add(port)
            arrayOf(true)
        }
        
        registerMethod("close", false, "close([port:number]):boolean -- Close port(s)") { args ->
            val port = (args.getOrNull(0) as? Number)?.toInt()
            
            if (port != null) {
                openPorts.remove(port)
            } else {
                openPorts.clear()
            }
            arrayOf(true)
        }
        
        registerMethod("isOpen", true, "isOpen(port:number):boolean -- Check if port is open") { args ->
            val port = (args.getOrNull(0) as? Number)?.toInt() ?: 0
            arrayOf(openPorts.contains(port))
        }
        
        registerMethod("send", false, "send(address:string,port:number,...):boolean -- Send data to address") { args ->
            val targetAddr = args.getOrNull(0)?.toString()
                ?: return@registerMethod arrayOf(false, "no address")
            val port = (args.getOrNull(1) as? Number)?.toInt()
                ?: return@registerMethod arrayOf(false, "invalid port")
            
            // Build data array from remaining args
            val data = args.drop(2)
            
            // In real implementation, this would queue the message for delivery
            // For now, just log it
            OCLogger.network("SEND", "To $targetAddr:$port - ${data.size} values")
            
            // Would need to find target computer and push modem_message signal
            arrayOf(true)
        }
        
        registerMethod("broadcast", false, "broadcast(port:number,...):boolean -- Broadcast to all on port") { args ->
            val port = (args.getOrNull(0) as? Number)?.toInt()
                ?: return@registerMethod arrayOf(false, "invalid port")
            
            val data = args.drop(1)
            
            OCLogger.network("BROADCAST", "Port $port - ${data.size} values")
            arrayOf(true)
        }
        
        registerMethod("setStrength", false, "setStrength(strength:number):number -- Set wireless strength") { args ->
            if (!isWireless) {
                return@registerMethod arrayOf(0, "not a wireless card")
            }
            
            val strength = (args.getOrNull(0) as? Number)?.toDouble() ?: 0.0
            // Clamp to max range
            val clamped = strength.coerceIn(0.0, maxWirelessRange.toDouble())
            arrayOf(clamped)
        }
        
        registerMethod("getStrength", true, "getStrength():number -- Get current wireless strength") { _ ->
            arrayOf(if (isWireless) maxWirelessRange else 0)
        }
        
        registerMethod("getWakeMessage", true, "getWakeMessage():string -- Get wake-on-LAN message") { _ ->
            arrayOf(wakeMessage)
        }
        
        registerMethod("setWakeMessage", false, "setWakeMessage(msg:string):string -- Set wake-on-LAN message") { args ->
            val old = wakeMessage
            wakeMessage = args.getOrNull(0)?.toString() ?: ""
            arrayOf(old)
        }
    }
    
    /**
     * Called when a network message is received for this card.
     * Pushes a modem_message signal to the connected machine.
     */
    fun receiveMessage(
        senderAddress: String,
        port: Int,
        distance: Double,
        vararg data: Any?
    ): Boolean {
        if (!openPorts.contains(port)) {
            return false
        }
        
        // Would need to push signal to machine
        // pushSignal("modem_message", localAddress, senderAddress, port, distance, *data)
        OCLogger.network("RECV", "From $senderAddress:$port distance=$distance")
        return true
    }
}
