package li.cil.oc.client

import li.cil.oc.OpenComputers
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.event.tick.LevelTickEvent

/**
 * Client-side event handler for sound system — port of original's event subscriptions.
 * Ticks the sound system and cleans up on world unload.
 */
@EventBusSubscriber(modid = OpenComputers.MOD_ID, value = [Dist.CLIENT])
object ClientEventHandler {

    @SubscribeEvent
    @JvmStatic
    fun onClientTick(event: LevelTickEvent.Post) {
        if (event.level == net.minecraft.client.Minecraft.getInstance().level) {
            Sound.tick()
        }
    }

    @SubscribeEvent
    @JvmStatic
    fun onLoggedOut(event: ClientPlayerNetworkEvent.LoggingOut) {
        Sound.stopAll()
    }
}
