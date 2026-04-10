package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level

/**
 * Chunkloader upgrade - keeps chunks loaded around the robot/computer.
 * Requires energy to maintain chunk loading.
 */
class ChunkloaderComponent : AbstractComponent("chunkloader") {
    
    companion object {
        const val ENERGY_COST_PER_TICK = 0.25
    }
    
    private var world: Level? = null
    private var position: BlockPos = BlockPos.ZERO
    private var isActive: Boolean = false
    private var loadedChunks = mutableSetOf<ChunkPos>()
    
    init {
        registerMethod("isActive", true, "isActive():boolean -- Check if chunkloader is active") { _ ->
            arrayOf(isActive)
        }
        
        registerMethod("setActive", false, "setActive(enabled:boolean):boolean -- Enable/disable chunkloader") { args ->
            val enabled = args.getOrNull(0) as? Boolean ?: return@registerMethod arrayOf(isActive)
            val oldState = isActive
            
            if (enabled != isActive) {
                isActive = enabled
                if (enabled) {
                    loadChunks()
                } else {
                    unloadChunks()
                }
            }
            
            arrayOf(oldState)
        }
    }
    
    fun setWorld(level: Level?, pos: BlockPos) {
        this.world = level
        this.position = pos
    }
    
    fun tick(energyAvailable: Double): Double {
        if (!isActive) return 0.0
        
        // Check if we can afford to keep chunks loaded
        if (energyAvailable < ENERGY_COST_PER_TICK) {
            isActive = false
            unloadChunks()
            return 0.0
        }
        
        // Refresh chunk tickets
        loadChunks()
        
        return ENERGY_COST_PER_TICK
    }
    
    private fun loadChunks() {
        val level = world as? ServerLevel ?: return
        val chunkPos = ChunkPos(position)
        
        // Force load the current chunk and adjacent chunks (3x3 area)
        for (dx in -1..1) {
            for (dz in -1..1) {
                val cp = ChunkPos(chunkPos.x + dx, chunkPos.z + dz)
                if (cp !in loadedChunks) {
                    level.setChunkForced(cp.x, cp.z, true)
                    loadedChunks.add(cp)
                }
            }
        }
    }
    
    private fun unloadChunks() {
        val level = world as? ServerLevel ?: return
        
        for (cp in loadedChunks) {
            level.setChunkForced(cp.x, cp.z, false)
        }
        loadedChunks.clear()
    }
    
    fun onRemoved() {
        unloadChunks()
    }
}
