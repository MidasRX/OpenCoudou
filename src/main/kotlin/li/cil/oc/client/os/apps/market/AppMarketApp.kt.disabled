package li.cil.oc.client.os.apps.market

import li.cil.oc.client.os.core.KotlinOS
import li.cil.oc.client.os.apps.Application
import li.cil.oc.client.os.apps.AppInfo
import li.cil.oc.client.os.apps.AppCategory
import li.cil.oc.client.os.libs.*
import li.cil.oc.client.os.network.NetworkStack
import li.cil.oc.client.os.filesystem.PersistentFileSystem
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * App Market - Application store for SkibidiOS2.
 * Download and install applications from online repositories.
 */

private val APP_MARKET_INFO = AppInfo(
    id = "app_market",
    name = "App Market",
    icon = "🛒",
    category = AppCategory.SYSTEM,
    description = "Download and install applications"
) { AppMarketApp(it) }

class AppMarketApp(os: KotlinOS) : Application(os, APP_MARKET_INFO) {
    
    // Market state
    private var currentCategory = MarketCategory.APPLICATIONS
    private var currentView = MarketView.BROWSE
    private var searchQuery = ""
    private var selectedAppIndex = 0
    private var scrollOffset = 0
    
    // Data
    private val installedApps = mutableSetOf<String>()
    private var marketApps = mutableListOf<MarketAppInfo>()
    private var filteredApps = mutableListOf<MarketAppInfo>()
    private var selectedApp: MarketAppInfo? = null
    
    // Download state
    private var isDownloading = false
    private var downloadProgress = 0
    private var downloadStatus = ""
    
    // Repository
    private val repositories = listOf(
        Repository("Official", "https://skibidios.example.com/api/v1/"),
        Repository("Community", "https://community.skibidios.example.com/api/v1/")
    )
    private var currentRepo = 0
    
    enum class MarketCategory(val displayName: String, val icon: String) {
        APPLICATIONS("Applications", "🎸"),
        LIBRARIES("Libraries", "📖"),
        SCRIPTS("Scripts", "📜"),
        WALLPAPERS("Wallpapers", "🖼"),
        THEMES("Themes", "🎨"),
        GAMES("Games", "🎮"),
        UTILITIES("Utilities", "🔧"),
        ALL("All", "📦")
    }
    
    enum class MarketView {
        BROWSE,
        SEARCH,
        APP_DETAILS,
        INSTALLING,
        INSTALLED,
        UPDATES,
        SETTINGS
    }
    
    data class Repository(
        val name: String,
        val url: String,
        var enabled: Boolean = true
    )
    
    data class MarketAppInfo(
        val id: String,
        val name: String,
        val description: String,
        val author: String,
        val version: String,
        val category: MarketCategory,
        val icon: String,
        val rating: Float,
        val downloads: Int,
        val size: Long,
        val dependencies: List<String> = emptyList(),
        val screenshots: List<String> = emptyList(),
        val changelog: String = "",
        val license: String = "MIT",
        val downloadUrl: String = "",
        val lastUpdate: Long = 0
    )
    
    override fun onCreate() {
        createWindow("App Market", 5, 2, 90, 30)
        loadInstalledApps()
        loadMarketData()
    }
    
    override fun onStart() {
        filterApps()
    }
    
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
    
    override fun onUpdate() {
        render()
    }
    
    override fun onKeyDown(keyCode: Int, char: Char) {
        when (currentView) {
            MarketView.BROWSE, MarketView.SEARCH -> handleBrowseInput(keyCode, char)
            MarketView.APP_DETAILS -> handleDetailsInput(keyCode, char)
            MarketView.INSTALLED -> handleInstalledInput(keyCode, char)
            else -> {}
        }
    }
    
