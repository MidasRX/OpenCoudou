package li.cil.oc2.common.config.common;

import li.cil.oc2.common.config.Config;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.UUID;

public class AdminSpec {
    // ROOT //
    public final ForgeConfigSpec.ConfigValue<String> fakePlayerUUID;
    // NETWORK //
    public final ForgeConfigSpec.IntValue projectorAverageMaxBytesPerSecond;
    // VIRTUAL NETWORK //
    public final ForgeConfigSpec.IntValue ethernetFrameTimeToLive;
    public final ForgeConfigSpec.IntValue hubEthernetFrameTimeToLive;

    AdminSpec(ForgeConfigSpec.Builder builder) {
        fakePlayerUUID = builder.comment("The UUID that the mod will use for it's fake player")
            .define("fakePlayerUUID", "e39dd9a7-514f-4a2d-aa5e-b6030621416d");

        builder.push("network");

        projectorAverageMaxBytesPerSecond = builder.comment("The maximum number of bytes a projector will send per second on average")
            .defineInRange("projectorAverageMaxBytesPerSecond", 160*1024, 0, Integer.MAX_VALUE);

        builder.pop();

        builder.push("virtual_network");

        ethernetFrameTimeToLive = builder.comment("The time to live of an ethernet frame sent over the virtual network")
            .defineInRange("ethernetFrameTimeToLive", 12, 0, Integer.MAX_VALUE);

        hubEthernetFrameTimeToLive = builder.comment("The time to live of an ethernet frame sent over the virtual network to a hub")
            .defineInRange("hubEthernetFrameTimeToLive", 32, 0, Integer.MAX_VALUE);

        builder.pop();
    }

    public void loadValues() {
        Config.fakePlayerUUID = UUID.fromString(fakePlayerUUID.get());
        Config.projectorAverageMaxBytesPerSecond = projectorAverageMaxBytesPerSecond.get();
        Config.ethernetFrameTimeToLive = ethernetFrameTimeToLive.get();
        Config.hubEthernetFramesPerTick = ethernetFrameTimeToLive.get();
    }
}
