package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.component.AccessPointComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * Access Point block entity - combines relay functionality with wireless network access.
 * Provides wireless network access for computers with wireless network cards.
 */
class AccessPointBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.ACCESS_POINT.get(), pos, state) {
    
    private val accessPoint = AccessPointComponent()
    
    // Wireless signal strength (blocks)
    private var strength = 400
    
    // Maximum concurrent connections
    private val maxConnections = 16
    private var connectedClients = mutableSetOf<String>()
    
    // Per-side connection state
    private var sides = BooleanArray(6) { true }
    
    // Packet rate limiting
    private val maxPacketsPerTick = 10
    private var packetsThisTick = 0
    
    fun getComponent(): AccessPointComponent = accessPoint
    
    fun tick() {
        packetsThisTick = 0
    }
    
    fun getWirelessStrength(): Int = strength
    
    fun setWirelessStrength(newStrength: Int) {
        strength = newStrength.coerceIn(0, 400)
        setChanged()
    }
    
    fun isClientConnected(address: String): Boolean = connectedClients.contains(address)
    
    fun connectClient(address: String): Boolean {
        if (connectedClients.size >= maxConnections) return false
        connectedClients.add(address)
        setChanged()
        return true
    }
    
    fun disconnectClient(address: String) {
        connectedClients.remove(address)
        setChanged()
    }
    
    fun getConnectedCount(): Int = connectedClients.size
    
    fun isSideOpen(side: Direction): Boolean = sides[side.ordinal]
    
    fun setSideOpen(side: Direction, open: Boolean) {
        sides[side.ordinal] = open
        setChanged()
    }
    
    fun toggleSide(side: Direction) {
        sides[side.ordinal] = !sides[side.ordinal]
        setChanged()
    }
    
    fun sendWirelessPacket(packet: Any): Boolean {
        if (packetsThisTick >= maxPacketsPerTick) return false
        packetsThisTick++
        
        // Would broadcast packet to all wireless clients in range
        return true
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putInt("strength", strength)
        val sidesTag = ByteArray(6) { if (sides[it]) 1 else 0 }
        tag.putByteArray("sides", sidesTag)
        tag.putString("access_point_address", accessPoint.address)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        strength = tag.getInt("strength")
        if (tag.contains("sides")) {
            val sidesTag = tag.getByteArray("sides")
            for (i in sidesTag.indices.take(6)) {
                sides[i] = sidesTag[i] != 0.toByte()
            }
        }
    }
}
