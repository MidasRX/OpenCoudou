package li.cil.oc.common.block

import li.cil.oc.common.blockentity.PrinterBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.BlockHitResult

/**
 * Printer block - 3D prints custom blocks using chamelium and ink.
 */
class PrinterBlock(properties: Properties) : Block(properties), EntityBlock {
    
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
        return PrinterBlockEntity(pos, state)
    }
    
    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            // TODO: Open printer GUI
        }
        return InteractionResult.SUCCESS
    }
}
