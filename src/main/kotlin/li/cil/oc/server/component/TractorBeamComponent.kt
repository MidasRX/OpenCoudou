package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * Tractor beam upgrade - allows robots/drones to pull items from a distance.
 */
class TractorBeamComponent : AbstractComponent("tractor_beam") {
    
    companion object {
        const val RANGE = 8.0
        const val ENERGY_COST_PER_ITEM = 5.0
    }
    
    private var world: Level? = null
    private var position: BlockPos = BlockPos.ZERO
    
    init {
        registerMethod("suck", false, "suck([range:number]):boolean,number -- Pull nearby items") { args ->
            val range = (args.getOrNull(0) as? Number)?.toDouble()?.coerceIn(1.0, RANGE) ?: RANGE
            val result = pullItems(range)
            arrayOf(result.first, result.second)
        }
        
        registerMethod("getRange", true, "getRange():number -- Get maximum range") { _ ->
            arrayOf(RANGE)
        }
    }
    
    fun setWorld(level: Level?, pos: BlockPos) {
        this.world = level
        this.position = pos
    }
    
    /**
     * Pull items towards the robot.
     * @return (success, count of items pulled)
     */
    private fun pullItems(range: Double): Pair<Boolean, Int> {
        val level = world ?: return Pair(false, 0)
        
        val centerX = position.x + 0.5
        val centerY = position.y + 0.5
        val centerZ = position.z + 0.5
        
        val box = AABB(
            centerX - range, centerY - range, centerZ - range,
            centerX + range, centerY + range, centerZ + range
        )
        
        val items = level.getEntitiesOfClass(ItemEntity::class.java, box)
        
        if (items.isEmpty()) {
            return Pair(false, 0)
        }
        
        var pulled = 0
        for (item in items) {
            // Move item towards robot
            val targetVec = Vec3(centerX, centerY, centerZ)
            val itemVec = item.position()
            val direction = targetVec.subtract(itemVec).normalize().scale(0.5)
            
            item.setDeltaMovement(direction)
            pulled++
        }
        
        return Pair(pulled > 0, pulled)
    }
    
    /**
     * Pull a specific entity towards the robot.
     */
    fun pullEntity(entity: Entity): Boolean {
        val level = world ?: return false
        
        val centerX = position.x + 0.5
        val centerY = position.y + 0.5
        val centerZ = position.z + 0.5
        
        val targetVec = Vec3(centerX, centerY, centerZ)
        val entityVec = entity.position()
        
        val distance = targetVec.distanceTo(entityVec)
        if (distance > RANGE) return false
        
        val direction = targetVec.subtract(entityVec).normalize().scale(0.3)
        entity.setDeltaMovement(entity.deltaMovement.add(direction))
        
        return true
    }
}
