package li.cil.oc.common.blockentity

import li.cil.oc.common.container.CaseSlotConfig
import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.common.init.ModItems
import li.cil.oc.common.init.ModSoundEvents
import li.cil.oc.common.item.HDDItem
import li.cil.oc.server.component.DataCardComponent
import li.cil.oc.server.component.DebugCardComponent
import li.cil.oc.server.component.EEPROMComponent
import li.cil.oc.server.component.FilesystemComponent
import li.cil.oc.server.component.GPUComponent
import li.cil.oc.server.component.InternetCardComponent
import li.cil.oc.server.component.LinkedCardComponent
import li.cil.oc.server.component.NetworkCardComponent
import li.cil.oc.server.component.RedstoneCardComponent
import li.cil.oc.server.component.ScreenComponent
import li.cil.oc.server.machine.Machine
import li.cil.oc.util.OCLogger
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.sounds.SoundSource
import net.minecraft.world.Containers
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.items.ItemStackHandler

class CaseBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.CASE.get(), pos, state), MenuProvider {

    private var tier: Int = 1
    var isPowered: Boolean = false
    var bootError: String? = null

    // The Lua VM machine
    var machine: Machine? = null
        private set

    // Use max slot count (tier 4/creative = 10 slots) to avoid resizing issues
    val inventory: ItemStackHandler = object : ItemStackHandler(10) {
        override fun onContentsChanged(slot: Int) {
            setChanged()
            // If computer is running and components change, log it
            if (isPowered) {
                OCLogger.component("Changed", getSlotType(slot), "slot $slot")
            }
        }

        override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
            // Only allow items in slots that exist for this tier
            return slot < CaseSlotConfig.getSlots(tier).size
        }
    }

    private fun getSlotType(slot: Int): String {
        val slots = CaseSlotConfig.getSlots(tier)
        return if (slot < slots.size) slots[slot].type else "unknown"
    }

    fun setTier(t: Int) {
        tier = t
        setChanged()
    }

    fun getTier(): Int = tier

    private fun slotCount(): Int = CaseSlotConfig.getSlots(tier).size

    // ============ Component Detection ============
    
    fun hasCPU(): Boolean {
        val slots = CaseSlotConfig.getSlots(tier)
        for ((i, def) in slots.withIndex()) {
            if (def.type == CaseSlotConfig.CPU) {
                val stack = inventory.getStackInSlot(i)
                if (!stack.isEmpty && isCPUItem(stack)) return true
            }
        }
        return false
    }

    fun hasRAM(): Boolean {
        val slots = CaseSlotConfig.getSlots(tier)
        for ((i, def) in slots.withIndex()) {
            if (def.type == CaseSlotConfig.MEMORY) {
                val stack = inventory.getStackInSlot(i)
                if (!stack.isEmpty && isMemoryItem(stack)) return true
            }
        }
        return false
    }

    fun hasEEPROM(): Boolean {
        val slots = CaseSlotConfig.getSlots(tier)
        for ((i, def) in slots.withIndex()) {
            if (def.type == CaseSlotConfig.EEPROM) {
                val stack = inventory.getStackInSlot(i)
                if (!stack.isEmpty && isEEPROMItem(stack)) return true
            }
        }
        return false
    }

    private fun isCPUItem(stack: ItemStack): Boolean {
        val item = stack.item
        return item == ModItems.CPU_TIER1.get() || 
               item == ModItems.CPU_TIER2.get() || 
               item == ModItems.CPU_TIER3.get() ||
               item == ModItems.CPU_CREATIVE.get() ||
               // APUs also work as CPUs (combined CPU+GPU)
               item == ModItems.APU_TIER1.get() ||
               item == ModItems.APU_TIER2.get() ||
               item == ModItems.APU_CREATIVE.get()
    }

    private fun isMemoryItem(stack: ItemStack): Boolean {
        val item = stack.item
        return item == ModItems.MEMORY_TIER1.get() || 
               item == ModItems.MEMORY_TIER1_5.get() ||
               item == ModItems.MEMORY_TIER2.get() || 
               item == ModItems.MEMORY_TIER2_5.get() ||
               item == ModItems.MEMORY_TIER3.get() ||
               item == ModItems.MEMORY_TIER3_5.get() ||
               item == ModItems.MEMORY_TIER4.get() ||
               item == ModItems.MEMORY_TIER5.get() ||
               item == ModItems.MEMORY_TIER6.get()
    }

    private fun isEEPROMItem(stack: ItemStack): Boolean {
        val item = stack.item
        return item == ModItems.EEPROM.get() || item == ModItems.LUA_BIOS.get()
    }

    // ============ Boot Logic ============
    
    /**
     * Attempt to start the computer. Returns error message or null on success.
     */
    fun tryStart(): String? {
        OCLogger.boot("START", "Attempting to boot computer at $blockPos (tier $tier)")
        
        // Log inventory contents
        val slots = CaseSlotConfig.getSlots(tier)
        OCLogger.boot("INVENTORY", "Checking ${slots.size} slots")
        for ((i, def) in slots.withIndex()) {
            val stack = inventory.getStackInSlot(i)
            OCLogger.debug("  Slot $i (${def.type}): ${if (stack.isEmpty) "EMPTY" else stack.item.toString()}")
        }
        
        // Check CPU
        if (!hasCPU()) {
            OCLogger.boot("FAIL", "No CPU installed")
            bootError = "message.opencomputers.computer.no_cpu"
            return bootError
        }
        OCLogger.boot("CHECK", "CPU found")
        
        // Check RAM
        if (!hasRAM()) {
            OCLogger.boot("FAIL", "No RAM installed")
            bootError = "message.opencomputers.computer.no_ram"
            return bootError
        }
        OCLogger.boot("CHECK", "RAM found")
        
        // Check EEPROM
        if (!hasEEPROM()) {
            OCLogger.boot("FAIL", "No EEPROM installed")
            bootError = "message.opencomputers.computer.no_eeprom"
            return bootError
        }
        OCLogger.boot("CHECK", "EEPROM found")
        
        // All checks passed! Create and start the machine
        OCLogger.boot("SUCCESS", "All components present, creating machine")
        
        try {
            // Create machine
            machine = Machine()
            
            // Set creative mode if tier 4 (creative) case
            machine!!.isCreative = tier >= 4
            if (machine!!.isCreative) {
                OCLogger.boot("MODE", "Creative mode enabled - unlimited energy")
            }
            
            // Add internal components
            val eeprom = EEPROMComponent(tier)
            machine!!.network.add(eeprom)
            OCLogger.boot("COMPONENT", "Added EEPROM: ${eeprom.address}")
            
            // Add GPU (tier based on installed graphics card, default to tier 1)
            val gpu = GPUComponent(getGPUTier())
            machine!!.network.add(gpu)
            gpu.network = machine!!.network  // Give GPU access to network for screen lookup
            OCLogger.boot("COMPONENT", "Added GPU: ${gpu.address}")
            
            // Add filesystem components for installed HDDs
            addHDDComponents()
            
            // Add tmpfs (temporary filesystem based on RAM)
            val tmpfsSize = getTmpfsSize()
            if (tmpfsSize > 0) {
                val tmpfs = FilesystemComponent(label = "tmpfs", capacity = tmpfsSize.toLong())
                machine!!.network.add(tmpfs)
                OCLogger.boot("COMPONENT", "Added tmpfs: ${tmpfs.address} (${tmpfsSize / 1024}KB)")
            }
            
            // Add network cards from card slots
            addNetworkCardComponents()
            
            // Find and connect to nearby disk drives
            connectToDiskDrives()
            
            // Find and connect to nearby holograms
            connectToHolograms()
            
            // Find and connect to nearby geolyzer
            connectToGeolyzer()
            
            // Find and connect to nearby screen
            connectToScreen(gpu)
            
            // Start the machine with BIOS code
            val biosCode = eeprom.getCode()
            OCLogger.boot("BIOS", "Loading BIOS (${biosCode.length} bytes)")
            
            if (machine!!.start(biosCode)) {
                bootError = null
                isPowered = true
                setChanged()
                OCLogger.computer("STARTED", "Computer at $blockPos is now running")
                
                // Play computer running sound
                level?.playSound(null, blockPos, ModSoundEvents.COMPUTER_RUNNING.get(), 
                    SoundSource.BLOCKS, 0.5f, 1.0f)
                
                return null
            } else {
                bootError = machine!!.crashMessage ?: "Failed to start"
                machine = null
                return bootError
            }
        } catch (e: Exception) {
            OCLogger.error("Failed to start computer", e)
            bootError = "Boot error: ${e.message}"
            machine = null
            return bootError
        }
        
        return null
    }

    /**
     * Get the GPU tier based on installed graphics card or APU.
     */
    private fun getGPUTier(): Int {
        val slots = CaseSlotConfig.getSlots(tier)
        for ((i, def) in slots.withIndex()) {
            if (def.type == CaseSlotConfig.CARD) {
                val stack = inventory.getStackInSlot(i)
                if (!stack.isEmpty) {
                    val item = stack.item
                    if (item == ModItems.GRAPHICS_CARD_TIER1.get()) return 1
                    if (item == ModItems.GRAPHICS_CARD_TIER2.get()) return 2
                    if (item == ModItems.GRAPHICS_CARD_TIER3.get()) return 3
                    // APUs also provide GPU functionality
                    if (item == ModItems.APU_TIER1.get()) return 1
                    if (item == ModItems.APU_TIER2.get()) return 2
                    if (item == ModItems.APU_CREATIVE.get()) return 3
                }
            }
        }
        return 1 // Default to tier 1
    }
    
    /**
     * Get tmpfs size based on installed RAM.
     * Tmpfs is a temporary in-memory filesystem cleared on reboot.
     * Size is based on lowest tier RAM installed (64KB per tier).
     */
    private fun getTmpfsSize(): Int {
        val slots = CaseSlotConfig.getSlots(tier)
        var lowestTier = 0
        
        for ((i, def) in slots.withIndex()) {
            if (def.type == CaseSlotConfig.MEMORY) {
                val stack = inventory.getStackInSlot(i)
                if (!stack.isEmpty && isMemoryItem(stack)) {
                    val memTier = getMemoryTier(stack)
                    if (lowestTier == 0 || memTier < lowestTier) {
                        lowestTier = memTier
                    }
                }
            }
        }
        
        // Return tmpfs size: 64KB * tier (0 if no RAM)
        return lowestTier * 64 * 1024
    }
    
    /**
     * Get the tier of a memory item.
     */
    private fun getMemoryTier(stack: ItemStack): Int {
        val item = stack.item
        return when {
            item == ModItems.MEMORY_TIER1.get() -> 1
            item == ModItems.MEMORY_TIER1_5.get() -> 1
            item == ModItems.MEMORY_TIER2.get() -> 2
            item == ModItems.MEMORY_TIER2_5.get() -> 2
            item == ModItems.MEMORY_TIER3.get() -> 3
            item == ModItems.MEMORY_TIER3_5.get() -> 3
            item == ModItems.MEMORY_TIER4.get() -> 4
            item == ModItems.MEMORY_TIER5.get() -> 5
            item == ModItems.MEMORY_TIER6.get() -> 6
            else -> 1
        }
    }
    
    /**
     * Add filesystem components for all installed HDDs.
     */
    private fun addHDDComponents() {
        val slots = CaseSlotConfig.getSlots(tier)
        val hddSizes = longArrayOf(1024L * 1024, 2L * 1024 * 1024, 4L * 1024 * 1024) // Tier 1, 2, 3
        
        for ((i, def) in slots.withIndex()) {
            if (def.type == CaseSlotConfig.HDD) {
                val stack = inventory.getStackInSlot(i)
                if (!stack.isEmpty && stack.item is HDDItem) {
                    val hddItem = stack.item as HDDItem
                    val hddTier = hddItem.tier
                    val size = hddSizes.getOrElse(hddTier) { 1024L * 1024 }
                    
                    // Create filesystem component for this HDD
                    val label = stack.hoverName.string
                    val fs = FilesystemComponent(label = label, capacity = size)
                    machine!!.network.add(fs)
                    OCLogger.boot("COMPONENT", "Added HDD filesystem: ${fs.address} (${size / 1024}KB)")
                }
            }
        }
    }
    
    /**
     * Add network card components for installed cards.
     */
    private fun addNetworkCardComponents() {
        val slots = CaseSlotConfig.getSlots(tier)
        
        for ((i, def) in slots.withIndex()) {
            if (def.type == CaseSlotConfig.CARD) {
                val stack = inventory.getStackInSlot(i)
                if (!stack.isEmpty) {
                    val item = stack.item
                    
                    // Check for network cards
                    if (item == ModItems.NETWORK_CARD.get()) {
                        val networkCard = NetworkCardComponent(tier = 1)
                        machine!!.network.add(networkCard)
                        OCLogger.boot("COMPONENT", "Added Network Card: ${networkCard.address}")
                    } else if (item == ModItems.WIRELESS_CARD_TIER1.get()) {
                        val networkCard = NetworkCardComponent(tier = 1)
                        machine!!.network.add(networkCard)
                        OCLogger.boot("COMPONENT", "Added Wireless Network Card T1: ${networkCard.address}")
                    } else if (item == ModItems.WIRELESS_CARD_TIER2.get()) {
                        val networkCard = NetworkCardComponent(tier = 2)
                        machine!!.network.add(networkCard)
                        OCLogger.boot("COMPONENT", "Added Wireless Network Card T2: ${networkCard.address}")
                    }
                    // Check for redstone cards
                    else if (item == ModItems.REDSTONE_CARD_TIER1.get()) {
                        val redstoneCard = RedstoneCardComponent(tier = 1)
                        redstoneCard.computerPos = blockPos
                        redstoneCard.level = level
                        machine!!.network.add(redstoneCard)
                        OCLogger.boot("COMPONENT", "Added Redstone Card T1: ${redstoneCard.address}")
                    } else if (item == ModItems.REDSTONE_CARD_TIER2.get()) {
                        val redstoneCard = RedstoneCardComponent(tier = 2)
                        redstoneCard.computerPos = blockPos
                        redstoneCard.level = level
                        machine!!.network.add(redstoneCard)
                        OCLogger.boot("COMPONENT", "Added Redstone Card T2: ${redstoneCard.address}")
                    }
                    // Check for internet card
                    else if (item == ModItems.INTERNET_CARD.get()) {
                        val internetCard = InternetCardComponent()
                        machine!!.network.add(internetCard)
                        OCLogger.boot("COMPONENT", "Added Internet Card: ${internetCard.address}")
                    }
                    // Check for data cards
                    else if (item == ModItems.DATA_CARD_TIER1.get()) {
                        val dataCard = DataCardComponent(tier = 1)
                        machine!!.network.add(dataCard)
                        OCLogger.boot("COMPONENT", "Added Data Card T1: ${dataCard.address}")
                    } else if (item == ModItems.DATA_CARD_TIER2.get()) {
                        val dataCard = DataCardComponent(tier = 2)
                        machine!!.network.add(dataCard)
                        OCLogger.boot("COMPONENT", "Added Data Card T2: ${dataCard.address}")
                    } else if (item == ModItems.DATA_CARD_TIER3.get()) {
                        val dataCard = DataCardComponent(tier = 3)
                        machine!!.network.add(dataCard)
                        OCLogger.boot("COMPONENT", "Added Data Card T3: ${dataCard.address}")
                    }
                    // Check for linked card (creative tier)
                    else if (item == ModItems.LINKED_CARD.get()) {
                        val linkedCard = LinkedCardComponent()
                        machine!!.network.add(linkedCard)
                        OCLogger.boot("COMPONENT", "Added Linked Card: ${linkedCard.address}")
                    }
                    // Check for debug card (creative only)
                    else if (item == ModItems.DEBUG_CARD.get()) {
                        val debugCard = DebugCardComponent()
                        machine!!.network.add(debugCard)
                        OCLogger.boot("COMPONENT", "Added Debug Card (Creative): ${debugCard.address}")
                    }
                }
            }
        }
    }

    /**
     * Find and connect to nearby disk drives.
     */
    private fun connectToDiskDrives() {
        if (level == null || machine == null) return
        
        // Search for disk drives in adjacent blocks
        for (dir in net.minecraft.core.Direction.entries) {
            val checkPos = blockPos.relative(dir)
            val be = level!!.getBlockEntity(checkPos)
            if (be is DiskDriveBlockEntity) {
                val fs: FilesystemComponent? = be.getFilesystem()
                if (fs != null) {
                    machine!!.network.add(fs)
                    OCLogger.boot("COMPONENT", "Connected to disk drive at $checkPos: ${fs.address}")
                }
            }
        }
    }

    /**
     * Find and connect to nearby holograms.
     */
    private fun connectToHolograms() {
        if (level == null || machine == null) return
        
        // Search for holograms in a 16 block radius
        val searchRadius = 16
        for (x in -searchRadius..searchRadius) {
            for (y in -searchRadius..searchRadius) {
                for (z in -searchRadius..searchRadius) {
                    val checkPos = blockPos.offset(x, y, z)
                    val be = level!!.getBlockEntity(checkPos)
                    if (be is HologramBlockEntity) {
                        val hologram = be.getComponent()
                        machine!!.network.add(hologram)
                        OCLogger.boot("COMPONENT", "Connected to hologram at $checkPos: ${hologram.address}")
                    }
                }
            }
        }
    }

    /**
     * Find and connect to nearby geolyzer.
     */
    private fun connectToGeolyzer() {
        if (level == null || machine == null) return
        
        // Search for geolyzer in adjacent blocks
        for (dir in net.minecraft.core.Direction.entries) {
            val checkPos = blockPos.relative(dir)
            val be = level!!.getBlockEntity(checkPos)
            if (be is GeolyzerBlockEntity) {
                val geolyzer = be.getComponent()
                machine!!.network.add(geolyzer)
                OCLogger.boot("COMPONENT", "Connected to geolyzer at $checkPos: ${geolyzer.address}")
            }
        }
    }

    /**
     * Find and connect to a nearby screen.
     */
    private fun connectToScreen(gpu: GPUComponent) {
        if (level == null) return
        
        // Search for screens in a 16 block radius
        val searchRadius = 16
        for (x in -searchRadius..searchRadius) {
            for (y in -searchRadius..searchRadius) {
                for (z in -searchRadius..searchRadius) {
                    val checkPos = blockPos.offset(x, y, z)
                    val be = level!!.getBlockEntity(checkPos)
                    if (be is ScreenBlockEntity) {
                        // Found a screen! Add its component to our network
                        machine?.network?.add(be.screen)
                        be.connectedComputer = machine?.address
                        be.isOn = true
                        
                        // Pre-bind the GPU to this screen so rendering works immediately
                        gpu.bind(be.screen)
                        
                        OCLogger.boot("SCREEN", "Connected to screen at $checkPos (${be.screen.address})")
                        return
                    }
                }
            }
        }
        OCLogger.boot("SCREEN", "No screen found nearby")
    }

    fun shutdown() {
        OCLogger.computer("SHUTDOWN", "Computer at $blockPos")
        machine?.shutdown()
        machine = null
        isPowered = false
        bootError = null
        setChanged()
    }

    fun togglePower(): String? {
        return if (isPowered) {
            shutdown()
            null
        } else {
            tryStart()
        }
    }

    override fun getDisplayName(): Component =
        Component.translatable("block.opencomputers.case${tier}")

    override fun createMenu(containerId: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu {
        return li.cil.oc.common.container.CaseMenu(
            containerId, playerInventory,
            ContainerLevelAccess.create(level!!, blockPos),
            inventory, tier
        )
    }

    fun dropContents(level: Level, pos: BlockPos) {
        for (i in 0 until inventory.slots) {
            val stack = inventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                Containers.dropItemStack(level, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), stack)
                inventory.setStackInSlot(i, ItemStack.EMPTY)
            }
        }
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.put("Inventory", inventory.serializeNBT(registries))
        tag.putInt("Tier", tier)
        tag.putBoolean("Powered", isPowered)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        tier = tag.getInt("Tier").coerceAtLeast(1)
        isPowered = tag.getBoolean("Powered")
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"))
        }
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = super.getUpdateTag(registries)
        tag.putInt("Tier", tier)
        return tag
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket =
        ClientboundBlockEntityDataPacket.create(this)
}
