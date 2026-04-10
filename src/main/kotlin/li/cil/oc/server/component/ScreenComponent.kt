package li.cil.oc.server.component

import li.cil.oc.util.TextBuffer

/**
 * Screen component that provides the display buffer.
 * Can be bound to a GPU for rendering.
 */
class ScreenComponent(private var tier: Int = 1) : AbstractComponent("screen") {
    
    // Text buffer for this screen
    var buffer: TextBuffer
        private set
    
    // Screen dimensions based on tier
    private val defaultResolutions = mapOf(
        1 to Pair(50, 16),
        2 to Pair(80, 25),
        3 to Pair(160, 50)
    )
    
    // Is screen currently on?
    var isOn = true
        private set
    
    // Precise mode (sub-character rendering)
    var preciseMode = false
    
    // Callback for when screen content changes (for syncing)
    var onBufferChanged: (() -> Unit)? = null
    
    // Mark screen as needing sync
    fun markDirty() {
        onBufferChanged?.invoke()
    }
    
    init {
        val res = defaultResolutions[tier] ?: Pair(50, 16)
        buffer = TextBuffer(res.first, res.second)
        
        registerMethod("isOn", doc = "isOn():boolean -- Whether screen is on") { _ ->
            arrayOf(isOn)
        }
        
        registerMethod("turnOn", doc = "turnOn():boolean -- Turn screen on") { _ ->
            val wasOff = !isOn
            turnOn()
            arrayOf(wasOff)
        }
        
        registerMethod("turnOff", doc = "turnOff():boolean -- Turn screen off") { _ ->
            val wasOn = isOn
            turnOff()
            arrayOf(wasOn)
        }
        
        registerMethod("getAspectRatio", doc = "getAspectRatio():number,number -- Get screen aspect ratio") { _ ->
            arrayOf(1, 1) // 1:1 blocks
        }
        
        registerMethod("getKeyboards", doc = "getKeyboards():table -- Get connected keyboards") { _ ->
            // Would return list of keyboard addresses
            arrayOf(emptyArray<String>())
        }
        
        registerMethod("setPrecise", doc = "setPrecise(precise:boolean):boolean -- Set precise mode") { args ->
            val precise = args.getOrNull(0) as? Boolean ?: false
            val old = preciseMode
            preciseMode = precise
            arrayOf(old)
        }
        
        registerMethod("isPrecise", doc = "isPrecise():boolean -- Check precise mode") { _ ->
            arrayOf(preciseMode)
        }
    }
    
    fun turnOn() {
        isOn = true
    }
    
    fun turnOff() {
        isOn = false
    }
    
    fun setTier(newTier: Int) {
        tier = newTier
        val res = defaultResolutions[tier] ?: Pair(50, 16)
        buffer = TextBuffer(res.first, res.second)
    }
    
    fun getTier(): Int = tier
}
