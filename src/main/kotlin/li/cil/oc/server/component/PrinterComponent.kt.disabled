package li.cil.oc.server.component

/**
 * Printer component - 3D print custom shapes using chamelium and ink.
 * Creates decorative blocks with custom textures and shapes.
 */
class PrinterComponent : AbstractComponent("printer") {
    
    companion object {
        const val MAX_SHAPES = 24
        const val MAX_LABEL_LENGTH = 48
    }
    
    // Current print job configuration
    private var label: String = ""
    private var tooltip: String = ""
    private var lightLevel: Int = 0
    private var redstoneLevel: Int = 0
    private var buttonMode: Boolean = false
    private var collidable: Boolean = true
    
    // Print states (one per shape slot)
    private val shapes = mutableListOf<PrintShape>()
    
    // Materials
    private var inkLevel: Int = 0
    private var chameliumLevel: Int = 0
    
    data class PrintShape(
        val minX: Int, val minY: Int, val minZ: Int,
        val maxX: Int, val maxY: Int, val maxZ: Int,
        val texture: String,
        val tint: Int,
        val state: Boolean // true = on state, false = off state
    )
    
    init {
        registerMethod("reset", false, "reset() -- Clear the current print configuration") { _ ->
            label = ""
            tooltip = ""
            lightLevel = 0
            redstoneLevel = 0
            buttonMode = false
            collidable = true
            shapes.clear()
            arrayOf(true)
        }
        
        registerMethod("setLabel", false, "setLabel(label:string) -- Set print label") { args ->
            label = args.getOrNull(0)?.toString()?.take(MAX_LABEL_LENGTH) ?: ""
            arrayOf(true)
        }
        
        registerMethod("getLabel", true, "getLabel():string -- Get print label") { _ ->
            arrayOf(label)
        }
        
        registerMethod("setTooltip", false, "setTooltip(tooltip:string) -- Set tooltip") { args ->
            tooltip = args.getOrNull(0)?.toString()?.take(128) ?: ""
            arrayOf(true)
        }
        
        registerMethod("getTooltip", true, "getTooltip():string -- Get tooltip") { _ ->
            arrayOf(tooltip)
        }
        
        registerMethod("setLightLevel", false, "setLightLevel(level:number) -- Set light level (0-15)") { args ->
            lightLevel = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 15) ?: 0
            arrayOf(true)
        }
        
        registerMethod("getLightLevel", true, "getLightLevel():number -- Get light level") { _ ->
            arrayOf(lightLevel)
        }
        
        registerMethod("setRedstoneEmitter", false, "setRedstoneEmitter(level:number) -- Set redstone output (0-15)") { args ->
            redstoneLevel = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 15) ?: 0
            arrayOf(true)
        }
        
        registerMethod("isRedstoneEmitter", true, "isRedstoneEmitter():boolean,number -- Get redstone output state") { _ ->
            arrayOf(redstoneLevel > 0, redstoneLevel)
        }
        
        registerMethod("setButtonMode", false, "setButtonMode(enabled:boolean) -- Set button mode") { args ->
            buttonMode = args.getOrNull(0) as? Boolean ?: false
            arrayOf(true)
        }
        
        registerMethod("isButtonMode", true, "isButtonMode():boolean -- Get button mode") { _ ->
            arrayOf(buttonMode)
        }
        
        registerMethod("setCollidable", false, "setCollidable(collidable:boolean[,state:boolean]) -- Set collision") { args ->
            collidable = args.getOrNull(0) as? Boolean ?: true
            arrayOf(true)
        }
        
        registerMethod("isCollidable", true, "isCollidable():boolean,boolean -- Get collision state") { _ ->
            arrayOf(collidable, collidable)
        }
        
        registerMethod("addShape", false, "addShape(minX:number,minY:number,minZ:number,maxX:number,maxY:number,maxZ:number,texture:string[,state:boolean,tint:number]) -- Add shape") { args ->
            if (shapes.size >= MAX_SHAPES) {
                return@registerMethod arrayOf(false, "too many shapes")
            }
            
            val minX = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 16) ?: 0
            val minY = (args.getOrNull(1) as? Number)?.toInt()?.coerceIn(0, 16) ?: 0
            val minZ = (args.getOrNull(2) as? Number)?.toInt()?.coerceIn(0, 16) ?: 0
            val maxX = (args.getOrNull(3) as? Number)?.toInt()?.coerceIn(0, 16) ?: 16
            val maxY = (args.getOrNull(4) as? Number)?.toInt()?.coerceIn(0, 16) ?: 16
            val maxZ = (args.getOrNull(5) as? Number)?.toInt()?.coerceIn(0, 16) ?: 16
            val texture = args.getOrNull(6)?.toString() ?: "minecraft:block/stone"
            val state = args.getOrNull(7) as? Boolean ?: false
            val tint = (args.getOrNull(8) as? Number)?.toInt() ?: 0xFFFFFF
            
            shapes.add(PrintShape(minX, minY, minZ, maxX, maxY, maxZ, texture, tint, state))
            arrayOf(true)
        }
        
        registerMethod("getShapeCount", true, "getShapeCount():number -- Get number of shapes") { _ ->
            arrayOf(shapes.size)
        }
        
        registerMethod("getMaxShapeCount", true, "getMaxShapeCount():number -- Get max shapes allowed") { _ ->
            arrayOf(MAX_SHAPES)
        }
        
        registerMethod("status", true, "status():string,number -- Get printer status") { _ ->
            // Status: idle, busy, or printing progress
            arrayOf("idle", 100)
        }
        
        registerMethod("commit", false, "commit(count:number):boolean -- Start printing") { args ->
            val count = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(1, 64) ?: 1
            
            // Check materials
            val chameliumNeeded = shapes.sumOf { s: PrintShape ->
                (s.maxX - s.minX) * (s.maxY - s.minY) * (s.maxZ - s.minZ)
            } * count
            val inkNeeded = shapes.size * count
            
            if (chameliumLevel < chameliumNeeded) {
                return@registerMethod arrayOf(false, "not enough chamelium")
            }
            if (inkLevel < inkNeeded) {
                return@registerMethod arrayOf(false, "not enough ink")
            }
            
            // Would produce printed blocks here
            arrayOf(true)
        }
        
        // Material levels
        registerMethod("getInkLevel", true, "getInkLevel():number -- Get ink level") { _ ->
            arrayOf(inkLevel)
        }
        
        registerMethod("getChameliumLevel", true, "getChameliumLevel():number -- Get chamelium level") { _ ->
            arrayOf(chameliumLevel)
        }
    }
    
    fun addInk(amount: Int): Int {
        val toAdd = amount.coerceIn(0, 100000 - inkLevel)
        inkLevel += toAdd
        return toAdd
    }
    
    fun addChamelium(amount: Int): Int {
        val toAdd = amount.coerceIn(0, 256000 - chameliumLevel)
        chameliumLevel += toAdd
        return toAdd
    }
}
