package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.CaseMenu
import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.MenuType
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister

object ModMenus {
    private val MENUS: DeferredRegister<MenuType<*>> = 
        DeferredRegister.create(Registries.MENU, OpenComputers.MOD_ID)

    val CASE: DeferredHolder<MenuType<*>, MenuType<CaseMenu>> =
        MENUS.register("case") { ->
            IMenuTypeExtension.create(CaseMenu::fromNetwork)
        }

    fun register(eventBus: IEventBus) {
        MENUS.register(eventBus)
    }
}
