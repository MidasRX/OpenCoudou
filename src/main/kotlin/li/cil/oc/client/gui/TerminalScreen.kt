package li.cil.oc.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.input.KeyboardInputHandler
import li.cil.oc.common.blockentity.ScreenBlockEntity
import li.cil.oc.network.ModPackets
import li.cil.oc.network.ScreenTouchPacket
import li.cil.oc.util.TextBuffer
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component

/**
 * Full-screen terminal GUI that renders the TextBuffer contents.
 * Looks like a real computer terminal / OC screen.
 */
class TerminalScreen(private val screenPos: BlockPos) : Screen(Component.literal("Terminal")) {

    private var buffer: TextBuffer? = null
    private var isDragging = false
    private var pendingKeyCode = -1

    // Terminal rendering area
    private var termX = 0
    private var termY = 0
    private var termW = 0
    private var termH = 0
    private var cellW = 0f
    private var cellH = 0f

    // Colors
    companion object {
        private const val BORDER_COLOR = 0xFF1A1A1A.toInt()
        private const val BEZEL_COLOR = 0xFF222222.toInt()
        private const val FRAME_COLOR = 0xFF333333.toInt()
        private const val MARGIN = 16
        private const val BEZEL = 8
    }

    override fun init() {
        super.init()
        KeyboardInputHandler.focusScreen(screenPos)
        refreshBuffer()
    }

    override fun removed() {
        super.removed()
        KeyboardInputHandler.unfocus()
    }

    private fun refreshBuffer() {
        val level = minecraft?.level ?: return
        val be = level.getBlockEntity(screenPos)
        if (be is ScreenBlockEntity) {
            buffer = be.buffer
        }
    }

    private fun computeLayout() {
        val buf = buffer ?: return
        // Available space inside the bezel
        val availW = width - (MARGIN + BEZEL) * 2
        val availH = height - (MARGIN + BEZEL) * 2

        // Calculate cell size to fit all characters
        val maxCellW = availW.toFloat() / buf.width
        val maxCellH = availH.toFloat() / buf.height

        // Use the minimum to keep cells square-ish (slightly taller than wide like a real terminal)
        val baseCell = minOf(maxCellW, maxCellH * 0.55f)
        cellW = baseCell
        cellH = baseCell * 1.8f

        // If cellH-based layout overflows, re-scale
        if (cellH * buf.height > availH) {
            cellH = availH.toFloat() / buf.height
            cellW = cellH / 1.8f
        }
        if (cellW * buf.width > availW) {
            cellW = availW.toFloat() / buf.width
            cellH = cellW * 1.8f
        }

        termW = (cellW * buf.width).toInt()
        termH = (cellH * buf.height).toInt()
        termX = (width - termW) / 2
        termY = (height - termH) / 2
    }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // Dim the world behind
        graphics.fill(0, 0, width, height, 0xCC000000.toInt())
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(graphics, mouseX, mouseY, partialTick)
        refreshBuffer()
        val buf = buffer ?: return
        computeLayout()

        // Draw monitor frame/bezel
        val frameX = termX - BEZEL - 2
        val frameY = termY - BEZEL - 2
        val frameW = termW + (BEZEL + 2) * 2
        val frameH = termH + (BEZEL + 2) * 2

        // Outer frame
        graphics.fill(frameX - 2, frameY - 2, frameX + frameW + 2, frameY + frameH + 2, BORDER_COLOR)
        // Inner bezel
        graphics.fill(frameX, frameY, frameX + frameW, frameY + frameH, BEZEL_COLOR)
        // Screen background (slightly lighter than black for contrast)
        graphics.fill(termX - 1, termY - 1, termX + termW + 1, termY + termH + 1, FRAME_COLOR)

        // Draw each cell
        for (y in 0 until buf.height) {
            for (x in 0 until buf.width) {
                val idx = y * buf.width + x
                val bg = buf.bgData[idx]
                val fg = buf.fgData[idx]
                val char = buf.charData[idx]

                val px = termX + (x * cellW).toInt()
                val py = termY + (y * cellH).toInt()
                val px2 = termX + ((x + 1) * cellW).toInt()
                val py2 = termY + ((y + 1) * cellH).toInt()

                // Background
                if (bg != 0) {
                    graphics.fill(px, py, px2, py2, bg or (0xFF shl 24))
                } else {
                    graphics.fill(px, py, px2, py2, 0xFF000000.toInt())
                }

                // Character
                if (char > 32) {
                    val charStr = String(Character.toChars(char))
                    val scale = cellH / 9f // font is 9px high
                    val poseStack = graphics.pose()
                    poseStack.pushPose()
                    poseStack.translate(px.toDouble() + 1.0, py.toDouble() + (cellH - 9f * scale) / 2.0 + 1.0, 0.0)
                    poseStack.scale(scale * 0.9f, scale, 1f)
                    graphics.drawString(font, charStr, 0, 0, fg or (0xFF shl 24), false)
                    poseStack.popPose()
                }
            }
        }

