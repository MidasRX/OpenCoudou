package li.cil.oc.client

import li.cil.oc.OpenComputers
import li.cil.oc.client.gui.*
import li.cil.oc.client.renderer.ScreenRenderer
import li.cil.oc.common.init.ModBlockEntities
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
    }

    @SubscribeEvent
    @JvmStatic
    fun registerBlockEntityRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerBlockEntityRenderer(ModBlockEntities.SCREEN.get(), ::ScreenRenderer)
    }
}
