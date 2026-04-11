package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.component.WaypointComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * Waypoint block entity - provides navigation reference points for robots/drones.
 * Can be detected by navigation upgrade and used for pathfinding.
 */
class WaypointBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.WAYPOINT.get(), pos, state) {
    
    private val waypoint = WaypointComponent()
    
    // Waypoint label for identification
    private var label: String = ""
    
    // Redstone behavior
    private var redstoneEnabled = false
    private var lastRedstoneState = false
    
    // Facing direction (for oriented navigation)
    private var facing: Direction = Direction.NORTH
    
    fun getComponent(): WaypointComponent = waypoint
    
    fun getLabel(): String = label
    
    fun setLabel(newLabel: String) {
        label = newLabel.take(32) // Limit label length
        setChanged()
    }
    
    fun getFacing(): Direction = facing
    
    fun setFacing(direction: Direction) {
        if (direction.axis.isHorizontal) {
            facing = direction
            setChanged()
        }
    }
    
    fun isRedstoneEnabled(): Boolean = redstoneEnabled
    
    fun setRedstoneEnabled(enabled: Boolean) {
        redstoneEnabled = enabled
        setChanged()
    }
    
    fun tick() {
        if (level?.isClientSide == true) return
        
        if (redstoneEnabled) {
            val redstonePowered = level?.hasNeighborSignal(blockPos) ?: false
            if (redstonePowered != lastRedstoneState) {
                lastRedstoneState = redstonePowered
                // Would signal change
            }
        }
    }
    
    /**
     * Check if this waypoint is active (not disabled by redstone).
     */
    fun isActive(): Boolean {
        return if (redstoneEnabled) {
            level?.hasNeighborSignal(blockPos) ?: false
        } else {
            true
        }
    }
    
    /**
     * Get the position as coordinates for navigation.
     */
    fun getPosition(): Triple<Int, Int, Int> {
        return Triple(blockPos.x, blockPos.y, blockPos.z)
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putString("label", label)
        tag.putString("facing", facing.name)
        tag.putBoolean("redstoneEnabled", redstoneEnabled)
        tag.putString("waypoint_address", waypoint.address)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        label = tag.getString("label")
        if (tag.contains("facing")) {
            facing = try {
                Direction.valueOf(tag.getString("facing"))
            } catch (e: Exception) {
                Direction.NORTH
            }
        }
        redstoneEnabled = tag.getBoolean("redstoneEnabled")
    }
}
