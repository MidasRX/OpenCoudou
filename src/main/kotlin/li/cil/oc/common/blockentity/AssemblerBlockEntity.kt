package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.component.AssemblerComponent
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
 * Assembler block entity - assembles robots, tablets, and drones from components.
 */
class AssemblerBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.ASSEMBLER.get(), pos, state), Container {
    
    private val assembler = AssemblerComponent()
    
    // Inventory for components
    private val inventory = ItemStackHandler(21) // 21 slots for components
    
    // Assembly state
    private var assembling = false
    private var progress = 0
    private val assemblyTime = 200 // 10 seconds in ticks
    private var outputStack: ItemStack = ItemStack.EMPTY
    
    fun getComponent(): AssemblerComponent = assembler
    
    fun tick() {
        if (level?.isClientSide == true) return
        
        if (assembling) {
            progress++
            if (progress >= assemblyTime) {
                finishAssembly()
            }
            setChanged()
        }
    }
    
    fun startAssembly(): Boolean {
        if (assembling) return false
        
        // Check if we have a valid recipe
        val result = checkRecipe()
        if (result.isEmpty) return false
        
        assembling = true
        progress = 0
        outputStack = result
        setChanged()
        return true
    }
    
    private fun checkRecipe(): ItemStack {
        // Would check for valid robot/tablet/drone recipe
        // Returns the resulting item if valid, empty otherwise
        return ItemStack.EMPTY
    }
    
    private fun finishAssembly() {
        assembling = false
        progress = 0
        
        // Output would go to output slot or be available for extraction
        // Clear used components
        setChanged()
    }
    
    fun getProgress(): Float {
        if (!assembling) return 0f
        return progress.toFloat() / assemblyTime.toFloat()
    }
    
    fun isAssembling(): Boolean = assembling
    
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
    
    override fun setChanged() {
        super.setChanged()
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putBoolean("assembling", assembling)
        tag.putInt("progress", progress)
        tag.put("inventory", inventory.serializeNBT(registries))
        tag.putString("assembler_address", assembler.address)
        if (!outputStack.isEmpty) {
            tag.put("output", outputStack.save(registries))
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        assembling = tag.getBoolean("assembling")
        progress = tag.getInt("progress")
        inventory.deserializeNBT(registries, tag.getCompound("inventory"))
        // Address is generated, don't reload
        if (tag.contains("output")) {
            outputStack = ItemStack.parseOptional(registries, tag.getCompound("output"))
        }
    }
}
