package li.cil.oc.client.os.apps.utilities

import li.cil.oc.client.os.apps.*
import li.cil.oc.client.os.core.KotlinOS
import li.cil.oc.client.os.gui.*
import li.cil.oc.client.os.libs.Screen
import li.cil.oc.client.os.libs.Color
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.*

/**
 * Calculator app with scientific functions.
 */
class CalculatorApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "calculator",
            name = "Calculator",
            icon = "🔢",
            category = AppCategory.UTILITIES,
            description = "Scientific calculator",
            version = "1.0",
            author = "System"
        ) { CalculatorApp(it) }
    }
    
    private var display = "0"
    private var currentValue = 0.0
    private var pendingOperation: String? = null
    private var newNumber = true
    private var memory = 0.0
    private var history = mutableListOf<String>()
    private var scientificMode = false
    
    private val buttons = listOf(
        listOf("MC", "MR", "M+", "M-", "C"),
        listOf("7", "8", "9", "/", "√"),
        listOf("4", "5", "6", "*", "x²"),
        listOf("1", "2", "3", "-", "1/x"),
        listOf("0", ".", "±", "+", "=")
    )
    
    private val scientificButtons = listOf(
        listOf("sin", "cos", "tan", "π", "e"),
        listOf("log", "ln", "exp", "^", "!")
    )
    
    override fun onCreate() {
        createWindow("Calculator", 25, 5, 30, 20)
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
            char.isDigit() -> inputDigit(char.toString())
            char == '.' -> inputDigit(".")
            char == '+' -> setOperation("+")
            char == '-' -> setOperation("-")
            char == '*' -> setOperation("*")
            char == '/' -> setOperation("/")
            char == '=' || char == '\n' -> calculate()
            char == 'c' || char == 'C' -> clear()
            char == 's' || char == 'S' -> scientificMode = !scientificMode
        }
    }
    
    private fun inputDigit(digit: String) {
        if (newNumber) {
            display = if (digit == ".") "0." else digit
            newNumber = false
        } else {
            if (digit == "." && display.contains(".")) return
            if (display.length < 15) {
                display += digit
            }
        }
    }
    
    private fun setOperation(op: String) {
        if (pendingOperation != null) {
            calculate()
        }
        currentValue = display.toDoubleOrNull() ?: 0.0
        pendingOperation = op
        newNumber = true
    }
    
    private fun calculate() {
        val value = display.toDoubleOrNull() ?: return
        val result = when (pendingOperation) {
            "+" -> currentValue + value
            "-" -> currentValue - value
            "*" -> currentValue * value
            "/" -> if (value != 0.0) currentValue / value else Double.NaN
            "^" -> currentValue.pow(value)
            else -> value
        }
        
        if (pendingOperation != null) {
            history.add("$currentValue $pendingOperation $value = $result")
            if (history.size > 5) history.removeAt(0)
        }
        
        display = formatNumber(result)
        pendingOperation = null
        newNumber = true
    }
    
    private fun clear() {
        display = "0"
        currentValue = 0.0
        pendingOperation = null
        newNumber = true
    }
    
    private fun performFunction(func: String) {
        val value = display.toDoubleOrNull() ?: return
        val result = when (func) {
            "√" -> sqrt(value)
            "x²" -> value * value
            "1/x" -> if (value != 0.0) 1.0 / value else Double.NaN
            "±" -> -value
            "sin" -> sin(Math.toRadians(value))
            "cos" -> cos(Math.toRadians(value))
            "tan" -> tan(Math.toRadians(value))
            "log" -> log10(value)
            "ln" -> ln(value)
            "exp" -> exp(value)
            "!" -> factorial(value.toInt()).toDouble()
            "π" -> PI
            "e" -> E
            else -> value
        }
        display = formatNumber(result)
        newNumber = true
    }
    
    private fun factorial(n: Int): Long {
        return if (n <= 1) 1L else n * factorial(n - 1)
    }
    
    private fun formatNumber(value: Double): String {
        return if (value.isNaN() || value.isInfinite()) "Error"
               else if (value == value.toLong().toDouble()) value.toLong().toString()
               else "%.10g".format(value)
    }
    
    private fun memoryOperation(op: String) {
        val value = display.toDoubleOrNull() ?: return
        when (op) {
            "MC" -> memory = 0.0
            "MR" -> { display = formatNumber(memory); newNumber = true }
            "M+" -> memory += value
            "M-" -> memory -= value
        }
    }
    
    override fun onMouseDown(x: Int, y: Int, button: Int) {
        val relX = x - 1
        val relY = y - 4
        
        if (relY < 0) return
        
        val buttonRow = relY
        val buttonCol = relX / 5
        
        val allButtons = if (scientificMode) scientificButtons + buttons else buttons
        
        if (buttonRow in allButtons.indices && buttonCol in allButtons[buttonRow].indices) {
            val btn = allButtons[buttonRow][buttonCol]
            when (btn) {
                in listOf("MC", "MR", "M+", "M-") -> memoryOperation(btn)
                "C" -> clear()
                "=" -> calculate()
                "+", "-", "*", "/", "^" -> setOperation(btn)
                in listOf("√", "x²", "1/x", "±", "sin", "cos", "tan", "log", "ln", "exp", "!", "π", "e") -> performFunction(btn)
                else -> inputDigit(btn)
            }
        }
    }
    
    private fun render() {
        Screen.setBackground(0x222222)
        Screen.fill(1, 1, 28, 18, ' ')
        
        // Display
        Screen.setBackground(0x004400)
        Screen.setForeground(0x00FF00)
        Screen.fill(1, 1, 28, 2, ' ')
        val displayText = display.takeLast(26)
        Screen.set(28 - displayText.length, 1, displayText)
        
        // Memory indicator
        if (memory != 0.0) {
            Screen.setForeground(0xFFFF00)
            Screen.set(1, 1, "M")
        }
        
        // Pending operation
        Screen.setForeground(0x888888)
        Screen.set(1, 2, pendingOperation ?: " ")
        
        // Mode toggle
        Screen.setForeground(0xAAAAFF)
        Screen.set(20, 2, if (scientificMode) "[SCI]" else "[STD]")
        
        // Buttons
        val allButtons = if (scientificMode) scientificButtons + buttons else buttons
        var y = 4
        for (row in allButtons) {
            var bx = 1
            for (btn in row) {
                val isOperator = btn in listOf("+", "-", "*", "/", "=", "^")
                val isFunction = btn in listOf("√", "x²", "1/x", "±", "sin", "cos", "tan", "log", "ln", "exp", "!", "π", "e")
                val isMemory = btn in listOf("MC", "MR", "M+", "M-")
                
                Screen.setBackground(when {
                    isOperator -> 0x884400
                    isFunction -> 0x444488
                    isMemory -> 0x448844
                    btn == "C" -> 0x884444
                    else -> 0x444444
                })
                Screen.setForeground(0xFFFFFF)
                Screen.fill(bx, y, 4, 1, ' ')
                Screen.set(bx + (4 - btn.length) / 2, y, btn)
                bx += 5
            }
            y++
        }
        
        // History
        if (history.isNotEmpty()) {
            Screen.setBackground(0x222222)
            Screen.setForeground(0x666666)
            Screen.set(1, 17, "S: Sci mode")
        }
    }
}

