package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Stub registration for creative tabs - to be expanded.
 */
object ModCreativeTabs {
    private val CREATIVE_TABS: DeferredRegister<CreativeModeTab> = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OpenComputers.MOD_ID)
    
    val OC_TAB = CREATIVE_TABS.register("tab") { ->
        CreativeModeTab.builder()
            .icon { ItemStack.EMPTY }
            .title(Component.translatable("itemGroup.${OpenComputers.MOD_ID}"))
            .build()
    }
    
    fun register(eventBus: IEventBus) {
        CREATIVE_TABS.register(eventBus)
    }
}
