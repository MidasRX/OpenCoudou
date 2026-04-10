package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level

/**
 * Robot component - provides movement and interaction capabilities for robots.
 * Robots are mobile computers that can move, mine, place blocks, and interact with the world.
 */
class RobotComponent : AbstractComponent("robot") {
    
    companion object {
        const val ENERGY_COST_MOVE = 15.0
        const val ENERGY_COST_TURN = 5.0
        const val ENERGY_COST_SWING = 10.0
        const val ENERGY_COST_USE = 10.0
        const val ENERGY_COST_PLACE = 5.0
    }
    
    private var world: Level? = null
    private var position: BlockPos = BlockPos.ZERO
    private var facing: Direction = Direction.NORTH
    
    // Robot state
    private var selectedSlot: Int = 1
    private var energy: Double = 10000.0
    private var maxEnergy: Double = 10000.0
    private var lightColor: Int = 0x66FF66  // Default green
    
    init {
        // Movement methods
        registerMethod("forward", false, "forward():boolean,string -- Move forward") { _ ->
            val success = tryMove(facing)
            arrayOf(success, if (success) null else "movement impossible")
        }
        
        registerMethod("back", false, "back():boolean,string -- Move backward") { _ ->
            val success = tryMove(facing.opposite)
            arrayOf(success, if (success) null else "movement impossible")
        }
        
        registerMethod("up", false, "up():boolean,string -- Move up") { _ ->
            val success = tryMove(Direction.UP)
            arrayOf(success, if (success) null else "movement impossible")
        }
        
        registerMethod("down", false, "down():boolean,string -- Move down") { _ ->
            val success = tryMove(Direction.DOWN)
            arrayOf(success, if (success) null else "movement impossible")
        }
        
        registerMethod("turnLeft", false, "turnLeft():boolean -- Turn left") { _ ->
            if (consumeEnergy(ENERGY_COST_TURN)) {
                facing = facing.counterClockWise
                arrayOf(true)
            } else {
                arrayOf(false, "not enough energy")
            }
        }
        
        registerMethod("turnRight", false, "turnRight():boolean -- Turn right") { _ ->
            if (consumeEnergy(ENERGY_COST_TURN)) {
                facing = facing.clockWise
                arrayOf(true)
            } else {
                arrayOf(false, "not enough energy")
            }
        }
        
        registerMethod("turnAround", false, "turnAround():boolean -- Turn 180 degrees") { _ ->
            if (consumeEnergy(ENERGY_COST_TURN * 2)) {
                facing = facing.opposite
                arrayOf(true)
            } else {
                arrayOf(false, "not enough energy")
            }
        }
        
        // Inventory methods
        registerMethod("select", false, "select(slot:number):number -- Select inventory slot") { args ->
            val slot = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(selectedSlot)
            selectedSlot = slot.coerceIn(1, inventorySize())
            arrayOf(selectedSlot)
        }
        
        registerMethod("selectedSlot", true, "selectedSlot():number -- Get selected slot") { _ ->
            arrayOf(selectedSlot)
        }
        
        registerMethod("inventorySize", true, "inventorySize():number -- Get inventory size") { _ ->
            arrayOf(inventorySize())
        }
        
        registerMethod("count", true, "count([slot:number]):number -- Get item count in slot") { args ->
            val slot = (args.getOrNull(0) as? Number)?.toInt() ?: selectedSlot
            arrayOf(getStackSize(slot))
        }
        
        registerMethod("space", true, "space([slot:number]):number -- Get free space in slot") { args ->
            val slot = (args.getOrNull(0) as? Number)?.toInt() ?: selectedSlot
            arrayOf(64 - getStackSize(slot))
        }
        
        registerMethod("compareTo", true, "compareTo(slot:number):boolean -- Compare selected to slot") { args ->
            val slot = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(false)
            arrayOf(compareSlots(selectedSlot, slot))
        }
        
        registerMethod("transferTo", false, "transferTo(slot:number[,count:number]):boolean -- Move items") { args ->
            val slot = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(false)
            val count = (args.getOrNull(1) as? Number)?.toInt() ?: 64
            arrayOf(transferItems(selectedSlot, slot, count))
        }
        
        // Interaction methods
        registerMethod("swing", false, "swing([side:string]):boolean,string -- Swing tool") { args ->
            val side = parseSide(args.getOrNull(0)?.toString())
            if (!consumeEnergy(ENERGY_COST_SWING)) {
                return@registerMethod arrayOf(false, "not enough energy")
            }
            val result = trySwing(side)
            arrayOf(result.first, result.second)
        }
        
        registerMethod("use", false, "use([side:string,sneaky:boolean]):boolean,string -- Use item") { args ->
            val side = parseSide(args.getOrNull(0)?.toString())
            val sneaky = args.getOrNull(1) as? Boolean ?: false
            if (!consumeEnergy(ENERGY_COST_USE)) {
                return@registerMethod arrayOf(false, "not enough energy")
            }
            val result = tryUse(side, sneaky)
            arrayOf(result.first, result.second)
        }
        
        registerMethod("place", false, "place([side:string,sneaky:boolean]):boolean,string -- Place block") { args ->
            val side = parseSide(args.getOrNull(0)?.toString())
            val sneaky = args.getOrNull(1) as? Boolean ?: false
            if (!consumeEnergy(ENERGY_COST_PLACE)) {
                return@registerMethod arrayOf(false, "not enough energy")
            }
            val result = tryPlace(side, sneaky)
            arrayOf(result.first, result.second)
        }
        
        registerMethod("drop", false, "drop([side:string,count:number]):boolean -- Drop items") { args ->
            val side = parseSide(args.getOrNull(0)?.toString())
            val count = (args.getOrNull(1) as? Number)?.toInt() ?: 64
            arrayOf(tryDrop(side, count))
        }
        
        registerMethod("suck", false, "suck([side:string,count:number]):boolean -- Pick up items") { args ->
            val side = parseSide(args.getOrNull(0)?.toString())
            val count = (args.getOrNull(1) as? Number)?.toInt() ?: 64
            arrayOf(trySuck(side, count))
        }
        
        // Detection methods
        registerMethod("detect", true, "detect([side:string]):boolean,string -- Detect block") { args ->
            val side = parseSide(args.getOrNull(0)?.toString())
            val result = detectBlock(side)
            arrayOf(result.first, result.second)
        }
        
        registerMethod("compare", true, "compare([side:string]):boolean -- Compare block") { args ->
            val side = parseSide(args.getOrNull(0)?.toString())
            arrayOf(compareBlock(side))
        }
        
        // Tank methods (for tank upgrades)
        registerMethod("tankCount", true, "tankCount():number -- Get number of tanks") { _ ->
            arrayOf(0) // No tanks by default
        }
        
        registerMethod("selectTank", false, "selectTank(tank:number):number -- Select tank") { args ->
            val tank = (args.getOrNull(0) as? Number)?.toInt() ?: 1
            arrayOf(tank.coerceIn(1, 1))
        }
        
        // Energy methods
        registerMethod("energy", true, "energy():number -- Get current energy") { _ ->
            arrayOf(energy)
        }
        
        registerMethod("maxEnergy", true, "maxEnergy():number -- Get max energy") { _ ->
            arrayOf(maxEnergy)
        }
        
        // Status methods
        registerMethod("name", true, "name():string -- Get robot name") { _ ->
            arrayOf("Robot")
        }
        
        registerMethod("getLightColor", true, "getLightColor():number -- Get status light color") { _ ->
            arrayOf(lightColor)
        }
        
        registerMethod("setLightColor", false, "setLightColor(color:number):number -- Set status light color") { args ->
            val newColor = (args.getOrNull(0) as? Number)?.toInt() ?: lightColor
            val oldColor = lightColor
            lightColor = newColor and 0xFFFFFF
            arrayOf(oldColor)
        }
        
        // Durability for tools
        registerMethod("durability", true, "durability():number -- Get durability of equipped tool") { _ ->
            arrayOf(1.0) // Full durability
        }
    }
    
