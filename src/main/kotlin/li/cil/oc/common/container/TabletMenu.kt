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
 * Tablet container menu - displays tablet's internal components.
 * Tablets contain a screen, keyboard, and upgrades.
 */
class TabletMenu(
    containerId: Int,
    private val playerInventory: Inventory,
    private val tabletInventory: IItemHandler,
    val tier: Int,
    private val heldSlot: Int
) : AbstractContainerMenu(ModMenus.TABLET.get(), containerId) {

    private val componentSlotCount: Int
    
    init {
        // Tablet component slots based on tier
        // Tier 1: 2 upgrade slots
        // Tier 2: 3 upgrade slots
        // Creative: 9 upgrade slots
        componentSlotCount = tabletInventory.slots
        
        // Position component slots
        val startX = 80
        val startY = 17
        val cols = if (componentSlotCount <= 3) 1 else 3
        
        for (i in 0 until componentSlotCount) {
            val row = i / cols
            val col = i % cols
            addSlot(SlotItemHandler(tabletInventory, i, startX + col * 18, startY + row * 18))
        }
        
        // Player inventory (3 rows of 9)
        val playerInvY = 84
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                val slotIndex = col + row * 9 + 9
                val slot = if (slotIndex == heldSlot) {
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
                if (!moveItemStackTo(stack, componentSlotCount, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else {
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
    
    class LockedSlot(inventory: net.minecraft.world.Container, index: Int, x: Int, y: Int) : Slot(inventory, index, x, y) {
        override fun mayPickup(player: Player): Boolean = false
        override fun mayPlace(stack: ItemStack): Boolean = false
    }
    
    companion object {
        fun fromNetwork(containerId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): TabletMenu {
            val tier = buf.readVarInt()
            val heldSlot = buf.readVarInt()
            val slotCount = when (tier) {
                1 -> 2
                2 -> 3
                else -> 9
            }
            val tabletInventory = ItemStackHandler(slotCount)
            
            return TabletMenu(containerId, playerInventory, tabletInventory, tier, heldSlot)
        }
    }
}
