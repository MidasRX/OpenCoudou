package li.cil.oc.common.event

import li.cil.oc.OpenComputers
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.capabilities.Capabilities
import org.slf4j.LoggerFactory

/**
 * Provides world interaction utilities for robots and drones.
 * Handles block breaking, placing, entity interaction, etc.
 */
object WorldInteractionHandler {
    
    private val logger = LoggerFactory.getLogger("OpenComputers")
    
    // ========================================
    // Block Interaction
    // ========================================
    
    /**
     * Break a block at the given position.
     * Returns the drops from the block.
     */
    fun breakBlock(level: Level, pos: BlockPos, tool: ItemStack, fakePlayer: Player?): List<ItemStack> {
        if (level.isClientSide) return emptyList()
        val serverLevel = level as ServerLevel
        
        val state = level.getBlockState(pos)
        if (state.isAir) return emptyList()
        
        // Check if block can be broken
        val hardness = state.getDestroySpeed(level, pos)
        if (hardness < 0) return emptyList() // Unbreakable (like bedrock)
        
        // Get drops before breaking
        val drops = Block.getDrops(state, serverLevel, pos, level.getBlockEntity(pos), fakePlayer, tool)
        
        // Break the block
        level.removeBlock(pos, false)
        
        return drops
    }
    
    /**
     * Place a block at the given position.
     * Returns true if successful.
     */
    fun placeBlock(level: Level, pos: BlockPos, state: BlockState, fakePlayer: Player?): Boolean {
        if (level.isClientSide) return false
        
        // Check if position is valid for placement
        val currentState = level.getBlockState(pos)
        if (!currentState.canBeReplaced()) return false
        
        // Check for entities in the way
        val entities = level.getEntities(null, AABB(pos))
        if (entities.any { it !is ItemEntity }) return false
        
        // Place the block
        return level.setBlock(pos, state, Block.UPDATE_ALL)
    }
    
    /**
     * Use/activate a block at the given position.
     * Returns true if interaction was successful.
     */
    fun useBlock(level: Level, pos: BlockPos, direction: Direction, fakePlayer: Player): Boolean {
        if (level.isClientSide) return false
        
        val state = level.getBlockState(pos)
        val hitResult = BlockHitResult(
            Vec3.atCenterOf(pos),
            direction.opposite,
            pos,
            false
        )
        
        // Try to use the block
        val result = state.useWithoutItem(level, fakePlayer, hitResult)
        return result.consumesAction()
    }
    
    /**
     * Detect if there's a block at the given position.
     */
    fun detectBlock(level: Level, pos: BlockPos): BlockDetectionResult {
        val state = level.getBlockState(pos)
        
        if (state.isAir) {
            return BlockDetectionResult(false, null, null, 0f)
        }
        
        val hardness = state.getDestroySpeed(level, pos)
        val be = level.getBlockEntity(pos)
        
        return BlockDetectionResult(
            hasBlock = true,
            blockState = state,
            blockEntity = be,
            hardness = hardness
        )
    }
    
    data class BlockDetectionResult(
        val hasBlock: Boolean,
        val blockState: BlockState?,
        val blockEntity: BlockEntity?,
        val hardness: Float
    )
    
    // ========================================
    // Entity Interaction
    // ========================================
    
    /**
     * Get entities in a bounding box.
     */
    fun getEntitiesInBox(level: Level, box: AABB, filter: (Entity) -> Boolean = { true }): List<Entity> {
        return level.getEntities(null, box).filter(filter)
    }
    
    /**
     * Get item entities that can be picked up.
     */
    fun getPickupableItems(level: Level, box: AABB, maxCount: Int): List<ItemEntity> {
        return level.getEntities(null, box)
            .filterIsInstance<ItemEntity>()
            .filter { !it.item.isEmpty }
            .take(maxCount)
    }
    
    /**
     * Pick up items in a direction.
     * Returns the items that were picked up.
     */
    fun suckItems(level: Level, pos: BlockPos, direction: Direction, maxCount: Int): List<ItemStack> {
        val targetPos = pos.relative(direction)
        val box = AABB(targetPos).inflate(0.5)
        
        val items = getPickupableItems(level, box, maxCount)
        val collected = mutableListOf<ItemStack>()
        var remaining = maxCount
        
        for (entity in items) {
            if (remaining <= 0) break
            
            val stack = entity.item
            val toTake = minOf(remaining, stack.count)
            
            collected.add(stack.copyWithCount(toTake))
            
            if (toTake >= stack.count) {
                entity.discard()
            } else {
                stack.shrink(toTake)
            }
            
            remaining -= toTake
        }
        
        return collected
    }
    
