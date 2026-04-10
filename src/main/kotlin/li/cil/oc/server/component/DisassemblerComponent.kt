package li.cil.oc.server.component

/**
 * Disassembler component - breaks down robots and other items into components.
 * Takes assembled machines and returns their parts.
 */
class DisassemblerComponent : AbstractComponent("disassembler") {
    
    companion object {
        const val DISASSEMBLY_TIME_BASE = 100 // ticks per item
        const val ENERGY_COST_PER_TICK = 5.0
    }
    
    // Disassembly state
    private var isDisassembling: Boolean = false
    private var disassemblyProgress: Int = 0
    private var disassemblyTarget: Int = 0
    
    init {
        registerMethod("status", true, "status():string,number -- Get disassembler status") { _ ->
            if (isDisassembling) {
                val progress = if (disassemblyTarget > 0) {
                    (disassemblyProgress.toDouble() / disassemblyTarget * 100).toInt()
                } else 0
                arrayOf("busy", progress)
            } else {
                arrayOf("idle", 100)
            }
        }
        
        registerMethod("start", false, "start():boolean,string -- Start disassembly") { _ ->
            if (isDisassembling) {
                return@registerMethod arrayOf(false, "already disassembling")
            }
            
            // Would check for item to disassemble
            isDisassembling = true
            disassemblyProgress = 0
            disassemblyTarget = DISASSEMBLY_TIME_BASE
            
            arrayOf(true)
        }
        
        registerMethod("stop", false, "stop():boolean -- Stop disassembly") { _ ->
            val wasDisassembling = isDisassembling
            isDisassembling = false
            disassemblyProgress = 0
            arrayOf(wasDisassembling)
        }
    }
    
    fun tick(energyAvailable: Double): Double {
        if (!isDisassembling) return 0.0
        
        if (energyAvailable < ENERGY_COST_PER_TICK) {
            return 0.0
        }
        
        disassemblyProgress++
        
        if (disassemblyProgress >= disassemblyTarget) {
            isDisassembling = false
            // Would produce component items
        }
        
        return ENERGY_COST_PER_TICK
    }
    
    fun isDisassembling(): Boolean = isDisassembling
    
    fun getProgress(): Double {
        return if (disassemblyTarget > 0) {
            disassemblyProgress.toDouble() / disassemblyTarget
        } else 0.0
    }
}
