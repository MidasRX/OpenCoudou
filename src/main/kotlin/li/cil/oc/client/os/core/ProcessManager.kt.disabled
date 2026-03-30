package li.cil.oc.client.os.core

import kotlinx.coroutines.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Process manager for SkibidiOS2.
 * Handles process lifecycle, scheduling, and resource management.
 */
class ProcessManager(private val os: KotlinOS) {
    
    private val processes = ConcurrentHashMap<Int, Process>()
    private val pidCounter = AtomicInteger(1)
    private val processScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    val processCount: Int get() = processes.size
    
    /**
     * Create and start a new process.
     */
    fun spawn(name: String, priority: ProcessPriority = ProcessPriority.NORMAL, block: suspend () -> Unit): Process {
        val pid = pidCounter.getAndIncrement()
        val process = Process(
            pid = pid,
            name = name,
            priority = priority,
            state = ProcessState.RUNNING,
            startTime = System.currentTimeMillis()
        )
        
        process.job = processScope.launch {
            try {
                block()
                process.state = ProcessState.TERMINATED
            } catch (e: CancellationException) {
                process.state = ProcessState.KILLED
            } catch (e: Exception) {
                process.state = ProcessState.CRASHED
                process.exitCode = -1
            } finally {
                processes.remove(pid)
            }
        }
        
        processes[pid] = process
        return process
    }
    
    /**
     * Kill a process by PID.
     */
    fun kill(pid: Int): Boolean {
        val process = processes[pid] ?: return false
        process.job?.cancel()
        process.state = ProcessState.KILLED
        processes.remove(pid)
        return true
    }
    
    /**
     * Kill all processes.
     */
    fun killAll() {
        processes.values.forEach { process ->
            process.job?.cancel()
            process.state = ProcessState.KILLED
        }
        processes.clear()
    }
    
    /**
     * Get process by PID.
     */
    fun getProcess(pid: Int): Process? = processes[pid]
    
    /**
     * Get all running processes.
     */
    fun listProcesses(): List<Process> = processes.values.toList()
    
    /**
     * Process events from all running processes.
     */
    fun processEvents() {
        // Event processing happens via coroutines
        // This method is for synchronous event handling if needed
    }
}

/**
 * Represents a running process in the OS.
 */
data class Process(
    val pid: Int,
    val name: String,
    val priority: ProcessPriority,
    var state: ProcessState,
    val startTime: Long,
    var exitCode: Int = 0,
    var job: Job? = null
) {
    val uptime: Long get() = System.currentTimeMillis() - startTime
    
    fun isAlive(): Boolean = state == ProcessState.RUNNING || state == ProcessState.SLEEPING
}

/**
 * Process priority levels.
 */
enum class ProcessPriority(val value: Int) {
    REALTIME(0),
    HIGH(1),
    NORMAL(2),
    LOW(3),
    IDLE(4)
}

/**
 * Process states.
 */
enum class ProcessState {
    RUNNING,
    SLEEPING,
    WAITING,
    TERMINATED,
    KILLED,
    CRASHED
}
