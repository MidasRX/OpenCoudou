package li.cil.oc.common.container

import li.cil.oc.common.init.ModMenus
import li.cil.oc.server.entity.DroneEntity
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemStackHandler
import net.neoforged.neoforge.items.SlotItemHandler

/**
 * Drone container menu - displays drone's 8 inventory slots with power controls.
 */
class DroneMenu(
    containerId: Int,
    private val playerInventory: Inventory,
    private val droneInventory: IItemHandler,
    private val containerData: ContainerData,
    private val drone: DroneEntity?
) : AbstractContainerMenu(ModMenus.DRONE.get(), containerId) {

    val running: Boolean get() = containerData.get(0) != 0
    val energy: Int get() = containerData.get(1)
    val maxEnergy: Int get() = containerData.get(2)
    val tier: Int get() = containerData.get(3)
    
    init {
        addDataSlots(containerData)
        
        // Drone inventory slots (8 slots, 4x2)
        for (row in 0 until 2) {
            for (col in 0 until 4) {
                val slot = row * 4 + col
                addSlot(SlotItemHandler(droneInventory, slot, 53 + col * 18, 26 + row * 18))
            }
        }
        
        // Player inventory (3 rows of 9)
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        
        // Player hotbar
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 142))
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var result = ItemStack.EMPTY
        val slot = slots.getOrNull(index) ?: return result
        
        if (slot.hasItem()) {
            val stack = slot.item
            result = stack.copy()
            
            if (index < 8) {
                // Move from drone to player
                if (!moveItemStackTo(stack, 8, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else {
                // Move from player to drone
                if (!moveItemStackTo(stack, 0, 8, false)) {
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

    override fun stillValid(player: Player): Boolean {
        return drone?.isAlive == true && player.distanceToSqr(drone) <= 64.0
    }

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        when (id) {
            0 -> {
                // Power toggle
                drone?.let {
                    if (it.isRunning()) {
                        it.stop()
                    } else {
                        val result = it.start()
                        if (!result && player is ServerPlayer) {
                            player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable("oc.drone.no_energy"),
                                true
                            )
                        }
                    }
                }
                return true
            }
        }
        return super.clickMenuButton(player, id)
    }
    
    companion object {
        /**
         * Factory for creating menu from network data.
         */
        fun fromNetwork(containerId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): DroneMenu {
            val entityId = buf.readVarInt()
            val droneInventory = ItemStackHandler(8)
            val containerData = SimpleContainerData(4)
            
            val level = playerInventory.player.level()
            val drone = level.getEntity(entityId) as? DroneEntity
            
            return DroneMenu(containerId, playerInventory, droneInventory, containerData, drone)
        }
    }
}