    fun setWorld(level: Level?, pos: BlockPos) {
        this.world = level
        this.position = pos
    }
    
    fun setFacing(dir: Direction) {
        this.facing = dir
    }
    
    fun getFacing(): Direction = facing
    
    private fun parseSide(side: String?): Direction {
        return when (side?.lowercase()) {
            "front", "forward", "f" -> facing
            "back", "backward", "b" -> facing.opposite
            "left", "l" -> facing.counterClockWise
            "right", "r" -> facing.clockWise
            "up", "top", "u" -> Direction.UP
            "down", "bottom", "d" -> Direction.DOWN
            else -> facing
        }
    }
    
    private fun tryMove(direction: Direction): Boolean {
        if (!consumeEnergy(ENERGY_COST_MOVE)) return false
        
        val level = world ?: return false
        val targetPos = position.relative(direction)
        
        // Check if target is air or passable
        val targetState = level.getBlockState(targetPos)
        if (!targetState.isAir && targetState.isSolid) {
            return false
        }
        
        position = targetPos
        return true
    }
    
    private fun consumeEnergy(amount: Double): Boolean {
        if (energy >= amount) {
            energy -= amount
            return true
        }
        return false
    }
    
    private fun inventorySize(): Int = 16 // Base inventory size
    
    private fun getStackSize(slot: Int): Int {
        // Would check actual inventory
        return 0
    }
    
