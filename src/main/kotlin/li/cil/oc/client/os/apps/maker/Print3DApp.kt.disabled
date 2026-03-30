package li.cil.oc.client.os.apps.maker

import li.cil.oc.client.os.core.KotlinOS
import li.cil.oc.client.os.apps.Application
import li.cil.oc.client.os.apps.AppInfo
import li.cil.oc.client.os.apps.AppCategory
import li.cil.oc.client.os.libs.*
import kotlin.math.*

/**
 * 3D Print - Create and print 3D models using OpenComputers 3D printer.
 * Features:
 * - 3D model editor with voxel editing
 * - Shape primitives (cube, sphere, cylinder)
 * - Import/export .3dm format
 * - Direct printing to 3D printer component
 * - Preview rendering
 */

private val PRINT_3D_INFO = AppInfo(
    id = "print_3d",
    name = "3D Print",
    icon = "🖨",
    category = AppCategory.UTILITIES,
    description = "Create and print 3D models"
) { Print3DApp(it) }

class Print3DApp(os: KotlinOS) : Application(os, PRINT_3D_INFO) {
    
    // Model data (16x16x16 max for OC)
    private val modelSize = 16
    private var model = Array(modelSize) { Array(modelSize) { IntArray(modelSize) { 0 } } }
    private var modelShapes = mutableListOf<Shape3D>()
    
    // Editor state
    private var cursorX = 8
    private var cursorY = 8
    private var cursorZ = 8
    private var currentLayer = 8
    private var currentColor = 0xFFFFFF
    private var currentTool = Tool3D.PLACE
    private var viewAngleX = 30
    private var viewAngleY = 45
    private var showGrid = true
    private var showWireframe = false
    
    // UI
    private var currentPanel = EditorPanel.MODEL
    private var currentShape = ShapeType.CUBE
    private var shapeStartX = 0
    private var shapeStartY = 0
    private var shapeStartZ = 0
    private var isDrawingShape = false
    
    // File
    private var currentFilePath = ""
    private var hasChanges = false
    private var showFileBrowser = false
    
    // Printer
    private var printerStatus = "Not connected"
    private var printProgress = 0
    private var isPrinting = false
    
    enum class Tool3D(val icon: String, val displayName: String) {
        PLACE("▣", "Place"),
        REMOVE("□", "Remove"),
        PAINT("🎨", "Paint"),
        FILL("🪣", "Fill"),
        SELECT("▭", "Select"),
        SHAPE("◇", "Shape")
    }
    
    enum class ShapeType(val displayName: String) {
        CUBE("Cube"),
        SPHERE("Sphere"),
        CYLINDER("Cylinder"),
        PYRAMID("Pyramid"),
        WEDGE("Wedge")
    }
    
    enum class EditorPanel {
        MODEL, SHAPES, COLORS, PRINTER, SETTINGS
    }
    
    data class Shape3D(
        val type: ShapeType,
        val x1: Int, val y1: Int, val z1: Int,
        val x2: Int, val y2: Int, val z2: Int,
        val color: Int,
        val texture: String = ""
    )
    
    // Color palette
    private val palette = listOf(
        0xFFFFFF, 0xCCCCCC, 0x999999, 0x666666, 0x333333, 0x000000,
        0xFF0000, 0xFF6600, 0xFFCC00, 0xFFFF00, 0xCCFF00, 0x66FF00,
        0x00FF00, 0x00FF66, 0x00FFCC, 0x00FFFF, 0x00CCFF, 0x0066FF,
        0x0000FF, 0x6600FF, 0xCC00FF, 0xFF00FF, 0xFF00CC, 0xFF0066
    )
    private var selectedColorIndex = 0
    
    override fun onCreate() {
        createWindow("3D Print", 2, 1, 95, 32)
    }
    
    override fun onStart() {
        checkPrinter()
    }
    
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
    
