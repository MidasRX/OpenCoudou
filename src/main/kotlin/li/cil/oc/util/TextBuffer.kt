package li.cil.oc.util

/**
 * Text buffer for screen rendering. Stores characters and colors in a 2D grid.
 * Based on how terminal emulators work.
 */
class TextBuffer(var width: Int, var height: Int) {
    
    // Character data (one int per cell - Unicode codepoint) - flat array for serialization
    var charData = IntArray(width * height) { ' '.code }
    
    // Foreground colors (packed RGB) - flat array
    var fgData = IntArray(width * height) { 0xFFFFFF }
    
    // Background colors (packed RGB) - flat array
    var bgData = IntArray(width * height) { 0x000000 }
    
    /**
     * Copy raw buffer data (used for network sync).
     */
    fun setRawData(chars: IntArray, fg: IntArray, bg: IntArray) {
        if (chars.size == width * height) {
            chars.copyInto(charData)
        }
        if (fg.size == width * height) {
            fg.copyInto(fgData)
        }
        if (bg.size == width * height) {
            bg.copyInto(bgData)
        }
        dirty = true
    }
    
    // Current cursor position
    var cursorX = 0
    var cursorY = 0
    var cursorBlink = true
    
    // Current drawing colors
    var foreground = 0xFFFFFF
    var background = 0x000000
    
    // Dirty flag for rendering optimization
    var dirty = true
    
    private fun idx(x: Int, y: Int) = y * width + x
    
    /**
     * Resize the buffer, preserving existing content where possible.
     */
    fun resize(newWidth: Int, newHeight: Int) {
        if (newWidth == width && newHeight == height) return
        
        val newChars = IntArray(newWidth * newHeight) { ' '.code }
        val newFg = IntArray(newWidth * newHeight) { foreground }
        val newBg = IntArray(newWidth * newHeight) { background }
        
        // Copy existing data
        for (y in 0 until minOf(height, newHeight)) {
            for (x in 0 until minOf(width, newWidth)) {
                val oldIdx = y * width + x
                val newIdx = y * newWidth + x
                newChars[newIdx] = charData[oldIdx]
                newFg[newIdx] = fgData[oldIdx]
                newBg[newIdx] = bgData[oldIdx]
            }
        }
        
        charData = newChars
        fgData = newFg
        bgData = newBg
        width = newWidth
        height = newHeight
        dirty = true
    }
    
    /**
     * Set a single character at position.
     */
    fun set(x: Int, y: Int, char: Int, fgColor: Int = foreground, bgColor: Int = background): Boolean {
        if (x < 0 || x >= width || y < 0 || y >= height) return false
        val i = idx(x, y)
        charData[i] = char
        fgData[i] = fgColor
        bgData[i] = bgColor
        dirty = true
        return true
    }
    
    /**
     * Set a string starting at position.
     */
    fun set(x: Int, y: Int, text: String, vertical: Boolean = false): Boolean {
        if (y < 0 || y >= height) return false
        var px = x
        var py = y
        for (char in text) {
            if (vertical) {
                if (py >= height) break
                if (px in 0 until width) {
                    val i = idx(px, py)
                    charData[i] = char.code
                    fgData[i] = foreground
                    bgData[i] = background
                }
                py++
            } else {
                if (px >= width) break
                if (px >= 0) {
                    val i = idx(px, y)
                    charData[i] = char.code
                    fgData[i] = foreground
                    bgData[i] = background
                }
                px++
            }
        }
        dirty = true
        return true
    }
    
    /**
     * Get character at position.
     */
    fun getChar(x: Int, y: Int): Int {
        if (x < 0 || x >= width || y < 0 || y >= height) return ' '.code
        return charData[idx(x, y)]
    }
    
    /**
     * Set just the character at position (for bitblt).
     */
    fun setChar(x: Int, y: Int, char: Int) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            charData[idx(x, y)] = char
            dirty = true
        }
    }
    
    /**
     * Get foreground color at position.
     */
    fun getForeground(x: Int, y: Int): Int {
        if (x < 0 || x >= width || y < 0 || y >= height) return foreground
        return fgData[idx(x, y)]
    }
    
    /**
     * Set just the foreground color at position (for bitblt).
     */
    fun setForeground(x: Int, y: Int, color: Int) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            fgData[idx(x, y)] = color
            dirty = true
        }
    }
    
    /**
     * Get background color at position.
     */
    fun getBackground(x: Int, y: Int): Int {
        if (x < 0 || x >= width || y < 0 || y >= height) return background
        return bgData[idx(x, y)]
    }
    
    /**
     * Set just the background color at position (for bitblt).
     */
    fun setBackground(x: Int, y: Int, color: Int) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            bgData[idx(x, y)] = color
            dirty = true
        }
    }
    
    /**
     * Fill a rectangle with a character.
     */
    fun fill(x: Int, y: Int, w: Int, h: Int, char: Int): Boolean {
        val x1 = maxOf(0, x)
        val y1 = maxOf(0, y)
        val x2 = minOf(width, x + w)
        val y2 = minOf(height, y + h)
        
        for (py in y1 until y2) {
            for (px in x1 until x2) {
                val i = idx(px, py)
                charData[i] = char
                fgData[i] = foreground
                bgData[i] = background
            }
        }
        dirty = true
        return true
    }
    
    /**
     * Copy a region to another position.
     */
    fun copy(x: Int, y: Int, w: Int, h: Int, tx: Int, ty: Int): Boolean {
        // Create temp copy
        val tempChars = IntArray(w * h)
        val tempFg = IntArray(w * h)
        val tempBg = IntArray(w * h)
        
        for (dy in 0 until h) {
            for (dx in 0 until w) {
                val sx = x + dx
                val sy = y + dy
                val ti = dy * w + dx
                if (sx in 0 until width && sy in 0 until height) {
                    val si = idx(sx, sy)
                    tempChars[ti] = charData[si]
                    tempFg[ti] = fgData[si]
                    tempBg[ti] = bgData[si]
                } else {
                    tempChars[ti] = ' '.code
                    tempFg[ti] = foreground
                    tempBg[ti] = background
                }
            }
        }
        
        // Paste at target (tx/ty are translation offsets from source, like original OC)
        for (dy in 0 until h) {
            for (dx in 0 until w) {
                val px = x + dx + tx
                val py = y + dy + ty
                if (px in 0 until width && py in 0 until height) {
                    val ti = dy * w + dx
                    val pi = idx(px, py)
                    charData[pi] = tempChars[ti]
                    fgData[pi] = tempFg[ti]
                    bgData[pi] = tempBg[ti]
                }
            }
        }
        dirty = true
        return true
    }
    
    /**
     * Clear the entire buffer.
     */
    fun clear() {
        fill(0, 0, width, height, ' '.code)
    }
    
    /**
     * Get a line of text.
     */
    fun getLine(y: Int): String {
        if (y < 0 || y >= height) return ""
        val sb = StringBuilder()
        for (x in 0 until width) {
            sb.append(charData[idx(x, y)].toChar())
        }
        return sb.toString()
    }
    
    /**
     * Get all lines.
     */
    fun getLines(): List<String> = (0 until height).map { getLine(it) }
}
