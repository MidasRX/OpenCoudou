package li.cil.oc.server.component

/**
 * Battery upgrade - provides additional energy storage for robots and tablets.
 * Higher tiers provide more storage.
 */
class BatteryUpgradeComponent(private val tier: Int = 1) : AbstractComponent("battery") {
    
    private val capacity: Double = when (tier) {
        1 -> 10000.0
        2 -> 25000.0
        3 -> 50000.0
        else -> 10000.0
    }
    
    private var storedEnergy: Double = 0.0
    
    init {
        registerMethod("level", true, "level():number -- Get current energy level (0-1)") { _ ->
            arrayOf(storedEnergy / capacity)
        }
        
        registerMethod("energy", true, "energy():number -- Get current energy") { _ ->
            arrayOf(storedEnergy)
        }
        
        registerMethod("maxEnergy", true, "maxEnergy():number -- Get max energy storage") { _ ->
            arrayOf(capacity)
        }
    }
    
    fun getCapacity(): Double = capacity
    
    fun getStoredEnergy(): Double = storedEnergy
    
    fun addEnergy(amount: Double): Double {
        val toAdd = minOf(amount, capacity - storedEnergy)
        storedEnergy += toAdd
        return toAdd
    }
    
    fun consumeEnergy(amount: Double): Boolean {
        if (storedEnergy >= amount) {
            storedEnergy -= amount
            return true
        }
        return false
    }
    
    fun drainEnergy(amount: Double): Double {
        val toDrain = minOf(amount, storedEnergy)
        storedEnergy -= toDrain
        return toDrain
    }
}
