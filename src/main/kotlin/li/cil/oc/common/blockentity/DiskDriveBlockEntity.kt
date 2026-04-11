package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.common.init.ModItems
import li.cil.oc.server.component.FilesystemComponent
import li.cil.oc.server.machine.Machine
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Containers
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID

/**
 * Block entity for the disk drive.
 * Holds a floppy disk and provides filesystem access to connected computers.
 */
class DiskDriveBlockEntity(pos: BlockPos, state: BlockState) 
    : BlockEntity(ModBlockEntities.DISK_DRIVE.get(), pos, state) {
    
    companion object {
        private const val TAG_DISK = "DiskItem"
        private const val TAG_FLOPPY_UUID = "FloppyUUID"
        
        // Floppy disk capacity based on tier
        private const val FLOPPY_CAPACITY = 512 * 1024L // 512KB
    }
    
    private var diskStack: ItemStack = ItemStack.EMPTY
    private var filesystem: FilesystemComponent? = null
    private var floppyUUID: UUID? = null
    
    // Connected machines that should receive disk signals
    private val connectedMachines = mutableSetOf<Machine>()
    
    /**
     * Register a machine to receive disk insert/eject signals
     */
    fun registerMachine(machine: Machine) {
        connectedMachines.add(machine)
    }
    
    /**
     * Unregister a machine from receiving signals
     */
    fun unregisterMachine(machine: Machine) {
        connectedMachines.remove(machine)
    }
    
    fun getDisk(): ItemStack = diskStack.copy()
    
    fun setDisk(stack: ItemStack) {
        val oldDisk = diskStack
        val hadDisk = !oldDisk.isEmpty
        val oldFs = filesystem
        
        diskStack = if (stack.isEmpty) ItemStack.EMPTY else stack.copyWithCount(1)
        
        // Clear old filesystem and fire removal signal
        if (hadDisk && oldFs != null) {
            // Fire disk_removed signal to all connected machines
            for (machine in connectedMachines) {
                machine.pushSignal("component_removed", oldFs.address, "filesystem")
            }
            filesystem = null
            floppyUUID = null
        }
        
        // Create new filesystem for the floppy and fire insertion signal
        if (!diskStack.isEmpty) {
            // Get or create UUID for this floppy
            val customData = diskStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
            val tag = customData.copyTag()
            
            floppyUUID = if (tag.hasUUID("UUID")) {
                tag.getUUID("UUID")
            } else {
                val newUUID = UUID.randomUUID()
                tag.putUUID("UUID", newUUID)
                diskStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
                newUUID
            }
            
            filesystem = createFilesystemForFloppy()
            
            // Fire disk_inserted signal (component_added) to all connected machines
            val newFs = filesystem
            if (newFs != null) {
                for (machine in connectedMachines) {
                    // Add the new filesystem to the machine's network
                    machine.network.add(newFs)
                    machine.pushSignal("component_added", newFs.address, "filesystem")
                }
            }
        }
        
        setChanged()
    }
    
    fun canInsert(stack: ItemStack): Boolean {
        // Check if it's a floppy disk item
        val item = stack.item
        return item == ModItems.FLOPPY.get() ||
               item == ModItems.OPENOS.get() ||
               item == ModItems.LOOT_DATA.get() ||
               item == ModItems.LOOT_NETWORK.get()
    }
    
    fun hasDisk(): Boolean = !diskStack.isEmpty
    
    fun getFilesystem(): FilesystemComponent? = filesystem
    
    private fun createFilesystemForFloppy(): FilesystemComponent {
        val label = getFloppyLabel()
        val fs = FilesystemComponent(label = label, capacity = FLOPPY_CAPACITY)
        
        // Initialize with content based on floppy type
        initializeFloppyContent(fs)
        
        return fs
    }
    
    private fun getFloppyLabel(): String {
        val item = diskStack.item
        return when (item) {
            ModItems.OPENOS.get() -> "openos"
            ModItems.LOOT_DATA.get() -> "data"
            ModItems.LOOT_NETWORK.get() -> "network"
            else -> "floppy"
        }
    }
    
    private fun initializeFloppyContent(fs: FilesystemComponent) {
        // For OpenOS, initialize with a bootable system
        val item = diskStack.item
        if (item == ModItems.OPENOS.get()) {
            // Create directory structure
            fs.makeDirectory("boot")
            fs.makeDirectory("lib")
            fs.makeDirectory("bin")
            fs.makeDirectory("etc")
            fs.makeDirectory("usr")
            fs.makeDirectory("tmp")
            fs.makeDirectory("home")
            
            // Create minimal boot loader that works with our machine
            fs.writeFile("init.lua", """
-- OpenOS Mini Boot Loader
-- Minimal implementation for OpenComputers NeoForge

local gpu = component.proxy(component.list("gpu")())
local screen = component.list("screen")()
if gpu and screen then gpu.bind(screen) end

local w, h = gpu and gpu.getResolution() or 50, 16

local function clear()
    if gpu then gpu.fill(1, 1, w, h, " ") end
end

local function print(msg)
    if not gpu then return end
    -- Simple print at bottom of screen
    gpu.copy(1, 2, w, h - 1, 0, -1)
    gpu.fill(1, h, w, 1, " ")
    gpu.set(1, h, tostring(msg))
end

clear()
gpu.set(1, 1, "OpenOS Mini Shell v1.0")
gpu.set(1, 2, "Type 'help' for commands")
gpu.set(1, 3, "")

local cursor = 4
local input = ""

local function prompt()
    gpu.set(1, cursor, "> " .. input .. "_")
end

prompt()

while true do
    local sig, addr, char, code, player = computer.pullSignal(0.5)
    
    if sig == "key_down" then
        if code == 28 then -- Enter
            gpu.set(1, cursor, "> " .. input .. " ")
            cursor = cursor + 1
            
            if input == "help" then
                print("Commands: help, exit, list, clear")
            elseif input == "exit" then
                computer.shutdown()
            elseif input == "list" then
                for addr, type in component.list() do
                    print(type .. ": " .. addr:sub(1, 8))
                end
            elseif input == "clear" then
                clear()
                cursor = 1
            elseif #input > 0 then
                local fn, err = load(input)
                if fn then
                    local ok, result = pcall(fn)
                    if ok then
                        if result ~= nil then print(tostring(result)) end
                    else
                        print("Error: " .. tostring(result))
                    end
                else
                    print("Syntax: " .. tostring(err))
                end
            end
            
            input = ""
            if cursor > h then
                gpu.copy(1, 2, w, h - 1, 0, -1)
                gpu.fill(1, h, w, 1, " ")
                cursor = h
            end
            prompt()
        elseif code == 14 then -- Backspace
            if #input > 0 then
                input = input:sub(1, -2)
                gpu.fill(1, cursor, w, 1, " ")
                prompt()
            end
        elseif char and char > 31 and char < 127 then
            input = input .. unicode.char(char)
            gpu.fill(1, cursor, w, 1, " ")
            prompt()
        end
    end
end
            """.trimIndent())
        }
    }
    
    fun dropContents(level: Level, pos: BlockPos) {
        if (!diskStack.isEmpty) {
            Containers.dropItemStack(level, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, diskStack)
            diskStack = ItemStack.EMPTY
            filesystem = null
        }
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        
        if (!diskStack.isEmpty) {
            val diskTag = diskStack.save(registries)
            tag.put(TAG_DISK, diskTag)
        }
        
        floppyUUID?.let { tag.putUUID(TAG_FLOPPY_UUID, it) }
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        
        if (tag.contains(TAG_DISK)) {
            val diskTag = tag.getCompound(TAG_DISK)
            diskStack = ItemStack.parse(registries, diskTag).orElse(ItemStack.EMPTY)
            
            if (tag.hasUUID(TAG_FLOPPY_UUID)) {
                floppyUUID = tag.getUUID(TAG_FLOPPY_UUID)
            }
            
            if (!diskStack.isEmpty) {
                filesystem = createFilesystemForFloppy()
            }
        }
    }
}
