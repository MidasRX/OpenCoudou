package li.cil.oc.common

import li.cil.oc.OpenComputers
import li.cil.oc.common.init.*
// import li.cil.oc.common.config.Config  // TODO: Re-enable when config system is ready
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.ModLoadingContext
import org.apache.logging.log4j.LogManager

/**
 * Common mod setup for OpenComputers.
 * Handles registration and initialization shared between client and server.
 * NOTE: Registration is handled by the main OpenComputers class.
 */
object CommonSetup {
    
    private val LOGGER = LogManager.getLogger("OpenComputers")
    
    fun id(path: String): ResourceLocation = 
        ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, path)
    
    fun onCommonSetup(event: FMLCommonSetupEvent) {
        event.enqueueWork {
            LOGGER.info("OpenComputers common setup...")
            
            // Initialize network handling
            // NetworkHandler.register()
            
            // Initialize component drivers
            // DriverRegistry.init()
            
            LOGGER.info("OpenComputers common setup complete")
        }
    }
}