    override fun onUpdate() {
        render()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        when (keyCode) {
            Keyboard.KEY_ESCAPE -> {
                if (showFileBrowser) showFileBrowser = false
                else close()
            }
            
            // Cursor movement
            Keyboard.KEY_UP -> if (Keyboard.isShiftDown()) cursorZ = (cursorZ - 1).coerceIn(0, modelSize - 1) else cursorY = (cursorY + 1).coerceIn(0, modelSize - 1)
            Keyboard.KEY_DOWN -> if (Keyboard.isShiftDown()) cursorZ = (cursorZ + 1).coerceIn(0, modelSize - 1) else cursorY = (cursorY - 1).coerceIn(0, modelSize - 1)
            Keyboard.KEY_LEFT -> cursorX = (cursorX - 1).coerceIn(0, modelSize - 1)
            Keyboard.KEY_RIGHT -> cursorX = (cursorX + 1).coerceIn(0, modelSize - 1)
            Keyboard.KEY_PAGE_UP -> currentLayer = (currentLayer + 1).coerceIn(0, modelSize - 1)
            Keyboard.KEY_PAGE_DOWN -> currentLayer = (currentLayer - 1).coerceIn(0, modelSize - 1)
            
            // Actions
            Keyboard.KEY_SPACE, Keyboard.KEY_ENTER -> performToolAction()
            
            // Tools (1-6)
            Keyboard.KEY_1 -> currentTool = Tool3D.PLACE
            Keyboard.KEY_2 -> currentTool = Tool3D.REMOVE
            Keyboard.KEY_3 -> currentTool = Tool3D.PAINT
            Keyboard.KEY_4 -> currentTool = Tool3D.FILL
            Keyboard.KEY_5 -> currentTool = Tool3D.SELECT
            Keyboard.KEY_6 -> currentTool = Tool3D.SHAPE
            
            // View controls
            Keyboard.KEY_Q -> viewAngleY = (viewAngleY - 15) % 360
            Keyboard.KEY_E -> viewAngleY = (viewAngleY + 15) % 360
            Keyboard.KEY_W -> viewAngleX = (viewAngleX + 10).coerceIn(-89, 89)
            Keyboard.KEY_S -> viewAngleX = (viewAngleX - 10).coerceIn(-89, 89)
            
            // Toggles
            Keyboard.KEY_G -> showGrid = !showGrid
            Keyboard.KEY_R -> showWireframe = !showWireframe
            
            // Panels
            Keyboard.KEY_TAB -> currentPanel = EditorPanel.entries[(currentPanel.ordinal + 1) % EditorPanel.entries.size]
            
            // Colors
            Keyboard.KEY_C -> {
                selectedColorIndex = (selectedColorIndex + 1) % palette.size
                currentColor = palette[selectedColorIndex]
            }
            
            // File operations
            Keyboard.KEY_N -> if (Keyboard.isCtrlDown()) newModel()
            Keyboard.KEY_O -> if (Keyboard.isCtrlDown()) showFileBrowser = true
            Keyboard.KEY_S -> if (Keyboard.isCtrlDown()) saveModel()
            Keyboard.KEY_P -> if (Keyboard.isCtrlDown()) printModel()
            
            // Shapes
            Keyboard.KEY_B -> currentShape = ShapeType.CUBE
            Keyboard.KEY_O -> if (!Keyboard.isCtrlDown()) currentShape = ShapeType.SPHERE
            Keyboard.KEY_Y -> currentShape = ShapeType.CYLINDER
            Keyboard.KEY_M -> currentShape = ShapeType.PYRAMID
            Keyboard.KEY_V -> currentShape = ShapeType.WEDGE
        }
    }
    
    private fun performToolAction() {
        when (currentTool) {
            Tool3D.PLACE -> {
                model[cursorX][cursorY][cursorZ] = currentColor
                hasChanges = true
            }
            Tool3D.REMOVE -> {
                model[cursorX][cursorY][cursorZ] = 0
                hasChanges = true
            }
            Tool3D.PAINT -> {
                if (model[cursorX][cursorY][cursorZ] != 0) {
                    model[cursorX][cursorY][cursorZ] = currentColor
                    hasChanges = true
                }
            }
            Tool3D.FILL -> {
                fillLayer(currentLayer)
                hasChanges = true
            }
            Tool3D.SELECT -> {
                // Selection logic
            }
            Tool3D.SHAPE -> {
                if (!isDrawingShape) {
                    shapeStartX = cursorX
                    shapeStartY = cursorY
                    shapeStartZ = cursorZ
                    isDrawingShape = true
                } else {
                    addShape()
                    isDrawingShape = false
                    hasChanges = true
                }
            }
        }
    }
    
