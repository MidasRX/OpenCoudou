package li.cil.oc.common.event

import li.cil.oc.OpenComputers
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent
import net.neoforged.neoforge.event.level.ExplosionEvent
import org.slf4j.LoggerFactory

/**
 * Handles robot and drone specific events.
 */
@EventBusSubscriber(modid = OpenComputers.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
object RobotEventHandler {
    
    private val logger = LoggerFactory.getLogger("OpenComputers")
    
    // Track active robots for collision avoidance
    private val activeRobots = mutableMapOf<BlockPos, RobotInfo>()
    
    data class RobotInfo(
        val level: Level,
        val pos: BlockPos,
        var isMoving: Boolean = false,
        var targetPos: BlockPos? = null
    )
    
    @SubscribeEvent
    @JvmStatic
    fun onExplosion(event: ExplosionEvent.Detonate) {
        val level = event.level
        val explosion = event.explosion
        val center = explosion.center()
        
        // Protect robots from explosions if they have the upgrade
        val affectedEntities = event.affectedEntities.toMutableList()
        val iterator = affectedEntities.iterator()
        
        while (iterator.hasNext()) {
            val entity = iterator.next()
            // Check if entity is a robot with explosion resistance upgrade
            if (isProtectedRobot(entity)) {
                iterator.remove()
            }
        }
    }
    
    @SubscribeEvent
    @JvmStatic
    fun onLivingDrops(event: LivingDropsEvent) {
        val entity = event.entity
        val level = entity.level()
        val pos = entity.blockPosition()
        
        // Check if there's a robot nearby with magnet upgrade
        val nearbyRobots = findRobotsWithMagnet(level, pos, 8.0)
        
        for (robot in nearbyRobots) {
            // Queue items for pickup by robot
            queueItemsForRobot(robot, event.drops)
        }
    }
    
    @SubscribeEvent
    @JvmStatic
    fun onEntityJoinLevel(event: EntityJoinLevelEvent) {
        val entity = event.entity
        
        // Track item entities for robots with magnet upgrade
        if (entity is ItemEntity) {
            val level = event.level
            val pos = entity.blockPosition()
            
            // Check for robots with magnet upgrade in range
            val nearbyRobots = findRobotsWithMagnet(level, pos, 8.0)
            for (robot in nearbyRobots) {
                // Add item to robot's magnet queue
                queueItemForMagnet(robot, entity)
            }
        }
    }
    
    private fun isProtectedRobot(entity: Entity): Boolean {
        // Check if entity is a robot with explosion resistance
        return false // TODO: implement when robot entity exists
    }
    
    private fun findRobotsWithMagnet(level: Level, center: BlockPos, range: Double): List<RobotInfo> {
        return activeRobots.values.filter { robot ->
            robot.level == level && 
            robot.pos.closerThan(center, range)
        }
    }
    
    private fun queueItemsForRobot(robot: RobotInfo, drops: MutableCollection<ItemEntity>) {
        // Queue items for robot pickup
    }
    
    private fun queueItemForMagnet(robot: RobotInfo, item: ItemEntity) {
        // Add item to magnet queue
    }
    
    /**
     * Register a robot as active.
     */
    fun registerRobot(level: Level, pos: BlockPos): RobotInfo {
        val info = RobotInfo(level, pos)
        activeRobots[pos] = info
        return info
    }
    
    /**
     * Unregister a robot.
     */
    fun unregisterRobot(pos: BlockPos) {
        activeRobots.remove(pos)
    }
    
    /**
     * Update robot position after movement.
     */
    fun updateRobotPosition(oldPos: BlockPos, newPos: BlockPos) {
        activeRobots[oldPos]?.let { robot ->
            activeRobots.remove(oldPos)
            activeRobots[newPos] = robot.copy(pos = newPos)
        }
    }
    
    /**
     * Check if a robot can move to the target position.
     */
    fun canRobotMoveTo(level: Level, from: BlockPos, to: BlockPos): Boolean {
        // Check if target is occupied by another robot
        if (activeRobots[to] != null) return false
        
        // Check if target block is passable
        val state = level.getBlockState(to)
        if (!state.isAir && state.getCollisionShape(level, to).isEmpty) {
            return false
        }
        
        return true
    }
    
    /**
     * Get all active robots in a level.
     */
    fun getRobotsInLevel(level: Level): List<RobotInfo> {
        return activeRobots.values.filter { it.level == level }
    }
}
