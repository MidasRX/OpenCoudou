package li.cil.oc.client

import li.cil.oc.OpenComputers
import li.cil.oc.client.gui.CaseScreen
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
    }

    @SubscribeEvent
    @JvmStatic
    fun registerBlockEntityRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerBlockEntityRenderer(ModBlockEntities.SCREEN.get(), ::ScreenRenderer)
    }
}
