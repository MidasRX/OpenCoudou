package li.cil.oc.client.input

import li.cil.oc.network.KeyboardInputPacket
import li.cil.oc.util.KeyboardKeys
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
     * @param char The character typed (or 0 for non-printable keys)
     * @param keyCode The GLFW key code (will be converted to OC/LWJGL2 code)
     */
    fun sendKeyDown(char: Char, keyCode: Int) {
        val pos = focusedScreen ?: return
        val player = Minecraft.getInstance().player ?: return
        
        // Convert GLFW keycode to OC-compatible LWJGL2 keycode
        val ocKeyCode = KeyboardKeys.glfwToOC(keyCode)
        
        val packet = KeyboardInputPacket(pos, char.code, ocKeyCode, true, player.uuid)
        PacketDistributor.sendToServer(packet)
        OCLogger.debug("Sent key_down: $char (GLFW:$keyCode -> OC:$ocKeyCode)")
    }
    
    /**
     * Send a key up event.
     * @param char The character (or 0 for non-printable keys)
     * @param keyCode The GLFW key code (will be converted to OC/LWJGL2 code)
     */
    fun sendKeyUp(char: Char, keyCode: Int) {
        val pos = focusedScreen ?: return
        val player = Minecraft.getInstance().player ?: return
        
        // Convert GLFW keycode to OC-compatible LWJGL2 keycode
        val ocKeyCode = KeyboardKeys.glfwToOC(keyCode)
        
        val packet = KeyboardInputPacket(pos, char.code, ocKeyCode, false, player.uuid)
        PacketDistributor.sendToServer(packet)
    }
    
    /**
     * Send clipboard paste - sends as a single clipboard signal.
     * Original OC sends clipboard(keyboardAddress, text, playerName).
     */
    fun sendClipboard(text: String) {
        val pos = focusedScreen ?: return
        val player = Minecraft.getInstance().player ?: return
        
        // Send clipboard as a single packet with code=-1 marker and full text
        val packet = KeyboardInputPacket(pos, 0, -1, true, player.uuid, text)
        PacketDistributor.sendToServer(packet)
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
