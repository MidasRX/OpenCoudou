package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.EntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Stub registration for entities - to be expanded.
 */
object ModEntities {
    private val ENTITIES: DeferredRegister<EntityType<*>> = 
        DeferredRegister.create(Registries.ENTITY_TYPE, OpenComputers.MOD_ID)
    
    fun register(eventBus: IEventBus) {
        ENTITIES.register(eventBus)
    }
}
