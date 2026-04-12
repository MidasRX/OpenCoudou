package li.cil.oc.common.block

import li.cil.oc.common.blockentity.CaseBlockEntity
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.redstone.Orientation
import net.minecraft.world.phys.BlockHitResult

class CaseBlock(private val tier: Int, properties: Properties) : Block(properties), EntityBlock {

    companion object {
        val FACING = HorizontalDirectionalBlock.FACING
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

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return CaseBlockEntity(pos, state).also { it.tier = tier }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : BlockEntity> getTicker(
        level: Level, state: BlockState, type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        if (type != ModBlockEntities.CASE.get()) return null
        return BlockEntityTicker<T> { lvl, pos, st, be ->
            CaseBlockEntity.tick(lvl, pos, st, be as CaseBlockEntity)
        }
    }

    override fun useWithoutItem(
        state: BlockState, level: Level, pos: BlockPos,
        player: Player, hitResult: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS
        }
        if (player is ServerPlayer) {
            val be = level.getBlockEntity(pos) as? CaseBlockEntity
            if (be != null) {
                player.openMenu(be) { buf -> buf.writeVarInt(be.tier) }
            }
        }
        return InteractionResult.CONSUME
    }
    
    @Suppress("OVERRIDE_DEPRECATION")
    override fun neighborChanged(
        state: BlockState, level: Level, pos: BlockPos,
        block: Block, orientation: Orientation?, isMoving: Boolean
    ) {
        super.neighborChanged(state, level, pos, block, orientation, isMoving)
        if (!level.isClientSide) {
            val be = level.getBlockEntity(pos) as? CaseBlockEntity
            be?.onRedstoneNeighborChanged()
        }
    }

    override fun onRemove(
        state: BlockState, level: Level, pos: BlockPos,
        newState: BlockState, isMoving: Boolean
    ) {
        if (!state.`is`(newState.block)) {
            val be = level.getBlockEntity(pos) as? CaseBlockEntity
            be?.shutdown()
            be?.dropContents(level, pos)
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }
}
