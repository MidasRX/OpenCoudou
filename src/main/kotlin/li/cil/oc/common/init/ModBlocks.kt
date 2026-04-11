package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.common.block.AccessPointBlock
import li.cil.oc.common.block.AdapterBlock
import li.cil.oc.common.block.AssemblerBlock
import li.cil.oc.common.block.CableBlock
import li.cil.oc.common.block.CapacitorBlock
import li.cil.oc.common.block.CaseBlock
import li.cil.oc.common.block.ChargerBlock
import li.cil.oc.common.block.DisassemblerBlock
import li.cil.oc.common.block.DiskDriveBlock
import li.cil.oc.common.block.GeolyzerBlock
import li.cil.oc.common.block.HologramBlock
import li.cil.oc.common.block.KeyboardBlock
import li.cil.oc.common.block.MotionSensorBlock
import li.cil.oc.common.block.NetSplitterBlock
import li.cil.oc.common.block.PowerConverterBlock
import li.cil.oc.common.block.PowerDistributorBlock
import li.cil.oc.common.block.PrinterBlock
import li.cil.oc.common.block.RaidBlock
import li.cil.oc.common.block.RelayBlock
import li.cil.oc.common.block.ScreenBlock
import li.cil.oc.common.block.ServerRackBlock
import li.cil.oc.common.block.TransposerBlock
import li.cil.oc.common.block.WaypointBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredRegister

object ModBlocks {
    private val BLOCKS: DeferredRegister.Blocks = DeferredRegister.createBlocks(OpenComputers.MOD_ID)
    
    // Case blocks with GUI interaction
    val CASE_TIER1: DeferredBlock<Block> = BLOCKS.registerBlock("case1", { props -> CaseBlock(1, props) }, BlockBehaviour.Properties.of())
    val CASE_TIER2: DeferredBlock<Block> = BLOCKS.registerBlock("case2", { props -> CaseBlock(2, props) }, BlockBehaviour.Properties.of())
    val CASE_TIER3: DeferredBlock<Block> = BLOCKS.registerBlock("case3", { props -> CaseBlock(3, props) }, BlockBehaviour.Properties.of())
    val CASE_CREATIVE: DeferredBlock<Block> = BLOCKS.registerBlock("case_creative", { props -> CaseBlock(4, props) }, BlockBehaviour.Properties.of())
    
    val SCREEN_TIER1: DeferredBlock<Block> = BLOCKS.registerBlock("screen1", { props -> ScreenBlock(props) }, BlockBehaviour.Properties.of())
    val SCREEN_TIER2: DeferredBlock<Block> = BLOCKS.registerBlock("screen2", { props -> ScreenBlock(props) }, BlockBehaviour.Properties.of())
    val SCREEN_TIER3: DeferredBlock<Block> = BLOCKS.registerBlock("screen3", { props -> ScreenBlock(props) }, BlockBehaviour.Properties.of())
    
    val KEYBOARD: DeferredBlock<Block> = BLOCKS.registerBlock("keyboard", { props -> KeyboardBlock(props) }, BlockBehaviour.Properties.of().noOcclusion())
    val REDSTONE_IO: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("redstone_io", BlockBehaviour.Properties.of())
    
    val CABLE: DeferredBlock<Block> = BLOCKS.registerBlock("cable", { props -> CableBlock(props) }, BlockBehaviour.Properties.of().noOcclusion())
    val RELAY: DeferredBlock<Block> = BLOCKS.registerBlock("relay", { props -> RelayBlock(props) }, BlockBehaviour.Properties.of())
    val ACCESS_POINT: DeferredBlock<Block> = BLOCKS.registerBlock("access_point", { props -> AccessPointBlock(props) }, BlockBehaviour.Properties.of())
    
    val CAPACITOR: DeferredBlock<Block> = BLOCKS.registerBlock("capacitor", { props -> CapacitorBlock(props) }, BlockBehaviour.Properties.of())
    val POWER_CONVERTER: DeferredBlock<Block> = BLOCKS.registerBlock("power_converter", { props -> PowerConverterBlock(props) }, BlockBehaviour.Properties.of())
    val POWER_DISTRIBUTOR: DeferredBlock<Block> = BLOCKS.registerBlock("power_distributor", { props -> PowerDistributorBlock(props) }, BlockBehaviour.Properties.of())
    val CHARGER: DeferredBlock<Block> = BLOCKS.registerBlock("charger", { props -> ChargerBlock(props) }, BlockBehaviour.Properties.of())
    
    val ASSEMBLER: DeferredBlock<Block> = BLOCKS.registerBlock("assembler", { props -> AssemblerBlock(props) }, BlockBehaviour.Properties.of())
    val DISASSEMBLER: DeferredBlock<Block> = BLOCKS.registerBlock("disassembler", { props -> DisassemblerBlock(props) }, BlockBehaviour.Properties.of())
    val PRINTER: DeferredBlock<Block> = BLOCKS.registerBlock("printer", { props -> PrinterBlock(props) }, BlockBehaviour.Properties.of())
    val RAID: DeferredBlock<Block> = BLOCKS.registerBlock("raid", { props -> RaidBlock(props) }, BlockBehaviour.Properties.of())
    
    val ADAPTER: DeferredBlock<Block> = BLOCKS.registerBlock("adapter", { props -> AdapterBlock(props) }, BlockBehaviour.Properties.of())
    val HOLOGRAM_TIER1: DeferredBlock<Block> = BLOCKS.registerBlock("hologram1", { props -> HologramBlock(1, props) }, BlockBehaviour.Properties.of().noOcclusion())
    val HOLOGRAM_TIER2: DeferredBlock<Block> = BLOCKS.registerBlock("hologram2", { props -> HologramBlock(2, props) }, BlockBehaviour.Properties.of().noOcclusion())
    val DISK_DRIVE: DeferredBlock<Block> = BLOCKS.registerBlock("disk_drive", { props -> DiskDriveBlock(props) }, BlockBehaviour.Properties.of())
    
    val GEOLYZER: DeferredBlock<Block> = BLOCKS.registerBlock("geolyzer", { props -> GeolyzerBlock(props) }, BlockBehaviour.Properties.of())
    val MOTION_SENSOR: DeferredBlock<Block> = BLOCKS.registerBlock("motion_sensor", { props -> MotionSensorBlock(props) }, BlockBehaviour.Properties.of())
    val TRANSPOSER: DeferredBlock<Block> = BLOCKS.registerBlock("transposer", { props -> TransposerBlock(props) }, BlockBehaviour.Properties.of())
    val WAYPOINT: DeferredBlock<Block> = BLOCKS.registerBlock("waypoint", { props -> WaypointBlock(props) }, BlockBehaviour.Properties.of())
    
    val NET_SPLITTER: DeferredBlock<Block> = BLOCKS.registerBlock("net_splitter", { props -> NetSplitterBlock(props) }, BlockBehaviour.Properties.of())
    val CHAMELIUM_BLOCK: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("chamelium_block", BlockBehaviour.Properties.of())
    val WEB_DISPLAY: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("web_display", BlockBehaviour.Properties.of())
    
    // Additional blocks for ModItems
    val RACK: DeferredBlock<Block> = BLOCKS.registerBlock("rack", { props -> ServerRackBlock(props) }, BlockBehaviour.Properties.of())
    val MICROCONTROLLER: DeferredBlock<Block> = BLOCKS.registerSimpleBlock("microcontroller", BlockBehaviour.Properties.of())
    
    fun register(eventBus: IEventBus) {
        BLOCKS.register(eventBus)
    }
}
