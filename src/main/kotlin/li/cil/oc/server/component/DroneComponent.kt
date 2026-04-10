package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import kotlin.math.abs

/**
 * Drone component - flying mobile computer with limited capabilities.
 * Drones can fly, interact with world, and perform tasks faster than robots.
 */
class DroneComponent : AbstractComponent("drone") {
    
    companion object {
        const val MAX_VELOCITY = 8.0
        const val ACCELERATION = 0.5
        const val ENERGY_COST_MOVE = 0.5
        const val ENERGY_COST_ACTION = 5.0
    }
    
    private var world: Level? = null
    private var position: BlockPos = BlockPos.ZERO
    private var targetPos: BlockPos = BlockPos.ZERO
    
    // Floating point position for smooth movement
    private var x: Double = 0.0
    private var y: Double = 0.0
    private var z: Double = 0.0
    
    // Target position
    private var targetX: Double = 0.0
    private var targetY: Double = 0.0
    private var targetZ: Double = 0.0
    
    // Velocity
    private var velocityX: Double = 0.0
    private var velocityY: Double = 0.0
    private var velocityZ: Double = 0.0
    
    // Energy
    private var energy: Double = 5000.0
    private var maxEnergy: Double = 5000.0
    
    // State
    private var lightColor: Int = 0x66FF66
    private var selectedSlot: Int = 1
    private var droneMode: String = "hover" // hover, follow, guard
    
    init {
        // Movement methods
        registerMethod("move", false, "move(dx:number,dy:number,dz:number):boolean -- Move by offset") { args ->
            val dx = (args.getOrNull(0) as? Number)?.toDouble() ?: 0.0
            val dy = (args.getOrNull(1) as? Number)?.toDouble() ?: 0.0
            val dz = (args.getOrNull(2) as? Number)?.toDouble() ?: 0.0
            
            // Set target relative to current position
            targetX = x + dx
            targetY = y + dy  
            targetZ = z + dz
            
            arrayOf(true)
        }
        
        registerMethod("getOffset", true, "getOffset():number,number,number -- Get remaining offset to target") { _ ->
            arrayOf(targetX - x, targetY - y, targetZ - z)
        }
        
        registerMethod("getVelocity", true, "getVelocity():number,number,number -- Get current velocity") { _ ->
            arrayOf(velocityX, velocityY, velocityZ)
        }
        
        registerMethod("getMaxVelocity", true, "getMaxVelocity():number -- Get max velocity") { _ ->
            arrayOf(MAX_VELOCITY)
        }
        
        registerMethod("getAcceleration", true, "getAcceleration():number -- Get acceleration") { _ ->
            arrayOf(ACCELERATION)
        }
        
        // Inventory
        registerMethod("select", false, "select(slot:number):number -- Select inventory slot") { args ->
            val slot = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(selectedSlot)
            selectedSlot = slot.coerceIn(1, 4) // Drones have limited inventory
            arrayOf(selectedSlot)
        }
        
        registerMethod("selectedSlot", true, "selectedSlot():number -- Get selected slot") { _ ->
            arrayOf(selectedSlot)
        }
        
        registerMethod("count", true, "count([slot:number]):number -- Get item count") { args ->
            val slot = (args.getOrNull(0) as? Number)?.toInt() ?: selectedSlot
            arrayOf(0) // Would check actual inventory
        }
        
        // Actions
        registerMethod("swing", false, "swing([side:string]):boolean,string -- Swing") { args ->
            if (!consumeEnergy(ENERGY_COST_ACTION)) {
                return@registerMethod arrayOf(false, "not enough energy")
            }
            arrayOf(true, "block")
        }
        
        registerMethod("use", false, "use([side:string,duration:number]):boolean,string -- Use item") { args ->
            if (!consumeEnergy(ENERGY_COST_ACTION)) {
                return@registerMethod arrayOf(false, "not enough energy")
            }
            arrayOf(true, "item_used")
        }
        
        registerMethod("place", false, "place([side:string]):boolean,string -- Place block") { args ->
            if (!consumeEnergy(ENERGY_COST_ACTION)) {
                return@registerMethod arrayOf(false, "not enough energy")
            }
            arrayOf(true, "placed")
        }
        
        registerMethod("suck", false, "suck([side:string,count:number]):boolean -- Pick up items") { args ->
            val count = (args.getOrNull(1) as? Number)?.toInt() ?: 64
            arrayOf(true)
        }
        
        registerMethod("drop", false, "drop([side:string,count:number]):boolean -- Drop items") { args ->
            val count = (args.getOrNull(1) as? Number)?.toInt() ?: 64
            arrayOf(true)
        }
        
        // Detection
        registerMethod("detect", true, "detect([side:string]):boolean,string -- Detect block") { args ->
            val level = world ?: return@registerMethod arrayOf(false, "air")
            // Check in front
            val checkPos = BlockPos(x.toInt(), y.toInt(), z.toInt() - 1)
            val state = level.getBlockState(checkPos)
            arrayOf(!state.isAir, if (state.isAir) "air" else "solid")
        }
        
        // Energy
        registerMethod("energy", true, "energy():number -- Get current energy") { _ ->
            arrayOf(energy)
        }
        
        registerMethod("maxEnergy", true, "maxEnergy():number -- Get max energy") { _ ->
            arrayOf(maxEnergy)
        }
        
        // Status
        registerMethod("name", true, "name():string -- Get drone name") { _ ->
            arrayOf("Drone")
        }
        
        registerMethod("getLightColor", true, "getLightColor():number -- Get status light color") { _ ->
            arrayOf(lightColor)
        }
        
        registerMethod("setLightColor", false, "setLightColor(color:number):number -- Set status light") { args ->
            val newColor = (args.getOrNull(0) as? Number)?.toInt() ?: lightColor
            val oldColor = lightColor
            lightColor = newColor and 0xFFFFFF
            arrayOf(oldColor)
        }
        
        registerMethod("getStatusText", true, "getStatusText():string -- Get status text") { _ ->
            arrayOf("idle")
        }
        
        registerMethod("setStatusText", false, "setStatusText(text:string):string -- Set status text") { args ->
            val text = args.getOrNull(0)?.toString() ?: ""
            arrayOf(text)
        }
    }
    
