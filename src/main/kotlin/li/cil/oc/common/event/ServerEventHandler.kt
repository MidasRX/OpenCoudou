package li.cil.oc.common.event

import li.cil.oc.OpenComputers
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import org.slf4j.LoggerFactory

/**
 * Handles server lifecycle and player connection events.
 */
@EventBusSubscriber(modid = OpenComputers.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
object ServerEventHandler {
    
    private val logger = LoggerFactory.getLogger("OpenComputers")
    
    @SubscribeEvent
    @JvmStatic
    fun onServerStarting(event: ServerStartingEvent) {
        logger.info("OpenComputers server starting - initializing systems...")
        
        // Initialize filesystem paths
        initializeFilesystems(event.server)
        
        // Start background services
        startServices()
    }
    
    @SubscribeEvent
    @JvmStatic
    fun onServerStopping(event: ServerStoppingEvent) {
        logger.info("OpenComputers server stopping - shutting down systems...")
        
        // Shutdown all running machines gracefully
        shutdownAllMachines()
        
        // Stop background services
        stopServices()
    }
    
    @SubscribeEvent
    @JvmStatic
    fun onPlayerLoggedIn(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity
        logger.debug("Player ${player.name.string} logged in")
        
        // Send client config sync packet
        syncClientConfig(player)
    }
    
    @SubscribeEvent
    @JvmStatic
    fun onPlayerLoggedOut(event: PlayerEvent.PlayerLoggedOutEvent) {
        val player = event.entity
        logger.debug("Player ${player.name.string} logged out")
        
        // Clean up player-specific data
        cleanupPlayerData(player)
    }
    
    @SubscribeEvent
    @JvmStatic
    fun onPlayerRespawn(event: PlayerEvent.PlayerRespawnEvent) {
        val player = event.entity
        
        // Re-sync nanomachines if player had them
        syncNanomachines(player)
    }
    
    private fun initializeFilesystems(server: net.minecraft.server.MinecraftServer) {
        // Create world-specific directories for persistent storage
        try {
            val worldDir = server.serverDirectory
            val ocPath = worldDir.resolve("opencomputers")
            
            java.nio.file.Files.createDirectories(ocPath)
            java.nio.file.Files.createDirectories(ocPath.resolve("state"))
            java.nio.file.Files.createDirectories(ocPath.resolve("tmp"))
        } catch (e: Exception) {
            logger.error("Failed to create OpenComputers directories", e)
        }
    }
    
    private fun startServices() {
        // Start thread pool for machine execution
        // Start network message processing
    }
    
    private fun stopServices() {
        // Stop all services gracefully
    }
    
    private fun shutdownAllMachines() {
        // Signal all machines to shutdown
    }
    
    private fun syncClientConfig(player: net.minecraft.world.entity.player.Player) {
        // Send config packet to client
    }
    
    private fun cleanupPlayerData(player: net.minecraft.world.entity.player.Player) {
        // Remove temporary player data
    }
    
    private fun syncNanomachines(player: net.minecraft.world.entity.player.Player) {
        // Sync nanomachine state after respawn
    }
}
