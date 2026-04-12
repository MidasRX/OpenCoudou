package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler

/**
 * Transposer component - moves items and fluids between inventories/tanks.
 * Can interact with adjacent inventories on all 6 sides.
 */
class TransposerComponent : AbstractComponent("transposer") {
    
    var transposerPos: BlockPos = BlockPos.ZERO
    var level: Level? = null
    
    init {
        registerMethod("getInventorySize", true, "getInventorySize(side:number):number -- Get inventory size on side") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val handler = getInventoryHandler(side)
            arrayOf(handler?.slots ?: 0)
        }
        
        registerMethod("getSlotStackSize", true, "getSlotStackSize(side:number,slot:number):number -- Get stack count in slot") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val slot = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
            
            val handler = getInventoryHandler(side) ?: return@registerMethod arrayOf(0)
            if (slot < 0 || slot >= handler.slots) return@registerMethod arrayOf(0)
            
            val stack = handler.getStackInSlot(slot)
            arrayOf(stack.count)
        }
        
        registerMethod("getSlotMaxStackSize", true, "getSlotMaxStackSize(side:number,slot:number):number -- Get max stack size in slot") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val slot = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
            
            val handler = getInventoryHandler(side) ?: return@registerMethod arrayOf(0)
            if (slot < 0 || slot >= handler.slots) return@registerMethod arrayOf(0)
            
            arrayOf(handler.getSlotLimit(slot))
        }
        
        registerMethod("getStackInSlot", true, "getStackInSlot(side:number,slot:number):table -- Get stack info in slot") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val slot = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
            
            val handler = getInventoryHandler(side) ?: return@registerMethod arrayOf(null)
            if (slot < 0 || slot >= handler.slots) return@registerMethod arrayOf(null)
            
            val stack = handler.getStackInSlot(slot)
            if (stack.isEmpty) return@registerMethod arrayOf(null)
            
            val info = mapOf(
                "name" to stack.item.descriptionId,
                "count" to stack.count,
                "maxCount" to stack.maxStackSize,
                "damage" to stack.damageValue,
                "maxDamage" to stack.maxDamage,
                "label" to stack.displayName.string
            )
            arrayOf(info)
        }
        
        registerMethod("getAllStacks", true, "getAllStacks(side:number):table -- Get all stacks in inventory") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            
            val handler = getInventoryHandler(side) ?: return@registerMethod arrayOf(null)
            
            val stacks = mutableListOf<Map<String, Any?>>()
            for (i in 0 until handler.slots) {
                val stack = handler.getStackInSlot(i)
                if (!stack.isEmpty) {
                    stacks.add(mapOf(
                        "slot" to (i + 1),
                        "name" to stack.item.descriptionId,
                        "count" to stack.count,
                        "label" to stack.displayName.string
                    ))
                }
            }
            arrayOf(stacks)
        }
        
        registerMethod("transferItem", false, "transferItem(sourceSide:number,sinkSide:number,count:number[,sourceSlot:number,sinkSlot:number]):number -- Move items") { args ->
            val sourceSide = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val sinkSide = (args.getOrNull(1) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val count = (args.getOrNull(2) as? Number)?.toInt()?.coerceIn(1, 64) ?: 1
            val sourceSlot = (args.getOrNull(3) as? Number)?.toInt()?.minus(1)
            val sinkSlot = (args.getOrNull(4) as? Number)?.toInt()?.minus(1)
            
            val sourceHandler = getInventoryHandler(sourceSide) 
                ?: return@registerMethod arrayOf(0, "no source inventory")
            val sinkHandler = getInventoryHandler(sinkSide) 
                ?: return@registerMethod arrayOf(0, "no sink inventory")
            
            var transferred = 0
            
            // Determine source slots to check
            val sourceSlots = if (sourceSlot != null && sourceSlot >= 0 && sourceSlot < sourceHandler.slots) {
                listOf(sourceSlot)
            } else {
                (0 until sourceHandler.slots).toList()
            }
            
            // Transfer items
            for (srcSlot in sourceSlots) {
                if (transferred >= count) break
                
                val available = sourceHandler.extractItem(srcSlot, count - transferred, true)
                if (available.isEmpty) continue
                
                // Try to insert into sink
                var remaining = available.copy()
                val sinkSlots = if (sinkSlot != null && sinkSlot >= 0 && sinkSlot < sinkHandler.slots) {
                    listOf(sinkSlot)
                } else {
                    (0 until sinkHandler.slots).toList()
                }
                
                for (snkSlot in sinkSlots) {
                    if (remaining.isEmpty) break
                    remaining = sinkHandler.insertItem(snkSlot, remaining, false)
                }
                
                val inserted = available.count - remaining.count
                if (inserted > 0) {
                    sourceHandler.extractItem(srcSlot, inserted, false)
                    transferred += inserted
                }
            }
            
            arrayOf(transferred)
        }
        
        registerMethod("store", false, "store(side:number,slot:number,dbAddress:string,dbSlot:number):boolean -- Store item info in database") { _ ->
            arrayOf(false, "not implemented")
        }
        
        registerMethod("compareStackToDatabase", true, "compareStackToDatabase(side:number,slot:number,dbAddress:string,dbSlot:number,checkNBT:boolean):boolean -- Compare stack to database entry") { _ ->
            arrayOf(false, "not implemented")
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
        
        val targetPos = transposerPos.relative(direction)
        return world.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, direction.opposite)
    }
    
    private fun sideToDirection(side: Int): Direction {
        return when (side) {
            0 -> Direction.DOWN
            1 -> Direction.UP
            2 -> Direction.NORTH
            3 -> Direction.SOUTH
            4 -> Direction.WEST
            else -> Direction.EAST
        }
    }
}
