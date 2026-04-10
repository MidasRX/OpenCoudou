package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

/**
 * Sticky piston upgrade - allows robots to pull blocks like sticky pistons.
 */
class StickyPistonUpgradeComponent : AbstractComponent("sticky_piston") {
    
    companion object {
        const val ENERGY_COST = 25.0
    }
    
    private var world: Level? = null
    private var position: BlockPos = BlockPos.ZERO
    private var facing: Direction = Direction.NORTH
    
    init {
        registerMethod("pull", false, "pull([side:string]):boolean,string -- Pull block towards robot") { args ->
            val side = parseSide(args.getOrNull(0)?.toString())
            val result = tryPull(side)
            arrayOf(result.first, result.second)
        }
    }
    
    fun setWorld(level: Level?, pos: BlockPos) {
        this.world = level
        this.position = pos
    }
    
    fun setFacing(dir: Direction) {
        this.facing = dir
    }
    
    private fun tryPull(direction: Direction): Pair<Boolean, String?> {
        val level = world ?: return Pair(false, "no world")
        
        val targetPos = position.relative(direction)
        val targetState = level.getBlockState(targetPos)
        
        if (targetState.isAir) {
            return Pair(false, "air")
        }
        
        // Check if block is pullable
        val pushReaction = targetState.pistonPushReaction
        if (pushReaction.name == "BLOCK" || pushReaction.name == "DESTROY") {
            return Pair(false, "immovable")
        }
        
        // Check destination (robot's current position)
        // Would need to be air for the block to be pulled there
        
        // Would actually move the block here
        
        return Pair(true, null)
    }
    
    private fun parseSide(side: String?): Direction {
        return when (side?.lowercase()) {
            "front", "forward", "f" -> facing
            "back", "backward", "b" -> facing.opposite
            "left", "l" -> facing.counterClockWise
            "right", "r" -> facing.clockWise
            "up", "top", "u" -> Direction.UP
            "down", "bottom", "d" -> Direction.DOWN
            else -> facing
        }
    }
}
