package li.cil.oc.integration

import li.cil.oc.OpenComputers
import li.cil.oc.api.driver.Converter
import li.cil.oc.api.driver.DriverBlock
import li.cil.oc.api.driver.DriverItem
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.network.ManagedEnvironment
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.neoforged.neoforge.capabilities.Capabilities

/**
 * Integration manager for other mods.
 * Handles auto-detection and registration of integration modules.
 */
object IntegrationManager {
    
    private val loadedIntegrations = mutableListOf<ModIntegration>()
    private val blockDrivers = mutableListOf<DriverBlock>()
    private val itemDrivers = mutableListOf<DriverItem>()
    private val converters = mutableListOf<Converter>()
    private val environmentProviders = mutableListOf<EnvironmentProvider>()
    
    /**
     * Initialize all integrations during mod loading.
     */
    fun initialize() {
        // Register built-in integrations
        registerIntegration(VanillaIntegration())
        registerIntegration(NeoForgeIntegration())
        
        // Try to load optional mod integrations
        tryLoadIntegration("mekanism") { MekanismIntegration() }
        tryLoadIntegration("ae2") { AppliedEnergisticsIntegration() }
        tryLoadIntegration("create") { CreateIntegration() }
        tryLoadIntegration("computercraft") { ComputerCraftIntegration() }
        
        OpenComputers.LOGGER.info("Loaded ${loadedIntegrations.size} mod integrations")
    }
    
    private fun tryLoadIntegration(modId: String, factory: () -> ModIntegration) {
        try {
            if (net.neoforged.fml.ModList.get().isLoaded(modId)) {
                registerIntegration(factory())
                OpenComputers.LOGGER.info("Loaded integration for $modId")
            }
        } catch (e: Exception) {
            OpenComputers.LOGGER.warn("Failed to load integration for $modId: ${e.message}")
        }
    }
    
    private fun registerIntegration(integration: ModIntegration) {
        loadedIntegrations.add(integration)
        
        // Register drivers
        integration.getBlockDrivers().forEach { blockDrivers.add(it) }
        integration.getItemDrivers().forEach { itemDrivers.add(it) }
        integration.getConverters().forEach { converters.add(it) }
        integration.getEnvironmentProviders().forEach { environmentProviders.add(it) }
        
        integration.onRegister()
    }
    
    fun getBlockDrivers(): List<DriverBlock> = blockDrivers
    fun getItemDrivers(): List<DriverItem> = itemDrivers
    fun getConverters(): List<Converter> = converters
    fun getEnvironmentProviders(): List<EnvironmentProvider> = environmentProviders
    
    /**
     * Find a driver for a block at a position.
     */
    fun findBlockDriver(level: Level, pos: BlockPos, side: Direction): DriverBlock? {
        return blockDrivers.find { it.worksWith(level, pos, side) }
    }
    
    /**
     * Find a driver for an item.
     */
    fun findItemDriver(stack: ItemStack): DriverItem? {
        return itemDrivers.find { it.worksWith(stack) }
    }
    
    /**
     * Convert a value for Lua.
     */
    fun convert(value: Any?): Any? {
        if (value == null) return null
        
        for (converter in converters) {
            val result = converter.convert(value)
            if (result !== value) {
                return result
            }
        }
        
        return value
    }
}

/**
 * Base interface for mod integrations.
 */
interface ModIntegration {
    val modId: String
    
    fun onRegister() {}
    
    fun getBlockDrivers(): List<DriverBlock> = emptyList()
    fun getItemDrivers(): List<DriverItem> = emptyList()
    fun getConverters(): List<Converter> = emptyList()
    fun getEnvironmentProviders(): List<EnvironmentProvider> = emptyList()
}

/**
 * Vanilla Minecraft integration.
 */
class VanillaIntegration : ModIntegration {
    override val modId = "minecraft"
    
