package li.cil.oc.common.container

import li.cil.oc.common.init.ModMenus
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Menu for the Database Upgrade item.
 * Shows a grid of slots for storing item metadata (ghost items).
 * Grid size depends on tier: T1=9 slots (3x3), T2=25 (5x5), T3=81 (9x9).
 */
class DatabaseMenu(
    containerId: Int,
    playerInventory: Inventory,
    private val database: Container,
    val tier: Int
) : AbstractContainerMenu(ModMenus.DATABASE.get(), containerId) {
    
    val rows: Int
    private val offset: Int
    
    constructor(containerId: Int, playerInventory: Inventory) : this(
        containerId,
        playerInventory,
        SimpleContainer(9),
        0
    )
    
    init {
        // Calculate grid size based on slot count
        rows = ceil(sqrt(database.containerSize.toDouble())).toInt()
        
        // Offset to center the grid
        val tierOffset = when (tier) {
            0 -> 3 * 18  // Tier 1: 3x3, more offset
            1 -> 2 * 18  // Tier 2: 5x5, medium offset
            else -> 0   // Tier 3: 9x9, no offset
        }
        offset = 8 + tierOffset
        
        // Database slots in a grid
        for (row in 0 until rows) {
            for (col in 0 until rows) {
                val slotIndex = row * rows + col
                if (slotIndex < database.containerSize) {
                    addSlot(DatabaseSlot(database, slotIndex, offset + col * 18, offset + row * 18))
                }
            }
        }
        
        // Player inventory
        for (row in 0..2) {
            for (col in 0..8) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 174 + row * 18))
            }
        }
        
        // Hotbar
        for (col in 0..8) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 232))
        }
    }
    
    override fun stillValid(player: Player): Boolean = true
    
    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var remaining = ItemStack.EMPTY
        val slot = slots.getOrNull(index) ?: return remaining
        
        if (slot.hasItem()) {
            val stack = slot.item
            remaining = stack.copy()
            
            val databaseSlotCount = rows * rows
            
            if (index < databaseSlotCount) {
                // From database to player
                if (!moveItemStackTo(stack, databaseSlotCount, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else {
                // From player to database (as ghost item - 1 count copy)
                val ghostStack = stack.copy()
                ghostStack.count = 1
                
                // Find empty database slot
                for (i in 0 until databaseSlotCount) {
                    if (!slots[i].hasItem()) {
                        slots[i].set(ghostStack)
                        return remaining
                    }
                }
                return ItemStack.EMPTY
            }
            
            if (stack.isEmpty) {
                slot.set(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }
        }
        
        return remaining
    }
    
    /**
     * Database slot that stores ghost items (1 count copies).
     */
    class DatabaseSlot(
        container: Container,
        slot: Int,
        x: Int,
        y: Int
    ) : Slot(container, slot, x, y) {
        
        override fun mayPlace(stack: ItemStack): Boolean = true
        
        override fun set(stack: ItemStack) {
            // Store ghost items with count 1
            val ghost = if (stack.isEmpty) ItemStack.EMPTY else stack.copy().also { it.count = 1 }
            super.set(ghost)
        }
        
        override fun getMaxStackSize(): Int = 1
        
        override fun getMaxStackSize(stack: ItemStack): Int = 1
    }
    
    companion object {
        fun fromNetwork(containerId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): DatabaseMenu {
            val tier = buf.readVarInt()
            val slotCount = when (tier) {
                0 -> 9    // 3x3
                1 -> 25   // 5x5
                else -> 81 // 9x9
            }
            return DatabaseMenu(containerId, playerInventory, SimpleContainer(slotCount), tier)
        }
    }
}
