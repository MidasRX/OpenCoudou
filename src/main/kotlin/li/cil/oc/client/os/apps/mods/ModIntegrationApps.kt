package li.cil.oc.client.os.apps.mods

import li.cil.oc.client.os.apps.*
import li.cil.oc.client.os.core.KotlinOS
import li.cil.oc.client.os.gui.*
import li.cil.oc.client.os.libs.Screen
import li.cil.oc.client.os.libs.Color
import kotlin.random.Random

/**
 * IC2 Nuclear Reactor control app.
 * For controlling IndustrialCraft 2 nuclear reactors.
 */
class IC2ReactorsApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "ic2_reactors",
            name = "IC2 Reactors",
            icon = "☢️",
            category = AppCategory.UTILITIES,
            description = "IC2 Nuclear reactor control",
            version = "1.0",
            author = "System"
        ) { IC2ReactorsApp(it) }
    }
    
    private data class Reactor(
        val name: String,
        var enabled: Boolean = false,
        var temperature: Int = 0,
        var maxTemperature: Int = 10000,
        var output: Int = 0,
        var maxOutput: Int = 2048,
        var uraniumRemaining: Int = 100
    )
    
    private val reactors = mutableListOf(
        Reactor("Reactor A"),
        Reactor("Reactor B"),
        Reactor("Reactor C")
    )
    
    private var selectedReactor = 0
    private var autoShutdown = true
    private var shutdownTemp = 8000
    
    override fun onCreate() {
        createWindow("IC2 Reactor Control", 5, 2, 70, 22)
    }
    
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
    
    override fun onUpdate() {
        updateReactors()
        render()
    }
    
    private fun updateReactors() {
        for (reactor in reactors) {
            if (reactor.enabled) {
                reactor.temperature = minOf(reactor.maxTemperature, reactor.temperature + Random.nextInt(50, 150))
                reactor.output = (reactor.maxOutput * (reactor.temperature.toFloat() / reactor.maxTemperature * 0.5f + 0.5f)).toInt()
                reactor.uraniumRemaining = maxOf(0, reactor.uraniumRemaining - 1)
                
                if (reactor.uraniumRemaining == 0 || (autoShutdown && reactor.temperature >= shutdownTemp)) {
                    reactor.enabled = false
                }
            } else {
                reactor.temperature = maxOf(0, reactor.temperature - 100)
                reactor.output = 0
            }
        }
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        when (char.lowercaseChar()) {
            'w' -> selectedReactor = (selectedReactor - 1 + reactors.size) % reactors.size
            's' -> selectedReactor = (selectedReactor + 1) % reactors.size
            't' -> reactors[selectedReactor].enabled = !reactors[selectedReactor].enabled
            'a' -> autoShutdown = !autoShutdown
            '+', '=' -> shutdownTemp = minOf(10000, shutdownTemp + 500)
            '-', '_' -> shutdownTemp = maxOf(1000, shutdownTemp - 500)
        }
    }
    
    private fun render() {
        Screen.setBackground(0x1A1A2E)
        Screen.fill(1, 1, 68, 20, ' ')
        
        // Header
        Screen.setBackground(0x16213E)
        Screen.fill(1, 1, 68, 2, ' ')
        Screen.setForeground(0xFFFF00)
        Screen.set(2, 1, "☢️ IC2 NUCLEAR REACTOR CONTROL")
        Screen.setForeground(0x888888)
        Screen.set(2, 2, "Auto-shutdown: ${if (autoShutdown) "ON" else "OFF"} at ${shutdownTemp}°C")
        
        // Reactor list
        for ((i, reactor) in reactors.withIndex()) {
            val y = 4 + i * 5
            val isSelected = i == selectedReactor
            
            // Reactor border
            Screen.setBackground(if (isSelected) 0x264F78 else 0x222222)
            Screen.fill(2, y, 30, 4, ' ')
            
            // Name and status
            Screen.setForeground(0xFFFFFF)
            Screen.set(3, y, reactor.name)
            Screen.setForeground(if (reactor.enabled) 0x00FF00 else 0xFF0000)
            Screen.set(20, y, if (reactor.enabled) "ONLINE" else "OFFLINE")
            
            // Temperature bar
            Screen.setForeground(0xAAAAAA)
            Screen.set(3, y + 1, "Temp:")
            val tempPercent = reactor.temperature.toFloat() / reactor.maxTemperature
            val tempColor = when {
                tempPercent > 0.8 -> 0xFF0000
                tempPercent > 0.5 -> 0xFFFF00
                else -> 0x00FF00
            }
            drawProgressBar(10, y + 1, 18, tempPercent, tempColor)
            Screen.setForeground(0xFFFFFF)
            Screen.set(29, y + 1, "${reactor.temperature}°")
            
            // Output
            Screen.setForeground(0xAAAAAA)
            Screen.set(3, y + 2, "Out:")
            drawProgressBar(10, y + 2, 18, reactor.output.toFloat() / reactor.maxOutput, 0x00AAFF)
            Screen.setForeground(0xFFFFFF)
            Screen.set(29, y + 2, "${reactor.output}EU")
            
            // Uranium
            Screen.setForeground(0xAAAAAA)
            Screen.set(3, y + 3, "Fuel:")
            drawProgressBar(10, y + 3, 18, reactor.uraniumRemaining / 100f, 0x00FF00)
            Screen.setForeground(0xFFFFFF)
            Screen.set(29, y + 3, "${reactor.uraniumRemaining}%")
        }
        
        // Stats panel
        Screen.setBackground(0x333333)
        Screen.fill(35, 4, 32, 10, ' ')
        Screen.setForeground(0xFFFFFF)
        Screen.set(36, 4, "📊 Total Statistics")
        
        val totalOutput = reactors.sumOf { it.output }
        val avgTemp = reactors.map { it.temperature }.average().toInt()
        val activeCount = reactors.count { it.enabled }
        
        Screen.setForeground(0x00FF00)
        Screen.set(36, 6, "Total Output: ${totalOutput} EU/t")
        Screen.setForeground(0xFFAA00)
        Screen.set(36, 7, "Avg Temp: ${avgTemp}°C")
        Screen.setForeground(0x00AAFF)
        Screen.set(36, 8, "Active: $activeCount / ${reactors.size}")
        
        // Controls
        Screen.setBackground(0x1A1A2E)
        Screen.setForeground(0x888888)
        Screen.set(2, 19, "W/S: Select | T: Toggle | A: Auto-shutdown | +/-: Temp threshold")
    }
    
    private fun drawProgressBar(x: Int, y: Int, width: Int, percent: Float, color: Int) {
        val filled = (width * percent.coerceIn(0f, 1f)).toInt()
        Screen.setBackground(color)
        Screen.fill(x, y, filled, 1, ' ')
        Screen.setBackground(0x444444)
        Screen.fill(x + filled, y, width - filled, 1, ' ')
    }
}

