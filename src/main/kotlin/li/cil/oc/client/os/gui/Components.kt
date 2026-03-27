package li.cil.oc.client.os.gui

import li.cil.oc.client.os.core.FrameBuffer

/**
 * UI Components library for SkibidiOS2.
 * Provides pre-built widgets for building applications.
 */

// ============================================================
// Label - Text display
// ============================================================

class Label(
    x: Int, y: Int,
    var text: String,
    var color: Int = FrameBuffer.TEXT,
    var bgColor: Int = FrameBuffer.WINDOW_BG
) : UIComponent(x, y, text.length, 1) {
    
    override fun render(buffer: FrameBuffer) {
        if (!visible) return
        buffer.drawString(x, y, text, color, bgColor)
    }
    
    override fun update() {
        width = text.length
    }
}

// ============================================================
// Button - Clickable button
// ============================================================

class Button(
    x: Int, y: Int,
    var text: String,
    var onClick: (() -> Unit)? = null
) : UIComponent(x, y, text.length + 4, 1) {
    
    var hovered = false
    var pressed = false
    
    override fun render(buffer: FrameBuffer) {
        if (!visible) return
        
        val bg = when {
            pressed -> FrameBuffer.ACCENT
            hovered -> 0x4A4A5A
            else -> FrameBuffer.WINDOW_TITLE
        }
        val fg = if (pressed) FrameBuffer.BLACK else FrameBuffer.TEXT
        
        buffer.drawString(x, y, "[ $text ]", fg, bg)
    }
    
    override fun update() {
        width = text.length + 4
    }
    
    override fun handleClick(localX: Int, localY: Int, button: Int): Boolean {
        if (enabled) {
            pressed = true
            onClick?.invoke()
            pressed = false
            return true
        }
        return false
    }
}

// ============================================================
// TextField - Text input
// ============================================================

class TextField(
    x: Int, y: Int,
    width: Int,
    var text: String = "",
    var placeholder: String = "",
    var maxLength: Int = 256,
    var onTextChanged: ((String) -> Unit)? = null,
    var onSubmit: ((String) -> Unit)? = null
) : UIComponent(x, y, width, 1) {
    
    var cursorPos = text.length
    private var cursorVisible = true
    private var cursorBlinkTimer = 0
    
    override fun render(buffer: FrameBuffer) {
        if (!visible) return
        
        val bg = if (focused) 0x3A3A4A else FrameBuffer.WINDOW_BG
        val fg = FrameBuffer.TEXT
        
        // Background
        buffer.fillRect(x, y, width, 1, ' ', fg, bg)
        
        // Border
        buffer.setChar(x, y, '[', FrameBuffer.TEXT_DIM, bg)
        buffer.setChar(x + width - 1, y, ']', FrameBuffer.TEXT_DIM, bg)
        
        // Text or placeholder
        val displayText = if (text.isEmpty()) placeholder else text
        val textColor = if (text.isEmpty()) FrameBuffer.TEXT_DIM else fg
        val maxTextWidth = width - 2
        val truncatedText = if (displayText.length > maxTextWidth) {
            displayText.takeLast(maxTextWidth)
        } else {
            displayText
        }
        buffer.drawString(x + 1, y, truncatedText.padEnd(maxTextWidth), textColor, bg)
        
        // Cursor
        if (focused && cursorVisible) {
            val cursorX = x + 1 + minOf(cursorPos, maxTextWidth - 1)
            buffer.setChar(cursorX, y, '▋', FrameBuffer.ACCENT, bg)
        }
    }
    
    override fun update() {
        cursorBlinkTimer++
        if (cursorBlinkTimer >= 30) {
            cursorVisible = !cursorVisible
            cursorBlinkTimer = 0
        }
    }
    
    override fun handleClick(localX: Int, localY: Int, button: Int): Boolean {
        focused = true
        cursorPos = minOf(localX, text.length)
        return true
    }
    
    override fun handleKey(keyCode: Int, char: Char): Boolean {
        if (!focused) return false
        
        when (keyCode) {
            259 -> { // Backspace
                if (cursorPos > 0) {
                    text = text.removeRange(cursorPos - 1, cursorPos)
                    cursorPos--
                    onTextChanged?.invoke(text)
                }
            }
            261 -> { // Delete
                if (cursorPos < text.length) {
                    text = text.removeRange(cursorPos, cursorPos + 1)
                    onTextChanged?.invoke(text)
                }
            }
            263 -> { // Left arrow
                if (cursorPos > 0) cursorPos--
            }
            262 -> { // Right arrow
                if (cursorPos < text.length) cursorPos++
            }
            268 -> { // Home
                cursorPos = 0
            }
            269 -> { // End
                cursorPos = text.length
            }
            257, 335 -> { // Enter
                onSubmit?.invoke(text)
            }
            else -> {
                if (char.isLetterOrDigit() || char.isWhitespace() || char in "!@#$%^&*()-_=+[]{}|;':\",./<>?`~") {
                    if (text.length < maxLength) {
                        text = text.substring(0, cursorPos) + char + text.substring(cursorPos)
                        cursorPos++
                        onTextChanged?.invoke(text)
                    }
                }
            }
        }
        return true
    }
}

