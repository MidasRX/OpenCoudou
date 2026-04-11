package li.cil.oc.common.container

import li.cil.oc.common.init.ModMenus
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemStackHandler
import net.neoforged.neoforge.items.SlotItemHandler

class PrinterMenu(
    containerId: Int,
    private val playerInventory: Inventory,
    private val levelAccess: ContainerLevelAccess,
    private val printerInventory: IItemHandler,
    private val data: ContainerData
) : AbstractContainerMenu(ModMenus.PRINTER.get(), containerId) {

    companion object {
        const val SLOT_COUNT = 3
        const val CHAMELIUM_SLOT = 0
        const val INK_SLOT = 1
        const val OUTPUT_SLOT = 2
        
        const val DATA_CHAMELIUM = 0
        const val DATA_INK = 1
        const val DATA_PROGRESS = 2
        const val DATA_MAX_CHAMELIUM = 3
        const val DATA_MAX_INK = 4
        
        fun fromNetwork(containerId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): PrinterMenu {
            return PrinterMenu(
                containerId,
                playerInventory,
                ContainerLevelAccess.NULL,
                ItemStackHandler(SLOT_COUNT),
                SimpleContainerData(5)
            )
        }
    }

    init {
        // Chamelium input slot
        addSlot(ChameliumSlot(printerInventory, CHAMELIUM_SLOT, 26, 22))
        // Ink input slot (dye)
        addSlot(InkSlot(printerInventory, INK_SLOT, 26, 48))
        // Output slot
        addSlot(OutputSlot(printerInventory, OUTPUT_SLOT, 134, 35))

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

        addDataSlots(data)
    }

    val chamelium: Int get() = data.get(DATA_CHAMELIUM)
    val ink: Int get() = data.get(DATA_INK)
    val progress: Int get() = data.get(DATA_PROGRESS)
    val maxChamelium: Int get() = data.get(DATA_MAX_CHAMELIUM)
    val maxInk: Int get() = data.get(DATA_MAX_INK)

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
                    // Try chamelium slot first, then ink slot
                    if (!moveItemStackTo(stack, CHAMELIUM_SLOT, CHAMELIUM_SLOT + 1, false)) {
                        if (!moveItemStackTo(stack, INK_SLOT, INK_SLOT + 1, false)) {
                            return ItemStack.EMPTY
                        }
                    }
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

    private class ChameliumSlot(handler: IItemHandler, index: Int, x: Int, y: Int) : SlotItemHandler(handler, index, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean {
            return stack.item.descriptionId.contains("chamelium")
        }
    }
    
    private class InkSlot(handler: IItemHandler, index: Int, x: Int, y: Int) : SlotItemHandler(handler, index, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean {
            // Accept dyes
            return stack.item.descriptionId.contains("dye")
        }
    }
    
    private class OutputSlot(handler: IItemHandler, index: Int, x: Int, y: Int) : SlotItemHandler(handler, index, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean = false
    }
}
