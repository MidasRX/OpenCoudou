package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.common.init.ModMenus
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

    companion object {
        const val TOTAL_SLOTS = 12
    }

    val inventory: ItemStackHandler = object : ItemStackHandler(TOTAL_SLOTS) {
        override fun onContentsChanged(slot: Int) {
            setChanged()
        }
    }

    private var tier: Int = 1

    fun setTier(t: Int) { tier = t }
    fun getTier(): Int = tier

    // MenuProvider
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
        for (i in 0 until TOTAL_SLOTS) {
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
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"))
        }
        tier = tag.getInt("Tier").coerceAtLeast(1)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        tag.putInt("Tier", tier)
        return tag
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this)
}
