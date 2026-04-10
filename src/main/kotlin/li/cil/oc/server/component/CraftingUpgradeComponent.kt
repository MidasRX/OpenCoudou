package li.cil.oc.server.component

import net.minecraft.core.NonNullList
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

/**
 * Crafting upgrade - allows robots to craft items from their inventory.
 * The robot's inventory acts as a 3x3 crafting grid.
 */
class CraftingUpgradeComponent : AbstractComponent("crafting") {
    
    private var world: Level? = null
    
    // Virtual inventory for robot (16 slots, first 9 are crafting grid)
    private val inventory = NonNullList.withSize(16, ItemStack.EMPTY)
    
    init {
        registerMethod("craft", false, "craft([count:number]):boolean,number -- Craft items using top-left 3x3 of inventory") { args ->
            val count = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(1, 64) ?: 64
            val result = tryCraft(count)
            arrayOf(result.first, result.second)
        }
    }
    
    fun setWorld(level: Level?) {
        this.world = level
    }
    
    /**
     * Attempt to craft items.
     * @param maxCount Maximum number of items to craft
     * @return (success, count crafted)
     */
    private fun tryCraft(maxCount: Int): Pair<Boolean, Int> {
        val level = world ?: return Pair(false, 0)
        
        // TODO: Implement actual crafting using CraftingInput and RecipeManager
        // For now, return failure - needs recipe lookup implementation
        
        return Pair(false, 0)
    }
    
    fun setSlot(slot: Int, stack: ItemStack) {
        if (slot in inventory.indices) {
            inventory[slot] = stack
        }
    }
    
    fun getSlot(slot: Int): ItemStack {
        return if (slot in inventory.indices) inventory[slot] else ItemStack.EMPTY
    }
}
