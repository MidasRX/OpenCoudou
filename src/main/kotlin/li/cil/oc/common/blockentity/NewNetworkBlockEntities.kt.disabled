package li.cil.oc.common.blockentity

import li.cil.oc.api.network.ComponentVisibility
import li.cil.oc.api.network.Environment
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.NodeBuilder
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.items.ItemStackHandler

/**
 * Switch block entity - managed network switch.
 * Connects up to 6 network segments (one per face) and routes
 * messages between them with bandwidth limiting.
 */
class SwitchBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.SWITCH.get(), pos, state), Environment {

    private var _node: Node? = null
    override fun node(): Node? = _node

    private var maxPacketsPerCycle: Int = 100
    private var packetCount: Int = 0
    private var tickCounter: Int = 0

    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withComponent("switch", ComponentVisibility.NETWORK)
                .build()
        }
    }

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        if (_node == null) {
            initializeOnLoad()
            connectToNetwork()
        }
        tickCounter++
        if (tickCounter >= 20) {
            tickCounter = 0
            packetCount = 0
        }
    }

    private fun connectToNetwork() {
        val level = level as? ServerLevel ?: return
        for (dir in Direction.entries) {
            val neighborPos = blockPos.relative(dir)
            val neighbor = level.getBlockEntity(neighborPos)
            if (neighbor is Environment) {
                neighbor.node()?.let { _node?.connect(it) }
            }
        }
    }

    fun getMaxPacketsPerCycle(): Int = maxPacketsPerCycle
    fun setMaxPacketsPerCycle(max: Int) { maxPacketsPerCycle = max.coerceIn(1, 10000) }
    fun getPacketCount(): Int = packetCount

    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {
        if (packetCount < maxPacketsPerCycle) {
            packetCount++
        }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putInt("MaxPackets", maxPacketsPerCycle)
        _node?.let { n -> val t = CompoundTag(); n.saveData(t); tag.put("Node", t) }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        maxPacketsPerCycle = tag.getInt("MaxPackets").takeIf { it > 0 } ?: 100
        if (tag.contains("Node")) { initializeOnLoad(); _node?.loadData(tag.getCompound("Node")) }
    }
}

/**
 * Net splitter block entity - splits a network into two halves.
 * The top/bottom/north faces form one side; south/east/west form the other.
 * A redstone signal merges the two sides into one network.
 */
class NetSplitterBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.NET_SPLITTER.get(), pos, state), Environment {

    private var _node: Node? = null
    override fun node(): Node? = _node

    // When open=true the two halves are connected (redstone high)
    private var open: Boolean = false

    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withComponent("net_splitter", ComponentVisibility.NETWORK)
                .build()
        }
    }

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        if (_node == null) {
            initializeOnLoad()
            connectToNetwork()
        }
        // Check redstone
        val powered = level.hasNeighborSignal(blockPos)
        if (powered != open) {
            open = powered
            setChanged()
        }
    }

    private fun connectToNetwork() {
        val level = level as? ServerLevel ?: return
        for (dir in Direction.entries) {
            val neighborPos = blockPos.relative(dir)
            val neighbor = level.getBlockEntity(neighborPos)
            if (neighbor is Environment) {
                neighbor.node()?.let { _node?.connect(it) }
            }
        }
    }

    fun isOpen(): Boolean = open

    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putBoolean("Open", open)
        _node?.let { n -> val t = CompoundTag(); n.saveData(t); tag.put("Node", t) }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        open = tag.getBoolean("Open")
        if (tag.contains("Node")) { initializeOnLoad(); _node?.loadData(tag.getCompound("Node")) }
    }
}

/**
 * Robot block entity - holds the assembled robot's component data
 * while the robot is sitting still as a placed block.
 * The robot entity is spawned/removed when the block is placed/broken.
 */
class RobotBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.ROBOT.get(), pos, state), Environment {

    private var _node: Node? = null
    override fun node(): Node? = _node

    // Inventory for robot components (up to 32 slots: 3 components + 4 upgrades + HDD + RAM etc.)
    val inventory: ItemStackHandler = ItemStackHandler(32)

    private var robotName: String = "Robot"
    private var energy: Double = 0.0
    private var maxEnergy: Double = 10000.0

    fun initializeOnLoad() {
        if (_node == null) {
            _node = NodeBuilder.create()
                .withHost(this)
                .withConnector(maxEnergy)
                .withComponent("robot", ComponentVisibility.NEIGHBORS)
                .build()
        }
    }

    fun tick(level: Level, pos: BlockPos, state: BlockState) {
        if (level.isClientSide) return
        if (_node == null) initializeOnLoad()
    }

    fun getRobotName(): String = robotName
    fun setRobotName(name: String) { robotName = name.take(64); setChanged() }
    fun getEnergy(): Double = energy
    fun getMaxEnergy(): Double = maxEnergy

    override fun onConnect(node: Node) {}
    override fun onDisconnect(node: Node) {}
    override fun onMessage(message: Message) {}

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.put("Inventory", inventory.serializeNBT(registries))
        tag.putString("RobotName", robotName)
        tag.putDouble("Energy", energy)
        tag.putDouble("MaxEnergy", maxEnergy)
        _node?.let { n -> val t = CompoundTag(); n.saveData(t); tag.put("Node", t) }
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"))
        robotName = tag.getString("RobotName").ifEmpty { "Robot" }
        energy = tag.getDouble("Energy")
        maxEnergy = tag.getDouble("MaxEnergy").takeIf { it > 0 } ?: 10000.0
        if (tag.contains("Node")) { initializeOnLoad(); _node?.loadData(tag.getCompound("Node")) }
    }
}
