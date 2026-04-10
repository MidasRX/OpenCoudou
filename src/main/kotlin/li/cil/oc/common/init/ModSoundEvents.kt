package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

/**
 * Registration for OpenComputers sound events.
 * Only registers sounds that have actual .ogg files.
 */
object ModSoundEvents {
    private val SOUNDS: DeferredRegister<SoundEvent> = 
        DeferredRegister.create(Registries.SOUND_EVENT, OpenComputers.MOD_ID)
    
    // Computer running sound (the main ambient sound)
    val COMPUTER_RUNNING: DeferredHolder<SoundEvent, SoundEvent> = registerSound("computer_running")
    
    // Floppy disk sounds
    val FLOPPY_INSERT: DeferredHolder<SoundEvent, SoundEvent> = registerSound("floppy_insert")
    val FLOPPY_EJECT: DeferredHolder<SoundEvent, SoundEvent> = registerSound("floppy_eject")
    val FLOPPY_ACCESS: DeferredHolder<SoundEvent, SoundEvent> = registerSound("floppy_access")
    
    // HDD access sound
    val HDD_ACCESS: DeferredHolder<SoundEvent, SoundEvent> = registerSound("hdd_access")
    
    // ========================================
    // Helper Methods
    // ========================================
    
    private fun registerSound(name: String): DeferredHolder<SoundEvent, SoundEvent> {
        val location = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, name)
        val supplier: Supplier<SoundEvent> = Supplier {
            SoundEvent.createVariableRangeEvent(location)
        }
        return SOUNDS.register(name, supplier)
    }
    
    // ========================================
    // Registration
    // ========================================
    
    fun register(bus: IEventBus) {
        SOUNDS.register(bus)
        OpenComputers.LOGGER.debug("Registered sound events")
    }
}
