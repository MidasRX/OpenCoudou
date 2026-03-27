package li.cil.oc.client.os.gui

import li.cil.oc.client.os.core.KotlinOS
import li.cil.oc.client.os.core.FrameBuffer
import li.cil.oc.client.os.apps.ApplicationInfo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Desktop environment for SkibidiOS2.
 * Manages taskbar, app icons, wallpaper, and system tray.
 */
class Desktop(private val os: KotlinOS) {
    
    // Desktop icons
    private val icons = mutableListOf<DesktopIcon>()
    
    // Taskbar
    private val taskbarHeight = 1
    private val runningApps = mutableListOf<TaskbarItem>()
    
    // Wallpaper
    var wallpaperColor = FrameBuffer.DESKTOP_BG
    var wallpaperPattern: WallpaperPattern = WallpaperPattern.SOLID
    
    // Time format
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
    // Start menu
    var startMenuOpen = false
    
    fun initialize() {
        // Create default desktop icons
        addIcon(DesktopIcon("📁", "Files", "file_manager", 2, 2))
        addIcon(DesktopIcon("📝", "Editor", "text_editor", 2, 5))
        addIcon(DesktopIcon("🖥️", "Terminal", "terminal", 2, 8))
        addIcon(DesktopIcon("🌐", "Browser", "web_browser", 2, 11))
        addIcon(DesktopIcon("⚙️", "Settings", "settings", 2, 14))
        addIcon(DesktopIcon("📊", "Monitor", "system_monitor", 2, 17))
        addIcon(DesktopIcon("🎮", "Lua", "lua_console", 2, 20))
    }
    
    /**
     * Add a desktop icon.
     */
    fun addIcon(icon: DesktopIcon) {
        icons.add(icon)
    }
    
    /**
     * Add app to taskbar.
     */
    fun addToTaskbar(app: ApplicationInfo, windowId: Int) {
        runningApps.add(TaskbarItem(app.name, app.icon, windowId))
    }
    
    /**
     * Remove app from taskbar.
     */
    fun removeFromTaskbar(windowId: Int) {
        runningApps.removeAll { it.windowId == windowId }
    }
    
    /**
     * Render desktop to frame buffer.
     */
    fun render(buffer: FrameBuffer) {
        // Draw wallpaper
        renderWallpaper(buffer)
        
        // Draw desktop icons
        icons.forEach { icon ->
            if (icon.visible) {
                renderIcon(buffer, icon)
            }
        }
        
        // Draw taskbar
        renderTaskbar(buffer)
        
        // Draw start menu if open
        if (startMenuOpen) {
            renderStartMenu(buffer)
        }
    }
    
