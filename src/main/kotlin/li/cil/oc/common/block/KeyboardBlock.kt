package li.cil.oc.common.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

class KeyboardBlock(properties: Properties) : Block(properties) {

    companion object {
        val FACING = HorizontalDirectionalBlock.FACING

        // Thin keyboard shape per facing direction (2px tall, 14px wide, 8px deep)
        private val SHAPE_NORTH = box(1.0, 0.0, 4.0, 15.0, 2.0, 12.0)
        private val SHAPE_SOUTH = box(1.0, 0.0, 4.0, 15.0, 2.0, 12.0)
        private val SHAPE_EAST  = box(4.0, 0.0, 1.0, 12.0, 2.0, 15.0)
        private val SHAPE_WEST  = box(4.0, 0.0, 1.0, 12.0, 2.0, 15.0)
    }

    init {
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return when (state.getValue(FACING)) {
            Direction.SOUTH -> SHAPE_SOUTH
            Direction.EAST -> SHAPE_EAST
            Direction.WEST -> SHAPE_WEST
            else -> SHAPE_NORTH
        }
    }

    override fun propagatesSkylightDown(state: BlockState): Boolean = true
}
