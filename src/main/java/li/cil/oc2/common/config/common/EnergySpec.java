package li.cil.oc2.common.config.common;

import li.cil.oc2.common.config.Config;
import net.minecraftforge.common.ForgeConfigSpec;

public class EnergySpec {
    // BLOCKS //
    public final ForgeConfigSpec.DoubleValue busCableEnergyPerTick;
    public final ForgeConfigSpec.DoubleValue busInterfaceEnergyPerTick;
    public final ForgeConfigSpec.IntValue computerEnergyPerTick;
    public final ForgeConfigSpec.IntValue computerEnergyStorage;
    public final ForgeConfigSpec.IntValue chargerEnergyPerTick;
    public final ForgeConfigSpec.IntValue chargerEnergyStorage;
    public final ForgeConfigSpec.IntValue projectorEnergyPerTick;
    public final ForgeConfigSpec.IntValue projectorEnergyStorage;
    public final ForgeConfigSpec.IntValue monitorEnergyPerTick;
    public final ForgeConfigSpec.IntValue monitorEnergyStorage;
    public final ForgeConfigSpec.IntValue cardCageEnergyPerTick;
    public final ForgeConfigSpec.IntValue cardCageEnergyStorage;
    public final ForgeConfigSpec.IntValue gatewayEnergyPerPacket;
    public final ForgeConfigSpec.IntValue gatewayEnergyStorage;
    // ENTITIES //
    public final ForgeConfigSpec.IntValue robotEnergyPerTick;
    public final ForgeConfigSpec.IntValue robotEnergyStorage;
    // ITEMS //
    public final ForgeConfigSpec.DoubleValue memoryEnergyPerMegabytePerTick;
    public final ForgeConfigSpec.DoubleValue hardDriveEnergyPerMegabytePerTick;
    public final ForgeConfigSpec.DoubleValue cpuEnergyPerMegahertzPerTick;
    public final ForgeConfigSpec.IntValue redstoneInterfaceCardEnergyPerTick;
    public final ForgeConfigSpec.IntValue networkInterfaceEnergyPerTick;
    public final ForgeConfigSpec.IntValue fileImportExportCardEnergyPerTick;
    public final ForgeConfigSpec.IntValue soundCardEnergyPerTick;
    public final ForgeConfigSpec.IntValue blockOperationsModuleEnergyPerTick;
    public final ForgeConfigSpec.IntValue inventoryOperationsModuleEnergyPerTick;
    public final ForgeConfigSpec.IntValue networkTunnelEnergyPerTick;

