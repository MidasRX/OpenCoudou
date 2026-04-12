package li.cil.oc.common.container

import li.cil.oc.common.blockentity.AdapterBlockEntity
import li.cil.oc.common.init.ModMenus
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

/**
 * Menu for the Adapter block.
 * The adapter has one slot for upgrades that provide component access.
 */
class AdapterMenu(
    containerId: Int,
    playerInventory: Inventory,
    private val adapter: Container,
    private val access: ContainerLevelAccess
) : AbstractContainerMenu(ModMenus.ADAPTER.get(), containerId) {
    
    constructor(containerId: Int, playerInventory: Inventory) : this(
        containerId,
        playerInventory,
        SimpleContainer(1),
        ContainerLevelAccess.NULL
    )
    
    init {
        // Adapter upgrade slot (centered)
        addSlot(Slot(adapter, 0, 80, 35))
        
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
    }
    
    override fun stillValid(player: Player): Boolean {
        return access.evaluate({ level, pos ->
            if (level.getBlockEntity(pos) is AdapterBlockEntity) {
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
            
            if (index == 0) {
                // From adapter to player
                if (!moveItemStackTo(stack, 1, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else {
                // From player to adapter
                if (!moveItemStackTo(stack, 0, 1, false)) {
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
        fun fromNetwork(containerId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): AdapterMenu {
            return AdapterMenu(containerId, playerInventory, SimpleContainer(1), ContainerLevelAccess.NULL)
        }
    }
}
