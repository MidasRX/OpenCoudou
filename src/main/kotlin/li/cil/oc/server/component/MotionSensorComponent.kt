package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB

/**
 * Motion sensor component - detects entity movement in range.
 * Generates events when entities enter or leave the detection area.
 */
class MotionSensorComponent : AbstractComponent("motion_sensor") {
    
    companion object {
        const val DEFAULT_RANGE = 8.0
        const val MAX_RANGE = 32.0
        const val MIN_RANGE = 1.0
    }
    
    var sensorPos: BlockPos = BlockPos.ZERO
    var level: Level? = null
    
    private var sensitivity = DEFAULT_RANGE
    
    init {
        registerMethod("getSensitivity", true, "getSensitivity():number -- Get current detection range") { _ ->
            arrayOf(sensitivity)
        }
        
        registerMethod("setSensitivity", false, "setSensitivity(range:number):number -- Set detection range") { args ->
            val newSensitivity = (args.getOrNull(0) as? Number)?.toDouble() 
                ?: return@registerMethod arrayOf(sensitivity)
            
            val old = sensitivity
            sensitivity = newSensitivity.coerceIn(MIN_RANGE, MAX_RANGE)
            arrayOf(old)
        }
        
        registerMethod("getEntities", true, "getEntities():table -- Get all entities in range") { _ ->
            val world = level ?: return@registerMethod arrayOf(listOf<Map<String, Any?>>())
            
            val box = AABB.ofSize(
                sensorPos.center,
                sensitivity * 2,
                sensitivity * 2,
                sensitivity * 2
            )
            
            val entities = world.getEntities(null as Entity?, box) { it is LivingEntity }
            
            val result = entities.map { entity ->
                mapOf(
                    "name" to entity.name.string,
                    "x" to (entity.x - sensorPos.x),
                    "y" to (entity.y - sensorPos.y),
                    "z" to (entity.z - sensorPos.z),
                    "type" to entity.type.descriptionId,
                    "isPlayer" to (entity is Player)
                )
            }
            
            arrayOf(result)
        }
        
        registerMethod("getPlayers", true, "getPlayers():table -- Get all players in range") { _ ->
            val world = level ?: return@registerMethod arrayOf(listOf<Map<String, Any?>>())
            
            val box = AABB.ofSize(
                sensorPos.center,
                sensitivity * 2,
                sensitivity * 2,
                sensitivity * 2
            )
            
            val players = world.getEntitiesOfClass(Player::class.java, box)
            
            val result = players.map { player ->
                mapOf(
                    "name" to player.name.string,
                    "x" to (player.x - sensorPos.x),
                    "y" to (player.y - sensorPos.y),
                    "z" to (player.z - sensorPos.z),
                    "uuid" to player.uuid.toString()
                )
            }
            
            arrayOf(result)
        }
    }
    
    /**
     * Called by the block entity to check for motion and generate events.
     * Returns a list of entities that have moved significantly.
     */
    fun detectMotion(): List<Entity> {
        val world = level ?: return emptyList()
        
        val box = AABB.ofSize(
            sensorPos.center,
            sensitivity * 2,
            sensitivity * 2,
            sensitivity * 2
        )
        
        return world.getEntities(null as Entity?, box) { entity ->
            entity is LivingEntity && entity.deltaMovement.lengthSqr() > 0.001
        }
    }
}