/**
 * Stargate control app.
 * For controlling Stargate mods (SGCraft, etc.)
 */
class StargateApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "stargate",
            name = "Stargate",
            icon = "🌀",
            category = AppCategory.UTILITIES,
            description = "Stargate controller",
            version = "1.0",
            author = "System"
        ) { StargateApp(it) }
    }
    
    private val chevrons = Array(9) { false }
    private var address = ""
    private var connected = false
    private var dialing = false
    private var dialProgress = 0
    private var irisOpen = true
    
    private val savedAddresses = mutableListOf(
        "Earth" to "AABCD1",
        "Abydos" to "BCDEF2",
        "Chulak" to "CDEFG3",
        "Atlantis" to "DEFGH4"
    )
    
    private var selectedAddress = 0
    
    override fun onCreate() {
        createWindow("Stargate Control", 10, 3, 60, 20)
    }
    
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
    
    override fun onUpdate() {
        if (dialing && !connected) {
            dialProgress++
            if (dialProgress % 10 == 0 && dialProgress / 10 <= address.length) {
                chevrons[dialProgress / 10 - 1] = true
            }
            if (dialProgress >= address.length * 10 + 20) {
                connected = true
                dialing = false
            }
        }
        render()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        when (char.lowercaseChar()) {
            'w' -> selectedAddress = (selectedAddress - 1 + savedAddresses.size) % savedAddresses.size
            's' -> selectedAddress = (selectedAddress + 1) % savedAddresses.size
            'd' -> startDial()
            'c' -> closeGate()
            'i' -> irisOpen = !irisOpen
        }
        
        // Direct address input
        if (char.isLetterOrDigit() && address.length < 9) {
            address += char.uppercaseChar()
        } else if (char == '\b' && address.isNotEmpty()) {
            address = address.dropLast(1)
        }
    }
    
    private fun startDial() {
        if (!connected && !dialing && address.isNotEmpty()) {
            dialing = true
            dialProgress = 0
            chevrons.fill(false)
        }
    }
    
    private fun closeGate() {
        connected = false
        dialing = false
        dialProgress = 0
        chevrons.fill(false)
    }
    
    private fun render() {
        Screen.setBackground(0x0A0A1A)
        Screen.fill(1, 1, 58, 18, ' ')
        
        // Stargate visualization
        val centerX = 20
        val centerY = 9
        val radius = 6
        
        // Draw gate ring
        Screen.setForeground(0x555555)
        for (i in 0 until 360 step 10) {
            val rad = Math.toRadians(i.toDouble())
            val x = (centerX + kotlin.math.cos(rad) * radius).toInt()
            val y = (centerY + kotlin.math.sin(rad) * radius / 2).toInt()
            Screen.set(x, y, "○")
        }
        
        // Draw chevrons
        for (i in 0 until 9) {
            val angle = -90 + i * 40
            val rad = Math.toRadians(angle.toDouble())
            val x = (centerX + kotlin.math.cos(rad) * (radius + 1)).toInt()
            val y = (centerY + kotlin.math.sin(rad) * (radius + 1) / 2).toInt()
            
            Screen.setForeground(if (chevrons[i]) 0xFF8800 else 0x333333)
            Screen.set(x, y, "◆")
        }
        
        // Event horizon
        if (connected) {
            Screen.setForeground(0x4488FF)
            for (r in 1..4) {
                for (i in 0 until 360 step 30) {
                    val rad = Math.toRadians(i.toDouble())
                    val x = (centerX + kotlin.math.cos(rad) * r).toInt()
                    val y = (centerY + kotlin.math.sin(rad) * r / 2).toInt()
                    Screen.set(x, y, "~")
                }
            }
        } else if (!irisOpen) {
            Screen.setForeground(0x888888)
            for (r in 1..4) {
                Screen.fill(centerX - r, centerY, r * 2, 1, '▓')
            }
        }
        
        // Status panel
        Screen.setBackground(0x1A1A2E)
        Screen.fill(35, 1, 23, 10, ' ')
        Screen.setForeground(0xFFFFFF)
        Screen.set(36, 1, "Status:")
        
        Screen.setForeground(if (connected) 0x00FF00 else if (dialing) 0xFFFF00 else 0xFF0000)
        Screen.set(36, 2, if (connected) "CONNECTED" else if (dialing) "DIALING..." else "IDLE")
        
        Screen.setForeground(0xAAAAAA)
        Screen.set(36, 4, "Address:")
        Screen.setForeground(0x00AAFF)
        Screen.set(36, 5, address.ifEmpty { "_ _ _ _ _ _" })
        
        Screen.setForeground(0xAAAAAA)
        Screen.set(36, 7, "Iris:")
        Screen.setForeground(if (irisOpen) 0x00FF00 else 0xFF0000)
        Screen.set(42, 7, if (irisOpen) "OPEN" else "CLOSED")
        
        Screen.setForeground(0xAAAAAA)
        Screen.set(36, 9, "Chevrons: ${chevrons.count { it }}/9")
        
        // Saved addresses
        Screen.setBackground(0x222222)
        Screen.fill(35, 12, 23, 6, ' ')
        Screen.setForeground(0xFFFFFF)
        Screen.set(36, 12, "Saved Addresses:")
        
        for ((i, addr) in savedAddresses.withIndex()) {
            Screen.setForeground(if (i == selectedAddress) 0x00AAFF else 0xAAAAAA)
            Screen.set(36, 13 + i, "${addr.first}: ${addr.second}")
        }
        
        // Controls
        Screen.setBackground(0x0A0A1A)
        Screen.setForeground(0x888888)
        Screen.set(2, 17, "W/S: Select | D: Dial | C: Close | I: Iris | Type address")
    }
}