    override fun getBlockDrivers(): List<DriverBlock> = listOf(
        VanillaInventoryDriver(),
        VanillaRedstoneDriver(),
        VanillaSignDriver(),
        VanillaJukeboxDriver(),
        VanillaNoteBlockDriver(),
        VanillaBeaconDriver()
    )
    
    override fun getConverters(): List<Converter> = listOf(
        VanillaConverter()
    )
}

/**
 * NeoForge capabilities integration.
 */
class NeoForgeIntegration : ModIntegration {
    override val modId = "neoforge"
    
    override fun getBlockDrivers(): List<DriverBlock> = listOf(
        ForgeEnergyDriver(),
        ForgeFluidDriver()
    )
}

// Stub integrations for popular mods
class MekanismIntegration : ModIntegration {
    override val modId = "mekanism"
}

class AppliedEnergisticsIntegration : ModIntegration {
    override val modId = "ae2"
}

class CreateIntegration : ModIntegration {
    override val modId = "create"
}

class ComputerCraftIntegration : ModIntegration {
    override val modId = "computercraft"
}

/**
 * Driver for vanilla inventories (chests, hoppers, etc.)
 */
class VanillaInventoryDriver : DriverBlock {
    
    override fun worksWith(level: Level, pos: BlockPos, side: Direction): Boolean {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side) != null
    }
    
    override fun createEnvironment(level: Level, pos: BlockPos, side: Direction): ManagedEnvironment? {
        val handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side) ?: return null
        return InventoryEnvironment(handler, pos)
    }
}

/**
 * Environment wrapper for vanilla inventories.
 */
class InventoryEnvironment(
    private val handler: net.neoforged.neoforge.items.IItemHandler,
    private val pos: BlockPos
) : SimpleComponent("inventory") {
    
    override fun methods(): Map<String, (li.cil.oc.api.machine.Context, li.cil.oc.api.machine.Arguments) -> Array<Any?>> {
        return mapOf(
            "getInventorySize" to { _, _ -> arrayOf(handler.slots) },
            "getStackInSlot" to { _, args ->
                val slot = args.checkInteger(0) - 1
                if (slot < 0 || slot >= handler.slots) {
                    arrayOf(null, "invalid slot")
                } else {
                    val stack = handler.getStackInSlot(slot)
                    if (stack.isEmpty) arrayOf(null) else arrayOf(stackToTable(stack))
                }
            },
            "getAllStacks" to { _, _ ->
                val stacks = mutableMapOf<Int, Map<String, Any?>>()
                for (i in 0 until handler.slots) {
                    val stack = handler.getStackInSlot(i)
                    if (!stack.isEmpty) {
                        stacks[i + 1] = stackToTable(stack)
                    }
                }
                arrayOf(stacks)
            }
        )
    }
}

/**
 * Driver for redstone-emitting blocks.
 */
class VanillaRedstoneDriver : DriverBlock {
    
    override fun worksWith(level: Level, pos: BlockPos, side: Direction): Boolean {
        return level.getBlockState(pos).isSignalSource
    }
    
    override fun createEnvironment(level: Level, pos: BlockPos, side: Direction): ManagedEnvironment? {
        return RedstoneBlockEnvironment(level, pos)
    }
}

class RedstoneBlockEnvironment(
    private val level: Level,
    private val pos: BlockPos
) : SimpleComponent("redstone_block") {
    
    override fun methods(): Map<String, (li.cil.oc.api.machine.Context, li.cil.oc.api.machine.Arguments) -> Array<Any?>> {
        return mapOf(
            "getInput" to { _, args ->
                val sideNum = args.checkInteger(0)
                val direction = Direction.from3DDataValue(sideNum)
                arrayOf(level.getSignal(pos.relative(direction), direction))
            }
        )
    }
}

/**
 * Driver for signs.
 */
class VanillaSignDriver : DriverBlock {
    
    override fun worksWith(level: Level, pos: BlockPos, side: Direction): Boolean {
        return level.getBlockEntity(pos) is net.minecraft.world.level.block.entity.SignBlockEntity
    }
    
