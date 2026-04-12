package li.cil.oc.common.block

import li.cil.oc.common.blockentity.RedstoneIOBlockEntity
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

/**
 * Redstone I/O block - provides redstone input/output for computers.
 * 
 * Can read redstone signals from all 6 sides and output independently per side.
 * Connects to computers via the component network and provides the redstone API.
 */
class RedstoneIOBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return RedstoneIOBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (!level.isClientSide && type == ModBlockEntities.REDSTONE_IO.get()) {
            BlockEntityTicker { _, _, _, blockEntity ->
                (blockEntity as? RedstoneIOBlockEntity)?.tick()
            }
        } else null
    }
    
    // ==================== Redstone ====================
    
    override fun isSignalSource(state: BlockState): Boolean = true
    
    override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
        val be = level.getBlockEntity(pos) as? RedstoneIOBlockEntity ?: return 0
        // Return output on the opposite side (direction is where signal goes TO)
        return be.getOutput(direction.opposite)
    }
    
    override fun getDirectSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
        return getSignal(state, level, pos, direction)
    }
    
    @Deprecated("Deprecated in Java", ReplaceWith("true"))
    override fun hasAnalogOutputSignal(state: BlockState): Boolean = true
    
    @Deprecated("Deprecated in Java")
    override fun getAnalogOutputSignal(state: BlockState, level: Level, pos: BlockPos): Int {
        val be = level.getBlockEntity(pos) as? RedstoneIOBlockEntity ?: return 0
        return be.getComparatorOutput()
    }
    
    @Suppress("OVERRIDE_DEPRECATION")
    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        block: Block,
        orientation: net.minecraft.world.level.redstone.Orientation?,
        isMoving: Boolean
    ) {
        super.neighborChanged(state, level, pos, block, orientation, isMoving)
        // Trigger input update on neighbor change
        val be = level.getBlockEntity(pos) as? RedstoneIOBlockEntity ?: return
        be.tick() // This will update inputs
    }
}