/**
 * Nanomachines control app.
 * For controlling player nanomachines.
 */
class NanomachinesApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "nanomachines",
            name = "Nanomachines",
            icon = "🔬",
            category = AppCategory.UTILITIES,
            description = "Nanomachine controller",
            version = "1.0",
            author = "System"
        ) { NanomachinesApp(it) }
    }
    
    private data class NanoInput(
        val id: Int,
        var enabled: Boolean = false,
        var effect: String = "Unknown"
    )
    
    private val inputs = (1..18).map { NanoInput(it) }.toMutableList()
    private var selectedInput = 0
    private var power = 100
    private var health = 100
    
    private val possibleEffects = listOf(
        "Speed Boost", "Jump Boost", "Night Vision", "Regeneration",
        "Fire Resistance", "Water Breathing", "Invisibility", "Strength",
        "Haste", "Slow Falling", "Damage", "Hunger", "Weakness",
        "Mining Fatigue", "Blindness", "Nausea", "Poison", "Wither"
    )
    
    init {
        inputs.forEachIndexed { i, input ->
            input.effect = possibleEffects[i % possibleEffects.size]
        }
    }
    
    override fun onCreate() {
        createWindow("Nanomachines", 10, 2, 60, 22)
    }
    
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
    
    override fun onUpdate() {
        // Simulate power drain
        if (inputs.any { it.enabled }) {
            power = maxOf(0, power - inputs.count { it.enabled })
            if (power == 0) {
                inputs.forEach { it.enabled = false }
            }
        }
        render()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        when (char.lowercaseChar()) {
            'w' -> selectedInput = (selectedInput - 1 + inputs.size) % inputs.size
            's' -> selectedInput = (selectedInput + 1) % inputs.size
            't' -> inputs[selectedInput].enabled = !inputs[selectedInput].enabled
            'r' -> inputs.forEach { it.enabled = false }
            'c' -> power = 100
        }
    }
    
    private fun render() {
        Screen.setBackground(0x0D1B2A)
        Screen.fill(1, 1, 58, 20, ' ')
        
        // Header
        Screen.setBackground(0x1B263B)
        Screen.fill(1, 1, 58, 2, ' ')
        Screen.setForeground(0x00FF00)
        Screen.set(2, 1, "🔬 NANOMACHINE CONTROL INTERFACE")
        
        // Power bar
        Screen.setForeground(0xAAAAAA)
        Screen.set(2, 2, "Power:")
        drawProgressBar(10, 2, 25, power / 100f, 0x00AAFF)
        Screen.setForeground(0xFFFFFF)
        Screen.set(36, 2, "$power%")
        
        // Health indicator
        Screen.setForeground(0xFF0000)
        Screen.set(45, 2, "❤ $health%")
        
        // Input grid
        Screen.setBackground(0x0D1B2A)
        Screen.setForeground(0xFFFFFF)
        Screen.set(2, 4, "Neural Inputs:")
        
        for (i in 0 until 9) {
            val x = 2 + (i % 3) * 18
            val y = 6 + (i / 3) * 3
            val input = inputs[i]
            val isSelected = i == selectedInput
            
            Screen.setBackground(if (isSelected) 0x264F78 else 0x1B263B)
            Screen.fill(x, y, 16, 2, ' ')
            
            Screen.setForeground(if (input.enabled) 0x00FF00 else 0xFF0000)
            Screen.set(x + 1, y, "●")
            Screen.setForeground(0xFFFFFF)
            Screen.set(x + 3, y, "Input ${input.id}")
            Screen.setForeground(0xAAAAAA)
            Screen.set(x + 1, y + 1, input.effect.take(14))
        }
        
        // Second row of inputs
        for (i in 9 until 18) {
            val x = 2 + ((i - 9) % 3) * 18
            val y = 15 + ((i - 9) / 3)
            val input = inputs[i]
            val isSelected = i == selectedInput
            
            Screen.setBackground(if (isSelected) 0x264F78 else 0x1B263B)
            Screen.fill(x, y, 16, 1, ' ')
            
            Screen.setForeground(if (input.enabled) 0x00FF00 else 0x666666)
            Screen.set(x + 1, y, "●")
            Screen.setForeground(0xAAAAAA)
            Screen.set(x + 3, y, "${input.id}: ${input.effect.take(10)}")
        }
        
        // Active effects
        val activeEffects = inputs.filter { it.enabled }
        Screen.setBackground(0x1B263B)
        Screen.fill(40, 4, 18, 10, ' ')
        Screen.setForeground(0xFFFFFF)
        Screen.set(41, 4, "Active: ${activeEffects.size}")
        
        for ((i, effect) in activeEffects.take(8).withIndex()) {
            Screen.setForeground(0x00FF00)
            Screen.set(41, 6 + i, "• ${effect.effect.take(12)}")
        }
        
        // Controls
        Screen.setBackground(0x0D1B2A)
        Screen.setForeground(0x888888)
        Screen.set(2, 19, "W/S: Select | T: Toggle | R: Reset all | C: Charge")
    }
    
    private fun drawProgressBar(x: Int, y: Int, width: Int, percent: Float, color: Int) {
        val filled = (width * percent.coerceIn(0f, 1f)).toInt()
        Screen.setBackground(color)
        Screen.fill(x, y, filled, 1, ' ')
        Screen.setBackground(0x333333)
        Screen.fill(x + filled, y, width - filled, 1, ' ')
    }
}

