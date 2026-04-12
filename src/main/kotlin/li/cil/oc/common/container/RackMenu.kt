package li.cil.oc.common.container

import li.cil.oc.common.blockentity.ServerRackBlockEntity
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
 * Menu for the Server Rack block.
 * The rack has 4 slots for mounting server blades and other components.
 */
class RackMenu(
    containerId: Int,
    playerInventory: Inventory,
    private val rack: Container,
    private val access: ContainerLevelAccess
) : AbstractContainerMenu(ModMenus.RACK.get(), containerId) {
    
    // Track which mountables have network connections
    val nodePresence = Array(4) { BooleanArray(4) { false } }
    var isRelayEnabled = false
    
    constructor(containerId: Int, playerInventory: Inventory) : this(
        containerId,
        playerInventory,
        SimpleContainer(4),
        ContainerLevelAccess.NULL
    )
    
    init {
        // 4 rack mountable slots
        addSlot(Slot(rack, 0, 20, 23))
        addSlot(Slot(rack, 1, 20, 43))
        addSlot(Slot(rack, 2, 20, 63))
        addSlot(Slot(rack, 3, 20, 83))
        
        // Player inventory
        for (row in 0..2) {
            for (col in 0..8) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 128 + row * 18))
            }
        }
        
        // Hotbar
        for (col in 0..8) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 186))
        }
    }
    
    override fun stillValid(player: Player): Boolean {
        return access.evaluate({ level, pos ->
            if (level.getBlockEntity(pos) is ServerRackBlockEntity) {
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
                // From rack to player
                if (!moveItemStackTo(stack, 4, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else {
                // From player to rack
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
        fun fromNetwork(containerId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): RackMenu {
            return RackMenu(containerId, playerInventory, SimpleContainer(4), ContainerLevelAccess.NULL)
        }
    }
}