    override fun createEnvironment(level: Level, pos: BlockPos, side: Direction): ManagedEnvironment? {
        return SignEnvironment(level, pos)
    }
}

class SignEnvironment(
    private val level: Level,
    private val pos: BlockPos
) : SimpleComponent("sign") {
    
    override fun methods(): Map<String, (li.cil.oc.api.machine.Context, li.cil.oc.api.machine.Arguments) -> Array<Any?>> {
        return mapOf(
            "getText" to { _, _ ->
                val sign = level.getBlockEntity(pos) as? net.minecraft.world.level.block.entity.SignBlockEntity
                if (sign == null) {
                    arrayOf(null, "not a sign")
                } else {
                    arrayOf("") // Would need to extract sign text
                }
            }
        )
    }
}

/**
 * Driver for jukeboxes.
 */
class VanillaJukeboxDriver : DriverBlock {
    
    override fun worksWith(level: Level, pos: BlockPos, side: Direction): Boolean {
        return level.getBlockEntity(pos) is net.minecraft.world.level.block.entity.JukeboxBlockEntity
    }
    
    override fun createEnvironment(level: Level, pos: BlockPos, side: Direction): ManagedEnvironment? {
        return JukeboxEnvironment(level, pos)
    }
}

class JukeboxEnvironment(
    private val level: Level,
    private val pos: BlockPos
) : SimpleComponent("jukebox") {
    
    override fun methods(): Map<String, (li.cil.oc.api.machine.Context, li.cil.oc.api.machine.Arguments) -> Array<Any?>> {
        return mapOf(
            "getRecord" to { _, _ ->
                val jukebox = level.getBlockEntity(pos) as? net.minecraft.world.level.block.entity.JukeboxBlockEntity
                if (jukebox == null) {
                    arrayOf(null, "not a jukebox")
                } else {
                    val record = jukebox.theItem
                    if (record.isEmpty) arrayOf(null) else arrayOf(record.item.descriptionId)
                }
            }
        )
    }
}

/**
 * Driver for note blocks.
 */
class VanillaNoteBlockDriver : DriverBlock {
    
    override fun worksWith(level: Level, pos: BlockPos, side: Direction): Boolean {
        return level.getBlockState(pos).block is net.minecraft.world.level.block.NoteBlock
    }
    
    override fun createEnvironment(level: Level, pos: BlockPos, side: Direction): ManagedEnvironment? {
        return NoteBlockEnvironment(level, pos)
    }
}

class NoteBlockEnvironment(
    private val level: Level,
    private val pos: BlockPos
) : SimpleComponent("note_block") {
    
    override fun methods(): Map<String, (li.cil.oc.api.machine.Context, li.cil.oc.api.machine.Arguments) -> Array<Any?>> {
        return mapOf(
            "getNote" to { _, _ ->
                val state = level.getBlockState(pos)
                val note = state.getValue(net.minecraft.world.level.block.NoteBlock.NOTE)
                arrayOf(note)
            },
            "setNote" to { _, args ->
                val note = args.checkInteger(0).coerceIn(0, 24)
                val state = level.getBlockState(pos)
                val newState = state.setValue(net.minecraft.world.level.block.NoteBlock.NOTE, note)
                level.setBlock(pos, newState, 3)
                arrayOf()
            }
        )
    }
}

/**
 * Driver for beacons.
 */
class VanillaBeaconDriver : DriverBlock {
    
    override fun worksWith(level: Level, pos: BlockPos, side: Direction): Boolean {
        return level.getBlockEntity(pos) is net.minecraft.world.level.block.entity.BeaconBlockEntity
    }
    
    override fun createEnvironment(level: Level, pos: BlockPos, side: Direction): ManagedEnvironment? {
        return BeaconEnvironment(level, pos)
    }
}

