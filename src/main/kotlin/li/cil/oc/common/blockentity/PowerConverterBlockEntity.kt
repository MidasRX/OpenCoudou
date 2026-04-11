package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.component.PowerConverterComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.energy.IEnergyStorage

/**
 * Power Converter block entity - converts Forge Energy to OC energy and vice versa.
 * Provides compatibility with other energy mods.
 */
class PowerConverterBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.POWER_CONVERTER.get(), pos, state), IEnergyStorage {
    
    private val powerConverter = PowerConverterComponent()
    
    // Internal energy buffer
    private var energy = 0
    private val maxEnergy = 100000
    
    // Conversion rate: 1 FE = 10 OC energy (example)
    private val conversionRate = 10
    
    // Transfer rates
    private val maxReceive = 1000
    private val maxExtract = 1000
    
    fun getComponent(): PowerConverterComponent = powerConverter
    
    fun tick() {
        if (level?.isClientSide == true) return
        
        // Distribute OC energy to adjacent OC components
        distributeEnergy()
    }
    
    private fun distributeEnergy() {
        if (energy <= 0) return
        
        for (direction in Direction.values()) {
            val targetPos = blockPos.relative(direction)
            val entity = level?.getBlockEntity(targetPos)
            
            // Would check for OC energy consumers and transfer energy
            // For now, placeholder
        }
    }
    
    fun getOCEnergy(): Int = energy * conversionRate
    
    fun extractOCEnergy(amount: Int): Int {
        val ocAmount = amount
        val feAmount = ocAmount / conversionRate
        val extracted = minOf(feAmount, energy)
        energy -= extracted
        setChanged()
        return extracted * conversionRate
    }
    
    // IEnergyStorage implementation
    override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int {
        val received = minOf(maxReceive, this.maxReceive, maxEnergy - energy)
        if (!simulate && received > 0) {
            energy += received
            setChanged()
        }
        return received
    }
    
    override fun extractEnergy(maxExtract: Int, simulate: Boolean): Int {
        val extracted = minOf(maxExtract, this.maxExtract, energy)
        if (!simulate && extracted > 0) {
            energy -= extracted
            setChanged()
        }
        return extracted
    }
    
    override fun getEnergyStored(): Int = energy
    
    override fun getMaxEnergyStored(): Int = maxEnergy
    
    override fun canExtract(): Boolean = true
    
    override fun canReceive(): Boolean = true
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putInt("energy", energy)
        tag.putString("power_converter_address", powerConverter.address)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        energy = tag.getInt("energy")
    }
}
