package li.cil.oc.server.component

/**
 * Power Converter component.
 * Converts Forge Energy (FE) to OC internal energy.
 * 
 * Conversion ratio: 10 FE = 1 OC Energy (like original OC)
 */
class PowerConverterComponent : AbstractComponent("power_converter") {
    
    companion object {
        const val CONVERSION_RATIO = 10.0 // FE to OC energy
        const val MAX_BUFFER = 100000 // Internal buffer capacity
    }
    
    var storedEnergy: Double = 0.0
    var maxEnergy: Double = MAX_BUFFER.toDouble()
    
    init {
        registerMethod("getEnergy", true, "getEnergy():number -- Get stored energy") { _ ->
            arrayOf(storedEnergy)
        }
        
        registerMethod("getMaxEnergy", true, "getMaxEnergy():number -- Get max energy capacity") { _ ->
            arrayOf(maxEnergy)
        }
        
        registerMethod("getConversionRate", true, "getConversionRate():number -- Get FE to OC conversion rate") { _ ->
            arrayOf(CONVERSION_RATIO)
        }
    }
    
    /**
     * Receive Forge Energy and convert to OC energy.
     * @param feAmount Amount of FE to receive
     * @param simulate If true, don't actually change the stored energy
     * @return Amount of FE that was accepted
     */
    fun receiveEnergy(feAmount: Int, simulate: Boolean): Int {
        val ocAmount = feAmount / CONVERSION_RATIO
        val canStore = maxEnergy - storedEnergy
        val toStore = minOf(ocAmount, canStore)
        
        if (!simulate) {
            storedEnergy += toStore
        }
        
        return (toStore * CONVERSION_RATIO).toInt()
    }
    
    /**
     * Extract OC energy.
     * @param amount Amount of OC energy to extract
     * @return Actual amount extracted
     */
    fun extractEnergy(amount: Double): Double {
        val toExtract = minOf(amount, storedEnergy)
        storedEnergy -= toExtract
        return toExtract
    }
}
