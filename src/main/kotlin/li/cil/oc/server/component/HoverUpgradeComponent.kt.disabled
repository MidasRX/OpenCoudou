package li.cil.oc.server.component

/**
 * Hover upgrade - allows robots to fly higher above ground.
 * Tier 1: hover up to 8 blocks above ground
 * Tier 2: hover up to 64 blocks above ground (creative: unlimited)
 */
class HoverUpgradeComponent(private val tier: Int = 1) : AbstractComponent("hover") {
    
    private val maxHeight: Int = when (tier) {
        1 -> 8
        2 -> 64
        else -> 256 // Creative
    }
    
    init {
        registerMethod("getMaxHeight", true, "getMaxHeight():number -- Get max hover height above ground") { _ ->
            arrayOf(maxHeight)
        }
    }
    
    /**
     * Check if the robot can hover at the given height above ground.
     */
    fun canHoverAt(heightAboveGround: Int): Boolean {
        return heightAboveGround <= maxHeight
    }
    
    fun getMaxHoverHeight(): Int = maxHeight
}
