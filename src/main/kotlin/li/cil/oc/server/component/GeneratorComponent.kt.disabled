package li.cil.oc.server.component

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * Generator upgrade - burns fuel to produce energy.
 * Accepts standard furnace fuels.
 */
class GeneratorComponent : AbstractComponent("generator") {
    
    companion object {
        const val ENERGY_PER_COAL = 6400.0
        const val ENERGY_PER_CHARCOAL = 4800.0
        const val ENERGY_PER_WOOD = 1200.0
        const val ENERGY_PER_STICK = 400.0
        const val ENERGY_PER_BLAZE_ROD = 9600.0
        const val ENERGY_PER_LAVA = 64000.0
        
        const val GENERATION_RATE = 80.0 // Energy per tick
    }
    
    // Fuel buffer
    private var bufferedEnergy: Double = 0.0
    private var fuelItem: ItemStack = ItemStack.EMPTY
    private var burnTimeRemaining: Int = 0
    
    // Inventory slot for fuel
    private var fuelSlot: ItemStack = ItemStack.EMPTY
    
    init {
        registerMethod("count", true, "count():number -- Get fuel item count") { _ ->
            arrayOf(fuelSlot.count)
        }
        
        registerMethod("insert", false, "insert([count:number]):boolean,number -- Insert fuel from robot/drone") { args ->
            val count = (args.getOrNull(0) as? Number)?.toInt() ?: 64
            // Would take items from robot inventory
            arrayOf(true, 0)
        }
        
        registerMethod("remove", false, "remove([count:number]):boolean,number -- Remove unburned fuel") { args ->
            val count = (args.getOrNull(0) as? Number)?.toInt() ?: 64
            if (!fuelSlot.isEmpty) {
                val toRemove = minOf(count, fuelSlot.count)
                val removed = fuelSlot.copyWithCount(toRemove)
                fuelSlot.shrink(toRemove)
                return@registerMethod arrayOf(true, toRemove)
            }
            arrayOf(false, 0)
        }
    }
    
    /**
     * Generate energy from fuel.
     * @return Energy generated this tick
     */
    fun tick(): Double {
        if (burnTimeRemaining > 0) {
            burnTimeRemaining--
            return GENERATION_RATE
        }
        
        // Try to burn more fuel
        if (!fuelSlot.isEmpty) {
            val burnTime = getBurnTime(fuelSlot)
            if (burnTime > 0) {
                burnTimeRemaining = burnTime
                fuelSlot.shrink(1)
                return GENERATION_RATE
            }
        }
        
        return 0.0
    }
    
    fun insertFuel(stack: ItemStack): Int {
        if (getBurnTime(stack) <= 0) return 0
        
        if (fuelSlot.isEmpty) {
            fuelSlot = stack.copy()
            return stack.count
        } else if (ItemStack.isSameItemSameComponents(fuelSlot, stack)) {
            val space = fuelSlot.maxStackSize - fuelSlot.count
            val toAdd = minOf(space, stack.count)
            fuelSlot.grow(toAdd)
            return toAdd
        }
        return 0
    }
    
    private fun getBurnTime(stack: ItemStack): Int {
        return when {
            stack.`is`(Items.COAL) -> (ENERGY_PER_COAL / GENERATION_RATE).toInt()
            stack.`is`(Items.CHARCOAL) -> (ENERGY_PER_CHARCOAL / GENERATION_RATE).toInt()
            stack.`is`(Items.OAK_LOG) || stack.`is`(Items.BIRCH_LOG) || 
            stack.`is`(Items.SPRUCE_LOG) || stack.`is`(Items.JUNGLE_LOG) ||
            stack.`is`(Items.ACACIA_LOG) || stack.`is`(Items.DARK_OAK_LOG) ||
            stack.`is`(Items.MANGROVE_LOG) || stack.`is`(Items.CHERRY_LOG) -> 
                (ENERGY_PER_WOOD / GENERATION_RATE).toInt()
            stack.`is`(Items.STICK) -> (ENERGY_PER_STICK / GENERATION_RATE).toInt()
            stack.`is`(Items.BLAZE_ROD) -> (ENERGY_PER_BLAZE_ROD / GENERATION_RATE).toInt()
            else -> 0
        }
    }
    
    fun isBurning(): Boolean = burnTimeRemaining > 0
}
