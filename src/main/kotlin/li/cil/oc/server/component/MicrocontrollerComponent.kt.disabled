package li.cil.oc.server.component

/**
 * Microcontroller component - a simplified computer with limited functionality.
 * No file system access, limited memory, but low power consumption.
 */
class MicrocontrollerComponent(private val tier: Int = 1) : AbstractComponent("microcontroller") {
    
    companion object {
        const val TIER1_RAM = 2 // KB
        const val TIER2_RAM = 4 // KB
        const val ENERGY_COST_PER_TICK = 0.5
    }
    
    private var isRunning: Boolean = false
    private var startTime: Long = 0
    
    private val ramSize: Int = when (tier) {
        1 -> TIER1_RAM
        2 -> TIER2_RAM
        else -> TIER1_RAM
    }
    
    init {
        registerMethod("start", false, "start():boolean -- Start the microcontroller") { _ ->
            if (isRunning) {
                return@registerMethod arrayOf(false, "already running")
            }
            isRunning = true
            startTime = System.currentTimeMillis()
            arrayOf(true)
        }
        
        registerMethod("stop", false, "stop():boolean -- Stop the microcontroller") { _ ->
            if (!isRunning) {
                return@registerMethod arrayOf(false, "not running")
            }
            isRunning = false
            arrayOf(true)
        }
        
        registerMethod("isRunning", true, "isRunning():boolean -- Check if running") { _ ->
            arrayOf(isRunning)
        }
        
        registerMethod("uptime", true, "uptime():number -- Get uptime in seconds") { _ ->
            val uptime = if (isRunning) {
                (System.currentTimeMillis() - startTime) / 1000.0
            } else 0.0
            arrayOf(uptime)
        }
        
        registerMethod("getTotalMemory", true, "getTotalMemory():number -- Get total RAM") { _ ->
            arrayOf(ramSize * 1024)
        }
        
        registerMethod("getFreeMemory", true, "getFreeMemory():number -- Get free RAM") { _ ->
            // Would track actual memory usage
            arrayOf(ramSize * 1024)
        }
    }
    
    fun isRunning(): Boolean = isRunning
    
    fun tick(): Double {
        if (!isRunning) return 0.0
        return ENERGY_COST_PER_TICK
    }
}
