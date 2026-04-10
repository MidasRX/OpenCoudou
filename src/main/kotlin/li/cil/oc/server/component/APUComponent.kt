package li.cil.oc.server.component

import java.util.UUID

/**
 * APU (Accelerated Processing Unit) - Combined CPU and GPU Component.
 * 
 * The APU combines the functionality of both a CPU and a GPU in a single
 * component slot, saving space in compact builds like drones or tablets.
 * 
 * While slightly less powerful than dedicated CPU+GPU combinations,
 * the APU provides excellent value for space-constrained builds.
 * 
 * Tier-based capabilities:
 * - Tier 1: 80x25 resolution, 8 colors, 6 components
 * - Tier 2: 160x50 resolution, 256 colors, 8 components
 * - Creative (Tier 3): Unlimited resolution, 16M colors, unlimited components
 */
class APUComponent(
    private val tier: Int = 1
) : AbstractComponent("gpu") {
    
    private var energy = 5000.0
    
    // GPU State
    private var boundScreen: UUID? = null
    private var width = 80
    private var height = 25
    private var foreground = 0xFFFFFF
    private var background = 0x000000
    private var buffer = Array(height) { CharArray(width) { ' ' } }
    private var fgColors = Array(height) { IntArray(width) { foreground } }
    private var bgColors = Array(height) { IntArray(width) { background } }
    
    // CPU State
    private var cpuSpeed = 1.0
    private var maxComponents = 6
    
    init {
        when (tier) {
            1 -> {
                width = 80
                height = 25
                cpuSpeed = 1.0
                maxComponents = 6
            }
            2 -> {
                width = 160
                height = 50
                cpuSpeed = 1.5
                maxComponents = 8
            }
            else -> { // Tier 3+ = Creative
                width = 320
                height = 200
                cpuSpeed = 4.0
                maxComponents = Int.MAX_VALUE
            }
        }
        initBuffer()
        
        // GPU Methods
        registerMethod("bind", false, "function(address:string[, reset:boolean]):boolean -- Binds a screen to this GPU.") { args ->
            val addressStr = args.getOrNull(0) as? String ?: return@registerMethod arrayOf(null, "address required")
            val reset = args.getOrNull(1) as? Boolean ?: true
            
            boundScreen = try {
                UUID.fromString(addressStr)
            } catch (e: Exception) {
                return@registerMethod arrayOf(null, "invalid address")
            }
            
            if (reset) {
                initBuffer()
            }
            
            arrayOf(true)
        }
        
        registerMethod("getScreen", true, "function():string -- Gets the address of the bound screen.") { _ ->
            arrayOf(boundScreen?.toString())
        }
        
        registerMethod("getResolution", true, "function():number, number -- Gets current resolution.") { _ ->
            arrayOf(width, height)
        }
        
        registerMethod("setResolution", false, "function(width:number, height:number):boolean -- Sets resolution.") { args ->
            val newWidth = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "width required")
            val newHeight = (args.getOrNull(1) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "height required")
            
            val maxRes = getMaxResolution()
            if (newWidth > maxRes.first || newHeight > maxRes.second || newWidth < 1 || newHeight < 1) {
                return@registerMethod arrayOf(null, "unsupported resolution")
            }
            
            width = newWidth
            height = newHeight
            initBuffer()
            
            arrayOf(true)
        }
        
        registerMethod("maxResolution", true, "function():number, number -- Gets maximum resolution.") { _ ->
            val max = getMaxResolution()
            arrayOf(max.first, max.second)
        }
        
        registerMethod("getDepth", true, "function():number -- Gets color depth (1, 4, or 8).") { _ ->
            arrayOf(when (tier) {
                1 -> 4  // 8 colors
                2 -> 8  // 256 colors
                else -> 24 // 16M colors (creative)
            })
        }
        
        registerMethod("maxDepth", true, "function():number -- Gets maximum color depth.") { _ ->
            arrayOf(when (tier) {
                1 -> 4
                2 -> 8
                else -> 24
            })
        }
        
        registerMethod("setDepth", false, "function(depth:number):boolean -- Sets color depth.") { _ ->
            arrayOf(true)
        }
        
        registerMethod("setForeground", false, "function(color:number[, isPaletteIndex:boolean]):number -- Sets foreground color.") { args ->
            val color = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "color required")
            val old = foreground
            foreground = color and 0xFFFFFF
            arrayOf(old)
        }
        
        registerMethod("getForeground", true, "function():number -- Gets foreground color.") { _ ->
            arrayOf(foreground)
        }
        
        registerMethod("setBackground", false, "function(color:number[, isPaletteIndex:boolean]):number -- Sets background color.") { args ->
            val color = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "color required")
            val old = background
            background = color and 0xFFFFFF
            arrayOf(old)
        }
        
        registerMethod("getBackground", true, "function():number -- Gets background color.") { _ ->
            arrayOf(background)
        }
        
        registerMethod("set", false, "function(x:number, y:number, value:string[, vertical:boolean]):boolean -- Sets text at position.") { args ->
            val x = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "x required")
            val y = (args.getOrNull(1) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "y required")
            val value = args.getOrNull(2) as? String ?: return@registerMethod arrayOf(null, "value required")
            val vertical = args.getOrNull(3) as? Boolean ?: false
            
            if (y < 1 || y > height || x < 1) {
                return@registerMethod arrayOf(false)
            }
            
            if (vertical) {
                for ((i, char) in value.withIndex()) {
                    val cy = y + i - 1
                    if (cy in 0 until height && (x - 1) in 0 until width) {
                        buffer[cy][x - 1] = char
                        fgColors[cy][x - 1] = foreground
                        bgColors[cy][x - 1] = background
                    }
                }
            } else {
                for ((i, char) in value.withIndex()) {
                    val cx = x + i - 1
                    if (cx in 0 until width && (y - 1) in 0 until height) {
                        buffer[y - 1][cx] = char
                        fgColors[y - 1][cx] = foreground
                        bgColors[y - 1][cx] = background
                    }
                }
            }
            
            arrayOf(true)
        }
        
        registerMethod("get", true, "function(x:number, y:number):string, number, number -- Gets character and colors at position.") { args ->
            val x = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "x required")
            val y = (args.getOrNull(1) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "y required")
            
            if (x < 1 || x > width || y < 1 || y > height) {
                return@registerMethod arrayOf(" ", 0xFFFFFF, 0x000000)
            }
            
            val char = buffer[y - 1][x - 1]
            val fg = fgColors[y - 1][x - 1]
            val bg = bgColors[y - 1][x - 1]
            
            arrayOf(char.toString(), fg, bg)
        }
        
        registerMethod("fill", false, "function(x:number, y:number, width:number, height:number, char:string):boolean -- Fills a rectangle.") { args ->
            val startX = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "x required")
            val startY = (args.getOrNull(1) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "y required")
            val fillWidth = (args.getOrNull(2) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "width required")
            val fillHeight = (args.getOrNull(3) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "height required")
            val char = (args.getOrNull(4) as? String)?.firstOrNull() ?: ' '
            
            for (dy in 0 until fillHeight) {
                for (dx in 0 until fillWidth) {
                    val rx = startX + dx - 1
                    val ry = startY + dy - 1
                    if (rx in 0 until this@APUComponent.width && ry in 0 until this@APUComponent.height) {
                        buffer[ry][rx] = char
                        fgColors[ry][rx] = foreground
                        bgColors[ry][rx] = background
                    }
                }
            }
            
            arrayOf(true)
        }
        
        registerMethod("copy", false, "function(x:number, y:number, width:number, height:number, tx:number, ty:number):boolean -- Copies a region.") { args ->
            val x = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "x required")
            val y = (args.getOrNull(1) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "y required")
            val copyWidth = (args.getOrNull(2) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "width required")
            val copyHeight = (args.getOrNull(3) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "height required")
            val tx = (args.getOrNull(4) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "tx required")
            val ty = (args.getOrNull(5) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "ty required")
            
            // Copy to temp buffer first
            val tempBuffer = Array(copyHeight) { CharArray(copyWidth) }
            val tempFg = Array(copyHeight) { IntArray(copyWidth) }
            val tempBg = Array(copyHeight) { IntArray(copyWidth) }
            
            for (dy in 0 until copyHeight) {
                for (dx in 0 until copyWidth) {
                    val sx = x + dx - 1
                    val sy = y + dy - 1
                    if (sx in 0 until this@APUComponent.width && sy in 0 until this@APUComponent.height) {
                        tempBuffer[dy][dx] = buffer[sy][sx]
                        tempFg[dy][dx] = fgColors[sy][sx]
                        tempBg[dy][dx] = bgColors[sy][sx]
                    }
                }
            }
            
            // Copy from temp buffer to destination
            for (dy in 0 until copyHeight) {
                for (dx in 0 until copyWidth) {
                    val destX = x + tx + dx - 1
                    val destY = y + ty + dy - 1
                    if (destX in 0 until this@APUComponent.width && destY in 0 until this@APUComponent.height) {
                        buffer[destY][destX] = tempBuffer[dy][dx]
                        fgColors[destY][destX] = tempFg[dy][dx]
                        bgColors[destY][destX] = tempBg[dy][dx]
                    }
                }
            }
            
            arrayOf(true)
        }
        
        // CPU Methods
        registerMethod("getArchitecture", true, "function():string -- Gets the architecture name.") { _ ->
            arrayOf("Lua 5.3")
        }
        
        registerMethod("getSpeed", true, "function():number -- Gets the CPU speed multiplier.") { _ ->
            arrayOf(cpuSpeed)
        }
        
        registerMethod("getMaxComponents", true, "function():number -- Gets maximum supported components.") { _ ->
            arrayOf(maxComponents)
        }
        
        // APU-specific Methods
        registerMethod("isAPU", true, "function():boolean -- Returns true if this is an APU.") { _ ->
            arrayOf(true)
        }
        
        registerMethod("getTier", true, "function():number -- Gets the APU tier.") { _ ->
            arrayOf(tier)
        }
    }
    
    private fun initBuffer() {
        buffer = Array(height) { CharArray(width) { ' ' } }
        fgColors = Array(height) { IntArray(width) { foreground } }
        bgColors = Array(height) { IntArray(width) { background } }
    }
    
    private fun getMaxResolution(): Pair<Int, Int> = when (tier) {
        1 -> Pair(80, 25)
        2 -> Pair(160, 50)
        else -> Pair(320, 200) // Creative
    }
    
    fun isCreative(): Boolean = tier >= 3
}
