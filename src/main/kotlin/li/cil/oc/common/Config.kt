package li.cil.oc.common

import net.neoforged.neoforge.common.ModConfigSpec

/**
 * Stub configuration - to be expanded.
 */
object Config {
    private val BUILDER = ModConfigSpec.Builder()
    
    // Client config stubs
    object Client {
        val textLingerTicks: Int = 40
        val screenTextFadeStartDistance: Double = 8.0
        val maxScreenTextRenderDistance: Double = 20.0
        val soundVolume: Double = 1.0
        val textInputDelay: Int = 5
        val nanomachineHudPos: List<Double> = listOf(0.0, 0.0)
        val monochromeColor: Int = 0x66FF66
    }
    
    // Computer config stubs
    object Computer {
        val threads: Int = 4
        val timeout: Double = 5.0
        val startupDelay: Double = 0.25
        val eraseTmpOnReboot: Boolean = true
        val eepromSize: Int = 4096
        val eepromDataSize: Int = 256
        val cpuComponentCount: IntArray = intArrayOf(8, 12, 16)
        val callBudgets: DoubleArray = doubleArrayOf(0.5, 1.0, 1.5)
        val allowBytecode: Boolean = false
        val allowGC: Boolean = false
    }
    
    // Robot config stubs  
    object Robot {
        val allowActivateBlocks: Boolean = true
        val allowUseItemsWithDuration: Boolean = true
        val canAttackPlayers: Boolean = false
        val limitFlightHeight: Int = 8
        val swingRange: Double = 0.5
        val useAndPlaceRange: Double = 0.5
        val itemDamageRate: Double = 0.1
        val nameFormat: String = "%s"
        val uuidFormat: String = "%s"
        val xpBaseCost: Double = 40.0
        val xpGrowth: Double = 1.2
    }
    
    // Power config stubs
    object Power {
        val ignorePower: Boolean = false
        val buffer: Double = 10000.0
        val bufferConverterUpgrade: Double = 1000.0
        val generatorEfficiency: Double = 1.0
        val solarGeneratorEfficiency: Double = 1.0
        val chargerEfficiency: Double = 0.8
        val assemblerTickAmount: Double = 50.0
        val cost: PowerCost = PowerCost
        
        object PowerCost {
            val computer: Double = 0.5
            val robot: Double = 0.25
            val sleepFactor: Double = 0.1
            val screen: Double = 0.05
            val holo: Double = 0.2
            val microcontroller: Double = 0.1
            val drone: Double = 0.25
            val eepromWrite: Double = 50.0
            val gpuFill: Double = 1.0 / 256.0
            val gpuClear: Double = 1.0 / 128.0
            val gpuCopy: Double = 1.0 / 128.0
            val hddRead: Double = 1.0 / 512.0
            val hddWrite: Double = 1.0 / 256.0
            val assemblerPerTick: Double = 5.0
            val disassemblerPerTick: Double = 5.0
            val printerPerTick: Double = 1.0
            val chunkLoaderDimensionCost: Double = 0.0
            val geolyzerScan: Double = 10.0
            val robotTurn: Double = 2.5
            val robotMove: Double = 15.0
            val robotExhaustion: Double = 10.0
            val wirelessStrength: Double = 0.05
            val abstractBusPacket: Double = 1.0
            val nanoMachineReconfigure: Double = 2.5
        }
    }
    
    // Filesystem config stubs
    object Filesystem {
        val hddSizes: IntArray = intArrayOf(1024 * 1024, 2 * 1024 * 1024, 4 * 1024 * 1024)
        val hddPlatterCount: IntArray = intArrayOf(2, 4, 6)
        val floppySize: Int = 512 * 1024
        val tmpSize: Int = 64 * 1024
        val maxHandles: Int = 256
        val maxReadBuffer: Int = 2048
        val sectorSeekTime: Double = 0.1
        val sectorSeekThreshold: Int = 128
        val bufferChanges: Boolean = true
        val fileCost: Long = 512
    }
    
    // Internet config stubs
    object Internet {
        val enable: Boolean = true
        val enableHttp: Boolean = true
        val enableTcp: Boolean = true
        val filteringRules: String = ""
        val httpTimeout: Int = 30
        val httpThreads: Int = 4
        val maxTcpConnections: Int = 4
    }
    
    // Printer config stubs
    object Printer {
        val maxShapeCount: Int = 128
        val chamelium: Int = 200
        val chameliumInk: Int = 50
        val inkAmount: Int = 100000
        val printSpeed: Int = 100
    }
    
    // Hologram config stubs
    object Hologram {
        val maxScale: Double = 3.0
        val maxTranslation: Double = 0.25
        val light: Double = 0.2
        val lightLevel: Int = 0
        val setRawDelay: Int = 2
    }
    
    // Misc config stubs
    object Misc {
        val maxNetworkPacketSize: Int = 8192
        val maxNetworkPacketParts: Int = 256
        val maxWirelessRange: Double = 400.0
        val rTreeMaxEntries: Int = 10
        val geolyzerRange: Int = 32
        val geolyzerNoise: Double = 2.0
        val disassemblerBreakChance: Double = 0.05
        val disassemblerInputBlacklist: List<String> = emptyList()
        val updateCheck: Boolean = true
        val lootProbability: Int = 5
        val lootRecrafting: Boolean = true
        val debugPersistence: Boolean = false
        val dataCardTiers: IntArray = intArrayOf(1, 2, 3)
    }
    
    val SPEC: ModConfigSpec = BUILDER.build()
}
