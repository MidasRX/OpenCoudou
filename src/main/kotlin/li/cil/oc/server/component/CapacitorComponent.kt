package li.cil.oc.server.component

/**
 * Capacitor component - stores OC energy.
 * Multiple capacitors can be placed together to increase storage.
 */
class CapacitorComponent : AbstractComponent("capacitor") {
    
    companion object {
        const val BASE_CAPACITY = 100000.0 // OC energy units
    }
    
    var storedEnergy: Double = 0.0
    var maxEnergy: Double = BASE_CAPACITY
    
    init {
        registerMethod("getEnergy", true, "getEnergy():number -- Get stored energy") { _ ->
            arrayOf(storedEnergy)
        }
        
        registerMethod("getMaxEnergy", true, "getMaxEnergy():number -- Get max energy capacity") { _ ->
            arrayOf(maxEnergy)
        }
        
        registerMethod("getFillLevel", true, "getFillLevel():number -- Get fill level (0.0 to 1.0)") { _ ->
            arrayOf(if (maxEnergy > 0) storedEnergy / maxEnergy else 0.0)
        }
    }
    
    /**
     * Store energy in the capacitor.
     * @param amount Amount of OC energy to store
     * @return Actual amount stored
     */
    fun storeEnergy(amount: Double): Double {
        val canStore = maxEnergy - storedEnergy
        val toStore = minOf(amount, canStore)
        storedEnergy += toStore
        return toStore
    }
    
    /**
     * Extract energy from the capacitor.
     * @param amount Amount of OC energy to extract
     * @return Actual amount extracted
     */
    fun extractEnergy(amount: Double): Double {
        val toExtract = minOf(amount, storedEnergy)
        storedEnergy -= toExtract
        return toExtract
    }
}
