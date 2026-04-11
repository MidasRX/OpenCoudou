package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.common.item.*
import li.cil.oc.common.item.MaterialItems.CuttingWireItem
import li.cil.oc.common.item.MaterialItems.AcidItem
import li.cil.oc.common.item.MaterialItems.RawCircuitBoardItem
import li.cil.oc.common.item.MaterialItems.CircuitBoardItem
import li.cil.oc.common.item.MaterialItems.PrintedCircuitBoardItem
import li.cil.oc.common.item.MaterialItems.CardBaseItem
import li.cil.oc.common.item.MaterialItems.TransistorItem
import li.cil.oc.common.item.MaterialItems.MicrochipItem
import li.cil.oc.common.item.MaterialItems.ALUItem
import li.cil.oc.common.item.MaterialItems.ControlUnitItem
import li.cil.oc.common.item.MaterialItems.DiskPlatterItem
import li.cil.oc.common.item.MaterialItems.InterwebItem
import li.cil.oc.common.item.MaterialItems.ButtonGroupItem
import li.cil.oc.common.item.MaterialItems.ArrowKeysItem
import li.cil.oc.common.item.MaterialItems.NumpadItem
import li.cil.oc.common.item.MaterialItems.ChameliumItem
import li.cil.oc.common.item.MaterialItems.InkCartridgeItem
import li.cil.oc.common.item.MaterialItems.DroneCaseItem
import li.cil.oc.common.item.MaterialItems.RobotCaseItem
import li.cil.oc.common.item.MaterialItems.MicrocontrollerCaseItem
import li.cil.oc.common.item.MaterialItems.TabletCaseItem
import li.cil.oc.common.item.MaterialItems.NanomachinesItem
import li.cil.oc.common.item.ToolItems.AnalyzerItem
import li.cil.oc.common.item.ToolItems.WrenchItem
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister

/**
 * Registration for all OpenComputers items.
 */
object ModItems {
    private val ITEMS: DeferredRegister.Items = DeferredRegister.createItems(OpenComputers.MOD_ID)
    
    // ========================================
    // Block Items
    // ========================================
    
