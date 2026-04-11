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

class DisassemblerMenu(
    containerId: Int,
    private val playerInventory: Inventory,
    private val levelAccess: ContainerLevelAccess,
    private val disassemblerInventory: IItemHandler,
    private val data: ContainerData
) : AbstractContainerMenu(ModMenus.DISASSEMBLER.get(), containerId) {

    companion object {
        const val SLOT_COUNT = 16
        const val INPUT_SLOT = 0
        const val OUTPUT_START = 1
        const val OUTPUT_COUNT = 15
        
        const val DATA_PROGRESS = 0
        const val DATA_TOTAL = 1
        
        fun fromNetwork(containerId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): DisassemblerMenu {
            return DisassemblerMenu(
                containerId,
                playerInventory,
                ContainerLevelAccess.NULL,
                ItemStackHandler(SLOT_COUNT),
                SimpleContainerData(2)
            )
        }
    }

    init {
        // Input slot
        addSlot(SlotItemHandler(disassemblerInventory, INPUT_SLOT, 26, 35))
        
        // Output slots (3 rows of 5)
        for (row in 0 until 3) {
            for (col in 0 until 5) {
                val slotIndex = OUTPUT_START + row * 5 + col
                addSlot(OutputSlot(disassemblerInventory, slotIndex, 80 + col * 18, 17 + row * 18))
            }
        }

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

    val progress: Int get() = data.get(DATA_PROGRESS)
    val total: Int get() = data.get(DATA_TOTAL)

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var result = ItemStack.EMPTY
        val slot = slots.getOrNull(index) ?: return result
        if (slot.hasItem()) {
            val stack = slot.item
            result = stack.copy()
            when {
                index == INPUT_SLOT -> {
                    if (!moveItemStackTo(stack, SLOT_COUNT, slots.size, true)) return ItemStack.EMPTY
                }
                index in OUTPUT_START until SLOT_COUNT -> {
                    if (!moveItemStackTo(stack, SLOT_COUNT, slots.size, true)) return ItemStack.EMPTY
                }
                else -> {
                    if (!moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) return ItemStack.EMPTY
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

    private class OutputSlot(handler: IItemHandler, index: Int, x: Int, y: Int) : SlotItemHandler(handler, index, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean = false
    }
}
