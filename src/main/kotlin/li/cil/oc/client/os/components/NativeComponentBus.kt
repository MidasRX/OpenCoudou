package li.cil.oc.client.os.components

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Native Component Bus for SkibidiOS2.
 * Provides zero-copy access to OpenComputers hardware components.
 * Replaces Lua component.proxy() with native Kotlin interfaces.
 */

// ============================================================
// Component Types
// ============================================================

enum class ComponentType {
    COMPUTER,
    SCREEN,
    GPU,
    KEYBOARD,
    FILESYSTEM,
    EEPROM,
    INTERNET,
    MODEM,
    REDSTONE,
    ROBOT,
    DRONE,
    CRAFTING,
    INVENTORY_CONTROLLER,
    TANK_CONTROLLER,
    GEOLYZER,
    HOLOGRAM,
    DEBUG,
    WORLD_SENSOR,
    NAVIGATION,
    SIGN,
    CHUNKLOADER,
    EXPERIENCE,
    GENERATOR,
    TRANSPOSER,
    TUNNEL,
    ABSTRACT_BUS,
    DATA,
    DATABASE,
    MICROCONTROLLER,
    TABLET,
    LEASH,
    TRACTOR_BEAM,
    PISTON,
    UNKNOWN
}

// ============================================================
// Base Component Interface
// ============================================================

interface Component {
    val address: String
    val type: ComponentType
    val slot: Int
    
    fun methods(): List<String>
    fun invoke(method: String, vararg args: Any?): Any?
    fun documentation(method: String): String?
}

// ============================================================
// Component Proxy Classes
// ============================================================

class GpuComponent(
    override val address: String,
    override val slot: Int = -1,
    private val bus: NativeComponentBus
) : Component {
    override val type = ComponentType.GPU
    
    // GPU state
    var boundScreen: String? = null
    private var foreground = 0xFFFFFF
    private var background = 0x000000
    private var resolution = Pair(80, 25)
    private var maxResolution = Pair(160, 50)
    private var depth = 8 // Color depth
    
    override fun methods() = listOf(
        "bind", "getScreen",
        "getBackground", "setBackground", "getForeground", "setForeground",
        "getPaletteColor", "setPaletteColor",
        "maxDepth", "getDepth", "setDepth",
        "maxResolution", "getResolution", "setResolution", "getViewport", "setViewport",
        "get", "set", "copy", "fill"
    )
    
    override fun invoke(method: String, vararg args: Any?): Any? {
        return when (method) {
            "bind" -> bind(args[0] as String, args.getOrNull(1) as? Boolean ?: true)
            "getScreen" -> boundScreen
            "getBackground" -> Pair(background, false)
            "setBackground" -> { val old = background; background = (args[0] as Number).toInt(); old }
            "getForeground" -> Pair(foreground, false)
            "setForeground" -> { val old = foreground; foreground = (args[0] as Number).toInt(); old }
            "maxDepth" -> depth
            "getDepth" -> depth
            "setDepth" -> { depth = (args[0] as Number).toInt().coerceIn(1, 8); depth }
            "maxResolution" -> maxResolution
            "getResolution" -> resolution
            "setResolution" -> {
                val w = (args[0] as Number).toInt().coerceIn(1, maxResolution.first)
                val h = (args[1] as Number).toInt().coerceIn(1, maxResolution.second)
                resolution = Pair(w, h)
                true
            }
            "get" -> get((args[0] as Number).toInt(), (args[1] as Number).toInt())
            "set" -> set(
                (args[0] as Number).toInt(),
                (args[1] as Number).toInt(),
                args[2] as String,
                args.getOrNull(3) as? Boolean ?: false
            )
            "copy" -> copy(
                (args[0] as Number).toInt(), (args[1] as Number).toInt(),
                (args[2] as Number).toInt(), (args[3] as Number).toInt(),
                (args[4] as Number).toInt(), (args[5] as Number).toInt()
            )
            "fill" -> fill(
                (args[0] as Number).toInt(), (args[1] as Number).toInt(),
                (args[2] as Number).toInt(), (args[3] as Number).toInt(),
                args[4] as String
            )
            else -> null
        }
    }
    
    override fun documentation(method: String) = when (method) {
        "bind" -> "bind(address:string[, reset:boolean=true]):boolean -- Binds the GPU to the screen at the specified address."
        "setBackground" -> "setBackground(color:number[, isPalette:boolean]):number -- Sets the background color."
        "setForeground" -> "setForeground(color:number[, isPalette:boolean]):number -- Sets the foreground color."
        "set" -> "set(x:number, y:number, value:string[, vertical:boolean]):boolean -- Writes a string to the screen."
        "fill" -> "fill(x:number, y:number, w:number, h:number, char:string):boolean -- Fills a rectangle with a character."
        else -> null
    }
    
    // Direct GPU methods
    fun bind(screenAddress: String, reset: Boolean = true): Boolean {
        boundScreen = screenAddress
        if (reset) {
            foreground = 0xFFFFFF
            background = 0x000000
        }
        return true
    }
    
    fun get(x: Int, y: Int): Triple<String, Int, Int> {
        // Return character, foreground, background at position
        return Triple(" ", foreground, background)
    }
    
    fun set(x: Int, y: Int, value: String, vertical: Boolean = false): Boolean {
        // In native mode, this would render directly to the screen
        bus.onGpuWrite?.invoke(this, x, y, value, foreground, background, vertical)
        return true
    }
    
    fun copy(x: Int, y: Int, w: Int, h: Int, tx: Int, ty: Int): Boolean {
        bus.onGpuCopy?.invoke(this, x, y, w, h, tx, ty)
        return true
    }
    
    fun fill(x: Int, y: Int, w: Int, h: Int, char: String): Boolean {
        val c = char.firstOrNull() ?: ' '
        bus.onGpuFill?.invoke(this, x, y, w, h, c, foreground, background)
        return true
    }
}

