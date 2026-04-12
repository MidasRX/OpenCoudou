package li.cil.oc.common.block

import li.cil.oc.common.blockentity.CarpetedCapacitorBlockEntity
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

/**
 * Carpeted Capacitor - a capacitor with wool carpet on top.
 * Generates power when sheep or cats stand on it (static electricity).
 * The wool top catches static from their fur as they move.
 */
class CarpetedCapacitorBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return CarpetedCapacitorBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        if (type != ModBlockEntities.CARPETED_CAPACITOR.get()) return null
        
        return BlockEntityTicker { lvl, pos, st, blockEntity ->
            if (blockEntity is CarpetedCapacitorBlockEntity) {
                blockEntity.tick()
            }
        }
    }
}