    private fun handleBrowseInput(keyCode: Int, char: Char) {
        when (keyCode) {
            Keyboard.KEY_UP -> navigateUp()
            Keyboard.KEY_DOWN -> navigateDown()
            Keyboard.KEY_LEFT -> previousCategory()
            Keyboard.KEY_RIGHT -> nextCategory()
            Keyboard.KEY_ENTER -> {
                if (filteredApps.isNotEmpty()) {
                    selectedApp = filteredApps[selectedAppIndex]
                    currentView = MarketView.APP_DETAILS
                }
            }
            Keyboard.KEY_TAB -> cycleView()
            Keyboard.KEY_ESCAPE -> {
                if (currentView == MarketView.SEARCH) {
                    currentView = MarketView.BROWSE
                    searchQuery = ""
                    filterApps()
                } else {
                    close()
                }
            }
            Keyboard.KEY_S -> {
                currentView = MarketView.SEARCH
            }
            Keyboard.KEY_I -> {
                currentView = MarketView.INSTALLED
            }
            Keyboard.KEY_R -> {
                loadMarketData()
            }
        }
        
        // Search input
        if (currentView == MarketView.SEARCH && char.isLetterOrDigit()) {
            searchQuery += char
            filterApps()
        } else if (currentView == MarketView.SEARCH && keyCode == Keyboard.KEY_BACK) {
            if (searchQuery.isNotEmpty()) {
                searchQuery = searchQuery.dropLast(1)
                filterApps()
            }
        }
    }
    
    private fun handleDetailsInput(keyCode: Int, char: Char) {
        when (keyCode) {
            Keyboard.KEY_ESCAPE, Keyboard.KEY_BACK -> {
                currentView = MarketView.BROWSE
                selectedApp = null
            }
            Keyboard.KEY_ENTER, Keyboard.KEY_I -> {
                selectedApp?.let { installApp(it) }
            }
            Keyboard.KEY_U -> {
                selectedApp?.let { 
                    if (it.id in installedApps) {
                        uninstallApp(it)
                    }
                }
            }
        }
    }
    
    private fun handleInstalledInput(keyCode: Int, char: Char) {
        when (keyCode) {
            Keyboard.KEY_ESCAPE -> currentView = MarketView.BROWSE
            Keyboard.KEY_UP -> navigateUp()
            Keyboard.KEY_DOWN -> navigateDown()
            Keyboard.KEY_U -> {
                if (filteredApps.isNotEmpty()) {
                    uninstallApp(filteredApps[selectedAppIndex])
                }
            }
        }
    }
    
    private fun navigateUp() {
        if (selectedAppIndex > 0) {
            selectedAppIndex--
            if (selectedAppIndex < scrollOffset) {
                scrollOffset = selectedAppIndex
            }
        }
    }
    
    private fun navigateDown() {
        if (selectedAppIndex < filteredApps.size - 1) {
            selectedAppIndex++
            val visibleCount = 15
            if (selectedAppIndex >= scrollOffset + visibleCount) {
                scrollOffset = selectedAppIndex - visibleCount + 1
            }
        }
    }
    
    private fun previousCategory() {
        val categories = MarketCategory.entries
        val currentIndex = categories.indexOf(currentCategory)
        currentCategory = categories[(currentIndex - 1 + categories.size) % categories.size]
        selectedAppIndex = 0
        scrollOffset = 0
        filterApps()
    }
    
    private fun nextCategory() {
        val categories = MarketCategory.entries
        val currentIndex = categories.indexOf(currentCategory)
        currentCategory = categories[(currentIndex + 1) % categories.size]
        selectedAppIndex = 0
        scrollOffset = 0
        filterApps()
    }
    
    private fun cycleView() {
        currentView = when (currentView) {
            MarketView.BROWSE -> MarketView.INSTALLED
            MarketView.INSTALLED -> MarketView.UPDATES
            MarketView.UPDATES -> MarketView.BROWSE
            else -> MarketView.BROWSE
        }
        selectedAppIndex = 0
        scrollOffset = 0
        filterApps()
    }
    
    private fun loadInstalledApps() {
        // Load from filesystem
        val fs = os.fileSystem
        val appsDir = "/opt/apps"
        fs.list(appsDir)?.forEach { name ->
            if (name.endsWith(".app")) {
                installedApps.add(name.removeSuffix(".app"))
            }
        }
    }
    
    private fun loadMarketData() {
        // In production, this would fetch from the API
        // For now, populate with sample data
        marketApps.clear()
        marketApps.addAll(getSampleApps())
        filterApps()
    }
    
    private fun filterApps() {
        filteredApps.clear()
        
        val apps = when (currentView) {
            MarketView.INSTALLED -> marketApps.filter { it.id in installedApps }
            MarketView.UPDATES -> marketApps.filter { it.id in installedApps } // Would check versions
            else -> marketApps
        }
        
        filteredApps.addAll(apps.filter { app ->
            val matchesCategory = currentCategory == MarketCategory.ALL || app.category == currentCategory
            val matchesSearch = searchQuery.isEmpty() || 
                app.name.contains(searchQuery, ignoreCase = true) ||
                app.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        })
        
        if (selectedAppIndex >= filteredApps.size) {
            selectedAppIndex = maxOf(0, filteredApps.size - 1)
        }
    }
    