/**
 * Calendar app with month view.
 */
class CalendarApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "calendar",
            name = "Calendar",
            icon = "📅",
            category = AppCategory.UTILITIES,
            description = "Calendar with events",
            version = "1.0",
            author = "System"
        ) { CalendarApp(it) }
    }
    
    private var currentMonth = YearMonth.now()
    private var selectedDay = LocalDate.now().dayOfMonth
    private val events = mutableMapOf<LocalDate, MutableList<String>>()
    
    override fun onCreate() {
        createWindow("Calendar", 15, 3, 50, 20)
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
        when (char.lowercaseChar()) {
            'q' -> currentMonth = currentMonth.minusMonths(1)
            'e' -> currentMonth = currentMonth.plusMonths(1)
            'w' -> selectedDay = maxOf(1, selectedDay - 7)
            's' -> selectedDay = minOf(currentMonth.lengthOfMonth(), selectedDay + 7)
            'a' -> selectedDay = maxOf(1, selectedDay - 1)
            'd' -> selectedDay = minOf(currentMonth.lengthOfMonth(), selectedDay + 1)
            't' -> {
                currentMonth = YearMonth.now()
                selectedDay = LocalDate.now().dayOfMonth
            }
        }
        
        // Clamp selected day to month
        selectedDay = selectedDay.coerceIn(1, currentMonth.lengthOfMonth())
    }
    
    private fun render() {
        Screen.setBackground(0x1A1A2E)
        Screen.fill(1, 1, 48, 18, ' ')
        
        val today = LocalDate.now()
        
        // Header
        Screen.setBackground(0x16213E)
        Screen.fill(1, 1, 48, 2, ' ')
        Screen.setForeground(0xFFFFFF)
        val monthName = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val header = "$monthName ${currentMonth.year}"
        Screen.set(24 - header.length / 2, 1, header)
        
        Screen.setForeground(0x888888)
        Screen.set(2, 2, "< Q")
        Screen.set(44, 2, "E >")
        
        // Day headers
        Screen.setBackground(0x1A1A2E)
        Screen.setForeground(0x888888)
        val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        for ((i, day) in days.withIndex()) {
            Screen.set(2 + i * 6, 4, day)
        }
        
        // Calendar grid
        val firstDay = currentMonth.atDay(1)
        val startOffset = firstDay.dayOfWeek.value % 7
        val daysInMonth = currentMonth.lengthOfMonth()
        
        var day = 1
        for (week in 0 until 6) {
            for (dow in 0 until 7) {
                val cellIndex = week * 7 + dow
                val x = 2 + dow * 6
                val y = 5 + week * 2
                
                if (cellIndex >= startOffset && day <= daysInMonth) {
                    val date = currentMonth.atDay(day)
                    val isToday = date == today
                    val isSelected = day == selectedDay
                    val hasEvents = events[date]?.isNotEmpty() == true
                    
                    if (isSelected) {
                        Screen.setBackground(0x0F4C75)
                    } else if (isToday) {
                        Screen.setBackground(0x3282B8)
                    } else {
                        Screen.setBackground(0x1A1A2E)
                    }
                    
                    Screen.setForeground(if (dow == 0) 0xFF6B6B else 0xFFFFFF)
                    Screen.fill(x, y, 5, 1, ' ')
                    Screen.set(x + 1, y, "%2d".format(day))
                    
                    if (hasEvents) {
                        Screen.setForeground(0xFFFF00)
                        Screen.set(x + 4, y, "•")
                    }
                    
                    day++
                }
            }
        }
        
        // Selected date info
        Screen.setBackground(0x16213E)
        Screen.fill(1, 17, 48, 2, ' ')
        Screen.setForeground(0xFFFFFF)
        val selectedDate = currentMonth.atDay(selectedDay)
        Screen.set(2, 17, "Selected: $selectedDate")
        Screen.setForeground(0x888888)
        Screen.set(2, 18, "WASD: Navigate | Q/E: Month | T: Today")
    }
    
    override fun onMouseDown(x: Int, y: Int, button: Int) {
        val gridX = (x - 2) / 6
        val gridY = (y - 5) / 2
        
        if (gridX in 0..6 && gridY in 0..5) {
            val firstDay = currentMonth.atDay(1)
            val startOffset = firstDay.dayOfWeek.value % 7
            val cellIndex = gridY * 7 + gridX
            val day = cellIndex - startOffset + 1
            
            if (day in 1..currentMonth.lengthOfMonth()) {
                selectedDay = day
            }
        }
    }
}