    fun setWorld(level: Level?, pos: BlockPos) {
        this.world = level
        this.position = pos
        this.x = pos.x.toDouble()
        this.y = pos.y.toDouble()
        this.z = pos.z.toDouble()
        this.targetX = x
        this.targetY = y
        this.targetZ = z
    }
    
    fun tick() {
        // Move towards target
        val dx = targetX - x
        val dy = targetY - y
        val dz = targetZ - z
        
        val distance = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
        if (distance < 0.01) {
            velocityX = 0.0
            velocityY = 0.0
            velocityZ = 0.0
            return
        }
        
        // Calculate acceleration direction
        if (distance > 0) {
            velocityX += (dx / distance) * ACCELERATION
            velocityY += (dy / distance) * ACCELERATION
            velocityZ += (dz / distance) * ACCELERATION
        }
        
        // Clamp velocity
        velocityX = velocityX.coerceIn(-MAX_VELOCITY, MAX_VELOCITY)
        velocityY = velocityY.coerceIn(-MAX_VELOCITY, MAX_VELOCITY)
        velocityZ = velocityZ.coerceIn(-MAX_VELOCITY, MAX_VELOCITY)
        
        // Apply velocity if we have energy
        val energyCost = ENERGY_COST_MOVE * (abs(velocityX) + abs(velocityY) + abs(velocityZ)) / MAX_VELOCITY
        if (consumeEnergy(energyCost)) {
            x += velocityX * 0.05 // Scale to tick time
            y += velocityY * 0.05
            z += velocityZ * 0.05
        } else {
            velocityX = 0.0
            velocityY = 0.0
            velocityZ = 0.0
        }
        
        position = BlockPos(x.toInt(), y.toInt(), z.toInt())
    }
    
    private fun consumeEnergy(amount: Double): Boolean {
        if (energy >= amount) {
            energy -= amount
            return true
        }
        return false
    }
    
    fun addEnergy(amount: Double): Double {
        val toAdd = amount.coerceIn(0.0, maxEnergy - energy)
        energy += toAdd
        return toAdd
    }
    
    fun getExactPosition(): Triple<Double, Double, Double> = Triple(x, y, z)
}
