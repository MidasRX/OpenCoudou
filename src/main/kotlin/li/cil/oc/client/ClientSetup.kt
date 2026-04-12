package li.cil.oc.client

import li.cil.oc.OpenComputers
import li.cil.oc.client.gui.*
import li.cil.oc.client.renderer.CaseRenderer
import li.cil.oc.client.renderer.DroneModel
import li.cil.oc.client.renderer.DroneRenderer
import li.cil.oc.client.renderer.MicrocontrollerRenderer
import li.cil.oc.client.renderer.RobotModel
import li.cil.oc.client.renderer.RobotRenderer
import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.common.init.ModEntities
import li.cil.oc.common.init.ModMenus
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent

@EventBusSubscriber(modid = OpenComputers.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ClientSetup {
    @SubscribeEvent
    @JvmStatic
    fun registerMenuScreens(event: RegisterMenuScreensEvent) {
        event.register(ModMenus.CASE.get(), ::CaseScreen)
        event.register(ModMenus.ASSEMBLER.get(), ::AssemblerScreen)
        event.register(ModMenus.DISASSEMBLER.get(), ::DisassemblerScreen)
        event.register(ModMenus.RAID.get(), ::RaidScreen)
        event.register(ModMenus.CHARGER.get(), ::ChargerScreen)
        event.register(ModMenus.DISK_DRIVE.get(), ::DiskDriveScreen)
        event.register(ModMenus.PRINTER.get(), ::PrinterScreen)
        event.register(ModMenus.ADAPTER.get(), ::AdapterScreen)
        event.register(ModMenus.RACK.get(), ::RackScreen)
        event.register(ModMenus.DATABASE.get(), ::DatabaseScreen)
        event.register(ModMenus.RELAY.get(), ::RelayScreen)
        event.register(ModMenus.ROBOT.get(), ::RobotScreen)
        event.register(ModMenus.DRONE.get(), ::DroneScreen)
        event.register(ModMenus.SERVER.get(), ::ServerScreen)
        event.register(ModMenus.TABLET.get(), ::TabletScreen)
        event.register(ModMenus.SWITCH.get(), ::SwitchScreen)
    }

    @SubscribeEvent
    @JvmStatic
    fun registerBlockEntityRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        // Block entity renderers
        event.registerBlockEntityRenderer(ModBlockEntities.CASE.get(), ::CaseRenderer)
        event.registerBlockEntityRenderer(ModBlockEntities.MICROCONTROLLER.get(), ::MicrocontrollerRenderer)
        event.registerBlockEntityRenderer(ModBlockEntities.SCREEN.get()) { ctx -> 
            li.cil.oc.client.renderer.SimpleScreenRenderer(ctx)
        }
        
        // TODO: Re-enable these renderers when implementations are ready
        // event.registerBlockEntityRenderer(ModBlockEntities.SCREEN.get(), ::ScreenRenderer)
        // event.registerBlockEntityRenderer(ModBlockEntities.HOLOGRAM.get(), ::HologramRenderer)
        // event.registerBlockEntityRenderer(ModBlockEntities.PRINT.get(), ::PrintRenderer)
        // event.registerBlockEntityRenderer(ModBlockEntities.SERVER_RACK.get(), ::RackRenderer)
        // event.registerBlockEntityRenderer(ModBlockEntities.ASSEMBLER.get(), ::AssemblerRenderer)
        // event.registerBlockEntityRenderer(ModBlockEntities.DISASSEMBLER.get(), ::DisassemblerRenderer)
        // event.registerBlockEntityRenderer(ModBlockEntities.CHARGER.get(), ::ChargerRenderer)
        // event.registerBlockEntityRenderer(ModBlockEntities.CABLE.get(), ::CableRenderer)
        
        // Entity renderers
        event.registerEntityRenderer(ModEntities.DRONE.get(), ::DroneRenderer)
        event.registerEntityRenderer(ModEntities.ROBOT.get(), ::RobotRenderer)
    }
    
    @SubscribeEvent
    @JvmStatic
    fun registerLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions) {
        event.registerLayerDefinition(DroneModel.LAYER_LOCATION, DroneModel::createBodyLayer)
        event.registerLayerDefinition(RobotModel.LAYER_LOCATION, RobotModel::createBodyLayer)
    }
}