/**
 * Color Palette app.
 */
class PaletteApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "palette",
            name = "Palette",
            icon = "🎨",
            category = AppCategory.UTILITIES,
            description = "Color palette tool",
            version = "1.0",
            author = "System"
        ) { PaletteApp(it) }
    }
    
    private var selectedColor = 0xFFFFFF
    private var hue = 0.0
    private var saturation = 1.0
    private var value = 1.0
    private var mode = 0 // 0 = HSV, 1 = RGB, 2 = Palette
    
    override fun onCreate() {
        createWindow("Color Palette", 10, 3, 60, 22)
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
        when (char) {
            'h' -> hue = (hue - 0.02).coerceIn(0.0, 1.0)
            'H' -> hue = (hue + 0.02).coerceIn(0.0, 1.0)
            's' -> saturation = (saturation - 0.02).coerceIn(0.0, 1.0)
            'S' -> saturation = (saturation + 0.02).coerceIn(0.0, 1.0)
            'v' -> value = (value - 0.02).coerceIn(0.0, 1.0)
            'V' -> value = (value + 0.02).coerceIn(0.0, 1.0)
            'm', 'M' -> mode = (mode + 1) % 3
        }
        updateColor()
    }
    
    private fun updateColor() {
        selectedColor = Color.fromHSV(hue, saturation, value)
    }
    
    override fun onMouseDown(x: Int, y: Int, button: Int) {
        // Color picker area
        if (x in 2..33 && y in 3..14) {
            saturation = ((x - 2) / 31.0).coerceIn(0.0, 1.0)
            value = 1.0 - ((y - 3) / 11.0).coerceIn(0.0, 1.0)
            updateColor()
        }
        // Hue bar
        if (x in 36..55 && y in 3..14) {
            hue = ((y - 3) / 11.0).coerceIn(0.0, 1.0)
            updateColor()
        }
    }
    
    private fun render() {
        Screen.setBackground(0x222222)
        Screen.fill(1, 1, 58, 20, ' ')
        
        // Title
        Screen.setForeground(0xFFFFFF)
        Screen.set(2, 1, "Color Palette - Mode: ${listOf("HSV", "RGB", "Swatches")[mode]}")
        
        // Saturation/Value picker
        for (py in 0 until 12) {
            for (px in 0 until 32) {
                val s = px / 31.0
                val v = 1.0 - py / 11.0
                val c = Color.fromHSV(hue, s, v)
                Screen.setBackground(c)
                Screen.set(2 + px, 3 + py, " ")
            }
        }
        
        // Hue bar
        for (py in 0 until 12) {
            val h = py / 11.0
            val c = Color.fromHSV(h, 1.0, 1.0)
            Screen.setBackground(c)
            Screen.fill(36, 3 + py, 20, 1, ' ')
        }
        
        // Selected color preview
        Screen.setBackground(selectedColor)
        Screen.fill(2, 16, 20, 3, ' ')
        
        // Color info
        Screen.setBackground(0x333333)
        Screen.setForeground(0xFFFFFF)
        Screen.fill(24, 16, 34, 3, ' ')
        
        val r = (selectedColor shr 16) and 0xFF
        val g = (selectedColor shr 8) and 0xFF
        val b = selectedColor and 0xFF
        
        Screen.set(25, 16, "HEX: #%06X".format(selectedColor))
        Screen.set(25, 17, "RGB: $r, $g, $b")
        Screen.set(25, 18, "HSV: %.0f°, %.0f%%, %.0f%%".format(hue * 360, saturation * 100, value * 100))
        
        // Controls
        Screen.setBackground(0x222222)
        Screen.setForeground(0x888888)
        Screen.set(2, 20, "H/h: Hue | S/s: Sat | V/v: Val | M: Mode | Click to pick")
    }
}