class ScreenComponent(
    override val address: String,
    override val slot: Int = -1
) : Component {
    override val type = ComponentType.SCREEN
    
    var width = 80
    var height = 25
    var colorDepth = 8
    var touchMode = TouchMode.SINGLE
    var precise = false
    
    enum class TouchMode { NONE, SINGLE, MULTI }
    
    override fun methods() = listOf(
        "isOn", "turnOn", "turnOff",
        "getAspectRatio", "getKeyboards",
        "setPrecise", "isPrecise",
        "setTouchModeInverted", "isTouchModeInverted"
    )
    
    override fun invoke(method: String, vararg args: Any?): Any? {
        return when (method) {
            "isOn" -> true
            "turnOn" -> true
            "turnOff" -> true
            "getAspectRatio" -> Pair(width.toDouble() / height, 1.0)
            "getKeyboards" -> emptyList<String>()
            "setPrecise" -> { precise = args[0] as Boolean; precise }
            "isPrecise" -> precise
            else -> null
        }
    }
    
    override fun documentation(method: String) = null
}

class FilesystemComponent(
    override val address: String,
    override val slot: Int = -1,
    val label: String = "disk",
    val isReadOnly: Boolean = false,
    val spaceTotal: Long = 1024 * 1024,
    var spaceUsed: Long = 0
) : Component {
    override val type = ComponentType.FILESYSTEM
    
    private val files = ConcurrentHashMap<String, ByteArray>()
    private val directories = mutableSetOf("/")
    
    override fun methods() = listOf(
        "getLabel", "setLabel",
        "isReadOnly", "spaceTotal", "spaceUsed",
        "exists", "isDirectory", "size", "lastModified",
        "list", "makeDirectory", "remove", "rename",
        "open", "read", "write", "seek", "close"
    )
    
    override fun invoke(method: String, vararg args: Any?): Any? {
        return when (method) {
            "getLabel" -> label
            "isReadOnly" -> isReadOnly
            "spaceTotal" -> spaceTotal
            "spaceUsed" -> spaceUsed
            "exists" -> exists(args[0] as String)
            "isDirectory" -> isDirectory(args[0] as String)
            "size" -> getSize(args[0] as String)
            "list" -> list(args[0] as String)
            "makeDirectory" -> makeDirectory(args[0] as String)
            "remove" -> remove(args[0] as String)
            else -> null
        }
    }
    
    override fun documentation(method: String) = null
    
    fun exists(path: String): Boolean = files.containsKey(path) || directories.contains(path)
    fun isDirectory(path: String): Boolean = directories.contains(path)
    fun getSize(path: String): Long = files[path]?.size?.toLong() ?: 0
    
    fun list(path: String): List<String>? {
        if (!isDirectory(path)) return null
        val prefix = if (path.endsWith("/")) path else "$path/"
        val items = mutableSetOf<String>()
        
        files.keys.filter { it.startsWith(prefix) }.forEach { 
            val relative = it.removePrefix(prefix)
            val firstPart = relative.split("/").first()
            items.add(firstPart)
        }
        directories.filter { it.startsWith(prefix) && it != prefix }.forEach {
            val relative = it.removePrefix(prefix)
            val firstPart = relative.split("/").first()
            if (firstPart.isNotEmpty()) items.add("$firstPart/")
        }
        
        return items.toList()
    }
    
    fun makeDirectory(path: String): Boolean {
        if (isReadOnly) return false
        directories.add(if (path.endsWith("/")) path else "$path/")
        return true
    }
    
    fun remove(path: String): Boolean {
        if (isReadOnly) return false
        files.remove(path)
        directories.remove(path)
        return true
    }
    
    fun readFile(path: String): ByteArray? = files[path]
    
    fun writeFile(path: String, data: ByteArray): Boolean {
        if (isReadOnly) return false
        val oldSize = files[path]?.size ?: 0
        files[path] = data
        spaceUsed += data.size - oldSize
        return true
    }
}

