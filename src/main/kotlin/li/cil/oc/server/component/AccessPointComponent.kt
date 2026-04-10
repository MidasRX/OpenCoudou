package li.cil.oc.server.component

/**
 * Access Point component - provides wireless network connectivity.
 * Combines relay functionality with wireless transmission.
 */
class AccessPointComponent : AbstractComponent("access_point") {
    
    companion object {
        const val DEFAULT_STRENGTH = 16
        const val MAX_WIRELESS_RANGE = 400
    }
    
    private var signalStrength: Int = DEFAULT_STRENGTH
    private var wirelessEnabled: Boolean = true
    private var wiredEnabled: Boolean = true
    
    init {
        registerMethod("getStrength", true, "getStrength():number -- Get signal strength") { _ ->
            arrayOf(signalStrength)
        }
        
        registerMethod("setStrength", false, "setStrength(strength:number):number -- Set signal strength") { args ->
            val newStrength = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, MAX_WIRELESS_RANGE) ?: signalStrength
            val oldStrength = signalStrength
            signalStrength = newStrength
            arrayOf(oldStrength)
        }
        
        registerMethod("isWirelessEnabled", true, "isWirelessEnabled():boolean -- Check if wireless is enabled") { _ ->
            arrayOf(wirelessEnabled)
        }
        
        registerMethod("setWirelessEnabled", false, "setWirelessEnabled(enabled:boolean):boolean -- Enable/disable wireless") { args ->
            val enabled = args.getOrNull(0) as? Boolean ?: wirelessEnabled
            val oldEnabled = wirelessEnabled
            wirelessEnabled = enabled
            arrayOf(oldEnabled)
        }
        
        registerMethod("isWiredEnabled", true, "isWiredEnabled():boolean -- Check if wired relay is enabled") { _ ->
            arrayOf(wiredEnabled)
        }
        
        registerMethod("setWiredEnabled", false, "setWiredEnabled(enabled:boolean):boolean -- Enable/disable wired relay") { args ->
            val enabled = args.getOrNull(0) as? Boolean ?: wiredEnabled
            val oldEnabled = wiredEnabled
            wiredEnabled = enabled
            arrayOf(oldEnabled)
        }
    }
    
    fun getWirelessRange(): Int = if (wirelessEnabled) signalStrength else 0
    
    fun isWirelessEnabled(): Boolean = wirelessEnabled
    
    fun isWiredEnabled(): Boolean = wiredEnabled
}
