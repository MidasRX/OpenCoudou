package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.common.blockentity.AccessPointBlockEntity
import li.cil.oc.common.blockentity.AdapterBlockEntity
import li.cil.oc.common.blockentity.AssemblerBlockEntity
import li.cil.oc.common.blockentity.CableBlockEntity
import li.cil.oc.common.blockentity.CapacitorBlockEntity
import li.cil.oc.common.blockentity.CarpetedCapacitorBlockEntity
import li.cil.oc.common.blockentity.CaseBlockEntity
import li.cil.oc.common.blockentity.ChargerBlockEntity
import li.cil.oc.common.blockentity.DisassemblerBlockEntity
import li.cil.oc.common.blockentity.DiskDriveBlockEntity
import li.cil.oc.common.blockentity.GeolyzerBlockEntity
import li.cil.oc.common.blockentity.HologramBlockEntity
import li.cil.oc.common.blockentity.KeyboardBlockEntity
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity
import li.cil.oc.common.blockentity.MotionSensorBlockEntity
import li.cil.oc.common.blockentity.NetSplitterBlockEntity
import li.cil.oc.common.blockentity.PowerConverterBlockEntity
import li.cil.oc.common.blockentity.PowerDistributorBlockEntity
import li.cil.oc.common.blockentity.PrintBlockEntity
import li.cil.oc.common.blockentity.PrinterBlockEntity
import li.cil.oc.common.blockentity.RaidBlockEntity
import li.cil.oc.common.blockentity.RedstoneIOBlockEntity
import li.cil.oc.common.blockentity.RelayBlockEntity
import li.cil.oc.common.blockentity.RobotProxyBlockEntity
import li.cil.oc.common.blockentity.ScreenBlockEntity
import li.cil.oc.common.blockentity.ServerRackBlockEntity
import li.cil.oc.common.blockentity.SwitchBlockEntity
import li.cil.oc.common.blockentity.TransposerBlockEntity
import li.cil.oc.common.blockentity.WaypointBlockEntity
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModBlockEntities {
    private val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> = 
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OpenComputers.MOD_ID)

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val CASE: DeferredHolder<BlockEntityType<*>, BlockEntityType<CaseBlockEntity>> =
        BLOCK_ENTITIES.register("case", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> CaseBlockEntity(pos, state) },
                ModBlocks.CASE_TIER1.get(), ModBlocks.CASE_TIER2.get(),
                ModBlocks.CASE_TIER3.get(), ModBlocks.CASE_CREATIVE.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val SCREEN: DeferredHolder<BlockEntityType<*>, BlockEntityType<ScreenBlockEntity>> =
        BLOCK_ENTITIES.register("screen", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> ScreenBlockEntity(pos, state) },
                ModBlocks.SCREEN_TIER1.get(), ModBlocks.SCREEN_TIER2.get(),
                ModBlocks.SCREEN_TIER3.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val DISK_DRIVE: DeferredHolder<BlockEntityType<*>, BlockEntityType<DiskDriveBlockEntity>> =
        BLOCK_ENTITIES.register("disk_drive", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> DiskDriveBlockEntity(pos, state) },
                ModBlocks.DISK_DRIVE.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val HOLOGRAM: DeferredHolder<BlockEntityType<*>, BlockEntityType<HologramBlockEntity>> =
        BLOCK_ENTITIES.register("hologram", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> HologramBlockEntity(pos, state) },
                ModBlocks.HOLOGRAM_TIER1.get(), ModBlocks.HOLOGRAM_TIER2.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val GEOLYZER: DeferredHolder<BlockEntityType<*>, BlockEntityType<GeolyzerBlockEntity>> =
        BLOCK_ENTITIES.register("geolyzer", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> GeolyzerBlockEntity(pos, state) },
                ModBlocks.GEOLYZER.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val ADAPTER: DeferredHolder<BlockEntityType<*>, BlockEntityType<AdapterBlockEntity>> =
        BLOCK_ENTITIES.register("adapter", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> AdapterBlockEntity(pos, state) },
                ModBlocks.ADAPTER.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val PRINTER: DeferredHolder<BlockEntityType<*>, BlockEntityType<PrinterBlockEntity>> =
        BLOCK_ENTITIES.register("printer", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> PrinterBlockEntity(pos, state) },
                ModBlocks.PRINTER.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val PRINT: DeferredHolder<BlockEntityType<*>, BlockEntityType<PrintBlockEntity>> =
        BLOCK_ENTITIES.register("print", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> PrintBlockEntity(pos, state) },
                ModBlocks.PRINT.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val CHARGER: DeferredHolder<BlockEntityType<*>, BlockEntityType<ChargerBlockEntity>> =
        BLOCK_ENTITIES.register("charger", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> ChargerBlockEntity(pos, state) },
                ModBlocks.CHARGER.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val ASSEMBLER: DeferredHolder<BlockEntityType<*>, BlockEntityType<AssemblerBlockEntity>> =
        BLOCK_ENTITIES.register("assembler", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> AssemblerBlockEntity(pos, state) },
                ModBlocks.ASSEMBLER.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val DISASSEMBLER: DeferredHolder<BlockEntityType<*>, BlockEntityType<DisassemblerBlockEntity>> =
        BLOCK_ENTITIES.register("disassembler", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> DisassemblerBlockEntity(pos, state) },
                ModBlocks.DISASSEMBLER.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val CAPACITOR: DeferredHolder<BlockEntityType<*>, BlockEntityType<CapacitorBlockEntity>> =
        BLOCK_ENTITIES.register("capacitor", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> CapacitorBlockEntity(pos, state) },
                ModBlocks.CAPACITOR.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val CARPETED_CAPACITOR: DeferredHolder<BlockEntityType<*>, BlockEntityType<CarpetedCapacitorBlockEntity>> =
        BLOCK_ENTITIES.register("carpeted_capacitor", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> CarpetedCapacitorBlockEntity(pos, state) },
                ModBlocks.CARPETED_CAPACITOR.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val RAID: DeferredHolder<BlockEntityType<*>, BlockEntityType<RaidBlockEntity>> =
        BLOCK_ENTITIES.register("raid", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> RaidBlockEntity(pos, state) },
                ModBlocks.RAID.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val RELAY: DeferredHolder<BlockEntityType<*>, BlockEntityType<RelayBlockEntity>> =
        BLOCK_ENTITIES.register("relay", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> RelayBlockEntity(pos, state) },
                ModBlocks.RELAY.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val ACCESS_POINT: DeferredHolder<BlockEntityType<*>, BlockEntityType<AccessPointBlockEntity>> =
        BLOCK_ENTITIES.register("access_point", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> AccessPointBlockEntity(pos, state) },
                ModBlocks.ACCESS_POINT.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val POWER_CONVERTER: DeferredHolder<BlockEntityType<*>, BlockEntityType<PowerConverterBlockEntity>> =
        BLOCK_ENTITIES.register("power_converter", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> PowerConverterBlockEntity(pos, state) },
                ModBlocks.POWER_CONVERTER.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val POWER_DISTRIBUTOR: DeferredHolder<BlockEntityType<*>, BlockEntityType<PowerDistributorBlockEntity>> =
        BLOCK_ENTITIES.register("power_distributor", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> PowerDistributorBlockEntity(pos, state) },
                ModBlocks.POWER_DISTRIBUTOR.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val MOTION_SENSOR: DeferredHolder<BlockEntityType<*>, BlockEntityType<MotionSensorBlockEntity>> =
        BLOCK_ENTITIES.register("motion_sensor", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> MotionSensorBlockEntity(pos, state) },
                ModBlocks.MOTION_SENSOR.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val TRANSPOSER: DeferredHolder<BlockEntityType<*>, BlockEntityType<TransposerBlockEntity>> =
        BLOCK_ENTITIES.register("transposer", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> TransposerBlockEntity(pos, state) },
                ModBlocks.TRANSPOSER.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val WAYPOINT: DeferredHolder<BlockEntityType<*>, BlockEntityType<WaypointBlockEntity>> =
        BLOCK_ENTITIES.register("waypoint", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> WaypointBlockEntity(pos, state) },
                ModBlocks.WAYPOINT.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val NET_SPLITTER: DeferredHolder<BlockEntityType<*>, BlockEntityType<NetSplitterBlockEntity>> =
        BLOCK_ENTITIES.register("net_splitter", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> NetSplitterBlockEntity(pos, state) },
                ModBlocks.NET_SPLITTER.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val SERVER_RACK: DeferredHolder<BlockEntityType<*>, BlockEntityType<ServerRackBlockEntity>> =
        BLOCK_ENTITIES.register("server_rack", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> ServerRackBlockEntity(pos, state) },
                ModBlocks.RACK.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val KEYBOARD: DeferredHolder<BlockEntityType<*>, BlockEntityType<KeyboardBlockEntity>> =
        BLOCK_ENTITIES.register("keyboard", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> KeyboardBlockEntity(pos, state) },
                ModBlocks.KEYBOARD.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val REDSTONE_IO: DeferredHolder<BlockEntityType<*>, BlockEntityType<RedstoneIOBlockEntity>> =
        BLOCK_ENTITIES.register("redstone_io", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> RedstoneIOBlockEntity(pos, state) },
                ModBlocks.REDSTONE_IO.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val MICROCONTROLLER: DeferredHolder<BlockEntityType<*>, BlockEntityType<MicrocontrollerBlockEntity>> =
        BLOCK_ENTITIES.register("microcontroller", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> MicrocontrollerBlockEntity(pos, state) },
                ModBlocks.MICROCONTROLLER.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val ROBOT_PROXY: DeferredHolder<BlockEntityType<*>, BlockEntityType<RobotProxyBlockEntity>> =
        BLOCK_ENTITIES.register("robot_proxy", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> RobotProxyBlockEntity(pos, state) },
                ModBlocks.ROBOT_PROXY.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val SWITCH: DeferredHolder<BlockEntityType<*>, BlockEntityType<SwitchBlockEntity>> =
        BLOCK_ENTITIES.register("switch", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> SwitchBlockEntity(pos, state) },
                ModBlocks.SWITCH.get()
            )
        })

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val CABLE: DeferredHolder<BlockEntityType<*>, BlockEntityType<CableBlockEntity>> =
        BLOCK_ENTITIES.register("cable", Supplier {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> CableBlockEntity(pos, state) },
                ModBlocks.CABLE.get()
            )
        })

    fun register(eventBus: IEventBus) {
        BLOCK_ENTITIES.register(eventBus)
    }
}
