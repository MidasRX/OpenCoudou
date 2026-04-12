package li.cil.oc.common.block

import li.cil.oc.common.blockentity.PrintBlockEntity
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * Print block - a 3D printed object from the Printer.
 * 
 * Prints have dynamic shapes based on their configured print data.
 * They can emit light, output redstone, respond to activation, etc.
 */
class PrintBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.MODEL
    }
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return PrintBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (!level.isClientSide && type == ModBlockEntities.PRINT.get()) {
            BlockEntityTicker { _, _, _, entity ->
                (entity as? PrintBlockEntity)?.tick()
            }
        } else null
    }
    
    // ==================== Shape ====================
    
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val be = level.getBlockEntity(pos) as? PrintBlockEntity
        return be?.getVoxelShape() ?: Shapes.block()
    }
    
    override fun getCollisionShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return getShape(state, level, pos, context)
    }
    
    override fun getOcclusionShape(state: BlockState): VoxelShape {
        // Non-occluding since shape is dynamic
        return Shapes.empty()
    }
    
    override fun useShapeForLightOcclusion(state: BlockState): Boolean = false
    
    override fun propagatesSkylightDown(state: BlockState): Boolean = true
    
    // ==================== Light ====================
    
    override fun getLightEmission(state: BlockState, level: BlockGetter, pos: BlockPos): Int {
        val be = level.getBlockEntity(pos) as? PrintBlockEntity
        return be?.lightLevel?.coerceIn(0, 15) ?: 0
    }
    
    // ==================== Redstone ====================
    
    override fun isSignalSource(state: BlockState): Boolean = true
    
    override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
        val be = level.getBlockEntity(pos) as? PrintBlockEntity
        if (be == null || be.redstoneLevel == 0) return 0
        
        // If button is active, output full signal
        if (be.isButton && be.isActive) return 15
        
        return be.redstoneLevel
    }
    
    override fun getDirectSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
        return getSignal(state, level, pos, direction)
    }
    
    // ==================== Interaction ====================
    
    @Suppress("OVERRIDE_DEPRECATION")
    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        val be = level.getBlockEntity(pos) as? PrintBlockEntity ?: return InteractionResult.PASS
        
        // Toggle if redstone activated or button
        if (be.redstoneActivated || be.isButton) {
            if (!level.isClientSide) {
                be.toggle()
                
                // Update neighbors if this affects redstone
                if (be.redstoneLevel > 0 || be.isButton) {
                    level.updateNeighborsAt(pos, this)
                }
            }
            return InteractionResult.SUCCESS
        }
        
        return InteractionResult.PASS
    }
    
    // ==================== Neighbor Changes ====================
    
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
        
        // Check for redstone input changes
        val be = level.getBlockEntity(pos) as? PrintBlockEntity ?: return
        
        if (be.redstoneActivated && !be.isButton) {
            val powered = level.hasNeighborSignal(pos)
            if (powered != be.isActive) {
                be.isActive = powered
                be.setChanged()
            }
        }
    }
    
    // ==================== Block Breaking ====================
    
    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        isMoving: Boolean
    ) {
        if (!state.`is`(newState.block)) {
            // Could drop print data as item here
            // For now, just let it drop as a regular block
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }
    
    // ==================== Miscellaneous ====================
    
    @Suppress("DEPRECATION")
    override fun hasAnalogOutputSignal(state: BlockState): Boolean = true
    
    @Suppress("DEPRECATION")
    override fun getAnalogOutputSignal(state: BlockState, level: Level, pos: BlockPos): Int {
        val be = level.getBlockEntity(pos) as? PrintBlockEntity ?: return 0
        return if (be.isActive) 15 else 0
    }
}
