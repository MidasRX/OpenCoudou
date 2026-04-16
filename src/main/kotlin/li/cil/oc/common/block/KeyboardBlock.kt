package li.cil.oc.common.block

import li.cil.oc.common.blockentity.KeyboardBlockEntity
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ScheduledTickAccess
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.AttachFace
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.util.RandomSource

/**
 * Keyboard block - attaches to case/screen blocks like a button.
 * Must be placed on top of a case or on the side of a screen.
 */
class KeyboardBlock(properties: Properties) : Block(properties), EntityBlock {

    companion object {
        val FACING = HorizontalDirectionalBlock.FACING
        val ATTACH_FACE = BlockStateProperties.ATTACH_FACE

        // Keyboard shapes based on attachment face and facing direction
        // Keyboard is thin: 14px wide, 8px deep, 1px tall
        
        // Floor attachment (on top of case) - keyboard sits on top
        private val SHAPE_FLOOR_NORTH = box(1.0, 0.0, 4.0, 15.0, 1.0, 12.0)
        private val SHAPE_FLOOR_SOUTH = box(1.0, 0.0, 4.0, 15.0, 1.0, 12.0)
        private val SHAPE_FLOOR_EAST  = box(4.0, 0.0, 1.0, 12.0, 1.0, 15.0)
        private val SHAPE_FLOOR_WEST  = box(4.0, 0.0, 1.0, 12.0, 1.0, 15.0)
        
        // Wall attachment - keyboard sticks out from wall (1px thick, on the wall surface)
        // Facing = direction keyboard faces (away from wall = away from screen)
        // The screen is always BEHIND the keyboard (facing.opposite direction)
        // So the model must be flush against the BACK face of the keyboard's block space
        private val SHAPE_WALL_NORTH = box(1.0, 4.0, 15.0, 15.0, 12.0, 16.0) // facing=north, screen to south → z=15..16
        private val SHAPE_WALL_SOUTH = box(1.0, 4.0, 0.0, 15.0, 12.0, 1.0)   // facing=south, screen to north → z=0..1
        private val SHAPE_WALL_EAST  = box(0.0, 4.0, 1.0, 1.0, 12.0, 15.0)   // facing=east,  screen to west  → x=0..1
        private val SHAPE_WALL_WEST  = box(15.0, 4.0, 1.0, 16.0, 12.0, 15.0) // facing=west,  screen to east  → x=15..16
        
        // Ceiling attachment (under a block)
        private val SHAPE_CEILING_NORTH = box(1.0, 15.0, 4.0, 15.0, 16.0, 12.0)
        private val SHAPE_CEILING_SOUTH = box(1.0, 15.0, 4.0, 15.0, 16.0, 12.0)
        private val SHAPE_CEILING_EAST  = box(4.0, 15.0, 1.0, 12.0, 16.0, 15.0)
        private val SHAPE_CEILING_WEST  = box(4.0, 15.0, 1.0, 12.0, 16.0, 15.0)
    }

    init {
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(ATTACH_FACE, AttachFace.FLOOR))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, ATTACH_FACE)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        // Place based on the face that was clicked (like buttons)
        val clickedFace = context.clickedFace
        
        val state = when (clickedFace) {
            Direction.UP -> defaultBlockState()
                .setValue(ATTACH_FACE, AttachFace.FLOOR)
                .setValue(FACING, context.horizontalDirection)
            Direction.DOWN -> defaultBlockState()
                .setValue(ATTACH_FACE, AttachFace.CEILING)
                .setValue(FACING, context.horizontalDirection)
            else -> defaultBlockState()
                .setValue(ATTACH_FACE, AttachFace.WALL)
                .setValue(FACING, clickedFace)  // Facing = direction keyboard faces (away from wall)
        }
        
        return if (state.canSurvive(context.level, context.clickedPos)) state else null
    }

    override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {
        val attachFace = state.getValue(ATTACH_FACE)
        val facing = state.getValue(FACING)
        
        // Support direction is where the wall/floor/ceiling is
        val supportDir = when (attachFace) {
            AttachFace.FLOOR -> Direction.DOWN
            AttachFace.CEILING -> Direction.UP
            AttachFace.WALL -> facing.opposite  // Wall is OPPOSITE to where keyboard faces
        }
        
        val supportPos = pos.relative(supportDir)
        val supportState = level.getBlockState(supportPos)
        
        // Accept any non-air block as support (isFaceSturdy is too strict for mod blocks)
        return !supportState.isAir
    }

    override fun updateShape(
        state: BlockState,
        level: LevelReader,
        scheduledTick: ScheduledTickAccess,
        pos: BlockPos,
        direction: Direction,
        neighborPos: BlockPos,
        neighborState: BlockState,
        random: RandomSource
    ): BlockState {
        // Drop if support is gone
        val attachFace = state.getValue(ATTACH_FACE)
        val facing = state.getValue(FACING)
        
        val supportDir = when (attachFace) {
            AttachFace.FLOOR -> Direction.DOWN
            AttachFace.CEILING -> Direction.UP
            AttachFace.WALL -> facing.opposite  // Wall is OPPOSITE to where keyboard faces
        }
        
        if (direction == supportDir) {
            val supportState = level.getBlockState(pos.relative(supportDir))
            if (supportState.isAir) {
                return Blocks.AIR.defaultBlockState()
            }
        }
        
        return super.updateShape(state, level, scheduledTick, pos, direction, neighborPos, neighborState, random)
    }

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return KeyboardBlockEntity(pos, state)
    }

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (!level.isClientSide && type == ModBlockEntities.KEYBOARD.get()) {
            BlockEntityTicker { _, _, _, blockEntity ->
                (blockEntity as? KeyboardBlockEntity)?.tick()
            }
        } else null
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val attachFace = state.getValue(ATTACH_FACE)
        val facing = state.getValue(FACING)
        
        return when (attachFace) {
            AttachFace.FLOOR -> when (facing) {
                Direction.SOUTH -> SHAPE_FLOOR_SOUTH
                Direction.EAST -> SHAPE_FLOOR_EAST
                Direction.WEST -> SHAPE_FLOOR_WEST
                else -> SHAPE_FLOOR_NORTH
            }
            AttachFace.WALL -> when (facing) {
                Direction.SOUTH -> SHAPE_WALL_SOUTH
                Direction.EAST -> SHAPE_WALL_EAST
                Direction.WEST -> SHAPE_WALL_WEST
                else -> SHAPE_WALL_NORTH
            }
            AttachFace.CEILING -> when (facing) {
                Direction.SOUTH -> SHAPE_CEILING_SOUTH
                Direction.EAST -> SHAPE_CEILING_EAST
                Direction.WEST -> SHAPE_CEILING_WEST
                else -> SHAPE_CEILING_NORTH
            }
        }
    }

    override fun propagatesSkylightDown(state: BlockState): Boolean = true
}