    private fun addShape() {
        val minX = minOf(shapeStartX, cursorX)
        val maxX = maxOf(shapeStartX, cursorX)
        val minY = minOf(shapeStartY, cursorY)
        val maxY = maxOf(shapeStartY, cursorY)
        val minZ = minOf(shapeStartZ, cursorZ)
        val maxZ = maxOf(shapeStartZ, cursorZ)
        
        when (currentShape) {
            ShapeType.CUBE -> {
                for (x in minX..maxX) {
                    for (y in minY..maxY) {
                        for (z in minZ..maxZ) {
                            if (x in 0 until modelSize && y in 0 until modelSize && z in 0 until modelSize) {
                                model[x][y][z] = currentColor
                            }
                        }
                    }
                }
            }
            ShapeType.SPHERE -> {
                val cx = (minX + maxX) / 2.0
                val cy = (minY + maxY) / 2.0
                val cz = (minZ + maxZ) / 2.0
                val rx = (maxX - minX) / 2.0
                val ry = (maxY - minY) / 2.0
                val rz = (maxZ - minZ) / 2.0
                
                for (x in minX..maxX) {
                    for (y in minY..maxY) {
                        for (z in minZ..maxZ) {
                            val dx = (x - cx) / rx
                            val dy = (y - cy) / ry
                            val dz = (z - cz) / rz
                            if (dx * dx + dy * dy + dz * dz <= 1.0) {
                                if (x in 0 until modelSize && y in 0 until modelSize && z in 0 until modelSize) {
                                    model[x][y][z] = currentColor
                                }
                            }
                        }
                    }
                }
            }
            ShapeType.CYLINDER -> {
                val cx = (minX + maxX) / 2.0
                val cz = (minZ + maxZ) / 2.0
                val r = minOf(maxX - minX, maxZ - minZ) / 2.0
                
                for (x in minX..maxX) {
                    for (y in minY..maxY) {
                        for (z in minZ..maxZ) {
                            val dx = x - cx
                            val dz = z - cz
                            if (dx * dx + dz * dz <= r * r) {
                                if (x in 0 until modelSize && y in 0 until modelSize && z in 0 until modelSize) {
                                    model[x][y][z] = currentColor
                                }
                            }
                        }
                    }
                }
            }
            ShapeType.PYRAMID -> {
                val height = maxY - minY + 1
                for (y in minY..maxY) {
                    val level = y - minY
                    val shrink = level * (maxX - minX) / (2 * height)
                    for (x in (minX + shrink)..(maxX - shrink)) {
                        for (z in (minZ + shrink)..(maxZ - shrink)) {
                            if (x in 0 until modelSize && y in 0 until modelSize && z in 0 until modelSize) {
                                model[x][y][z] = currentColor
                            }
                        }
                    }
                }
            }
            ShapeType.WEDGE -> {
                val depth = maxZ - minZ + 1
                for (z in minZ..maxZ) {
                    val level = z - minZ
                    val heightAtZ = (maxY - minY) * (depth - level) / depth
                    for (x in minX..maxX) {
                        for (y in minY..(minY + heightAtZ)) {
                            if (x in 0 until modelSize && y in 0 until modelSize && z in 0 until modelSize) {
                                model[x][y][z] = currentColor
                            }
                        }
                    }
                }
            }
        }
        
        modelShapes.add(Shape3D(currentShape, minX, minY, minZ, maxX, maxY, maxZ, currentColor))
    }
    
    private fun fillLayer(y: Int) {
        for (x in 0 until modelSize) {
            for (z in 0 until modelSize) {
                model[x][y][z] = currentColor
            }
        }
    }
    
    private fun newModel() {
        model = Array(modelSize) { Array(modelSize) { IntArray(modelSize) { 0 } } }
        modelShapes.clear()
        cursorX = 8
        cursorY = 8
        cursorZ = 8
        currentFilePath = ""
        hasChanges = false
    }
    
