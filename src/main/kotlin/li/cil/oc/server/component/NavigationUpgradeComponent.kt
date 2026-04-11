package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import kotlin.math.sqrt

/**
 * Navigation upgrade component - provides GPS-like positioning and waypoint detection.
 * Can detect waypoints within range and provide relative coordinates.
 */
class NavigationUpgradeComponent(
    val tier: Int = 1
) : AbstractComponent("navigation") {
    
    companion object {
        // Range per tier (in blocks)
        val RANGES = intArrayOf(64, 128, 256)
        
        // Global waypoint registry
        private val waypoints = mutableMapOf<BlockPos, WaypointInfo>()
        
        fun registerWaypoint(pos: BlockPos, label: String, redstoneStrength: Int) {
            waypoints[pos] = WaypointInfo(pos, label, redstoneStrength)
        }
        
        fun unregisterWaypoint(pos: BlockPos) {
            waypoints.remove(pos)
        }
        
        fun getWaypoints(): Collection<WaypointInfo> = waypoints.values
    }
    
    // Position reference for navigation
    var navigationPos: BlockPos = BlockPos.ZERO
    var level: Level? = null
    
    // Map center (can be configured via filled map item)
    private var mapCenterX: Int = 0
    private var mapCenterZ: Int = 0
    
    init {
        registerMethod("getPosition", true, "getPosition():number,number,number -- Get current position relative to map center") { _ ->
            arrayOf(
                navigationPos.x - mapCenterX,
                navigationPos.y,
                navigationPos.z - mapCenterZ
            )
        }
        
        registerMethod("getFacing", true, "getFacing():number -- Get current facing direction (0-3 for N/E/S/W)") { _ ->
            // Would need entity reference for actual facing
            // Return 0 (North) as default
            arrayOf(0)
        }
        
        registerMethod("getRange", true, "getRange():number -- Get map range") { _ ->
            arrayOf(getRange())
        }
        
        registerMethod("findWaypoints", true, "findWaypoints(range:number):table -- Find waypoints within range") { args ->
            val range = (args.getOrNull(0) as? Number)?.toDouble() ?: getRange().toDouble()
            val maxRange = minOf(range, getRange().toDouble())
            
            val results = mutableListOf<Map<String, Any>>()
            
            for (waypoint in getWaypoints()) {
                val dx = waypoint.pos.x - navigationPos.x
                val dy = waypoint.pos.y - navigationPos.y
                val dz = waypoint.pos.z - navigationPos.z
                val distance = sqrt((dx * dx + dy * dy + dz * dz).toDouble())
                
                if (distance <= maxRange) {
                    results.add(mapOf(
                        "position" to listOf(dx, dy, dz),
                        "label" to waypoint.label,
                        "redstone" to waypoint.redstoneStrength,
                        "distance" to distance
                    ))
                }
            }
            
            // Sort by distance
            results.sortBy { it["distance"] as Double }
            
            arrayOf(results)
        }
        
        registerMethod("setMapCenter", false, "setMapCenter(x:number,z:number) -- Set map center (requires filled map)") { args ->
            mapCenterX = (args.getOrNull(0) as? Number)?.toInt() ?: 0
            mapCenterZ = (args.getOrNull(1) as? Number)?.toInt() ?: 0
            arrayOf(true)
        }
        
        registerMethod("getMapCenter", true, "getMapCenter():number,number -- Get map center coordinates") { _ ->
            arrayOf(mapCenterX, mapCenterZ)
        }
        
        registerMethod("getHeading", true, "getHeading():number -- Get heading in degrees (0-360)") { _ ->
            // Would need entity reference
            arrayOf(0)
        }
        
        registerMethod("getLevel", true, "getLevel():number -- Get Y level") { _ ->
            arrayOf(navigationPos.y)
        }
    }
    
    private fun getRange(): Int = RANGES.getOrElse(tier - 1) { 64 }
    
    /**
     * Update position reference.
     */
    fun setPosition(pos: BlockPos, world: Level) {
        navigationPos = pos
        level = world
    }
    
    /**
     * Waypoint info data class.
     */
    data class WaypointInfo(
        val pos: BlockPos,
        val label: String,
        val redstoneStrength: Int
    )
}
