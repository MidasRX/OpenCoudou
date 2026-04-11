package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.component.TransposerComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandler

/**
 * Transposer block entity - moves items and fluids between adjacent inventories/tanks.
 * Provides inventory and fluid tank access to Lua.
 */
class TransposerBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.TRANSPOSER.get(), pos, state) {
    
    private val transposer = TransposerComponent()
    
    fun getComponent(): TransposerComponent = transposer
    
    /**
     * Transfer items between adjacent inventories.
     */
    fun transferItem(
        sourceSide: Direction,
        sinkSide: Direction,
        count: Int = 64,
        sourceSlot: Int? = null,
        sinkSlot: Int? = null
    ): Int {
        val sourceHandler = getItemHandler(sourceSide) ?: return 0
        val sinkHandler = getItemHandler(sinkSide) ?: return 0
        
        var transferred = 0
        val remaining = count
        
        if (sourceSlot != null) {
            // Transfer from specific slot
            val extracted = sourceHandler.extractItem(sourceSlot, remaining, true)
            if (!extracted.isEmpty) {
                val leftover = insertItem(sinkHandler, extracted, sinkSlot)
                val actualTransfer = extracted.count - leftover.count
                if (actualTransfer > 0) {
                    sourceHandler.extractItem(sourceSlot, actualTransfer, false)
                    transferred = actualTransfer
                }
            }
        } else {
            // Transfer from any slot
            for (slot in 0 until sourceHandler.slots) {
                if (transferred >= count) break
                
                val toTransfer = count - transferred
                val extracted = sourceHandler.extractItem(slot, toTransfer, true)
                if (!extracted.isEmpty) {
                    val leftover = insertItem(sinkHandler, extracted, sinkSlot)
                    val actualTransfer = extracted.count - leftover.count
                    if (actualTransfer > 0) {
                        sourceHandler.extractItem(slot, actualTransfer, false)
                        transferred += actualTransfer
                    }
                }
            }
        }
        
        return transferred
    }
    
    private fun insertItem(handler: IItemHandler, stack: ItemStack, slot: Int?): ItemStack {
        var remaining = stack.copy()
        
        if (slot != null) {
            remaining = handler.insertItem(slot, remaining, false)
        } else {
            for (i in 0 until handler.slots) {
                remaining = handler.insertItem(i, remaining, false)
                if (remaining.isEmpty) break
            }
        }
        
        return remaining
    }
    
    /**
     * Transfer fluid between adjacent tanks.
     */
    fun transferFluid(
        sourceSide: Direction,
        sinkSide: Direction,
        amount: Int = 1000
    ): Int {
        val sourceHandler = getFluidHandler(sourceSide) ?: return 0
        val sinkHandler = getFluidHandler(sinkSide) ?: return 0
        
        val drained = sourceHandler.drain(amount, IFluidHandler.FluidAction.SIMULATE)
        if (drained.isEmpty) return 0
        
        val filled = sinkHandler.fill(drained, IFluidHandler.FluidAction.SIMULATE)
        if (filled <= 0) return 0
        
        val actualDrain = sourceHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE)
        sinkHandler.fill(actualDrain, IFluidHandler.FluidAction.EXECUTE)
        
        return actualDrain.amount
    }
    
    /**
     * Get item count in adjacent inventory.
     */
    fun getInventorySize(side: Direction): Int {
        return getItemHandler(side)?.slots ?: 0
    }
    
    /**
     * Get item in specific slot of adjacent inventory.
     */
    fun getStackInSlot(side: Direction, slot: Int): ItemStack {
        return getItemHandler(side)?.getStackInSlot(slot) ?: ItemStack.EMPTY
    }
    
    /**
     * Get fluid tank count on side.
     */
    fun getTankCount(side: Direction): Int {
        return getFluidHandler(side)?.tanks ?: 0
    }
    
    /**
     * Get fluid in specific tank of adjacent tank.
     */
    fun getFluidInTank(side: Direction, tank: Int): FluidStack {
        val handler = getFluidHandler(side) ?: return FluidStack.EMPTY
        return if (tank in 0 until handler.tanks) {
            handler.getFluidInTank(tank)
        } else FluidStack.EMPTY
    }
    
    private fun getItemHandler(side: Direction): IItemHandler? {
        val targetPos = blockPos.relative(side)
        return level?.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, side.opposite)
    }
    
    private fun getFluidHandler(side: Direction): IFluidHandler? {
        val targetPos = blockPos.relative(side)
        return level?.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, side.opposite)
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putString("transposer_address", transposer.address)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
    }
}
