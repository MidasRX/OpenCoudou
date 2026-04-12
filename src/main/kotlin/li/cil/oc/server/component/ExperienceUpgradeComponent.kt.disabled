package li.cil.oc.server.component

/**
 * Experience upgrade - Robots can gain XP from harvesting and using it for bonuses.
 */
class ExperienceUpgradeComponent : AbstractComponent("experience") {
    
    companion object {
        const val XP_PER_LEVEL = 1000.0
        const val MAX_LEVEL = 30
    }
    
    private var experience: Double = 0.0
    
    init {
        registerMethod("level", true, "level():number -- Get current experience level") { _ ->
            arrayOf(getLevel())
        }
        
        registerMethod("experience", true, "experience():number -- Get raw experience points") { _ ->
            arrayOf(experience)
        }
        
        registerMethod("consume", false, "consume([amount:number]):number -- Consume XP for energy") { args ->
            val requestAmount = (args.getOrNull(0) as? Number)?.toDouble() ?: XP_PER_LEVEL
            val consumed = minOf(requestAmount, experience)
            experience -= consumed
            // Returns energy gained (100 energy per XP)
            arrayOf(consumed * 100)
        }
    }
    
    fun addExperience(amount: Double): Boolean {
        if (getLevel() >= MAX_LEVEL) return false
        experience += amount
        return true
    }
    
    fun getLevel(): Int {
        return (experience / XP_PER_LEVEL).toInt().coerceIn(0, MAX_LEVEL)
    }
    
    // Bonuses based on level
    fun getMiningSpeedBonus(): Double = 1.0 + (getLevel() * 0.05)
    fun getHarvestBonus(): Double = 1.0 + (getLevel() * 0.03)
    fun getEnergyEfficiency(): Double = 1.0 - (getLevel() * 0.01)
}
