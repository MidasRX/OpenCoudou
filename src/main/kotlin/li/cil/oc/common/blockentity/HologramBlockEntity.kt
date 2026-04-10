package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.component.HologramComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * Block entity for the hologram projector.
 * Contains the hologram component and syncs data to clients for rendering.
 */
class HologramBlockEntity(pos: BlockPos, state: BlockState, val tier: Int = 1) 
    : BlockEntity(ModBlockEntities.HOLOGRAM.get(), pos, state) {
    
    val hologram = HologramComponent(tier)
    
    // Track if dirty for sync
    private var needsSync = false
    
    fun getComponent(): HologramComponent = hologram
    
    fun markNeedsSync() {
        needsSync = true
        setChanged()
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putInt("Tier", tier)
        tag.putByteArray("Voxels", hologram.getVoxelData())
        
        val palette = hologram.getPalette()
        tag.putIntArray("Palette", palette.toList().toIntArray())
        
        tag.putDouble("Scale", hologram.getScale())
        
        val (offsetX, offsetY, offsetZ) = hologram.getOffset()
        tag.putDouble("OffsetX", offsetX)
        tag.putDouble("OffsetY", offsetY)
        tag.putDouble("OffsetZ", offsetZ)
        
        val (angle, speeds) = hologram.getRotation()
        tag.putDouble("RotationAngle", angle)
        tag.putDouble("RotationSpeedX", speeds.first)
        tag.putDouble("RotationSpeedY", speeds.second)
        tag.putDouble("RotationSpeedZ", speeds.third)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        
        // Load voxel data
        if (tag.contains("Voxels")) {
            val voxels = tag.getByteArray("Voxels")
            // Would need a method to set voxels directly
        }
        
        // Load palette
        if (tag.contains("Palette")) {
            val palette = tag.getIntArray("Palette")
            palette.forEachIndexed { index, color ->
                // Would call hologram.setPaletteColor internally
            }
        }
    }
    
    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        tag.putInt("Tier", tier)
        tag.putByteArray("Voxels", hologram.getVoxelData())
        
        val palette = hologram.getPalette()
        tag.putIntArray("Palette", palette.toList().toIntArray())
        
        tag.putDouble("Scale", hologram.getScale())
        
        return tag
    }
    
    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this)
}
