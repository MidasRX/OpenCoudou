package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler

/**
 * Inventory Controller upgrade component - provides detailed inventory inspection.
 * Allows robots and drones to analyze items in detail and manage internal inventory.
 */
class InventoryControllerUpgradeComponent : AbstractComponent("inventory_controller") {
    
    var controllerPos: BlockPos = BlockPos.ZERO
    var level: Level? = null
    
    init {
        registerMethod("getInventorySize", true, "getInventorySize(side:number):number -- Get inventory size on side") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val handler = getInventoryHandler(side)
            arrayOf(handler?.slots ?: 0)
        }
        
        registerMethod("getStackInSlot", true, "getStackInSlot(side:number,slot:number):table -- Get detailed stack info") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val slot = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
            
            val handler = getInventoryHandler(side) ?: return@registerMethod arrayOf(null)
            if (slot < 0 || slot >= handler.slots) return@registerMethod arrayOf(null)
            
            val stack = handler.getStackInSlot(slot)
            if (stack.isEmpty) return@registerMethod arrayOf(null)
            
            // Detailed item info
            arrayOf(mapOf(
                "name" to stack.item.descriptionId,
                "label" to stack.displayName.string,
                "size" to stack.count,
                "maxSize" to stack.maxStackSize,
                "damage" to stack.damageValue,
                "maxDamage" to stack.maxDamage,
                "durability" to if (stack.maxDamage > 0) 1.0 - (stack.damageValue.toDouble() / stack.maxDamage) else 1.0,
                "hasComponents" to !stack.components.isEmpty,
                "slot" to (slot + 1)
            ))
        }
        
        registerMethod("getAllStacks", true, "getAllStacks(side:number):table -- Get all stacks in inventory") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            
            val handler = getInventoryHandler(side) ?: return@registerMethod arrayOf(null)
            
            val stacks = mutableListOf<Map<String, Any?>>()
            for (i in 0 until handler.slots) {
                val stack = handler.getStackInSlot(i)
                stacks.add(if (stack.isEmpty) {
                    mapOf("slot" to (i + 1))
                } else {
                    mapOf(
                        "slot" to (i + 1),
                        "name" to stack.item.descriptionId,
                        "label" to stack.displayName.string,
                        "size" to stack.count,
                        "maxSize" to stack.maxStackSize
                    )
                })
            }
            arrayOf(stacks)
        }
        
        registerMethod("getSlotStackSize", true, "getSlotStackSize(side:number,slot:number):number -- Get stack count in slot") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val slot = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
            
            val handler = getInventoryHandler(side) ?: return@registerMethod arrayOf(0)
            if (slot < 0 || slot >= handler.slots) return@registerMethod arrayOf(0)
            
            arrayOf(handler.getStackInSlot(slot).count)
        }
        
        registerMethod("getSlotMaxStackSize", true, "getSlotMaxStackSize(side:number,slot:number):number -- Get max stack size") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val slot = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
            
            val handler = getInventoryHandler(side) ?: return@registerMethod arrayOf(0)
            if (slot < 0 || slot >= handler.slots) return@registerMethod arrayOf(0)
            
            arrayOf(handler.getSlotLimit(slot))
        }
        
        registerMethod("compareStacks", true, "compareStacks(side:number,slotA:number,slotB:number):boolean -- Compare two stacks") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val slotA = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
            val slotB = (args.getOrNull(2) as? Number)?.toInt()?.minus(1) ?: 0
            
            val handler = getInventoryHandler(side) ?: return@registerMethod arrayOf(false)
            if (slotA < 0 || slotA >= handler.slots) return@registerMethod arrayOf(false)
            if (slotB < 0 || slotB >= handler.slots) return@registerMethod arrayOf(false)
            
            val stackA = handler.getStackInSlot(slotA)
            val stackB = handler.getStackInSlot(slotB)
            
            // Compare item type (ignoring count and NBT)
            arrayOf(stackA.item == stackB.item)
        }
        
        registerMethod("areStacksEquivalent", true, "areStacksEquivalent(side:number,slotA:number,slotB:number,checkNBT:boolean):boolean -- Compare stacks with optional NBT check") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val slotA = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
            val slotB = (args.getOrNull(2) as? Number)?.toInt()?.minus(1) ?: 0
            val checkNBT = args.getOrNull(3) as? Boolean ?: false
            
            val handler = getInventoryHandler(side) ?: return@registerMethod arrayOf(false)
            if (slotA < 0 || slotA >= handler.slots) return@registerMethod arrayOf(false)
            if (slotB < 0 || slotB >= handler.slots) return@registerMethod arrayOf(false)
            
            val stackA = handler.getStackInSlot(slotA)
            val stackB = handler.getStackInSlot(slotB)
            
            val sameItem = stackA.item == stackB.item && stackA.damageValue == stackB.damageValue
            if (!sameItem) return@registerMethod arrayOf(false)
            
            if (checkNBT) {
                return@registerMethod arrayOf(net.minecraft.world.item.ItemStack.isSameItemSameComponents(stackA, stackB))
            }
            
            arrayOf(true)
        }
        
        registerMethod("store", false, "store(side:number,slot:number,dbAddress:string,dbSlot:number):boolean -- Store item in database") { args ->
            // Would store item fingerprint in database component
            arrayOf(false, "requires database component")
        }
        
        registerMethod("compareStackToDatabase", true, "compareStackToDatabase(side:number,slot:number,dbAddress:string,dbSlot:number,checkNBT:boolean):boolean -- Compare stack to database entry") { args ->
            // Would compare to database component
            arrayOf(false, "requires database component")
        }
        
        registerMethod("getItemCount", true, "getItemCount(side:number,itemName:string):number -- Count items matching name") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val itemName = args.getOrNull(1)?.toString() ?: ""
            
            val handler = getInventoryHandler(side) ?: return@registerMethod arrayOf(0)
            
            var count = 0
            for (i in 0 until handler.slots) {
                val stack = handler.getStackInSlot(i)
                if (!stack.isEmpty && stack.item.descriptionId.contains(itemName)) {
                    count += stack.count
                }
            }
            arrayOf(count)
        }
        
        registerMethod("dropIntoSlot", false, "dropIntoSlot(side:number,slot:number,count:number):number -- Drop items into specific slot") { args ->
            // Would require robot/drone context
            arrayOf(0, "requires robot context")
        }
        
        registerMethod("suckFromSlot", false, "suckFromSlot(side:number,slot:number,count:number):number -- Suck items from specific slot") { args ->
            // Would require robot/drone context  
            arrayOf(0, "requires robot context")
        }
    }
    
    private fun getInventoryHandler(side: Int): IItemHandler? {
        val world = level ?: return null
        val direction = when (side) {
            0 -> Direction.DOWN
            1 -> Direction.UP
            2 -> Direction.NORTH
            3 -> Direction.SOUTH
            4 -> Direction.WEST
            else -> Direction.EAST
        }
        
        val targetPos = controllerPos.relative(direction)
        return world.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, direction.opposite)
    }
}
