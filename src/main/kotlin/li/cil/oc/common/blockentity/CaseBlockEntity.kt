package li.cil.oc.common.blockentity

import li.cil.oc.api.machine.Machine
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.machine.MachineState
import li.cil.oc.api.network.Component
import li.cil.oc.api.network.Node
import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.common.init.ModItems
import li.cil.oc.common.container.CaseMenu
import li.cil.oc.common.container.CaseSlotConfig
import li.cil.oc.common.item.*
import li.cil.oc.server.machine.InstalledComponents
import li.cil.oc.server.machine.SimpleMachine
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.items.ItemStackHandler

/**
 * Computer case block entity.
 * Hosts a machine that runs Lua programs.
 */
class CaseBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.CASE.get(), pos, state), MenuProvider, MachineHost {

    var tier: Int = 1
    var bootError: String? = null
    
    // Persistent component inventory
    val inventory = ItemStackHandler(10)
    
    // The machine running in this computer
    private var _machine: SimpleMachine? = null
    val machine: Machine? get() = _machine
    
    // Client-side power state (synced from server)
    private var _clientPowered: Boolean = false
    
    val isPowered: Boolean get() {
        // On server, check machine state; on client, use synced value
        return if (level?.isClientSide == true) {
            _clientPowered
        } else {
            _machine?.state?.isRunning ?: false
        }
    }
    
    // MachineHost implementation
    override fun machine(): Machine? = _machine
    override fun world(): Level? = level
    override fun hostPosition(): BlockPos = blockPos
    override fun node(): Node? = _machine?.node()
    override fun machineComponents(): Iterable<Component> = emptyList()
    override fun markDirty() { setChanged() }
    
    override fun onMachineStateChanged(state: MachineState) {
        setChanged()
        // Sync power state to all nearby clients via ComputerStatePacket
        val lvl = level ?: return
        if (!lvl.isClientSide) {
            li.cil.oc.network.ModPackets.sendToAllTracking(
                lvl, blockPos,
                li.cil.oc.network.ComputerStatePacket(blockPos, state.isRunning, 0.0, 0.0, 0)
            )
        }
    }
    
    override fun onComponentConnected(component: Component) {
        _machine?.registerComponent(component.address, component.name)
    }
    
    override fun onComponentDisconnected(component: Component) {
        _machine?.unregisterComponent(component.address)
    }
    
    fun togglePower(): String? {
        if (_machine == null) {
            _machine = SimpleMachine(this)
        }
        
        val m = _machine!!
        val lvl = level
        // Click sound on power-button press
        if (lvl != null && !lvl.isClientSide) {
            lvl.playSound(
                null, blockPos,
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                net.minecraft.sounds.SoundSource.BLOCKS,
                0.3f, 1.0f
            )
        }
        return if (m.state == MachineState.STOPPED || m.state == MachineState.CRASHED) {
            // Scan inventory for installed components
            val components = scanInventory()
            
            // Validate required components (like original OC)
            if (components.cpuTier < 0) {
                bootError = "No CPU installed"
                return bootError
            }
            if (components.totalMemory <= 0) {
                bootError = "No RAM installed"
                return bootError
            }
            
            // Pass component info to machine
            m.installedComponents = components

            if (!m.start()) {
                bootError = m.crashMessage ?: "Failed to start"
                bootError
            } else {
                // Auto-connect nearby screens after starting (node is available now)
                connectNearbyScreens(m)
                bootError = null
                null
            }
        } else {
            m.stop()
            disconnectAllScreens()
            bootError = null
            null
        }
    }

    /**
     * Scan the case inventory and determine what components are installed.
     * This is used to validate booting and configure the machine.
     */
    private fun scanInventory(): InstalledComponents {
        val slotDefs = CaseSlotConfig.getSlots(tier)
        var cpuTier = -1
        var totalMemory = 0
        var gpuTier = -1
        var hasEEPROM = false
        val hddTiers = mutableListOf<Int>()
        var hasFloppyDrive = false
        
        for (i in 0 until inventory.slots.coerceAtMost(slotDefs.size)) {
            val stack = inventory.getStackInSlot(i)
            if (stack.isEmpty) continue
            val item = stack.item
            
            when (item) {
                is CPUItem -> cpuTier = item.tier
                is MemoryItem -> totalMemory += item.getMemorySize() * 1024
                is GPUItem -> if (item.tier > gpuTier) gpuTier = item.tier
                is HDDItem -> hddTiers.add(item.tier)
                is EEPROMItem -> hasEEPROM = true
                is FloppyDiskItem -> {} // Floppy disk in floppy slot
            }
        }
        
        return InstalledComponents(
            cpuTier = cpuTier,
            totalMemory = totalMemory,
            gpuTier = gpuTier,
            hddTiers = hddTiers,
            hasEEPROM = hasEEPROM
        )
    }

