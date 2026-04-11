package li.cil.oc.common.block

import li.cil.oc.common.blockentity.PowerDistributorBlockEntity
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
 * Power Distributor block that balances energy between connected capacitors.
 */
class PowerDistributorBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return PowerDistributorBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        
        return if (type == ModBlockEntities.POWER_DISTRIBUTOR.get()) {
            BlockEntityTicker { _, _, _, be ->
                if (be is PowerDistributorBlockEntity) {
                    be.tick()
                }
            }
        } else null
    }
}