/**
 * Multi-screen setup app.
 * For configuring multiple monitors.
 */
class MultiscreenApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "multiscreen",
            name = "Multiscreen",
            icon = "🖥️",
            category = AppCategory.UTILITIES,
            description = "Multi-monitor setup",
            version = "1.0",
            author = "System"
        ) { MultiscreenApp(it) }
    }
    
    private data class ScreenConfig(
        val id: Int,
        var x: Int,
        var y: Int,
        var width: Int,
        var height: Int,
        var enabled: Boolean = true,
        var primary: Boolean = false
    )
    
    private val screens = mutableListOf(
        ScreenConfig(1, 0, 0, 80, 25, primary = true),
        ScreenConfig(2, 80, 0, 80, 25),
        ScreenConfig(3, 0, 25, 80, 25)
    )
    
    private var selectedScreen = 0
    
    override fun onCreate() {
        createWindow("Multiscreen Setup", 10, 3, 60, 20)
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
        val screen = screens[selectedScreen]
        when (char.lowercaseChar()) {
            'w' -> selectedScreen = (selectedScreen - 1 + screens.size) % screens.size
            's' -> selectedScreen = (selectedScreen + 1) % screens.size
            't' -> screen.enabled = !screen.enabled
            'p' -> {
                screens.forEach { it.primary = false }
                screen.primary = true
            }
        }
    }
    
    private fun render() {
        Screen.setBackground(0x1A1A1A)
        Screen.fill(1, 1, 58, 18, ' ')
        
        // Title
        Screen.setForeground(0xFFFFFF)
        Screen.set(2, 1, "🖥️ Multi-Screen Configuration")
        
        // Preview area
        Screen.setBackground(0x333333)
        Screen.fill(2, 3, 40, 10, ' ')
        
        // Draw screen previews
        val scale = 0.2f
        for ((i, scr) in screens.withIndex()) {
            if (!scr.enabled) continue
            
            val px = 2 + (scr.x * scale).toInt()
            val py = 3 + (scr.y * scale).toInt()
            val pw = maxOf(1, (scr.width * scale).toInt())
            val ph = maxOf(1, (scr.height * scale).toInt())
            
            val color = when {
                i == selectedScreen -> 0x4488FF
                scr.primary -> 0x00AA00
                else -> 0x666666
            }
            
            Screen.setBackground(color)
            Screen.fill(px, py, pw, ph, ' ')
            Screen.setForeground(0xFFFFFF)
            Screen.set(px + 1, py, "${scr.id}")
        }
        
        // Screen list
        Screen.setBackground(0x222222)
        Screen.fill(44, 3, 14, 10, ' ')
        Screen.setForeground(0xFFFFFF)
        Screen.set(45, 3, "Screens:")
        
        for ((i, scr) in screens.withIndex()) {
            val isSelected = i == selectedScreen
            Screen.setForeground(when {
                !scr.enabled -> 0x666666
                isSelected -> 0x00AAFF
                scr.primary -> 0x00FF00
                else -> 0xAAAAAA
            })
            val status = when {
                !scr.enabled -> "OFF"
                scr.primary -> "PRI"
                else -> "ON"
            }
            Screen.set(45, 5 + i, "Screen ${scr.id}: $status")
        }
        
        // Selected screen info
        val selected = screens[selectedScreen]
        Screen.setBackground(0x1A1A1A)
        Screen.setForeground(0xFFFFFF)
        Screen.set(2, 14, "Selected: Screen ${selected.id}")
        Screen.setForeground(0xAAAAAA)
        Screen.set(2, 15, "Position: ${selected.x}, ${selected.y}")
        Screen.set(2, 16, "Size: ${selected.width}x${selected.height}")
        Screen.set(30, 15, "Status: ${if (selected.enabled) "Enabled" else "Disabled"}")
        Screen.set(30, 16, "Primary: ${if (selected.primary) "Yes" else "No"}")
        
        // Controls
        Screen.setForeground(0x888888)
        Screen.set(2, 18, "W/S: Select | T: Toggle | P: Set primary")
    }
}