class InternetComponent(
    override val address: String,
    override val slot: Int = -1
) : Component {
    override val type = ComponentType.INTERNET
    
    override fun methods() = listOf(
        "isHttpEnabled", "isTcpEnabled",
        "request", "connect"
    )
    
    override fun invoke(method: String, vararg args: Any?): Any? {
        return when (method) {
            "isHttpEnabled" -> true
            "isTcpEnabled" -> true
            else -> null
        }
    }
    
    override fun documentation(method: String) = null
}

class RedstoneComponent(
    override val address: String,
    override val slot: Int = -1
) : Component {
    override val type = ComponentType.REDSTONE
    
    private val input = IntArray(6) { 0 }
    private val output = IntArray(6) { 0 }
    
    override fun methods() = listOf(
        "getInput", "getOutput", "setOutput",
        "getBundledInput", "getBundledOutput", "setBundledOutput",
        "getWirelessInput", "getWirelessOutput", "setWirelessOutput",
        "getWirelessFrequency", "setWirelessFrequency", "getWakeThreshold", "setWakeThreshold"
    )
    
    override fun invoke(method: String, vararg args: Any?): Any? {
        return when (method) {
            "getInput" -> input[(args[0] as Number).toInt()]
            "getOutput" -> output[(args[0] as Number).toInt()]
            "setOutput" -> {
                output[(args[0] as Number).toInt()] = (args[1] as Number).toInt()
                true
            }
            else -> null
        }
    }
    
    override fun documentation(method: String) = null
}

