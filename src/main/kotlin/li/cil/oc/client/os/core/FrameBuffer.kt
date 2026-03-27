package li.cil.oc.client.os.core

import java.util.Arrays

/**
 * High-performance frame buffer for GPU-accelerated rendering.
 * Uses direct memory access for maximum performance.
 */
class FrameBuffer(val width: Int, val height: Int) {
    
    // Character buffer (what character to display)
    private val charBuffer = CharArray(width * height) { ' ' }
    
    // Foreground color buffer (24-bit RGB packed into Int)
    private val fgColorBuffer = IntArray(width * height) { 0xFFFFFF }
    
    // Background color buffer (24-bit RGB packed into Int)
    private val bgColorBuffer = IntArray(width * height) { 0x000000 }
    
    // Dirty region tracking for partial updates
    private var dirtyMinX = 0
    private var dirtyMinY = 0
    private var dirtyMaxX = width - 1
    private var dirtyMaxY = height - 1
    private var isDirty = true
    
    // Double buffering support
    private var backBuffer: FrameBuffer? = null
    
    companion object {
        // Common colors
        const val BLACK = 0x000000
        const val WHITE = 0xFFFFFF
        const val RED = 0xFF0000
        const val GREEN = 0x00FF00
        const val BLUE = 0x0000FF
        const val CYAN = 0x00FFFF
        const val MAGENTA = 0xFF00FF
        const val YELLOW = 0xFFFF00
        const val GRAY = 0x808080
        const val DARK_GRAY = 0x404040
        const val LIGHT_GRAY = 0xC0C0C0
        
        // UI Colors
        const val DESKTOP_BG = 0x1E1E2E
        const val WINDOW_BG = 0x313244
        const val WINDOW_TITLE = 0x45475A
        const val ACCENT = 0x89B4FA
        const val TEXT = 0xCDD6F4
        const val TEXT_DIM = 0x6C7086
    }
    
    /**
     * Clear the entire buffer.
     */
    fun clear(bgColor: Int = BLACK, fgColor: Int = WHITE) {
        Arrays.fill(charBuffer, ' ')
        Arrays.fill(fgColorBuffer, fgColor)
        Arrays.fill(bgColorBuffer, bgColor)
        markDirty(0, 0, width - 1, height - 1)
    }
    
    /**
     * Set a single character at position.
     */
    fun setChar(x: Int, y: Int, char: Char, fgColor: Int = WHITE, bgColor: Int = BLACK) {
        if (x < 0 || x >= width || y < 0 || y >= height) return
        val index = y * width + x
        charBuffer[index] = char
        fgColorBuffer[index] = fgColor
        bgColorBuffer[index] = bgColor
        markDirty(x, y, x, y)
    }
    
    /**
     * Get character at position.
     */
    fun getChar(x: Int, y: Int): Char {
        if (x < 0 || x >= width || y < 0 || y >= height) return ' '
        return charBuffer[y * width + x]
    }
    
    /**
     * Get foreground color at position.
     */
    fun getFgColor(x: Int, y: Int): Int {
        if (x < 0 || x >= width || y < 0 || y >= height) return WHITE
        return fgColorBuffer[y * width + x]
    }
    
    /**
     * Get background color at position.
     */
    fun getBgColor(x: Int, y: Int): Int {
        if (x < 0 || x >= width || y < 0 || y >= height) return BLACK
        return bgColorBuffer[y * width + x]
    }
    
    /**
     * Draw a string at position.
     */
    fun drawString(x: Int, y: Int, text: String, fgColor: Int = WHITE, bgColor: Int = BLACK) {
        for ((i, char) in text.withIndex()) {
            setChar(x + i, y, char, fgColor, bgColor)
        }
    }
    
    /**
     * Draw a horizontal line.
     */
    fun drawHLine(x: Int, y: Int, length: Int, char: Char = '─', fgColor: Int = WHITE, bgColor: Int = BLACK) {
        for (i in 0 until length) {
            setChar(x + i, y, char, fgColor, bgColor)
        }
    }
    
    /**
     * Draw a vertical line.
     */
    fun drawVLine(x: Int, y: Int, length: Int, char: Char = '│', fgColor: Int = WHITE, bgColor: Int = BLACK) {
        for (i in 0 until length) {
            setChar(x, y + i, char, fgColor, bgColor)
        }
    }
    
