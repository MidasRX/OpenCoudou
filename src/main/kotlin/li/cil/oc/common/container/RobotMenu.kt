package li.cil.oc.common.container

import li.cil.oc.common.init.ModMenus
import li.cil.oc.server.entity.RobotEntity
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
 * Robot container menu - displays robot inventory with power controls.
 * Robots have 16/32/64/100 inventory slots depending on tier.
 */
class RobotMenu(
    containerId: Int,
    private val playerInventory: Inventory,
    private val robotInventory: IItemHandler,
    private val toolInventory: IItemHandler,
    private val containerData: ContainerData,
    private val robot: RobotEntity?
) : AbstractContainerMenu(ModMenus.ROBOT.get(), containerId) {

    val tier: Int get() = containerData.get(0)
    val running: Boolean get() = containerData.get(1) != 0
    val selectedSlot: Int get() = containerData.get(2)
    val energy: Int get() = containerData.get(3)
    val maxEnergy: Int get() = containerData.get(4)
    
    private val inventorySlotCount: Int
    
    init {
        addDataSlots(containerData)
        
        // Tool slot (top left)
        addSlot(SlotItemHandler(toolInventory, 0, 8, 8))
        
        // Main inventory slots based on tier
        // Tier 1: 16 slots (4x4), Tier 2: 32 slots (4x8), Tier 3: 64 slots (8x8), Creative: 100 slots (10x10)
        val slotCount = robotInventory.slots
        inventorySlotCount = slotCount + 1 // +1 for tool slot
        
        val cols = when {
            slotCount <= 16 -> 4
            slotCount <= 32 -> 4
            slotCount <= 64 -> 8
            else -> 10
        }
        val rows = (slotCount + cols - 1) / cols
        
        // Position inventory starting at y=28 (below tool)
        val startX = 8
        val startY = 28
        
        for (i in 0 until slotCount) {
            val row = i / cols
            val col = i % cols
            addSlot(SlotItemHandler(robotInventory, i, startX + col * 18, startY + row * 18))
        }
        
        // Calculate player inventory position based on robot inventory height
        val playerInvY = startY + rows * 18 + 14
        
        // Player inventory (3 rows of 9)
        for (row in 0 until 3) {
            for (col in 0 until 9) {
                addSlot(Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18))
            }
        }
        
        // Player hotbar
        for (col in 0 until 9) {
            addSlot(Slot(playerInventory, col, 8 + col * 18, playerInvY + 58))
        }
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
        var result = ItemStack.EMPTY
        val slot = slots.getOrNull(index) ?: return result
        
        if (slot.hasItem()) {
            val stack = slot.item
            result = stack.copy()
            
            if (index < inventorySlotCount) {
                // Move from robot to player
                if (!moveItemStackTo(stack, inventorySlotCount, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else {
                // Move from player to robot
                if (!moveItemStackTo(stack, 0, inventorySlotCount, false)) {
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
        return robot?.isAlive == true && player.distanceToSqr(robot) <= 64.0
    }

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        when (id) {
            0 -> {
                // Power toggle
                robot?.let {
                    if (it.isRunning()) {
                        it.stop()
                    } else {
                        val result = it.start()
                        if (!result && player is ServerPlayer) {
                            player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable("oc.robot.no_energy"),
                                true
                            )
                        }
                    }
                }
                return true
            }
            in 1..100 -> {
                // Select inventory slot
                robot?.setSelectedSlot(id - 1)
                return true
            }
        }
        return super.clickMenuButton(player, id)
    }
    
    companion object {
        /**
         * Factory for creating menu from network data.
         */
        fun fromNetwork(containerId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): RobotMenu {
            val entityId = buf.readVarInt()
            val tier = buf.readVarInt()
            val slotCount = when (tier) {
                1 -> 16
                2 -> 32
                3 -> 64
                else -> 100
            }
            val robotInventory = ItemStackHandler(slotCount)
            val toolInventory = ItemStackHandler(1)
            val containerData = SimpleContainerData(5)
            containerData.set(0, tier)
            
            val level = playerInventory.player.level()
            val robot = level.getEntity(entityId) as? RobotEntity
            
            return RobotMenu(containerId, playerInventory, robotInventory, toolInventory, containerData, robot)
        }
    }
}