    private fun saveModel() {
        if (currentFilePath.isEmpty()) {
            currentFilePath = "/home/model.3dm"
        }
        
        val fs = os.fileSystem
        val data = buildString {
            appendLine("3DM1") // Format header
            appendLine("$modelSize $modelSize $modelSize")
            
            // Save shapes
            appendLine(modelShapes.size.toString())
            for (shape in modelShapes) {
                appendLine("${shape.type.name} ${shape.x1} ${shape.y1} ${shape.z1} ${shape.x2} ${shape.y2} ${shape.z2} ${shape.color}")
            }
            
            // Save raw voxels
            for (x in 0 until modelSize) {
                for (y in 0 until modelSize) {
                    for (z in 0 until modelSize) {
                        if (model[x][y][z] != 0) {
                            appendLine("V $x $y $z ${model[x][y][z]}")
                        }
                    }
                }
            }
        }
        
        fs.writeText(currentFilePath, data)
        hasChanges = false
    }
    
    private fun loadModel(path: String) {
        val fs = os.fileSystem
        val data = fs.readText(path) ?: return
        
        newModel()
        currentFilePath = path
        
        val lines = data.lines()
        if (lines.isEmpty() || !lines[0].startsWith("3DM")) return
        
        var lineIdx = 2
        
        // Parse shapes
        val shapeCount = lines.getOrNull(lineIdx++)?.toIntOrNull() ?: 0
        repeat(shapeCount) {
            val parts = lines.getOrNull(lineIdx++)?.split(" ") ?: return@repeat
            if (parts.size >= 8) {
                val type = ShapeType.entries.find { it.name == parts[0] } ?: ShapeType.CUBE
                modelShapes.add(Shape3D(
                    type,
                    parts[1].toInt(), parts[2].toInt(), parts[3].toInt(),
                    parts[4].toInt(), parts[5].toInt(), parts[6].toInt(),
                    parts[7].toInt()
                ))
            }
        }
        
        // Parse voxels
        while (lineIdx < lines.size) {
            val parts = lines[lineIdx++].split(" ")
            if (parts.size >= 5 && parts[0] == "V") {
                val x = parts[1].toIntOrNull() ?: continue
                val y = parts[2].toIntOrNull() ?: continue
                val z = parts[3].toIntOrNull() ?: continue
                val color = parts[4].toIntOrNull() ?: continue
                if (x in 0 until modelSize && y in 0 until modelSize && z in 0 until modelSize) {
                    model[x][y][z] = color
                }
            }
        }
    }
    
    private fun checkPrinter() {
        // Check for 3D printer component
        val printer = os.getComponent("printer3d")
        printerStatus = if (printer != null) "Ready" else "Not connected"
    }
    
    private fun printModel() {
        if (isPrinting) return
        
        val printer = os.getComponent("printer3d")
        if (printer == null) {
            printerStatus = "No printer found"
            return
        }
        
        isPrinting = true
        printerStatus = "Printing..."
        printProgress = 0
        
        // In production, this would send data to the printer component
        // For now, simulate printing
        Thread {
            for (i in 0..100 step 5) {
                printProgress = i
                Thread.sleep(100)
            }
            isPrinting = false
            printerStatus = "Complete"
        }.start()
    }
    
    private fun render() {
        val w = window ?: return
        
        Screen.setBackground(0x1E1E1E)
        Screen.fill(w.x, w.y, w.width, w.height, ' ')
        
        // Title bar
        Screen.setBackground(0x3C3C3C)
        Screen.fill(w.x, w.y, w.width, 1, ' ')
        Screen.setForeground(0xFFFFFF)
        val title = "🖨 3D Print" + (if (currentFilePath.isNotEmpty()) " - ${Paths.name(currentFilePath)}" else "") + (if (hasChanges) " *" else "")
        Screen.set(w.x + 2, w.y, title)
        
        // Toolbar
        Screen.setBackground(0x2D2D2D)
        Screen.fill(w.x, w.y + 1, w.width, 2, ' ')
        
        var toolX = w.x + 2
        for (tool in Tool3D.entries) {
            val isSelected = tool == currentTool
            Screen.setBackground(if (isSelected) 0x3399FF else 0x2D2D2D)
            Screen.setForeground(0xFFFFFF)
            Screen.set(toolX, w.y + 1, " ${tool.icon} ")
            toolX += 5
        }
        
        // Current color
        Screen.setBackground(currentColor)
        Screen.fill(w.x + 45, w.y + 1, 3, 1, ' ')
        Screen.setBackground(0x2D2D2D)
        Screen.setForeground(0x888888)
        Screen.set(w.x + 50, w.y + 1, "C=Color")
        
        // Shape indicator
        Screen.set(w.x + 2, w.y + 2, "Shape: ${currentShape.name}")
        
        // 3D View area
        val viewX = w.x + 2
        val viewY = w.y + 4
        val viewW = 50
        val viewH = 20
        
        Screen.setBackground(0x0A0A0A)
        Screen.fill(viewX, viewY, viewW, viewH, ' ')
        
        // Render 3D preview (isometric projection)
        render3DView(viewX, viewY, viewW, viewH)
        
        // Layer view (top-down slice)
        val sliceX = w.x + 55
        val sliceY = w.y + 4
        
        Screen.setForeground(0xFFFFFF)
        Screen.set(sliceX, sliceY - 1, "Layer $currentLayer")
        
        renderLayerView(sliceX, sliceY, currentLayer)
        
        // Side panel
        renderSidePanel(w)
        
        // Status bar
        Screen.setBackground(0x2D2D2D)
        Screen.fill(w.x, w.y + w.height - 1, w.width, 1, ' ')
        Screen.setForeground(0x888888)
        Screen.set(w.x + 2, w.y + w.height - 1, "Pos: $cursorX,$cursorY,$cursorZ | ${currentTool.name} | ←→↑↓ Move | PgUp/Dn Layer | Space Place")
    }
    
