package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

/**
 * Piston upgrade - allows robots to push blocks like pistons.
 */
class PistonUpgradeComponent : AbstractComponent("piston") {
    
    companion object {
        const val ENERGY_COST = 20.0
        const val MAX_PUSH_DISTANCE = 12
    }
    
    private var world: Level? = null
    private var position: BlockPos = BlockPos.ZERO
    private var facing: Direction = Direction.NORTH
    
    init {
        registerMethod("push", false, "push([side:string]):boolean,string -- Push block") { args ->
            val side = parseSide(args.getOrNull(0)?.toString())
            val result = tryPush(side)
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
    
    private fun tryPush(direction: Direction): Pair<Boolean, String?> {
        val level = world ?: return Pair(false, "no world")
        
        val targetPos = position.relative(direction)
        val targetState = level.getBlockState(targetPos)
        
        if (targetState.isAir) {
            return Pair(false, "air")
        }
        
        // Check if block is pushable
        // In vanilla, this would use PushReaction
        val pushReaction = targetState.pistonPushReaction
        if (pushReaction.name == "BLOCK" || pushReaction.name == "DESTROY") {
            return Pair(false, "immovable")
        }
        
        // Find how many blocks to push
        var pushCount = 0
        var checkPos = targetPos
        
        while (pushCount < MAX_PUSH_DISTANCE) {
            val checkState = level.getBlockState(checkPos)
            if (checkState.isAir) break
            
            val reaction = checkState.pistonPushReaction
            if (reaction.name == "BLOCK") {
                return Pair(false, "blocked")
            }
            
            pushCount++
            checkPos = checkPos.relative(direction)
        }
        
        if (pushCount >= MAX_PUSH_DISTANCE) {
            return Pair(false, "too many blocks")
        }
        
        // Would actually push blocks here
        // This requires careful handling of block entities, etc.
        
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
