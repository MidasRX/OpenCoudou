package li.cil.oc.client.os.apps.development

import li.cil.oc.client.os.apps.*
import li.cil.oc.client.os.core.KotlinOS
import li.cil.oc.client.os.gui.*
import li.cil.oc.client.os.libs.Screen
import li.cil.oc.client.os.libs.Color
import kotlin.random.Random

/**
 * Lua REPL - Interactive Lua interpreter.
 */
class LuaApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "lua",
            name = "Lua",
            icon = "🌙",
            category = AppCategory.DEVELOPMENT,
            description = "Lua REPL interpreter",
            version = "1.0",
            author = "System"
        ) { LuaApp(it) }
    }
    
    private val history = mutableListOf<Pair<String, Int>>() // text, color
    private var inputBuffer = ""
    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1
    
    // Simple Lua state simulation
    private val variables = mutableMapOf<String, Any>(
        "os" to "SkibidiOS2",
        "version" to "1.0",
        "_VERSION" to "Lua 5.3 (simulated)"
    )
    
    override fun onCreate() {
        createWindow("Lua REPL", 5, 2, 70, 22)
        history.add("Lua 5.3 (simulated) - SkibidiOS2 Interactive Mode" to 0x00AAFF)
        history.add("Type 'help' for available commands, 'exit' to quit" to 0x888888)
        history.add("" to 0xFFFFFF)
    }
    
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
    
    override fun onUpdate() {
        render()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        when {
            char == '\n' -> executeCommand()
            char == '\b' || keyCode == 0x103 -> {
                if (inputBuffer.isNotEmpty()) {
                    inputBuffer = inputBuffer.dropLast(1)
                }
            }
            keyCode == 0x111 -> { // Up arrow
                if (commandHistory.isNotEmpty() && historyIndex < commandHistory.size - 1) {
                    historyIndex++
                    inputBuffer = commandHistory[commandHistory.size - 1 - historyIndex]
                }
            }
            keyCode == 0x112 -> { // Down arrow
                if (historyIndex > 0) {
                    historyIndex--
                    inputBuffer = commandHistory[commandHistory.size - 1 - historyIndex]
                } else if (historyIndex == 0) {
                    historyIndex = -1
                    inputBuffer = ""
                }
            }
            char.code >= 32 -> {
                inputBuffer += char
            }
        }
    }
    
    private fun executeCommand() {
        val cmd = inputBuffer.trim()
        if (cmd.isEmpty()) return
        
        history.add("> $cmd" to 0x00FF00)
        commandHistory.add(cmd)
        historyIndex = -1
        inputBuffer = ""
        
        val result = evaluateLua(cmd)
        if (result != null) {
            history.add(result.first to result.second)
        }
        
        // Trim old history
        while (history.size > 100) {
            history.removeAt(0)
        }
    }
    
    private fun evaluateLua(code: String): Pair<String, Int>? {
        // Command handlers
        when {
            code == "help" -> return """
                |Commands:
                |  help        - Show this help
                |  exit        - Close Lua REPL
                |  clear       - Clear screen
                |  print(x)    - Print value
                |  type(x)     - Get type
                |  tostring(x) - Convert to string
                |  tonumber(x) - Convert to number
                |  math.*      - Math functions
                |  Variables: x = value
            """.trimMargin() to 0xAAAAAA
            
            code == "exit" -> {
                os.closeApplication(appInfo.id)
                return null
            }
            
            code == "clear" -> {
                history.clear()
                return null
            }
            
            code.startsWith("print(") && code.endsWith(")") -> {
                val arg = code.substring(6, code.length - 1).trim()
                val value = evaluateExpression(arg)
                return value.toString() to 0xFFFFFF
            }
            
            code.startsWith("type(") && code.endsWith(")") -> {
                val arg = code.substring(5, code.length - 1).trim()
                val value = evaluateExpression(arg)
                val type = when (value) {
                    is String -> "string"
                    is Number -> "number"
                    is Boolean -> "boolean"
                    null -> "nil"
                    else -> "table"
                }
                return type to 0xFFFF00
            }
            
            code.contains("=") && !code.contains("==") -> {
                val parts = code.split("=", limit = 2)
                if (parts.size == 2) {
                    val name = parts[0].trim()
                    val value = evaluateExpression(parts[1].trim())
                    variables[name] = value ?: "nil"
                    return null
                }
            }
            
            code.startsWith("math.") -> {
                val func = code.substringAfter("math.").substringBefore("(")
                val argStr = code.substringAfter("(").substringBefore(")")
                val arg = argStr.toDoubleOrNull() ?: 0.0
                
                val result = when (func) {
                    "sin" -> kotlin.math.sin(arg)
                    "cos" -> kotlin.math.cos(arg)
                    "tan" -> kotlin.math.tan(arg)
                    "sqrt" -> kotlin.math.sqrt(arg)
                    "abs" -> kotlin.math.abs(arg)
                    "floor" -> kotlin.math.floor(arg)
                    "ceil" -> kotlin.math.ceil(arg)
                    "random" -> Random.nextDouble()
                    "pi" -> kotlin.math.PI
                    else -> return "Unknown math function: $func" to 0xFF0000
                }
                return result.toString() to 0xFFFF00
            }
        }
        
        // Try to evaluate as expression
        val result = evaluateExpression(code)
        return if (result != null) {
            result.toString() to 0xFFFFFF
        } else {
            null
        }
    }
    
    private fun evaluateExpression(expr: String): Any? {
        val trimmed = expr.trim()
        
        // String literal
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length - 1)
        }
        
        // Number
        trimmed.toDoubleOrNull()?.let { return it }
        
        // Boolean
        if (trimmed == "true") return true
        if (trimmed == "false") return false
        if (trimmed == "nil") return null
        
        // Variable
        if (variables.containsKey(trimmed)) {
            return variables[trimmed]
        }
        
        // Simple arithmetic
        if (trimmed.contains("+")) {
            val parts = trimmed.split("+").map { it.trim() }
            val nums = parts.mapNotNull { evaluateExpression(it) as? Number }
            if (nums.size == parts.size) {
                return nums.sumOf { it.toDouble() }
            }
        }
        
        if (trimmed.contains("-") && !trimmed.startsWith("-")) {
            val parts = trimmed.split("-").map { it.trim() }
            if (parts.size == 2) {
                val a = (evaluateExpression(parts[0]) as? Number)?.toDouble()
                val b = (evaluateExpression(parts[1]) as? Number)?.toDouble()
                if (a != null && b != null) return a - b
            }
        }
        
        if (trimmed.contains("*")) {
            val parts = trimmed.split("*").map { it.trim() }
            val nums = parts.mapNotNull { evaluateExpression(it) as? Number }
            if (nums.size == parts.size) {
                return nums.fold(1.0) { acc, n -> acc * n.toDouble() }
            }
        }
        
        if (trimmed.contains("/")) {
            val parts = trimmed.split("/").map { it.trim() }
            if (parts.size == 2) {
                val a = (evaluateExpression(parts[0]) as? Number)?.toDouble()
                val b = (evaluateExpression(parts[1]) as? Number)?.toDouble()
                if (a != null && b != null && b != 0.0) return a / b
            }
        }
        
        return null
    }
    
    private fun render() {
        Screen.setBackground(0x0A0A0A)
        Screen.fill(1, 1, 68, 20, ' ')
        
        // Output
        val visibleLines = 17
        val startLine = maxOf(0, history.size - visibleLines)
        for ((i, line) in history.drop(startLine).take(visibleLines).withIndex()) {
            Screen.setForeground(line.second)
            Screen.set(2, 1 + i, line.first.take(66))
        }
        
        // Input line
        Screen.setBackground(0x1A1A1A)
        Screen.fill(1, 19, 68, 1, ' ')
        Screen.setForeground(0x00FF00)
        Screen.set(2, 19, "> ")
        Screen.setForeground(0xFFFFFF)
        Screen.set(4, 19, inputBuffer.takeLast(62))
        
        // Cursor
        val cursorX = 4 + minOf(inputBuffer.length, 62)
        Screen.setForeground(0xFFFFFF)
        Screen.set(cursorX, 19, "_")
    }
}