    /**
     * Render wallpaper.
     */
    private fun renderWallpaper(buffer: FrameBuffer) {
        when (wallpaperPattern) {
            WallpaperPattern.SOLID -> {
                buffer.clear(wallpaperColor, FrameBuffer.TEXT)
            }
            WallpaperPattern.GRADIENT -> {
                for (y in 0 until os.screenHeight - taskbarHeight) {
                    val ratio = y.toFloat() / os.screenHeight
                    val r = ((wallpaperColor shr 16) and 0xFF) * (1 - ratio * 0.5)
                    val g = ((wallpaperColor shr 8) and 0xFF) * (1 - ratio * 0.5)
                    val b = (wallpaperColor and 0xFF) * (1 - ratio * 0.5)
                    val color = (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
                    buffer.fillRect(0, y, os.screenWidth, 1, ' ', FrameBuffer.TEXT, color)
                }
            }
            WallpaperPattern.DOTS -> {
                buffer.clear(wallpaperColor, FrameBuffer.TEXT)
                for (y in 0 until os.screenHeight - taskbarHeight step 2) {
                    for (x in 0 until os.screenWidth step 4) {
                        buffer.setChar(x, y, '·', FrameBuffer.TEXT_DIM, wallpaperColor)
                    }
                }
            }
            WallpaperPattern.GRID -> {
                buffer.clear(wallpaperColor, FrameBuffer.TEXT)
                for (y in 0 until os.screenHeight - taskbarHeight step 3) {
                    buffer.drawHLine(0, y, os.screenWidth, '─', 0x2A2A3A, wallpaperColor)
                }
                for (x in 0 until os.screenWidth step 6) {
                    buffer.drawVLine(x, 0, os.screenHeight - taskbarHeight, '│', 0x2A2A3A, wallpaperColor)
                }
            }
        }
    }
    
    /**
     * Render a desktop icon.
     */
    private fun renderIcon(buffer: FrameBuffer, icon: DesktopIcon) {
        // Icon background (hover effect)
        if (icon.hovered) {
            buffer.fillRect(icon.x - 1, icon.y - 1, 10, 4, ' ', FrameBuffer.TEXT, 0x3A3A4A)
        }
        
        // Icon emoji/symbol
        buffer.drawString(icon.x + 2, icon.y, icon.symbol, FrameBuffer.TEXT, 
            if (icon.hovered) 0x3A3A4A else wallpaperColor)
        
        // Icon label (truncated)
        val label = if (icon.label.length > 8) icon.label.take(7) + "…" else icon.label
        buffer.drawString(icon.x, icon.y + 1, label.padEnd(8), FrameBuffer.TEXT,
            if (icon.hovered) 0x3A3A4A else wallpaperColor)
    }
    
    /**
     * Render taskbar.
     */
    private fun renderTaskbar(buffer: FrameBuffer) {
        val taskbarY = os.screenHeight - taskbarHeight
        
        // Taskbar background
        buffer.fillRect(0, taskbarY, os.screenWidth, taskbarHeight, ' ', 
            FrameBuffer.TEXT, FrameBuffer.WINDOW_TITLE)
        
        // Start button
        buffer.drawString(1, taskbarY, "⊞", FrameBuffer.ACCENT, FrameBuffer.WINDOW_TITLE)
        buffer.drawString(3, taskbarY, "Start", FrameBuffer.TEXT, FrameBuffer.WINDOW_TITLE)
        
        // Running apps
        var appX = 10
        runningApps.forEach { item ->
            val focused = os.windowManager.getFocusedWindow()?.id == item.windowId
            val bg = if (focused) FrameBuffer.ACCENT else FrameBuffer.WINDOW_TITLE
            val fg = if (focused) FrameBuffer.BLACK else FrameBuffer.TEXT
            
            buffer.drawString(appX, taskbarY, "${item.icon} ${item.name.take(8)}", fg, bg)
            appX += item.name.take(8).length + 3
        }
        
        // System tray (right side)
        val time = LocalDateTime.now().format(timeFormatter)
        buffer.drawString(os.screenWidth - time.length - 1, taskbarY, time, 
            FrameBuffer.TEXT, FrameBuffer.WINDOW_TITLE)
    }
    
    /**
     * Render start menu.
     */
    private fun renderStartMenu(buffer: FrameBuffer) {
        val menuX = 0
        val menuY = os.screenHeight - 15
        val menuWidth = 25
        val menuHeight = 14
        
        // Menu background
        buffer.fillRect(menuX, menuY, menuWidth, menuHeight, ' ', 
            FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
        buffer.drawRect(menuX, menuY, menuWidth, menuHeight, 
            FrameBuffer.ACCENT, FrameBuffer.WINDOW_BG)
        
        // Menu title
        buffer.drawString(menuX + 2, menuY + 1, "⊞ SkibidiOS2", 
            FrameBuffer.ACCENT, FrameBuffer.WINDOW_BG)
        buffer.drawHLine(menuX + 1, menuY + 2, menuWidth - 2, '─', 
            FrameBuffer.TEXT_DIM, FrameBuffer.WINDOW_BG)
        
        // App list
        val apps = os.appRegistry.getInstalledApps()
        apps.take(8).forEachIndexed { index, app ->
            buffer.drawString(menuX + 2, menuY + 3 + index, 
                "${app.icon} ${app.name.take(18)}", 
                FrameBuffer.TEXT, FrameBuffer.WINDOW_BG)
        }
        
        // Footer
        buffer.drawHLine(menuX + 1, menuY + 12, menuWidth - 2, '─', 
            FrameBuffer.TEXT_DIM, FrameBuffer.WINDOW_BG)
        buffer.drawString(menuX + 2, menuY + 13, "⏻ Shutdown  🔄 Reboot", 
            FrameBuffer.TEXT_DIM, FrameBuffer.WINDOW_BG)
    }
    
    /**
     * Handle click on desktop.
     */
    fun handleClick(x: Int, y: Int, button: Int): Boolean {
        // Check taskbar
        if (y == os.screenHeight - 1) {
            // Start button
            if (x in 1..8) {
                startMenuOpen = !startMenuOpen
                return true
            }
            // TODO: Handle taskbar item clicks
            return true
        }
        
        // Check start menu
        if (startMenuOpen) {
            if (x < 25 && y > os.screenHeight - 15) {
                // Handle menu item click
                val itemIndex = y - (os.screenHeight - 12)
                if (itemIndex in 0..7) {
                    val apps = os.appRegistry.getInstalledApps()
                    if (itemIndex < apps.size) {
                        os.appRegistry.launchApp(apps[itemIndex].id)
                        startMenuOpen = false
                        return true
                    }
                }
                // Shutdown/Reboot
                if (y == os.screenHeight - 2) {
                    if (x < 12) os.shutdown() else os.reboot()
                    return true
                }
            }
            startMenuOpen = false
        }
        
        // Check desktop icons
        icons.forEach { icon ->
            if (x >= icon.x - 1 && x < icon.x + 9 && y >= icon.y - 1 && y < icon.y + 3) {
                if (button == 0) {
                    // Double click to launch (simplified to single click)
                    os.appRegistry.launchApp(icon.appId)
                    return true
                }
            }
        }
        
        return false
    }
    
    /**
     * Handle mouse move for hover effects.
     */
    fun handleMouseMove(x: Int, y: Int) {
        icons.forEach { icon ->
            icon.hovered = x >= icon.x - 1 && x < icon.x + 9 && y >= icon.y - 1 && y < icon.y + 3
        }
    }
}

/**
 * Desktop icon data.
 */
data class DesktopIcon(
    val symbol: String,
    val label: String,
    val appId: String,
    var x: Int,
    var y: Int,
    var visible: Boolean = true,
    var hovered: Boolean = false
)

/**
 * Taskbar item data.
 */
data class TaskbarItem(
    val name: String,
    val icon: String,
    val windowId: Int
)

/**
 * Wallpaper patterns.
 */
enum class WallpaperPattern {
    SOLID,
    GRADIENT,
    DOTS,
    GRID
}
