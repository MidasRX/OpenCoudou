package li.cil.oc.common.block

import li.cil.oc.common.blockentity.RobotProxyBlockEntity
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

/**
 * Robot Proxy block - a placeholder block that represents a robot's position in the world.
 * When a robot is placed, this block is created at that position. It delegates most
 * functionality to the underlying robot entity/data.
 */
class RobotProxyBlock(properties: Properties) : Block(properties), EntityBlock {
    
    init {
        // Not in creative menu - robots are placed via the assembled robot item
    }
    
    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return RobotProxyBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        if (type != ModBlockEntities.ROBOT_PROXY.get()) return null
        
        return BlockEntityTicker { lvl, pos, st, blockEntity ->
            if (blockEntity is RobotProxyBlockEntity) {
                blockEntity.tick()
            }
        }
    }
    
    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hit: BlockHitResult
    ): InteractionResult {
        val be = level.getBlockEntity(pos)
        if (be is RobotProxyBlockEntity) {
            // Open robot inventory GUI
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }
    
    // Prevent light blocking
    override fun propagatesSkylightDown(state: BlockState): Boolean = true
}
