package li.cil.oc.client.os.apps.media

import li.cil.oc.client.os.core.KotlinOS
import li.cil.oc.client.os.apps.Application
import li.cil.oc.client.os.apps.AppInfo
import li.cil.oc.client.os.apps.AppCategory
import li.cil.oc.client.os.libs.*
import kotlin.math.*

/**
 * Picture Edit - Advanced image editor for OCIF format images.
 * Features:
 * - Multiple drawing tools (brush, line, rectangle, ellipse, fill, text, eraser)
 * - Color picker with recent colors
 * - Layer support (future)
 * - Undo/redo
 * - Import/export images
 */

private val PICTURE_EDIT_INFO = AppInfo(
    id = "picture_edit",
    name = "Picture Edit",
    icon = "🎨",
    category = AppCategory.MEDIA,
    description = "Advanced image editor for OCIF images"
) { PictureEditApp(it) }

class PictureEditApp(os: KotlinOS) : Application(os, PICTURE_EDIT_INFO) {
    
    // Canvas
    private var canvasWidth = 80
    private var canvasHeight = 25
    private var canvas = Array(canvasHeight) { IntArray(canvasWidth) { 0xFFFFFF } }
    private var canvasChars = Array(canvasHeight) { CharArray(canvasWidth) { ' ' } }
    private var canvasX = 0
    private var canvasY = 0
    
    // Colors
    private var foregroundColor = 0xFFFFFF
    private var backgroundColor = 0x000000
    private val recentColors = mutableListOf<Int>()
    private val maxRecentColors = 52
    
    // Tools
    private var currentTool = Tool.BRUSH
    private var brushSize = 1
    private var toolStartX = 0
    private var toolStartY = 0
    private var isDrawing = false
    
    // UI State
    private var showColorPicker = false
    private var showToolSettings = false
    private var showFileDialog = false
    private var currentFilePath = ""
    private var hasChanges = false
    
    // Undo/Redo
    private val undoStack = mutableListOf<CanvasState>()
    private val redoStack = mutableListOf<CanvasState>()
    private val maxUndoSteps = 50
    
    // Cursor
    private var cursorX = 0
    private var cursorY = 0
    
    // View
    private var viewOffsetX = 0
    private var viewOffsetY = 0
    private var zoom = 1
    
    enum class Tool(val icon: String, val name: String) {
        BRUSH("🖌", "Brush"),
        ERASER("⌫", "Eraser"),
        LINE("╱", "Line"),
        RECTANGLE("▢", "Rectangle"),
        FILLED_RECT("▣", "Filled Rectangle"),
        ELLIPSE("○", "Ellipse"),
        FILLED_ELLIPSE("●", "Filled Ellipse"),
        FILL("🪣", "Fill"),
        PICK("💉", "Color Picker"),
        TEXT("A", "Text"),
        SELECT("▭", "Selection")
    }
    
