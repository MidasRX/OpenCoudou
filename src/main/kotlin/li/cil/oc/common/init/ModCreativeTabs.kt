package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * OpenComputers creative tab - contains all mod items.
 */
object ModCreativeTabs {
    private val CREATIVE_TABS: DeferredRegister<CreativeModeTab> = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OpenComputers.MOD_ID)
    
    val OC_TAB = CREATIVE_TABS.register("tab") { ->
        CreativeModeTab.builder()
            .icon { ItemStack(ModItems.CASE_TIER1_ITEM.get()) }
            .title(Component.translatable("itemGroup.${OpenComputers.MOD_ID}"))
            .displayItems { _, output ->
                // Blocks
                output.accept(ModItems.CASE_TIER1_ITEM.get())
                output.accept(ModItems.CASE_TIER2_ITEM.get())
                output.accept(ModItems.CASE_TIER3_ITEM.get())
                output.accept(ModItems.CASE_CREATIVE_ITEM.get())
                
                output.accept(ModItems.SCREEN_TIER1_ITEM.get())
                output.accept(ModItems.SCREEN_TIER2_ITEM.get())
                output.accept(ModItems.SCREEN_TIER3_ITEM.get())
                
                output.accept(ModItems.KEYBOARD_ITEM.get())
                output.accept(ModItems.REDSTONE_IO_ITEM.get())
                
                output.accept(ModItems.CABLE_ITEM.get())
                output.accept(ModItems.RELAY_ITEM.get())
                output.accept(ModItems.ACCESS_POINT_ITEM.get())
                
                output.accept(ModItems.CAPACITOR_ITEM.get())
                output.accept(ModItems.POWER_CONVERTER_ITEM.get())
                output.accept(ModItems.POWER_DISTRIBUTOR_ITEM.get())
                output.accept(ModItems.CHARGER_ITEM.get())
                
                output.accept(ModItems.ADAPTER_ITEM.get())
                output.accept(ModItems.TRANSPOSER_ITEM.get())
                output.accept(ModItems.DISK_DRIVE_ITEM.get())
                output.accept(ModItems.RAID_ITEM.get())
                
                output.accept(ModItems.GEOLYZER_ITEM.get())
                output.accept(ModItems.MOTION_SENSOR_ITEM.get())
                output.accept(ModItems.WAYPOINT_ITEM.get())
                
                output.accept(ModItems.RACK_ITEM.get())
                output.accept(ModItems.ASSEMBLER_ITEM.get())
                output.accept(ModItems.DISASSEMBLER_ITEM.get())
                output.accept(ModItems.PRINTER_ITEM.get())
                
                output.accept(ModItems.HOLOGRAM_TIER1_ITEM.get())
                output.accept(ModItems.HOLOGRAM_TIER2_ITEM.get())
                output.accept(ModItems.MICROCONTROLLER_ITEM.get())
                output.accept(ModItems.CHAMELIUM_BLOCK_ITEM.get())
                output.accept(ModItems.WEB_DISPLAY_ITEM.get())
                
                // CPUs
                output.accept(ModItems.CPU_TIER1.get())
                output.accept(ModItems.CPU_TIER2.get())
                output.accept(ModItems.CPU_TIER3.get())
                output.accept(ModItems.CPU_CREATIVE.get())
                
                // APUs (combined CPU+GPU)
                output.accept(ModItems.APU_TIER1.get())
                output.accept(ModItems.APU_TIER2.get())
                output.accept(ModItems.APU_CREATIVE.get())
                
                // Memory
                output.accept(ModItems.MEMORY_TIER1.get())
                output.accept(ModItems.MEMORY_TIER1_5.get())
                output.accept(ModItems.MEMORY_TIER2.get())
                output.accept(ModItems.MEMORY_TIER2_5.get())
                output.accept(ModItems.MEMORY_TIER3.get())
                output.accept(ModItems.MEMORY_TIER3_5.get())
                
                // Storage
                output.accept(ModItems.HDD_TIER1.get())
                output.accept(ModItems.HDD_TIER2.get())
                output.accept(ModItems.HDD_TIER3.get())
                
                // Graphics Cards
                output.accept(ModItems.GPU_TIER1.get())
                output.accept(ModItems.GPU_TIER2.get())
                output.accept(ModItems.GPU_TIER3.get())
                
                // Network Cards
                output.accept(ModItems.NETWORK_CARD.get())
                output.accept(ModItems.WIRELESS_CARD_TIER1.get())
                output.accept(ModItems.WIRELESS_CARD_TIER2.get())
                output.accept(ModItems.LINKED_CARD.get())
                output.accept(ModItems.INTERNET_CARD.get())
                
                // Creative/Debug Cards
                output.accept(ModItems.DEBUG_CARD.get())
                
                // Other Cards
                output.accept(ModItems.REDSTONE_CARD_TIER1.get())
                output.accept(ModItems.REDSTONE_CARD_TIER2.get())
                output.accept(ModItems.DATA_CARD_TIER1.get())
                output.accept(ModItems.DATA_CARD_TIER2.get())
                output.accept(ModItems.DATA_CARD_TIER3.get())
                
                // Storage Media
                output.accept(ModItems.FLOPPY.get())
                output.accept(ModItems.EEPROM.get())
                
                // Tools
                output.accept(ModItems.ANALYZER.get())
                output.accept(ModItems.TERMINAL.get())
                output.accept(ModItems.MANUAL.get())
                output.accept(ModItems.WRENCH.get())
                
                // Materials
                output.accept(ModItems.CUTTING_WIRE.get())
                output.accept(ModItems.ACID.get())
                output.accept(ModItems.RAW_CIRCUIT_BOARD.get())
                output.accept(ModItems.CIRCUIT_TIER1.get())
                output.accept(ModItems.CIRCUIT_TIER2.get())
                output.accept(ModItems.CARD_BASE.get())
                output.accept(ModItems.TRANSISTOR.get())
                output.accept(ModItems.MICROCHIP_TIER1.get())
                output.accept(ModItems.MICROCHIP_TIER2.get())
                output.accept(ModItems.MICROCHIP_TIER3.get())
                output.accept(ModItems.ALU.get())
                output.accept(ModItems.CONTROL_UNIT.get())
                output.accept(ModItems.DISK_PLATTER.get())
                output.accept(ModItems.INTERWEB.get())
                output.accept(ModItems.BUTTON_GROUP.get())
                output.accept(ModItems.ARROW_KEYS.get())
                output.accept(ModItems.NUMPAD.get())
                output.accept(ModItems.CHAMELIUM.get())
                output.accept(ModItems.INK_CARTRIDGE.get())
                output.accept(ModItems.INK_CARTRIDGE_EMPTY.get())
                output.accept(ModItems.DRONE_CASE_TIER1.get())
                output.accept(ModItems.DRONE_CASE_TIER2.get())
                output.accept(ModItems.DRONE_CASE_CREATIVE.get())
                output.accept(ModItems.MICROCONTROLLER_CASE_TIER1.get())
                output.accept(ModItems.MICROCONTROLLER_CASE_TIER2.get())
                output.accept(ModItems.MICROCONTROLLER_CASE_CREATIVE.get())
                output.accept(ModItems.TABLET_CASE_TIER1.get())
                output.accept(ModItems.TABLET_CASE_TIER2.get())
                output.accept(ModItems.TABLET_CASE_CREATIVE.get())
                
                // Servers
                output.accept(ModItems.SERVER_TIER1.get())
                output.accept(ModItems.SERVER_TIER2.get())
                output.accept(ModItems.SERVER_TIER3.get())
                output.accept(ModItems.SERVER_CREATIVE.get())
                
                output.accept(ModItems.NANOMACHINES.get())
            }
            .build()
    }
    
    fun register(eventBus: IEventBus) {
        CREATIVE_TABS.register(eventBus)
    }
}
