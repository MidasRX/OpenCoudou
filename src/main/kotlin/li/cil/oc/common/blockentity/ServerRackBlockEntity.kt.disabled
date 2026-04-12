package li.cil.oc.common.blockentity

import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.common.init.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * Server Rack block entity that can hold multiple server modules.
 * Servers can run independent computers and share components.
 */
class ServerRackBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(
    ModBlockEntities.SERVER_RACK.get(),
    pos,
    state
) {
    companion object {
        const val SLOT_COUNT = 4
        const val MAX_ENERGY = 100000.0
    }
    
    // Server modules installed in each slot (0-3)
    private val servers = arrayOfNulls<ItemStack>(SLOT_COUNT)
    
    // Energy storage
    var energy: Double = 0.0
        private set
    
    // Rack mounting status
    private val mountedSides = BooleanArray(SLOT_COUNT) { false }
    
    // Network address
    var address: String = java.util.UUID.randomUUID().toString()
        private set
    
    /**
     * Mounts a server to the specified slot.
     */
    fun mountServer(slot: Int, server: ItemStack) {
        if (slot !in 0 until SLOT_COUNT) return
        if (!isValidServer(server)) return
        
        servers[slot] = server.copy()
        setChanged()
    }
    
    /**
     * Unmounts a server from the specified slot and returns it.
     */
    fun unmountServer(slot: Int): ItemStack? {
        if (slot !in 0 until SLOT_COUNT) return null
        
        val server = servers[slot] ?: return null
        servers[slot] = null
        setChanged()
        return server
    }
    
    /**
     * Returns the server in the specified slot.
     */
    fun getServer(slot: Int): ItemStack? {
        if (slot !in 0 until SLOT_COUNT) return null
        return servers[slot]
    }
    
    /**
     * Checks if the given item is a valid server module.
     */
    private fun isValidServer(stack: ItemStack): Boolean {
        val item = stack.item
        return item == ModItems.SERVER_TIER1.get() ||
               item == ModItems.SERVER_TIER2.get() ||
               item == ModItems.SERVER_TIER3.get() ||
               item == ModItems.SERVER_CREATIVE.get() ||
               item == ModItems.TERMINAL_SERVER.get()
    }
    
    /**
     * Sets the connection status for a rack side.
     */
    fun setSideConnected(slot: Int, connected: Boolean) {
        if (slot !in 0 until SLOT_COUNT) return
        mountedSides[slot] = connected
        setChanged()
    }
    
    /**
     * Checks if a side is connected.
     */
    fun isSideConnected(slot: Int): Boolean {
        if (slot !in 0 until SLOT_COUNT) return false
        return mountedSides[slot]
    }
    
    /**
     * Adds energy to the rack.
     */
    fun addEnergy(amount: Double): Double {
        val toAdd = minOf(amount, MAX_ENERGY - energy)
        energy += toAdd
        if (toAdd > 0) setChanged()
        return toAdd
    }
    
    /**
     * Tries to use energy from the rack.
     */
    fun useEnergy(amount: Double): Boolean {
        if (energy >= amount) {
            energy -= amount
            setChanged()
            return true
        }
        return false
    }
    
    /**
     * Server tick - update each mounted server.
     */
    fun tick() {
        if (level?.isClientSide != false) return
        
        // Process each server slot
        for (slot in 0 until SLOT_COUNT) {
            val server = servers[slot] ?: continue
            
            // Use energy for running servers
            useEnergy(0.5) // Small energy cost per tick per server
        }
    }
    
    // ========================================
    // NBT Serialization
    // ========================================
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        
        tag.putDouble("Energy", energy)
        tag.putString("Address", address)
        
        // Save servers
        val serverList = ListTag()
        for (i in 0 until SLOT_COUNT) {
            val serverTag = CompoundTag()
            servers[i]?.let { server ->
                server.save(registries, serverTag)
            }
            serverTag.putBoolean("Connected", mountedSides[i])
            serverList.add(serverTag)
        }
        tag.put("Servers", serverList)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        
        energy = tag.getDouble("Energy")
        if (tag.contains("Address")) {
            address = tag.getString("Address")
        }
        
        // Load servers
        if (tag.contains("Servers", Tag.TAG_LIST.toInt())) {
            val serverList = tag.getList("Servers", Tag.TAG_COMPOUND.toInt())
            for (i in 0 until minOf(serverList.size, SLOT_COUNT)) {
                val serverTag = serverList.getCompound(i)
                servers[i] = if (serverTag.isEmpty) null else ItemStack.parseOptional(registries, serverTag)
                mountedSides[i] = serverTag.getBoolean("Connected")
            }
        }
    }
    
    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }
    
    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }
}
