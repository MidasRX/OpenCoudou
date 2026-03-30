package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

/**
 * Stub registration for blocks - to be expanded.
 */
object ModBlocks {
    private val BLOCKS: DeferredRegister.Blocks = DeferredRegister.createBlocks(OpenComputers.MOD_ID)
    
    private fun simpleBlock(): Supplier<Block> = Supplier { Block(BlockBehaviour.Properties.of()) }
    
    // Stub blocks - these will be expanded later
    val CASE_TIER1: DeferredBlock<Block> = BLOCKS.register("case1", simpleBlock())
    val CASE_TIER2: DeferredBlock<Block> = BLOCKS.register("case2", simpleBlock())
    val CASE_TIER3: DeferredBlock<Block> = BLOCKS.register("case3", simpleBlock())
    val CASE_CREATIVE: DeferredBlock<Block> = BLOCKS.register("case_creative", simpleBlock())
    
    val SCREEN_TIER1: DeferredBlock<Block> = BLOCKS.register("screen1", simpleBlock())
    val SCREEN_TIER2: DeferredBlock<Block> = BLOCKS.register("screen2", simpleBlock())
    val SCREEN_TIER3: DeferredBlock<Block> = BLOCKS.register("screen3", simpleBlock())
    
    val KEYBOARD: DeferredBlock<Block> = BLOCKS.register("keyboard", simpleBlock())
    val REDSTONE_IO: DeferredBlock<Block> = BLOCKS.register("redstone_io", simpleBlock())
    
    val CABLE: DeferredBlock<Block> = BLOCKS.register("cable", simpleBlock())
    val RELAY: DeferredBlock<Block> = BLOCKS.register("relay", simpleBlock())
    val ACCESS_POINT: DeferredBlock<Block> = BLOCKS.register("access_point", simpleBlock())
    
    val CAPACITOR: DeferredBlock<Block> = BLOCKS.register("capacitor", simpleBlock())
    val POWER_CONVERTER: DeferredBlock<Block> = BLOCKS.register("power_converter", simpleBlock())
    val POWER_DISTRIBUTOR: DeferredBlock<Block> = BLOCKS.register("power_distributor", simpleBlock())
    val CHARGER: DeferredBlock<Block> = BLOCKS.register("charger", simpleBlock())
    
    val ASSEMBLER: DeferredBlock<Block> = BLOCKS.register("assembler", simpleBlock())
    val DISASSEMBLER: DeferredBlock<Block> = BLOCKS.register("disassembler", simpleBlock())
    val PRINTER: DeferredBlock<Block> = BLOCKS.register("printer", simpleBlock())
    val RAID: DeferredBlock<Block> = BLOCKS.register("raid", simpleBlock())
    
    val ADAPTER: DeferredBlock<Block> = BLOCKS.register("adapter", simpleBlock())
    val HOLOGRAM_TIER1: DeferredBlock<Block> = BLOCKS.register("hologram1", simpleBlock())
    val HOLOGRAM_TIER2: DeferredBlock<Block> = BLOCKS.register("hologram2", simpleBlock())
    val DISK_DRIVE: DeferredBlock<Block> = BLOCKS.register("disk_drive", simpleBlock())
    
    val GEOLYZER: DeferredBlock<Block> = BLOCKS.register("geolyzer", simpleBlock())
    val MOTION_SENSOR: DeferredBlock<Block> = BLOCKS.register("motion_sensor", simpleBlock())
    val TRANSPOSER: DeferredBlock<Block> = BLOCKS.register("transposer", simpleBlock())
    val WAYPOINT: DeferredBlock<Block> = BLOCKS.register("waypoint", simpleBlock())
    
    val NET_SPLITTER: DeferredBlock<Block> = BLOCKS.register("net_splitter", simpleBlock())
    val CHAMELIUM_BLOCK: DeferredBlock<Block> = BLOCKS.register("chamelium_block", simpleBlock())
    val WEB_DISPLAY: DeferredBlock<Block> = BLOCKS.register("web_display", simpleBlock())
    
    // Additional blocks for ModItems
    val RACK: DeferredBlock<Block> = BLOCKS.register("rack", simpleBlock())
    val MICROCONTROLLER: DeferredBlock<Block> = BLOCKS.register("microcontroller", simpleBlock())
    
    fun register(eventBus: IEventBus) {
        BLOCKS.register(eventBus)
    }
}
