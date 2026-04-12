package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * Leash upgrade - allows robots to leash and lead animals.
 */
class LeashUpgradeComponent : AbstractComponent("leash") {
    
    companion object {
        const val LEASH_RANGE = 8.0
        const val MAX_LEASHED = 4
    }
    
    private var world: Level? = null
    private var position: BlockPos = BlockPos.ZERO
    
    private val leashedEntities = mutableListOf<Int>() // Entity IDs
    
    init {
        registerMethod("leash", false, "leash([side:string]):boolean,string -- Leash nearest animal") { args ->
            val level = world ?: return@registerMethod arrayOf(false, "no world")
            
            if (leashedEntities.size >= MAX_LEASHED) {
                return@registerMethod arrayOf(false, "too many leashed")
            }
            
            val centerX = position.x + 0.5
            val centerY = position.y + 0.5
            val centerZ = position.z + 0.5
            
            val box = AABB(
                centerX - LEASH_RANGE, centerY - LEASH_RANGE, centerZ - LEASH_RANGE,
                centerX + LEASH_RANGE, centerY + LEASH_RANGE, centerZ + LEASH_RANGE
            )
            
            val animals = level.getEntitiesOfClass(Animal::class.java, box)
            
            for (animal in animals) {
                if (animal.id !in leashedEntities && !animal.isLeashed) {
                    leashedEntities.add(animal.id)
                    return@registerMethod arrayOf(true, animal.type.descriptionId)
                }
            }
            
            arrayOf(false, "no animal found")
        }
        
        registerMethod("unleash", false, "unleash([index:number]):boolean -- Unleash an animal") { args ->
            val index = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
            
            if (index in leashedEntities.indices) {
                leashedEntities.removeAt(index)
                return@registerMethod arrayOf(true)
            }
            
            arrayOf(false)
        }
        
        registerMethod("unleashAll", false, "unleashAll():number -- Unleash all animals") { _ ->
            val count = leashedEntities.size
            leashedEntities.clear()
            arrayOf(count)
        }
        
        registerMethod("getLeashedCount", true, "getLeashedCount():number -- Get number of leashed animals") { _ ->
            arrayOf(leashedEntities.size)
        }
        
        registerMethod("getMaxLeashed", true, "getMaxLeashed():number -- Get max leashable animals") { _ ->
            arrayOf(MAX_LEASHED)
        }
        
        registerMethod("getLeashedEntities", true, "getLeashedEntities():table -- Get info about leashed animals") { _ ->
            val level = world ?: return@registerMethod arrayOf(emptyList<Any>())
            
            val result = mutableListOf<Map<String, Any?>>()
            for (entityId in leashedEntities) {
                val entity = level.getEntity(entityId)
                if (entity != null) {
                    result.add(mapOf(
                        "name" to entity.type.descriptionId,
                        "x" to entity.x,
                        "y" to entity.y,
                        "z" to entity.z
                    ))
                }
            }
            arrayOf(result)
        }
    }
    
    fun setWorld(level: Level?, pos: BlockPos) {
        this.world = level
        this.position = pos
    }
    
    /**
     * Called each tick to pull leashed entities toward the robot.
     */
    fun tick() {
        val level = world ?: return
        
        val centerX = position.x + 0.5
        val centerY = position.y + 0.5
        val centerZ = position.z + 0.5
        
        val iterator = leashedEntities.iterator()
        while (iterator.hasNext()) {
            val entityId = iterator.next()
            val entity = level.getEntity(entityId)
            
            if (entity == null || !entity.isAlive) {
                iterator.remove()
                continue
            }
            
            val distance = entity.distanceToSqr(centerX, centerY, centerZ)
            if (distance > LEASH_RANGE * LEASH_RANGE * 4) {
                // Too far, break leash
                iterator.remove()
                continue
            }
            
            if (distance > 4.0) {
                // Pull entity toward robot
                val direction = Vec3(centerX - entity.x, centerY - entity.y, centerZ - entity.z).normalize().scale(0.1)
                entity.setDeltaMovement(entity.deltaMovement.add(direction))
            }
        }
    }
}
