package li.cil.oc.client.os.apps

import li.cil.oc.client.os.core.KotlinOS
import li.cil.oc.client.os.gui.Window
import java.util.concurrent.ConcurrentHashMap

/**
 * Application registry for SkibidiOS2.
 * Manages app registration, launching, and lifecycle.
 */

data class AppInfo(
    val id: String,
    val name: String,
    val icon: String,
    val category: AppCategory,
    val description: String = "",
    val version: String = "1.0",
    val author: String = "System",
    val factory: (KotlinOS) -> Application
)

enum class AppCategory {
    SYSTEM,
    UTILITIES,
    DEVELOPMENT,
    NETWORK,
    GAMES,
    MEDIA,
    OTHER
}

abstract class Application(
    val os: KotlinOS,
    val info: AppInfo
) {
    var window: Window? = null
    var running = false
    var processId: Int? = null
    
    // Lifecycle callbacks
    abstract fun onCreate()
    abstract fun onStart()
    abstract fun onResume()
    abstract fun onPause()
    abstract fun onStop()
    abstract fun onDestroy()
    
    // Called every frame while app is active
    open fun onUpdate() {}
    
    // Handle keyboard input
    open fun onKeyDown(keyCode: Int, char: Char) {}
    open fun onKeyUp(keyCode: Int) {}
    
    // Create the window for this app
    protected fun createWindow(
        title: String = info.name,
        x: Int = 5,
        y: Int = 3,
        width: Int = 60,
        height: Int = 20
    ): Window {
        val w = os.windowManager.createWindow(title, x, y, width, height)
        w.onClose = { close() }
        window = w
        return w
    }
    
    fun close() {
        onPause()
        onStop()
        onDestroy()
        window?.let { os.windowManager.closeWindow(it) }
        running = false
        processId?.let { os.processManager.kill(it) }
    }
    
    fun minimize() {
        window?.minimized = true
        onPause()
    }
    
    fun restore() {
        window?.minimized = false
        window?.let { os.windowManager.focus(it) }
        onResume()
    }
}

class ApplicationRegistry(private val os: KotlinOS) {
    private val apps = ConcurrentHashMap<String, AppInfo>()
    private val runningApps = ConcurrentHashMap<String, MutableList<Application>>()
    
    init {
        registerBuiltInApps()
    }
    
    private fun registerBuiltInApps() {
        // File Manager
        register(AppInfo(
            id = "file_manager",
            name = "Files",
            icon = "📁",
            category = AppCategory.SYSTEM,
            description = "Browse and manage files"
        ) { FileManagerApp(it) })
        
        // Text Editor
        register(AppInfo(
            id = "text_editor",
            name = "Editor",
            icon = "📝",
            category = AppCategory.UTILITIES,
            description = "Edit text files with syntax highlighting"
        ) { TextEditorApp(it) })
        
        // Terminal
        register(AppInfo(
            id = "terminal",
            name = "Terminal",
            icon = "💻",
            category = AppCategory.SYSTEM,
            description = "Command line interface"
        ) { TerminalApp(it) })
        
        // Web Browser
        register(AppInfo(
            id = "web_browser",
            name = "Browser",
            icon = "🌐",
            category = AppCategory.NETWORK,
            description = "Browse the internet"
        ) { WebBrowserApp(it) })
        
        // Settings
        register(AppInfo(
            id = "settings",
            name = "Settings",
            icon = "⚙",
            category = AppCategory.SYSTEM,
            description = "System configuration"
        ) { SettingsApp(it) })
        
        // System Monitor
        register(AppInfo(
            id = "system_monitor",
            name = "Monitor",
            icon = "📊",
            category = AppCategory.SYSTEM,
            description = "View system resources"
        ) { SystemMonitorApp(it) })
        
        // Lua Interpreter (for compatibility)
        register(AppInfo(
            id = "lua",
            name = "Lua",
            icon = "🌙",
            category = AppCategory.DEVELOPMENT,
            description = "Lua scripting console"
        ) { LuaConsoleApp(it) })
        
        // Calculator
        register(AppInfo(
            id = "calculator",
            name = "Calculator",
            icon = "🔢",
            category = AppCategory.UTILITIES,
            description = "Basic calculator"
        ) { CalculatorApp(it) })
        
        // Notes
        register(AppInfo(
            id = "notes",
            name = "Notes",
            icon = "📋",
            category = AppCategory.UTILITIES,
            description = "Quick notes and to-do lists"
        ) { NotesApp(it) })
    }
    
    fun register(info: AppInfo): Boolean {
        if (apps.containsKey(info.id)) return false
        apps[info.id] = info
        return true
    }
    
