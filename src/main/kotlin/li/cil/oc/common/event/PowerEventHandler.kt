package li.cil.oc.common.event

import li.cil.oc.OpenComputers
import li.cil.oc.common.blockentity.CaseBlockEntity
import li.cil.oc.common.blockentity.ChargerBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.IEnergyStorage
import net.neoforged.neoforge.event.tick.LevelTickEvent
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles power network events and energy distribution.
 */
@EventBusSubscriber(modid = OpenComputers.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
object PowerEventHandler {
    
    private val logger = LoggerFactory.getLogger("OpenComputers")
    
    // Power networks indexed by root position
    private val powerNetworks = ConcurrentHashMap<BlockPos, PowerNetwork>()
    
    // Power consumers that need updates
    private val powerConsumers = ConcurrentHashMap<BlockPos, PowerConsumer>()
    
    data class PowerNetwork(
        val root: BlockPos,
        val members: MutableSet<BlockPos> = mutableSetOf(),
        var totalCapacity: Long = 0,
        var totalStored: Long = 0,
        var isDirty: Boolean = true
    )
    
    data class PowerConsumer(
        val pos: BlockPos,
        val level: Level,
        val consumption: Int,
        var lastUpdate: Long = 0
    )
    
    @SubscribeEvent
    @JvmStatic
    fun onLevelTick(event: LevelTickEvent.Post) {
        val level = event.level
        if (level.isClientSide) return
        
        val gameTime = level.gameTime
        
        // Update power distribution every 10 ticks
        if (gameTime % 10 == 0L) {
            updatePowerDistribution(level)
        }
        
        // Update consumers every tick
        updateConsumers(level, gameTime)
    }
    
    private fun updatePowerDistribution(level: Level) {
        // Rebuild dirty networks
        powerNetworks.values
            .filter { it.isDirty }
            .forEach { network ->
                rebuildNetwork(level, network)
            }
        
        // Distribute power within networks
        for (network in powerNetworks.values) {
            if (network.members.isEmpty()) continue
            
            // Calculate total available and needed power
            var available = 0L
            var needed = 0L
            
            for (pos in network.members) {
                val handler = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null) ?: continue
                val be = level.getBlockEntity(pos)
                when (be) {
                    is ChargerBlockEntity -> {
                        available += handler.energyStored.toLong()
                    }
                    is CaseBlockEntity -> {
                        needed += (handler.maxEnergyStored - handler.energyStored).toLong()
                    }
                }
            }
            
            // Distribute if there's both supply and demand
            if (available > 0 && needed > 0) {
                distributepower(level, network)
            }
        }
    }
    
    private fun rebuildNetwork(level: Level, network: PowerNetwork) {
        network.members.clear()
        network.totalCapacity = 0
        network.totalStored = 0
        
        // BFS to find connected power blocks
        val visited = mutableSetOf<BlockPos>()
        val queue = ArrayDeque<BlockPos>()
        queue.add(network.root)
        
        while (queue.isNotEmpty()) {
            val pos = queue.removeFirst()
            if (pos in visited) continue
            visited.add(pos)
            
            val be = level.getBlockEntity(pos)
            
            // Check if this is a power-capable block
            val handler = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null)
            if (handler != null) {
                network.members.add(pos)
                network.totalCapacity += handler.maxEnergyStored
                network.totalStored += handler.energyStored
                
                // Check neighbors for cables
                for (dir in Direction.entries) {
                    val neighbor = pos.relative(dir)
                    if (neighbor !in visited && isPowerConnector(level, neighbor)) {
                        queue.add(neighbor)
                    }
                }
            }
        }
        
        network.isDirty = false
    }
    
    private fun isPowerConnector(level: Level, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        // Check if block is a cable or power block
        return state.block.descriptionId.contains("cable") ||
               level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null) != null
    }
    
    private fun distributepower(level: Level, network: PowerNetwork) {
        // Collect sources and sinks
        val sources = mutableListOf<Pair<BlockPos, Int>>()
        val sinks = mutableListOf<Pair<BlockPos, Int>>()
        
        for (pos in network.members) {
            val handler = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null) ?: continue
            
            if (handler.energyStored > 0 && handler.canExtract()) {
                sources.add(pos to handler.energyStored)
            }
            if (handler.energyStored < handler.maxEnergyStored && handler.canReceive()) {
                sinks.add(pos to (handler.maxEnergyStored - handler.energyStored))
            }
        }
        
        // Transfer power from sources to sinks
        for ((sourcePos, available) in sources) {
            if (sinks.isEmpty()) break
            
            val sourceHandler = level.getCapability(Capabilities.EnergyStorage.BLOCK, sourcePos, null) ?: continue
            
            for ((sinkPos, needed) in sinks) {
                val sinkHandler = level.getCapability(Capabilities.EnergyStorage.BLOCK, sinkPos, null) ?: continue
                
                val transfer = minOf(available, needed, 1000) // Max 1000 per tick per connection
                if (transfer > 0) {
                    val extracted = sourceHandler.extractEnergy(transfer, false)
                    val accepted = sinkHandler.receiveEnergy(extracted, false)
                    
                    if (accepted < extracted) {
                        // Return excess to source
                        sourceHandler.receiveEnergy(extracted - accepted, false)
                    }
                }
            }
        }
    }
    
    private fun updateConsumers(level: Level, gameTime: Long) {
        val iterator = powerConsumers.entries.iterator()
        
        while (iterator.hasNext()) {
            val (pos, consumer) = iterator.next()
            
            if (consumer.level != level) continue
            
            val be = level.getBlockEntity(pos)
            if (be == null) {
                iterator.remove()
                continue
            }
            
            // Consume power for running machines
            val handler = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null)
            if (handler != null && handler.energyStored >= consumer.consumption) {
                handler.extractEnergy(consumer.consumption, false)
            }
            
            consumer.lastUpdate = gameTime
        }
    }
    
    /**
     * Register a power consumer.
     */
    fun registerConsumer(level: Level, pos: BlockPos, consumption: Int) {
        powerConsumers[pos] = PowerConsumer(pos, level, consumption)
    }
    
    /**
     * Unregister a power consumer.
     */
    fun unregisterConsumer(pos: BlockPos) {
        powerConsumers.remove(pos)
    }
    
    /**
     * Mark a network as needing rebuild.
     */
    fun markNetworkDirty(pos: BlockPos) {
        powerNetworks.values
            .filter { pos in it.members || pos == it.root }
            .forEach { it.isDirty = true }
    }
    
    /**
     * Create or join a power network.
     */
    fun joinNetwork(level: Level, pos: BlockPos) {
        // Check if already in a network
        val existing = powerNetworks.values.find { pos in it.members }
        if (existing != null) {
            existing.isDirty = true
            return
        }
        
        // Check for adjacent networks
        for (dir in Direction.entries) {
            val neighbor = pos.relative(dir)
            val neighborNetwork = powerNetworks.values.find { neighbor in it.members }
            if (neighborNetwork != null) {
                neighborNetwork.members.add(pos)
                neighborNetwork.isDirty = true
                return
            }
        }
        
        // Create new network
        powerNetworks[pos] = PowerNetwork(pos).apply {
            members.add(pos)
        }
    }
    
    /**
     * Leave a power network.
     */
    fun leaveNetwork(pos: BlockPos) {
        val network = powerNetworks.values.find { pos in it.members }
        if (network != null) {
            network.members.remove(pos)
            network.isDirty = true
            
            if (network.members.isEmpty()) {
                powerNetworks.remove(network.root)
            }
        }
    }
}