/**
 * Hologram Clock app.
 * Displays time on a holographic projector.
 */
class HoloClockApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "holoclock",
            name = "HoloClock",
            icon = "⏰",
            category = AppCategory.UTILITIES,
            description = "Holographic clock display",
            version = "1.0",
            author = "System"
        ) { HoloClockApp(it) }
    }
    
    private var displayMode = 0 // 0 = digital, 1 = analog, 2 = binary
    private var colorScheme = 0
    private var showSeconds = true
    
    private val colorSchemes = listOf(
        0x00FF00 to "Green",
        0x00FFFF to "Cyan",
        0xFF00FF to "Magenta",
        0xFFFF00 to "Yellow",
        0xFF8800 to "Orange"
    )
    
    // Big digit patterns
    private val digitPatterns = mapOf(
        '0' to listOf("███", "█ █", "█ █", "█ █", "███"),
        '1' to listOf(" █ ", "██ ", " █ ", " █ ", "███"),
        '2' to listOf("███", "  █", "███", "█  ", "███"),
        '3' to listOf("███", "  █", "███", "  █", "███"),
        '4' to listOf("█ █", "█ █", "███", "  █", "  █"),
        '5' to listOf("███", "█  ", "███", "  █", "███"),
        '6' to listOf("███", "█  ", "███", "█ █", "███"),
        '7' to listOf("███", "  █", "  █", "  █", "  █"),
        '8' to listOf("███", "█ █", "███", "█ █", "███"),
        '9' to listOf("███", "█ █", "███", "  █", "███"),
        ':' to listOf("   ", " █ ", "   ", " █ ", "   ")
    )
    
    override fun onCreate() {
        createWindow("HoloClock", 15, 5, 50, 16)
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
            'm' -> displayMode = (displayMode + 1) % 3
            'c' -> colorScheme = (colorScheme + 1) % colorSchemes.size
            's' -> showSeconds = !showSeconds
        }
    }
    
    private fun render() {
        Screen.setBackground(0x000000)
        Screen.fill(1, 1, 48, 14, ' ')
        
        val now = java.time.LocalTime.now()
        val color = colorSchemes[colorScheme].first
        
        when (displayMode) {
            0 -> renderDigital(now, color)
            1 -> renderAnalog(now, color)
            2 -> renderBinary(now, color)
        }
        
        // Mode indicator
        Screen.setForeground(0x888888)
        Screen.set(2, 13, "M: Mode | C: Color | S: Seconds")
        Screen.setForeground(color)
        Screen.set(40, 13, colorSchemes[colorScheme].second)
    }
    
    private fun renderDigital(time: java.time.LocalTime, color: Int) {
        val timeStr = if (showSeconds) {
            "%02d:%02d:%02d".format(time.hour, time.minute, time.second)
        } else {
            "%02d:%02d".format(time.hour, time.minute)
        }
        
        Screen.setForeground(color)
        
        var x = 3
        for (c in timeStr) {
            val pattern = digitPatterns[c] ?: continue
            for ((row, line) in pattern.withIndex()) {
                Screen.set(x, 3 + row, line)
            }
            x += 5
        }
        
        // Date below
        Screen.setForeground(0x888888)
        val date = java.time.LocalDate.now()
        Screen.set(15, 10, date.toString())
    }
    
    private fun renderAnalog(time: java.time.LocalTime, color: Int) {
        val centerX = 24
        val centerY = 6
        val radius = 5
        
        // Clock face
        Screen.setForeground(0x444444)
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30 - 90).toDouble())
            val x = (centerX + kotlin.math.cos(angle) * radius).toInt()
            val y = (centerY + kotlin.math.sin(angle) * radius / 2).toInt()
            Screen.set(x, y, if (i % 3 == 0) "●" else "·")
        }
        
        // Hour hand
        Screen.setForeground(color)
        val hourAngle = Math.toRadians(((time.hour % 12) * 30 + time.minute / 2 - 90).toDouble())
        for (r in 1..3) {
            val x = (centerX + kotlin.math.cos(hourAngle) * r).toInt()
            val y = (centerY + kotlin.math.sin(hourAngle) * r / 2).toInt()
            Screen.set(x, y, "█")
        }
        
        // Minute hand
        val minAngle = Math.toRadians((time.minute * 6 - 90).toDouble())
        for (r in 1..4) {
            val x = (centerX + kotlin.math.cos(minAngle) * r).toInt()
            val y = (centerY + kotlin.math.sin(minAngle) * r / 2).toInt()
            Screen.set(x, y, "▓")
        }
        
        // Second hand
        if (showSeconds) {
            Screen.setForeground(0xFF0000)
            val secAngle = Math.toRadians((time.second * 6 - 90).toDouble())
            val x = (centerX + kotlin.math.cos(secAngle) * 4).toInt()
            val y = (centerY + kotlin.math.sin(secAngle) * 4 / 2).toInt()
            Screen.set(x, y, "·")
        }
        
        // Digital time below
        Screen.setForeground(color)
        Screen.set(18, 11, "%02d:%02d:%02d".format(time.hour, time.minute, time.second))
    }
    
    private fun renderBinary(time: java.time.LocalTime, color: Int) {
        Screen.setForeground(0xAAAAAA)
        Screen.set(5, 2, "H  H   M  M   S  S")
        Screen.set(5, 3, "10 01  10 01  10 01")
        
        val h1 = time.hour / 10
        val h2 = time.hour % 10
        val m1 = time.minute / 10
        val m2 = time.minute % 10
        val s1 = time.second / 10
        val s2 = time.second % 10
        
        val values = listOf(h1, h2, m1, m2, s1, s2)
        
        for ((col, value) in values.withIndex()) {
            for (row in 0 until 4) {
                val bit = (value shr (3 - row)) and 1
                Screen.setForeground(if (bit == 1) color else 0x333333)
                val x = 5 + col * 6 + (if (col >= 2) 1 else 0) + (if (col >= 4) 1 else 0)
                Screen.set(x, 5 + row, if (bit == 1) "●" else "○")
            }
        }
        
        // Time display
        Screen.setForeground(color)
        Screen.set(15, 11, "%02d:%02d:%02d".format(time.hour, time.minute, time.second))
    }
}

