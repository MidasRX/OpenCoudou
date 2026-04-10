package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.component.PrinterComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * Block entity for 3D printer - holds ink, chamelium, and printed output.
 */
class PrinterBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.PRINTER.get(), pos, state), Container {
    
    private val printerComponent = PrinterComponent()
    
    // Inventory: 0 = ink input, 1 = chamelium input, 2 = output
    private val inventory = NonNullList.withSize(3, ItemStack.EMPTY)
    
    fun getComponent(): PrinterComponent = printerComponent
    
    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        // Process materials from inventory
        processInk()
        processChamelium()
    }
    
    private fun processInk() {
        val inkSlot = inventory[0]
        if (!inkSlot.isEmpty) {
            // Check for ink cartridge items (black dye as placeholder)
            if (inkSlot.`is`(Items.BLACK_DYE)) {
                val added = printerComponent.addInk(1000)
                if (added > 0) {
                    inkSlot.shrink(1)
                    setChanged()
                }
            }
        }
    }
    
    private fun processChamelium() {
        val chameliumSlot = inventory[1]
        if (!chameliumSlot.isEmpty) {
            // Chamelium items (clay ball as placeholder)
            if (chameliumSlot.`is`(Items.CLAY_BALL)) {
                val added = printerComponent.addChamelium(1000)
                if (added > 0) {
                    chameliumSlot.shrink(1)
                    setChanged()
                }
            }
        }
    }
    
    // Container implementation
    override fun getContainerSize(): Int = 3
    
    override fun isEmpty(): Boolean = inventory.all { it.isEmpty }
    
    override fun getItem(slot: Int): ItemStack {
        return if (slot in 0 until inventory.size) inventory[slot] else ItemStack.EMPTY
    }
    
    override fun removeItem(slot: Int, amount: Int): ItemStack {
        val result = ContainerHelper.removeItem(inventory, slot, amount)
        if (!result.isEmpty) setChanged()
        return result
    }
    
    override fun removeItemNoUpdate(slot: Int): ItemStack {
        return ContainerHelper.takeItem(inventory, slot)
    }
    
    override fun setItem(slot: Int, stack: ItemStack) {
        if (slot in 0 until inventory.size) {
            inventory[slot] = stack
            if (stack.count > maxStackSize) {
                stack.count = maxStackSize
            }
            setChanged()
        }
    }
    
    override fun stillValid(player: Player): Boolean {
        return level?.getBlockEntity(blockPos) === this &&
               player.distanceToSqr(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5) <= 64.0
    }
    
    override fun clearContent() {
        inventory.clear()
        setChanged()
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        ContainerHelper.saveAllItems(tag, inventory, registries)
        tag.putString("printer_address", printerComponent.address)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        inventory.clear()
        ContainerHelper.loadAllItems(tag, inventory, registries)
    }
}