    private fun installApp(app: MarketAppInfo) {
        if (isDownloading) return
        
        isDownloading = true
        currentView = MarketView.INSTALLING
        downloadProgress = 0
        downloadStatus = "Preparing..."
        
        // Simulate download
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Check dependencies
                downloadStatus = "Checking dependencies..."
                downloadProgress = 10
                delay(300)
                
                for (dep in app.dependencies) {
                    if (dep !in installedApps) {
                        downloadStatus = "Installing dependency: $dep"
                        delay(500)
                    }
                }
                
                // Download
                downloadStatus = "Downloading ${app.name}..."
                for (i in 20..80 step 10) {
                    downloadProgress = i
                    delay(200)
                }
                
                // Install
                downloadStatus = "Installing..."
                downloadProgress = 90
                delay(300)
                
                // Create app directory
                val fs = os.fileSystem
                val appDir = "/opt/apps/${app.id}.app"
                fs.mkdir(appDir)
                fs.writeText("$appDir/main.lua", "-- ${app.name}\nreturn {}")
                fs.writeText("$appDir/info.json", """
                    {"name":"${app.name}","version":"${app.version}","author":"${app.author}"}
                """.trimIndent())
                
                installedApps.add(app.id)
                
                downloadStatus = "Complete!"
                downloadProgress = 100
                delay(500)
                
            } catch (e: Exception) {
                downloadStatus = "Error: ${e.message}"
            } finally {
                isDownloading = false
                currentView = MarketView.APP_DETAILS
            }
        }
    }
    
    private fun uninstallApp(app: MarketAppInfo) {
        val fs = os.fileSystem
        val appDir = "/opt/apps/${app.id}.app"
        fs.delete(appDir, recursive = true)
        installedApps.remove(app.id)
        filterApps()
    }
    
    private fun render() {
        val w = window ?: return
        Screen.setBackground(0x1E1E1E)
        Screen.fill(w.x, w.y, w.width, w.height, ' ')
        
        // Title bar
        Screen.setBackground(0x3C3C3C)
        Screen.fill(w.x, w.y, w.width, 1, ' ')
        Screen.setForeground(0xFFFFFF)
        Screen.set(w.x + 2, w.y, "🛒 App Market")
        
        // Repository indicator
        Screen.setForeground(0x888888)
        Screen.set(w.x + w.width - 20, w.y, repositories[currentRepo].name)
        
        when (currentView) {
            MarketView.BROWSE, MarketView.SEARCH -> renderBrowseView(w)
            MarketView.APP_DETAILS -> renderDetailsView(w)
            MarketView.INSTALLING -> renderInstallingView(w)
            MarketView.INSTALLED -> renderInstalledView(w)
            MarketView.UPDATES -> renderUpdatesView(w)
            MarketView.SETTINGS -> renderSettingsView(w)
        }
        
        // Status bar
        Screen.setBackground(0x2D2D2D)
        Screen.fill(w.x, w.y + w.height - 1, w.width, 1, ' ')
        Screen.setForeground(0x888888)
        val statusText = when (currentView) {
            MarketView.BROWSE -> "↑↓ Navigate | ←→ Category | Enter Select | S Search | I Installed | R Refresh"
            MarketView.SEARCH -> "Type to search | Enter Select | Esc Cancel"
            MarketView.APP_DETAILS -> "Enter Install | U Uninstall | Esc Back"
            MarketView.INSTALLED -> "↑↓ Navigate | U Uninstall | Esc Back"
            else -> "Esc Back"
        }
        Screen.set(w.x + 2, w.y + w.height - 1, statusText)
    }
    
    private fun renderBrowseView(w: li.cil.oc.client.os.gui.Window) {
        val contentY = w.y + 2
        
        // Categories
        Screen.setBackground(0x2D2D2D)
        Screen.fill(w.x, contentY, 20, w.height - 3, ' ')
        
        Screen.setForeground(0xFFFFFF)
        Screen.set(w.x + 2, contentY, "Categories")
        
        var catY = contentY + 2
        for (cat in MarketCategory.entries) {
            val isSelected = cat == currentCategory
            Screen.setBackground(if (isSelected) 0x3399FF else 0x2D2D2D)
            Screen.setForeground(if (isSelected) 0xFFFFFF else 0xAAAAAA)
            Screen.fill(w.x + 1, catY, 18, 1, ' ')
            Screen.set(w.x + 2, catY, "${cat.icon} ${cat.displayName}")
            catY++
        }
        
        // Search bar (if in search mode)
        if (currentView == MarketView.SEARCH) {
            Screen.setBackground(0x1A1A1A)
            Screen.fill(w.x + 21, contentY, w.width - 22, 1, ' ')
            Screen.setForeground(0xFFFFFF)
            Screen.set(w.x + 22, contentY, "🔍 $searchQuery█")
        }
        
        // App list
        val listX = w.x + 21
        val listY = if (currentView == MarketView.SEARCH) contentY + 2 else contentY
        val listWidth = w.width - 22
        val listHeight = w.height - 4 - (if (currentView == MarketView.SEARCH) 2 else 0)
        
        Screen.setBackground(0x1E1E1E)
        
        if (filteredApps.isEmpty()) {
            Screen.setForeground(0x666666)
            Screen.set(listX + 10, listY + 5, "No apps found")
        } else {
            val visibleApps = filteredApps.drop(scrollOffset).take(listHeight / 2)
            
            for ((index, app) in visibleApps.withIndex()) {
                val appY = listY + index * 2
                val absoluteIndex = scrollOffset + index
                val isSelected = absoluteIndex == selectedAppIndex
                
                Screen.setBackground(if (isSelected) 0x3C3C3C else 0x1E1E1E)
                Screen.fill(listX, appY, listWidth, 2, ' ')
                
                // Icon and name
                Screen.setForeground(0xFFFFFF)
                Screen.set(listX + 1, appY, app.icon)
                Screen.set(listX + 4, appY, app.name)
                
                // Installed badge
                if (app.id in installedApps) {
                    Screen.setForeground(0x55FF55)
                    Screen.set(listX + 4 + app.name.length + 1, appY, "✓")
                }
                
                // Version and author
                Screen.setForeground(0x888888)
                Screen.set(listX + 4, appY + 1, "v${app.version} by ${app.author}")
                
                // Rating
                Screen.setForeground(0xFFAA00)
                val stars = "★".repeat(app.rating.toInt()) + "☆".repeat(5 - app.rating.toInt())
                Screen.set(listX + listWidth - 10, appY, stars)
                
                // Downloads
                Screen.setForeground(0x666666)
                Screen.set(listX + listWidth - 10, appY + 1, "${Number.formatSI(app.downloads.toLong())} ⬇")
            }
        }
        
        // Scrollbar
        if (filteredApps.size > listHeight / 2) {
            val scrollbarHeight = listHeight
            val thumbSize = maxOf(1, scrollbarHeight * (listHeight / 2) / filteredApps.size)
            val thumbPos = scrollbarHeight * scrollOffset / filteredApps.size
            
            Screen.setBackground(0x333333)
            Screen.fill(w.x + w.width - 1, listY, 1, scrollbarHeight, ' ')
            Screen.setBackground(0x666666)
            Screen.fill(w.x + w.width - 1, listY + thumbPos, 1, thumbSize, ' ')
        }
    }
    
    private fun renderDetailsView(w: li.cil.oc.client.os.gui.Window) {
        val app = selectedApp ?: return
        val contentY = w.y + 2
        
        Screen.setBackground(0x1E1E1E)
        
        // App header
        Screen.setForeground(0xFFFFFF)
        Screen.set(w.x + 3, contentY, app.icon)
        Screen.set(w.x + 6, contentY, app.name)
        
        Screen.setForeground(0x888888)
        Screen.set(w.x + 6, contentY + 1, "v${app.version} by ${app.author}")
        
        // Rating
        Screen.setForeground(0xFFAA00)
        val stars = "★".repeat(app.rating.toInt()) + "☆".repeat(5 - app.rating.toInt())
        Screen.set(w.x + 6, contentY + 2, "$stars (${app.rating})")
        
        // Stats
        Screen.setForeground(0xAAAAAA)
        Screen.set(w.x + 6, contentY + 3, "Downloads: ${Number.formatSI(app.downloads.toLong())} | Size: ${Number.formatBytes(app.size)}")
        
        // Install/Uninstall button
        val isInstalled = app.id in installedApps
        Screen.setBackground(if (isInstalled) 0xFF5555 else 0x55AA55)
        Screen.setForeground(0xFFFFFF)
        Screen.fill(w.x + w.width - 20, contentY, 15, 3, ' ')
        Screen.set(w.x + w.width - 17, contentY + 1, if (isInstalled) "Uninstall" else "Install")
        
        // Description
        Screen.setBackground(0x1E1E1E)
        Screen.setForeground(0xFFFFFF)
        Screen.set(w.x + 3, contentY + 5, "Description:")
        Screen.setForeground(0xCCCCCC)
        
        val descLines = Text.wrap(app.description, w.width - 6)
        for ((i, line) in descLines.withIndex()) {
            if (contentY + 6 + i < w.y + w.height - 5) {
                Screen.set(w.x + 3, contentY + 6 + i, line)
            }
        }
        
        // Dependencies
        if (app.dependencies.isNotEmpty()) {
            val depY = contentY + 6 + descLines.size + 1
            Screen.setForeground(0xFFFFFF)
            Screen.set(w.x + 3, depY, "Dependencies:")
            Screen.setForeground(0x888888)
            Screen.set(w.x + 3, depY + 1, app.dependencies.joinToString(", "))
        }
        
        // License
        Screen.setForeground(0x666666)
        Screen.set(w.x + 3, w.y + w.height - 3, "License: ${app.license}")
    }
    
    private fun renderInstallingView(w: li.cil.oc.client.os.gui.Window) {
        val centerY = w.y + w.height / 2
        
        Screen.setBackground(0x1E1E1E)
        Screen.setForeground(0xFFFFFF)
        
        // Status
        Screen.drawCentered(centerY - 2, downloadStatus)
        
        // Progress bar
        val barWidth = 50
        val barX = w.x + (w.width - barWidth) / 2
        val filled = barWidth * downloadProgress / 100
        
        Screen.setBackground(0x333333)
        Screen.fill(barX, centerY, barWidth, 1, ' ')
        Screen.setBackground(0x3399FF)
        Screen.fill(barX, centerY, filled, 1, ' ')
        
        // Percentage
        Screen.setBackground(0x1E1E1E)
        Screen.setForeground(0xFFFFFF)
        Screen.drawCentered(centerY + 2, "$downloadProgress%")
    }
    
    private fun renderInstalledView(w: li.cil.oc.client.os.gui.Window) {
        val contentY = w.y + 2
        
        Screen.setForeground(0xFFFFFF)
        Screen.set(w.x + 3, contentY, "Installed Applications (${installedApps.size})")
        
        val installedList = marketApps.filter { it.id in installedApps }
        
        if (installedList.isEmpty()) {
            Screen.setForeground(0x666666)
            Screen.set(w.x + 10, contentY + 5, "No installed applications")
        } else {
            for ((index, app) in installedList.withIndex()) {
                val appY = contentY + 2 + index * 2
                val isSelected = index == selectedAppIndex
                
                Screen.setBackground(if (isSelected) 0x3C3C3C else 0x1E1E1E)
                Screen.fill(w.x + 2, appY, w.width - 4, 2, ' ')
                
                Screen.setForeground(0xFFFFFF)
                Screen.set(w.x + 3, appY, "${app.icon} ${app.name}")
                Screen.setForeground(0x888888)
                Screen.set(w.x + 3, appY + 1, "v${app.version}")
            }
        }
    }
    
    private fun renderUpdatesView(w: li.cil.oc.client.os.gui.Window) {
        Screen.setForeground(0xFFFFFF)
        Screen.set(w.x + 3, w.y + 2, "Updates")
        Screen.setForeground(0x55FF55)
        Screen.set(w.x + 3, w.y + 5, "All applications are up to date!")
    }
    
    private fun renderSettingsView(w: li.cil.oc.client.os.gui.Window) {
        Screen.setForeground(0xFFFFFF)
        Screen.set(w.x + 3, w.y + 2, "Settings")
    }
    
    private fun getSampleApps(): List<MarketAppInfo> = listOf(
        MarketAppInfo("minecode", "MineCode IDE", "Full-featured code editor with syntax highlighting, autocomplete, and debugging support for Lua and other languages.", "Igor Timofeev", "1.2.5", MarketCategory.APPLICATIONS, "💻", 4.8f, 15420, 256000),
        MarketAppInfo("3dprint", "3D Print", "Create and print 3D models using OpenComputers 3D printer component.", "Igor Timofeev", "1.0.3", MarketCategory.APPLICATIONS, "🖨", 4.5f, 8930, 128000),
        MarketAppInfo("picture_edit", "Picture Edit", "Advanced image editor with layers, tools, and filters for .pic format.", "Igor Timofeev", "2.1.0", MarketCategory.APPLICATIONS, "🎨", 4.7f, 12350, 180000),
        MarketAppInfo("picture_view", "Picture View", "Fast image viewer with slideshow support.", "Igor Timofeev", "1.5.0", MarketCategory.APPLICATIONS, "🖼", 4.6f, 18200, 64000),
        MarketAppInfo("irc", "IRC Client", "Connect to IRC networks and chat with other users.", "ECS", "1.1.2", MarketCategory.NETWORK, "💬", 4.3f, 5620, 96000),
        MarketAppInfo("stargate", "Stargate Control", "Control Stargate mod gates from your computer.", "Community", "2.0.1", MarketCategory.UTILITIES, "🌀", 4.9f, 9870, 112000),
        MarketAppInfo("ic2reactor", "IC2 Reactor Control", "Monitor and control IndustrialCraft 2 nuclear reactors.", "Community", "1.3.0", MarketCategory.UTILITIES, "☢", 4.4f, 7340, 98000),
        MarketAppInfo("nanomachines", "Nanomachines Control", "Configure and control nanomachines effects.", "System", "1.0.0", MarketCategory.UTILITIES, "🔬", 4.2f, 4560, 52000),
        MarketAppInfo("raycast", "RayCast 3D", "3D raycasting demo and game engine.", "ECS", "1.0.5", MarketCategory.GAMES, "🎯", 4.1f, 6780, 145000),
        MarketAppInfo("shooting", "Shooting Game", "Classic shooting gallery game.", "ECS", "1.0.0", MarketCategory.GAMES, "🎯", 3.9f, 3420, 78000),
        MarketAppInfo("spinner", "Spinner", "Fidget spinner simulator.", "Community", "1.0.0", MarketCategory.GAMES, "💫", 3.5f, 2150, 32000),
        MarketAppInfo("holoclock", "HoloClock", "Display time using hologram projector.", "System", "1.2.0", MarketCategory.UTILITIES, "🕐", 4.0f, 5890, 48000),
        MarketAppInfo("graph", "Graph", "Create and display graphs and charts.", "ECS", "1.1.0", MarketCategory.UTILITIES, "📊", 4.2f, 4230, 86000),
        MarketAppInfo("hex", "HEX Editor", "Binary file hex editor.", "ECS", "1.0.2", MarketCategory.UTILITIES, "🔢", 4.4f, 3890, 72000),
        MarketAppInfo("palette", "Palette", "Color palette viewer and picker.", "System", "1.0.0", MarketCategory.UTILITIES, "🎨", 4.1f, 2670, 36000),
        MarketAppInfo("calendar", "Calendar", "Calendar with events and reminders.", "System", "1.1.0", MarketCategory.UTILITIES, "📅", 4.3f, 6120, 54000),
        MarketAppInfo("translate", "Translate", "Text translation using online services.", "Community", "1.0.1", MarketCategory.UTILITIES, "🌐", 4.0f, 3450, 68000),
        MarketAppInfo("weather", "Weather", "Display weather information.", "Community", "1.0.0", MarketCategory.UTILITIES, "🌤", 3.8f, 2890, 42000),
        MarketAppInfo("nyancat", "NyanCat Wallpaper", "Animated NyanCat wallpaper.", "ECS", "1.0.0", MarketCategory.WALLPAPERS, "🐱", 4.6f, 8920, 24000),
        MarketAppInfo("rain", "Rain Wallpaper", "Animated rain effect wallpaper.", "ECS", "1.0.0", MarketCategory.WALLPAPERS, "🌧", 4.5f, 7650, 18000),
        MarketAppInfo("snow", "Snow Wallpaper", "Animated snow effect wallpaper.", "ECS", "1.0.0", MarketCategory.WALLPAPERS, "❄", 4.4f, 6340, 16000),
        MarketAppInfo("stars", "Stars Wallpaper", "Animated starfield wallpaper.", "ECS", "1.0.0", MarketCategory.WALLPAPERS, "⭐", 4.3f, 5890, 14000)
    )
}