    private fun connectNearbyScreens(machine: SimpleMachine) {
        val lvl = level ?: return
        val machineAddr = try { machine.node().address } catch (_: Exception) { return }

        // Find the primary screen using the same scan order as SimpleLuaArchitecture.
        // GPU writes ONLY to this screen, so we must connect ONLY this one.
        var primaryScreen: ScreenBlockEntity? = null
        outer@ for (dx in -8..8) {
            for (dy in -4..4) {
                for (dz in -8..8) {
                    val be = lvl.getBlockEntity(blockPos.offset(dx, dy, dz))
                    if (be is ScreenBlockEntity) {
                        primaryScreen = be
                        break@outer
                    }
                }
            }
        }

        // Disconnect all nearby screens from this machine, then connect only the primary.
        for (dx in -8..8) {
            for (dy in -4..4) {
                for (dz in -8..8) {
                    val be = lvl.getBlockEntity(blockPos.offset(dx, dy, dz))
                    if (be is ScreenBlockEntity) {
                        if (be === primaryScreen) {
                            be.connectedComputer = machineAddr
                            if (be.keyboardAddress == null) {
                                be.keyboardAddress = "kb-" + be.address
                            }
                        } else if (be.connectedComputer == machineAddr) {
                            // Disconnect screens that were previously connected to this machine
                            be.connectedComputer = null
                        }
                        be.setChanged()
                    }
                }
            }
        }
    }
    
    private var lastRedstoneInput = IntArray(6) { 0 }
    
    fun onRedstoneNeighborChanged() {
        val lvl = level ?: return
        val pos = blockPos
        val directions = net.minecraft.core.Direction.values()
        for (side in directions.indices) {
            val dir = directions[side]
            val newValue = lvl.getSignal(pos.relative(dir), dir)
            val oldValue = lastRedstoneInput[side]
            if (newValue != oldValue) {
                lastRedstoneInput[side] = newValue
                _machine?.signal("redstone_changed", side, oldValue, newValue)
            }
        }
    }
    
    fun shutdown() {
        _machine?.stop()
        disconnectAllScreens()
    }

    /**
     * Disconnect every nearby screen that was bound to this case.
     * Clears the buffer so the screen no longer shows stale text after the
     * computer is broken or removed.
     */
    private fun disconnectAllScreens() {
        val lvl = level ?: return
        if (lvl.isClientSide) return
        val machineAddr = try { _machine?.node()?.address } catch (_: Exception) { null }
        for (dx in -8..8) for (dy in -4..4) for (dz in -8..8) {
            val be = lvl.getBlockEntity(blockPos.offset(dx, dy, dz)) as? ScreenBlockEntity ?: continue
            // Disconnect either screens bound to this exact machine, or any screen
            // claiming a connection that no longer exists when machineAddr is null
            if (machineAddr == null || be.connectedComputer == machineAddr) {
                be.connectedComputer = null
                be.buffer.clear()
                be.markForSync()
                be.setChanged()
            }
        }
    }
    
    fun dropContents(level: Level, pos: BlockPos) {
        for (i in 0 until inventory.slots) {
            val stack = inventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                net.minecraft.world.Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
                inventory.setStackInSlot(i, net.minecraft.world.item.ItemStack.EMPTY)
            }
        }
    }

    override fun getDisplayName(): net.minecraft.network.chat.Component = 
        net.minecraft.network.chat.Component.literal("Computer")
    
    override fun createMenu(
        containerId: Int,
        playerInventory: Inventory,
        player: Player
    ): AbstractContainerMenu {
        val lvl = level ?: return CaseMenu(containerId, playerInventory, ContainerLevelAccess.NULL, inventory, tier)
        return CaseMenu(
            containerId, playerInventory,
            ContainerLevelAccess.create(lvl, blockPos),
            inventory,
            tier
        )
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putInt("tier", tier)
        tag.put("inventory", inventory.serializeNBT(registries))
        bootError?.let { tag.putString("bootError", it) }
        _machine?.let { machine ->
            val machineTag = CompoundTag()
            machine.saveData(machineTag)
            tag.put("machine", machineTag)
        }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        tier = tag.getInt("tier")
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"))
        }
        bootError = if (tag.contains("bootError")) tag.getString("bootError") else null
        // Load client power state from sync packet
        if (tag.contains("clientPowered")) {
            _clientPowered = tag.getBoolean("clientPowered")
        }
        if (tag.contains("machine")) {
            if (_machine == null) {
                _machine = SimpleMachine(this)
            }
            _machine?.loadData(tag.getCompound("machine"))
        }
    }
    
    // Client sync methods
    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        tag.putInt("tier", tier)
        tag.putBoolean("clientPowered", _machine?.state?.isRunning ?: false)
        return tag
    }
    
    override fun getUpdatePacket(): Packet<ClientGamePacketListener>? {
        return ClientboundBlockEntityDataPacket.create(this)
    }
    
    override fun handleUpdateTag(tag: CompoundTag, registries: HolderLookup.Provider) {
        loadAdditional(tag, registries)
    }

    /** Called client-side from ComputerStatePacket handler to update power state. */
    fun updateClientState(powered: Boolean) {
        _clientPowered = powered
    }
    
    companion object {
        @JvmStatic
        fun tick(level: Level, pos: BlockPos, state: BlockState, blockEntity: CaseBlockEntity) {
            blockEntity._machine?.let { machine ->
                // Before a world-load reboot, populate installedComponents from inventory
                // so start() has the correct memory/CPU info (same as togglePower does).
                if (machine.needsReboot) {
                    machine.installedComponents = blockEntity.scanInventory()
                }
                if (machine.state.isRunning || machine.needsReboot) {
                    machine.update()
                }
            }
        }
    }
}