    val CASE_TIER1_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.CASE_TIER1)
    val CASE_TIER2_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.CASE_TIER2)
    val CASE_TIER3_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.CASE_TIER3)
    val CASE_CREATIVE_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.CASE_CREATIVE)
    
    val SCREEN_TIER1_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.SCREEN_TIER1)
    val SCREEN_TIER2_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.SCREEN_TIER2)
    val SCREEN_TIER3_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.SCREEN_TIER3)
    
    val KEYBOARD_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.KEYBOARD)
    val REDSTONE_IO_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.REDSTONE_IO)
    
    val CABLE_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.CABLE)
    val RELAY_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.RELAY)
    val ACCESS_POINT_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.ACCESS_POINT)
    
    val CAPACITOR_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.CAPACITOR)
    val POWER_CONVERTER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.POWER_CONVERTER)
    val POWER_DISTRIBUTOR_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.POWER_DISTRIBUTOR)
    val CHARGER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.CHARGER)
    
    val ADAPTER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.ADAPTER)
    val TRANSPOSER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.TRANSPOSER)
    val DISK_DRIVE_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.DISK_DRIVE)
    val RAID_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.RAID)
    
    val GEOLYZER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.GEOLYZER)
    val MOTION_SENSOR_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.MOTION_SENSOR)
    val WAYPOINT_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.WAYPOINT)
    val NET_SPLITTER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.NET_SPLITTER)
    
    val RACK_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.RACK)
    val ASSEMBLER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.ASSEMBLER)
    val DISASSEMBLER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.DISASSEMBLER)
    val PRINTER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.PRINTER)
    
    val HOLOGRAM_TIER1_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.HOLOGRAM_TIER1)
    val HOLOGRAM_TIER2_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.HOLOGRAM_TIER2)
    
    val MICROCONTROLLER_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.MICROCONTROLLER)
    val CHAMELIUM_BLOCK_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.CHAMELIUM_BLOCK)
    val WEB_DISPLAY_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem(ModBlocks.WEB_DISPLAY)
    
    // ========================================
    // CPUs
    // ========================================
    
    val CPU_TIER1: DeferredItem<CPUItem> = ITEMS.registerItem("cpu1") { props ->
        CPUItem(props, 0)
    }
    
    val CPU_TIER2: DeferredItem<CPUItem> = ITEMS.registerItem("cpu2") { props ->
        CPUItem(props, 1)
    }
    
    val CPU_TIER3: DeferredItem<CPUItem> = ITEMS.registerItem("cpu3") { props ->
        CPUItem(props, 2)
    }
    
    val CPU_CREATIVE: DeferredItem<CPUItem> = ITEMS.registerItem("cpu_creative") { props ->
        CPUItem(props, 3)
    }
    
    // ========================================
    // Memory
    // ========================================
    
    val MEMORY_TIER1: DeferredItem<MemoryItem> = ITEMS.registerItem("ram1") { props ->
        MemoryItem(props, 0)
    }
    
    val MEMORY_TIER1_5: DeferredItem<MemoryItem> = ITEMS.registerItem("ram1_5") { props ->
        MemoryItem(props, 1)
    }
    
    val MEMORY_TIER2: DeferredItem<MemoryItem> = ITEMS.registerItem("ram2") { props ->
        MemoryItem(props, 2)
    }
    
    val MEMORY_TIER2_5: DeferredItem<MemoryItem> = ITEMS.registerItem("ram2_5") { props ->
        MemoryItem(props, 3)
    }
    
    val MEMORY_TIER3: DeferredItem<MemoryItem> = ITEMS.registerItem("ram3") { props ->
        MemoryItem(props, 4)
    }
    
    val MEMORY_TIER3_5: DeferredItem<MemoryItem> = ITEMS.registerItem("ram3_5") { props ->
        MemoryItem(props, 5)
    }
    
    // ========================================
    // Graphics Cards
    // ========================================
    
    val GPU_TIER1: DeferredItem<GPUItem> = ITEMS.registerItem("graphicscard1") { props ->
        GPUItem(props, 0)
    }
    
    val GPU_TIER2: DeferredItem<GPUItem> = ITEMS.registerItem("graphicscard2") { props ->
        GPUItem(props, 1)
    }
    
    val GPU_TIER3: DeferredItem<GPUItem> = ITEMS.registerItem("graphicscard3") { props ->
        GPUItem(props, 2)
    }
    
    // ========================================
    // Storage
    // ========================================
    
    val HDD_TIER1: DeferredItem<HDDItem> = ITEMS.registerItem("hdd1") { props ->
        HDDItem(props, 0)
    }
    
    val HDD_TIER2: DeferredItem<HDDItem> = ITEMS.registerItem("hdd2") { props ->
        HDDItem(props, 1)
    }
    
    val HDD_TIER3: DeferredItem<HDDItem> = ITEMS.registerItem("hdd3") { props ->
        HDDItem(props, 2)
    }
    
    val FLOPPY: DeferredItem<FloppyItem> = ITEMS.registerItem("floppy", ::FloppyItem)
    
    val EEPROM: DeferredItem<EEPROMItem> = ITEMS.registerItem("eeprom", ::EEPROMItem)
    
    // ========================================
    // Network Cards
    // ========================================
    
    val NETWORK_CARD: DeferredItem<NetworkCardItem> = ITEMS.registerItem("lancard", ::NetworkCardItem)
    
    val WIRELESS_CARD_TIER1: DeferredItem<WirelessCardItem> = ITEMS.registerItem("wlancard1") { props ->
        WirelessCardItem(props, 0)
    }
    
    val WIRELESS_CARD_TIER2: DeferredItem<WirelessCardItem> = ITEMS.registerItem("wlancard2") { props ->
        WirelessCardItem(props, 1)
    }
    
    val INTERNET_CARD: DeferredItem<InternetCardItem> = ITEMS.registerItem("internetcard", ::InternetCardItem)
    
    val LINKED_CARD: DeferredItem<LinkedCardItem> = ITEMS.registerItem("linkedcard", ::LinkedCardItem)
    
    // ========================================
    // Other Cards
    // ========================================
    
    val REDSTONE_CARD_TIER1: DeferredItem<RedstoneCardItem> = ITEMS.registerItem("redstonecard1") { props ->
        RedstoneCardItem(props, 0)
    }
    
    val REDSTONE_CARD_TIER2: DeferredItem<RedstoneCardItem> = ITEMS.registerItem("redstonecard2") { props ->
        RedstoneCardItem(props, 1)
    }
    
    val DATA_CARD_TIER1: DeferredItem<DataCardItem> = ITEMS.registerItem("datacard1") { props ->
        DataCardItem(props, 0)
    }
    
    val DATA_CARD_TIER2: DeferredItem<DataCardItem> = ITEMS.registerItem("datacard2") { props ->
        DataCardItem(props, 1)
    }
    
    val DATA_CARD_TIER3: DeferredItem<DataCardItem> = ITEMS.registerItem("datacard3") { props ->
        DataCardItem(props, 2)
    }
    
    val WORLD_SENSOR_CARD: DeferredItem<WorldSensorCardItem> = ITEMS.registerItem("worldsensorcard", ::WorldSensorCardItem)
    
    // ========================================
    // Upgrades
    // ========================================
    
    val UPGRADE_ANGEL: DeferredItem<AngelUpgradeItem> = ITEMS.registerItem("angelupgrade", ::AngelUpgradeItem)
    
    val UPGRADE_BATTERY_TIER1: DeferredItem<BatteryUpgradeItem> = ITEMS.registerItem("batteryupgrade1") { props ->
        BatteryUpgradeItem(props, 0)
    }
    
    val UPGRADE_BATTERY_TIER2: DeferredItem<BatteryUpgradeItem> = ITEMS.registerItem("batteryupgrade2") { props ->
        BatteryUpgradeItem(props, 1)
    }
    
    val UPGRADE_BATTERY_TIER3: DeferredItem<BatteryUpgradeItem> = ITEMS.registerItem("batteryupgrade3") { props ->
        BatteryUpgradeItem(props, 2)
    }
    
    val UPGRADE_CHUNKLOADER: DeferredItem<ChunkloaderUpgradeItem> = ITEMS.registerItem("chunkloaderupgrade", ::ChunkloaderUpgradeItem)
    
    val UPGRADE_CRAFTING: DeferredItem<CraftingUpgradeItem> = ITEMS.registerItem("craftingupgrade", ::CraftingUpgradeItem)
    
    val UPGRADE_DATABASE_TIER1: DeferredItem<DatabaseUpgradeItem> = ITEMS.registerItem("databaseupgrade1") { props ->
        DatabaseUpgradeItem(props, 0)
    }
    
    val UPGRADE_DATABASE_TIER2: DeferredItem<DatabaseUpgradeItem> = ITEMS.registerItem("databaseupgrade2") { props ->
        DatabaseUpgradeItem(props, 1)
    }
    
    val UPGRADE_DATABASE_TIER3: DeferredItem<DatabaseUpgradeItem> = ITEMS.registerItem("databaseupgrade3") { props ->
        DatabaseUpgradeItem(props, 2)
    }
    
    val UPGRADE_EXPERIENCE: DeferredItem<ExperienceUpgradeItem> = ITEMS.registerItem("experienceupgrade", ::ExperienceUpgradeItem)
    
    val UPGRADE_GENERATOR: DeferredItem<GeneratorUpgradeItem> = ITEMS.registerItem("generatorupgrade", ::GeneratorUpgradeItem)
    
    val UPGRADE_HOVER_TIER1: DeferredItem<HoverUpgradeItem> = ITEMS.registerItem("hoverupgrade1") { props ->
        HoverUpgradeItem(props, 0)
    }
    
    val UPGRADE_HOVER_TIER2: DeferredItem<HoverUpgradeItem> = ITEMS.registerItem("hoverupgrade2") { props ->
        HoverUpgradeItem(props, 1)
    }
    
    val UPGRADE_INVENTORY: DeferredItem<InventoryUpgradeItem> = ITEMS.registerItem("inventoryupgrade", ::InventoryUpgradeItem)
    
    val UPGRADE_INVENTORY_CONTROLLER: DeferredItem<InventoryControllerUpgradeItem> = ITEMS.registerItem("inventorycontrollerupgrade", ::InventoryControllerUpgradeItem)
    
    val UPGRADE_LEASH: DeferredItem<LeashUpgradeItem> = ITEMS.registerItem("leashupgrade", ::LeashUpgradeItem)
    
    val UPGRADE_MFU: DeferredItem<MFUItem> = ITEMS.registerItem("mfu", ::MFUItem)
    
    val UPGRADE_NAVIGATION: DeferredItem<NavigationUpgradeItem> = ITEMS.registerItem("navigationupgrade", ::NavigationUpgradeItem)
    
    val UPGRADE_PISTON: DeferredItem<PistonUpgradeItem> = ITEMS.registerItem("pistonupgrade", ::PistonUpgradeItem)
    
    val UPGRADE_SIGN: DeferredItem<SignIOUpgradeItem> = ITEMS.registerItem("signupgrade", ::SignIOUpgradeItem)
    
    val UPGRADE_SOLAR_GENERATOR: DeferredItem<SolarGeneratorUpgradeItem> = ITEMS.registerItem("solargeneratorupgrade", ::SolarGeneratorUpgradeItem)
    
    val UPGRADE_TANK: DeferredItem<TankUpgradeItem> = ITEMS.registerItem("tankupgrade", ::TankUpgradeItem)
    
    val UPGRADE_TANK_CONTROLLER: DeferredItem<TankControllerUpgradeItem> = ITEMS.registerItem("tankcontrollerupgrade", ::TankControllerUpgradeItem)
    
    val UPGRADE_TRACTOR_BEAM: DeferredItem<TractorBeamUpgradeItem> = ITEMS.registerItem("tractorupgrade", ::TractorBeamUpgradeItem)
    
    val UPGRADE_TRADING: DeferredItem<TradingUpgradeItem> = ITEMS.registerItem("tradingupgrade", ::TradingUpgradeItem)
    
    // ========================================
    // Containers
    // ========================================
    
    val CARD_CONTAINER_TIER1: DeferredItem<ContainerItem> = ITEMS.registerItem("cardcontainer1") { props ->
        ContainerItem("card", 0, props)
    }
    
    val CARD_CONTAINER_TIER2: DeferredItem<ContainerItem> = ITEMS.registerItem("cardcontainer2") { props ->
        ContainerItem("card", 1, props)
    }
    
    val CARD_CONTAINER_TIER3: DeferredItem<ContainerItem> = ITEMS.registerItem("cardcontainer3") { props ->
        ContainerItem("card", 2, props)
    }
    
    val UPGRADE_CONTAINER_TIER1: DeferredItem<ContainerItem> = ITEMS.registerItem("upgradecontainer1") { props ->
        ContainerItem("upgrade", 0, props)
    }
    
    val UPGRADE_CONTAINER_TIER2: DeferredItem<ContainerItem> = ITEMS.registerItem("upgradecontainer2") { props ->
        ContainerItem("upgrade", 1, props)
    }
    
    val UPGRADE_CONTAINER_TIER3: DeferredItem<ContainerItem> = ITEMS.registerItem("upgradecontainer3") { props ->
        ContainerItem("upgrade", 2, props)
    }
    
    // ========================================
    // Special Items
    // ========================================
    
    val TABLET: DeferredItem<TabletItem> = ITEMS.registerItem("tablet", ::TabletItem)
    
    val DRONE_CASE_TIER1: DeferredItem<DroneCaseItem> = ITEMS.registerItem("dronecase1") { props ->
        DroneCaseItem(props, 0)
    }
    
    val DRONE_CASE_TIER2: DeferredItem<DroneCaseItem> = ITEMS.registerItem("dronecase2") { props ->
        DroneCaseItem(props, 1)
    }
    
    val DRONE_CASE_CREATIVE: DeferredItem<DroneCaseItem> = ITEMS.registerItem("dronecase_creative") { props ->
        DroneCaseItem(props, 2)
    }
    
    val ROBOT_CASE_TIER1: DeferredItem<RobotCaseItem> = ITEMS.registerItem("robotcase1") { props ->
        RobotCaseItem(props, 0)
    }
    
    val ROBOT_CASE_TIER2: DeferredItem<RobotCaseItem> = ITEMS.registerItem("robotcase2") { props ->
        RobotCaseItem(props, 1)
    }
    
    val ROBOT_CASE_TIER3: DeferredItem<RobotCaseItem> = ITEMS.registerItem("robotcase3") { props ->
        RobotCaseItem(props, 2)
    }
    
    val ROBOT_CASE_CREATIVE: DeferredItem<RobotCaseItem> = ITEMS.registerItem("robotcase_creative") { props ->
        RobotCaseItem(props, 3)
    }
    
    val HOVER_BOOTS: DeferredItem<HoverBootsItem> = ITEMS.registerItem("hoverboots", ::HoverBootsItem)
    
    val NANOMACHINES: DeferredItem<NanomachinesItem> = ITEMS.registerItem("nanomachines", ::NanomachinesItem)
    
    val TERMINAL: DeferredItem<TerminalItem> = ITEMS.registerItem("terminal", ::TerminalItem)
    
    val ANALYZER: DeferredItem<AnalyzerItem> = ITEMS.registerItem("analyzer", ::AnalyzerItem)
    
    val MANUAL: DeferredItem<ManualItem> = ITEMS.registerItem("manual", ::ManualItem)
    
    // ========================================
    // Materials
    // ========================================
    
    val CHAMELIUM: DeferredItem<Item> = ITEMS.registerSimpleItem("chamelium")
    
    val CIRCUIT_TIER1: DeferredItem<Item> = ITEMS.registerSimpleItem("circuitboard")
    val CIRCUIT_TIER2: DeferredItem<Item> = ITEMS.registerSimpleItem("printedcircuitboard")
    val CIRCUIT_TIER3: DeferredItem<Item> = ITEMS.registerSimpleItem("integratedcircuit")
    val CIRCUIT_TIER4: DeferredItem<Item> = ITEMS.registerSimpleItem("processor")
    
    val TRANSISTOR: DeferredItem<Item> = ITEMS.registerSimpleItem("transistor")
    val MICROCHIP_TIER1: DeferredItem<Item> = ITEMS.registerSimpleItem("chip1")
    val MICROCHIP_TIER2: DeferredItem<Item> = ITEMS.registerSimpleItem("chip2")
    val MICROCHIP_TIER3: DeferredItem<Item> = ITEMS.registerSimpleItem("chip3")
    
    val ALU: DeferredItem<Item> = ITEMS.registerSimpleItem("alu")
    val CONTROL_UNIT: DeferredItem<Item> = ITEMS.registerSimpleItem("controlunit")
    
    val DISK_PLATTER: DeferredItem<Item> = ITEMS.registerSimpleItem("diskplatter")
    val CARD_BASE: DeferredItem<Item> = ITEMS.registerSimpleItem("card")
    
    val CUTTING_WIRE: DeferredItem<Item> = ITEMS.registerSimpleItem("cuttingwire")
    val ACID: DeferredItem<Item> = ITEMS.registerSimpleItem("acid")
    val RAW_CIRCUIT_BOARD: DeferredItem<Item> = ITEMS.registerSimpleItem("rawcircuitboard")
    
    val INK_CARTRIDGE: DeferredItem<Item> = ITEMS.registerSimpleItem("inkcartridge")
    val INK_CARTRIDGE_EMPTY: DeferredItem<Item> = ITEMS.registerSimpleItem("inkcartridgeempty")
    
    /** Item representing a completed 3D-printed block/object from the Printer. */
    val PRINTED_BLOCK: DeferredItem<Item> = ITEMS.registerSimpleItem("printedblock")
    
    val INTERWEB: DeferredItem<Item> = ITEMS.registerSimpleItem("interweb")
    val COMPONENT_BUS_TIER1: DeferredItem<Item> = ITEMS.registerSimpleItem("componentbus1")
    val COMPONENT_BUS_TIER2: DeferredItem<Item> = ITEMS.registerSimpleItem("componentbus2")
    val COMPONENT_BUS_TIER3: DeferredItem<Item> = ITEMS.registerSimpleItem("componentbus3")
    
    // ========================================
    // Tools
    // ========================================
    
    val WRENCH: DeferredItem<WrenchItem> = ITEMS.registerItem("wrench", ::WrenchItem)
    
    val DEBUG_CARD: DeferredItem<DebugCardItem> = ITEMS.registerItem("debugcard", ::DebugCardItem)
    
    // ========================================
    // Web Display Items
    // ========================================
    
    val LASER_POINTER: DeferredItem<LaserPointerItem> = ITEMS.registerItem("laser_pointer", ::LaserPointerItem)
    
    val REMOTE_KEYBOARD: DeferredItem<RemoteKeyboardItem> = ITEMS.registerItem("remote_keyboard", ::RemoteKeyboardItem)
    
    // ========================================
    // APUs (Accelerated Processing Units)
    // ========================================

    val APU_TIER1: DeferredItem<APUItem> = ITEMS.registerItem("apu1") { props ->
        APUItem(props, 0)
    }

    val APU_TIER2: DeferredItem<APUItem> = ITEMS.registerItem("apu2") { props ->
        APUItem(props, 1)
    }

    // ========================================
    // Keyboard Components
    // ========================================

    val ARROW_KEYS: DeferredItem<MaterialItems.ArrowKeysItem> = ITEMS.registerItem("arrowkeys") { props ->
        MaterialItems.ArrowKeysItem(props)
    }
    val BUTTON_GROUP: DeferredItem<MaterialItems.ButtonGroupItem> = ITEMS.registerItem("buttongroup") { props ->
        MaterialItems.ButtonGroupItem(props)
    }
    val NUMPAD: DeferredItem<MaterialItems.NumpadItem> = ITEMS.registerItem("numpad") { props ->
        MaterialItems.NumpadItem(props)
    }

    // ========================================
    // Servers
    // ========================================

    val SERVER_TIER1: DeferredItem<ServerItem> = ITEMS.registerItem("server1") { props ->
        ServerItem(props, 1)
    }
    val SERVER_TIER2: DeferredItem<ServerItem> = ITEMS.registerItem("server2") { props ->
        ServerItem(props, 2)
    }
    val SERVER_TIER3: DeferredItem<ServerItem> = ITEMS.registerItem("server3") { props ->
        ServerItem(props, 3)
    }
    val SERVER_CREATIVE: DeferredItem<ServerItem> = ITEMS.registerItem("server_creative") { props ->
        ServerItem(props, 4)
    }

    // ========================================
    // Remote Terminal / Terminal Server
    // ========================================

    val REMOTE_TERMINAL: DeferredItem<RemoteTerminalItem> = ITEMS.registerItem("remoteterminal", ::RemoteTerminalItem)
    val TERMINAL_SERVER: DeferredItem<TerminalServerItem> = ITEMS.registerItem("terminalserver", ::TerminalServerItem)

    // ========================================
    // Microcontroller Cases
    // ========================================

    val MICROCONTROLLER_CASE_TIER1: DeferredItem<MaterialItems.MicrocontrollerCaseItem> = ITEMS.registerItem("microcontrollercase1") { props ->
        MaterialItems.MicrocontrollerCaseItem(props, 1)
    }
    val MICROCONTROLLER_CASE_TIER2: DeferredItem<MaterialItems.MicrocontrollerCaseItem> = ITEMS.registerItem("microcontrollercase2") { props ->
        MaterialItems.MicrocontrollerCaseItem(props, 2)
    }
    val MICROCONTROLLER_CASE_CREATIVE: DeferredItem<MaterialItems.MicrocontrollerCaseItem> = ITEMS.registerItem("microcontrollercase_creative") { props ->
        MaterialItems.MicrocontrollerCaseItem(props, 3)
    }

    // ========================================
    // Tablet Cases
    // ========================================

    val TABLET_CASE_TIER1: DeferredItem<MaterialItems.TabletCaseItem> = ITEMS.registerItem("tabletcase1") { props ->
        MaterialItems.TabletCaseItem(props, 1)
    }
    val TABLET_CASE_TIER2: DeferredItem<MaterialItems.TabletCaseItem> = ITEMS.registerItem("tabletcase2") { props ->
        MaterialItems.TabletCaseItem(props, 2)
    }
    val TABLET_CASE_CREATIVE: DeferredItem<MaterialItems.TabletCaseItem> = ITEMS.registerItem("tabletcase_creative") { props ->
        MaterialItems.TabletCaseItem(props, 3)
    }

    // ========================================
    // Higher Tier Memory (Tier 4/5/6)
    // ========================================
    
    val MEMORY_TIER4: DeferredItem<MemoryItem> = ITEMS.registerItem("ram4") { props ->
        MemoryItem(props, 4)
    }
    
    val MEMORY_TIER5: DeferredItem<MemoryItem> = ITEMS.registerItem("ram5") { props ->
        MemoryItem(props, 5)
    }
    
    val MEMORY_TIER6: DeferredItem<MemoryItem> = ITEMS.registerItem("ram6") { props ->
        MemoryItem(props, 6)
    }

    // ========================================
    // Creative APU
    // ========================================
    
    val APU_CREATIVE: DeferredItem<APUItem> = ITEMS.registerItem("apu_creative") { props ->
        APUItem(props, 3)
    }

    // ========================================
    // Loot Disks & Storage
    // ========================================
    
    val LUA_BIOS: DeferredItem<EEPROMItem> = ITEMS.registerItem("luabios", ::EEPROMItem)
    
    val OPENOS: DeferredItem<FloppyItem> = ITEMS.registerItem("openos", ::FloppyItem)
    
    // Loot disks (from original OC)
    val LOOT_NETWORK: DeferredItem<FloppyItem> = ITEMS.registerItem("loot_network", ::FloppyItem)
    val LOOT_PLAN9K: DeferredItem<FloppyItem> = ITEMS.registerItem("loot_plan9k", ::FloppyItem)
    val LOOT_IRC: DeferredItem<FloppyItem> = ITEMS.registerItem("loot_irc", ::FloppyItem)
    val LOOT_OPENLOADER: DeferredItem<FloppyItem> = ITEMS.registerItem("loot_openloader", ::FloppyItem)
    val LOOT_OPPM: DeferredItem<FloppyItem> = ITEMS.registerItem("loot_oppm", ::FloppyItem)
    val LOOT_BUILDER: DeferredItem<FloppyItem> = ITEMS.registerItem("loot_builder", ::FloppyItem)
    val LOOT_DIG: DeferredItem<FloppyItem> = ITEMS.registerItem("loot_dig", ::FloppyItem)
    val LOOT_MAZE: DeferredItem<FloppyItem> = ITEMS.registerItem("loot_maze", ::FloppyItem)
    val LOOT_DATA: DeferredItem<FloppyItem> = ITEMS.registerItem("loot_data", ::FloppyItem)
    val LOOT_GENERATOR: DeferredItem<FloppyItem> = ITEMS.registerItem("loot_generator", ::FloppyItem)
    
    val DISK: DeferredItem<HDDItem> = ITEMS.registerItem("disk") { props ->
        HDDItem(props, 1) // Managed disk
    }

    // ========================================
    // Assembled Entities
    // ========================================
    
    val DRONE: DeferredItem<Item> = ITEMS.registerSimpleItem("drone") // TODO: DroneItem class

    // ========================================
    // Additional Upgrades
    // ========================================
    
    val UPGRADE_STICKY_PISTON: DeferredItem<Item> = ITEMS.registerSimpleItem("stickypistonupgrade")  // TODO: Proper upgrade item

    // ========================================
    // Creative/Debug Items
    // ========================================
    
    val ABSTRACT_BUS_CARD: DeferredItem<Item> = ITEMS.registerSimpleItem("abstractbuscard")
    
    val DEBUGGER: DeferredItem<Item> = ITEMS.registerSimpleItem("debugger")
    
    val TEXTURE_PICKER: DeferredItem<Item> = ITEMS.registerSimpleItem("texturepicker")
    
    val DISK_DRIVE_MOUNTABLE: DeferredItem<Item> = ITEMS.registerSimpleItem("diskdrivemountable")

    // ========================================
    // Additional Materials
    // ========================================
    
    val DIAMOND_CHIP: DeferredItem<Item> = ITEMS.registerSimpleItem("chipdiamond")

    val INK_CARTRIDGE_COLOR: DeferredItem<Item> = ITEMS.registerSimpleItem("inkcartridge_color")
    
    // ========================================
    // Easter Eggs
    // ========================================
    
    val PRESENT: DeferredItem<Item> = ITEMS.registerSimpleItem("present")

    val SIGN_IO_UPGRADE: DeferredItem<SignIOUpgradeItem> = ITEMS.registerItem("signio_upgrade", ::SignIOUpgradeItem)

    // ========================================
    // Aliases for datagen compatibility
    // ========================================

    // Block item aliases
    val CABLE get() = CABLE_ITEM
    val CAPACITOR get() = CAPACITOR_ITEM

    // Item name aliases
    val FLOPPY_DISK get() = FLOPPY
    val GRAPHICS_CARD_TIER1 get() = GPU_TIER1
    val GRAPHICS_CARD_TIER2 get() = GPU_TIER2
    val GRAPHICS_CARD_TIER3 get() = GPU_TIER3
    val MEMORY_TIER15 get() = MEMORY_TIER1_5
    val MEMORY_TIER25 get() = MEMORY_TIER2_5
    val MEMORY_TIER35 get() = MEMORY_TIER3_5
    val RAM_TIER1 get() = MEMORY_TIER1
    val RAM_TIER15 get() = MEMORY_TIER1_5
    val RAM_TIER2 get() = MEMORY_TIER2
    val RAM_TIER25 get() = MEMORY_TIER2_5
    val RAM_TIER3 get() = MEMORY_TIER3
    val RAM_TIER35 get() = MEMORY_TIER3_5
    val RAM_TIER4 get() = MEMORY_TIER4
    val RAM_TIER5 get() = MEMORY_TIER5
    val RAM_TIER6 get() = MEMORY_TIER6

    // Upgrade aliases
    val ANGEL_UPGRADE get() = UPGRADE_ANGEL
    val BATTERY_UPGRADE_TIER1 get() = UPGRADE_BATTERY_TIER1
    val BATTERY_UPGRADE_TIER2 get() = UPGRADE_BATTERY_TIER2
    val BATTERY_UPGRADE_TIER3 get() = UPGRADE_BATTERY_TIER3
    val CHUNKLOADER_UPGRADE get() = UPGRADE_CHUNKLOADER
    val CRAFTING_UPGRADE get() = UPGRADE_CRAFTING
    val DATABASE_UPGRADE_TIER1 get() = UPGRADE_DATABASE_TIER1
    val DATABASE_UPGRADE_TIER2 get() = UPGRADE_DATABASE_TIER2
    val DATABASE_UPGRADE_TIER3 get() = UPGRADE_DATABASE_TIER3
    val EXPERIENCE_UPGRADE get() = UPGRADE_EXPERIENCE
    val GENERATOR_UPGRADE get() = UPGRADE_GENERATOR
    val HOVER_UPGRADE_TIER1 get() = UPGRADE_HOVER_TIER1
    val HOVER_UPGRADE_TIER2 get() = UPGRADE_HOVER_TIER2
    val INVENTORY_UPGRADE get() = UPGRADE_INVENTORY
    val INVENTORY_CONTROLLER_UPGRADE get() = UPGRADE_INVENTORY_CONTROLLER
    val LEASH_UPGRADE get() = UPGRADE_LEASH
    val MFU get() = UPGRADE_MFU
    val NAVIGATION_UPGRADE get() = UPGRADE_NAVIGATION
    val PISTON_UPGRADE get() = UPGRADE_PISTON
    val SIGN_UPGRADE get() = UPGRADE_SIGN
    val SOLAR_GENERATOR_UPGRADE get() = UPGRADE_SOLAR_GENERATOR
    val TANK_UPGRADE get() = UPGRADE_TANK
    val TANK_CONTROLLER_UPGRADE get() = UPGRADE_TANK_CONTROLLER
    val TRACTOR_BEAM_UPGRADE get() = UPGRADE_TRACTOR_BEAM
    val TRADING_UPGRADE get() = UPGRADE_TRADING
    val STICKY_PISTON_UPGRADE get() = UPGRADE_STICKY_PISTON

    // Material aliases
    val CIRCUIT_BOARD get() = CIRCUIT_TIER1
    val PRINTED_CIRCUIT_BOARD get() = CIRCUIT_TIER2
    val CHIP_DIAMOND get() = DIAMOND_CHIP

    // ========================================
    // Registration
    // ========================================
    
    fun register(bus: IEventBus) {
        ITEMS.register(bus)
        OpenComputers.LOGGER.debug("Registered items")
    }
}
