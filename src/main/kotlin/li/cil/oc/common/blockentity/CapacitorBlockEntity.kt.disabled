package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage

/**
 * Capacitor block entity - stores energy for the network.
 * Multiple capacitors can be placed adjacent to increase total storage.
 */
class CapacitorBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.CAPACITOR.get(), pos, state), IEnergyStorage {
    
    // Internal energy storage
    private var energy: Int = 0
    private val maxEnergy: Int = 100000
    private val transferRate: Int = 1000
    
    fun tick() {
        if (level?.isClientSide == true) return
        
        // Distribute energy to adjacent blocks that can receive
        distributeEnergy()
    }
    
    private fun distributeEnergy() {
        val world = level ?: return
        if (energy <= 0) return
        
        // Check all adjacent positions
        for (dir in Direction.entries) {
            if (energy <= 0) break
            
            val targetPos = blockPos.relative(dir)
            val energyCap = world.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, dir.opposite)
            
            if (energyCap != null && energyCap.canReceive() && energyCap !is CapacitorBlockEntity) {
                // Don't transfer to other capacitors - let the network self-balance
                val toTransfer = minOf(transferRate, energy, energyCap.maxEnergyStored - energyCap.energyStored)
                if (toTransfer > 0) {
                    val received = energyCap.receiveEnergy(toTransfer, false)
                    energy -= received
                    if (received > 0) setChanged()
                }
            }
        }
    }
    
    // IEnergyStorage implementation
    override fun receiveEnergy(toReceive: Int, simulate: Boolean): Int {
        val canReceive = minOf(toReceive, transferRate, maxEnergy - energy)
        if (!simulate && canReceive > 0) {
            energy += canReceive
            setChanged()
        }
        return canReceive
    }
    
    override fun extractEnergy(toExtract: Int, simulate: Boolean): Int {
        val canExtract = minOf(toExtract, transferRate, energy)
        if (!simulate && canExtract > 0) {
            energy -= canExtract
            setChanged()
        }
        return canExtract
    }
    
    override fun getEnergyStored(): Int = energy
    
    override fun getMaxEnergyStored(): Int = maxEnergy
    
    override fun canExtract(): Boolean = true
    
    override fun canReceive(): Boolean = true
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putInt("energy", energy)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        energy = tag.getInt("energy")
    }
}
