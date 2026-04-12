package li.cil.oc.datagen

import li.cil.oc.OpenComputers
import li.cil.oc.common.init.ModBlocks
import li.cil.oc.common.init.ModItems
import net.minecraft.core.HolderLookup
import net.minecraft.data.PackOutput
import net.minecraft.tags.BlockTags
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.common.data.BlockTagsProvider
import net.neoforged.neoforge.common.data.ExistingFileHelper
import net.neoforged.neoforge.common.data.LanguageProvider
import net.neoforged.neoforge.data.event.GatherDataEvent
import java.util.concurrent.CompletableFuture

/**
 * Data generation entry point.
 * Registers all data providers for OpenComputers.
 */
@EventBusSubscriber(modid = OpenComputers.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
object DataGenerators {
    
    @SubscribeEvent
    @JvmStatic
    fun gatherData(event: GatherDataEvent) {
        val generator = event.generator
        val output = generator.packOutput
        val lookupProvider = event.lookupProvider
        val existingFileHelper = event.existingFileHelper
        
        // Block tags
        val blockTagsProvider = OCBlockTagsProvider(output, lookupProvider, existingFileHelper)
        generator.addProvider(true, blockTagsProvider)
        
        // Item tags
        generator.addProvider(true, OCItemTagsProvider(output, lookupProvider, blockTagsProvider.contentsGetter(), existingFileHelper))
        
        // Language
        generator.addProvider(true, OCLanguageProvider(output))
    }
}

/**
 * Custom tags for OpenComputers.
 */
object OCTags {
    object Blocks {
        val COMPUTER_BLOCKS: TagKey<Block> = BlockTags.create(OpenComputers.loc("computer_blocks"))
        val NETWORK_CABLES: TagKey<Block> = BlockTags.create(OpenComputers.loc("network_cables"))
        val POWER_BLOCKS: TagKey<Block> = BlockTags.create(OpenComputers.loc("power_blocks"))
        val ROBOT_BREAKABLE: TagKey<Block> = BlockTags.create(OpenComputers.loc("robot_breakable"))
    }
    
    object Items {
        val COMPONENTS: TagKey<Item> = ItemTags.create(OpenComputers.loc("components"))
        val CPUS: TagKey<Item> = ItemTags.create(OpenComputers.loc("cpus"))
        val MEMORY: TagKey<Item> = ItemTags.create(OpenComputers.loc("memory"))
        val STORAGE: TagKey<Item> = ItemTags.create(OpenComputers.loc("storage"))
        val CARDS: TagKey<Item> = ItemTags.create(OpenComputers.loc("cards"))
        val UPGRADES: TagKey<Item> = ItemTags.create(OpenComputers.loc("upgrades"))
        val CRAFTING_MATERIALS: TagKey<Item> = ItemTags.create(OpenComputers.loc("crafting_materials"))
    }
}

/**
 * Block tags provider.
 */
class OCBlockTagsProvider(
    output: PackOutput,
    lookupProvider: CompletableFuture<HolderLookup.Provider>,
    existingFileHelper: ExistingFileHelper?
) : BlockTagsProvider(output, lookupProvider, OpenComputers.MOD_ID, existingFileHelper) {
    
    override fun addTags(provider: HolderLookup.Provider) {
        // Computer blocks
        tag(OCTags.Blocks.COMPUTER_BLOCKS)
            .add(ModBlocks.CASE_TIER1.get())
            .add(ModBlocks.CASE_TIER2.get())
            .add(ModBlocks.CASE_TIER3.get())
            .add(ModBlocks.CASE_CREATIVE.get())
            .add(ModBlocks.RACK.get())
            .add(ModBlocks.MICROCONTROLLER.get())
        
        // Network blocks
        tag(OCTags.Blocks.NETWORK_CABLES)
            .add(ModBlocks.CABLE.get())
            .add(ModBlocks.RELAY.get())
            .add(ModBlocks.ACCESS_POINT.get())
            .add(ModBlocks.NET_SPLITTER.get())
            .add(ModBlocks.SWITCH.get())
        
        // Power blocks
        tag(OCTags.Blocks.POWER_BLOCKS)
            .add(ModBlocks.CAPACITOR.get())
            .add(ModBlocks.CARPETED_CAPACITOR.get())
            .add(ModBlocks.CHARGER.get())
            .add(ModBlocks.POWER_CONVERTER.get())
            .add(ModBlocks.POWER_DISTRIBUTOR.get())
        
        // All OC blocks can be mined with pickaxe
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .addTag(OCTags.Blocks.COMPUTER_BLOCKS)
            .addTag(OCTags.Blocks.NETWORK_CABLES)
            .addTag(OCTags.Blocks.POWER_BLOCKS)
            .add(ModBlocks.SCREEN_TIER1.get())
            .add(ModBlocks.SCREEN_TIER2.get())
            .add(ModBlocks.SCREEN_TIER3.get())
            .add(ModBlocks.ADAPTER.get())
            .add(ModBlocks.ASSEMBLER.get())
            .add(ModBlocks.DISASSEMBLER.get())
            .add(ModBlocks.DISK_DRIVE.get())
            .add(ModBlocks.GEOLYZER.get())
            .add(ModBlocks.HOLOGRAM_TIER1.get())
            .add(ModBlocks.HOLOGRAM_TIER2.get())
            .add(ModBlocks.MOTION_SENSOR.get())
            .add(ModBlocks.PRINTER.get())
            .add(ModBlocks.RAID.get())
            .add(ModBlocks.REDSTONE_IO.get())
            .add(ModBlocks.TRANSPOSER.get())
            .add(ModBlocks.WAYPOINT.get())
            .add(ModBlocks.CHAMELIUM_BLOCK.get())
    }
}

/**
 * Item tags provider.
 */
class OCItemTagsProvider(
    output: PackOutput,
    lookupProvider: CompletableFuture<HolderLookup.Provider>,
    blockTags: CompletableFuture<TagLookup<Block>>,
    existingFileHelper: ExistingFileHelper?
) : net.minecraft.data.tags.ItemTagsProvider(output, lookupProvider, blockTags, OpenComputers.MOD_ID, existingFileHelper) {
    
    override fun addTags(provider: HolderLookup.Provider) {
        // CPUs
        tag(OCTags.Items.CPUS)
            .add(ModItems.CPU_TIER1.get())
            .add(ModItems.CPU_TIER2.get())
            .add(ModItems.CPU_TIER3.get())
            .add(ModItems.CPU_CREATIVE.get())
            .add(ModItems.APU_TIER1.get())
            .add(ModItems.APU_TIER2.get())
        
        // Memory
        tag(OCTags.Items.MEMORY)
            .add(ModItems.MEMORY_TIER1.get())
            .add(ModItems.MEMORY_TIER1_5.get())
            .add(ModItems.MEMORY_TIER2.get())
            .add(ModItems.MEMORY_TIER2_5.get())
            .add(ModItems.MEMORY_TIER3.get())
            .add(ModItems.MEMORY_TIER3_5.get())
        
        // Storage
        tag(OCTags.Items.STORAGE)
            .add(ModItems.HDD_TIER1.get())
            .add(ModItems.HDD_TIER2.get())
            .add(ModItems.HDD_TIER3.get())
            .add(ModItems.FLOPPY.get())
            .add(ModItems.EEPROM.get())
        
        // Cards
        tag(OCTags.Items.CARDS)
            .add(ModItems.GPU_TIER1.get())
            .add(ModItems.GPU_TIER2.get())
            .add(ModItems.GPU_TIER3.get())
            .add(ModItems.NETWORK_CARD.get())
            .add(ModItems.WIRELESS_CARD_TIER1.get())
            .add(ModItems.WIRELESS_CARD_TIER2.get())
            .add(ModItems.INTERNET_CARD.get())
            .add(ModItems.LINKED_CARD.get())
            .add(ModItems.REDSTONE_CARD_TIER1.get())
            .add(ModItems.REDSTONE_CARD_TIER2.get())
            .add(ModItems.DATA_CARD_TIER1.get())
            .add(ModItems.DATA_CARD_TIER2.get())
            .add(ModItems.DATA_CARD_TIER3.get())
            .add(ModItems.WORLD_SENSOR_CARD.get())
        
        // Crafting materials
        tag(OCTags.Items.CRAFTING_MATERIALS)
            .add(ModItems.CHAMELIUM.get())
            .add(ModItems.TRANSISTOR.get())
            .add(ModItems.MICROCHIP_TIER1.get())
            .add(ModItems.MICROCHIP_TIER2.get())
            .add(ModItems.MICROCHIP_TIER3.get())
            .add(ModItems.ALU.get())
            .add(ModItems.CONTROL_UNIT.get())
            .add(ModItems.DISK_PLATTER.get())
            .add(ModItems.CARD_BASE.get())
            .add(ModItems.CIRCUIT_TIER1.get())
            .add(ModItems.CIRCUIT_TIER2.get())
            .add(ModItems.CIRCUIT_TIER3.get())
            .add(ModItems.RAW_CIRCUIT_BOARD.get())
            .add(ModItems.CUTTING_WIRE.get())
            .add(ModItems.ACID.get())
    }
}

/**
 * Language provider for localization.
 */
class OCLanguageProvider(output: PackOutput) : LanguageProvider(output, OpenComputers.MOD_ID, "en_us") {
    
    override fun addTranslations() {
        // Creative tab
        add("itemGroup.${OpenComputers.MOD_ID}", "OpenComputers")
        
        // === Blocks ===
        addBlock(ModBlocks.CASE_TIER1, "Computer Case (Tier 1)")
        addBlock(ModBlocks.CASE_TIER2, "Computer Case (Tier 2)")
        addBlock(ModBlocks.CASE_TIER3, "Computer Case (Tier 3)")
        addBlock(ModBlocks.CASE_CREATIVE, "Creative Computer Case")
        
        addBlock(ModBlocks.SCREEN_TIER1, "Screen (Tier 1)")
        addBlock(ModBlocks.SCREEN_TIER2, "Screen (Tier 2)")
        addBlock(ModBlocks.SCREEN_TIER3, "Screen (Tier 3)")
        
        addBlock(ModBlocks.KEYBOARD, "Keyboard")
        addBlock(ModBlocks.REDSTONE_IO, "Redstone I/O")
        
        addBlock(ModBlocks.CABLE, "Cable")
        addBlock(ModBlocks.RELAY, "Relay")
        addBlock(ModBlocks.ACCESS_POINT, "Access Point")
        addBlock(ModBlocks.NET_SPLITTER, "Net Splitter")
        addBlock(ModBlocks.SWITCH, "Switch")
        
        addBlock(ModBlocks.CAPACITOR, "Capacitor")
        addBlock(ModBlocks.CARPETED_CAPACITOR, "Carpeted Capacitor")
        addBlock(ModBlocks.POWER_CONVERTER, "Power Converter")
        addBlock(ModBlocks.POWER_DISTRIBUTOR, "Power Distributor")
        addBlock(ModBlocks.CHARGER, "Charger")
        
        addBlock(ModBlocks.ADAPTER, "Adapter")
        addBlock(ModBlocks.ASSEMBLER, "Electronics Assembler")
        addBlock(ModBlocks.DISASSEMBLER, "Disassembler")
        addBlock(ModBlocks.DISK_DRIVE, "Disk Drive")
        addBlock(ModBlocks.RAID, "RAID")
        
        addBlock(ModBlocks.GEOLYZER, "Geolyzer")
        addBlock(ModBlocks.HOLOGRAM_TIER1, "Hologram Projector (Tier 1)")
        addBlock(ModBlocks.HOLOGRAM_TIER2, "Hologram Projector (Tier 2)")
        addBlock(ModBlocks.MOTION_SENSOR, "Motion Sensor")
        addBlock(ModBlocks.PRINTER, "3D Printer")
        addBlock(ModBlocks.RACK, "Server Rack")
        addBlock(ModBlocks.TRANSPOSER, "Transposer")
        addBlock(ModBlocks.WAYPOINT, "Waypoint")
        
        addBlock(ModBlocks.MICROCONTROLLER, "Microcontroller")
        addBlock(ModBlocks.CHAMELIUM_BLOCK, "Chamelium Block")
        addBlock(ModBlocks.WEB_DISPLAY, "Web Display")
        addBlock(ModBlocks.PRINT, "3D Print")
        
        // === CPUs ===
        addItem(ModItems.CPU_TIER1, "Central Processing Unit (Tier 1)")
        addItem(ModItems.CPU_TIER2, "Central Processing Unit (Tier 2)")
        addItem(ModItems.CPU_TIER3, "Central Processing Unit (Tier 3)")
        addItem(ModItems.CPU_CREATIVE, "Creative Central Processing Unit")
        addItem(ModItems.APU_TIER1, "Accelerated Processing Unit (Tier 1)")
        addItem(ModItems.APU_TIER2, "Accelerated Processing Unit (Tier 2)")
        
        // === Memory ===
        addItem(ModItems.MEMORY_TIER1, "Memory (Tier 1)")
        addItem(ModItems.MEMORY_TIER1_5, "Memory (Tier 1.5)")
        addItem(ModItems.MEMORY_TIER2, "Memory (Tier 2)")
        addItem(ModItems.MEMORY_TIER2_5, "Memory (Tier 2.5)")
        addItem(ModItems.MEMORY_TIER3, "Memory (Tier 3)")
        addItem(ModItems.MEMORY_TIER3_5, "Memory (Tier 3.5)")
        
        // === Graphics Cards ===
        addItem(ModItems.GPU_TIER1, "Graphics Card (Tier 1)")
        addItem(ModItems.GPU_TIER2, "Graphics Card (Tier 2)")
        addItem(ModItems.GPU_TIER3, "Graphics Card (Tier 3)")
        
        // === Storage ===
        addItem(ModItems.HDD_TIER1, "Hard Disk Drive (Tier 1)")
        addItem(ModItems.HDD_TIER2, "Hard Disk Drive (Tier 2)")
        addItem(ModItems.HDD_TIER3, "Hard Disk Drive (Tier 3)")
        addItem(ModItems.FLOPPY, "Floppy Disk")
        addItem(ModItems.EEPROM, "EEPROM")
        
        // === Network Cards ===
        addItem(ModItems.NETWORK_CARD, "Network Card")
        addItem(ModItems.WIRELESS_CARD_TIER1, "Wireless Network Card (Tier 1)")
        addItem(ModItems.WIRELESS_CARD_TIER2, "Wireless Network Card (Tier 2)")
        addItem(ModItems.INTERNET_CARD, "Internet Card")
        addItem(ModItems.LINKED_CARD, "Linked Card")
        
        // === Other Cards ===
        addItem(ModItems.REDSTONE_CARD_TIER1, "Redstone Card (Tier 1)")
        addItem(ModItems.REDSTONE_CARD_TIER2, "Redstone Card (Tier 2)")
        addItem(ModItems.DATA_CARD_TIER1, "Data Card (Tier 1)")
        addItem(ModItems.DATA_CARD_TIER2, "Data Card (Tier 2)")
        addItem(ModItems.DATA_CARD_TIER3, "Data Card (Tier 3)")
        addItem(ModItems.WORLD_SENSOR_CARD, "World Sensor Card")
        
        // === Upgrades ===
        addItem(ModItems.UPGRADE_ANGEL, "Angel Upgrade")
        addItem(ModItems.UPGRADE_BATTERY_TIER1, "Battery Upgrade (Tier 1)")
        addItem(ModItems.UPGRADE_BATTERY_TIER2, "Battery Upgrade (Tier 2)")
        addItem(ModItems.UPGRADE_BATTERY_TIER3, "Battery Upgrade (Tier 3)")
        addItem(ModItems.UPGRADE_CHUNKLOADER, "Chunkloader Upgrade")
        addItem(ModItems.UPGRADE_CRAFTING, "Crafting Upgrade")
        addItem(ModItems.UPGRADE_DATABASE_TIER1, "Database Upgrade (Tier 1)")
        addItem(ModItems.UPGRADE_DATABASE_TIER2, "Database Upgrade (Tier 2)")
        addItem(ModItems.UPGRADE_DATABASE_TIER3, "Database Upgrade (Tier 3)")
        addItem(ModItems.UPGRADE_EXPERIENCE, "Experience Upgrade")
        addItem(ModItems.UPGRADE_GENERATOR, "Generator Upgrade")
        addItem(ModItems.UPGRADE_HOVER_TIER1, "Hover Upgrade (Tier 1)")
        addItem(ModItems.UPGRADE_HOVER_TIER2, "Hover Upgrade (Tier 2)")
        addItem(ModItems.UPGRADE_INVENTORY, "Inventory Upgrade")
        addItem(ModItems.UPGRADE_INVENTORY_CONTROLLER, "Inventory Controller Upgrade")
        addItem(ModItems.UPGRADE_LEASH, "Leash Upgrade")
        addItem(ModItems.UPGRADE_MFU, "MFU")
        addItem(ModItems.UPGRADE_NAVIGATION, "Navigation Upgrade")
        addItem(ModItems.UPGRADE_PISTON, "Piston Upgrade")
        addItem(ModItems.UPGRADE_SIGN, "Sign I/O Upgrade")
        addItem(ModItems.UPGRADE_SOLAR_GENERATOR, "Solar Generator Upgrade")
        addItem(ModItems.UPGRADE_TANK, "Tank Upgrade")
        addItem(ModItems.UPGRADE_TANK_CONTROLLER, "Tank Controller Upgrade")
        addItem(ModItems.UPGRADE_TRACTOR_BEAM, "Tractor Beam Upgrade")
        addItem(ModItems.UPGRADE_TRADING, "Trading Upgrade")
        
        // === Containers ===
        addItem(ModItems.CARD_CONTAINER_TIER1, "Card Container (Tier 1)")
        addItem(ModItems.CARD_CONTAINER_TIER2, "Card Container (Tier 2)")
        addItem(ModItems.CARD_CONTAINER_TIER3, "Card Container (Tier 3)")
        addItem(ModItems.UPGRADE_CONTAINER_TIER1, "Upgrade Container (Tier 1)")
        addItem(ModItems.UPGRADE_CONTAINER_TIER2, "Upgrade Container (Tier 2)")
        addItem(ModItems.UPGRADE_CONTAINER_TIER3, "Upgrade Container (Tier 3)")
        
        // === Special Items ===
        addItem(ModItems.TABLET, "Tablet")
        addItem(ModItems.HOVER_BOOTS, "Hover Boots")
        addItem(ModItems.NANOMACHINES, "Nanomachines")
        addItem(ModItems.TERMINAL, "Terminal")
        addItem(ModItems.ANALYZER, "Analyzer")
        addItem(ModItems.MANUAL, "Manual")
        addItem(ModItems.WRENCH, "Wrench")
        addItem(ModItems.DEBUG_CARD, "Debug Card")
        addItem(ModItems.LASER_POINTER, "Laser Pointer")
        addItem(ModItems.REMOTE_KEYBOARD, "Remote Keyboard")
        
        // === Drone/Robot/Tablet Cases ===
        addItem(ModItems.DRONE_CASE_TIER1, "Drone Case (Tier 1)")
        addItem(ModItems.DRONE_CASE_TIER2, "Drone Case (Tier 2)")
        addItem(ModItems.DRONE_CASE_CREATIVE, "Creative Drone Case")
        addItem(ModItems.ROBOT_CASE_TIER1, "Robot Case (Tier 1)")
        addItem(ModItems.ROBOT_CASE_TIER2, "Robot Case (Tier 2)")
        addItem(ModItems.ROBOT_CASE_TIER3, "Robot Case (Tier 3)")
        addItem(ModItems.ROBOT_CASE_CREATIVE, "Creative Robot Case")
        addItem(ModItems.MICROCONTROLLER_CASE_TIER1, "Microcontroller Case (Tier 1)")
        addItem(ModItems.MICROCONTROLLER_CASE_TIER2, "Microcontroller Case (Tier 2)")
        addItem(ModItems.MICROCONTROLLER_CASE_CREATIVE, "Creative Microcontroller Case")
        addItem(ModItems.TABLET_CASE_TIER1, "Tablet Case (Tier 1)")
        addItem(ModItems.TABLET_CASE_TIER2, "Tablet Case (Tier 2)")
        addItem(ModItems.TABLET_CASE_CREATIVE, "Creative Tablet Case")
        
        // === Servers ===
        addItem(ModItems.SERVER_TIER1, "Server (Tier 1)")
        addItem(ModItems.SERVER_TIER2, "Server (Tier 2)")
        addItem(ModItems.SERVER_TIER3, "Server (Tier 3)")
        addItem(ModItems.SERVER_CREATIVE, "Creative Server")
        addItem(ModItems.REMOTE_TERMINAL, "Remote Terminal")
        addItem(ModItems.TERMINAL_SERVER, "Terminal Server")
        
        // === Crafting Materials ===
        addItem(ModItems.CHAMELIUM, "Chamelium")
        addItem(ModItems.TRANSISTOR, "Transistor")
        addItem(ModItems.MICROCHIP_TIER1, "Microchip (Tier 1)")
        addItem(ModItems.MICROCHIP_TIER2, "Microchip (Tier 2)")
        addItem(ModItems.MICROCHIP_TIER3, "Microchip (Tier 3)")
        addItem(ModItems.ALU, "Arithmetic Logic Unit")
        addItem(ModItems.CONTROL_UNIT, "Control Unit")
        addItem(ModItems.DISK_PLATTER, "Disk Platter")
        addItem(ModItems.CARD_BASE, "Card Base")
        addItem(ModItems.CIRCUIT_TIER1, "Circuit Board")
        addItem(ModItems.CIRCUIT_TIER2, "Printed Circuit Board")
        addItem(ModItems.CIRCUIT_TIER3, "Integrated Circuit")
        addItem(ModItems.CIRCUIT_TIER4, "Processor")
        addItem(ModItems.RAW_CIRCUIT_BOARD, "Raw Circuit Board")
        addItem(ModItems.CUTTING_WIRE, "Cutting Wire")
        addItem(ModItems.ACID, "Acid")
        addItem(ModItems.INK_CARTRIDGE, "Ink Cartridge")
        addItem(ModItems.INK_CARTRIDGE_EMPTY, "Empty Ink Cartridge")
        addItem(ModItems.INTERWEB, "Interweb")
        addItem(ModItems.ARROW_KEYS, "Arrow Keys")
        addItem(ModItems.BUTTON_GROUP, "Button Group")
        addItem(ModItems.NUMPAD, "Numeric Keypad")
        addItem(ModItems.COMPONENT_BUS_TIER1, "Component Bus (Tier 1)")
        addItem(ModItems.COMPONENT_BUS_TIER2, "Component Bus (Tier 2)")
        addItem(ModItems.COMPONENT_BUS_TIER3, "Component Bus (Tier 3)")
        addItem(ModItems.COMPONENT_BUS_CREATIVE, "Creative Component Bus")
        
        // === Tooltips ===
        add("tooltip.${OpenComputers.MOD_ID}.tier", "Tier: %s")
        add("tooltip.${OpenComputers.MOD_ID}.energy", "Energy: %s/%s")
        add("tooltip.${OpenComputers.MOD_ID}.address", "Address: %s")
        add("tooltip.${OpenComputers.MOD_ID}.empty", "Empty")
        
        // === Messages ===
        add("message.${OpenComputers.MOD_ID}.analyzer.address", "§9Address: §f%s")
        add("message.${OpenComputers.MOD_ID}.analyzer.type", "§9Type: §f%s")
        add("message.${OpenComputers.MOD_ID}.analyzer.energy", "§9Energy: §f%s/%s")
        add("message.${OpenComputers.MOD_ID}.analyzer.components", "§9Components: §f%s")
        
        add("message.${OpenComputers.MOD_ID}.computer.turnon", "Computer turning on...")
        add("message.${OpenComputers.MOD_ID}.computer.turnoff", "Computer turned off.")
        add("message.${OpenComputers.MOD_ID}.computer.crash", "Computer crashed: %s")
        
        // === GUI ===
        add("gui.${OpenComputers.MOD_ID}.case", "Computer")
        add("gui.${OpenComputers.MOD_ID}.screen", "Screen")
        add("gui.${OpenComputers.MOD_ID}.rack", "Server Rack")
        add("gui.${OpenComputers.MOD_ID}.assembler", "Assembler")
        add("gui.${OpenComputers.MOD_ID}.disassembler", "Disassembler")
        add("gui.${OpenComputers.MOD_ID}.printer", "3D Printer")
        add("gui.${OpenComputers.MOD_ID}.robot", "Robot")
        add("gui.${OpenComputers.MOD_ID}.tablet", "Tablet")
        add("gui.${OpenComputers.MOD_ID}.drone", "Drone")
        
        add("gui.${OpenComputers.MOD_ID}.power", "Power")
        add("gui.${OpenComputers.MOD_ID}.start", "Start")
        add("gui.${OpenComputers.MOD_ID}.stop", "Stop")
        add("gui.${OpenComputers.MOD_ID}.assembling", "Assembling...")
        add("gui.${OpenComputers.MOD_ID}.disassembling", "Disassembling...")
        
        // === Keys ===
        add("key.${OpenComputers.MOD_ID}.clipboard_paste", "Paste Clipboard")
        add("key.categories.${OpenComputers.MOD_ID}", "OpenComputers")
        
        // === Commands ===
        add("commands.${OpenComputers.MOD_ID}.oc.usage", "OpenComputers commands")
        add("commands.${OpenComputers.MOD_ID}.oc.debug", "Debug command")
    }
}
