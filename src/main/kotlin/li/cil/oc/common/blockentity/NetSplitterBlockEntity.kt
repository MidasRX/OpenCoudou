package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.component.NetSplitterComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * Net Splitter block entity - dynamically connects/disconnects network sides.
 * Unlike relay, can be used to segment networks on-the-fly via redstone control.
 */
class NetSplitterBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.NET_SPLITTER.get(), pos, state) {
    
    private val netSplitter = NetSplitterComponent()
    
    // Per-side open/closed state
    private var sides = BooleanArray(6) { true }
    
    // Whether to invert redstone signal
    private var invertSignal = false
    
    fun getComponent(): NetSplitterComponent = netSplitter
    
    fun tick() {
        if (level?.isClientSide == true) return
        
        // Update connections based on redstone
        updateRedstoneState()
    }
    
    private fun updateRedstoneState() {
        val powered = level?.hasNeighborSignal(blockPos) ?: false
        val shouldBeOpen = if (invertSignal) powered else !powered
        
        // If main signal changes, could toggle all sides or specific behavior
    }
    
    /**
     * Check if a side allows network connections.
     */
    fun isSideOpen(side: Direction): Boolean = sides[side.ordinal]
    
    /**
     * Set whether a side allows connections.
     */
    fun setSideOpen(side: Direction, open: Boolean) {
        if (sides[side.ordinal] != open) {
            sides[side.ordinal] = open
            setChanged()
            onSideChanged(side, open)
        }
    }
    
    /**
     * Toggle a side's connection state.
     */
    fun toggleSide(side: Direction) {
        setSideOpen(side, !isSideOpen(side))
    }
    
    private fun onSideChanged(side: Direction, open: Boolean) {
        // Would notify network of connection change
    }
    
    /**
     * Get open sides count.
     */
    fun getOpenSidesCount(): Int = sides.count { it }
    
    /**
     * Set invert behavior.
     */
    fun setInvertSignal(invert: Boolean) {
        invertSignal = invert
        setChanged()
    }
    
    fun isInvertSignal(): Boolean = invertSignal
    
    /**
     * Get all sides status as a map.
     */
    fun getSidesStatus(): Map<Direction, Boolean> {
        return Direction.values().associateWith { sides[it.ordinal] }
    }
    
    /**
     * Set all sides at once.
     */
    fun setAllSides(open: Boolean) {
        for (i in sides.indices) {
            sides[i] = open
        }
        setChanged()
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        val sidesTag = ByteArray(6) { if (sides[it]) 1 else 0 }
        tag.putByteArray("sides", sidesTag)
        tag.putBoolean("invertSignal", invertSignal)
        tag.putString("net_splitter_address", netSplitter.address)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        if (tag.contains("sides")) {
            val sidesTag = tag.getByteArray("sides")
            for (i in sidesTag.indices.take(6)) {
                sides[i] = sidesTag[i] != 0.toByte()
            }
        }
        invertSignal = tag.getBoolean("invertSignal")
    }
}