/**
 * HEX Viewer/Editor app.
 */
class HEXViewerApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "hex",
            name = "HEX Viewer",
            icon = "🔍",
            category = AppCategory.UTILITIES,
            description = "Hexadecimal file viewer",
            version = "1.0",
            author = "System"
        ) { HEXViewerApp(it) }
    }
    
    private var data = ByteArray(256) { it.toByte() }
    private var offset = 0
    private var cursorPos = 0
    private var filePath = ""
    private val bytesPerRow = 16
    private val visibleRows = 16
    
    override fun onCreate() {
        createWindow("HEX Viewer", 5, 2, 70, 22)
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
        when (char.lowercaseChar()) {
            'w' -> cursorPos = maxOf(0, cursorPos - bytesPerRow)
            's' -> cursorPos = minOf(data.size - 1, cursorPos + bytesPerRow)
            'a' -> cursorPos = maxOf(0, cursorPos - 1)
            'd' -> cursorPos = minOf(data.size - 1, cursorPos + 1)
        }
        
        // Scroll if needed
        val row = cursorPos / bytesPerRow
        if (row < offset) offset = row
        if (row >= offset + visibleRows) offset = row - visibleRows + 1
    }
    
    private fun render() {
        Screen.setBackground(0x1E1E1E)
        Screen.fill(1, 1, 68, 20, ' ')
        
        // Header
        Screen.setForeground(0x569CD6)
        Screen.set(2, 1, "Offset")
        Screen.set(11, 1, "00 01 02 03 04 05 06 07  08 09 0A 0B 0C 0D 0E 0F")
        Screen.set(62, 1, "ASCII")
        
        // Data rows
        for (row in 0 until minOf(visibleRows, (data.size + bytesPerRow - 1) / bytesPerRow)) {
            val rowOffset = (offset + row) * bytesPerRow
            if (rowOffset >= data.size) break
            
            val y = 3 + row
            
            // Offset
            Screen.setForeground(0x569CD6)
            Screen.set(2, y, "%08X".format(rowOffset))
            
            // Hex bytes
            val hexBuilder = StringBuilder()
            val asciiBuilder = StringBuilder()
            
            for (col in 0 until bytesPerRow) {
                val index = rowOffset + col
                if (index < data.size) {
                    val byte = data[index].toInt() and 0xFF
                    val isCursor = index == cursorPos
                    
                    if (isCursor) {
                        Screen.setBackground(0x264F78)
                    } else {
                        Screen.setBackground(0x1E1E1E)
                    }
                    
                    Screen.setForeground(if (byte == 0) 0x666666 else 0xCE9178)
                    val hexX = 11 + col * 3 + (if (col >= 8) 1 else 0)
                    Screen.set(hexX, y, "%02X".format(byte))
                    
                    // ASCII
                    Screen.setForeground(0xD4D4D4)
                    val char = if (byte in 32..126) byte.toChar() else '.'
                    Screen.set(62 + col, y, char.toString())
                } else {
                    Screen.setBackground(0x1E1E1E)
                    val hexX = 11 + col * 3 + (if (col >= 8) 1 else 0)
                    Screen.set(hexX, y, "  ")
                }
            }
            
            Screen.setBackground(0x1E1E1E)
        }
        
        // Footer
        Screen.setBackground(0x007ACC)
        Screen.setForeground(0xFFFFFF)
        Screen.fill(1, 20, 68, 1, ' ')
        Screen.set(2, 20, "Pos: %08X".format(cursorPos))
        Screen.set(20, 20, "Size: %d bytes".format(data.size))
        Screen.set(45, 20, "WASD: Navigate")
    }
}

