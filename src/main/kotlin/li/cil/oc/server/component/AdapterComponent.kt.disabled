package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.Container
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandler

/**
 * Adapter component - allows computers to interact with non-OC blocks.
 * Provides a unified interface to inventories, tanks, redstone, and energy storage
 * of adjacent blocks.
 */
class AdapterComponent : AbstractComponent("adapter") {
    
    var adapterPos: BlockPos = BlockPos.ZERO
    var level: Level? = null
    
    init {
        // Inventory methods
        registerMethod("getInventorySize", true, "getInventorySize(side:number):number -- Get inventory size on side") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val handler = getInventoryHandler(side)
            arrayOf(handler?.slots ?: 0)
        }
        
        registerMethod("getStackInSlot", true, "getStackInSlot(side:number,slot:number):table -- Get stack in slot") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val slot = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
            
            val handler = getInventoryHandler(side) ?: return@registerMethod arrayOf(null)
            if (slot < 0 || slot >= handler.slots) return@registerMethod arrayOf(null)
            
            val stack = handler.getStackInSlot(slot)
            if (stack.isEmpty) return@registerMethod arrayOf(null)
            
            arrayOf(mapOf(
                "name" to stack.item.descriptionId,
                "count" to stack.count,
                "damage" to stack.damageValue,
                "maxDamage" to stack.maxDamage,
                "label" to stack.displayName.string
            ))
        }
        
        // Tank/Fluid methods
        registerMethod("getTankCount", true, "getTankCount(side:number):number -- Get number of tanks on side") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val handler = getFluidHandler(side)
            arrayOf(handler?.tanks ?: 0)
        }
        
        registerMethod("getFluidInTank", true, "getFluidInTank(side:number,tank:number):table -- Get fluid in tank") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val tank = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
            
            val handler = getFluidHandler(side) ?: return@registerMethod arrayOf(null)
            if (tank < 0 || tank >= handler.tanks) return@registerMethod arrayOf(null)
            
            val fluid = handler.getFluidInTank(tank)
            if (fluid.isEmpty) return@registerMethod arrayOf(null)
            
            arrayOf(mapOf(
                "name" to fluid.fluid.fluidType.descriptionId,
                "amount" to fluid.amount,
                "capacity" to handler.getTankCapacity(tank)
            ))
        }
        
        // Energy methods
        registerMethod("getEnergyStored", true, "getEnergyStored(side:number):number -- Get stored energy on side") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val handler = getEnergyHandler(side)
            arrayOf(handler?.energyStored ?: 0)
        }
        
        registerMethod("getMaxEnergyStored", true, "getMaxEnergyStored(side:number):number -- Get max energy capacity on side") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val handler = getEnergyHandler(side)
            arrayOf(handler?.maxEnergyStored ?: 0)
        }
        
        // Block info methods
        registerMethod("getBlockInfo", true, "getBlockInfo(side:number):table -- Get info about block on side") { args ->
            val world = level ?: return@registerMethod arrayOf(null)
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val direction = sideToDirection(side)
            val targetPos = adapterPos.relative(direction)
            
            val state = world.getBlockState(targetPos)
            val block = state.block
            
            arrayOf(mapOf(
                "name" to block.descriptionId,
                "hardness" to state.getDestroySpeed(world, targetPos),
                "harvestLevel" to 0, // Not really applicable in modern MC
                "harvestTool" to "any"
            ))
        }
        
        // Component listing
        registerMethod("getComponents", true, "getComponents():table -- List all available component types") { _ ->
            val components = mutableListOf<String>()
            
            for (side in 0..5) {
                if (getInventoryHandler(side) != null) components.add("inventory")
                if (getFluidHandler(side) != null) components.add("fluid")
                if (getEnergyHandler(side) != null) components.add("energy")
            }
            
            arrayOf(components.distinct())
        }
    }
    
    private fun getInventoryHandler(side: Int): IItemHandler? {
        val world = level ?: return null
        val direction = sideToDirection(side)
        val targetPos = adapterPos.relative(direction)
        return world.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, direction.opposite)
    }
    
    private fun getFluidHandler(side: Int): IFluidHandler? {
        val world = level ?: return null
        val direction = sideToDirection(side)
        val targetPos = adapterPos.relative(direction)
        return world.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, direction.opposite)
    }
    
    private fun getEnergyHandler(side: Int): IEnergyStorage? {
        val world = level ?: return null
        val direction = sideToDirection(side)
        val targetPos = adapterPos.relative(direction)
        return world.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, direction.opposite)
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
    
    fun setWorld(level: Level?, pos: BlockPos) {
        this.level = level
        this.adapterPos = pos
    }
}
