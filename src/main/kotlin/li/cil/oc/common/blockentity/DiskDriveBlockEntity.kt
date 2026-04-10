package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.common.init.ModItems
import li.cil.oc.server.component.FilesystemComponent
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
    
    fun getDisk(): ItemStack = diskStack.copy()
    
    fun setDisk(stack: ItemStack) {
        val oldDisk = diskStack
        diskStack = if (stack.isEmpty) ItemStack.EMPTY else stack.copyWithCount(1)
        
        // Clear old filesystem
        if (!oldDisk.isEmpty) {
            filesystem = null
            floppyUUID = null
        }
        
        // Create new filesystem for the floppy
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
        // For OpenOS, we'd load the OpenOS files here
        // For now, just create a basic structure
        val item = diskStack.item
        if (item == ModItems.OPENOS.get()) {
            // OpenOS boot disk - would load actual OpenOS files
            fs.makeDirectory("boot")
            fs.makeDirectory("lib")
            fs.makeDirectory("bin")
            fs.makeDirectory("etc")
            fs.makeDirectory("usr")
            
            // Create minimal boot loader
            fs.writeFile("init.lua", """
                -- OpenOS Boot Loader
                local component = require("component")
                local computer = require("computer")
                
                print("OpenOS Loading...")
                
                -- Basic shell
                while true do
                    io.write("> ")
                    local line = io.read()
                    if line then
                        local fn, err = load(line)
                        if fn then
                            local ok, result = pcall(fn)
                            if ok then
                                if result ~= nil then print(result) end
                            else
                                print("Error: " .. tostring(result))
                            end
                        else
                            print("Syntax error: " .. err)
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
