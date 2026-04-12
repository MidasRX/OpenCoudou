package li.cil.oc.common.block

import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.BlockHitResult

/**
 * Microcontroller block - a minimal computer assembled in the assembler.
 * 
 * Unlike full computer cases, microcontrollers:
 * - Have components baked in at assembly time
 * - Cannot be opened/modified by players
 * - Have no screen capability
 * - Run programs from their EEPROM
 * - Use less power
 */
class MicrocontrollerBlock(
    private val tier: Int,
    properties: Properties
) : Block(properties), EntityBlock {
    
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
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return MicrocontrollerBlockEntity(pos, state).also {
            it.tier = tier
        }
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (!level.isClientSide && type == ModBlockEntities.MICROCONTROLLER.get()) {
            BlockEntityTicker { _, _, _, blockEntity ->
                (blockEntity as? MicrocontrollerBlockEntity)?.tick()
            }
        } else null
    }
    
    @Deprecated("Deprecated in Java")
    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) return InteractionResult.SUCCESS
        
        val be = level.getBlockEntity(pos) as? MicrocontrollerBlockEntity
            ?: return InteractionResult.PASS
        
        // Right-click toggles power
        if (be.isRunning()) {
            be.stop()
            // Could send message to player: "Microcontroller stopped"
        } else {
            if (be.start()) {
                // Could send message: "Microcontroller started"
            } else {
                // Could send message: "Cannot start - missing EEPROM"
            }
        }
        
        return InteractionResult.CONSUME
    }
    
    // ==================== Redstone ====================
    
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
        
        // Redstone can start/stop microcontroller
        val be = level.getBlockEntity(pos) as? MicrocontrollerBlockEntity ?: return
        
        val hasSignal = level.hasNeighborSignal(pos)
        if (hasSignal && !be.isRunning()) {
            be.start()
        }
    }
    
    override fun hasAnalogOutputSignal(state: BlockState): Boolean = true
    
    override fun getAnalogOutputSignal(state: BlockState, level: Level, pos: BlockPos): Int {
        val be = level.getBlockEntity(pos) as? MicrocontrollerBlockEntity ?: return 0
        // Output 15 if running, 0 if stopped
        return if (be.isRunning()) 15 else 0
    }
}