// ============================================================
// ListView - Scrollable list
// ============================================================

class ListView<T>(
    x: Int, y: Int,
    width: Int,
    height: Int,
    var items: List<T> = emptyList(),
    var renderItem: (T, Int, Boolean) -> String = { item, _, _ -> item.toString() },
    var onSelect: ((T, Int) -> Unit)? = null,
    var onDoubleClick: ((T, Int) -> Unit)? = null
) : UIComponent(x, y, width, height) {
    
    var selectedIndex = -1
    var scrollOffset = 0
    private var lastClickTime = 0L
    private var lastClickIndex = -1
    
    override fun render(buffer: FrameBuffer) {
        if (!visible) return
        
        // Background
        buffer.fillRect(x, y, width, height, ' ', FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
        
        // Items
        val visibleItems = items.drop(scrollOffset).take(height)
        visibleItems.forEachIndexed { index, item ->
            val actualIndex = index + scrollOffset
            val isSelected = actualIndex == selectedIndex
            val bg = if (isSelected) FrameBuffer.ACCENT else FrameBuffer.WINDOW_BG
            val fg = if (isSelected) FrameBuffer.BLACK else FrameBuffer.TEXT
            
            val itemText = renderItem(item, actualIndex, isSelected)
            val displayText = if (itemText.length > width) itemText.take(width - 1) + "…" else itemText.padEnd(width)
            buffer.drawString(x, y + index, displayText, fg, bg)
        }
        
        // Scrollbar
        if (items.size > height) {
            val scrollbarHeight = maxOf(1, (height * height) / items.size)
            val scrollbarPos = (scrollOffset * (height - scrollbarHeight)) / maxOf(1, items.size - height)
            
            for (i in 0 until height) {
                val char = if (i >= scrollbarPos && i < scrollbarPos + scrollbarHeight) '█' else '░'
                buffer.setChar(x + width - 1, y + i, char, FrameBuffer.TEXT_DIM, FrameBuffer.WINDOW_BG)
            }
        }
    }
    
    override fun update() {}
    
    override fun handleClick(localX: Int, localY: Int, button: Int): Boolean {
        val clickedIndex = scrollOffset + localY
        if (clickedIndex < items.size) {
            val currentTime = System.currentTimeMillis()
            
            // Double click detection
            if (clickedIndex == lastClickIndex && currentTime - lastClickTime < 400) {
                onDoubleClick?.invoke(items[clickedIndex], clickedIndex)
                lastClickTime = 0
            } else {
                selectedIndex = clickedIndex
                onSelect?.invoke(items[clickedIndex], clickedIndex)
                lastClickTime = currentTime
                lastClickIndex = clickedIndex
            }
            return true
        }
        return false
    }
    
    override fun handleKey(keyCode: Int, char: Char): Boolean {
        when (keyCode) {
            265 -> { // Up
                if (selectedIndex > 0) {
                    selectedIndex--
                    if (selectedIndex < scrollOffset) scrollOffset = selectedIndex
                    onSelect?.invoke(items[selectedIndex], selectedIndex)
                }
            }
            264 -> { // Down
                if (selectedIndex < items.size - 1) {
                    selectedIndex++
                    if (selectedIndex >= scrollOffset + height) scrollOffset = selectedIndex - height + 1
                    onSelect?.invoke(items[selectedIndex], selectedIndex)
                }
            }
            257, 335 -> { // Enter
                if (selectedIndex >= 0 && selectedIndex < items.size) {
                    onDoubleClick?.invoke(items[selectedIndex], selectedIndex)
                }
            }
        }
        return true
    }
    
    fun scrollUp() {
        if (scrollOffset > 0) scrollOffset--
    }
    
    fun scrollDown() {
        if (scrollOffset < items.size - height) scrollOffset++
    }
}

// ============================================================
// ProgressBar - Progress indicator
// ============================================================

class ProgressBar(
    x: Int, y: Int,
    width: Int,
    var progress: Float = 0f,
    var showPercentage: Boolean = true,
    var fgColor: Int = FrameBuffer.ACCENT,
    var bgColor: Int = FrameBuffer.DARK_GRAY
) : UIComponent(x, y, width, 1) {
    
    override fun render(buffer: FrameBuffer) {
        if (!visible) return
        
        val filledWidth = ((width - 2) * progress.coerceIn(0f, 1f)).toInt()
        
        // Background
        buffer.fillRect(x, y, width, 1, ' ', FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
        
        // Progress track
        buffer.setChar(x, y, '[', FrameBuffer.TEXT_DIM, FrameBuffer.WINDOW_BG)
        buffer.setChar(x + width - 1, y, ']', FrameBuffer.TEXT_DIM, FrameBuffer.WINDOW_BG)
        
        // Progress fill
        for (i in 0 until width - 2) {
            val char = if (i < filledWidth) '█' else '░'
            val color = if (i < filledWidth) fgColor else bgColor
            buffer.setChar(x + 1 + i, y, char, color, FrameBuffer.WINDOW_BG)
        }
        
        // Percentage text
        if (showPercentage) {
            val percentText = "${(progress * 100).toInt()}%"
            val textX = x + (width - percentText.length) / 2
            buffer.drawString(textX, y, percentText, FrameBuffer.WHITE, FrameBuffer.WINDOW_BG)
        }
    }
    
    override fun update() {}
}

// ============================================================
// TextArea - Multi-line text display/edit
// ============================================================

class TextArea(
    x: Int, y: Int,
    width: Int,
    height: Int,
    var lines: MutableList<String> = mutableListOf(""),
    var editable: Boolean = true,
    var onTextChanged: ((List<String>) -> Unit)? = null
) : UIComponent(x, y, width, height) {
    
    var cursorLine = 0
    var cursorCol = 0
    var scrollOffset = 0
    private var cursorVisible = true
    private var cursorBlinkTimer = 0
    
    override fun render(buffer: FrameBuffer) {
        if (!visible) return
        
        val bg = FrameBuffer.WINDOW_BG
        
        // Background
        buffer.fillRect(x, y, width, height, ' ', FrameBuffer.TEXT, bg)
        
        // Lines
        val visibleLines = lines.drop(scrollOffset).take(height)
        visibleLines.forEachIndexed { index, line ->
            val displayLine = if (line.length > width) line.take(width - 1) + "…" else line
            buffer.drawString(x, y + index, displayLine, FrameBuffer.TEXT, bg)
        }
        
        // Cursor
        if (focused && cursorVisible && editable) {
            val cursorY = cursorLine - scrollOffset
            if (cursorY in 0 until height) {
                val cursorX = minOf(cursorCol, width - 1)
                buffer.setChar(x + cursorX, y + cursorY, '▋', FrameBuffer.ACCENT, bg)
            }
        }
    }
    
    override fun update() {
        if (focused) {
            cursorBlinkTimer++
            if (cursorBlinkTimer >= 30) {
                cursorVisible = !cursorVisible
                cursorBlinkTimer = 0
            }
        }
    }
    
    override fun handleClick(localX: Int, localY: Int, button: Int): Boolean {
        focused = true
        cursorLine = scrollOffset + localY
        cursorCol = localX
        if (cursorLine >= lines.size) cursorLine = lines.size - 1
        if (cursorLine >= 0 && cursorCol > lines[cursorLine].length) {
            cursorCol = lines[cursorLine].length
        }
        return true
    }
    
    override fun handleKey(keyCode: Int, char: Char): Boolean {
        if (!focused || !editable) return false
        
        when (keyCode) {
            259 -> { // Backspace
                if (cursorCol > 0) {
                    lines[cursorLine] = lines[cursorLine].removeRange(cursorCol - 1, cursorCol)
                    cursorCol--
                } else if (cursorLine > 0) {
                    cursorCol = lines[cursorLine - 1].length
                    lines[cursorLine - 1] += lines[cursorLine]
                    lines.removeAt(cursorLine)
                    cursorLine--
                }
                onTextChanged?.invoke(lines)
            }
            261 -> { // Delete
                if (cursorCol < lines[cursorLine].length) {
                    lines[cursorLine] = lines[cursorLine].removeRange(cursorCol, cursorCol + 1)
                } else if (cursorLine < lines.size - 1) {
                    lines[cursorLine] += lines[cursorLine + 1]
                    lines.removeAt(cursorLine + 1)
                }
                onTextChanged?.invoke(lines)
            }
            263 -> cursorCol = maxOf(0, cursorCol - 1) // Left
            262 -> cursorCol = minOf(lines[cursorLine].length, cursorCol + 1) // Right
            265 -> { // Up
                if (cursorLine > 0) {
                    cursorLine--
                    cursorCol = minOf(cursorCol, lines[cursorLine].length)
                    if (cursorLine < scrollOffset) scrollOffset = cursorLine
                }
            }
            264 -> { // Down
                if (cursorLine < lines.size - 1) {
                    cursorLine++
                    cursorCol = minOf(cursorCol, lines[cursorLine].length)
                    if (cursorLine >= scrollOffset + height) scrollOffset = cursorLine - height + 1
                }
            }
            257 -> { // Enter
                val newLine = lines[cursorLine].substring(cursorCol)
                lines[cursorLine] = lines[cursorLine].substring(0, cursorCol)
                lines.add(cursorLine + 1, newLine)
                cursorLine++
                cursorCol = 0
                onTextChanged?.invoke(lines)
            }
            else -> {
                if (char.code >= 32) {
                    lines[cursorLine] = lines[cursorLine].substring(0, cursorCol) + char + 
                                       lines[cursorLine].substring(cursorCol)
                    cursorCol++
                    onTextChanged?.invoke(lines)
                }
            }
        }
        return true
    }
    
    fun getText(): String = lines.joinToString("\n")
    
    fun setText(text: String) {
        lines = text.split("\n").toMutableList()
        if (lines.isEmpty()) lines.add("")
        cursorLine = 0
        cursorCol = 0
        scrollOffset = 0
    }
}
