package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.component.ScreenComponent
import li.cil.oc.util.OCLogger
import li.cil.oc.util.TextBuffer
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

/**
 * Block entity for screens that holds the text buffer and screen component.
 */
class ScreenBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.SCREEN.get(), pos, state) {

    private var tier: Int = 1
    
    // Flag to track if buffer has changed and needs sync (written by Lua thread, read by server tick)
    @Volatile
    private var bufferDirty = false
    
    // The screen component with text buffer
    val screen: ScreenComponent = ScreenComponent(tier).also {
        // Set up callback to sync when buffer changes
        it.onBufferChanged = { 
            bufferDirty = true
        }
    }
    
    /**
     * Called every tick - syncs dirty buffer to client.
     */
    fun serverTick(level: Level, pos: BlockPos, state: BlockState) {
        if (bufferDirty) {
            val nonSpaceCount = buffer.charData.count { it > 32 }
            OCLogger.debug("[TICK] Buffer dirty, syncing. nonSpace=$nonSpaceCount, isOn=$isOn")
            bufferDirty = false
            setChanged()
            syncToClient()
        }
    }
    
    // Direct access to text buffer for rendering
    val buffer: TextBuffer get() = screen.buffer
    
    // Whether this screen is currently on
    var isOn: Boolean
        get() = screen.isOn
        set(value) {
            if (value) screen.turnOn() else screen.turnOff()
            setChanged()
            syncToClient()
        }
    
    // Connected computer address (for component network)
    var connectedComputer: String? = null
    
    // Keyboard component address (for key signal routing)
    var keyboardAddress: String? = null
    
    fun setTier(t: Int) {
        tier = t
        // Update screen component tier
        screen.setTier(t)
        setChanged()
    }
    
    fun getTier(): Int = tier
    
    /**
     * Set a character at position with colors.
     */
    fun setChar(x: Int, y: Int, char: Char, fg: Int = 0xFFFFFF, bg: Int = 0x000000) {
        buffer.set(x, y, char.code, fg, bg)
        setChanged()
        syncToClient()
    }
    
    /**
     * Set text at position.
     */
    fun setText(x: Int, y: Int, text: String, fg: Int = 0xFFFFFF, bg: Int = 0x000000) {
        buffer.foreground = fg
        buffer.background = bg
        buffer.set(x, y, text)
        setChanged()
        syncToClient()
    }
    
    /**
     * Clear the screen.
     */
    fun clear() {
        buffer.clear()
        setChanged()
        syncToClient()
    }
    
    /**
     * Sync buffer to client for rendering.
     */
    private fun syncToClient() {
        if (level != null && !level!!.isClientSide) {
            val firstChar = if (buffer.charData.isNotEmpty()) buffer.charData[0] else 0
            OCLogger.debug("[SYNC] Syncing screen to client: isOn=$isOn, firstChar=$firstChar ('${firstChar.toChar()}')")
            level!!.sendBlockUpdated(blockPos, blockState, blockState, 3)
        }
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putInt("tier", tier)
        tag.putBoolean("isOn", screen.isOn)
        tag.putString("address", screen.address)
        connectedComputer?.let { tag.putString("connectedComputer", it) }
        
        // Save buffer
        val bufferTag = CompoundTag()
        bufferTag.putInt("width", buffer.width)
        bufferTag.putInt("height", buffer.height)
        bufferTag.putIntArray("chars", buffer.charData)
        bufferTag.putIntArray("fg", buffer.fgData)
        bufferTag.putIntArray("bg", buffer.bgData)
        tag.put("buffer", bufferTag)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        tier = tag.getInt("tier").coerceIn(1, 3)
        if (tag.getBoolean("isOn")) screen.turnOn() else screen.turnOff()
        if (tag.contains("connectedComputer")) {
            connectedComputer = tag.getString("connectedComputer")
        }
        
        // Load buffer
        if (tag.contains("buffer")) {
            val bufferTag = tag.getCompound("buffer")
            val w = bufferTag.getInt("width")
            val h = bufferTag.getInt("height")
            if (w > 0 && h > 0) {
                buffer.resize(w, h)
                val chars = bufferTag.getIntArray("chars")
                val fg = bufferTag.getIntArray("fg")
                val bg = bufferTag.getIntArray("bg")
                buffer.setRawData(chars, fg, bg)
                val nonSpaceCount = chars.count { it > 32 }
                val isClient = level?.isClientSide ?: false
                OCLogger.debug("[LOAD] Buffer ${w}x${h}, firstChar=${if (chars.isNotEmpty()) chars[0] else 0} ('${if (chars.isNotEmpty()) chars[0].toChar() else '?'}'), nonSpace=$nonSpaceCount, isClient=$isClient")
            }
        }
    }
    
    override fun handleUpdateTag(tag: CompoundTag, lookupProvider: HolderLookup.Provider) {
        loadAdditional(tag, lookupProvider)
    }
    
    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        saveAdditional(tag, registries)
        return tag
    }
    
    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }
}
