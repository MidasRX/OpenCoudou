package li.cil.oc.common

import li.cil.oc.OpenComputers
import li.cil.oc.common.init.*
// import li.cil.oc.common.config.Config  // TODO: Re-enable when config system is ready
import net.minecraft.resources.ResourceLocation
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.ModLoadingContext
import org.apache.logging.log4j.LogManager

/**
 * Common mod setup for OpenComputers.
 * Handles registration and initialization shared between client and server.
 */
@Mod(OpenComputers.MOD_ID)
class CommonSetup(modBus: IEventBus) {
    
    companion object {
        private val LOGGER = LogManager.getLogger("OpenComputers")
        
        fun id(path: String): ResourceLocation = 
            ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, path)
    }
    
    init {
        LOGGER.info("OpenComputers initializing...")
        
        // Register config - TODO: Re-enable when config system is ready
        // Config.register(ModLoadingContext.get())
        
        // Register all deferred registers to mod bus
        ModBlocks.register(modBus)
        ModItems.register(modBus)
        ModBlockEntities.register(modBus)
        ModMenus.register(modBus)
        ModCreativeTabs.register(modBus)
        ModDataComponents.register(modBus)
        ModSoundEvents.register(modBus)
        ModEntities.register(modBus)
        
        // Register setup event
        modBus.addListener(::onCommonSetup)
        
        LOGGER.info("OpenComputers registration complete")
    }
    
    private fun onCommonSetup(event: FMLCommonSetupEvent) {
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
