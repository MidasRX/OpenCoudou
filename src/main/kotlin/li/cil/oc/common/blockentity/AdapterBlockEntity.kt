package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.component.AdapterComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * Block entity for adapter - interfaces with adjacent non-OC blocks.
 */
class AdapterBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.ADAPTER.get(), pos, state) {
    
    private val adapterComponent = AdapterComponent()
    
    // Cache of connected block positions
    private val connectedBlocks = mutableMapOf<Direction, BlockPos>()
    
    fun scanNeighbors() {
        val level = level ?: return
        
        connectedBlocks.clear()
        
        Direction.entries.forEach { dir ->
            val neighborPos = blockPos.relative(dir)
            val neighborState = level.getBlockState(neighborPos)
            
            // Skip air blocks
            if (!neighborState.isAir) {
                connectedBlocks[dir] = neighborPos
            }
        }
        
        // Update component with level and position
        adapterComponent.setWorld(level, blockPos)
        
        setChanged()
    }
    
    fun getComponent(): AdapterComponent {
        level?.let { adapterComponent.setWorld(it, blockPos) }
        return adapterComponent
    }
    
    override fun setLevel(level: Level) {
        super.setLevel(level)
        adapterComponent.setWorld(level, blockPos)
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putString("adapter_address", adapterComponent.address)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        if (tag.contains("adapter_address")) {
            // Restore address if needed
        }
    }
}
