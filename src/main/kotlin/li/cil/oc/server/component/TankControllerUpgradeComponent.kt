package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler

/**
 * Tank Controller upgrade component - provides fluid tank inspection and manipulation.
 * Similar to TransposerComponent but focused on fluids and with tier-based features.
 */
class TankControllerUpgradeComponent : AbstractComponent("tank_controller") {
    
    var controllerPos: BlockPos = BlockPos.ZERO
    var level: Level? = null
    
    init {
        registerMethod("getTankCount", true, "getTankCount(side:number):number -- Get number of tanks on side") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val handler = getFluidHandler(side)
            arrayOf(handler?.tanks ?: 0)
        }
        
        registerMethod("getFluidInTank", true, "getFluidInTank(side:number,tank:number):table -- Get fluid info in tank") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val tank = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
            
            val handler = getFluidHandler(side) ?: return@registerMethod arrayOf(null)
            if (tank < 0 || tank >= handler.tanks) return@registerMethod arrayOf(null)
            
            val fluid = handler.getFluidInTank(tank)
            if (fluid.isEmpty) return@registerMethod arrayOf(null)
            
            arrayOf(mapOf(
                "name" to fluid.fluid.fluidType.descriptionId,
                "amount" to fluid.amount,
                "capacity" to handler.getTankCapacity(tank),
                "label" to fluid.displayName.string
            ))
        }
        
        registerMethod("getTankCapacity", true, "getTankCapacity(side:number,tank:number):number -- Get tank capacity") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val tank = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
            
            val handler = getFluidHandler(side) ?: return@registerMethod arrayOf(0)
            if (tank < 0 || tank >= handler.tanks) return@registerMethod arrayOf(0)
            
            arrayOf(handler.getTankCapacity(tank))
        }
        
        registerMethod("getFluidInTankAmount", true, "getFluidInTankAmount(side:number,tank:number):number -- Get fluid amount in tank") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val tank = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
            
            val handler = getFluidHandler(side) ?: return@registerMethod arrayOf(0)
            if (tank < 0 || tank >= handler.tanks) return@registerMethod arrayOf(0)
            
            arrayOf(handler.getFluidInTank(tank).amount)
        }
        
        registerMethod("transferFluid", false, "transferFluid(sourceSide:number,sinkSide:number,amount:number):number,string -- Transfer fluid between tanks") { args ->
            val sourceSide = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val sinkSide = (args.getOrNull(1) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val amount = (args.getOrNull(2) as? Number)?.toInt()?.coerceIn(1, 64000) ?: 1000
            
            val sourceHandler = getFluidHandler(sourceSide)
                ?: return@registerMethod arrayOf(0, "no source tank")
            val sinkHandler = getFluidHandler(sinkSide)
                ?: return@registerMethod arrayOf(0, "no sink tank")
            
            // Drain from source
            val drained = sourceHandler.drain(amount, IFluidHandler.FluidAction.SIMULATE)
            if (drained.isEmpty) {
                return@registerMethod arrayOf(0, "no fluid to transfer")
            }
            
            // Fill to sink
            val filled = sinkHandler.fill(drained, IFluidHandler.FluidAction.SIMULATE)
            if (filled <= 0) {
                return@registerMethod arrayOf(0, "sink cannot accept fluid")
            }
            
            // Actually transfer
            val actualDrained = sourceHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE)
            val actualFilled = sinkHandler.fill(actualDrained, IFluidHandler.FluidAction.EXECUTE)
            
            arrayOf(actualFilled)
        }
        
        registerMethod("getAllFluids", true, "getAllFluids(side:number):table -- Get all fluids in tanks") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            
            val handler = getFluidHandler(side) ?: return@registerMethod arrayOf(null)
            
            val fluids = mutableListOf<Map<String, Any>>()
            for (i in 0 until handler.tanks) {
                val fluid = handler.getFluidInTank(i)
                if (!fluid.isEmpty) {
                    fluids.add(mapOf(
                        "tank" to (i + 1),
                        "name" to fluid.fluid.fluidType.descriptionId,
                        "amount" to fluid.amount,
                        "capacity" to handler.getTankCapacity(i),
                        "label" to fluid.displayName.string
                    ))
                }
            }
            arrayOf(fluids)
        }
        
        registerMethod("compareFluidTo", true, "compareFluidTo(side:number,tank:number,fluidName:string):boolean -- Compare tank fluid to name") { args ->
            val side = (args.getOrNull(0) as? Number)?.toInt()?.coerceIn(0, 5) ?: 0
            val tank = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
            val fluidName = args.getOrNull(2)?.toString() ?: ""
            
            val handler = getFluidHandler(side) ?: return@registerMethod arrayOf(false)
            if (tank < 0 || tank >= handler.tanks) return@registerMethod arrayOf(false)
            
            val fluid = handler.getFluidInTank(tank)
            arrayOf(!fluid.isEmpty && fluid.fluid.fluidType.descriptionId.contains(fluidName))
        }
    }
    
    private fun getFluidHandler(side: Int): IFluidHandler? {
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
        return world.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, direction.opposite)
    }
}