    data class CanvasState(
        val pixels: Array<IntArray>,
        val chars: Array<CharArray>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CanvasState) return false
            return pixels.contentDeepEquals(other.pixels) && chars.contentDeepEquals(other.chars)
        }
        override fun hashCode() = pixels.contentDeepHashCode()
    }
    
    // Palette - Standard OC colors
    private val palette = listOf(
        0x000000, 0x1A1A1A, 0x333333, 0x4D4D4D, 0x666666, 0x808080, 0x999999, 0xB3B3B3, 0xCCCCCC, 0xE6E6E6, 0xFFFFFF,
        0xFF0000, 0xFF3300, 0xFF6600, 0xFF9900, 0xFFCC00, 0xFFFF00, 0xCCFF00, 0x99FF00, 0x66FF00, 0x33FF00, 0x00FF00,
        0x00FF33, 0x00FF66, 0x00FF99, 0x00FFCC, 0x00FFFF, 0x00CCFF, 0x0099FF, 0x0066FF, 0x0033FF, 0x0000FF,
        0x3300FF, 0x6600FF, 0x9900FF, 0xCC00FF, 0xFF00FF, 0xFF00CC, 0xFF0099, 0xFF0066, 0xFF0033,
        0x800000, 0x804000, 0x808000, 0x408000, 0x008000, 0x008040, 0x008080, 0x004080, 0x000080, 0x400080, 0x800080, 0x800040
    )
    
    override fun onCreate() {
        createWindow("Picture Edit", 2, 1, 95, 32)
        initializeRecentColors()
        loadConfig()
    }
    
    override fun onStart() {
        newCanvas(80, 25)
    }
    
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() { saveConfig() }
    override fun onDestroy() { saveConfig() }
    
    override fun onUpdate() {
        render()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        if (showColorPicker) {
            handleColorPickerInput(keyCode, char)
            return
        }
        if (showFileDialog) {
            handleFileDialogInput(keyCode, char)
            return
        }
        
        when (keyCode) {
            Keyboard.KEY_ESCAPE -> {
                if (showToolSettings) showToolSettings = false
                else close()
            }
            
            // Movement
            Keyboard.KEY_UP -> moveCursor(0, -1)
            Keyboard.KEY_DOWN -> moveCursor(0, 1)
            Keyboard.KEY_LEFT -> moveCursor(-1, 0)
            Keyboard.KEY_RIGHT -> moveCursor(1, 0)
            
            // Drawing
            Keyboard.KEY_SPACE, Keyboard.KEY_ENTER -> {
                if (!isDrawing) {
                    startDrawing()
                } else {
                    endDrawing()
                }
            }
            
            // Tools (1-9, 0)
            Keyboard.KEY_1 -> currentTool = Tool.BRUSH
            Keyboard.KEY_2 -> currentTool = Tool.ERASER
            Keyboard.KEY_3 -> currentTool = Tool.LINE
            Keyboard.KEY_4 -> currentTool = Tool.RECTANGLE
            Keyboard.KEY_5 -> currentTool = Tool.FILLED_RECT
            Keyboard.KEY_6 -> currentTool = Tool.ELLIPSE
            Keyboard.KEY_7 -> currentTool = Tool.FILLED_ELLIPSE
            Keyboard.KEY_8 -> currentTool = Tool.FILL
            Keyboard.KEY_9 -> currentTool = Tool.PICK
            Keyboard.KEY_0 -> currentTool = Tool.TEXT
            
            // Shortcuts
            Keyboard.KEY_C -> showColorPicker = true
            Keyboard.KEY_X -> swapColors()
            Keyboard.KEY_Z -> if (Keyboard.isCtrlDown()) undo()
            Keyboard.KEY_Y -> if (Keyboard.isCtrlDown()) redo()
            Keyboard.KEY_S -> if (Keyboard.isCtrlDown()) saveImage() else showFileDialog = true
            Keyboard.KEY_O -> if (Keyboard.isCtrlDown()) showFileDialog = true
            Keyboard.KEY_N -> if (Keyboard.isCtrlDown()) newCanvas(80, 25)
            Keyboard.KEY_PLUS, Keyboard.KEY_EQUALS -> if (brushSize < 10) brushSize++
            Keyboard.KEY_MINUS -> if (brushSize > 1) brushSize--
            Keyboard.KEY_T -> showToolSettings = !showToolSettings
            
            // Char input for text tool
            else -> {
                if (currentTool == Tool.TEXT && char.isLetterOrDigit() || char in " !@#$%^&*()-=+[]{}|;':\",./<>?`~") {
                    drawChar(cursorX, cursorY, char)
                    moveCursor(1, 0)
                }
            }
        }
    }
    
    private fun handleColorPickerInput(keyCode: Int, char: Char) {
        when (keyCode) {
            Keyboard.KEY_ESCAPE -> showColorPicker = false
            Keyboard.KEY_ENTER -> showColorPicker = false
            Keyboard.KEY_LEFT -> {
                val idx = palette.indexOf(foregroundColor)
                if (idx > 0) foregroundColor = palette[idx - 1]
            }
            Keyboard.KEY_RIGHT -> {
                val idx = palette.indexOf(foregroundColor)
                if (idx < palette.size - 1 && idx >= 0) foregroundColor = palette[idx + 1]
                else if (idx == -1) foregroundColor = palette[0]
            }
            Keyboard.KEY_UP -> {
                val idx = palette.indexOf(foregroundColor)
                if (idx >= 11) foregroundColor = palette[idx - 11]
            }
            Keyboard.KEY_DOWN -> {
                val idx = palette.indexOf(foregroundColor)
                if (idx + 11 < palette.size) foregroundColor = palette[idx + 11]
            }
            Keyboard.KEY_TAB -> swapColors()
        }
    }
    
    private fun handleFileDialogInput(keyCode: Int, char: Char) {
        when (keyCode) {
            Keyboard.KEY_ESCAPE -> showFileDialog = false
            Keyboard.KEY_ENTER -> {
                if (currentFilePath.isNotEmpty()) {
                    loadImage(currentFilePath)
                }
                showFileDialog = false
            }
            Keyboard.KEY_BACK -> {
                if (currentFilePath.isNotEmpty()) {
                    currentFilePath = currentFilePath.dropLast(1)
                }
            }
            else -> {
                if (char.isLetterOrDigit() || char in "._-/") {
                    currentFilePath += char
                }
            }
        }
    }
    
    private fun moveCursor(dx: Int, dy: Int) {
        cursorX = (cursorX + dx).coerceIn(0, canvasWidth - 1)
        cursorY = (cursorY + dy).coerceIn(0, canvasHeight - 1)
        
        if (isDrawing) {
            when (currentTool) {
                Tool.BRUSH -> drawBrush(cursorX, cursorY)
                Tool.ERASER -> eraseBrush(cursorX, cursorY)
                else -> {} // Preview is shown in render
            }
        }
    }
    
    private fun startDrawing() {
        saveUndoState()
        isDrawing = true
        toolStartX = cursorX
        toolStartY = cursorY
        
        when (currentTool) {
            Tool.BRUSH -> drawBrush(cursorX, cursorY)
            Tool.ERASER -> eraseBrush(cursorX, cursorY)
            Tool.FILL -> floodFill(cursorX, cursorY, canvas[cursorY][cursorX], foregroundColor)
            Tool.PICK -> pickColor(cursorX, cursorY)
            else -> {}
        }
    }
    
    private fun endDrawing() {
        when (currentTool) {
            Tool.LINE -> drawLine(toolStartX, toolStartY, cursorX, cursorY)
            Tool.RECTANGLE -> drawRectangle(toolStartX, toolStartY, cursorX, cursorY, false)
            Tool.FILLED_RECT -> drawRectangle(toolStartX, toolStartY, cursorX, cursorY, true)
            Tool.ELLIPSE -> drawEllipse(toolStartX, toolStartY, cursorX, cursorY, false)
            Tool.FILLED_ELLIPSE -> drawEllipse(toolStartX, toolStartY, cursorX, cursorY, true)
            else -> {}
        }
        isDrawing = false
        hasChanges = true
        addRecentColor(foregroundColor)
    }
    
    private fun drawBrush(x: Int, y: Int) {
        for (dy in -brushSize/2..brushSize/2) {
            for (dx in -brushSize/2..brushSize/2) {
                val px = x + dx
                val py = y + dy
                if (px in 0 until canvasWidth && py in 0 until canvasHeight) {
                    canvas[py][px] = foregroundColor
                }
            }
        }
        hasChanges = true
    }
    
    private fun eraseBrush(x: Int, y: Int) {
        for (dy in -brushSize/2..brushSize/2) {
            for (dx in -brushSize/2..brushSize/2) {
                val px = x + dx
                val py = y + dy
                if (px in 0 until canvasWidth && py in 0 until canvasHeight) {
                    canvas[py][px] = backgroundColor
                    canvasChars[py][px] = ' '
                }
            }
        }
        hasChanges = true
    }
    
    private fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int) {
        // Bresenham's line algorithm
        val dx = abs(x2 - x1)
        val dy = abs(y2 - y1)
        val sx = if (x1 < x2) 1 else -1
        val sy = if (y1 < y2) 1 else -1
        var err = dx - dy
        var cx = x1
        var cy = y1
        
        while (true) {
            if (cx in 0 until canvasWidth && cy in 0 until canvasHeight) {
                canvas[cy][cx] = foregroundColor
            }
            if (cx == x2 && cy == y2) break
            val e2 = 2 * err
            if (e2 > -dy) { err -= dy; cx += sx }
            if (e2 < dx) { err += dx; cy += sy }
        }
    }
    
    private fun drawRectangle(x1: Int, y1: Int, x2: Int, y2: Int, filled: Boolean) {
        val minX = minOf(x1, x2)
        val maxX = maxOf(x1, x2)
        val minY = minOf(y1, y2)
        val maxY = maxOf(y1, y2)
        
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                if (x in 0 until canvasWidth && y in 0 until canvasHeight) {
                    if (filled || x == minX || x == maxX || y == minY || y == maxY) {
                        canvas[y][x] = foregroundColor
                    }
                }
            }
        }
    }
    
    private fun drawEllipse(x1: Int, y1: Int, x2: Int, y2: Int, filled: Boolean) {
        val cx = (x1 + x2) / 2
        val cy = (y1 + y2) / 2
        val rx = abs(x2 - x1) / 2
        val ry = abs(y2 - y1) / 2
        
        if (rx == 0 || ry == 0) return
        
        for (y in cy - ry..cy + ry) {
            for (x in cx - rx..cx + rx) {
                if (x in 0 until canvasWidth && y in 0 until canvasHeight) {
                    val dx = (x - cx).toDouble() / rx
                    val dy = (y - cy).toDouble() / ry
                    val dist = dx * dx + dy * dy
                    if (filled) {
                        if (dist <= 1.0) canvas[y][x] = foregroundColor
                    } else {
                        if (dist in 0.7..1.3) canvas[y][x] = foregroundColor
                    }
                }
            }
        }
    }
    
    private fun floodFill(x: Int, y: Int, targetColor: Int, replacementColor: Int) {
        if (targetColor == replacementColor) return
        if (x !in 0 until canvasWidth || y !in 0 until canvasHeight) return
        if (canvas[y][x] != targetColor) return
        
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(x to y)
        
        while (stack.isNotEmpty()) {
            val (px, py) = stack.removeLast()
            if (px !in 0 until canvasWidth || py !in 0 until canvasHeight) continue
            if (canvas[py][px] != targetColor) continue
            
            canvas[py][px] = replacementColor
            stack.addLast(px + 1 to py)
            stack.addLast(px - 1 to py)
            stack.addLast(px to py + 1)
            stack.addLast(px to py - 1)
        }
    }
    
    private fun pickColor(x: Int, y: Int) {
        if (x in 0 until canvasWidth && y in 0 until canvasHeight) {
            foregroundColor = canvas[y][x]
            addRecentColor(foregroundColor)
        }
        isDrawing = false
    }
    
    private fun drawChar(x: Int, y: Int, char: Char) {
        if (x in 0 until canvasWidth && y in 0 until canvasHeight) {
            saveUndoState()
            canvasChars[y][x] = char
            canvas[y][x] = foregroundColor
            hasChanges = true
        }
    }
    
    private fun swapColors() {
        val temp = foregroundColor
        foregroundColor = backgroundColor
        backgroundColor = temp
    }
    
    private fun addRecentColor(color: Int) {
        recentColors.remove(color)
        recentColors.add(0, color)
        while (recentColors.size > maxRecentColors) {
            recentColors.removeLast()
        }
    }
    
    private fun initializeRecentColors() {
        recentColors.addAll(palette.take(maxRecentColors))
    }
    
    private fun newCanvas(width: Int, height: Int) {
        canvasWidth = width
        canvasHeight = height
        canvas = Array(height) { IntArray(width) { backgroundColor } }
        canvasChars = Array(height) { CharArray(width) { ' ' } }
        cursorX = 0
        cursorY = 0
        undoStack.clear()
        redoStack.clear()
        hasChanges = false
        currentFilePath = ""
    }
    
    private fun saveUndoState() {
        val state = CanvasState(
            pixels = canvas.map { it.copyOf() }.toTypedArray(),
            chars = canvasChars.map { it.copyOf() }.toTypedArray()
        )
        undoStack.add(state)
        if (undoStack.size > maxUndoSteps) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }
    
    private fun undo() {
        if (undoStack.isNotEmpty()) {
            val currentState = CanvasState(
                pixels = canvas.map { it.copyOf() }.toTypedArray(),
                chars = canvasChars.map { it.copyOf() }.toTypedArray()
            )
            redoStack.add(currentState)
            
            val state = undoStack.removeLast()
            canvas = state.pixels.map { it.copyOf() }.toTypedArray()
            canvasChars = state.chars.map { it.copyOf() }.toTypedArray()
        }
    }
    
    private fun redo() {
        if (redoStack.isNotEmpty()) {
            val currentState = CanvasState(
                pixels = canvas.map { it.copyOf() }.toTypedArray(),
                chars = canvasChars.map { it.copyOf() }.toTypedArray()
            )
            undoStack.add(currentState)
            
            val state = redoStack.removeLast()
            canvas = state.pixels.map { it.copyOf() }.toTypedArray()
            canvasChars = state.chars.map { it.copyOf() }.toTypedArray()
        }
    }
    
    private fun saveImage() {
        if (currentFilePath.isEmpty()) {
            currentFilePath = "/home/untitled.pic"
        }
        
        val fs = os.fileSystem
        val imageData = buildString {
            // OCIF format header
            append("OCIF01")
            append(canvasWidth.toChar())
            append(canvasHeight.toChar())
            
            for (y in 0 until canvasHeight) {
                for (x in 0 until canvasWidth) {
                    val color = canvas[y][x]
                    val char = canvasChars[y][x]
                    // Simplified format: RGB + char
                    append(((color shr 16) and 0xFF).toChar())
                    append(((color shr 8) and 0xFF).toChar())
                    append((color and 0xFF).toChar())
                    append(char)
                }
            }
        }
        
        fs.writeText(currentFilePath, imageData)
        hasChanges = false
    }
    
    private fun loadImage(path: String) {
        val fs = os.fileSystem
        val data = fs.readText(path) ?: return
        
        if (data.length < 8 || !data.startsWith("OCIF")) return
        
        val width = data[6].code
        val height = data[7].code
        
        if (width <= 0 || height <= 0) return
        
        newCanvas(width, height)
        currentFilePath = path
        
        var idx = 8
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (idx + 4 <= data.length) {
                    val r = data[idx++].code and 0xFF
                    val g = data[idx++].code and 0xFF
                    val b = data[idx++].code and 0xFF
                    val char = data[idx++]
                    canvas[y][x] = (r shl 16) or (g shl 8) or b
                    canvasChars[y][x] = char
                }
            }
        }
    }
    
    private fun loadConfig() {
        val fs = os.fileSystem
        val config = fs.readText("/etc/picture_edit.cfg")
        // Parse config if exists
    }
    
    private fun saveConfig() {
        val fs = os.fileSystem
        val config = buildString {
            appendLine("foreground=$foregroundColor")
            appendLine("background=$backgroundColor")
            appendLine("brushSize=$brushSize")
            appendLine("tool=${currentTool.name}")
            appendLine("recentColors=${recentColors.joinToString(",")}")
        }
        fs.writeText("/etc/picture_edit.cfg", config)
    }
    
    private fun render() {
        val w = window ?: return
        
        // Clear
        Screen.setBackground(0x1E1E1E)
        Screen.fill(w.x, w.y, w.width, w.height, ' ')
        
        // Title bar
        Screen.setBackground(0x3C3C3C)
        Screen.fill(w.x, w.y, w.width, 1, ' ')
        Screen.setForeground(0xFFFFFF)
        val title = "🎨 Picture Edit" + (if (currentFilePath.isNotEmpty()) " - ${Paths.name(currentFilePath)}" else " - New") + (if (hasChanges) " *" else "")
        Screen.set(w.x + 2, w.y, title)
        
        // Toolbar
        Screen.setBackground(0x2D2D2D)
        Screen.fill(w.x, w.y + 1, w.width, 2, ' ')
        
        var toolX = w.x + 2
        for (tool in Tool.entries) {
            val isSelected = tool == currentTool
            Screen.setBackground(if (isSelected) 0x3399FF else 0x2D2D2D)
            Screen.setForeground(if (isSelected) 0xFFFFFF else 0xAAAAAA)
            Screen.set(toolX, w.y + 1, " ${tool.icon} ")
            toolX += 4
        }
        
        // Brush size
        Screen.setBackground(0x2D2D2D)
        Screen.setForeground(0x888888)
        Screen.set(w.x + 2, w.y + 2, "Size: $brushSize")
        
        // Color indicators
        Screen.setBackground(foregroundColor)
        Screen.fill(w.x + 15, w.y + 2, 3, 1, ' ')
        Screen.setBackground(backgroundColor)
        Screen.fill(w.x + 19, w.y + 2, 3, 1, ' ')
        Screen.setBackground(0x2D2D2D)
        Screen.setForeground(0x888888)
        Screen.set(w.x + 23, w.y + 2, "C=Color X=Swap")
        
        // Undo/Redo indicators
        Screen.setForeground(if (undoStack.isNotEmpty()) 0xFFFFFF else 0x666666)
        Screen.set(w.x + w.width - 15, w.y + 2, "↶${undoStack.size}")
        Screen.setForeground(if (redoStack.isNotEmpty()) 0xFFFFFF else 0x666666)
        Screen.set(w.x + w.width - 10, w.y + 2, "↷${redoStack.size}")
        
        // Canvas area
        val canvasStartX = w.x + 1
        val canvasStartY = w.y + 3
        val visibleWidth = minOf(canvasWidth, w.width - 2)
        val visibleHeight = minOf(canvasHeight, w.height - 5)
        
        // Draw canvas
        for (y in 0 until visibleHeight) {
            for (x in 0 until visibleWidth) {
                val cx = x + viewOffsetX
                val cy = y + viewOffsetY
                if (cx < canvasWidth && cy < canvasHeight) {
                    Screen.setBackground(canvas[cy][cx])
                    val char = canvasChars[cy][cx]
                    if (char != ' ') {
                        Screen.setForeground(getContrastColor(canvas[cy][cx]))
                        Screen.set(canvasStartX + x, canvasStartY + y, char.toString())
                    } else {
                        Screen.set(canvasStartX + x, canvasStartY + y, " ")
                    }
                }
            }
        }
        
        // Draw cursor
        val cursorScreenX = canvasStartX + cursorX - viewOffsetX
        val cursorScreenY = canvasStartY + cursorY - viewOffsetY
        if (cursorX - viewOffsetX in 0 until visibleWidth && cursorY - viewOffsetY in 0 until visibleHeight) {
            Screen.setBackground(getContrastColor(canvas[cursorY][cursorX]))
            Screen.setForeground(canvas[cursorY][cursorX])
            Screen.set(cursorScreenX, cursorScreenY, if (currentTool == Tool.TEXT) "█" else "╋")
        }
        
        // Drawing preview
        if (isDrawing && currentTool in listOf(Tool.LINE, Tool.RECTANGLE, Tool.FILLED_RECT, Tool.ELLIPSE, Tool.FILLED_ELLIPSE)) {
            Screen.setForeground(0xFFAA00)
            Screen.set(canvasStartX + toolStartX - viewOffsetX, canvasStartY + toolStartY - viewOffsetY, "○")
        }
        
        // Status bar
        Screen.setBackground(0x2D2D2D)
        Screen.fill(w.x, w.y + w.height - 1, w.width, 1, ' ')
        Screen.setForeground(0x888888)
        Screen.set(w.x + 2, w.y + w.height - 1, "${currentTool.name} | ${canvasWidth}x${canvasHeight} | Pos: $cursorX,$cursorY")
        
        // Color picker overlay
        if (showColorPicker) {
            renderColorPicker(w)
        }
        
        // File dialog overlay
        if (showFileDialog) {
            renderFileDialog(w)
        }
        
        // Tool settings overlay
        if (showToolSettings) {
            renderToolSettings(w)
        }
    }
    
    private fun renderColorPicker(w: li.cil.oc.client.os.gui.Window) {
        val pickerX = w.x + 10
        val pickerY = w.y + 5
        val pickerW = 54
        val pickerH = 15
        
        // Background
        Screen.setBackground(0x1A1A1A)
        Screen.fill(pickerX, pickerY, pickerW, pickerH, ' ')
        
        // Border
        Screen.setForeground(0x3C3C3C)
        Screen.drawBorder(pickerX, pickerY, pickerW, pickerH)
        
        // Title
        Screen.setForeground(0xFFFFFF)
        Screen.set(pickerX + 2, pickerY, " Color Picker ")
        
        // Palette
        var px = pickerX + 2
        var py = pickerY + 2
        for ((i, color) in palette.withIndex()) {
            Screen.setBackground(color)
            val marker = if (color == foregroundColor) "▪" else " "
            Screen.setForeground(getContrastColor(color))
            Screen.set(px, py, marker)
            px += 2
            if ((i + 1) % 22 == 0) {
                px = pickerX + 2
                py++
            }
        }
        
        // Current colors
        Screen.setBackground(0x1A1A1A)
        Screen.setForeground(0xFFFFFF)
        Screen.set(pickerX + 2, pickerY + pickerH - 4, "FG:")
        Screen.setBackground(foregroundColor)
        Screen.fill(pickerX + 6, pickerY + pickerH - 4, 4, 1, ' ')
        
        Screen.setBackground(0x1A1A1A)
        Screen.setForeground(0xFFFFFF)
        Screen.set(pickerX + 12, pickerY + pickerH - 4, "BG:")
        Screen.setBackground(backgroundColor)
        Screen.fill(pickerX + 16, pickerY + pickerH - 4, 4, 1, ' ')
        
        // Recent colors
        Screen.setBackground(0x1A1A1A)
        Screen.setForeground(0x888888)
        Screen.set(pickerX + 2, pickerY + pickerH - 2, "Recent:")
        
        px = pickerX + 10
        for (color in recentColors.take(20)) {
            Screen.setBackground(color)
            Screen.set(px, pickerY + pickerH - 2, " ")
            px += 2
        }
        
        // Instructions
        Screen.setBackground(0x1A1A1A)
        Screen.setForeground(0x666666)
        Screen.set(pickerX + 2, pickerY + pickerH - 1, "Arrows=Select Tab=Swap Enter/Esc=Close")
    }
    
    private fun renderFileDialog(w: li.cil.oc.client.os.gui.Window) {
        val dialogX = w.x + 20
        val dialogY = w.y + 10
        val dialogW = 50
        val dialogH = 8
        
        Screen.setBackground(0x1A1A1A)
        Screen.fill(dialogX, dialogY, dialogW, dialogH, ' ')
        Screen.setForeground(0x3C3C3C)
        Screen.drawBorder(dialogX, dialogY, dialogW, dialogH)
        
        Screen.setForeground(0xFFFFFF)
        Screen.set(dialogX + 2, dialogY, " Open/Save Image ")
        
        Screen.setForeground(0x888888)
        Screen.set(dialogX + 2, dialogY + 2, "Path:")
        
        Screen.setBackground(0x333333)
        Screen.fill(dialogX + 2, dialogY + 3, dialogW - 4, 1, ' ')
        Screen.setForeground(0xFFFFFF)
        Screen.set(dialogX + 3, dialogY + 3, currentFilePath + "█")
        
        Screen.setBackground(0x1A1A1A)
        Screen.setForeground(0x666666)
        Screen.set(dialogX + 2, dialogY + 5, "Enter=Open Esc=Cancel")
    }
    
    private fun renderToolSettings(w: li.cil.oc.client.os.gui.Window) {
        val settingsX = w.x + w.width - 25
        val settingsY = w.y + 3
        
        Screen.setBackground(0x2A2A2A)
        Screen.fill(settingsX, settingsY, 24, 10, ' ')
        Screen.setForeground(0xFFFFFF)
        Screen.set(settingsX + 1, settingsY, "Tool Settings")
        
        Screen.setForeground(0x888888)
        Screen.set(settingsX + 1, settingsY + 2, "Brush Size: $brushSize")
        Screen.set(settingsX + 1, settingsY + 3, "+/- to adjust")
    }
    
    private fun getContrastColor(color: Int): Int {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val brightness = (r * 299 + g * 587 + b * 114) / 1000
        return if (brightness > 128) 0x000000 else 0xFFFFFF
    }
}
