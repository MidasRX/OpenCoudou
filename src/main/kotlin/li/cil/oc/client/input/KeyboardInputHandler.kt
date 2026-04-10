package li.cil.oc.client.input

import li.cil.oc.network.KeyboardInputPacket
import li.cil.oc.util.OCLogger
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.neoforged.neoforge.network.PacketDistributor

/**
 * Handles keyboard input on the client side and sends it to the server.
 */
object KeyboardInputHandler {
    
    // Currently focused screen position (set when player opens a screen)
    var focusedScreen: BlockPos? = null
    
    /**
     * Send a key down event.
     */
    fun sendKeyDown(char: Char, keyCode: Int) {
        val pos = focusedScreen ?: return
        val player = Minecraft.getInstance().player ?: return
        
        val packet = KeyboardInputPacket(pos, char.code, keyCode, true, player.uuid)
        PacketDistributor.sendToServer(packet)
        OCLogger.debug("Sent key_down: $char ($keyCode)")
    }
    
    /**
     * Send a key up event.
     */
    fun sendKeyUp(char: Char, keyCode: Int) {
        val pos = focusedScreen ?: return
        val player = Minecraft.getInstance().player ?: return
        
        val packet = KeyboardInputPacket(pos, char.code, keyCode, false, player.uuid)
        PacketDistributor.sendToServer(packet)
    }
    
    /**
     * Send clipboard paste - sends each character as a key press.
     */
    fun sendClipboard(text: String) {
        val pos = focusedScreen ?: return
        val player = Minecraft.getInstance().player ?: return
        
        // Send each character
        for (c in text) {
            val packet = KeyboardInputPacket(pos, c.code, 0, true, player.uuid)
            PacketDistributor.sendToServer(packet)
        }
        OCLogger.debug("Sent clipboard: ${text.take(20)}...")
    }
    
    /**
     * Focus a screen for keyboard input.
     */
    fun focusScreen(pos: BlockPos) {
        focusedScreen = pos
        OCLogger.debug("Focused screen at $pos")
    }
    
    /**
     * Unfocus the currently focused screen.
     */
    fun unfocus() {
        focusedScreen = null
    }
    
    /**
     * Check if we currently have a focused screen.
     */
    fun hasFocus(): Boolean = focusedScreen != null
}
