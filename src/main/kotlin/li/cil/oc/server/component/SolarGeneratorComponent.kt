package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.LightLayer

/**
 * Solar generator upgrade - generates energy from sunlight.
 * Only works when the robot/drone can see the sky.
 */
class SolarGeneratorComponent(private val tier: Int = 1) : AbstractComponent("solar_generator") {
    
    companion object {
        const val TIER1_RATE = 2.0
        const val TIER2_RATE = 4.0
    }
    
    private var world: Level? = null
    private var position: BlockPos = BlockPos.ZERO
    
    private val generationRate: Double = when (tier) {
        1 -> TIER1_RATE
        2 -> TIER2_RATE
        else -> TIER1_RATE
    }
    
    init {
        registerMethod("isSunShining", true, "isSunShining():boolean -- Check if sunlight is available") { _ ->
            arrayOf(canGeneratePower())
        }
    }
    
    fun setWorld(level: Level?, pos: BlockPos) {
        this.world = level
        this.position = pos
    }
    
    fun setPosition(pos: BlockPos) {
        this.position = pos
    }
    
    /**
     * Generate energy from sunlight.
     * @return Energy generated this tick
     */
    fun tick(): Double {
        return if (canGeneratePower()) {
            val skyLight = world?.getBrightness(LightLayer.SKY, position.above()) ?: 0
            // Scale by sky light level (0-15)
            generationRate * (skyLight / 15.0)
        } else {
            0.0
        }
    }
    
    /**
     * Check if we can generate power.
     * Needs daytime, clear sky view, and not raining.
     */
    fun canGeneratePower(): Boolean {
        val level = world ?: return false
        
        // Check if it's daytime
        val timeOfDay = level.dayTime % 24000
        if (timeOfDay > 12000 && timeOfDay < 23000) {
            return false // Night time
        }
        
        // Check for rain
        if (level.isRaining && level.canSeeSky(position.above())) {
            // Reduced output during rain
            return true
        }
        
        // Check for clear sky view
        return level.canSeeSky(position.above())
    }
}