        // Cursor
        if (buf.cursorBlink && (System.currentTimeMillis() / 500) % 2 == 0L) {
            val cx = termX + (buf.cursorX * cellW).toInt()
            val cy = termY + (buf.cursorY * cellH).toInt()
            val cx2 = termX + ((buf.cursorX + 1) * cellW).toInt()
            val cy2 = termY + ((buf.cursorY + 1) * cellH).toInt()
            if (buf.cursorX in 0 until buf.width && buf.cursorY in 0 until buf.height) {
                graphics.fill(cx, cy, cx2, cy2, 0xFFCCCCCC.toInt())
            }
        }

        // Status bar at bottom
        val statusText = "ESC to close  |  Ctrl+V to paste"
        val statusX = (width - font.width(statusText)) / 2
        graphics.drawString(font, statusText, statusX, height - 12, 0x80FFFFFF.toInt(), false)
    }

    // Convert screen pixel coords to terminal character coords (1-indexed)
    private fun pixelToChar(mouseX: Double, mouseY: Double): Pair<Int, Int> {
        val buf = buffer ?: return Pair(1, 1)
        val cx = ((mouseX - termX) / cellW).toInt().coerceIn(0, buf.width - 1) + 1
        val cy = ((mouseY - termY) / cellH).toInt().coerceIn(0, buf.height - 1) + 1
        return Pair(cx, cy)
    }

    // === Input handling (same as ScreenInputScreen but with proper coord mapping) ===

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == 256) { // ESC
            onClose()
            return true
        }
        if (keyCode == 86 && (modifiers and 2) != 0) { // Ctrl+V
            val clipboard = minecraft?.keyboardHandler?.clipboard ?: ""
            if (clipboard.isNotEmpty()) {
                KeyboardInputHandler.sendClipboard(clipboard)
            }
            return true
        }
        val specialChar = charFromKeyCode(keyCode)
        if (specialChar != '\u0000') {
            KeyboardInputHandler.sendKeyDown(specialChar, keyCode)
            pendingKeyCode = -1
        } else if (keyCode < 256 && (modifiers and (1 or 2 or 4)) == 0) {
            pendingKeyCode = keyCode
        } else {
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
        val keyCode = if (pendingKeyCode >= 0) pendingKeyCode else 0
        pendingKeyCode = -1
        KeyboardInputHandler.sendKeyDown(char, keyCode)
        return true
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        isDragging = true
        val (cx, cy) = pixelToChar(mouseX, mouseY)
        ModPackets.sendToServer(ScreenTouchPacket(screenPos, cx.toDouble(), cy.toDouble(), button, "touch"))
        return true
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (isDragging) {
            val (cx, cy) = pixelToChar(mouseX, mouseY)
            ModPackets.sendToServer(ScreenTouchPacket(screenPos, cx.toDouble(), cy.toDouble(), button, "drag"))
        }
        return true
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (isDragging) {
            isDragging = false
            val (cx, cy) = pixelToChar(mouseX, mouseY)
            ModPackets.sendToServer(ScreenTouchPacket(screenPos, cx.toDouble(), cy.toDouble(), button, "drop"))
        }
        return true
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val direction = if (scrollY > 0) 1 else -1
        val (cx, cy) = pixelToChar(mouseX, mouseY)
        ModPackets.sendToServer(ScreenTouchPacket(screenPos, cx.toDouble(), cy.toDouble(), direction, "scroll"))
        return true
    }

    override fun isPauseScreen(): Boolean = false
    override fun shouldCloseOnEsc(): Boolean = true

    private fun charFromKeyCode(keyCode: Int): Char = when (keyCode) {
        257, 335 -> '\r'
        259 -> '\b'
        258 -> '\t'
        261 -> '\u007F'
        32 -> ' '
        else -> '\u0000'
    }
}
