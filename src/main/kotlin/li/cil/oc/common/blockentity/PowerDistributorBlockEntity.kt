package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage

/**
 * Power Distributor block entity that distributes energy between connected capacitors.
 * It balances energy levels across the network.
 */
class PowerDistributorBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(
    ModBlockEntities.POWER_DISTRIBUTOR.get(),
    pos,
    state
) {
    companion object {
        const val TRANSFER_RATE = 1000 // Energy per tick
    }
    
    // Network address for component API
    var address: String = java.util.UUID.randomUUID().toString()
        private set
    
    // Current power flow rate
    var currentFlowRate: Int = 0
        private set
    
    /**
     * Server tick - balance energy between connected capacitors.
     */
    fun tick() {
        if (level?.isClientSide != false) return
        
        val world = level ?: return
        
        // Find all connected energy storages
        val connectedStorages = mutableListOf<IEnergyStorage>()
        for (dir in net.minecraft.core.Direction.entries) {
            val neighborPos = blockPos.relative(dir)
            val storage = world.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, dir.opposite)
            if (storage != null && storage.canReceive() && storage.canExtract()) {
                connectedStorages.add(storage)
            }
        }
        
        if (connectedStorages.isEmpty()) {
            currentFlowRate = 0
            return
        }
        
        // Calculate average energy level
        val totalEnergy = connectedStorages.sumOf { it.energyStored }
        val avgEnergy = totalEnergy / connectedStorages.size
        
        var totalTransferred = 0
        
        // Balance energy
        for (source in connectedStorages) {
            val diff = source.energyStored - avgEnergy
            if (diff > 0) {
                // This storage has more than average, take some
                val toTake = minOf(diff, TRANSFER_RATE / connectedStorages.size)
                val extracted = source.extractEnergy(toTake, false)
                
                // Distribute to storages below average
                for (receiver in connectedStorages) {
                    if (receiver.energyStored < avgEnergy && extracted > 0) {
                        val toGive = minOf(extracted, avgEnergy - receiver.energyStored)
                        val received = receiver.receiveEnergy(toGive, false)
                        totalTransferred += received
                    }
                }
            }
        }
        
        currentFlowRate = totalTransferred
    }
    
    // ========================================
    // NBT Serialization
    // ========================================
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putString("Address", address)
        tag.putInt("FlowRate", currentFlowRate)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        if (tag.contains("Address")) {
            address = tag.getString("Address")
        }
        currentFlowRate = tag.getInt("FlowRate")
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