/**
 * Sample app - Demo/template application.
 */
class SampleApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "sample",
            name = "Sample",
            icon = "📝",
            category = AppCategory.DEVELOPMENT,
            description = "Sample template app",
            version = "1.0",
            author = "System"
        ) { SampleApp(it) }
    }
    
    private var counter = 0
    private var demoMode = 0
    
    override fun onCreate() {
        createWindow("Sample App", 20, 5, 40, 16)
    }
    
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
    
    override fun onUpdate() {
        counter++
        render()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        when (char.lowercaseChar()) {
            'm' -> demoMode = (demoMode + 1) % 3
            'r' -> counter = 0
        }
    }
    
    private fun render() {
        Screen.setBackground(0x2D2D2D)
        Screen.fill(1, 1, 38, 14, ' ')
        
        Screen.setForeground(0xFFFFFF)
        Screen.set(2, 1, "Sample Application")
        
        Screen.setForeground(0x888888)
        Screen.set(2, 3, "This is a template for")
        Screen.set(2, 4, "creating new applications.")
        
        Screen.setForeground(0x00FF00)
        Screen.set(2, 6, "Frame: $counter")
        Screen.set(2, 7, "Mode: $demoMode")
        
        // Demo visualization
        when (demoMode) {
            0 -> {
                Screen.setForeground(0xFF0000)
                val x = 10 + (kotlin.math.sin(counter * 0.1) * 10).toInt()
                Screen.set(x, 9, "●")
            }
            1 -> {
                for (i in 0 until 20) {
                    val c = ((counter + i * 10) % 256)
                    Screen.setForeground(Color.fromRGB(c, 255 - c, 128))
                    Screen.set(5 + i, 9, "█")
                }
            }
            2 -> {
                Screen.setForeground(0xFFFF00)
                val progress = (counter % 100) / 100f
                val filled = (30 * progress).toInt()
                Screen.fill(4, 9, filled, 1, '█')
            }
        }
        
        Screen.setForeground(0x666666)
        Screen.set(2, 12, "M: Change mode | R: Reset")
    }
}