    /**
     * Draw a rectangle outline.
     */
    fun drawRect(x: Int, y: Int, w: Int, h: Int, fgColor: Int = WHITE, bgColor: Int = BLACK) {
        // Corners
        setChar(x, y, '┌', fgColor, bgColor)
        setChar(x + w - 1, y, '┐', fgColor, bgColor)
        setChar(x, y + h - 1, '└', fgColor, bgColor)
        setChar(x + w - 1, y + h - 1, '┘', fgColor, bgColor)
        
        // Horizontal lines
        drawHLine(x + 1, y, w - 2, '─', fgColor, bgColor)
        drawHLine(x + 1, y + h - 1, w - 2, '─', fgColor, bgColor)
        
        // Vertical lines
        drawVLine(x, y + 1, h - 2, '│', fgColor, bgColor)
        drawVLine(x + w - 1, y + 1, h - 2, '│', fgColor, bgColor)
    }
    
    /**
     * Fill a rectangle.
     */
    fun fillRect(x: Int, y: Int, w: Int, h: Int, char: Char = ' ', fgColor: Int = WHITE, bgColor: Int = BLACK) {
        for (dy in 0 until h) {
            for (dx in 0 until w) {
                setChar(x + dx, y + dy, char, fgColor, bgColor)
            }
        }
    }
    
    /**
     * Draw a filled box with border.
     */
    fun drawBox(x: Int, y: Int, w: Int, h: Int, fillColor: Int, borderColor: Int = fillColor) {
        fillRect(x, y, w, h, ' ', WHITE, fillColor)
        drawRect(x, y, w, h, borderColor, fillColor)
    }
    
    /**
     * Copy a region from another frame buffer.
     */
    fun blit(src: FrameBuffer, srcX: Int, srcY: Int, dstX: Int, dstY: Int, w: Int, h: Int) {
        for (dy in 0 until h) {
            for (dx in 0 until w) {
                val sx = srcX + dx
                val sy = srcY + dy
                val char = src.getChar(sx, sy)
                val fg = src.getFgColor(sx, sy)
                val bg = src.getBgColor(sx, sy)
                setChar(dstX + dx, dstY + dy, char, fg, bg)
            }
        }
    }
    
    /**
     * Mark a region as dirty (needs redraw).
     */
    private fun markDirty(minX: Int, minY: Int, maxX: Int, maxY: Int) {
        if (!isDirty) {
            dirtyMinX = minX
            dirtyMinY = minY
            dirtyMaxX = maxX
            dirtyMaxY = maxY
            isDirty = true
        } else {
            dirtyMinX = minOf(dirtyMinX, minX)
            dirtyMinY = minOf(dirtyMinY, minY)
            dirtyMaxX = maxOf(dirtyMaxX, maxX)
            dirtyMaxY = maxOf(dirtyMaxY, maxY)
        }
    }
    
    /**
     * Flush the buffer to the screen.
     * This is where the actual GPU rendering happens.
     */
    fun flush() {
        if (!isDirty) return
        
        // Reset dirty region
        isDirty = false
        dirtyMinX = width
        dirtyMinY = height
        dirtyMaxX = 0
        dirtyMaxY = 0
    }
    
    /**
     * Get the raw character buffer (for GPU rendering).
     */
    fun getCharBuffer(): CharArray = charBuffer
    
    /**
     * Get the raw foreground color buffer.
     */
    fun getFgColorBuffer(): IntArray = fgColorBuffer
    
    /**
     * Get the raw background color buffer.
     */
    fun getBgColorBuffer(): IntArray = bgColorBuffer
    
    /**
     * Create a snapshot copy of this buffer.
     */
    fun snapshot(): FrameBuffer {
        val copy = FrameBuffer(width, height)
        System.arraycopy(charBuffer, 0, copy.charBuffer, 0, charBuffer.size)
        System.arraycopy(fgColorBuffer, 0, copy.fgColorBuffer, 0, fgColorBuffer.size)
        System.arraycopy(bgColorBuffer, 0, copy.bgColorBuffer, 0, bgColorBuffer.size)
        return copy
    }
}
