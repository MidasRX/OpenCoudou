package li.cil.oc.common.event

import li.cil.oc.OpenComputers
import li.cil.oc.common.blockentity.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.core.BlockPos
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.level.BlockEvent
import net.neoforged.neoforge.event.tick.LevelTickEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent

/**
 * Handles game events for OpenComputers.
 */
@EventBusSubscriber(modid = OpenComputers.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
object GameEventHandler {
    
    // Track running machines for power updates
    private val tickingMachines = mutableSetOf<BlockPos>()
    
    @SubscribeEvent
    @JvmStatic
    fun onServerTick(event: ServerTickEvent.Pre) {
        // Process network updates, power distribution, etc.
    }
    
    @SubscribeEvent
    @JvmStatic
    fun onLevelTick(event: LevelTickEvent.Post) {
        // Update components that need per-level tick processing
        val level = event.level
        if (level.isClientSide) return
        
        // Process power network updates
        processPowerNetwork(level)
    }
    
    @SubscribeEvent
    @JvmStatic
    fun onBlockBreak(event: BlockEvent.BreakEvent) {
        val level = event.level
        val pos = event.pos
        val blockEntity = level.getBlockEntity(pos)
        
        // Handle computer cleanup when case is broken
        when (blockEntity) {
            is CaseBlockEntity -> {
                // Computer will be cleaned up by block entity removal
            }
            is MicrocontrollerBlockEntity -> {
                // Microcontroller will be cleaned up by block entity removal
            }
        }
    }
    
    @SubscribeEvent
    @JvmStatic
    fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
        val level = event.level
        val pos = event.pos
        val player = event.entity
        val stack = event.itemStack
        
        // Handle Analyzer right-click on blocks
        if (stack.item.toString().contains("analyzer")) {
            handleAnalyzerUse(level, pos, player)
        }
    }
    
    private fun handleAnalyzerUse(level: Level, pos: BlockPos, player: Player) {
        val blockEntity = level.getBlockEntity(pos)
        
        // Show component info in chat
        when (blockEntity) {
            is CaseBlockEntity -> {
                val running = if (blockEntity.isPowered) "Running" else "Stopped"
                val handler = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null)
                val energy = if (handler != null) "${handler.energyStored}/${handler.maxEnergyStored} RF" else "N/A"
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Computer: $running, Energy: $energy"),
                    false
                )
            }
            is ScreenBlockEntity -> {
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Screen"),
                    false
                )
            }
            is ChargerBlockEntity -> {
                val handler = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null)
                val energy = if (handler != null) "${handler.energyStored}/${handler.maxEnergyStored} RF" else "N/A"
                player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Charger: $energy"),
                    false
                )
            }
        }
    }
    
    private fun processPowerNetwork(level: Level) {
        // Future: Implement power distribution across connected blocks
    }
    
    /**
     * Register a machine for tick processing.
     */
    fun registerMachine(pos: BlockPos) {
        tickingMachines.add(pos)
    }
    
    /**
     * Unregister a machine from tick processing.
     */
    fun unregisterMachine(pos: BlockPos) {
        tickingMachines.remove(pos)
    }
}
