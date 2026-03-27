package li.cil.oc.client.os.apps.network

import li.cil.oc.client.os.apps.*
import li.cil.oc.client.os.core.KotlinOS
import li.cil.oc.client.os.gui.*
import li.cil.oc.client.os.libs.Screen
import li.cil.oc.client.os.libs.Color
import kotlin.random.Random

/**
 * Weather app with forecast display.
 */
class WeatherApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "weather",
            name = "Weather",
            icon = "🌤️",
            category = AppCategory.UTILITIES,
            description = "Weather forecast",
            version = "1.0",
            author = "System"
        ) { WeatherApp(it) }
    }
    
    private data class WeatherData(
        val day: String,
        val temp: Int,
        val condition: String,
        val humidity: Int,
        val wind: Int
    )
    
    private val forecast = listOf(
        WeatherData("Today", 22, "Sunny", 45, 12),
        WeatherData("Tomorrow", 19, "Cloudy", 60, 15),
        WeatherData("Wed", 17, "Rain", 80, 20),
        WeatherData("Thu", 15, "Storm", 90, 35),
        WeatherData("Fri", 20, "Cloudy", 55, 10),
        WeatherData("Sat", 24, "Sunny", 40, 8),
        WeatherData("Sun", 23, "Sunny", 42, 10)
    )
    
    private var selectedDay = 0
    
    override fun onCreate() {
        createWindow("Weather", 15, 4, 50, 18)
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
            'a' -> selectedDay = maxOf(0, selectedDay - 1)
            'd' -> selectedDay = minOf(forecast.size - 1, selectedDay + 1)
        }
    }
    
    private fun render() {
        val weather = forecast[selectedDay]
        
        // Background based on condition
        val bgColor = when (weather.condition) {
            "Sunny" -> 0x4488FF
            "Cloudy" -> 0x666688
            "Rain" -> 0x444466
            "Storm" -> 0x222244
            else -> 0x4488FF
        }
        Screen.setBackground(bgColor)
        Screen.fill(1, 1, 48, 16, ' ')
        
        // Weather icon
        Screen.setForeground(0xFFFFFF)
        val icon = when (weather.condition) {
            "Sunny" -> "☀️"
            "Cloudy" -> "☁️"
            "Rain" -> "🌧️"
            "Storm" -> "⛈️"
            else -> "🌤️"
        }
        Screen.set(22, 3, icon)
        
        // Temperature
        Screen.setForeground(0xFFFFFF)
        Screen.set(18, 5, "${weather.temp}°C")
        
        // Condition
        Screen.set(18, 7, weather.condition)
        
        // Details
        Screen.setForeground(0xCCCCCC)
        Screen.set(5, 10, "💧 Humidity: ${weather.humidity}%")
        Screen.set(28, 10, "💨 Wind: ${weather.wind} km/h")
        
        // Forecast bar
        Screen.setBackground(0x00000044.toInt())
        Screen.fill(1, 13, 48, 3, ' ')
        
        val dayWidth = 6
        for ((i, day) in forecast.withIndex()) {
            val x = 2 + i * dayWidth
            Screen.setForeground(if (i == selectedDay) 0xFFFF00 else 0xAAAAAA)
            Screen.set(x, 13, day.day.take(3))
            
            val dayIcon = when (day.condition) {
                "Sunny" -> "☀"
                "Cloudy" -> "☁"
                "Rain" -> "🌧"
                "Storm" -> "⛈"
                else -> "🌤"
            }
            Screen.set(x, 14, dayIcon)
            Screen.set(x, 15, "${day.temp}°")
        }
    }
}

/**
 * Translation app.
 */
class TranslateApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "translate",
            name = "Translate",
            icon = "🌐",
            category = AppCategory.UTILITIES,
            description = "Text translator",
            version = "1.0",
            author = "System"
        ) { TranslateApp(it) }
    }
    
    private val languages = listOf("English", "Spanish", "French", "German", "Russian", "Chinese", "Japanese")
    private var sourceLang = 0
    private var targetLang = 1
    private var inputText = ""
    private var translatedText = ""
    
    // Simple mock translations
    private val translations = mapOf(
        "hello" to mapOf("Spanish" to "hola", "French" to "bonjour", "German" to "hallo", "Russian" to "привет"),
        "world" to mapOf("Spanish" to "mundo", "French" to "monde", "German" to "welt", "Russian" to "мир"),
        "computer" to mapOf("Spanish" to "computadora", "French" to "ordinateur", "German" to "computer", "Russian" to "компьютер")
    )
    
    override fun onCreate() {
        createWindow("Translator", 10, 3, 60, 20)
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
            char == '\n' -> translate()
            char == '\b' && inputText.isNotEmpty() -> inputText = inputText.dropLast(1)
            char == '\t' -> {
                val temp = sourceLang
                sourceLang = targetLang
                targetLang = temp
            }
            char.code >= 32 && inputText.length < 100 -> inputText += char
        }
    }
    
    private fun translate() {
        val words = inputText.lowercase().split(" ")
        val targetLanguage = languages[targetLang]
        
        translatedText = words.joinToString(" ") { word ->
            translations[word]?.get(targetLanguage) ?: "[$word]"
        }
    }
    
    private fun render() {
        Screen.setBackground(0x1E1E1E)
        Screen.fill(1, 1, 58, 18, ' ')
        
        // Language selectors
        Screen.setBackground(0x333333)
        Screen.fill(1, 1, 58, 2, ' ')
        Screen.setForeground(0xFFFFFF)
        Screen.set(2, 1, languages[sourceLang])
        Screen.set(27, 1, "⟷")
        Screen.set(35, 1, languages[targetLang])
        Screen.setForeground(0x888888)
        Screen.set(2, 2, "TAB: Swap languages")
        
        // Input area
        Screen.setBackground(0x2D2D2D)
        Screen.fill(2, 4, 55, 5, ' ')
        Screen.setForeground(0xFFFFFF)
        Screen.set(3, 4, "Input:")
        Screen.setForeground(0xAAAAAA)
        
        // Wrap input text
        val inputLines = inputText.chunked(50)
        for ((i, line) in inputLines.take(3).withIndex()) {
            Screen.set(3, 5 + i, line)
        }
        
        // Cursor
        if (inputText.length < 50) {
            Screen.setForeground(0xFFFFFF)
            Screen.set(3 + inputText.length, 5, "_")
        }
        
        // Output area
        Screen.setBackground(0x1A3A1A)
        Screen.fill(2, 10, 55, 5, ' ')
        Screen.setForeground(0x00FF00)
        Screen.set(3, 10, "Translation:")
        Screen.setForeground(0xCCFFCC)
        
        val outputLines = translatedText.chunked(50)
        for ((i, line) in outputLines.take(3).withIndex()) {
            Screen.set(3, 11 + i, line)
        }
        
        // Instructions
        Screen.setBackground(0x1E1E1E)
        Screen.setForeground(0x888888)
        Screen.set(2, 16, "Type text and press ENTER to translate")
        Screen.set(2, 17, "TAB: Swap languages | Backspace: Delete")
    }
}

/**
 * FTP Client app.
 */
class FTPClientApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "ftp",
            name = "FTP Client",
            icon = "📤",
            category = AppCategory.NETWORK,
            description = "FTP file transfer client",
            version = "1.0",
            author = "System"
        ) { FTPClientApp(it) }
    }
    
    private data class RemoteFile(
        val name: String,
        val isDirectory: Boolean,
        val size: Long
    )
    
    private var connected = false
    private var hostname = "ftp.example.com"
    private var username = "anonymous"
    private var currentPath = "/"
    
    private val remoteFiles = mutableListOf<RemoteFile>()
    private var selectedFile = 0
    private var transferProgress = -1
    
    override fun onCreate() {
        createWindow("FTP Client", 5, 2, 70, 22)
    }
    
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
    
    override fun onUpdate() {
        if (transferProgress in 0..99) {
            transferProgress += 5
            if (transferProgress >= 100) transferProgress = -1
        }
        render()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        when (char.lowercaseChar()) {
            'c' -> connect()
            'd' -> disconnect()
            'w' -> if (connected) selectedFile = maxOf(0, selectedFile - 1)
            's' -> if (connected) selectedFile = minOf(remoteFiles.size - 1, selectedFile + 1)
            'g' -> if (connected && selectedFile < remoteFiles.size) startDownload()
            'u' -> if (connected) startUpload()
        }
    }
    
    private fun connect() {
        connected = true
        remoteFiles.clear()
        remoteFiles.add(RemoteFile("..", true, 0))
        remoteFiles.add(RemoteFile("documents", true, 0))
        remoteFiles.add(RemoteFile("images", true, 0))
        remoteFiles.add(RemoteFile("readme.txt", false, 1024))
        remoteFiles.add(RemoteFile("data.zip", false, 52428800))
        selectedFile = 0
    }
    
    private fun disconnect() {
        connected = false
        remoteFiles.clear()
    }
    
    private fun startDownload() {
        transferProgress = 0
    }
    
    private fun startUpload() {
        transferProgress = 0
    }
    
    private fun render() {
        Screen.setBackground(0x1A1A2E)
        Screen.fill(1, 1, 68, 20, ' ')
        
        // Header
        Screen.setBackground(0x16213E)
        Screen.fill(1, 1, 68, 2, ' ')
        Screen.setForeground(if (connected) 0x00FF00 else 0xFF0000)
        Screen.set(2, 1, if (connected) "● Connected" else "○ Disconnected")
        Screen.setForeground(0xFFFFFF)
        Screen.set(20, 1, "Server: $hostname")
        Screen.setForeground(0x888888)
        Screen.set(2, 2, "User: $username")
        Screen.set(25, 2, "Path: $currentPath")
        
        if (!connected) {
            // Connection form
            Screen.setBackground(0x222233)
            Screen.fill(15, 7, 38, 7, ' ')
            Screen.setForeground(0xFFFFFF)
            Screen.set(25, 8, "FTP Connection")
            Screen.setForeground(0xAAAAAA)
            Screen.set(17, 10, "Host: $hostname")
            Screen.set(17, 11, "User: $username")
            Screen.setForeground(0x00AAFF)
            Screen.set(25, 13, "[ C: Connect ]")
        } else {
            // File list
            Screen.setBackground(0x1A1A2E)
            Screen.setForeground(0xFFFFFF)
            Screen.set(2, 4, "Remote Files:")
            
            for ((i, file) in remoteFiles.withIndex()) {
                val y = 5 + i
                if (y > 16) break
                
                val isSelected = i == selectedFile
                Screen.setBackground(if (isSelected) 0x264F78 else 0x1A1A2E)
                Screen.fill(2, y, 40, 1, ' ')
                
                Screen.setForeground(if (file.isDirectory) 0x569CD6 else 0xCE9178)
                val icon = if (file.isDirectory) "📁" else "📄"
                Screen.set(3, y, "$icon ${file.name}")
                
                if (!file.isDirectory) {
                    Screen.setForeground(0x888888)
                    Screen.set(30, y, formatSize(file.size))
                }
            }
            
            // Transfer panel
            Screen.setBackground(0x222233)
            Screen.fill(45, 4, 22, 12, ' ')
            Screen.setForeground(0xFFFFFF)
            Screen.set(46, 4, "Transfer")
            
            if (transferProgress >= 0) {
                Screen.setForeground(0x00AAFF)
                Screen.set(46, 6, "Progress: $transferProgress%")
                Screen.setBackground(0x00AAFF)
                Screen.fill(46, 7, transferProgress / 5, 1, ' ')
                Screen.setBackground(0x444444)
                Screen.fill(46 + transferProgress / 5, 7, 20 - transferProgress / 5, 1, ' ')
            } else {
                Screen.setForeground(0x888888)
                Screen.set(46, 6, "Idle")
            }
            
            Screen.setBackground(0x222233)
            Screen.setForeground(0x888888)
            Screen.set(46, 10, "G: Download")
            Screen.set(46, 11, "U: Upload")
            Screen.set(46, 12, "D: Disconnect")
        }
        
        // Status bar
        Screen.setBackground(0x16213E)
        Screen.fill(1, 19, 68, 1, ' ')
        Screen.setForeground(0x888888)
        Screen.set(2, 19, "W/S: Navigate | C: Connect | D: Disconnect | G: Get | U: Upload")
    }
    
    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}

/**
 * Graph/Chart visualization app.
 */
class GraphApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "graph",
            name = "Graph",
            icon = "📈",
            category = AppCategory.UTILITIES,
            description = "Data visualization",
            version = "1.0",
            author = "System"
        ) { GraphApp(it) }
    }
    
    private var chartType = 0 // 0 = line, 1 = bar, 2 = pie
    private val data = listOf(25, 40, 35, 60, 45, 80, 55, 70, 65, 90, 75, 85)
    private val labels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val colors = listOf(0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00, 0xFF00FF, 0x00FFFF)
    
    override fun onCreate() {
        createWindow("Graph", 10, 3, 60, 20)
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
            't' -> chartType = (chartType + 1) % 3
        }
    }
    
    private fun render() {
        Screen.setBackground(0x1E1E1E)
        Screen.fill(1, 1, 58, 18, ' ')
        
        // Title
        Screen.setForeground(0xFFFFFF)
        val chartName = listOf("Line Chart", "Bar Chart", "Pie Chart")[chartType]
        Screen.set(2, 1, "📈 $chartName (T: Change type)")
        
        when (chartType) {
            0 -> renderLineChart()
            1 -> renderBarChart()
            2 -> renderPieChart()
        }
    }
    
    private fun renderLineChart() {
        val chartX = 5
        val chartY = 3
        val chartW = 50
        val chartH = 12
        
        // Y axis
        Screen.setForeground(0x888888)
        for (y in 0..chartH) {
            Screen.set(chartX - 1, chartY + y, "│")
        }
        Screen.set(chartX - 1, chartY + chartH, "└")
        
        // X axis
        for (x in 0..chartW) {
            Screen.set(chartX + x, chartY + chartH, "─")
        }
        
        // Data points and lines
        Screen.setForeground(0x00FF00)
        val maxVal = data.maxOrNull() ?: 1
        
        var prevX = -1
        var prevY = -1
        
        for ((i, value) in data.withIndex()) {
            val x = chartX + (i * chartW / (data.size - 1))
            val y = chartY + chartH - 1 - (value * (chartH - 2) / maxVal)
            
            Screen.set(x, y, "●")
            
            prevX = x
            prevY = y
        }
        
        // Labels
        Screen.setForeground(0x888888)
        for ((i, label) in labels.withIndex()) {
            if (i % 3 == 0) {
                val x = chartX + (i * chartW / (data.size - 1))
                Screen.set(x - 1, chartY + chartH + 1, label)
            }
        }
    }
    
    private fun renderBarChart() {
        val chartX = 3
        val chartY = 3
        val chartH = 12
        val barWidth = 3
        val gap = 1
        
        val maxVal = data.maxOrNull() ?: 1
        
        for ((i, value) in data.take(10).withIndex()) {
            val barHeight = value * chartH / maxVal
            val x = chartX + i * (barWidth + gap)
            val color = colors[i % colors.size]
            
            Screen.setBackground(color)
            for (h in 0 until barHeight) {
                Screen.fill(x, chartY + chartH - h - 1, barWidth, 1, ' ')
            }
            
            Screen.setBackground(0x1E1E1E)
            Screen.setForeground(0x888888)
            Screen.set(x, chartY + chartH + 1, labels[i].take(3))
        }
        
        Screen.setBackground(0x1E1E1E)
    }
    
    private fun renderPieChart() {
        val centerX = 20
        val centerY = 10
        val radius = 6
        
        val total = data.take(6).sum()
        var startAngle = 0.0
        
        for ((i, value) in data.take(6).withIndex()) {
            val angle = (value.toDouble() / total) * 360
            val color = colors[i]
            
            // Draw pie segment (simplified)
            Screen.setForeground(color)
            for (a in startAngle.toInt() until (startAngle + angle).toInt() step 5) {
                val rad = Math.toRadians(a.toDouble())
                for (r in 1..radius) {
                    val x = (centerX + kotlin.math.cos(rad) * r).toInt()
                    val y = (centerY + kotlin.math.sin(rad) * r / 2).toInt()
                    Screen.set(x, y, "█")
                }
            }
            
            startAngle += angle
        }
        
        // Legend
        Screen.setBackground(0x1E1E1E)
        for ((i, value) in data.take(6).withIndex()) {
            Screen.setForeground(colors[i])
            Screen.set(35, 4 + i * 2, "■")
            Screen.setForeground(0xAAAAAA)
            val percent = (value * 100 / data.take(6).sum())
            Screen.set(37, 4 + i * 2, "${labels[i]}: $percent%")
        }
    }
}

/**
 * Pioneer - GPS/Navigation app.
 */
