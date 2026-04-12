package li.cil.oc.server.component

import net.minecraft.core.BlockPos

/**
 * Waypoint component - provides a visible waypoint marker for navigation.
 * Robots and drones can detect waypoints and use them for navigation.
 */
class WaypointComponent : AbstractComponent("waypoint") {
    
    private var label: String = ""
    private var redstoneOutput: Int = 0
    private var position: BlockPos = BlockPos.ZERO
    private var isRegistered: Boolean = false
    
    init {
        registerMethod("getLabel", true, "getLabel():string -- Get waypoint label") { _ ->
            arrayOf(label)
        }
        
        registerMethod("setLabel", false, "setLabel(label:string):string -- Set waypoint label") { args ->
            val newLabel = args.getOrNull(0)?.toString() ?: ""
            val old = label
            label = newLabel.take(32) // Limit label length
            updateRegistration()
            arrayOf(old)
        }
        
        registerMethod("getRedstoneOutput", true, "getRedstoneOutput():number -- Get redstone output level") { _ ->
            arrayOf(redstoneOutput)
        }
        
        registerMethod("setRedstoneOutput", false, "setRedstoneOutput(value:number):number -- Set redstone output level (0-15)") { args ->
            val value = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 15) ?: 0
            val old = redstoneOutput
            redstoneOutput = value
            updateRegistration()
            arrayOf(old)
        }
    }
    
    /**
     * Set the position of this waypoint and register it.
     */
    fun setPosition(pos: BlockPos) {
        // Unregister from old position
        if (isRegistered) {
            NavigationUpgradeComponent.unregisterWaypoint(position)
        }
        
        position = pos
        updateRegistration()
    }
    
    /**
     * Update the waypoint registration with current values.
     */
    private fun updateRegistration() {
        NavigationUpgradeComponent.registerWaypoint(position, label, redstoneOutput)
        isRegistered = true
    }
    
    /**
     * Unregister the waypoint when removed.
     */
    fun unregister() {
        if (isRegistered) {
            NavigationUpgradeComponent.unregisterWaypoint(position)
            isRegistered = false
        }
    }
    
    fun getLabel(): String = label
    fun getRedstoneOutput(): Int = redstoneOutput
    fun getPosition(): BlockPos = position
}
