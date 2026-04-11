package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.server.entity.DroneEntity
import li.cil.oc.server.entity.RobotEntity
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Entity registration for OpenComputers entities.
 */
object ModEntities {
    private val ENTITIES: DeferredRegister<EntityType<*>> = 
        DeferredRegister.create(Registries.ENTITY_TYPE, OpenComputers.MOD_ID)
    
    // Drone entity - flying robot
    val DRONE: DeferredHolder<EntityType<*>, EntityType<DroneEntity>> = ENTITIES.register("drone") { ->
        EntityType.Builder.of(::DroneEntity, MobCategory.MISC)
            .sized(0.4f, 0.4f)
            .clientTrackingRange(8)
            .updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "drone")))
    }
    
    // Robot entity - ground-based robot  
    val ROBOT: DeferredHolder<EntityType<*>, EntityType<RobotEntity>> = ENTITIES.register("robot") { ->
        EntityType.Builder.of(::RobotEntity, MobCategory.MISC)
            .sized(0.875f, 0.875f)
            .clientTrackingRange(8)
            .updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "robot")))
    }
    
    fun register(eventBus: IEventBus) {
        ENTITIES.register(eventBus)
        eventBus.addListener(::registerAttributes)
    }
    
    private fun registerAttributes(event: EntityAttributeCreationEvent) {
        event.put(DRONE.get(), DroneEntity.createAttributes().build())
        event.put(ROBOT.get(), RobotEntity.createAttributes().build())
    }
}
