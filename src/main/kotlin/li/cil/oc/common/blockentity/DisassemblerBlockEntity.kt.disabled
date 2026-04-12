package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.component.DisassemblerComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.items.ItemStackHandler

/**
 * Disassembler block entity - breaks down robots and other crafted items into components.
 */
class DisassemblerBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.DISASSEMBLER.get(), pos, state), Container {
    
    private val disassembler = DisassemblerComponent()
    
    // Input slot and output slots
    private val inventory = ItemStackHandler(16) // 1 input + 15 output
    
    // Disassembly state
    private var disassembling = false
    private var progress = 0
    private val disassemblyTime = 100 // 5 seconds in ticks
    
    // Energy
    private var energy = 0
    private val maxEnergy = 50000
    private val energyPerTick = 10
    
    fun getComponent(): DisassemblerComponent = disassembler
    
    fun tick() {
        if (level?.isClientSide == true) return
        
        if (disassembling) {
            if (energy >= energyPerTick) {
                energy -= energyPerTick
                progress++
                if (progress >= disassemblyTime) {
                    finishDisassembly()
                }
                setChanged()
            }
        } else if (!inventory.getStackInSlot(0).isEmpty) {
            // Auto-start disassembly if input has item
            startDisassembly()
        }
    }
    
    fun startDisassembly(): Boolean {
        if (disassembling) return false
        
        val input = inventory.getStackInSlot(0)
        if (input.isEmpty) return false
        
        // Check if item can be disassembled
        if (!canDisassemble(input)) return false
        
        disassembling = true
        progress = 0
        setChanged()
        return true
    }
    
    private fun canDisassemble(stack: ItemStack): Boolean {
        // Would check if item is a robot, tablet, drone, or other assembled device
        // For now, accept any item with complex components
        return !stack.isEmpty
    }
    
    private fun finishDisassembly() {
        val input = inventory.getStackInSlot(0)
        if (!input.isEmpty) {
            // Get components from the item
            val components = getComponents(input)
            
            // Remove input
            inventory.extractItem(0, 1, false)
            
            // Add components to output slots
            var slot = 1
            for (component in components) {
                if (slot >= inventory.slots) break
                val remainder = inventory.insertItem(slot, component, false)
                if (remainder.isEmpty) {
                    slot++
                } else {
                    // Would drop remainder or cancel
                }
            }
        }
        
        disassembling = false
        progress = 0
        setChanged()
    }
    
    private fun getComponents(stack: ItemStack): List<ItemStack> {
        // Would return list of component items from the assembled device
        // For now, return empty list
        return emptyList()
    }
    
    fun getProgress(): Float {
        if (!disassembling) return 0f
        return progress.toFloat() / disassemblyTime.toFloat()
    }
    
    fun isDisassembling(): Boolean = disassembling
    
    // Container implementation
    override fun getContainerSize(): Int = inventory.slots
    
    override fun isEmpty(): Boolean {
        for (i in 0 until inventory.slots) {
            if (!inventory.getStackInSlot(i).isEmpty) return false
        }
        return true
    }
    
    override fun getItem(slot: Int): ItemStack = inventory.getStackInSlot(slot)
    
    override fun removeItem(slot: Int, amount: Int): ItemStack = inventory.extractItem(slot, amount, false)
    
    override fun removeItemNoUpdate(slot: Int): ItemStack {
        val stack = inventory.getStackInSlot(slot)
        inventory.setStackInSlot(slot, ItemStack.EMPTY)
        return stack
    }
    
    override fun setItem(slot: Int, stack: ItemStack) {
        inventory.setStackInSlot(slot, stack)
    }
    
    override fun stillValid(player: Player): Boolean {
        return player.distanceToSqr(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5) <= 64.0
    }
    
    override fun clearContent() {
        for (i in 0 until inventory.slots) {
            inventory.setStackInSlot(i, ItemStack.EMPTY)
        }
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putBoolean("disassembling", disassembling)
        tag.putInt("progress", progress)
        tag.putInt("energy", energy)
        tag.put("inventory", inventory.serializeNBT(registries))
        tag.putString("disassembler_address", disassembler.address)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        disassembling = tag.getBoolean("disassembling")
        progress = tag.getInt("progress")
        energy = tag.getInt("energy")
        inventory.deserializeNBT(registries, tag.getCompound("inventory"))
        // Address is generated, don't reload
    }
}
