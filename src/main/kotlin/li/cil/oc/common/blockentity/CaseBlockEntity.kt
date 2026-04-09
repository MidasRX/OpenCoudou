package li.cil.oc.common.blockentity

import li.cil.oc.common.container.CaseSlotConfig
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.Containers
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.items.ItemStackHandler

class CaseBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.CASE.get(), pos, state), MenuProvider {

    private var tier: Int = 1
    var isPowered: Boolean = false

    val inventory: ItemStackHandler = object : ItemStackHandler(CaseSlotConfig.getSlots(tier).size) {
        override fun onContentsChanged(slot: Int) {
            setChanged()
        }
    }

    fun setTier(t: Int) {
        tier = t
        // Resize inventory to match tier
        val newSize = CaseSlotConfig.getSlots(tier).size
        if (inventory.slots != newSize) {
            val newInv = ItemStackHandler(newSize)
            for (i in 0 until minOf(inventory.slots, newSize)) {
                newInv.setStackInSlot(i, inventory.getStackInSlot(i))
            }
            // Copy contents into resized handler
            for (i in 0 until newSize) {
                if (i < inventory.slots) {
                    // keep existing
                } else {
                    break
                }
            }
        }
    }

    fun getTier(): Int = tier

    private fun slotCount(): Int = CaseSlotConfig.getSlots(tier).size

    override fun getDisplayName(): Component =
        Component.translatable("block.opencomputers.case${tier}")

    override fun createMenu(containerId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu {
        return li.cil.oc.common.container.CaseMenu(
            containerId, playerInventory,
            ContainerLevelAccess.create(level!!, blockPos),
            inventory, tier
        )
    }

    fun dropContents(level: Level, pos: BlockPos) {
        for (i in 0 until inventory.slots) {
            val stack = inventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
                inventory.setStackInSlot(i, ItemStack.EMPTY)
            }
        }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.put("Inventory", inventory.serializeNBT(registries))
        tag.putInt("Tier", tier)
        tag.putBoolean("Powered", isPowered)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        tier = tag.getInt("Tier").coerceAtLeast(1)
        isPowered = tag.getBoolean("Powered")
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"))
        }
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        tag.putInt("Tier", tier)
        return tag
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this)
}
