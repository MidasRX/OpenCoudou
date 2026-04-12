package li.cil.oc.server.component

import li.cil.oc.util.OCLogger
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

/**
 * Redstone card component for reading and setting redstone signals.
 * Tier 1: Basic redstone (0-15)
 * Tier 2: Bundled cable support (if available)
 */
class RedstoneCardComponent(
    private val tier: Int = 1
) : AbstractComponent("redstone") {
    
    // Reference to computer's position (set when installed)
    var computerPos: BlockPos? = null
    var level: Level? = null
    
    // Wake threshold for redstone wake signal
    private var wakeThreshold: Int = 0
    
    // Cached output values per side
    private val outputs = IntArray(6) { 0 }
    
    init {
        registerMethod("getInput", true, "getInput(side:number):number -- Get redstone input on side") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
            val dir = sideToDirection(side) ?: return@registerMethod arrayOf(0)
            
            val pos = computerPos ?: return@registerMethod arrayOf(0)
            val lvl = level ?: return@registerMethod arrayOf(0)
            
            val signal = lvl.getSignal(pos.relative(dir), dir)
            arrayOf(signal)
        }
        
        registerMethod("setOutput", false, "setOutput(side:number,value:number):number -- Set redstone output on side") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
            val value = (args.getOrNull(1) as? Number)?.toInt()?.coerceIn(0, 15) ?: 0
            
            if (side < 0 || side > 5) return@registerMethod arrayOf(0, "invalid side")
            
            val old = outputs[side]
            outputs[side] = value
            
            // Would need to update block to emit redstone
            OCLogger.component("REDSTONE", "setOutput", "Side $side = $value")
            
            arrayOf(old)
        }
        
        registerMethod("getOutput", true, "getOutput(side:number):number -- Get current output on side") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
            if (side < 0 || side > 5) return@registerMethod arrayOf(0)
            arrayOf(outputs[side])
        }
        
        registerMethod("getComparatorInput", true, "getComparatorInput(side:number):number -- Get comparator input") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
            val dir = sideToDirection(side) ?: return@registerMethod arrayOf(0)
            
            val pos = computerPos ?: return@registerMethod arrayOf(0)
            val lvl = level ?: return@registerMethod arrayOf(0)
            
            val targetPos = pos.relative(dir)
            val state = lvl.getBlockState(targetPos)
            
            // Get comparator output from the block
            if (state.hasAnalogOutputSignal()) {
                val signal = state.getAnalogOutputSignal(lvl, targetPos)
                return@registerMethod arrayOf(signal)
            }
            
            arrayOf(0)
        }
        
        registerMethod("setWakeThreshold", false, "setWakeThreshold(threshold:number):number -- Set wake threshold") { args ->
            val threshold = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 15) ?: 0
            val old = wakeThreshold
            wakeThreshold = threshold
            arrayOf(old)
        }
        
        registerMethod("getWakeThreshold", true, "getWakeThreshold():number -- Get wake threshold") { _ ->
            arrayOf(wakeThreshold)
        }
        
        // Tier 2 bundled cable support
        if (tier >= 2) {
            registerMethod("getBundledInput", true, "getBundledInput(side:number,color:number):number -- Get bundled input") { args ->
                val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
                val color = (args.getOrNull(1) as? Number)?.toInt() ?: 0
                // Would need integration with bundled cable mod
                arrayOf(0)
            }
            
            registerMethod("setBundledOutput", false, "setBundledOutput(side:number,color:number,value:number):number -- Set bundled output") { args ->
                val side = (args.getOrNull(0) as? Number)?.toInt() ?: 0
                val color = (args.getOrNull(1) as? Number)?.toInt() ?: 0
                val value = (args.getOrNull(2) as? Number)?.toInt()?.coerceIn(0, 255) ?: 0
                // Would need integration with bundled cable mod
                arrayOf(0)
            }
            
            registerMethod("getBundledOutput", true, "getBundledOutput(side:number,color:number):number -- Get bundled output") { args ->
                // Would need integration with bundled cable mod
                arrayOf(0)
            }
        }
    }
    
    private fun sideToDirection(side: Int): Direction? = when (side) {
        0 -> Direction.DOWN
        1 -> Direction.UP
        2 -> Direction.NORTH
        3 -> Direction.SOUTH
        4 -> Direction.WEST
        5 -> Direction.EAST
        else -> null
    }
    
    /**
     * Get all input signals (for polling).
     */
    fun getAllInputs(): IntArray {
        val pos = computerPos ?: return IntArray(6)
        val lvl = level ?: return IntArray(6)
        
        return IntArray(6) { side ->
            val dir = sideToDirection(side) ?: return@IntArray 0
            lvl.getSignal(pos.relative(dir), dir)
        }
    }
    
    /**
     * Check if any input exceeds wake threshold.
     */
    fun shouldWake(): Boolean {
        if (wakeThreshold == 0) return false
        return getAllInputs().any { it >= wakeThreshold }
    }
}
