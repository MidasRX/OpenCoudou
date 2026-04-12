package li.cil.oc.common.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState

/**
 * Robot Afterimage block - a transient block that appears at a robot's previous position
 * while it's animating movement to a new position. This creates the "ghost" effect.
 * 
 * This block is invisible and non-solid, removed automatically after the animation completes.
 */
class RobotAfterimageBlock(properties: Properties) : Block(properties) {
    
    init {
        // Not in creative menu
    }
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.INVISIBLE
    
    // Not solid - entities can pass through
    override fun isPossibleToRespawnInThis(state: BlockState): Boolean = true
    
    // Transparent
    override fun propagatesSkylightDown(state: BlockState): Boolean = true
    
    /**
     * Schedule removal when the block is placed.
     * In practice this is managed by the robot's movement animation.
     */
    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        super.onPlace(state, level, pos, oldState, movedByPiston)
        // Schedule removal after animation completes (about 10 ticks)
        level.scheduleTick(pos, this, 10)
    }
}