/**
 * Symbols picker app.
 */
class SymbolsApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "symbols",
            name = "Symbols",
            icon = "Ω",
            category = AppCategory.UTILITIES,
            description = "Unicode symbol picker",
            version = "1.0",
            author = "System"
        ) { SymbolsApp(it) }
    }
    
    private val categories = mapOf(
        "Arrows" to "←↑→↓↔↕⇐⇑⇒⇓⇔⇕➔➜➞➠➡➢➣➤",
        "Math" to "±×÷=≠≈≡≤≥∞√∑∏∫∂∆∇∈∉∋⊂⊃∩∪",
        "Greek" to "αβγδεζηθικλμνξοπρστυφχψω",
        "Shapes" to "■□▢▣▤▥▦▧▨▩●○◐◑◒◓◔◕",
        "Stars" to "★☆✦✧✨✩✪✫✬✭✮✯✰",
        "Hearts" to "♥♡❤❥❦❧💕💖💗💘💙💚💛",
        "Music" to "♩♪♫♬♭♮♯🎵🎶🎼🎤🎧🎸",
        "Weather" to "☀☁☂☃☄★☆☇☈☉☊☋☌☍",
        "Emoji" to "😀😃😄😁😆😅🤣😂🙂🙃😉😊",
        "Box" to "─│┌┐└┘├┤┬┴┼═║╔╗╚╝╠╣╦╩╬"
    )
    
    private var selectedCategory = 0
    private var selectedIndex = 0
    private var copiedSymbol = ""
    
    override fun onCreate() {
        createWindow("Symbols", 15, 3, 50, 20)
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
        val categoryNames = categories.keys.toList()
        val currentSymbols = categories[categoryNames[selectedCategory]] ?: ""
        
        when (char.lowercaseChar()) {
            'w' -> selectedCategory = (selectedCategory - 1 + categoryNames.size) % categoryNames.size
            's' -> selectedCategory = (selectedCategory + 1) % categoryNames.size
            'a' -> selectedIndex = maxOf(0, selectedIndex - 1)
            'd' -> selectedIndex = minOf(currentSymbols.length - 1, selectedIndex + 1)
            'c', '\n' -> {
                if (selectedIndex < currentSymbols.length) {
                    copiedSymbol = currentSymbols[selectedIndex].toString()
                }
            }
        }
        
        // Clamp index when category changes
        val newSymbols = categories[categoryNames[selectedCategory]] ?: ""
        selectedIndex = selectedIndex.coerceIn(0, maxOf(0, newSymbols.length - 1))
    }
    
    private fun render() {
        Screen.setBackground(0x222222)
        Screen.fill(1, 1, 48, 18, ' ')
        
        val categoryNames = categories.keys.toList()
        
        // Category list
        Screen.setForeground(0xFFFFFF)
        Screen.set(2, 1, "Categories:")
        
        for ((i, name) in categoryNames.withIndex()) {
            val isSelected = i == selectedCategory
            Screen.setBackground(if (isSelected) 0x444488 else 0x222222)
            Screen.setForeground(if (isSelected) 0xFFFFFF else 0xAAAAAA)
            Screen.fill(2, 3 + i, 12, 1, ' ')
            Screen.set(2, 3 + i, name.take(10))
        }
        
        // Symbols grid
        Screen.setBackground(0x333333)
        Screen.fill(16, 1, 32, 14, ' ')
        
        val symbols = categories[categoryNames[selectedCategory]] ?: ""
        Screen.setForeground(0xFFFFFF)
        Screen.set(17, 1, categoryNames[selectedCategory])
        
        for ((i, symbol) in symbols.withIndex()) {
            val x = 17 + (i % 15) * 2
            val y = 3 + i / 15
            
            val isSelected = i == selectedIndex
            Screen.setBackground(if (isSelected) 0x4488FF else 0x333333)
            Screen.setForeground(0xFFFFFF)
            Screen.set(x, y, symbol.toString())
        }
        
        // Selected symbol info
        Screen.setBackground(0x222222)
        Screen.setForeground(0xFFFFFF)
        if (selectedIndex < symbols.length) {
            val symbol = symbols[selectedIndex]
            Screen.set(17, 12, "Selected: $symbol")
            Screen.set(17, 13, "Code: U+%04X".format(symbol.code))
        }
        
        if (copiedSymbol.isNotEmpty()) {
            Screen.setForeground(0x00FF00)
            Screen.set(17, 14, "Copied: $copiedSymbol")
        }
        
        // Controls
        Screen.setForeground(0x888888)
        Screen.set(2, 17, "WASD: Navigate | Enter/C: Copy")
    }
}