    /**
     * Drop items in a direction.
     * Returns the number of items actually dropped.
     */
    fun dropItems(level: Level, pos: BlockPos, direction: Direction, stack: ItemStack): Int {
        if (stack.isEmpty) return 0
        
        val targetPos = pos.relative(direction)
        val centerX = targetPos.x + 0.5
        val centerY = targetPos.y + 0.5
        val centerZ = targetPos.z + 0.5
        
        val entity = ItemEntity(level, centerX, centerY, centerZ, stack.copy())
        entity.setPickUpDelay(10) // Brief pickup delay
        
        level.addFreshEntity(entity)
        
        return stack.count
    }
    
    // ========================================
    // Inventory Interaction
    // ========================================
    
    /**
     * Transfer items from one inventory to another.
     */
    fun transferItems(
        level: Level,
        fromPos: BlockPos,
        fromSide: Direction?,
        toPos: BlockPos,
        toSide: Direction?,
        count: Int,
        slot: Int? = null
    ): Int {
        val fromHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, fromPos, fromSide)
        val toHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, toPos, toSide)
        
        if (fromHandler == null || toHandler == null) return 0
        
        var transferred = 0
        val startSlot = slot ?: 0
        val endSlot = slot?.let { it + 1 } ?: fromHandler.slots
        
        for (i in startSlot until endSlot) {
            if (transferred >= count) break
            
            val extracted = fromHandler.extractItem(i, count - transferred, true)
            if (extracted.isEmpty) continue
            
            // Try to insert into destination
            var remaining = extracted.copy()
            for (j in 0 until toHandler.slots) {
                remaining = toHandler.insertItem(j, remaining, false)
                if (remaining.isEmpty) break
            }
            
            // Actually extract what was inserted
            val actuallyInserted = extracted.count - remaining.count
            if (actuallyInserted > 0) {
                fromHandler.extractItem(i, actuallyInserted, false)
                transferred += actuallyInserted
            }
        }
        
        return transferred
    }
    
    // ========================================
    // Fluid Interaction
    // ========================================
    
    /**
     * Drain fluid from a tank.
     * Returns the amount drained.
     */
    fun drainFluid(level: Level, pos: BlockPos, side: Direction?, maxAmount: Int): Int {
        val handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side)
            ?: return 0
        
        // Simulate drain to get amount
        val simulated = handler.drain(maxAmount, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE)
        if (simulated.isEmpty) return 0
        
        // Actually drain
        val drained = handler.drain(maxAmount, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)
        return drained.amount
    }
    
    /**
     * Fill fluid to a tank.
     * Returns the amount filled.
     */
    fun fillFluid(level: Level, pos: BlockPos, side: Direction?, maxAmount: Int): Int {
        val handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side)
            ?: return 0
        
        // Would need proper FluidStack to fill
        return 0
    }
    
    // ========================================
    // Navigation Helpers
    // ========================================
    
    /**
     * Check if a position is safe for a robot to move to.
     */
    fun canMoveTo(level: Level, pos: BlockPos, checkEntities: Boolean = true): Boolean {
        val state = level.getBlockState(pos)
        
        // Check if block is passable
        if (!state.isAir && state.getCollisionShape(level, pos).isEmpty.not()) {
            return false
        }
        
        // Check for blocking entities
        if (checkEntities) {
            val box = AABB(pos)
            val entities = level.getEntities(null, box)
            if (entities.any { it !is ItemEntity && it.isPickable }) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Get the positions a robot can move to from current position.
     */
    fun getValidMoveTargets(level: Level, pos: BlockPos): List<BlockPos> {
        val targets = mutableListOf<BlockPos>()
        
        for (dir in Direction.entries) {
            val target = pos.relative(dir)
            if (canMoveTo(level, target)) {
                targets.add(target)
            }
        }
        
        return targets
    }
    
    /**
     * Find a path from start to end (simple A* implementation).
     */
    fun findPath(level: Level, start: BlockPos, end: BlockPos, maxDistance: Int): List<BlockPos>? {
        if (start == end) return listOf(start)
        if (start.distManhattan(end) > maxDistance) return null
        
        // Simple BFS pathfinding
        val visited = mutableSetOf<BlockPos>()
        val queue = ArrayDeque<Pair<BlockPos, List<BlockPos>>>()
        queue.add(start to listOf(start))
        
        while (queue.isNotEmpty()) {
            val (current, path) = queue.removeFirst()
            
            if (current == end) return path
            if (current in visited) continue
            if (path.size > maxDistance) continue
            
            visited.add(current)
            
            for (next in getValidMoveTargets(level, current)) {
                if (next !in visited) {
                    queue.add(next to (path + next))
                }
            }
        }
        
        return null // No path found
    }
}
