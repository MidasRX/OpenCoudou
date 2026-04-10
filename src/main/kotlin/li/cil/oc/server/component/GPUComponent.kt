package li.cil.oc.server.component

import li.cil.oc.util.TextBuffer
import li.cil.oc.util.OCLogger

/**
 * GPU component that handles drawing to a screen's text buffer.
 * Provides the gpu API to Lua programs.
 */
class GPUComponent(tier: Int = 1, private var boundScreen: ScreenComponent? = null) : AbstractComponent("gpu") {
    
    // GPU tier determines max resolution
    var tier: Int = tier
    
    // Color depth: 1 = 1-bit, 4 = 4-bit (16 colors), 8 = 8-bit (256 colors)
    var depth: Int = when (tier) { 1 -> 1; 2 -> 4; else -> 8 }
    
    private val maxResolutions = mapOf(
        1 to Pair(50, 16),
        2 to Pair(80, 25),
        3 to Pair(160, 50)
    )
    
    // Network reference for looking up screens
    var network: ComponentNetwork? = null
    
    init {
        // Register GPU methods callable from Lua
        registerMethod("bind", doc = "bind(address:string[,reset:boolean]):boolean -- Bind to a screen") { args ->
            val address = args.getOrNull(0)?.toString() 
                ?: return@registerMethod arrayOf(false, "no address provided")
            val reset = args.getOrNull(1) as? Boolean ?: true
            
            OCLogger.component("GPU", "bind", "Binding to screen: $address")
            
            // Look up the screen in the network
            val net = network ?: return@registerMethod arrayOf(false, "no network")
            val component = net.get(address)
            
            if (component == null) {
                OCLogger.component("GPU", "bind", "Screen not found: $address")
                return@registerMethod arrayOf(false, "no such component")
            }
            
            if (component !is ScreenComponent) {
                return@registerMethod arrayOf(false, "not a screen")
            }
            
            boundScreen = component
            OCLogger.component("GPU", "bind", "Successfully bound to screen")
            
            if (reset) {
                // Reset the buffer to default state
                boundScreen?.buffer?.clear()
                boundScreen?.markDirty()
            }
            
            arrayOf(true)
        }
        
        registerMethod("getScreen", doc = "getScreen():string -- Get bound screen address") { _ ->
            arrayOf(boundScreen?.address)
        }
        
        registerMethod("getResolution", doc = "getResolution():number,number -- Get current resolution") { _ ->
            val buffer = boundScreen?.buffer
            if (buffer != null) {
                arrayOf(buffer.width, buffer.height)
            } else {
                arrayOf(0, 0)
            }
        }
        
        registerMethod("setResolution", doc = "setResolution(w:number,h:number):boolean -- Set resolution") { args ->
            val w = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(false, "invalid width")
            val h = (args.getOrNull(1) as? Number)?.toInt() ?: return@registerMethod arrayOf(false, "invalid height")
            val max = maxResolutions[this.tier] ?: Pair(50, 16)
            if (w > max.first || h > max.second) {
                return@registerMethod arrayOf(false, "resolution too high for GPU tier")
            }
            boundScreen?.buffer?.resize(w, h)
            arrayOf(true)
        }
        
        registerMethod("maxResolution", doc = "maxResolution():number,number -- Get max resolution") { _ ->
            val max = maxResolutions[this.tier] ?: Pair(50, 16)
            arrayOf(max.first, max.second)
        }
        
        registerMethod("setForeground", doc = "setForeground(color:number):number -- Set foreground color") { args ->
            val color = (args.getOrNull(0) as? Number)?.toInt() ?: 0xFFFFFF
            val old = boundScreen?.buffer?.foreground ?: 0xFFFFFF
            boundScreen?.buffer?.foreground = color
            arrayOf(old)
        }
        
        registerMethod("getForeground", doc = "getForeground():number -- Get foreground color") { _ ->
            arrayOf(boundScreen?.buffer?.foreground ?: 0xFFFFFF)
        }
        
        registerMethod("setBackground", doc = "setBackground(color:number):number -- Set background color") { args ->
            val color = (args.getOrNull(0) as? Number)?.toInt() ?: 0x000000
            val old = boundScreen?.buffer?.background ?: 0x000000
            boundScreen?.buffer?.background = color
            arrayOf(old)
        }
        
        registerMethod("getBackground", doc = "getBackground():number -- Get background color") { _ ->
            arrayOf(boundScreen?.buffer?.background ?: 0x000000)
        }
        
        registerMethod("set", doc = "set(x:number,y:number,value:string[,vertical:boolean]) -- Set text at position") { args ->
            val x = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: return@registerMethod arrayOf(false)
            val y = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: return@registerMethod arrayOf(false)
            val text = args.getOrNull(2)?.toString() ?: return@registerMethod arrayOf(false)
            val vertical = args.getOrNull(3) as? Boolean ?: false
            boundScreen?.buffer?.set(x, y, text, vertical)
            boundScreen?.markDirty()
            arrayOf(true)
        }
        
        registerMethod("get", doc = "get(x:number,y:number):string,number,number -- Get char and colors at position") { args ->
            val x = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: return@registerMethod arrayOf(" ", 0xFFFFFF, 0x000000)
            val y = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: return@registerMethod arrayOf(" ", 0xFFFFFF, 0x000000)
            val buffer = boundScreen?.buffer ?: return@registerMethod arrayOf(" ", 0xFFFFFF, 0x000000)
            val char = buffer.getChar(x, y).toChar().toString()
            val fg = buffer.getForeground(x, y)
            val bg = buffer.getBackground(x, y)
            arrayOf(char, fg, bg)
        }
        
        registerMethod("fill", doc = "fill(x:number,y:number,w:number,h:number,char:string):boolean -- Fill rectangle") { args ->
            val x = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: return@registerMethod arrayOf(false)
            val y = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: return@registerMethod arrayOf(false)
            val w = (args.getOrNull(2) as? Number)?.toInt() ?: return@registerMethod arrayOf(false)
            val h = (args.getOrNull(3) as? Number)?.toInt() ?: return@registerMethod arrayOf(false)
            val char = args.getOrNull(4)?.toString()?.firstOrNull()?.code ?: ' '.code
            boundScreen?.buffer?.fill(x, y, w, h, char)
            boundScreen?.markDirty()
            arrayOf(true)
        }
        
        registerMethod("copy", doc = "copy(x:number,y:number,w:number,h:number,tx:number,ty:number):boolean -- Copy region") { args ->
            val x = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: return@registerMethod arrayOf(false)
            val y = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: return@registerMethod arrayOf(false)
            val w = (args.getOrNull(2) as? Number)?.toInt() ?: return@registerMethod arrayOf(false)
            val h = (args.getOrNull(3) as? Number)?.toInt() ?: return@registerMethod arrayOf(false)
            val tx = (args.getOrNull(4) as? Number)?.toInt() ?: return@registerMethod arrayOf(false)
            val ty = (args.getOrNull(5) as? Number)?.toInt() ?: return@registerMethod arrayOf(false)
            boundScreen?.buffer?.copy(x, y, w, h, x + tx, y + ty)
            boundScreen?.markDirty()
            arrayOf(true)
        }
        
        registerMethod("getDepth", doc = "getDepth():number -- Get color depth") { _ ->
            arrayOf(this.depth)
        }
        
        registerMethod("maxDepth", doc = "maxDepth():number -- Get max color depth") { _ ->
            arrayOf(when (this.tier) { 1 -> 1; 2 -> 4; else -> 8 })
        }
        
        registerMethod("setDepth", doc = "setDepth(depth:number):boolean -- Set color depth") { args ->
            val d = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(false)
            val maxD = when (this.tier) { 1 -> 1; 2 -> 4; else -> 8 }
            if (d > maxD) return@registerMethod arrayOf(false)
            this.depth = d
            arrayOf(true)
        }
    }
    
    fun bind(screen: ScreenComponent?) {
        boundScreen = screen
    }
    
    fun getBuffer(): TextBuffer? = boundScreen?.buffer
}