    EnergySpec(ForgeConfigSpec.Builder builder) {
        builder.push("blocks");

        busCableEnergyPerTick = builder.comment("The amount of energy consumed per tick by a bus cable")
            .defineInRange("busCableEnergyPerTick", 0.05, 0, Double.MAX_VALUE);

        busInterfaceEnergyPerTick = builder.comment("The amount of energy consumed per tick by a bus interface")
            .defineInRange("busInterfaceEnergyPerTick", 0.05, 0, Double.MAX_VALUE);

        computerEnergyPerTick = builder.comment("The amount of energy consumed per tick by a computer")
            .defineInRange("computerEnergyPerTick", 10, 0, Integer.MAX_VALUE);

        computerEnergyStorage = builder.comment("The amount of energy stored in a computer")
            .defineInRange("computerEnergyStorage", 2000, 0, Integer.MAX_VALUE);

        chargerEnergyPerTick = builder.comment("The amount of energy consumed per tick by a charger")
            .defineInRange("chargerEnergyPerTick", 2500, 0, Integer.MAX_VALUE);

        chargerEnergyStorage = builder.comment("The amount of energy stored in a charger")
            .defineInRange("chargerEnergyStorage", 10000, 0, Integer.MAX_VALUE);

        projectorEnergyPerTick = builder.comment("The amount of energy consumed per tick by a projector")
            .defineInRange("projectorEnergyPerTick", 20, 0, Integer.MAX_VALUE);

        projectorEnergyStorage = builder.comment("The amount of energy stored in a projector")
            .defineInRange("projectorEnergyStorage", 2000, 0, Integer.MAX_VALUE);

        monitorEnergyPerTick = builder.comment("The amount of energy consumed per tick by a monitor")
            .defineInRange("monitorEnergyPerTick", 15, 0, Integer.MAX_VALUE);

        monitorEnergyStorage = builder.comment("The amount of energy stored in a monitor")
            .defineInRange("monitorEnergyStorage", 2000, 0, Integer.MAX_VALUE);

        cardCageEnergyPerTick = builder.comment("The amount of energy consumed per tick by a card cage")
            .defineInRange("cardCageEnergyPerTick", 20, 0, Integer.MAX_VALUE);

        cardCageEnergyStorage = builder.comment("The amount of energy stored in a card cage")
            .defineInRange("cardCageEnergyStorage", 2000, 0, Integer.MAX_VALUE);

        gatewayEnergyPerPacket = builder.comment("The amount of energy consumed per packet by a gateway")
            .defineInRange("gatewayEnergyPerPacket", 20, 0, Integer.MAX_VALUE);

        gatewayEnergyStorage = builder.comment("The amount of energy stored in a gateway")
            .defineInRange("gatewayEnergyStorage", 2000, 0, Integer.MAX_VALUE);

        builder.pop();

        builder.push("entities");

        robotEnergyPerTick = builder.comment("The amount of energy consumed per tick by a robot")
            .defineInRange("gatewayEnergyStorage", 5, 0, Integer.MAX_VALUE);

        robotEnergyStorage = builder.comment("The amount of energy stored in a robot")
            .defineInRange("robotEnergyStorage", 750000, 0, Integer.MAX_VALUE);

        builder.pop();

        builder.push("items");

        memoryEnergyPerMegabytePerTick = builder.comment("The amount of energy consumed per megabyte per tick for memory modules")
            .defineInRange("memoryEnergyPerMegabytePerTick", 0.05, 0, Double.MAX_VALUE);

        hardDriveEnergyPerMegabytePerTick = builder.comment("The amount of energy consumed per megabyte per tick for hard drive modules")
            .defineInRange("hardDriveEnergyPerMegabytePerTick", 1.0, 0, Double.MAX_VALUE);

        cpuEnergyPerMegahertzPerTick = builder.comment("The amount of energy consumed per megahertz per tick for CPU modules")
            .defineInRange("cpuEnergyPerMegahertzPerTick", 0.1, 0, Double.MAX_VALUE);

        redstoneInterfaceCardEnergyPerTick = builder.comment("The amount of energy consumed per tick for redstone interface cards")
            .defineInRange("redstoneInterfaceCardEnergyPerTick", 1, 0, Integer.MAX_VALUE);

        networkInterfaceEnergyPerTick = builder.comment("The amount of energy consumed per tick for network interface cards")
            .defineInRange("redstoneInterfaceCardEnergyPerTick", 1, 0, Integer.MAX_VALUE);

        fileImportExportCardEnergyPerTick = builder.comment("The amount of energy consumed per tick for file import/export cards")
            .defineInRange("fileImportExportCardEnergyPerTick", 1, 0, Integer.MAX_VALUE);

        soundCardEnergyPerTick = builder.comment("The amount of energy consumed per tick for sound cards")
            .defineInRange("soundCardEnergyPerTick", 1, 0, Integer.MAX_VALUE);

        blockOperationsModuleEnergyPerTick = builder.comment("The amount of energy consumed per tick for block operations modules")
            .defineInRange("blockOperationsModuleEnergyPerTick", 2, 0, Integer.MAX_VALUE);

        inventoryOperationsModuleEnergyPerTick = builder.comment("The amount of energy consumed per tick for inventory operations modules")
            .defineInRange("inventoryOperationsModuleEnergyPerTick", 1, 0, Integer.MAX_VALUE);

        networkTunnelEnergyPerTick = builder.comment("The amount of energy consumed per tick for network tunnels")
            .defineInRange("networkTunnelEnergyPerTick", 2, 0, Integer.MAX_VALUE);

        builder.pop();
    }

    public void loadValues() {
        // BLOCKS //
        Config.busCableEnergyPerTick = busCableEnergyPerTick.get();
        Config.busInterfaceEnergyPerTick = busInterfaceEnergyPerTick.get();
        Config.computerEnergyPerTick = computerEnergyPerTick.get();
        Config.computerEnergyStorage = computerEnergyStorage.get();
        Config.chargerEnergyPerTick = chargerEnergyPerTick.get();
        Config.chargerEnergyStorage = chargerEnergyStorage.get();
        Config.projectorEnergyPerTick = projectorEnergyPerTick.get();
        Config.projectorEnergyStorage = projectorEnergyStorage.get();
        Config.monitorEnergyPerTick = monitorEnergyPerTick.get();
        Config.monitorEnergyStorage = monitorEnergyStorage.get();
        Config.cardCageEnergyPerTick = cardCageEnergyPerTick.get();
        Config.cardCageEnergyStorage = cardCageEnergyStorage.get();
        Config.gatewayEnergyPerPacket = gatewayEnergyPerPacket.get();
        Config.gatewayEnergyStorage = gatewayEnergyStorage.get();
        // ENTITIES //
        Config.robotEnergyPerTick = robotEnergyPerTick.get();
        Config.robotEnergyStorage = robotEnergyStorage.get();
        // ITEMS //
        Config.memoryEnergyPerMegabytePerTick = memoryEnergyPerMegabytePerTick.get();
        Config.hardDriveEnergyPerMegabytePerTick = hardDriveEnergyPerMegabytePerTick.get();
        Config.cpuEnergyPerMegahertzPerTick = cpuEnergyPerMegahertzPerTick.get();
        Config.redstoneInterfaceCardEnergyPerTick = redstoneInterfaceCardEnergyPerTick.get();
        Config.networkInterfaceEnergyPerTick = networkInterfaceEnergyPerTick.get();
        Config.fileImportExportCardEnergyPerTick = fileImportExportCardEnergyPerTick.get();
        Config.soundCardEnergyPerTick = soundCardEnergyPerTick.get();
        Config.blockOperationsModuleEnergyPerTick = blockOperationsModuleEnergyPerTick.get();
        Config.inventoryOperationsModuleEnergyPerTick = inventoryOperationsModuleEnergyPerTick.get();
    }
}