class PioneerApp(os: KotlinOS) : Application(os, APP_INFO) {
    
    companion object {
        val APP_INFO = AppInfo(
            id = "pioneer",
            name = "Pioneer",
            icon = "🧭",
            category = AppCategory.UTILITIES,
            description = "GPS navigation",
            version = "1.0",
            author = "System"
        ) { PioneerApp(it) }
    }
    
    private data class Waypoint(
        val name: String,
        val x: Int,
        val y: Int,
        val z: Int
    )
    
    private var currentX = 0
    private var currentY = 64
    private var currentZ = 0
    
    private val waypoints = mutableListOf(
        Waypoint("Home", 0, 64, 0),
        Waypoint("Mine", -150, 30, 200),
        Waypoint("Farm", 100, 64, -50),
        Waypoint("Village", 500, 70, 300)
    )
    
    private var selectedWaypoint = 0
    private var tracking = false
    
    override fun onCreate() {
        createWindow("Pioneer GPS", 10, 3, 60, 20)
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
            'w' -> selectedWaypoint = maxOf(0, selectedWaypoint - 1)
            's' -> selectedWaypoint = minOf(waypoints.size - 1, selectedWaypoint + 1)
            't' -> tracking = !tracking
        }
    }
    
    private fun render() {
        Screen.setBackground(0x0D1B2A)
        Screen.fill(1, 1, 58, 18, ' ')
        
        // Header
        Screen.setBackground(0x1B263B)
        Screen.fill(1, 1, 58, 2, ' ')
        Screen.setForeground(0x00FF00)
        Screen.set(2, 1, "🧭 Pioneer GPS Navigation")
        Screen.setForeground(0xFFFFFF)
        Screen.set(2, 2, "Position: X:$currentX Y:$currentY Z:$currentZ")
        
        // Mini map
        Screen.setBackground(0x1A1A2A)
        Screen.fill(2, 4, 25, 12, ' ')
        
        // Draw grid
        Screen.setForeground(0x333344)
        for (y in 0..11) {
            for (x in 0..24) {
                if (x % 5 == 0 || y % 3 == 0) {
                    Screen.set(2 + x, 4 + y, "·")
                }
            }
        }
        
        // Draw waypoints on map
        for (wp in waypoints) {
            val mapX = 14 + (wp.x - currentX) / 50
            val mapY = 10 + (wp.z - currentZ) / 50
            if (mapX in 2..26 && mapY in 4..15) {
                Screen.setForeground(0xFFFF00)
                Screen.set(mapX, mapY, "◆")
            }
        }
        
        // Current position
        Screen.setForeground(0x00FF00)
        Screen.set(14, 10, "▲")
        
        // Waypoint list
        Screen.setBackground(0x0D1B2A)
        Screen.setForeground(0xFFFFFF)
        Screen.set(30, 4, "Waypoints:")
        
        for ((i, wp) in waypoints.withIndex()) {
            val y = 6 + i
            val isSelected = i == selectedWaypoint
            
            Screen.setForeground(if (isSelected) 0x00AAFF else 0xAAAAAA)
            val distance = kotlin.math.sqrt(
                ((wp.x - currentX) * (wp.x - currentX) +
                 (wp.z - currentZ) * (wp.z - currentZ)).toDouble()
            ).toInt()
            
            Screen.set(30, y, "${if (isSelected) ">" else " "} ${wp.name}")
            Screen.set(45, y, "${distance}m")
        }
        
        // Selected waypoint details
        if (selectedWaypoint < waypoints.size) {
            val wp = waypoints[selectedWaypoint]
            Screen.setBackground(0x1B263B)
            Screen.fill(30, 12, 27, 4, ' ')
            Screen.setForeground(0xFFFFFF)
            Screen.set(31, 12, wp.name)
            Screen.setForeground(0xAAAAAA)
            Screen.set(31, 13, "X:${wp.x} Y:${wp.y} Z:${wp.z}")
            Screen.setForeground(if (tracking) 0x00FF00 else 0x888888)
            Screen.set(31, 14, if (tracking) "Tracking..." else "T: Start tracking")
        }
        
        // Controls
        Screen.setBackground(0x0D1B2A)
        Screen.setForeground(0x888888)
        Screen.set(2, 17, "W/S: Select waypoint | T: Track | N: New waypoint")
    }
}
