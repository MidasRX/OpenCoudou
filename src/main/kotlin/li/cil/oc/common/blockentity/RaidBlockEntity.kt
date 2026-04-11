package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.component.RaidComponent
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
 * RAID block entity - provides large filesystem storage across multiple hard drives.
 * Acts as a combined filesystem from up to 3 hard drives.
 */
class RaidBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.RAID.get(), pos, state), Container {
    
    private val raid = RaidComponent()
    
    // 3 slots for hard drives
    private val inventory = ItemStackHandler(3)
    
    // Combined filesystem label
    private var label: String = ""
    
    fun getComponent(): RaidComponent = raid
    
    fun getLabel(): String = label
    
    fun setLabel(newLabel: String) {
        label = newLabel
        setChanged()
    }
    
    fun getInstalledDriveCount(): Int {
        var count = 0
        for (i in 0 until inventory.slots) {
            if (!inventory.getStackInSlot(i).isEmpty) {
                count++
            }
        }
        return count
    }
    
    fun getTotalCapacity(): Long {
        var total = 0L
        for (i in 0 until inventory.slots) {
            val stack = inventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                // Would get capacity from hard drive item
                // For now, assume 1MB per drive tier
                total += 1024L * 1024L
            }
        }
        return total
    }
    
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
        tag.put("inventory", inventory.serializeNBT(registries))
        tag.putString("label", label)
        tag.putString("raid_address", raid.address)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        inventory.deserializeNBT(registries, tag.getCompound("inventory"))
        label = tag.getString("label")
    }
}
