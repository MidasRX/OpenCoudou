package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Stub registration for blocks - to be expanded.
 */
object ModBlocks {
    private val BLOCKS: DeferredRegister.Blocks = DeferredRegister.createBlocks(OpenComputers.MOD_ID)
    
    // Stub blocks - these will be expanded later
    val CASE_TIER1: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("case1", BlockBehaviour.Properties.of())
    val CASE_TIER2: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("case2", BlockBehaviour.Properties.of())
    val CASE_TIER3: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("case3", BlockBehaviour.Properties.of())
    val CASE_CREATIVE: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("case_creative", BlockBehaviour.Properties.of())
    
    val SCREEN_TIER1: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("screen1", BlockBehaviour.Properties.of())
    val SCREEN_TIER2: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("screen2", BlockBehaviour.Properties.of())
    val SCREEN_TIER3: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("screen3", BlockBehaviour.Properties.of())
    
    val KEYBOARD: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("keyboard", BlockBehaviour.Properties.of())
    val REDSTONE_IO: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("redstone_io", BlockBehaviour.Properties.of())
    
    val CABLE: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("cable", BlockBehaviour.Properties.of())
    val RELAY: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("relay", BlockBehaviour.Properties.of())
    val ACCESS_POINT: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("access_point", BlockBehaviour.Properties.of())
    
    val CAPACITOR: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("capacitor", BlockBehaviour.Properties.of())
    val POWER_CONVERTER: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("power_converter", BlockBehaviour.Properties.of())
    val POWER_DISTRIBUTOR: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("power_distributor", BlockBehaviour.Properties.of())
    val CHARGER: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("charger", BlockBehaviour.Properties.of())
    
    val ASSEMBLER: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("assembler", BlockBehaviour.Properties.of())
    val DISASSEMBLER: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("disassembler", BlockBehaviour.Properties.of())
    val PRINTER: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("printer", BlockBehaviour.Properties.of())
    val RAID: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("raid", BlockBehaviour.Properties.of())
    
    val ADAPTER: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("adapter", BlockBehaviour.Properties.of())
    val HOLOGRAM_TIER1: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("hologram1", BlockBehaviour.Properties.of())
    val HOLOGRAM_TIER2: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("hologram2", BlockBehaviour.Properties.of())
    val DISK_DRIVE: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("disk_drive", BlockBehaviour.Properties.of())
    
    val GEOLYZER: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("geolyzer", BlockBehaviour.Properties.of())
    val MOTION_SENSOR: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("motion_sensor", BlockBehaviour.Properties.of())
    val TRANSPOSER: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("transposer", BlockBehaviour.Properties.of())
    val WAYPOINT: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("waypoint", BlockBehaviour.Properties.of())
    
    val NET_SPLITTER: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("net_splitter", BlockBehaviour.Properties.of())
    val CHAMELIUM_BLOCK: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("chamelium_block", BlockBehaviour.Properties.of())
    val WEB_DISPLAY: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("web_display", BlockBehaviour.Properties.of())
    
    // Additional blocks for ModItems
    val RACK: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("rack", BlockBehaviour.Properties.of())
    val MICROCONTROLLER: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("microcontroller", BlockBehaviour.Properties.of())
    
    fun register(eventBus: IEventBus) {
        BLOCKS.register(eventBus)
    }
}
