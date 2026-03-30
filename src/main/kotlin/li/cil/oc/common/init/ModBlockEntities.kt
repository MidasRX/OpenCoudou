package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Stub registration for block entities - to be expanded.
 */
object ModBlockEntities {
    private val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> = 
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OpenComputers.MOD_ID)
    
    fun register(eventBus: IEventBus) {
        BLOCK_ENTITIES.register(eventBus)
    }
}
