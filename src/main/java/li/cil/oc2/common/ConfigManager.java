/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common;

import li.cil.oc2.api.API;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.config.client.ClientSpec;
import li.cil.oc2.common.config.common.CommonSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = API.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ConfigManager {
    @SubscribeEvent
    public static void handleModConfigEvent(final ModConfigEvent event) {
        final ModConfig.Type config = event.getConfig().getType();
        if (config == ModConfig.Type.CLIENT) {
            ClientSpec.loadValues();
        }
        else {
            CommonSpec.loadValues();
            System.out.println(Config.captureInputMode);
        }
    }
}
