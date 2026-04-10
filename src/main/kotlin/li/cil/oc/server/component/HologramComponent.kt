package li.cil.oc.server.component

/**
 * Hologram projector component.
 * Projects a 3D holographic display that can be controlled from Lua.
 * 
 * Tier 1: 48x32x48 voxels, 2 colors
 * Tier 2: 48x32x48 voxels, 3 colors, rotation, scale
 */
class HologramComponent(
    val tier: Int = 1
) : AbstractComponent("hologram") {
    
    companion object {
        const val WIDTH = 48
        const val HEIGHT = 32
        const val DEPTH = 48
        const val MAX_COLORS = 3
    }
    
    // Hologram voxel data - each byte stores color index (0-3)
    private val voxels = ByteArray(WIDTH * HEIGHT * DEPTH)
    
    // Palette colors (ARGB)
    private val palette = intArrayOf(
        0x66FF00, // Default green
        0x0066FF, // Default blue
        0xFF0066  // Default red (tier 2+)
    )
    
    // Transformation
    private var scale = 1.0
    private var rotationAngle = 0.0 // Degrees
    private var rotationSpeedX = 0.0
    private var rotationSpeedY = 0.0
    private var rotationSpeedZ = 0.0
    private var offsetX = 0.0
    private var offsetY = 0.0
    private var offsetZ = 0.0
    
    init {
        registerMethod("clear", false, "clear() -- Clear the hologram") { _ ->
            voxels.fill(0)
            arrayOf(true)
        }
        
        registerMethod("get", true, "get(x:number,y:number,z:number):number -- Get voxel at position") { args ->
            val x = (args.getOrNull(0) as? Number)?.toInt() ?: 1
            val y = (args.getOrNull(1) as? Number)?.toInt() ?: 1
            val z = (args.getOrNull(2) as? Number)?.toInt() ?: 1
            
            if (x < 1 || x > WIDTH || y < 1 || y > HEIGHT || z < 1 || z > DEPTH) {
                return@registerMethod arrayOf(0)
            }
            
            val index = ((x - 1) + (y - 1) * WIDTH + (z - 1) * WIDTH * HEIGHT)
            arrayOf(voxels[index].toInt() and 0xFF)
        }
        
        registerMethod("set", false, "set(x:number,y:number,z:number,value:number) -- Set voxel at position") { args ->
            val x = (args.getOrNull(0) as? Number)?.toInt() ?: 1
            val y = (args.getOrNull(1) as? Number)?.toInt() ?: 1
            val z = (args.getOrNull(2) as? Number)?.toInt() ?: 1
            val value = (args.getOrNull(3) as? Number)?.toInt() ?: 0
            
            if (x < 1 || x > WIDTH || y < 1 || y > HEIGHT || z < 1 || z > DEPTH) {
                return@registerMethod arrayOf(false)
            }
            
            val maxColors = if (tier >= 2) MAX_COLORS else 2
            val clampedValue = value.coerceIn(0, maxColors)
            
            val index = ((x - 1) + (y - 1) * WIDTH + (z - 1) * WIDTH * HEIGHT)
            voxels[index] = clampedValue.toByte()
            arrayOf(true)
        }
        
        registerMethod("fill", false, "fill(x:number,y:number,z:number,w:number,h:number,d:number,value:number) -- Fill a region") { args ->
            val x = (args.getOrNull(0) as? Number)?.toInt() ?: 1
            val y = (args.getOrNull(1) as? Number)?.toInt() ?: 1
            val z = (args.getOrNull(2) as? Number)?.toInt() ?: 1
            val w = (args.getOrNull(3) as? Number)?.toInt() ?: 1
            val h = (args.getOrNull(4) as? Number)?.toInt() ?: 1
            val d = (args.getOrNull(5) as? Number)?.toInt() ?: 1
            val value = (args.getOrNull(6) as? Number)?.toInt() ?: 0
            
            val maxColors = if (tier >= 2) MAX_COLORS else 2
            val clampedValue = value.coerceIn(0, maxColors).toByte()
            
            for (dz in 0 until d) {
                for (dy in 0 until h) {
                    for (dx in 0 until w) {
                        val px = x + dx
                        val py = y + dy
                        val pz = z + dz
                        
                        if (px >= 1 && px <= WIDTH && py >= 1 && py <= HEIGHT && pz >= 1 && pz <= DEPTH) {
                            val index = ((px - 1) + (py - 1) * WIDTH + (pz - 1) * WIDTH * HEIGHT)
                            voxels[index] = clampedValue
                        }
                    }
                }
            }
            arrayOf(true)
        }
        
        registerMethod("copy", false, "copy(x:number,y:number,z:number,w:number,h:number,d:number,tx:number,ty:number,tz:number) -- Copy a region") { args ->
            val x = (args.getOrNull(0) as? Number)?.toInt() ?: 1
            val y = (args.getOrNull(1) as? Number)?.toInt() ?: 1
            val z = (args.getOrNull(2) as? Number)?.toInt() ?: 1
            val w = (args.getOrNull(3) as? Number)?.toInt() ?: 1
            val h = (args.getOrNull(4) as? Number)?.toInt() ?: 1
            val d = (args.getOrNull(5) as? Number)?.toInt() ?: 1
            val tx = (args.getOrNull(6) as? Number)?.toInt() ?: 0
            val ty = (args.getOrNull(7) as? Number)?.toInt() ?: 0
            val tz = (args.getOrNull(8) as? Number)?.toInt() ?: 0
            
            // Read source region
            val temp = Array(w) { Array(h) { ByteArray(d) } }
            for (dz in 0 until d) {
                for (dy in 0 until h) {
                    for (dx in 0 until w) {
                        val px = x + dx
                        val py = y + dy
                        val pz = z + dz
                        if (px >= 1 && px <= WIDTH && py >= 1 && py <= HEIGHT && pz >= 1 && pz <= DEPTH) {
                            val index = ((px - 1) + (py - 1) * WIDTH + (pz - 1) * WIDTH * HEIGHT)
                            temp[dx][dy][dz] = voxels[index]
                        }
                    }
                }
            }
            
            // Write to target region
            for (dz in 0 until d) {
                for (dy in 0 until h) {
                    for (dx in 0 until w) {
                        val px = x + tx + dx
                        val py = y + ty + dy
                        val pz = z + tz + dz
                        if (px >= 1 && px <= WIDTH && py >= 1 && py <= HEIGHT && pz >= 1 && pz <= DEPTH) {
                            val index = ((px - 1) + (py - 1) * WIDTH + (pz - 1) * WIDTH * HEIGHT)
                            voxels[index] = temp[dx][dy][dz]
                        }
                    }
                }
            }
            arrayOf(true)
        }
        
        registerMethod("maxDepth", true, "maxDepth():number -- Get max depth (Z axis)") { _ ->
            arrayOf(DEPTH)
        }
        
        registerMethod("getPaletteColor", true, "getPaletteColor(index:number):number -- Get palette color") { args ->
            val index = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
            if (index < 0 || index >= palette.size) {
                return@registerMethod arrayOf(0)
            }
            arrayOf(palette[index])
        }
        
        registerMethod("setPaletteColor", false, "setPaletteColor(index:number,color:number):number -- Set palette color") { args ->
            val index = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
            val color = (args.getOrNull(1) as? Number)?.toInt() ?: 0
            
            if (index < 0 || index >= palette.size) {
                return@registerMethod arrayOf(palette.getOrElse(0) { 0 })
            }
            
            val old = palette[index]
            palette[index] = color and 0xFFFFFF
            arrayOf(old)
        }
        
        // Tier 2 methods
        registerMethod("getScale", true, "getScale():number -- Get current scale (tier 2)") { _ ->
            if (tier < 2) return@registerMethod arrayOf(1.0)
            arrayOf(scale)
        }
        
        registerMethod("setScale", false, "setScale(scale:number):number -- Set scale (tier 2, 0.33 to 4)") { args ->
            if (tier < 2) return@registerMethod arrayOf(null, "requires tier 2")
            
            val newScale = (args.getOrNull(0) as? Number)?.toDouble() ?: 1.0
            val old = scale
            scale = newScale.coerceIn(0.33, 4.0)
            arrayOf(old)
        }
        
        registerMethod("getRotation", true, "getRotation():number,number,number,number -- Get rotation angle and speed") { _ ->
            arrayOf(rotationAngle, rotationSpeedX, rotationSpeedY, rotationSpeedZ)
        }
        
        registerMethod("setRotation", false, "setRotation(angle:number,sx:number,sy:number,sz:number) -- Set rotation (tier 2)") { args ->
            if (tier < 2) return@registerMethod arrayOf(null, "requires tier 2")
            
            rotationAngle = (args.getOrNull(0) as? Number)?.toDouble() ?: 0.0
            rotationSpeedX = (args.getOrNull(1) as? Number)?.toDouble() ?: 0.0
            rotationSpeedY = (args.getOrNull(2) as? Number)?.toDouble() ?: 0.0
            rotationSpeedZ = (args.getOrNull(3) as? Number)?.toDouble() ?: 0.0
            arrayOf(true)
        }
        
        registerMethod("getTranslation", true, "getTranslation():number,number,number -- Get translation offset") { _ ->
            arrayOf(offsetX, offsetY, offsetZ)
        }
        
        registerMethod("setTranslation", false, "setTranslation(x:number,y:number,z:number) -- Set translation (tier 2)") { args ->
            if (tier < 2) return@registerMethod arrayOf(null, "requires tier 2")
            
            offsetX = (args.getOrNull(0) as? Number)?.toDouble()?.coerceIn(-1.0, 1.0) ?: 0.0
            offsetY = (args.getOrNull(1) as? Number)?.toDouble()?.coerceIn(-1.0, 1.0) ?: 0.0
            offsetZ = (args.getOrNull(2) as? Number)?.toDouble()?.coerceIn(-1.0, 1.0) ?: 0.0
            arrayOf(true)
        }
        
        registerMethod("getRotationSpeed", true, "getRotationSpeed():number,number,number -- Get rotation speed (tier 2)") { _ ->
            if (tier < 2) return@registerMethod arrayOf(0.0, 0.0, 0.0)
            arrayOf(rotationSpeedX, rotationSpeedY, rotationSpeedZ)
        }
        
        registerMethod("setRotationSpeed", false, "setRotationSpeed(x:number,y:number,z:number) -- Set rotation speed (tier 2)") { args ->
            if (tier < 2) return@registerMethod arrayOf(null, "requires tier 2")
            
            rotationSpeedX = (args.getOrNull(0) as? Number)?.toDouble()?.coerceIn(-360.0, 360.0) ?: 0.0
            rotationSpeedY = (args.getOrNull(1) as? Number)?.toDouble()?.coerceIn(-360.0, 360.0) ?: 0.0
            rotationSpeedZ = (args.getOrNull(2) as? Number)?.toDouble()?.coerceIn(-360.0, 360.0) ?: 0.0
            arrayOf(true)
        }
    }
    
    // Methods to get data for rendering
    fun getVoxelData(): ByteArray = voxels.copyOf()
    fun getPalette(): IntArray = palette.copyOf()
    fun getScale(): Double = scale
    fun getOffset(): Triple<Double, Double, Double> = Triple(offsetX, offsetY, offsetZ)
    fun getRotation(): Pair<Double, Triple<Double, Double, Double>> = 
        Pair(rotationAngle, Triple(rotationSpeedX, rotationSpeedY, rotationSpeedZ))
}