class BeaconEnvironment(
    private val level: Level,
    private val pos: BlockPos
) : SimpleComponent("beacon") {
    
    override fun methods(): Map<String, (li.cil.oc.api.machine.Context, li.cil.oc.api.machine.Arguments) -> Array<Any?>> {
        return mapOf(
            "getLevel" to { _, _ ->
                // BeaconBlockEntity.levels is not directly accessible in 1.21.4
                // We just return placeholder
                arrayOf(0)
            }
        )
    }
}

/**
 * Driver for Forge energy storage.
 */
class ForgeEnergyDriver : DriverBlock {
    
    override fun worksWith(level: Level, pos: BlockPos, side: Direction): Boolean {
        return level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side) != null
    }
    
    override fun createEnvironment(level: Level, pos: BlockPos, side: Direction): ManagedEnvironment? {
        val handler = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side) ?: return null
        return EnergyStorageEnvironment(handler, pos)
    }
}

class EnergyStorageEnvironment(
    private val handler: net.neoforged.neoforge.energy.IEnergyStorage,
    private val pos: BlockPos
) : SimpleComponent("energy_storage") {
    
    override fun methods(): Map<String, (li.cil.oc.api.machine.Context, li.cil.oc.api.machine.Arguments) -> Array<Any?>> {
        return mapOf(
            "getEnergyStored" to { _, _ -> arrayOf(handler.energyStored) },
            "getMaxEnergyStored" to { _, _ -> arrayOf(handler.maxEnergyStored) },
            "canExtract" to { _, _ -> arrayOf(handler.canExtract()) },
            "canReceive" to { _, _ -> arrayOf(handler.canReceive()) }
        )
    }
}

/**
 * Driver for Forge fluid storage.
 */
class ForgeFluidDriver : DriverBlock {
    
    override fun worksWith(level: Level, pos: BlockPos, side: Direction): Boolean {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side) != null
    }
    
    override fun createEnvironment(level: Level, pos: BlockPos, side: Direction): ManagedEnvironment? {
        val handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side) ?: return null
        return FluidHandlerEnvironment(handler, pos)
    }
}

class FluidHandlerEnvironment(
    private val handler: net.neoforged.neoforge.fluids.capability.IFluidHandler,
    private val pos: BlockPos
) : SimpleComponent("fluid_handler") {
    
    override fun methods(): Map<String, (li.cil.oc.api.machine.Context, li.cil.oc.api.machine.Arguments) -> Array<Any?>> {
        return mapOf(
            "getTanks" to { _, _ -> arrayOf(handler.tanks) },
            "getFluidInTank" to { _, args ->
                val tank = args.checkInteger(0) - 1
                if (tank < 0 || tank >= handler.tanks) {
                    arrayOf(null, "invalid tank")
                } else {
                    val stack = handler.getFluidInTank(tank)
                    if (stack.isEmpty) arrayOf(null) else arrayOf(fluidToTable(stack))
                }
            },
            "getTankCapacity" to { _, args ->
                val tank = args.checkInteger(0) - 1
                if (tank < 0 || tank >= handler.tanks) {
                    arrayOf(null, "invalid tank")
                } else {
                    arrayOf(handler.getTankCapacity(tank))
                }
            }
        )
    }
}

/**
 * Converter for vanilla types.
 */
class VanillaConverter : Converter {
    override fun convert(value: Any): Any? {
        return when (value) {
            is BlockPos -> mapOf("x" to value.x, "y" to value.y, "z" to value.z)
            is Direction -> value.name.lowercase()
            is ItemStack -> if (value.isEmpty) null else mapOf(
                "name" to value.item.descriptionId,
                "count" to value.count,
                "maxCount" to value.maxStackSize
            )
            is net.neoforged.neoforge.fluids.FluidStack -> if (value.isEmpty) null else mapOf(
                "name" to value.fluid.fluidType.descriptionId,
                "amount" to value.amount
            )
            else -> value
        }
    }
}
