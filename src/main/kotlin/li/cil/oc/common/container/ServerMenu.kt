package li.cil.oc.common.container

import li.cil.oc.common.init.ModMenus
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemStackHandler
import net.neoforged.neoforge.items.SlotItemHandler

/**
 * Server container menu - displays server item components.
 * Servers are items that mount in racks and contain CPUs, RAM, HDDs.
 */
class ServerMenu(
    containerId: Int,
    private val playerInventory: Inventory,
    private val serverInventory: IItemHandler,
    val tier: Int,
    private val heldSlot: Int // Player's held slot with the server item
) : AbstractContainerMenu(ModMenus.SERVER.get(), containerId) {

    private val componentSlotCount: Int
    
    init {
        // Server component slots based on tier
        // Tier 1: 4 slots (1 CPU, 2 RAM, 1 HDD)
        // Tier 2: 6 slots (1 CPU, 4 RAM, 1 HDD)
        // Tier 3: 8 slots (1 CPU, 4 RAM, 2 HDD, 1 component bus)
        componentSlotCount = serverInventory.slots
        
        // Position component slots in a 2-column layout
        val startX = 62
        val startY = 17
        
        for (i in 0 until componentSlotCount) {
            val row = i / 2
            val col = i % 2
            addSlot(SlotItemHandler(serverInventory, i, startX + col * 36, startY + row * 18))
        }
        
        // Calculate player inventory position
        val playerInvY = 84
        
        // Player inventory (3 rows of 9)
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                val slotIndex = col + row * 9 + 9
                val slot = if (slotIndex == heldSlot) {
                    // Lock the slot containing the server item
                    LockedSlot(playerInventory, slotIndex, 8 + col * 18, playerInvY + row * 18)
                } else {
                    Slot(playerInventory, slotIndex, 8 + col * 18, playerInvY + row * 18)
                }
                addSlot(slot)
            }
        }
        
        // Player hotbar
        for (col in 0 until 9) {
            val slot = if (col == heldSlot) {
                LockedSlot(playerInventory, col, 8 + col * 18, playerInvY + 58)
            } else {
                Slot(playerInventory, col, 8 + col * 18, playerInvY + 58)
            }
            addSlot(slot)
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var result = ItemStack.EMPTY
        val slot = slots.getOrNull(index) ?: return result
        
        if (slot.hasItem() && slot !is LockedSlot) {
            val stack = slot.item
            result = stack.copy()
            
            if (index < componentSlotCount) {
                // Move from server to player
                if (!moveItemStackTo(stack, componentSlotCount, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else {
                // Move from player to server
                if (!moveItemStackTo(stack, 0, componentSlotCount, false)) {
                    return ItemStack.EMPTY
                }
            }
            
            if (stack.isEmpty) {
                slot.set(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }
        }
        
        return result
    }

    override fun stillValid(player: Player): Boolean = true
    
    /**
     * Locked slot that prevents item removal - used for the server item itself.
     */
    class LockedSlot(inventory: net.minecraft.world.Container, index: Int, x: Int, y: Int) : Slot(inventory, index, x, y) {
        override fun mayPickup(player: Player): Boolean = false
        override fun mayPlace(stack: ItemStack): Boolean = false
    }
    
    companion object {
        /**
         * Factory for creating menu from network data.
         */
        fun fromNetwork(containerId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): ServerMenu {
            val tier = buf.readVarInt()
            val heldSlot = buf.readVarInt()
            val slotCount = when (tier) {
                1 -> 4
                2 -> 6
                else -> 8
            }
            val serverInventory = ItemStackHandler(slotCount)
            
            return ServerMenu(containerId, playerInventory, serverInventory, tier, heldSlot)
        }
    }
}
