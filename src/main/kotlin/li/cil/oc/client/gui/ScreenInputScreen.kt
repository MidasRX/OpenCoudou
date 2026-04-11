package li.cil.oc.client.gui

import li.cil.oc.client.input.KeyboardInputHandler
import li.cil.oc.network.ModPackets
import li.cil.oc.network.ScreenTouchPacket
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component

/**
 * A transparent overlay screen that captures keyboard and mouse input for
 * a focused screen block. Like original OC:
 * - Keyboard: key_down, key_up, clipboard
 * - Mouse: touch, drag, drop, scroll
 * Press ESC to close.
 */
class ScreenInputScreen(private val screenPos: BlockPos) : Screen(Component.literal("Screen")) {
    
    private var isDragging = false
    
    // Screen resolution for coordinate mapping
    private var screenW = 50
    private var screenH = 16
    
    // Buffer the last keyPressed code to merge with charTyped
    private var pendingKeyCode = -1
    
    override fun init() {
        super.init()
        KeyboardInputHandler.focusScreen(screenPos)
        // Get actual screen resolution from block entity
        val level = minecraft?.level
        if (level != null) {
            val be = level.getBlockEntity(screenPos)
            if (be is li.cil.oc.common.blockentity.ScreenBlockEntity) {
                screenW = be.buffer.width
                screenH = be.buffer.height
            }
        }
    }
    
    override fun removed() {
        super.removed()
        KeyboardInputHandler.unfocus()
    }
    
    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // Don't render any background - let the game world show through
    }
    
    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // Minimal HUD hint at bottom
        val text = "Press ESC to close"
        val x = (width - font.width(text)) / 2
        graphics.drawString(font, text, x, height - 12, 0x80FFFFFF.toInt(), false)
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            onClose()
            return true
        }
        
        // Handle Ctrl+V paste
        if (keyCode == 86 && (modifiers and 2) != 0) { // V + CTRL
            val clipboard = minecraft?.keyboardHandler?.clipboard ?: ""
            if (clipboard.isNotEmpty()) {
                KeyboardInputHandler.sendClipboard(clipboard)
            }
            return true
        }
        
        // For keys that generate a charTyped event, buffer the keyCode
        // and wait for charTyped to send combined signal.
        // For non-printable keys (Enter, Backspace, Tab, Delete, function keys, etc.),
        // send immediately since charTyped won't fire for them.
        val specialChar = charFromKeyCode(keyCode)
        if (specialChar != '\u0000') {
            // Special key with known char mapping - send immediately
            KeyboardInputHandler.sendKeyDown(specialChar, keyCode)
            pendingKeyCode = -1
        } else if (keyCode < 256 && (modifiers and (1 or 2 or 4)) == 0) {
            // Printable key range without modifiers - wait for charTyped
            pendingKeyCode = keyCode
        } else {
            // Function keys, modifier combos, etc. - send immediately with no char
            KeyboardInputHandler.sendKeyDown('\u0000', keyCode)
            pendingKeyCode = -1
        }
        return true
    }
    
    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        val char = charFromKeyCode(keyCode)
        KeyboardInputHandler.sendKeyUp(char, keyCode)
        return true
    }
    
    override fun charTyped(char: Char, modifiers: Int): Boolean {
        // Merge with pending keyCode from keyPressed if available
        val keyCode = if (pendingKeyCode >= 0) pendingKeyCode else 0
        pendingKeyCode = -1
        // Send combined signal: key_down(kbAddr, charCode, keyCode, playerName)
        KeyboardInputHandler.sendKeyDown(char, keyCode)
        return true
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        isDragging = true
        val charX = (mouseX / width.toDouble() * screenW).coerceIn(1.0, screenW.toDouble())
        val charY = (mouseY / height.toDouble() * screenH).coerceIn(1.0, screenH.toDouble())
        ModPackets.sendToServer(ScreenTouchPacket(screenPos, charX, charY, button, "touch"))
        return true
    }
    
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (isDragging) {
            val charX = (mouseX / width.toDouble() * screenW).coerceIn(1.0, screenW.toDouble())
            val charY = (mouseY / height.toDouble() * screenH).coerceIn(1.0, screenH.toDouble())
            ModPackets.sendToServer(ScreenTouchPacket(screenPos, charX, charY, button, "drag"))
        }
        return true
    }
    
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (isDragging) {
            isDragging = false
            val charX = (mouseX / width.toDouble() * screenW).coerceIn(1.0, screenW.toDouble())
            val charY = (mouseY / height.toDouble() * screenH).coerceIn(1.0, screenH.toDouble())
            ModPackets.sendToServer(ScreenTouchPacket(screenPos, charX, charY, button, "drop"))
        }
        return true
    }
    
    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val direction = if (scrollY > 0) 1 else -1
        val charX = (mouseX / width.toDouble() * screenW).coerceIn(1.0, screenW.toDouble())
        val charY = (mouseY / height.toDouble() * screenH).coerceIn(1.0, screenH.toDouble())
        ModPackets.sendToServer(ScreenTouchPacket(screenPos, charX, charY, direction, "scroll"))
        return true
    }
    
    override fun isPauseScreen(): Boolean = false
    
    override fun shouldCloseOnEsc(): Boolean = true
    
    private fun charFromKeyCode(keyCode: Int): Char {
        return when (keyCode) {
            257 -> '\r'     // Enter
            335 -> '\r'     // Numpad Enter
            259 -> '\b'     // Backspace
            258 -> '\t'     // Tab
            261 -> '\u007F' // Delete
            32 -> ' '       // Space
            else -> '\u0000'
        }
    }
}
