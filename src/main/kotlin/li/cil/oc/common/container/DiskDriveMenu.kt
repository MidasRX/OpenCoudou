package li.cil.oc.common.container

import li.cil.oc.common.init.ModMenus
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemStackHandler
import net.neoforged.neoforge.items.SlotItemHandler

class DiskDriveMenu(
    containerId: Int,
    private val playerInventory: Inventory,
    private val levelAccess: ContainerLevelAccess,
    private val driveInventory: IItemHandler
) : AbstractContainerMenu(ModMenus.DISK_DRIVE.get(), containerId) {

    companion object {
        const val SLOT_COUNT = 1
        
        fun fromNetwork(containerId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): DiskDriveMenu {
            return DiskDriveMenu(
                containerId,
                playerInventory,
                ContainerLevelAccess.NULL,
                ItemStackHandler(SLOT_COUNT)
            )
        }
    }

    init {
        // Floppy slot
        addSlot(FloppySlot(driveInventory, 0, 80, 35))

        // Player inventory
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18))
            }
        }
        // Hotbar
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
            when {
                index < SLOT_COUNT -> {
                    if (!moveItemStackTo(stack, SLOT_COUNT, slots.size, true)) return ItemStack.EMPTY
                }
                else -> {
                    if (!moveItemStackTo(stack, 0, SLOT_COUNT, false)) return ItemStack.EMPTY
                }
            }
            if (stack.isEmpty) slot.set(ItemStack.EMPTY) else slot.setChanged()
        }
        return result
    }

    override fun stillValid(player: Player): Boolean {
        return levelAccess.evaluate({ level, pos ->
            player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)
    }

    private class FloppySlot(handler: IItemHandler, index: Int, x: Int, y: Int) : SlotItemHandler(handler, index, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean {
            val item = stack.item
            return item.descriptionId.contains("floppy") || item.descriptionId.contains("disk")
        }
        
        override fun getMaxStackSize(): Int = 1
    }
}
