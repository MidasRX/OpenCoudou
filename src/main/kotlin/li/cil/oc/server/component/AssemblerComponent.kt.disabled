package li.cil.oc.server.component

/**
 * Assembler component - assembles robots, drones, and other complex items.
 * Takes components and produces finished machines.
 */
class AssemblerComponent : AbstractComponent("assembler") {
    
    companion object {
        const val MAX_COMPLEXITY_TIER1 = 20
        const val MAX_COMPLEXITY_TIER2 = 30
        const val MAX_COMPLEXITY_TIER3 = 40
        
        const val ASSEMBLY_TIME_BASE = 200 // ticks
    }
    
    // Assembly state
    private var isAssembling: Boolean = false
    private var assemblyProgress: Int = 0
    private var assemblyTarget: Int = 0
    private var assemblyResult: String = ""
    
    init {
        registerMethod("status", true, "status():string,number -- Get assembler status") { _ ->
            if (isAssembling) {
                val progress = if (assemblyTarget > 0) {
                    (assemblyProgress.toDouble() / assemblyTarget * 100).toInt()
                } else 0
                arrayOf("busy", progress)
            } else {
                arrayOf("idle", 100)
            }
        }
        
        registerMethod("start", false, "start():boolean,string -- Start assembly") { _ ->
            if (isAssembling) {
                return@registerMethod arrayOf(false, "already assembling")
            }
            
            // Would check inventory for valid configuration
            // For now, always succeed if not busy
            isAssembling = true
            assemblyProgress = 0
            assemblyTarget = ASSEMBLY_TIME_BASE
            assemblyResult = "robot" // Default assembly type
            
            arrayOf(true)
        }
        
        registerMethod("complexity", true, "complexity():number,number -- Get current/max complexity") { _ ->
            // Would calculate actual complexity from inserted components
            arrayOf(0, MAX_COMPLEXITY_TIER1)
        }
    }
    
    fun tick(): Boolean {
        if (!isAssembling) return false
        
        assemblyProgress++
        
        if (assemblyProgress >= assemblyTarget) {
            isAssembling = false
            // Would produce assembled item
            return true
        }
        
        return false
    }
    
    fun isAssembling(): Boolean = isAssembling
    
    fun getProgress(): Double {
        return if (assemblyTarget > 0) {
            assemblyProgress.toDouble() / assemblyTarget
        } else 0.0
    }
}
