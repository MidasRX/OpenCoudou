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
    
    // Beep sounds
    val BEEP: DeferredHolder<SoundEvent, SoundEvent> = registerSound("beep")
    
    // Robot sounds
    val ROBOT_START: DeferredHolder<SoundEvent, SoundEvent> = registerSound("robot_start")
    val ROBOT_STOP: DeferredHolder<SoundEvent, SoundEvent> = registerSound("robot_stop")
    val ROBOT_MOVE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("robot_move")
    val ROBOT_TURN: DeferredHolder<SoundEvent, SoundEvent> = registerSound("robot_turn")
    val ROBOT_SWING: DeferredHolder<SoundEvent, SoundEvent> = registerSound("robot_swing")
    val ROBOT_USE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("robot_use")
    
    // Drone sounds
    val DRONE_HOVER: DeferredHolder<SoundEvent, SoundEvent> = registerSound("drone_hover")
    val DRONE_MOVE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("drone_move")
    
    // Printer sounds
    val PRINTER_PRINT: DeferredHolder<SoundEvent, SoundEvent> = registerSound("printer_print")
    val PRINTER_COMPLETE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("printer_complete")
    
    // Assembler sounds
    val ASSEMBLER_START: DeferredHolder<SoundEvent, SoundEvent> = registerSound("assembler_start")
    val ASSEMBLER_COMPLETE: DeferredHolder<SoundEvent, SoundEvent> = registerSound("assembler_complete")
    
    // Generic sounds
    val CLICK: DeferredHolder<SoundEvent, SoundEvent> = registerSound("click")
    val POWER_ON: DeferredHolder<SoundEvent, SoundEvent> = registerSound("power_on")
    val POWER_OFF: DeferredHolder<SoundEvent, SoundEvent> = registerSound("power_off")
    
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
