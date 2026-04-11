package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.component.RelayComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * Relay block entity - bridges networks together while maintaining separation.
 * Does not bridge component networks, only passes messages through.
 * Also supports wireless connections via a linked card.
 */
class RelayBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.RELAY.get(), pos, state) {
    
    private val relay = RelayComponent()
    
    // Maximum packet relay rate (packets per tick)
    private val maxPacketsPerTick = 5
    private var packetsThisTick = 0
    
    // Wireless signal strength
    private var strength = 400
    
    // Per-side connection state
    private var sides = BooleanArray(6) { true }
    
    fun getComponent(): RelayComponent = relay
    
    fun tick() {
        // Reset packet counter each tick
        packetsThisTick = 0
    }
    
    fun relayPacket(source: Direction, packet: Any): Boolean {
        if (packetsThisTick >= maxPacketsPerTick) return false
        if (!isSideOpen(source)) return false
        
        packetsThisTick++
        
        // Would relay packet to other connected sides
        for (side in Direction.values()) {
            if (side != source && isSideOpen(side)) {
                sendToSide(side, packet)
            }
        }
        
        return true
    }
    
    private fun sendToSide(side: Direction, packet: Any) {
        val targetPos = blockPos.relative(side)
        val entity = level?.getBlockEntity(targetPos)
        // Would send packet to target entity if it supports network messages
    }
    
    fun isSideOpen(side: Direction): Boolean = sides[side.ordinal]
    
    fun setSideOpen(side: Direction, open: Boolean) {
        sides[side.ordinal] = open
        setChanged()
    }
    
    fun toggleSide(side: Direction) {
        sides[side.ordinal] = !sides[side.ordinal]
        setChanged()
    }
    
    fun getWirelessStrength(): Int = strength
    
    fun setWirelessStrength(newStrength: Int) {
        strength = newStrength.coerceIn(0, 400)
        setChanged()
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        val sidesTag = ByteArray(6) { if (sides[it]) 1 else 0 }
        tag.putByteArray("sides", sidesTag)
        tag.putInt("strength", strength)
        tag.putString("relay_address", relay.address)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        if (tag.contains("sides")) {
            val sidesTag = tag.getByteArray("sides")
            for (i in sidesTag.indices.take(6)) {
                sides[i] = sidesTag[i] != 0.toByte()
            }
        }
        strength = tag.getInt("strength")
    }
}
