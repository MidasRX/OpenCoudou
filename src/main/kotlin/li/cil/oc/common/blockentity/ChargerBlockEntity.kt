package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.component.ChargerComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage

/**
 * Charger block entity - charges robots, tablets, and other devices.
 */
class ChargerBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.CHARGER.get(), pos, state), IEnergyStorage {
    
    private val charger = ChargerComponent()
    
    // Internal energy storage
    private var energy: Int = 0
    private val maxEnergy: Int = 100000
    private val chargeRate: Int = 500
    
    fun getComponent(): ChargerComponent = charger
    
    fun tick() {
        if (level?.isClientSide == true) return
        
        // Charge adjacent entities (robots, tablets, etc.)
        chargeAdjacentEntities()
    }
    
    private fun chargeAdjacentEntities() {
        val world = level ?: return
        
        // Check all adjacent positions for chargeable entities
        for (offset in listOf(
            BlockPos(0, 1, 0), // Above
            BlockPos(0, -1, 0), // Below
            BlockPos(1, 0, 0), BlockPos(-1, 0, 0),
            BlockPos(0, 0, 1), BlockPos(0, 0, -1)
        )) {
            val targetPos = blockPos.offset(offset)
            
            // Check for block entities with energy capability
            val blockEntity = world.getBlockEntity(targetPos)
            if (blockEntity != null) {
                val energyCap = world.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, null)
                if (energyCap != null && energyCap.canReceive()) {
                    val toTransfer = minOf(chargeRate, energy, energyCap.maxEnergyStored - energyCap.energyStored)
                    if (toTransfer > 0) {
                        val received = energyCap.receiveEnergy(toTransfer, false)
                        energy -= received
                    }
                }
            }
        }
    }
    
    // IEnergyStorage implementation
    override fun receiveEnergy(toReceive: Int, simulate: Boolean): Int {
        val canReceive = minOf(toReceive, maxEnergy - energy)
        if (!simulate) {
            energy += canReceive
        }
        return canReceive
    }
    
    override fun extractEnergy(toExtract: Int, simulate: Boolean): Int = 0
    
    override fun getEnergyStored(): Int = energy
    
    override fun getMaxEnergyStored(): Int = maxEnergy
    
    override fun canExtract(): Boolean = false
    
    override fun canReceive(): Boolean = true
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putInt("energy", energy)
        tag.putString("charger_address", charger.address)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        energy = tag.getInt("energy")
        // Address is generated, don't reload
    }
}
