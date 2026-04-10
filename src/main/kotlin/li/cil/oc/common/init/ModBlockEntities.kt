package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.common.blockentity.AdapterBlockEntity
import li.cil.oc.common.blockentity.CaseBlockEntity
import li.cil.oc.common.blockentity.DiskDriveBlockEntity
import li.cil.oc.common.blockentity.GeolyzerBlockEntity
import li.cil.oc.common.blockentity.HologramBlockEntity
import li.cil.oc.common.blockentity.PrinterBlockEntity
import li.cil.oc.common.blockentity.ScreenBlockEntity
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

object ModBlockEntities {
    private val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> = 
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OpenComputers.MOD_ID)

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val CASE: DeferredHolder<BlockEntityType<*>, BlockEntityType<CaseBlockEntity>> =
        BLOCK_ENTITIES.register("case") {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> CaseBlockEntity(pos, state) },
                ModBlocks.CASE_TIER1.get(), ModBlocks.CASE_TIER2.get(),
                ModBlocks.CASE_TIER3.get(), ModBlocks.CASE_CREATIVE.get()
            )
        }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val SCREEN: DeferredHolder<BlockEntityType<*>, BlockEntityType<ScreenBlockEntity>> =
        BLOCK_ENTITIES.register("screen") {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> ScreenBlockEntity(pos, state) },
                ModBlocks.SCREEN_TIER1.get(), ModBlocks.SCREEN_TIER2.get(),
                ModBlocks.SCREEN_TIER3.get()
            )
        }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val DISK_DRIVE: DeferredHolder<BlockEntityType<*>, BlockEntityType<DiskDriveBlockEntity>> =
        BLOCK_ENTITIES.register("disk_drive") {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> DiskDriveBlockEntity(pos, state) },
                ModBlocks.DISK_DRIVE.get()
            )
        }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val HOLOGRAM: DeferredHolder<BlockEntityType<*>, BlockEntityType<HologramBlockEntity>> =
        BLOCK_ENTITIES.register("hologram") {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> HologramBlockEntity(pos, state) },
                ModBlocks.HOLOGRAM_TIER1.get(), ModBlocks.HOLOGRAM_TIER2.get()
            )
        }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val GEOLYZER: DeferredHolder<BlockEntityType<*>, BlockEntityType<GeolyzerBlockEntity>> =
        BLOCK_ENTITIES.register("geolyzer") {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> GeolyzerBlockEntity(pos, state) },
                ModBlocks.GEOLYZER.get()
            )
        }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val ADAPTER: DeferredHolder<BlockEntityType<*>, BlockEntityType<AdapterBlockEntity>> =
        BLOCK_ENTITIES.register("adapter") {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> AdapterBlockEntity(pos, state) },
                ModBlocks.ADAPTER.get()
            )
        }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val PRINTER: DeferredHolder<BlockEntityType<*>, BlockEntityType<PrinterBlockEntity>> =
        BLOCK_ENTITIES.register("printer") {
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> PrinterBlockEntity(pos, state) },
                ModBlocks.PRINTER.get()
            )
        }

    fun register(eventBus: IEventBus) {
        BLOCK_ENTITIES.register(eventBus)
    }
}
