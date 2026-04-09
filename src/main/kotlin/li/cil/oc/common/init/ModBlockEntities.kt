package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.common.blockentity.CaseBlockEntity
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

object ModBlockEntities {
    private val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> = 
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OpenComputers.MOD_ID)

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    val CASE: DeferredHolder<BlockEntityType<*>, BlockEntityType<CaseBlockEntity>> =
        BLOCK_ENTITIES.register("case") { _: ResourceLocation ->
            BlockEntityType(
                BlockEntityType.BlockEntitySupplier { pos, state -> CaseBlockEntity(pos, state) },
                ModBlocks.CASE_TIER1.get(), ModBlocks.CASE_TIER2.get(),
                ModBlocks.CASE_TIER3.get(), ModBlocks.CASE_CREATIVE.get()
            )
        }

    fun register(eventBus: IEventBus) {
        BLOCK_ENTITIES.register(eventBus)
    }
}
