package li.cil.oc.common.event

import li.cil.oc.OpenComputers
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.tick.LevelTickEvent
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Handles scheduled tasks and deferred actions for OpenComputers.
 * Used for things like robot movement animations, delayed operations, etc.
 */
@EventBusSubscriber(modid = OpenComputers.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
object SchedulerEventHandler {
    
    private val logger = LoggerFactory.getLogger("OpenComputers")
    
    // Tasks scheduled for execution
    private val scheduledTasks = ConcurrentLinkedQueue<ScheduledTask>()
    
    // Repeating tasks
    private val repeatingTasks = ConcurrentHashMap<String, RepeatingTask>()
    
    // One-shot delayed tasks by level
    private val delayedTasks = ConcurrentHashMap<Level, MutableList<DelayedTask>>()
    
    data class ScheduledTask(
        val executeAt: Long,
        val action: () -> Unit
    )
    
    data class RepeatingTask(
        val id: String,
        val intervalTicks: Int,
        var lastRun: Long,
        val action: () -> Unit
    )
    
    data class DelayedTask(
        val executeAt: Long,
        val action: () -> Unit
    )
    
    @SubscribeEvent
    @JvmStatic
    fun onLevelTick(event: LevelTickEvent.Post) {
        val level = event.level
        if (level.isClientSide) return
        
        val gameTime = level.gameTime
        
        // Process scheduled tasks
        processScheduledTasks(gameTime)
        
        // Process repeating tasks
        processRepeatingTasks(gameTime)
        
        // Process delayed tasks for this level
        processDelayedTasks(level, gameTime)
    }
    
    private fun processScheduledTasks(currentTime: Long) {
        val iterator = scheduledTasks.iterator()
        while (iterator.hasNext()) {
            val task = iterator.next()
            if (currentTime >= task.executeAt) {
                try {
                    task.action()
                } catch (e: Exception) {
                    logger.error("Error executing scheduled task", e)
                }
                iterator.remove()
            }
        }
    }
    
    private fun processRepeatingTasks(currentTime: Long) {
        for ((_, task) in repeatingTasks) {
            if (currentTime - task.lastRun >= task.intervalTicks) {
                try {
                    task.action()
                    task.lastRun = currentTime
                } catch (e: Exception) {
                    logger.error("Error executing repeating task ${task.id}", e)
                }
            }
        }
    }
    
    private fun processDelayedTasks(level: Level, currentTime: Long) {
        val tasks = delayedTasks[level] ?: return
        
        val iterator = tasks.iterator()
        while (iterator.hasNext()) {
            val task = iterator.next()
            if (currentTime >= task.executeAt) {
                try {
                    task.action()
                } catch (e: Exception) {
                    logger.error("Error executing delayed task", e)
                }
                iterator.remove()
            }
        }
    }
    
    // ========================================
    // Public API
    // ========================================
    
    /**
     * Schedule a task to run at a specific game time.
     */
    fun scheduleAt(gameTime: Long, action: () -> Unit) {
        scheduledTasks.add(ScheduledTask(gameTime, action))
    }
    
    /**
     * Schedule a task to run after a delay.
     */
    fun scheduleAfter(level: Level, delayTicks: Int, action: () -> Unit) {
        val tasks = delayedTasks.getOrPut(level) { mutableListOf() }
        tasks.add(DelayedTask(level.gameTime + delayTicks, action))
    }
    
    /**
     * Register a repeating task.
     */
    fun registerRepeating(id: String, intervalTicks: Int, action: () -> Unit) {
        repeatingTasks[id] = RepeatingTask(id, intervalTicks, 0, action)
    }
    
    /**
     * Unregister a repeating task.
     */
    fun unregisterRepeating(id: String) {
        repeatingTasks.remove(id)
    }
    
    /**
     * Clear all tasks for a level (use on level unload).
     */
    fun clearLevelTasks(level: Level) {
        delayedTasks.remove(level)
    }
    
    /**
     * Schedule a robot movement animation.
     * Smoothly interpolates robot position over multiple ticks.
     */
    fun scheduleRobotMove(
        level: Level,
        fromPos: BlockPos,
        toPos: BlockPos,
        durationTicks: Int,
        onComplete: () -> Unit
    ) {
        var ticksElapsed = 0
        
        val moveTask = RepeatingTask(
            id = "robot_move_${fromPos}_${toPos}",
            intervalTicks = 1,
            lastRun = level.gameTime,
            action = {
                ticksElapsed++
                if (ticksElapsed >= durationTicks) {
                    unregisterRepeating("robot_move_${fromPos}_${toPos}")
                    onComplete()
                }
            }
        )
        
        repeatingTasks[moveTask.id] = moveTask
    }
    
    /**
     * Schedule a drone flight path.
     */
    fun scheduleDroneFlight(
        level: Level,
        droneId: String,
        waypoints: List<BlockPos>,
        speedBlocksPerTick: Float,
        onWaypoint: (Int, BlockPos) -> Unit,
        onComplete: () -> Unit
    ) {
        var currentWaypoint = 0
        
        val flightTask = RepeatingTask(
            id = "drone_flight_$droneId",
            intervalTicks = 1,
            lastRun = level.gameTime,
            action = {
                if (currentWaypoint < waypoints.size) {
                    onWaypoint(currentWaypoint, waypoints[currentWaypoint])
                    currentWaypoint++
                } else {
                    unregisterRepeating("drone_flight_$droneId")
                    onComplete()
                }
            }
        )
        
        repeatingTasks[flightTask.id] = flightTask
    }
    
    /**
     * Schedule an assembler operation.
     */
    fun scheduleAssembly(
        level: Level,
        pos: BlockPos,
        assemblyTimeTicks: Int,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit
    ) {
        var ticksElapsed = 0
        val taskId = "assembler_$pos"
        
        val assemblyTask = RepeatingTask(
            id = taskId,
            intervalTicks = 1,
            lastRun = level.gameTime,
            action = {
                ticksElapsed++
                val progress = ticksElapsed.toFloat() / assemblyTimeTicks
                onProgress(progress)
                
                if (ticksElapsed >= assemblyTimeTicks) {
                    unregisterRepeating(taskId)
                    onComplete()
                }
            }
        )
        
        repeatingTasks[assemblyTask.id] = assemblyTask
    }
    
    /**
     * Schedule a print job.
     */
    fun schedulePrintJob(
        level: Level,
        pos: BlockPos,
        printTimeTicks: Int,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit
    ) {
        var ticksElapsed = 0
        val taskId = "printer_$pos"
        
        val printTask = RepeatingTask(
            id = taskId,
            intervalTicks = 1,
            lastRun = level.gameTime,
            action = {
                ticksElapsed++
                val progress = ticksElapsed.toFloat() / printTimeTicks
                onProgress(progress)
                
                if (ticksElapsed >= printTimeTicks) {
                    unregisterRepeating(taskId)
                    onComplete()
                }
            }
        )
        
        repeatingTasks[printTask.id] = printTask
    }
}
