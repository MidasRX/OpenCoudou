package li.cil.oc.common.block

import li.cil.oc.common.blockentity.DiskDriveBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.Containers
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.BlockHitResult

/**
 * Disk drive block for inserting floppy disks.
 * Provides a filesystem component to connected computers.
 */
class DiskDriveBlock(properties: Properties) : Block(properties), EntityBlock {
    
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
        return DiskDriveBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return null // No ticking needed
    }
    
    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) return InteractionResult.SUCCESS
        
        val be = level.getBlockEntity(pos) as? DiskDriveBlockEntity ?: return InteractionResult.FAIL
        
        val heldItem = player.mainHandItem
        val currentDisk = be.getDisk()
        
        // If drive has a disk, eject it first
        if (!currentDisk.isEmpty) {
            be.setDisk(net.minecraft.world.item.ItemStack.EMPTY)
            li.cil.oc.common.Sound.playDiskEject(level, pos)
            if (!player.addItem(currentDisk)) {
                Containers.dropItemStack(level, pos.x + 0.5, pos.y + 1.0, pos.z + 0.5, currentDisk)
            }
            return InteractionResult.SUCCESS
        }
        
        // If player has a floppy, insert it
        if (!heldItem.isEmpty && be.canInsert(heldItem)) {
            val inserted = heldItem.copyWithCount(1)
            heldItem.shrink(1)
            be.setDisk(inserted)
            li.cil.oc.common.Sound.playDiskInsert(level, pos)
            return InteractionResult.SUCCESS
        }
        
        return InteractionResult.PASS
    }
    
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        if (!state.`is`(newState.block)) {
            val be = level.getBlockEntity(pos) as? DiskDriveBlockEntity
            be?.dropContents(level, pos)
        }
        super.onRemove(state, level, pos, newState, movedByPiston)
    }
}