class ComputerComponent(
    override val address: String,
    override val slot: Int = -1
) : Component {
    override val type = ComponentType.COMPUTER
    
    var energy = 10000.0
    var maxEnergy = 10000.0
    val users = mutableListOf<String>()
    
    override fun methods() = listOf(
        "start", "stop", "isRunning",
        "beep", "getDeviceInfo",
        "getProgramLocations", "pushSignal",
        "removeUser", "addUser", "isScreenAvailable",
        "getArchitecture", "setArchitecture", "getArchitectures"
    )
    
    override fun invoke(method: String, vararg args: Any?): Any? {
        return when (method) {
            "start" -> true
            "stop" -> true
            "isRunning" -> true
            "beep" -> true
            "getArchitecture" -> "Kotlin"
            "getArchitectures" -> listOf("Kotlin", "Lua")
            else -> null
        }
    }
    
    override fun documentation(method: String) = null
}

// ============================================================
// Component Bus
// ============================================================

class NativeComponentBus {
    private val components = ConcurrentHashMap<String, Component>()
    private val componentsByType = ConcurrentHashMap<ComponentType, MutableList<Component>>()
    
    // Callbacks for GPU operations (to be hooked by the renderer)
    var onGpuWrite: ((GpuComponent, Int, Int, String, Int, Int, Boolean) -> Unit)? = null
    var onGpuFill: ((GpuComponent, Int, Int, Int, Int, Char, Int, Int) -> Unit)? = null
    var onGpuCopy: ((GpuComponent, Int, Int, Int, Int, Int, Int) -> Unit)? = null
    
    fun register(component: Component) {
        components[component.address] = component
        componentsByType.getOrPut(component.type) { mutableListOf() }.add(component)
    }
    
    fun unregister(address: String): Component? {
        val component = components.remove(address)
        component?.let { componentsByType[it.type]?.remove(it) }
        return component
    }
    
    fun get(address: String): Component? = components[address]
    
    fun <T : Component> getByType(type: ComponentType): List<T> {
        @Suppress("UNCHECKED_CAST")
        return componentsByType[type]?.toList() as? List<T> ?: emptyList()
    }
    
    fun getPrimaryGpu(): GpuComponent? = getByType<GpuComponent>(ComponentType.GPU).firstOrNull()
    
    fun getPrimaryScreen(): ScreenComponent? = getByType<ScreenComponent>(ComponentType.SCREEN).firstOrNull()
    
    fun getFilesystem(label: String): FilesystemComponent? = 
        getByType<FilesystemComponent>(ComponentType.FILESYSTEM).find { it.label == label }
    
    fun list(): Map<String, ComponentType> = 
        components.mapValues { it.value.type }
    
    fun listByType(type: ComponentType): List<String> =
        componentsByType[type]?.map { it.address } ?: emptyList()
    
    /** Get the first component of the given string type name (for app compatibility). */
    fun getFirstComponent(typeName: String): Any? {
        val type = ComponentType.entries.find { it.name.equals(typeName, ignoreCase = true) }
            ?: return null
        return componentsByType[type]?.firstOrNull()
    }
    
    /** Alias for getFirstComponent for compatibility. */
    fun getPrimaryComponent(typeName: String) = getFirstComponent(typeName)
    
    fun invoke(address: String, method: String, vararg args: Any?): Any? {
        return components[address]?.invoke(method, *args)
    }
    
    fun methods(address: String): List<String>? = components[address]?.methods()
    
    fun documentation(address: String, method: String): String? =
        components[address]?.documentation(method)
    
    fun generateAddress(): String = UUID.randomUUID().toString()
    
    // Initialize default components
    fun initializeDefaults() {
        register(ComputerComponent(generateAddress()))
        register(GpuComponent(generateAddress(), bus = this))
        register(ScreenComponent(generateAddress()))
        register(FilesystemComponent(generateAddress(), label = "root", spaceTotal = 4 * 1024 * 1024))
        register(FilesystemComponent(generateAddress(), label = "home", spaceTotal = 2 * 1024 * 1024))
        register(InternetComponent(generateAddress()))
    }
}