/**
 * Reinstall OS app.
 */
class ReinstallOSApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "reinstall",
            name = "Reinstall OS",
            icon = "🔄",
            category = AppCategory.SYSTEM,
            description = "Reinstall operating system",
            version = "1.0",
            author = "System"
        ) { ReinstallOSApp(it) }
    }
    
    private var confirmStep = 0
    private var installing = false
    private var progress = 0
    
    override fun onCreate() {
        createWindow("Reinstall OS", 20, 6, 40, 14)
    }
    
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
    
    override fun onUpdate() {
        if (installing) {
            progress++
            if (progress >= 100) {
                installing = false
                confirmStep = 3 // Done
            }
        }
        render()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        if (installing) return
        
        when (char.lowercaseChar()) {
            'y' -> {
                if (confirmStep < 2) {
                    confirmStep++
                    if (confirmStep == 2) {
                        installing = true
                        progress = 0
                    }
                }
            }
            'n' -> {
                os.closeApplication(appInfo.id)
            }
        }
    }
    
    private fun render() {
        Screen.setBackground(0x2D0000)
        Screen.fill(1, 1, 38, 12, ' ')
        
        Screen.setForeground(0xFFFFFF)
        Screen.set(10, 1, "⚠️ Reinstall OS ⚠️")
        
        when (confirmStep) {
            0 -> {
                Screen.setForeground(0xFFAA00)
                Screen.set(2, 4, "This will reinstall the OS")
                Screen.set(2, 5, "and reset all settings.")
                Screen.set(2, 7, "Your files will be kept.")
                
                Screen.setForeground(0xFFFFFF)
                Screen.set(8, 10, "Continue? Y/N")
            }
            1 -> {
                Screen.setForeground(0xFF0000)
                Screen.set(2, 4, "⚠️ FINAL WARNING ⚠️")
                Screen.set(2, 6, "Are you absolutely sure?")
                Screen.set(2, 7, "This cannot be undone!")
                
                Screen.setForeground(0xFFFFFF)
                Screen.set(8, 10, "Confirm? Y/N")
            }
            2 -> {
                Screen.setForeground(0x00AAFF)
                Screen.set(2, 4, "Reinstalling OS...")
                Screen.set(2, 6, "Progress: $progress%")
                
                Screen.setBackground(0x00AAFF)
                Screen.fill(2, 8, progress * 34 / 100, 1, ' ')
                Screen.setBackground(0x444444)
                Screen.fill(2 + progress * 34 / 100, 8, 34 - progress * 34 / 100, 1, ' ')
                
                Screen.setBackground(0x2D0000)
                Screen.setForeground(0xFFAA00)
                Screen.set(2, 10, "Please wait...")
            }
            3 -> {
                Screen.setForeground(0x00FF00)
                Screen.set(5, 4, "✓ Installation Complete!")
                Screen.set(2, 6, "The system will restart.")
                
                Screen.setForeground(0xFFFFFF)
                Screen.set(8, 10, "Press any key...")
            }
        }
    }
}

/**
 * Print Image app - Print images to printer.
 */
class PrintImageApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "print_image",
            name = "Print Image",
            icon = "🖨️",
            category = AppCategory.MEDIA,
            description = "Print images to 3D printer",
            version = "1.0",
            author = "System"
        ) { PrintImageApp(it) }
    }
    
    private var selectedImage: String? = null
    private val images = listOf("photo1.pic", "logo.pic", "diagram.pic", "map.pic")
    private var selectedIndex = 0
    private var printing = false
    private var printProgress = 0
    
    override fun onCreate() {
        createWindow("Print Image", 15, 5, 50, 16)
    }
    
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
    
    override fun onUpdate() {
        if (printing) {
            printProgress++
            if (printProgress >= 100) {
                printing = false
            }
        }
        render()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        if (printing) return
        
        when (char.lowercaseChar()) {
            'w' -> selectedIndex = maxOf(0, selectedIndex - 1)
            's' -> selectedIndex = minOf(images.size - 1, selectedIndex + 1)
            'p' -> {
                printing = true
                printProgress = 0
            }
        }
    }
    
    private fun render() {
        Screen.setBackground(0x1E1E1E)
        Screen.fill(1, 1, 48, 14, ' ')
        
        Screen.setForeground(0xFFFFFF)
        Screen.set(2, 1, "🖨️ Print Image")
        
        // Image list
        Screen.setForeground(0x888888)
        Screen.set(2, 3, "Select image:")
        
        for ((i, img) in images.withIndex()) {
            Screen.setForeground(if (i == selectedIndex) 0x00AAFF else 0xAAAAAA)
            Screen.set(3, 5 + i, "${if (i == selectedIndex) "> " else "  "}$img")
        }
        
        // Preview area
        Screen.setBackground(0x333333)
        Screen.fill(25, 3, 22, 8, ' ')
        Screen.setForeground(0x888888)
        Screen.set(30, 6, "Preview")
        Screen.set(28, 7, "[No preview]")
        
        // Print button/progress
        Screen.setBackground(0x1E1E1E)
        if (printing) {
            Screen.setForeground(0x00AAFF)
            Screen.set(2, 11, "Printing: $printProgress%")
            Screen.setBackground(0x00AAFF)
            Screen.fill(2, 12, printProgress * 44 / 100, 1, ' ')
        } else {
            Screen.setForeground(0x00FF00)
            Screen.set(2, 12, "P: Print selected image")
        }
        
        Screen.setBackground(0x1E1E1E)
        Screen.setForeground(0x666666)
        Screen.set(2, 13, "W/S: Select | P: Print")
    }
}

/**
 * Running String - Scrolling text display.
 */
class RunningStringApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "running_string",
            name = "Running String",
            icon = "📜",
            category = AppCategory.UTILITIES,
            description = "Scrolling text display",
            version = "1.0",
            author = "System"
        ) { RunningStringApp(it) }
    }
    
    private var text = "Welcome to SkibidiOS2! This is a scrolling text demo. "
    private var offset = 0
    private var speed = 1
    private var direction = 1 // 1 = left, -1 = right
    private var fontSize = 1 // 1 = normal, 2 = big
    private var colorMode = 0
    
    override fun onCreate() {
        createWindow("Running String", 10, 6, 60, 12)
    }
    
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
    
    override fun onUpdate() {
        offset = (offset + speed * direction) % (text.length * 2)
        if (offset < 0) offset += text.length * 2
        render()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        when (char.lowercaseChar()) {
            '+', '=' -> speed = minOf(5, speed + 1)
            '-', '_' -> speed = maxOf(1, speed - 1)
            'r' -> direction = -direction
            'c' -> colorMode = (colorMode + 1) % 4
            'f' -> fontSize = if (fontSize == 1) 2 else 1
        }
        
        // Allow text input with shift
        if (char.isLetterOrDigit() || char in " !?.") {
            // Could add text editing
        }
    }
    
    private fun render() {
        Screen.setBackground(0x000000)
        Screen.fill(1, 1, 58, 10, ' ')
        
        // Scrolling text
        val displayText = text + text // Double for seamless scroll
        val visibleWidth = if (fontSize == 1) 55 else 27
        val startIdx = offset % text.length
        val visibleText = displayText.substring(startIdx, minOf(startIdx + visibleWidth, displayText.length))
        
        val y = if (fontSize == 1) 4 else 3
        
        for ((i, c) in visibleText.withIndex()) {
            val color = when (colorMode) {
                0 -> 0x00FF00
                1 -> Color.fromHSV((offset + i * 10) % 360 / 360.0, 1.0, 1.0)
                2 -> if ((offset + i) % 2 == 0) 0xFF0000 else 0xFFFF00
                3 -> 0xFFFFFF
                else -> 0x00FF00
            }
            Screen.setForeground(color)
            
            if (fontSize == 1) {
                Screen.set(2 + i, y, c.toString())
            } else {
                // Big text simulation
                Screen.set(2 + i * 2, y, c.toString())
                Screen.set(2 + i * 2, y + 1, c.toString())
            }
        }
        
        // Controls
        Screen.setForeground(0x666666)
        Screen.set(2, 9, "+/-: Speed ($speed) | R: Reverse | C: Color | F: Font size")
    }
}
