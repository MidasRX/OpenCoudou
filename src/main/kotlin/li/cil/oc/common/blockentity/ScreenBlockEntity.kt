package li.cil.oc.common.blockentity

import li.cil.oc.OpenComputers
import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.network.ModPackets
import li.cil.oc.network.ScreenUpdatePacket
import li.cil.oc.util.TextBuffer
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.nio.ByteBuffer
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

    // Network sync: true when buffer has changed and needs to be sent to clients
    @Volatile
    var needsSync = false

    override fun getDisplayName(): Component = Component.literal("Screen")
    
    override fun createMenu(
        containerId: Int,
        playerInventory: Inventory,
        player: Player
    ): AbstractContainerMenu? = null

    /** Mark screen for network sync on next server tick. */
    fun markForSync() {
        needsSync = true
    }

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

    // --- NeoForge chunk-load sync: sends buffer to client when chunk is first loaded ---

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        saveAdditional(tag, registries)
        return tag
    }

    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }
    
    override fun handleUpdateTag(tag: CompoundTag, registries: HolderLookup.Provider) {
        // Called on client when update tag is received
        loadAdditional(tag, registries)
    }
    
    // Periodic verification: every ~5s confirm the connected case still exists.
    // If not, clear stale connection and blank the screen.
    private var verifyTickCounter = 0

    fun serverTick(level: Level, pos: BlockPos, state: BlockState) {
        verifyTickCounter++
        if (connectedComputer != null && verifyTickCounter >= 100) {
            verifyTickCounter = 0
            if (!isConnectedCaseAlive(level, pos)) {
                connectedComputer = null
                buffer.clear()
                needsSync = true
                setChanged()
            }
        }

        if (!needsSync) return
        needsSync = false
        val packet = createFullSyncPacket()
        ModPackets.sendToAllTracking(level, pos, packet)
    }

    /** Scan a small area for a CaseBlockEntity whose machine matches our connectedComputer. */
    private fun isConnectedCaseAlive(level: Level, pos: BlockPos): Boolean {
        val expected = connectedComputer ?: return false
        for (dx in -8..8) for (dy in -4..4) for (dz in -8..8) {
            val be = level.getBlockEntity(pos.offset(dx, dy, dz)) ?: continue
            if (be is li.cil.oc.common.blockentity.CaseBlockEntity) {
                val addr = try { be.node()?.address } catch (_: Exception) { null }
                if (addr == expected) return true
            }
        }
        return false
    }

    /** Encode the entire buffer into a ScreenUpdatePacket. */
    fun createFullSyncPacket(): ScreenUpdatePacket {
        val w = buffer.width
        val h = buffer.height
        val size = w * h
        // 4 bytes per int, 3 arrays (char, fg, bg)
        val bb = ByteBuffer.allocate(size * 4 * 3)
        for (i in 0 until size) bb.putInt(buffer.charData[i])
        for (i in 0 until size) bb.putInt(buffer.fgData[i])
        for (i in 0 until size) bb.putInt(buffer.bgData[i])
        return ScreenUpdatePacket(blockPos, 0, 0, w, h, bb.array())
    }
    
    companion object {
        @JvmStatic
        fun tick(level: Level, pos: BlockPos, state: BlockState, blockEntity: ScreenBlockEntity) {
            if (!level.isClientSide) {
                blockEntity.serverTick(level, pos, state)
            }
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

        /** Decode a ScreenUpdatePacket's data into a client-side ScreenBlockEntity's buffer. */
        fun applyScreenUpdate(be: ScreenBlockEntity, packet: ScreenUpdatePacket) {
            val w = packet.width
            val h = packet.height
            if (w != be.buffer.width || h != be.buffer.height) {
                be.buffer.resize(w, h)
            }
            val size = w * h
            val expected = size * 4 * 3
            if (packet.data.size < expected) return
            val bb = ByteBuffer.wrap(packet.data)
            val chars = IntArray(size) { bb.getInt() }
            val fg = IntArray(size) { bb.getInt() }
            val bg = IntArray(size) { bb.getInt() }
            be.buffer.setRawData(chars, fg, bg)
        }
    }
}
