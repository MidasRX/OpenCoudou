package li.cil.oc.client.os.filesystem

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * Persistent filesystem for SkibidiOS2.
 * 
 * This filesystem persists data to NBT storage (saved in HDD component's block entity).
 * Unlike VirtualFileSystem which is memory-only, this survives:
 * - Computer shutdown/restart
 * - Chunk unload/reload  
 * - Server restart
 * - World save/load
 *
 * Usage:
 * - Create with an HDD component address
 * - Call loadFromNBT() when computer boots
 * - Call saveToNBT() when computer saves or shuts down
 * - Use markDirty() to flag that save is needed
 */
class PersistentFileSystem(
    val address: String,
    val capacity: Long = 1024 * 1024 * 2 // 2MB default
) {
    // Underlying virtual filesystem for operations
    private val vfs = VirtualFileSystem()
    
    // Track if changes need saving
    private var dirty = false
    
    // Callbacks for save events
    private var saveCallback: ((CompoundTag) -> Unit)? = null
    
    // ==================== Persistence ====================
    
    /**
     * Set callback for when filesystem needs to save.
     * The callback receives NBT data to store.
     */
    fun setSaveCallback(callback: (CompoundTag) -> Unit) {
        saveCallback = callback
    }
    
    /**
     * Mark filesystem as needing save.
     */
    fun markDirty() {
        dirty = true
    }
    
    /**
     * Check if filesystem has unsaved changes.
     */
    fun isDirty(): Boolean = dirty
    
    /**
     * Save filesystem to NBT.
     * Call this when HDD component saves.
     */
    fun saveToNBT(): CompoundTag {
        val tag = CompoundTag()
        tag.putString("address", address)
        tag.putLong("capacity", capacity)
        tag.putInt("version", 2)
        
        // Save all files and directories
        val filesTag = ListTag()
        saveNode(vfs, "/", filesTag)
        tag.put("files", filesTag)
        
        dirty = false
        return tag
    }
    
    private fun saveNode(fs: VirtualFileSystem, path: String, list: ListTag) {
        val entries = fs.list(path) ?: return
        
        for (entry in entries) {
            val fullPath = if (path == "/") "/$entry" else "$path/$entry"
            val stat = fs.stat(fullPath) ?: continue
            
            val nodeTag = CompoundTag()
            nodeTag.putString("path", fullPath)
            nodeTag.putInt("permissions", stat.permissions)
            nodeTag.putLong("created", stat.created)
            nodeTag.putLong("modified", stat.modified)
            
            if (stat.isDirectory) {
                nodeTag.putString("type", "dir")
                list.add(nodeTag)
                // Recurse into directory
                saveNode(fs, fullPath, list)
            } else {
                nodeTag.putString("type", "file")
                // Store file data as base64 string for NBT compatibility
                val data = fs.readFile(fullPath) ?: ByteArray(0)
                nodeTag.putString("data", Base64.getEncoder().encodeToString(data))
                nodeTag.putLong("size", data.size.toLong())
                list.add(nodeTag)
            }
        }
    }
    
    /**
     * Load filesystem from NBT.
     * Call this when HDD component loads.
     */
    fun loadFromNBT(tag: CompoundTag): Boolean {
        if (!tag.contains("files")) return false
        
        val version = tag.getInt("version")
        val filesList = tag.getList("files", Tag.TAG_COMPOUND.toInt())
        
        // Clear existing data (except standard dirs)
        clearUserData()
        
        // Load all entries
        for (i in 0 until filesList.size) {
            val nodeTag = filesList.getCompound(i)
            val path = nodeTag.getString("path")
            val type = nodeTag.getString("type")
            val permissions = nodeTag.getInt("permissions")
            val created = nodeTag.getLong("created")
            val modified = nodeTag.getLong("modified")
            
            if (type == "dir") {
                vfs.mkdir(path)
            } else {
                val dataBase64 = nodeTag.getString("data")
                val data = try {
                    Base64.getDecoder().decode(dataBase64)
                } catch (e: Exception) {
                    ByteArray(0)
                }
                vfs.writeFile(path, data)
            }
            
            // Restore metadata
            val stat = vfs.stat(path)
            // Note: Would need to expose setters on FSNode to restore permissions/timestamps
        }
        
        dirty = false
        return true
    }
    
    /**
     * Clear user data but keep system directories.
     */
    private fun clearUserData() {
        // Keep /bin, /etc, /lib, /usr, /var - clear /home, /mnt, /tmp
        vfs.delete("/home/user", recursive = true)
        vfs.mkdir("/home/user")
        vfs.delete("/tmp", recursive = true)
        vfs.mkdir("/tmp")
    }
    
    /**
     * Trigger save if dirty.
     */
    fun saveIfDirty() {
        if (dirty) {
            saveCallback?.invoke(saveToNBT())
        }
    }
    
    // ==================== Delegated Operations ====================
    // These delegate to VirtualFileSystem but mark dirty on writes
    
    fun exists(path: String) = vfs.exists(path)
    fun isFile(path: String) = vfs.isFile(path)
    fun isDirectory(path: String) = vfs.isDirectory(path)
    fun size(path: String) = vfs.size(path)
    fun list(path: String) = vfs.list(path)
    fun listDetailed(path: String) = vfs.listDetailed(path)
    fun stat(path: String) = vfs.stat(path)
    fun readFile(path: String) = vfs.readFile(path)
    fun readText(path: String) = vfs.readText(path)
    
    fun mkdir(path: String): Boolean {
        val result = vfs.mkdir(path)
        if (result) markDirty()
        return result
    }
    
    fun mkdirs(path: String): Boolean {
        val result = vfs.mkdirs(path)
        if (result) markDirty()
        return result
    }
    
    fun touch(path: String): Boolean {
        val result = vfs.touch(path)
        if (result) markDirty()
        return result
    }
    
    fun delete(path: String, recursive: Boolean = false): Boolean {
        val result = vfs.delete(path, recursive)
        if (result) markDirty()
        return result
    }
    
    fun rename(from: String, to: String): Boolean {
        val result = vfs.rename(from, to)
        if (result) markDirty()
        return result
    }
    
    fun copy(from: String, to: String, recursive: Boolean = false): Boolean {
        val result = vfs.copy(from, to, recursive)
        if (result) markDirty()
        return result
    }
    
    fun writeFile(path: String, data: ByteArray): Boolean {
        // Check capacity
        val currentUsed = spaceUsed()
        val existingSize = if (vfs.exists(path)) vfs.size(path) else 0
        val newSize = data.size.toLong()
        
        if (currentUsed - existingSize + newSize > capacity) {
            return false // Not enough space
        }
        
        val result = vfs.writeFile(path, data)
        if (result) markDirty()
        return result
    }
    
    fun writeText(path: String, content: String): Boolean {
        return writeFile(path, content.toByteArray(Charsets.UTF_8))
    }
    
    fun appendFile(path: String, data: ByteArray): Boolean {
        // Check capacity
        val currentUsed = spaceUsed()
        val newSize = data.size.toLong()
        
        if (currentUsed + newSize > capacity) {
            return false
        }
        
        val result = vfs.appendFile(path, data)
        if (result) markDirty()
        return result
    }
    
    fun appendText(path: String, content: String): Boolean {
        return appendFile(path, content.toByteArray(Charsets.UTF_8))
    }
    
    // File handles
    fun open(path: String, mode: String = "r"): Int? {
        val handle = vfs.open(path, mode)
        if (handle != null && ("w" in mode || "a" in mode)) {
            markDirty() // Will be modified
        }
        return handle
    }
    
    fun read(handle: Int, count: Int = -1) = vfs.read(handle, count)
    
    fun write(handle: Int, data: ByteArray): Boolean {
        val result = vfs.write(handle, data)
        if (result) markDirty()
        return result
    }
    
    fun seek(handle: Int, whence: String, offset: Int) = vfs.seek(handle, whence, offset)
    
    fun close(handle: Int): Boolean {
        val result = vfs.close(handle)
        saveIfDirty() // Save when file is closed
        return result
    }
    
    // Space info
    fun spaceTotal() = capacity
    fun spaceUsed() = vfs.spaceUsed()
    fun spaceFree() = capacity - spaceUsed()
    
    // Mount operations (delegate)
    fun mount(address: String, path: String, label: String, readonly: Boolean = false) = 
        vfs.mount(address, path, label, readonly)
    fun unmount(path: String) = vfs.unmount(path)
    fun getMounts() = vfs.getMounts()
    fun getMount(path: String) = vfs.getMount(path)
    
    companion object {
        // Cache of filesystems by HDD address
        private val instances = ConcurrentHashMap<String, PersistentFileSystem>()
        
        /**
         * Get or create persistent filesystem for an HDD.
         */
        fun getOrCreate(address: String, capacity: Long = 1024 * 1024 * 2): PersistentFileSystem {
            return instances.getOrPut(address) {
                PersistentFileSystem(address, capacity)
            }
        }
        
        /**
         * Remove filesystem from cache (when HDD is removed).
         */
        fun remove(address: String): PersistentFileSystem? {
            return instances.remove(address)
        }
        
        /**
         * Get all active filesystems.
         */
        fun getAll(): Collection<PersistentFileSystem> = instances.values
        
        /**
         * Save all dirty filesystems.
         */
        fun saveAll() {
            instances.values.forEach { it.saveIfDirty() }
        }
    }
}

/**
 * HDD tiers with different capacities.
 */
enum class HDDTier(val capacity: Long, val label: String) {
    TIER1(512 * 1024, "512KB HDD"),           // 512 KB
    TIER2(1024 * 1024, "1MB HDD"),            // 1 MB
    TIER3(2 * 1024 * 1024, "2MB HDD"),        // 2 MB
    TIER4(4 * 1024 * 1024, "4MB HDD"),        // 4 MB
    TIER5(8 * 1024 * 1024, "8MB HDD"),        // 8 MB - Creative
    FLOPPY(512 * 1024, "Floppy Disk");        // 512 KB
    
    companion object {
        fun fromTier(tier: Int): HDDTier = when (tier) {
            1 -> TIER1
            2 -> TIER2
            3 -> TIER3
            4 -> TIER4
            5 -> TIER5
            else -> TIER1
        }
    }
}
