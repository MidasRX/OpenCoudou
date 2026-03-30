package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.MenuType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Stub registration for menus - to be expanded.
 */
object ModMenus {
    private val MENUS: DeferredRegister<MenuType<*>> = 
        DeferredRegister.create(Registries.MENU, OpenComputers.MOD_ID)
    
    fun register(eventBus: IEventBus) {
        MENUS.register(eventBus)
    }
}
