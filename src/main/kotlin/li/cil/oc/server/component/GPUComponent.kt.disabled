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
    
    // Custom palette (16 colors, can be modified by user)
    private val palette = intArrayOf(
        0x000000, 0x000040, 0x004000, 0x004040,
        0x400000, 0x400040, 0x404000, 0xC0C0C0,
        0x404040, 0x0000FF, 0x00FF00, 0x00FFFF,
        0xFF0000, 0xFF00FF, 0xFFFF00, 0xFFFFFF
    )
    
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
        
        registerMethod("setForeground", doc = "setForeground(color:number[,isPalette:boolean]):number,boolean -- Set foreground color") { args ->
            val color = (args.getOrNull(0) as? Number)?.toInt() ?: 0xFFFFFF
            val isPalette = args.getOrNull(1) as? Boolean ?: false
            val old = boundScreen?.buffer?.foreground ?: 0xFFFFFF
            val actualColor = if (isPalette) getPaletteColorValue(color) else color
            boundScreen?.buffer?.foreground = actualColor
            arrayOf(old, false)
        }
        
        registerMethod("getForeground", doc = "getForeground():number,boolean -- Get foreground color") { _ ->
            arrayOf(boundScreen?.buffer?.foreground ?: 0xFFFFFF, false)
        }
        
        registerMethod("setBackground", doc = "setBackground(color:number[,isPalette:boolean]):number,boolean -- Set background color") { args ->
            val color = (args.getOrNull(0) as? Number)?.toInt() ?: 0x000000
            val isPalette = args.getOrNull(1) as? Boolean ?: false
            val old = boundScreen?.buffer?.background ?: 0x000000
            val actualColor = if (isPalette) getPaletteColorValue(color) else color
            boundScreen?.buffer?.background = actualColor
            arrayOf(old, false)
        }
        
        registerMethod("getBackground", doc = "getBackground():number,boolean -- Get background color") { _ ->
            arrayOf(boundScreen?.buffer?.background ?: 0x000000, false)
        }
        
        registerMethod("set", doc = "set(x:number,y:number,value:string[,vertical:boolean]) -- Set text at position") { args ->
            val x = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: return@registerMethod arrayOf(false)
            val y = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: return@registerMethod arrayOf(false)
            val text = args.getOrNull(2)?.toString() ?: return@registerMethod arrayOf(false)
            val vertical = args.getOrNull(3) as? Boolean ?: false
            
            if (text.isNotEmpty()) {
                OCLogger.debug("[GPU.set] Writing at ($x,$y): '${text.take(20)}' (len=${text.length}), screen=${boundScreen != null}\")")
            }
            
            boundScreen?.buffer?.set(x, y, text, vertical)
            boundScreen?.markDirty()
            arrayOf(true)
        }
        
        registerMethod("get", doc = "get(x:number,y:number):string,number,number,number,number -- Get char and colors at position") { args ->
            val x = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: return@registerMethod arrayOf(" ", 0xFFFFFF, 0x000000, null, null)
            val y = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: return@registerMethod arrayOf(" ", 0xFFFFFF, 0x000000, null, null)
            val buffer = boundScreen?.buffer ?: return@registerMethod arrayOf(" ", 0xFFFFFF, 0x000000, null, null)
            val char = buffer.getChar(x, y).toChar().toString()
            val fg = buffer.getForeground(x, y)
            val bg = buffer.getBackground(x, y)
            arrayOf(char, fg, bg, null, null)
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
        
        registerMethod("getViewport", doc = "getViewport():number,number -- Get viewport size") { _ ->
            val buffer = boundScreen?.buffer
            if (buffer != null) {
                arrayOf(buffer.width, buffer.height)
            } else {
                arrayOf(0, 0)
            }
        }
        
        registerMethod("setViewport", doc = "setViewport(w:number,h:number):boolean -- Set viewport size") { args ->
            val w = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(false)
            val h = (args.getOrNull(1) as? Number)?.toInt() ?: return@registerMethod arrayOf(false)
            // Viewport is same as resolution in our implementation
            val max = maxResolutions[this.tier] ?: Pair(50, 16)
            if (w > max.first || h > max.second) {
                return@registerMethod arrayOf(false, "viewport too large")
            }
            boundScreen?.buffer?.resize(w, h)
            arrayOf(true)
        }
        
        registerMethod("getPaletteColor", doc = "getPaletteColor(index:number):number -- Get palette color") { args ->
            val index = (args.getOrNull(0) as? Number)?.toInt() ?: 0
            if (index < 0 || index >= 16) {
                throw IllegalArgumentException("invalid palette index")
            }
            arrayOf(palette[index])
        }
        
        registerMethod("setPaletteColor", doc = "setPaletteColor(index:number,color:number):number -- Set palette color") { args ->
            val index = (args.getOrNull(0) as? Number)?.toInt() ?: 0 
            val color = (args.getOrNull(1) as? Number)?.toInt() ?: 0
            if (index < 0 || index >= 16) {
                throw IllegalArgumentException("invalid palette index")
            }
            val old = palette[index]
            palette[index] = color and 0xFFFFFF
            arrayOf(old)
        }
        
        // VRAM buffer methods (simplified - single buffer only for now)
        registerMethod("getActiveBuffer", doc = "getActiveBuffer():number -- Get active buffer index (0=screen)") { _ ->
            arrayOf(activeBuffer)
        }
        
        registerMethod("setActiveBuffer", doc = "setActiveBuffer(index:number):number -- Set active buffer index") { args ->
            val index = (args.getOrNull(0) as? Number)?.toInt() ?: 0
            val old = activeBuffer
            // Only support screen (0) for now
            if (index != 0 && !vramBuffers.containsKey(index)) {
                return@registerMethod arrayOf(null, "invalid buffer index")
            }
            activeBuffer = index
            arrayOf(old)
        }
        
        registerMethod("buffers", doc = "buffers():table -- Get list of allocated buffer indexes") { _ ->
            arrayOf(vramBuffers.keys.toTypedArray())
        }
        
        registerMethod("allocateBuffer", doc = "allocateBuffer([w:number,h:number]):number -- Allocate new VRAM buffer") { args ->
            val max = maxResolutions[this.tier] ?: Pair(50, 16)
            val w = (args.getOrNull(0) as? Number)?.toInt() ?: max.first
            val h = (args.getOrNull(1) as? Number)?.toInt() ?: max.second
            
            if (w <= 0 || h <= 0) {
                return@registerMethod arrayOf(null, "invalid dimensions")
            }
            if (w > max.first || h > max.second) {
                return@registerMethod arrayOf(null, "resolution too high")
            }
            
            val usedMemory = vramBuffers.values.sumOf { it.width * it.height }
            val needed = w * h
            if (usedMemory + needed > totalVRAM) {
                return@registerMethod arrayOf(null, "not enough video memory")
            }
            
            val index = nextBufferIndex++
            vramBuffers[index] = TextBuffer(w, h)
            arrayOf(index)
        }
        
        registerMethod("freeBuffer", doc = "freeBuffer([index:number]):boolean -- Free VRAM buffer") { args ->
            val index = (args.getOrNull(0) as? Number)?.toInt() ?: activeBuffer
            if (index == 0) {
                return@registerMethod arrayOf(null, "cannot free screen buffer")
            }
            val removed = vramBuffers.remove(index)
            if (activeBuffer == index) {
                activeBuffer = 0
            }
            arrayOf(removed != null)
        }
        
        registerMethod("freeAllBuffers", doc = "freeAllBuffers():number -- Free all VRAM buffers") { _ ->
            val count = vramBuffers.size
            vramBuffers.clear()
            activeBuffer = 0
            arrayOf(count)
        }
        
        registerMethod("totalMemory", doc = "totalMemory():number -- Get total VRAM size") { _ ->
            arrayOf(totalVRAM)
        }
        
        registerMethod("freeMemory", doc = "freeMemory():number -- Get free VRAM") { _ ->
            val used = vramBuffers.values.sumOf { it.width * it.height }
            arrayOf(totalVRAM - used)
        }
        
        registerMethod("getBufferSize", doc = "getBufferSize([index:number]):number,number -- Get buffer dimensions") { args ->
            val index = (args.getOrNull(0) as? Number)?.toInt() ?: activeBuffer
            if (index == 0) {
                val buffer = boundScreen?.buffer
                if (buffer != null) {
                    return@registerMethod arrayOf(buffer.width, buffer.height)
                }
                return@registerMethod arrayOf(0, 0)
            }
            val buffer = vramBuffers[index]
            if (buffer != null) {
                arrayOf(buffer.width, buffer.height)
            } else {
                arrayOf(null, "invalid buffer index")
            }
        }
        
        registerMethod("bitblt", doc = "bitblt([dst:number,col:number,row:number,width:number,height:number,src:number,fromCol:number,fromRow:number]):boolean -- Copy from buffer to buffer") { args ->
            val dst = (args.getOrNull(0) as? Number)?.toInt() ?: 0
            val col = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
            val row = (args.getOrNull(2) as? Number)?.toInt()?.minus(1) ?: 0
            val width = (args.getOrNull(3) as? Number)?.toInt()
            val height = (args.getOrNull(4) as? Number)?.toInt()
            val src = (args.getOrNull(5) as? Number)?.toInt() ?: activeBuffer
            val fromCol = (args.getOrNull(6) as? Number)?.toInt()?.minus(1) ?: 0
            val fromRow = (args.getOrNull(7) as? Number)?.toInt()?.minus(1) ?: 0
            
            val srcBuffer = if (src == 0) boundScreen?.buffer else vramBuffers[src]
            val dstBuffer = if (dst == 0) boundScreen?.buffer else vramBuffers[dst]
            
            if (srcBuffer == null || dstBuffer == null) {
                return@registerMethod arrayOf(false, "invalid buffer")
            }
            
            val w = width ?: srcBuffer.width
            val h = height ?: srcBuffer.height
            
            // Copy region
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val srcX = fromCol + x
                    val srcY = fromRow + y
                    val dstX = col + x
                    val dstY = row + y
                    
                    if (srcX in 0 until srcBuffer.width && srcY in 0 until srcBuffer.height &&
                        dstX in 0 until dstBuffer.width && dstY in 0 until dstBuffer.height) {
                        val char = srcBuffer.getChar(srcX, srcY)
                        val fg = srcBuffer.getForeground(srcX, srcY)
                        val bg = srcBuffer.getBackground(srcX, srcY)
                        dstBuffer.setChar(dstX, dstY, char)
                        dstBuffer.setForeground(dstX, dstY, fg)
                        dstBuffer.setBackground(dstX, dstY, bg)
                    }
                }
            }
            
            if (dst == 0) {
                boundScreen?.markDirty()
            }
            arrayOf(true)
        }
    }
    
    // Active buffer index (0 = screen)
    private var activeBuffer = 0
    
    // VRAM buffers (index -> buffer)
    private val vramBuffers = mutableMapOf<Int, TextBuffer>()
    private var nextBufferIndex = 1
    
    // Total VRAM based on tier
    private val totalVRAM: Int
        get() = when (tier) {
            1 -> 50 * 16  // Tier 1: ~800 cells
            2 -> 80 * 25 * 2  // Tier 2: ~4000 cells
            else -> 160 * 50 * 4  // Tier 3: ~32000 cells
        }
    
    fun bind(screen: ScreenComponent?) {
        boundScreen = screen
    }
    
    fun getBuffer(): TextBuffer? = boundScreen?.buffer
    
    /**
     * Get palette color (uses current palette which may have been modified).
     */
    private fun getPaletteColorValue(index: Int): Int {
        return palette.getOrElse(index.coerceIn(0, 15)) { 0x000000 }
    }
    
    /**
     * Reset palette to default OC colors.
     */
    fun resetPalette() {
        val defaultPalette = intArrayOf(
            0x000000, 0x000040, 0x004000, 0x004040,
            0x400000, 0x400040, 0x404000, 0xC0C0C0,
            0x404040, 0x0000FF, 0x00FF00, 0x00FFFF,
            0xFF0000, 0xFF00FF, 0xFFFF00, 0xFFFFFF
        )
        for (i in 0 until 16) {
            palette[i] = defaultPalette[i]
        }
    }
}