    private fun compareSlots(slot1: Int, slot2: Int): Boolean {
        // Would compare item stacks
        return false
    }
    
    private fun transferItems(from: Int, to: Int, count: Int): Boolean {
        // Would transfer items between slots
        return true
    }
    
    private fun trySwing(direction: Direction): Pair<Boolean, String?> {
        val level = world ?: return Pair(false, "no world")
        val targetPos = position.relative(direction)
        val state = level.getBlockState(targetPos)
        
        if (state.isAir) {
            // Check for entities
            return Pair(false, "air")
        }
        
        // Would break block or attack entity
        return Pair(true, "block")
    }
    
    private fun tryUse(direction: Direction, sneaky: Boolean): Pair<Boolean, String?> {
        // Would use item/interact with block
        return Pair(true, "item_used")
    }
    
    private fun tryPlace(direction: Direction, sneaky: Boolean): Pair<Boolean, String?> {
        // Would place block from inventory
        return Pair(true, "placed")
    }
    
    private fun tryDrop(direction: Direction, count: Int): Boolean {
        // Would drop items
        return true
    }
    
    private fun trySuck(direction: Direction, count: Int): Boolean {
        // Would pick up items
        return true
    }
    
    private fun detectBlock(direction: Direction): Pair<Boolean, String> {
        val level = world ?: return Pair(false, "air")
        val targetPos = position.relative(direction)
        val state = level.getBlockState(targetPos)
        
        if (state.isAir) {
            return Pair(false, "air")
        }
        
        // Check if it's a liquid
        if (!state.fluidState.isEmpty) {
            return Pair(true, "liquid")
        }
        
        // Check if it's replaceable
        if (state.canBeReplaced()) {
            return Pair(true, "replaceable")
        }
        
        // Check if it's passable
        if (!state.isSolid) {
            return Pair(true, "passable")
        }
        
        // TODO: Check for entities
        return Pair(true, "solid")
    }
    
    private fun compareBlock(direction: Direction): Boolean {
        // Would compare block to selected item
        return false
    }
    
    fun addEnergy(amount: Double): Double {
        val toAdd = amount.coerceIn(0.0, maxEnergy - energy)
        energy += toAdd
        return toAdd
    }
    
    fun getPosition(): BlockPos = position
}
