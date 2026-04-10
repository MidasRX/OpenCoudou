package li.cil.oc.server.component

/**
 * RAID component - combines multiple hard drives into a single large drive.
 * Provides redundancy and increased storage capacity.
 */
class RaidComponent : AbstractComponent("raid") {
    
    companion object {
        const val MAX_DRIVES = 3
    }
    
    // Connected drive addresses
    private val driveAddresses = mutableListOf<String>()
    
    // Combined filesystem info
    private var totalCapacity: Long = 0
    private var usedSpace: Long = 0
    private var label: String = "raid"
    
    init {
        registerMethod("getLabel", true, "getLabel():string -- Get RAID label") { _ ->
            arrayOf(label)
        }
        
        registerMethod("setLabel", false, "setLabel(label:string):string -- Set RAID label") { args ->
            val newLabel = args.getOrNull(0)?.toString()?.take(32) ?: label
            val oldLabel = label
            label = newLabel
            arrayOf(oldLabel)
        }
        
        registerMethod("getDrives", true, "getDrives():table -- Get connected drive addresses") { _ ->
            arrayOf(driveAddresses.toList())
        }
        
        registerMethod("spaceTotal", true, "spaceTotal():number -- Get total space") { _ ->
            arrayOf(totalCapacity)
        }
        
        registerMethod("spaceUsed", true, "spaceUsed():number -- Get used space") { _ ->
            arrayOf(usedSpace)
        }
        
        // Filesystem methods (delegates to combined drives)
        registerMethod("exists", true, "exists(path:string):boolean -- Check if file exists") { args ->
            val path = args.getOrNull(0)?.toString() ?: return@registerMethod arrayOf(false)
            // Would check across all drives
            arrayOf(false)
        }
        
        registerMethod("isDirectory", true, "isDirectory(path:string):boolean -- Check if path is directory") { args ->
            val path = args.getOrNull(0)?.toString() ?: return@registerMethod arrayOf(false)
            arrayOf(path == "/" || path.isEmpty())
        }
        
        registerMethod("list", true, "list(path:string):table -- List directory contents") { args ->
            val path = args.getOrNull(0)?.toString() ?: "/"
            // Would list combined contents
            arrayOf(emptyList<String>())
        }
        
        registerMethod("size", true, "size(path:string):number -- Get file size") { args ->
            val path = args.getOrNull(0)?.toString() ?: return@registerMethod arrayOf(0)
            arrayOf(0)
        }
        
        registerMethod("lastModified", true, "lastModified(path:string):number -- Get last modified time") { args ->
            val path = args.getOrNull(0)?.toString() ?: return@registerMethod arrayOf(0)
            arrayOf(System.currentTimeMillis())
        }
    }
    
    fun addDrive(address: String): Boolean {
        if (driveAddresses.size >= MAX_DRIVES) return false
        if (address in driveAddresses) return false
        
        driveAddresses.add(address)
        recalculateCapacity()
        return true
    }
    
    fun removeDrive(address: String): Boolean {
        val removed = driveAddresses.remove(address)
        if (removed) recalculateCapacity()
        return removed
    }
    
    private fun recalculateCapacity() {
        // Would sum up capacities of all drives
        totalCapacity = driveAddresses.size * 2 * 1024 * 1024L // 2MB per drive
    }
}
