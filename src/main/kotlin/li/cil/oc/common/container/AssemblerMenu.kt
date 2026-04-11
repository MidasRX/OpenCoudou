package li.cil.oc.common.container

import li.cil.oc.common.blockentity.AssemblerBlockEntity
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

class AssemblerMenu(
    containerId: Int,
    private val playerInventory: Inventory,
    private val levelAccess: ContainerLevelAccess,
    private val assemblerInventory: IItemHandler,
    private val data: ContainerData
) : AbstractContainerMenu(ModMenus.ASSEMBLER.get(), containerId) {

    companion object {
        const val SLOT_COUNT = 21
        const val OUTPUT_SLOT = 20
        
        // Data indices
        const val DATA_PROGRESS = 0
        const val DATA_REQUIRED_ENERGY = 1
        const val DATA_STATE = 2 // 0=idle, 1=assembling, 2=waiting
        
        fun fromNetwork(containerId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): AssemblerMenu {
            return AssemblerMenu(
                containerId,
                playerInventory,
                ContainerLevelAccess.NULL,
                ItemStackHandler(SLOT_COUNT),
                SimpleContainerData(3)
            )
        }
    }

    init {
        // Main assembly input slots (5 rows of 4 columns)
        val startX = 26
        val startY = 16
        for (row in 0 until 5) {
            for (col in 0 until 4) {
                val slotIndex = row * 4 + col
                addSlot(SlotItemHandler(assemblerInventory, slotIndex, startX + col * 18, startY + row * 18))
            }
        }
        // Output slot
        addSlot(OutputSlot(assemblerInventory, OUTPUT_SLOT, 134, 52))

        // Player inventory
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 120 + row * 18))
            }
        }
        // Hotbar
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 178))
        }

        addDataSlots(data)
    }

    val progress: Int get() = data.get(DATA_PROGRESS)
    val requiredEnergy: Int get() = data.get(DATA_REQUIRED_ENERGY)
    val state: AssemblerState get() = AssemblerState.entries.getOrNull(data.get(DATA_STATE)) ?: AssemblerState.IDLE

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
                    if (!moveItemStackTo(stack, 0, OUTPUT_SLOT, false)) return ItemStack.EMPTY
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

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        if (id == 0) {
            // Start assembly button
            levelAccess.execute { level, pos ->
                val be = level.getBlockEntity(pos) as? AssemblerBlockEntity ?: return@execute
                be.startAssembly()
            }
            return true
        }
        return super.clickMenuButton(player, id)
    }

    private class OutputSlot(handler: IItemHandler, index: Int, x: Int, y: Int) : SlotItemHandler(handler, index, x, y) {
        override fun mayPlace(stack: ItemStack): Boolean = false
    }
}

enum class AssemblerState {
    IDLE,
    ASSEMBLING,
    WAITING
}