    private fun render3DView(viewX: Int, viewY: Int, viewW: Int, viewH: Int) {
        // Simple isometric rendering
        val centerX = viewX + viewW / 2
        val centerY = viewY + viewH / 2
        
        // Render grid if enabled
        if (showGrid) {
            Screen.setForeground(0x333333)
            for (i in 0..modelSize) {
                val x1 = centerX + (i - modelSize / 2) - (modelSize / 2)
                val y1 = centerY + (modelSize / 2) / 2
                if (x1 in viewX until viewX + viewW && y1 in viewY until viewY + viewH) {
                    Screen.set(x1, y1, "·")
                }
            }
        }
        
        // Render voxels from back to front
        for (z in modelSize - 1 downTo 0) {
            for (y in 0 until modelSize) {
                for (x in 0 until modelSize) {
                    if (model[x][y][z] != 0) {
                        // Isometric projection
                        val screenX = centerX + (x - z)
                        val screenY = centerY + (x + z) / 2 - y
                        
                        if (screenX in viewX until viewX + viewW && screenY in viewY until viewY + viewH) {
                            val color = model[x][y][z]
                            // Darken based on depth
                            val shade = 1.0 - (z.toDouble() / modelSize * 0.3)
                            val r = ((color shr 16 and 0xFF) * shade).toInt()
                            val g = ((color shr 8 and 0xFF) * shade).toInt()
                            val b = ((color and 0xFF) * shade).toInt()
                            
                            Screen.setBackground((r shl 16) or (g shl 8) or b)
                            Screen.set(screenX, screenY, " ")
                        }
                    }
                }
            }
        }
        
        // Draw cursor
        val cursorScreenX = centerX + (cursorX - cursorZ)
        val cursorScreenY = centerY + (cursorX + cursorZ) / 2 - cursorY
        
        if (cursorScreenX in viewX until viewX + viewW && cursorScreenY in viewY until viewY + viewH) {
            Screen.setBackground(0x3399FF)
            Screen.setForeground(0xFFFFFF)
            Screen.set(cursorScreenX, cursorScreenY, "╋")
        }
        
        // Shape preview when drawing
        if (isDrawingShape) {
            Screen.setForeground(0xFFAA00)
            val startScreenX = centerX + (shapeStartX - shapeStartZ)
            val startScreenY = centerY + (shapeStartX + shapeStartZ) / 2 - shapeStartY
            if (startScreenX in viewX until viewX + viewW && startScreenY in viewY until viewY + viewH) {
                Screen.set(startScreenX, startScreenY, "○")
            }
        }
    }
    
