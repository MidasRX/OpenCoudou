package li.cil.oc.common.block

import li.cil.oc.common.blockentity.CableBlockEntity
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.redstone.Orientation
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * Cable block for connecting OpenComputers components.
 * Connects to adjacent cables and OC blocks (cases, screens, etc.)
 */
class CableBlock(properties: Properties) : Block(properties), EntityBlock {

    companion object {
        val NORTH: BooleanProperty = BooleanProperty.create("north")
        val SOUTH: BooleanProperty = BooleanProperty.create("south")
        val EAST: BooleanProperty = BooleanProperty.create("east")
        val WEST: BooleanProperty = BooleanProperty.create("west")
        val UP: BooleanProperty = BooleanProperty.create("up")
        val DOWN: BooleanProperty = BooleanProperty.create("down")

        // Core shape (center piece)
        private val CORE = box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0)
        
        // Connection shapes
        private val ARM_NORTH = box(5.0, 5.0, 0.0, 11.0, 11.0, 5.0)
        private val ARM_SOUTH = box(5.0, 5.0, 11.0, 11.0, 11.0, 16.0)
        private val ARM_EAST = box(11.0, 5.0, 5.0, 16.0, 11.0, 11.0)
        private val ARM_WEST = box(0.0, 5.0, 5.0, 5.0, 11.0, 11.0)
        private val ARM_UP = box(5.0, 11.0, 5.0, 11.0, 16.0, 11.0)
        private val ARM_DOWN = box(5.0, 0.0, 5.0, 11.0, 5.0, 11.0)

        // Set of blocks that cables can connect to
        private fun canConnectTo(block: Block): Boolean {
            return block is CableBlock ||
                   block is CaseBlock ||
                   block is ScreenBlock ||
                   block is KeyboardBlock
        }
    }

    init {
        registerDefaultState(stateDefinition.any()
            .setValue(NORTH, false)
            .setValue(SOUTH, false)
            .setValue(EAST, false)
            .setValue(WEST, false)
            .setValue(UP, false)
            .setValue(DOWN, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN)
    }

    private fun canConnect(level: BlockGetter, pos: BlockPos, dir: Direction): Boolean {
        val neighbor = level.getBlockState(pos.relative(dir))
        return canConnectTo(neighbor.block)
    }

    private fun computeConnections(level: BlockGetter, pos: BlockPos): BlockState {
        return defaultBlockState()
            .setValue(NORTH, canConnect(level, pos, Direction.NORTH))
            .setValue(SOUTH, canConnect(level, pos, Direction.SOUTH))
            .setValue(EAST, canConnect(level, pos, Direction.EAST))
            .setValue(WEST, canConnect(level, pos, Direction.WEST))
            .setValue(UP, canConnect(level, pos, Direction.UP))
            .setValue(DOWN, canConnect(level, pos, Direction.DOWN))
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return computeConnections(context.level, context.clickedPos)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, block: Block, orientation: Orientation?, moved: Boolean) {
        val newState = computeConnections(level, pos)
        if (newState != state) {
            level.setBlock(pos, newState, 3)
        }
        
        // Notify block entity of connection change
        (level.getBlockEntity(pos) as? CableBlockEntity)?.updateConnections()
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        var shape = CORE
        if (state.getValue(NORTH)) shape = Shapes.or(shape, ARM_NORTH)
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, ARM_SOUTH)
        if (state.getValue(EAST)) shape = Shapes.or(shape, ARM_EAST)
        if (state.getValue(WEST)) shape = Shapes.or(shape, ARM_WEST)
        if (state.getValue(UP)) shape = Shapes.or(shape, ARM_UP)
        if (state.getValue(DOWN)) shape = Shapes.or(shape, ARM_DOWN)
        return shape
    }

    override fun propagatesSkylightDown(state: BlockState): Boolean = true
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return CableBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? = null // Cables are passive, no ticking needed
}