/**
 * Console/Terminal app.
 */
class ConsoleApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "console",
            name = "Console",
            icon = ">_",
            category = AppCategory.UTILITIES,
            description = "System console/shell",
            version = "1.0",
            author = "System"
        ) { ConsoleApp(it) }
    }
    
    private val history = mutableListOf<String>()
    private val outputLines = mutableListOf<Pair<String, Int>>() // text, color
    private var inputBuffer = ""
    private var historyIndex = -1
    private val maxLines = 18
    
    override fun onCreate() {
        createWindow("Console", 5, 2, 70, 24)
        outputLines.add("SkibidiOS2 Console v1.0" to 0x00FF00)
        outputLines.add("Type 'help' for available commands." to 0xAAAAAA)
        outputLines.add("" to 0xFFFFFF)
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
                if (history.isNotEmpty() && historyIndex < history.size - 1) {
                    historyIndex++
                    inputBuffer = history[history.size - 1 - historyIndex]
                }
            }
            keyCode == 0x112 -> { // Down arrow
                if (historyIndex > 0) {
                    historyIndex--
                    inputBuffer = history[history.size - 1 - historyIndex]
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
        
        outputLines.add("> $cmd" to 0x00AAFF)
        history.add(cmd)
        historyIndex = -1
        inputBuffer = ""
        
        val parts = cmd.split(" ")
        val command = parts[0].lowercase()
        val args = parts.drop(1)
        
        when (command) {
            "help" -> {
                outputLines.add("Available commands:" to 0xFFFFFF)
                outputLines.add("  help     - Show this help" to 0xAAAAAA)
                outputLines.add("  clear    - Clear screen" to 0xAAAAAA)
                outputLines.add("  echo     - Print text" to 0xAAAAAA)
                outputLines.add("  date     - Show current date/time" to 0xAAAAAA)
                outputLines.add("  info     - System information" to 0xAAAAAA)
                outputLines.add("  ls       - List files" to 0xAAAAAA)
                outputLines.add("  pwd      - Current directory" to 0xAAAAAA)
                outputLines.add("  exit     - Close console" to 0xAAAAAA)
            }
            "clear" -> outputLines.clear()
            "echo" -> outputLines.add(args.joinToString(" ") to 0xFFFFFF)
            "date" -> outputLines.add(LocalDateTime.now().toString() to 0xFFFFFF)
            "info" -> {
                outputLines.add("SkibidiOS2 v1.0" to 0x00FF00)
                outputLines.add("Running on OpenComputers" to 0xAAAAAA)
                outputLines.add("Memory: Simulated" to 0xAAAAAA)
            }
            "ls" -> {
                outputLines.add("bin/  home/  tmp/  var/" to 0x00AAFF)
            }
            "pwd" -> outputLines.add("/home/user" to 0xFFFFFF)
            "exit" -> os.closeApplication(appInfo.id)
            else -> outputLines.add("Unknown command: $command" to 0xFF5555)
        }
        
        // Trim old lines
        while (outputLines.size > 100) {
            outputLines.removeAt(0)
        }
    }
    
    private fun render() {
        Screen.setBackground(0x1A1A1A)
        Screen.fill(1, 1, 68, 22, ' ')
        
        // Output
        val startLine = maxOf(0, outputLines.size - maxLines)
        for ((i, line) in outputLines.drop(startLine).take(maxLines).withIndex()) {
            Screen.setForeground(line.second)
            Screen.set(2, 1 + i, line.first.take(66))
        }
        
        // Input line
        Screen.setBackground(0x333333)
        Screen.fill(1, 21, 68, 1, ' ')
        Screen.setForeground(0x00FF00)
        Screen.set(2, 21, "> ")
        Screen.setForeground(0xFFFFFF)
        Screen.set(4, 21, inputBuffer.takeLast(62))
        
        // Cursor
        val cursorX = 4 + minOf(inputBuffer.length, 62)
        Screen.setForeground(0xFFFFFF)
        Screen.set(cursorX, 21, "_")
    }
}

