package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

/**
 * Geolyzer component - scans and analyzes blocks in the world.
 * Provides information about block hardness, type, and composition in an area.
 */
class GeolyzerComponent : AbstractComponent("geolyzer") {
    
    companion object {
        const val SCAN_RANGE = 32
        const val SCAN_COOLDOWN_MS = 100
    }
    
    // Position reference for scanning
    var geolyzerPos: BlockPos = BlockPos.ZERO
    var level: Level? = null
    
    private var lastScanTime = 0L
    
    init {
        registerMethod("scan", false, "scan(x:number,z:number[,y:number,w:number,d:number,h:number][,ignoreReplaceable:boolean]):table -- Scan area") { args ->
            val now = System.currentTimeMillis()
            if (now - lastScanTime < SCAN_COOLDOWN_MS) {
                return@registerMethod arrayOf(null, "cooldown")
            }
            lastScanTime = now
            
            val world = level ?: return@registerMethod arrayOf(null, "no world")
            
            // Parse arguments - OC syntax: scan(x, z) or scan(x, z, y, w, d, h)
            val offsetX = (args.getOrNull(0) as? Number)?.toInt() ?: 0
            val offsetZ = (args.getOrNull(1) as? Number)?.toInt() ?: 0
            val offsetY = (args.getOrNull(2) as? Number)?.toInt() ?: 0
            val width = (args.getOrNull(3) as? Number)?.toInt()?.coerceIn(1, 8) ?: 1
            val depth = (args.getOrNull(4) as? Number)?.toInt()?.coerceIn(1, 8) ?: 1
            val height = (args.getOrNull(5) as? Number)?.toInt()?.coerceIn(1, 64) ?: 64
            
            // Check range
            if (Math.abs(offsetX) > SCAN_RANGE || Math.abs(offsetY) > SCAN_RANGE || Math.abs(offsetZ) > SCAN_RANGE) {
                return@registerMethod arrayOf(null, "out of range")
            }
            
            val results = mutableListOf<Map<String, Any?>>()
            
            for (dy in 0 until height) {
                for (dz in 0 until depth) {
                    for (dx in 0 until width) {
                        val scanPos = geolyzerPos.offset(offsetX + dx, offsetY + dy, offsetZ + dz)
                        val state = world.getBlockState(scanPos)
                        val block = state.block
                        
                        val hardness = state.getDestroySpeed(world, scanPos)
                        val isAir = state.isAir
                        
                        // Create entry for this block
                        val entry = mapOf(
                            "name" to block.descriptionId,
                            "hardness" to hardness,
                            "air" to isAir
                        )
                        results.add(entry)
                    }
                }
            }
            
            arrayOf(results)
        }
        
        registerMethod("analyze", false, "analyze(side:number):table -- Analyze a block on one side") { args ->
            val now = System.currentTimeMillis()
            if (now - lastScanTime < SCAN_COOLDOWN_MS) {
                return@registerMethod arrayOf(null, "cooldown")
            }
            lastScanTime = now
            
            val world = level ?: return@registerMethod arrayOf(null, "no world")
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 1
            
            val direction = when (side) {
                0 -> net.minecraft.core.Direction.DOWN
                1 -> net.minecraft.core.Direction.UP
                2 -> net.minecraft.core.Direction.NORTH
                3 -> net.minecraft.core.Direction.SOUTH
                4 -> net.minecraft.core.Direction.WEST
                else -> net.minecraft.core.Direction.EAST
            }
            
            val targetPos = geolyzerPos.relative(direction)
            val state = world.getBlockState(targetPos)
            val block = state.block
            
            val result = mapOf(
                "name" to block.descriptionId,
                "hardness" to state.getDestroySpeed(world, targetPos),
                "air" to state.isAir,
                "liquid" to state.fluidState.isEmpty.not(),
                "metadata" to 0, // Legacy, always 0 in modern MC
                "color" to 0 // Map color not easily accessible in 1.21
            )
            
            arrayOf(result)
        }
        
        registerMethod("store", false, "store(side:number,dbAddress:string,dbSlot:number):boolean -- Store analysis in database (not implemented)") { _ ->
            arrayOf(false, "not implemented")
        }
        
        registerMethod("isSunVisible", true, "isSunVisible():boolean -- Check if sun is visible from geolyzer position") { _ ->
            val world = level ?: return@registerMethod arrayOf(false)
            val canSeeSky = world.canSeeSky(geolyzerPos.above())
            val isDay = world.isDay
            arrayOf(canSeeSky && isDay)
        }
        
        registerMethod("canSeeSky", true, "canSeeSky():boolean -- Check if geolyzer can see sky") { _ ->
            val world = level ?: return@registerMethod arrayOf(false)
            arrayOf(world.canSeeSky(geolyzerPos.above()))
        }
    }
}