    fun unregister(id: String): Boolean {
        return apps.remove(id) != null
    }
    
    fun getApp(id: String): AppInfo? = apps[id]
    
    fun getAllApps(): List<AppInfo> = apps.values.toList()
    
    fun getAppsByCategory(category: AppCategory): List<AppInfo> =
        apps.values.filter { it.category == category }
    
    fun launch(id: String): Application? {
        val info = apps[id] ?: return null
        
        val app = info.factory(os)
        app.running = true
        
        // Register with process manager
        app.processId = os.processManager.spawn("app:$id", app::onUpdate)
        
        // Track running instance
        runningApps.getOrPut(id) { mutableListOf() }.add(app)
        
        // Lifecycle
        app.onCreate()
        app.onStart()
        app.onResume()
        
        return app
    }
    
    fun getRunningApps(): List<Application> =
        runningApps.values.flatten()
    
    fun getRunningInstances(id: String): List<Application> =
        runningApps[id] ?: emptyList()
    
    fun closeAll() {
        runningApps.values.flatten().forEach { it.close() }
        runningApps.clear()
    }
    
    fun closeApp(app: Application) {
        app.close()
        runningApps[app.info.id]?.remove(app)
    }
}

// ============================================================
// Built-in App Stubs (to be implemented in separate files)
// ============================================================

// App Info objects defined statically to avoid circular references
private val FILE_MANAGER_INFO = AppInfo("file_manager", "Files", "📁", AppCategory.SYSTEM) { FileManagerApp(it) }
private val TEXT_EDITOR_INFO = AppInfo("text_editor", "Editor", "📝", AppCategory.UTILITIES) { TextEditorApp(it) }
private val TERMINAL_INFO = AppInfo("terminal", "Terminal", "💻", AppCategory.SYSTEM) { TerminalApp(it) }
private val WEB_BROWSER_INFO = AppInfo("web_browser", "Browser", "🌐", AppCategory.NETWORK) { WebBrowserApp(it) }
private val SETTINGS_INFO = AppInfo("settings", "Settings", "⚙", AppCategory.SYSTEM) { SettingsApp(it) }
private val SYSTEM_MONITOR_INFO = AppInfo("system_monitor", "Monitor", "📊", AppCategory.SYSTEM) { SystemMonitorApp(it) }
private val LUA_INFO = AppInfo("lua", "Lua", "🌙", AppCategory.DEVELOPMENT) { LuaConsoleApp(it) }
private val CALCULATOR_INFO = AppInfo("calculator", "Calculator", "🔢", AppCategory.UTILITIES) { CalculatorApp(it) }
private val NOTES_INFO = AppInfo("notes", "Notes", "📋", AppCategory.UTILITIES) { NotesApp(it) }

class FileManagerApp(os: KotlinOS) : Application(os, FILE_MANAGER_INFO) {
    override fun onCreate() {
        createWindow("Files", 5, 3, 60, 20)
    }
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
}

class TextEditorApp(os: KotlinOS) : Application(os, TEXT_EDITOR_INFO) {
    override fun onCreate() {
        createWindow("Editor", 10, 4, 70, 22)
    }
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
}

class TerminalApp(os: KotlinOS) : Application(os, TERMINAL_INFO) {
    override fun onCreate() {
        createWindow("Terminal", 8, 5, 80, 24)
    }
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
}

class WebBrowserApp(os: KotlinOS) : Application(os, WEB_BROWSER_INFO) {
    override fun onCreate() {
        createWindow("Browser", 3, 2, 90, 28)
    }
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
}

class SettingsApp(os: KotlinOS) : Application(os, SETTINGS_INFO) {
    override fun onCreate() {
        createWindow("Settings", 15, 5, 50, 18)
    }
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
}

class SystemMonitorApp(os: KotlinOS) : Application(os, SYSTEM_MONITOR_INFO) {
    override fun onCreate() {
        createWindow("System Monitor", 12, 4, 60, 20)
    }
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
}

class LuaConsoleApp(os: KotlinOS) : Application(os, LUA_INFO) {
    override fun onCreate() {
        createWindow("Lua", 6, 4, 70, 20)
    }
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
}

class CalculatorApp(os: KotlinOS) : Application(os, CALCULATOR_INFO) {
    override fun onCreate() {
        createWindow("Calculator", 30, 8, 25, 14)
    }
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
}

class NotesApp(os: KotlinOS) : Application(os, NOTES_INFO) {
    override fun onCreate() {
        createWindow("Notes", 20, 6, 40, 18)
    }
    override fun onStart() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onStop() {}
    override fun onDestroy() {}
}
