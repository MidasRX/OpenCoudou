package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.component.GeolyzerComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * Block entity for the Geolyzer.
 * Scans and analyzes blocks in the surrounding area.
 */
class GeolyzerBlockEntity(pos: BlockPos, state: BlockState) 
    : BlockEntity(ModBlockEntities.GEOLYZER.get(), pos, state) {
    
    val geolyzer = GeolyzerComponent()
    
    fun getComponent(): GeolyzerComponent {
        geolyzer.geolyzerPos = blockPos
        geolyzer.level = level
        return geolyzer
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
    }
    
    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        return super.getUpdateTag(registries)
    }
    
    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this)
}