/**
 * Camera control app.
 */
class CameraApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "camera",
            name = "Camera",
            icon = "📷",
            category = AppCategory.UTILITIES,
            description = "Camera/scanner control",
            version = "1.0",
            author = "System"
        ) { CameraApp(it) }
    }
    
    private var distance = 5f
    private var scanning = false
    private var scanProgress = 0
    private var lastScan: List<String>? = null
    
    override fun onCreate() {
        createWindow("Camera", 15, 5, 50, 16)
    }
    
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
    
    override fun onUpdate() {
        if (scanning) {
            scanProgress++
            if (scanProgress >= 100) {
                scanning = false
                lastScan = listOf(
                    "Block: minecraft:stone",
                    "Distance: ${distance}m",
                    "Light level: 7",
                    "Entity: None"
                )
            }
        }
        render()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        when (char.lowercaseChar()) {
            's' -> {
                if (!scanning) {
                    scanning = true
                    scanProgress = 0
                }
            }
            '+', '=' -> distance = minOf(32f, distance + 1)
            '-', '_' -> distance = maxOf(1f, distance - 1)
        }
    }
    
    private fun render() {
        Screen.setBackground(0x1A1A1A)
        Screen.fill(1, 1, 48, 14, ' ')
        
        // Viewfinder
        Screen.setBackground(0x000000)
        Screen.fill(2, 2, 25, 10, ' ')
        
        // Crosshair
        Screen.setForeground(0x00FF00)
        Screen.set(14, 7, "+")
        Screen.set(12, 7, "─")
        Screen.set(16, 7, "─")
        Screen.set(14, 6, "│")
        Screen.set(14, 8, "│")
        
        // Distance indicator
        Screen.setForeground(0xFFFF00)
        Screen.set(3, 3, "Distance: ${distance}m")
        
        // Scan progress
        if (scanning) {
            Screen.setForeground(0x00AAFF)
            Screen.set(3, 10, "Scanning: $scanProgress%")
            Screen.setBackground(0x00AAFF)
            Screen.fill(3, 11, scanProgress / 4, 1, ' ')
        }
        
        // Results panel
        Screen.setBackground(0x222222)
        Screen.fill(29, 2, 19, 10, ' ')
        Screen.setForeground(0xFFFFFF)
        Screen.set(30, 2, "Scan Results:")
        
        if (lastScan != null) {
            Screen.setForeground(0xAAAAAA)
            for ((i, line) in lastScan!!.withIndex()) {
                Screen.set(30, 4 + i, line.take(17))
            }
        } else {
            Screen.setForeground(0x666666)
            Screen.set(30, 5, "No scan data")
        }
        
        // Controls
        Screen.setBackground(0x1A1A1A)
        Screen.setForeground(0x888888)
        Screen.set(2, 13, "S: Scan | +/-: Distance")
    }
}

