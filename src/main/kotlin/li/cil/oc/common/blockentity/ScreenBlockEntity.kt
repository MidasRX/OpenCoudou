package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.util.TextBuffer
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID

/**
 * Screen block entity that displays text from a connected GPU.
 */
class ScreenBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.SCREEN.get(), pos, state), MenuProvider {

    var tier: Int = 1
    var connectedComputer: String? = null
    var keyboardAddress: String? = null
    
    // Screen address for component identification
    var address: String = UUID.randomUUID().toString().take(8)
    
    // Text buffer for display - size depends on tier
    val buffer: TextBuffer = TextBuffer(getWidthForTier(tier), getHeightForTier(tier))

    override fun getDisplayName(): Component = Component.literal("Screen")
    
    override fun createMenu(
        containerId: Int,
        playerInventory: Inventory,
        player: Player
    ): AbstractContainerMenu? = null

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putInt("tier", tier)
        tag.putString("address", address)
        connectedComputer?.let { tag.putString("connectedComputer", it) }
        keyboardAddress?.let { tag.putString("keyboardAddress", it) }
        
        // Save buffer contents
        tag.putIntArray("bufferChars", buffer.charData)
        tag.putIntArray("bufferFg", buffer.fgData)
        tag.putIntArray("bufferBg", buffer.bgData)
        tag.putInt("bufferWidth", buffer.width)
        tag.putInt("bufferHeight", buffer.height)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        tier = tag.getInt("tier")
        if (tag.contains("address")) address = tag.getString("address")
        connectedComputer = if (tag.contains("connectedComputer")) tag.getString("connectedComputer") else null
        keyboardAddress = if (tag.contains("keyboardAddress")) tag.getString("keyboardAddress") else null
        
        // Load buffer contents
        if (tag.contains("bufferWidth")) {
            val w = tag.getInt("bufferWidth")
            val h = tag.getInt("bufferHeight")
            buffer.resize(w, h)
            if (tag.contains("bufferChars")) {
                buffer.charData = tag.getIntArray("bufferChars")
                buffer.fgData = tag.getIntArray("bufferFg")
                buffer.bgData = tag.getIntArray("bufferBg")
            }
        }
    }
    
    fun serverTick(level: Level, pos: BlockPos, state: BlockState) {
        // TODO: Implement serverTick
    }
    
    companion object {
        @JvmStatic
        fun tick(level: Level, pos: BlockPos, state: BlockState, blockEntity: ScreenBlockEntity) {
            // TODO: Implement tick
        }
        
        fun getWidthForTier(tier: Int): Int = when (tier) {
            1 -> 50
            2 -> 80
            3 -> 160
            else -> 50
        }
        
        fun getHeightForTier(tier: Int): Int = when (tier) {
            1 -> 16
            2 -> 25
            3 -> 50
            else -> 16
        }
    }
}