/**
 * Events viewer app.
 */
class EventsApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "events",
            name = "Events",
            icon = "📋",
            category = AppCategory.UTILITIES,
            description = "System event viewer",
            version = "1.0",
            author = "System"
        ) { EventsApp(it) }
    }
    
    private data class EventLog(
        val timestamp: LocalDateTime,
        val type: String,
        val source: String,
        val message: String
    )
    
    private val events = mutableListOf<EventLog>()
    private var scrollOffset = 0
    private var filterType: String? = null
    
    override fun onCreate() {
        createWindow("Event Viewer", 5, 2, 70, 22)
        
        // Add some sample events
        events.add(EventLog(LocalDateTime.now().minusMinutes(30), "INFO", "System", "OS started"))
        events.add(EventLog(LocalDateTime.now().minusMinutes(25), "INFO", "Network", "Network initialized"))
        events.add(EventLog(LocalDateTime.now().minusMinutes(20), "WARN", "Memory", "Low memory warning"))
        events.add(EventLog(LocalDateTime.now().minusMinutes(15), "INFO", "App", "App Market started"))
        events.add(EventLog(LocalDateTime.now().minusMinutes(10), "ERROR", "Disk", "Read error on sector 42"))
        events.add(EventLog(LocalDateTime.now().minusMinutes(5), "INFO", "User", "User logged in"))
        events.add(EventLog(LocalDateTime.now(), "INFO", "Events", "Event viewer opened"))
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
        val filteredEvents = getFilteredEvents()
        when (char.lowercaseChar()) {
            'w' -> scrollOffset = maxOf(0, scrollOffset - 1)
            's' -> scrollOffset = minOf(maxOf(0, filteredEvents.size - 15), scrollOffset + 1)
            'i' -> filterType = if (filterType == "INFO") null else "INFO"
            'e' -> filterType = if (filterType == "ERROR") null else "ERROR"
            'r' -> filterType = if (filterType == "WARN") null else "WARN"
            'c' -> events.clear()
        }
    }
    
    private fun getFilteredEvents(): List<EventLog> {
        return if (filterType != null) events.filter { it.type == filterType }
               else events
    }
    
    private fun render() {
        Screen.setBackground(0x1E1E1E)
        Screen.fill(1, 1, 68, 20, ' ')
        
        // Header
        Screen.setBackground(0x333333)
        Screen.fill(1, 1, 68, 1, ' ')
        Screen.setForeground(0xFFFFFF)
        Screen.set(2, 1, "Time")
        Screen.set(12, 1, "Type")
        Screen.set(20, 1, "Source")
        Screen.set(32, 1, "Message")
        
        // Filter buttons
        Screen.set(50, 1, "Filter:")
        Screen.setForeground(if (filterType == "INFO") 0x00FF00 else 0x888888)
        Screen.set(58, 1, "[I]")
        Screen.setForeground(if (filterType == "WARN") 0xFFFF00 else 0x888888)
        Screen.set(62, 1, "[W]")
        Screen.setForeground(if (filterType == "ERROR") 0xFF0000 else 0x888888)
        Screen.set(66, 1, "[E]")
        
        // Events
        val filteredEvents = getFilteredEvents()
        for ((i, event) in filteredEvents.drop(scrollOffset).take(16).withIndex()) {
            val y = 3 + i
            Screen.setBackground(if (i % 2 == 0) 0x1E1E1E else 0x252525)
            Screen.fill(1, y, 68, 1, ' ')
            
            Screen.setForeground(0x888888)
            Screen.set(2, y, event.timestamp.toLocalTime().toString().take(8))
            
            Screen.setForeground(when (event.type) {
                "INFO" -> 0x00FF00
                "WARN" -> 0xFFFF00
                "ERROR" -> 0xFF0000
                else -> 0xFFFFFF
            })
            Screen.set(12, y, event.type)
            
            Screen.setForeground(0x569CD6)
            Screen.set(20, y, event.source.take(10))
            
            Screen.setForeground(0xFFFFFF)
            Screen.set(32, y, event.message.take(36))
        }
        
        // Footer
        Screen.setBackground(0x333333)
        Screen.fill(1, 20, 68, 1, ' ')
        Screen.setForeground(0x888888)
        Screen.set(2, 20, "W/S: Scroll | I/W/E: Filter | C: Clear")
        Screen.set(50, 20, "${filteredEvents.size} events")
    }
}

/**
 * Advanced file finder.
 */
class FinderApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "finder",
            name = "Finder",
            icon = "📁",
            category = AppCategory.UTILITIES,
            description = "Advanced file browser",
            version = "1.0",
            author = "System"
        ) { FinderApp(it) }
    }
    
    private data class FileEntry(
        val name: String,
        val isDirectory: Boolean,
        val size: Long,
        val modified: LocalDateTime
    )
    
    private var currentPath = "/home"
    private var files = mutableListOf<FileEntry>()
    private var selectedIndex = 0
    private var scrollOffset = 0
    private var viewMode = 0 // 0 = list, 1 = details, 2 = icons
    
    override fun onCreate() {
        createWindow("Finder", 5, 2, 70, 22)
        loadDirectory()
    }
    
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
    
    private fun loadDirectory() {
        files.clear()
        
        // Simulated file system
        if (currentPath == "/home") {
            files.add(FileEntry("..", true, 0, LocalDateTime.now()))
            files.add(FileEntry("Documents", true, 0, LocalDateTime.now()))
            files.add(FileEntry("Pictures", true, 0, LocalDateTime.now()))
            files.add(FileEntry("Downloads", true, 0, LocalDateTime.now()))
            files.add(FileEntry("config.cfg", false, 1024, LocalDateTime.now()))
            files.add(FileEntry("readme.txt", false, 256, LocalDateTime.now()))
        } else {
            files.add(FileEntry("..", true, 0, LocalDateTime.now()))
            files.add(FileEntry("file1.txt", false, 512, LocalDateTime.now()))
            files.add(FileEntry("file2.lua", false, 2048, LocalDateTime.now()))
        }
        
        selectedIndex = 0
        scrollOffset = 0
    }
    
    override fun onUpdate() {
        render()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        when (char.lowercaseChar()) {
            'w' -> {
                selectedIndex = maxOf(0, selectedIndex - 1)
                if (selectedIndex < scrollOffset) scrollOffset = selectedIndex
            }
            's' -> {
                selectedIndex = minOf(files.size - 1, selectedIndex + 1)
                if (selectedIndex >= scrollOffset + 15) scrollOffset = selectedIndex - 14
            }
            'v' -> viewMode = (viewMode + 1) % 3
        }
        
        if (char == '\n' && selectedIndex < files.size) {
            val file = files[selectedIndex]
            if (file.isDirectory) {
                currentPath = if (file.name == "..") {
                    currentPath.substringBeforeLast("/").ifEmpty { "/" }
                } else {
                    "$currentPath/${file.name}"
                }
                loadDirectory()
            }
        }
    }
    
    private fun render() {
        Screen.setBackground(0x1E1E1E)
        Screen.fill(1, 1, 68, 20, ' ')
        
        // Path bar
        Screen.setBackground(0x333333)
        Screen.fill(1, 1, 68, 1, ' ')
        Screen.setForeground(0xFFFFFF)
        Screen.set(2, 1, "📂 $currentPath")
        Screen.setForeground(0x888888)
        Screen.set(58, 1, listOf("List", "Details", "Icons")[viewMode])
        
        // Column headers (for details view)
        if (viewMode == 1) {
            Screen.setBackground(0x252525)
            Screen.fill(1, 2, 68, 1, ' ')
            Screen.setForeground(0x888888)
            Screen.set(2, 2, "Name")
            Screen.set(40, 2, "Size")
            Screen.set(52, 2, "Modified")
        }
        
        // Files
        val startY = if (viewMode == 1) 3 else 2
        for ((i, file) in files.drop(scrollOffset).take(16).withIndex()) {
            val y = startY + i
            val isSelected = i + scrollOffset == selectedIndex
            
            Screen.setBackground(if (isSelected) 0x264F78 else if (i % 2 == 0) 0x1E1E1E else 0x252525)
            Screen.fill(1, y, 68, 1, ' ')
            
            val icon = if (file.isDirectory) "📁" else when (file.name.substringAfterLast('.')) {
                "txt" -> "📄"
                "lua" -> "📜"
                "cfg" -> "⚙️"
                "pic" -> "🖼️"
                else -> "📄"
            }
            
            Screen.setForeground(if (file.isDirectory) 0x569CD6 else 0xCE9178)
            Screen.set(2, y, "$icon ${file.name}")
            
            if (viewMode == 1) {
                Screen.setForeground(0x888888)
                if (!file.isDirectory) {
                    Screen.set(40, y, formatSize(file.size))
                }
                Screen.set(52, y, file.modified.toLocalDate().toString())
            }
        }
        
        // Footer
        Screen.setBackground(0x333333)
        Screen.fill(1, 20, 68, 1, ' ')
        Screen.setForeground(0x888888)
        Screen.set(2, 20, "W/S: Navigate | Enter: Open | V: View mode")
        Screen.set(55, 20, "${files.size} items")
    }
    
    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