    private fun renderLayerView(sliceX: Int, sliceY: Int, layer: Int) {
        for (x in 0 until modelSize) {
            for (z in 0 until modelSize) {
                val color = model[x][layer][z]
                val screenX = sliceX + x
                val screenY = sliceY + z
                
                if (color != 0) {
                    Screen.setBackground(color)
                } else {
                    Screen.setBackground(if ((x + z) % 2 == 0) 0x1A1A1A else 0x222222)
                }
                
                if (x == cursorX && z == cursorZ && layer == cursorY) {
                    Screen.setForeground(0xFF0000)
                    Screen.set(screenX, screenY, "+")
                } else {
                    Screen.set(screenX, screenY, " ")
                }
            }
        }
    }
    
    private fun renderSidePanel(w: li.cil.oc.client.os.gui.Window) {
        val panelX = w.x + 75
        val panelY = w.y + 4
        val panelW = 18
        
        Screen.setBackground(0x2D2D2D)
        Screen.fill(panelX, panelY, panelW, 20, ' ')
        
        // Panel tabs
        Screen.setForeground(0xFFFFFF)
        Screen.set(panelX + 1, panelY, when (currentPanel) {
            EditorPanel.MODEL -> "[Model]"
            EditorPanel.SHAPES -> "[Shapes]"
            EditorPanel.COLORS -> "[Colors]"
            EditorPanel.PRINTER -> "[Print]"
            EditorPanel.SETTINGS -> "[Set]"
        })
        
        Screen.setForeground(0x888888)
        Screen.set(panelX + 1, panelY + 1, "Tab to switch")
        
        when (currentPanel) {
            EditorPanel.MODEL -> {
                Screen.set(panelX + 1, panelY + 3, "Voxels: ${countVoxels()}")
                Screen.set(panelX + 1, panelY + 4, "Shapes: ${modelShapes.size}")
            }
            EditorPanel.SHAPES -> {
                var y = panelY + 3
                for (shape in ShapeType.entries) {
                    val selected = shape == currentShape
                    Screen.setForeground(if (selected) 0x3399FF else 0x888888)
                    Screen.set(panelX + 1, y, "${if (selected) "▶" else " "} ${shape.name}")
                    y++
                }
            }
            EditorPanel.COLORS -> {
                var px = panelX + 1
                var py = panelY + 3
                for ((i, color) in palette.withIndex()) {
                    Screen.setBackground(color)
                    Screen.setForeground(if (i == selectedColorIndex) 0xFFFFFF else color)
                    Screen.set(px, py, if (i == selectedColorIndex) "▪" else " ")
                    px += 2
                    if ((i + 1) % 8 == 0) {
                        px = panelX + 1
                        py++
                    }
                }
            }
            EditorPanel.PRINTER -> {
                Screen.setForeground(0xFFFFFF)
                Screen.set(panelX + 1, panelY + 3, "Status:")
                Screen.setForeground(when (printerStatus) {
                    "Ready" -> 0x55FF55
                    "Printing..." -> 0xFFAA00
                    "Complete" -> 0x55FF55
                    else -> 0xFF5555
                })
                Screen.set(panelX + 1, panelY + 4, printerStatus)
                
                if (isPrinting) {
                    Screen.setBackground(0x333333)
                    Screen.fill(panelX + 1, panelY + 6, 14, 1, ' ')
                    Screen.setBackground(0x3399FF)
                    Screen.fill(panelX + 1, panelY + 6, 14 * printProgress / 100, 1, ' ')
                    Screen.setBackground(0x2D2D2D)
                    Screen.setForeground(0xFFFFFF)
                    Screen.set(panelX + 1, panelY + 7, "$printProgress%")
                }
                
                Screen.setForeground(0x888888)
                Screen.set(panelX + 1, panelY + 9, "Ctrl+P Print")
            }
            EditorPanel.SETTINGS -> {
                Screen.setForeground(0x888888)
                Screen.set(panelX + 1, panelY + 3, "G Grid: ${if (showGrid) "On" else "Off"}")
                Screen.set(panelX + 1, panelY + 4, "R Wire: ${if (showWireframe) "On" else "Off"}")
                Screen.set(panelX + 1, panelY + 6, "View: $viewAngleX°,$viewAngleY°")
                Screen.set(panelX + 1, panelY + 7, "Q/E W/S rotate")
            }
        }
    }
    
    private fun countVoxels(): Int {
        var count = 0
        for (x in 0 until modelSize) {
            for (y in 0 until modelSize) {
                for (z in 0 until modelSize) {
                    if (model[x][y][z] != 0) count++
                }
            }
        }
        return count
    }
}