/**
 * Control app - hardware control panel.
 */
class ControlApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "control",
            name = "Control",
            icon = "🎛️",
            category = AppCategory.UTILITIES,
            description = "Hardware control panel",
            version = "1.0",
            author = "System"
        ) { ControlApp(it) }
    }
    
    private data class ControlItem(
        val name: String,
        var value: Int,
        val min: Int,
        val max: Int,
        val type: String // "slider", "toggle", "button"
    )
    
    private val controls = mutableListOf(
        ControlItem("Redstone Output", 0, 0, 15, "slider"),
        ControlItem("Volume", 50, 0, 100, "slider"),
        ControlItem("Brightness", 80, 0, 100, "slider"),
        ControlItem("Auto-Update", 1, 0, 1, "toggle"),
        ControlItem("Network", 1, 0, 1, "toggle"),
        ControlItem("Reboot", 0, 0, 1, "button")
    )
    
    private var selectedControl = 0
    
    override fun onCreate() {
        createWindow("Control Panel", 15, 4, 50, 18)
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
        val control = controls[selectedControl]
        when (char.lowercaseChar()) {
            'w' -> selectedControl = (selectedControl - 1 + controls.size) % controls.size
            's' -> selectedControl = (selectedControl + 1) % controls.size
            'a' -> control.value = maxOf(control.min, control.value - 1)
            'd' -> control.value = minOf(control.max, control.value + 1)
            ' ', '\n' -> {
                when (control.type) {
                    "toggle" -> control.value = if (control.value == 0) 1 else 0
                    "button" -> {
                        // Trigger action
                    }
                }
            }
        }
    }
    
    private fun render() {
        Screen.setBackground(0x1E1E1E)
        Screen.fill(1, 1, 48, 16, ' ')
        
        // Header
        Screen.setForeground(0xFFFFFF)
        Screen.set(2, 1, "🎛️ Hardware Control Panel")
        
        // Controls
        for ((i, control) in controls.withIndex()) {
            val y = 3 + i * 2
            val isSelected = i == selectedControl
            
            Screen.setBackground(if (isSelected) 0x264F78 else 0x1E1E1E)
            Screen.fill(2, y, 44, 2, ' ')
            
            Screen.setForeground(0xFFFFFF)
            Screen.set(3, y, control.name)
            
            when (control.type) {
                "slider" -> {
                    val percent = (control.value - control.min).toFloat() / (control.max - control.min)
                    val barWidth = 20
                    val filled = (barWidth * percent).toInt()
                    
                    Screen.setBackground(0x00AAFF)
                    Screen.fill(20, y, filled, 1, ' ')
                    Screen.setBackground(0x444444)
                    Screen.fill(20 + filled, y, barWidth - filled, 1, ' ')
                    
                    Screen.setBackground(if (isSelected) 0x264F78 else 0x1E1E1E)
                    Screen.setForeground(0xFFFFFF)
                    Screen.set(42, y, "${control.value}")
                }
                "toggle" -> {
                    Screen.setForeground(if (control.value == 1) 0x00FF00 else 0xFF0000)
                    Screen.set(35, y, if (control.value == 1) "[ON ]" else "[OFF]")
                }
                "button" -> {
                    Screen.setBackground(0x884422)
                    Screen.setForeground(0xFFFFFF)
                    Screen.set(35, y, "[PRESS]")
                }
            }
        }
        
        // Instructions
        Screen.setBackground(0x1E1E1E)
        Screen.setForeground(0x888888)
        Screen.set(2, 15, "W/S: Select | A/D: Adjust | Space: Toggle/Press")
    }
}
