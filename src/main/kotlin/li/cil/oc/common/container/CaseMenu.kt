package li.cil.oc.common.container

import li.cil.oc.common.blockentity.CaseBlockEntity
import li.cil.oc.common.init.ModMenus
import li.cil.oc.common.item.*
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

/**
 * A slot that validates items based on the slot type from CaseSlotConfig.
 */
class ValidatedSlot(
    handler: IItemHandler, index: Int, x: Int, y: Int,
    private val slotType: String,
    private val maxTier: Int
) : SlotItemHandler(handler, index, x, y) {
    
    override fun mayPlace(stack: ItemStack): Boolean {
        val item = stack.item
        return when (slotType) {
            CaseSlotConfig.CPU -> item is CPUItem && item.tier <= maxTier
            CaseSlotConfig.MEMORY -> item is MemoryItem
            CaseSlotConfig.HDD -> item is HDDItem && item.tier <= maxTier
            CaseSlotConfig.EEPROM -> item is EEPROMItem
            CaseSlotConfig.FLOPPY -> item is FloppyDiskItem
            CaseSlotConfig.CARD -> {
                // Cards: GPU, Network, Wireless, Internet, Redstone, Data, WorldSensor, Linked, Debug
                item is GPUItem || item is NetworkCardItem || item is WirelessCardItem ||
                item is InternetCardItem || item is RedstoneCardItem || item is DataCardItem ||
                item is WorldSensorCardItem || item is LinkedCardItem || item is DebugCardItem ||
                item is APUItem || item is ComponentBusItem
            }
            else -> false
        }
    }
}

class CaseMenu(
    containerId: Int,
    private val playerInventory: Inventory,
    private val levelAccess: ContainerLevelAccess,
    private val caseInventory: IItemHandler,
    val tier: Int
) : AbstractContainerMenu(ModMenus.CASE.get(), containerId) {

    val slotDefs: List<CaseSlotConfig.SlotDef> = CaseSlotConfig.getSlots(tier)
    private var componentSlotCount = 0

    init {
        // Component slots positioned per tier config, with type validation
        for ((i, def) in slotDefs.withIndex()) {
            addSlot(ValidatedSlot(caseInventory, i, def.x, def.y, def.type, def.maxTier))
        }
        componentSlotCount = slotDefs.size

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
            if (index < componentSlotCount) {
                if (!moveItemStackTo(stack, componentSlotCount, slots.size, true)) return ItemStack.EMPTY
            } else {
                if (!moveItemStackTo(stack, 0, componentSlotCount, false)) return ItemStack.EMPTY
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
            // Power toggle
            levelAccess.execute { level, pos ->
                val be = level.getBlockEntity(pos) as? CaseBlockEntity ?: return@execute
                val error = be.togglePower()
                if (error != null && player is net.minecraft.server.level.ServerPlayer) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable(error), true)
                }
            }
            return true
        }
        return super.clickMenuButton(player, id)
    }

    companion object {
        fun fromNetwork(containerId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): CaseMenu {
            val tier = buf.readVarInt()
            // Use max slot count (10) to match server's inventory size
            return CaseMenu(containerId, playerInventory, ContainerLevelAccess.NULL, ItemStackHandler(10), tier)
        }
    }
}
