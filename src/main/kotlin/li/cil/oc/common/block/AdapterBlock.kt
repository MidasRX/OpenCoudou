package li.cil.oc.common.block

import li.cil.oc.common.blockentity.AdapterBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.redstone.Orientation

/**
 * Adapter block - allows computers to interface with non-OC blocks.
 * Connect adjacent inventories, fluid tanks, energy storage, and more.
 */
class AdapterBlock(properties: Properties) : Block(properties), EntityBlock {
    
    companion object {
        val FACING = HorizontalDirectionalBlock.FACING
    }
    
    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }
    
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }
    
    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)
    }
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return AdapterBlockEntity(pos, state)
    }
    
    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        if (!level.isClientSide) {
            (level.getBlockEntity(pos) as? AdapterBlockEntity)?.scanNeighbors()
        }
    }
    
    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, block: Block, orientation: Orientation?, isMoving: Boolean) {
        super.neighborChanged(state, level, pos, block, orientation, isMoving)
        if (!level.isClientSide) {
            (level.getBlockEntity(pos) as? AdapterBlockEntity)?.scanNeighbors()
        }
    }
}
