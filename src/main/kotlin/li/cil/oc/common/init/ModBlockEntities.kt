package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.common.blockentity.AccessPointBlockEntity
import li.cil.oc.common.blockentity.AdapterBlockEntity
import li.cil.oc.common.blockentity.AssemblerBlockEntity
import li.cil.oc.common.blockentity.CapacitorBlockEntity
import li.cil.oc.common.blockentity.CaseBlockEntity
import li.cil.oc.common.blockentity.ChargerBlockEntity
import li.cil.oc.common.blockentity.DisassemblerBlockEntity
import li.cil.oc.common.blockentity.DiskDriveBlockEntity
import li.cil.oc.common.blockentity.GeolyzerBlockEntity
import li.cil.oc.common.blockentity.HologramBlockEntity
import li.cil.oc.common.blockentity.MotionSensorBlockEntity
import li.cil.oc.common.blockentity.NetSplitterBlockEntity
import li.cil.oc.common.blockentity.PowerConverterBlockEntity
import li.cil.oc.common.blockentity.PowerDistributorBlockEntity
import li.cil.oc.common.blockentity.PrinterBlockEntity
import li.cil.oc.common.blockentity.RaidBlockEntity
import li.cil.oc.common.blockentity.RelayBlockEntity
import li.cil.oc.common.blockentity.ScreenBlockEntity
import li.cil.oc.common.blockentity.ServerRackBlockEntity
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

    fun register(eventBus: IEventBus) {
        BLOCK_ENTITIES.register(eventBus)
    }
}
