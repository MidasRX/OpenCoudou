package li.cil.oc.common.container

import li.cil.oc.common.blockentity.RelayBlockEntity
import li.cil.oc.common.init.ModMenus
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

/**
 * Menu for the Relay block.
 * The relay has upgrade slots and displays packet stats.
 */
class RelayMenu(
    containerId: Int,
    playerInventory: Inventory,
    private val relay: Container,
    private val data: ContainerData,
    private val access: ContainerLevelAccess
) : AbstractContainerMenu(ModMenus.RELAY.get(), containerId) {
    
    constructor(containerId: Int, playerInventory: Inventory) : this(
        containerId,
        playerInventory,
        SimpleContainer(4),
        SimpleContainerData(5), // relayDelay, relayAmount, maxQueueSize, packetsAvg, queueSize
        ContainerLevelAccess.NULL
    )
    
    init {
        // Upgrade slots (on the tab)
        addSlot(Slot(relay, 0, 151, 15))  // CPU
        addSlot(Slot(relay, 1, 151, 34))  // Memory
        addSlot(Slot(relay, 2, 151, 53))  // HDD
        addSlot(Slot(relay, 3, 178, 15))  // Card
        
        // Player inventory
        for (row in 0..2) {
            for (col in 0..8) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        
        // Hotbar
        for (col in 0..8) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }
        
        addDataSlots(data)
    }
    
    // Data accessors
    val relayDelay: Int get() = data.get(0)
    val relayAmount: Int get() = data.get(1)
    val maxQueueSize: Int get() = data.get(2)
    val packetsPerCycleAvg: Int get() = data.get(3)
    val queueSize: Int get() = data.get(4)
    
    override fun stillValid(player: Player): Boolean {
        return access.evaluate({ level, pos ->
            if (level.getBlockEntity(pos) is RelayBlockEntity) {
                player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
            } else false
        }, true)
    }
    
    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var remaining = ItemStack.EMPTY
        val slot = slots.getOrNull(index) ?: return remaining
        
        if (slot.hasItem()) {
            val stack = slot.item
            remaining = stack.copy()
            
            if (index < 4) {
                // From relay to player
                if (!moveItemStackTo(stack, 4, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else {
                // From player to relay
                if (!moveItemStackTo(stack, 0, 4, false)) {
                    return ItemStack.EMPTY
                }
            }
            
            if (stack.isEmpty) {
                slot.set(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }
        }
        
        return remaining
    }
    
    companion object {
        fun fromNetwork(containerId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): RelayMenu {
            return RelayMenu(containerId, playerInventory, SimpleContainer(4), SimpleContainerData(5), ContainerLevelAccess.NULL)
        }
    }
}
