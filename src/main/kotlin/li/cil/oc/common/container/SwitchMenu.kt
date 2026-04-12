package li.cil.oc.common.container

import li.cil.oc.common.blockentity.NetSplitterBlockEntity
import li.cil.oc.common.init.ModMenus
import net.minecraft.core.Direction
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

/**
 * Network Switch (Net Splitter) container menu.
 * Displays side connection toggles and network statistics.
 */
class SwitchMenu(
    containerId: Int,
    private val playerInventory: Inventory,
    private val levelAccess: ContainerLevelAccess,
    private val containerData: ContainerData
) : AbstractContainerMenu(ModMenus.SWITCH.get(), containerId) {

    // Container data layout:
    // 0-5: Side connection state (0=closed, 1=open) for DOWN, UP, NORTH, SOUTH, WEST, EAST
    // 6: Packets relayed
    // 7: Bytes relayed (low bits)
    // 8: Queue size
    
    fun isSideOpen(side: Direction): Boolean = containerData.get(side.ordinal) != 0
    val packetsRelayed: Int get() = containerData.get(6)
    val bytesRelayed: Int get() = containerData.get(7)
    val queueSize: Int get() = containerData.get(8)
    
    init {
        addDataSlots(containerData)
        
        // No inventory slots, just the player inventory for completeness
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

    override fun quickMoveStack(player: Player, index: Int): ItemStack = ItemStack.EMPTY

    override fun stillValid(player: Player): Boolean {
        return levelAccess.evaluate({ level, pos ->
            player.distanceToSqr(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5) <= 64.0
        }, true)
    }

    override fun clickMenuButton(player: Player, id: Int): Boolean {
        when (id) {
            in 0..5 -> {
                // Toggle side connection
                val side = Direction.from3DDataValue(id)
                levelAccess.execute { level, pos ->
                    val be = level.getBlockEntity(pos) as? NetSplitterBlockEntity ?: return@execute
                    be.toggleSide(side)
                }
                return true
            }
            6 -> {
                // Reset statistics
                levelAccess.execute { level, pos ->
                    // Would reset stats
                }
                return true
            }
        }
        return super.clickMenuButton(player, id)
    }
    
    companion object {
        fun fromNetwork(containerId: Int, playerInventory: Inventory, buf: FriendlyByteBuf): SwitchMenu {
            val pos = buf.readBlockPos()
            val level = playerInventory.player.level()
            val containerData = SimpleContainerData(9)
            
            // Read initial side states
            for (i in 0..5) {
                containerData.set(i, if (buf.readBoolean()) 1 else 0)
            }
            
            return SwitchMenu(
                containerId, playerInventory,
                ContainerLevelAccess.create(level, pos),
                containerData
            )
        }
    }
}
