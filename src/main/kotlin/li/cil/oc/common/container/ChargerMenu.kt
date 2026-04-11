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

class ChargerMenu(
    containerId: Int,
    private val playerInventory: Inventory,
    private val levelAccess: ContainerLevelAccess,
    private val chargerInventory: IItemHandler,
    private val data: ContainerData
) : AbstractContainerMenu(ModMenus.CHARGER.get(), containerId) {

    companion object {
        const val SLOT_COUNT = 16
        
        const val DATA_CHARGE_SPEED = 0
        const val DATA_INVERT_CONTROL = 1
        
        fun fromNetwork(containerId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): ChargerMenu {
            return ChargerMenu(
                containerId,
                playerInventory,
                ContainerLevelAccess.NULL,
                ItemStackHandler(SLOT_COUNT),
                SimpleContainerData(2)
            )
        }
    }

    init {
        // Charging slots (4x4 grid)
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                val slotIndex = row * 4 + col
                addSlot(SlotItemHandler(chargerInventory, slotIndex, 53 + col * 18, 17 + row * 18))
            }
        }

        // Player inventory
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 102 + row * 18))
            }
        }
        // Hotbar
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, 160))
        }

        addDataSlots(data)
    }

    val chargeSpeed: Double get() = data.get(DATA_CHARGE_SPEED) / 100.0
    val invertControl: Boolean get() = data.get(DATA_INVERT_CONTROL) != 0

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

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        when (id) {
            0 -> {
                // Toggle invert control
                levelAccess.execute { level, pos ->
                    val be = level.getBlockEntity(pos) ?: return@execute
                    // Would toggle invert setting
                }
                return true
            }
        }
        return super.clickMenuButton(player, id)
    }
}
