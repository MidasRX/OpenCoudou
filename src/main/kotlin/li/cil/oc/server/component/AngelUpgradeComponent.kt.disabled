package li.cil.oc.server.component

/**
 * Angel upgrade - allows robots to place blocks in mid-air without support.
 * Without this upgrade, robots need an adjacent block to place against.
 */
class AngelUpgradeComponent : AbstractComponent("angel") {
    
    private var isEnabled: Boolean = true
    
    init {
        registerMethod("isEnabled", true, "isEnabled():boolean -- Check if angel placement is enabled") { _ ->
            arrayOf(isEnabled)
        }
        
        registerMethod("setEnabled", false, "setEnabled(enabled:boolean):boolean -- Enable/disable angel placement") { args ->
            val enabled = args.getOrNull(0) as? Boolean ?: return@registerMethod arrayOf(isEnabled)
            isEnabled = enabled
            arrayOf(isEnabled)
        }
    }
    
    /**
     * Check if the robot can place a block at the target position.
     * With angel upgrade, no support block is needed.
     */
    fun canPlaceInAir(): Boolean = isEnabled
}
