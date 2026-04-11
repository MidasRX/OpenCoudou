package li.cil.oc.server.component

import li.cil.oc.util.OCLogger
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * A virtual file with data and metadata.
 */
private data class VirtualFile(
    var data: ByteArray,
    var lastModified: Long = System.currentTimeMillis()
) {
    val size: Int get() = data.size
}

/**
 * Filesystem component that provides file storage.
 * Used for HDDs, floppies, and tmpfs.
 */
class FilesystemComponent(
    val label: String = "filesystem",
    val capacity: Long = 1048576, // 1MB default
    val isReadOnly: Boolean = false
) : AbstractComponent("filesystem") {
    
    // In-memory filesystem (would use NBT in real implementation)
    private val files = mutableMapOf<String, VirtualFile>()
    private var usedSpace = 0L
    
    init {
        registerMethod("getLabel", true, "getLabel():string -- Get filesystem label") { _ ->
            arrayOf(label)
        }
        
        registerMethod("setLabel", false, "setLabel(label:string):string -- Set filesystem label") { args ->
            // Would update label
            val newLabel = args.getOrNull(0)?.toString() ?: label
            arrayOf(newLabel)
        }
        
        registerMethod("isReadOnly", true, "isReadOnly():boolean -- Is filesystem read-only") { _ ->
            arrayOf(isReadOnly)
        }
        
        registerMethod("spaceTotal", true, "spaceTotal():number -- Total space in bytes") { _ ->
            arrayOf(capacity)
        }
        
        registerMethod("spaceUsed", true, "spaceUsed():number -- Used space in bytes") { _ ->
            arrayOf(usedSpace)
        }
        
        registerMethod("exists", true, "exists(path:string):boolean -- Check if path exists") { args ->
            val path = normalizePath(args.getOrNull(0)?.toString() ?: "")
            arrayOf(files.containsKey(path) || isDirectory(path))
        }
        
        registerMethod("isDirectory", true, "isDirectory(path:string):boolean -- Check if path is directory") { args ->
            val path = normalizePath(args.getOrNull(0)?.toString() ?: "")
            arrayOf(isDirectory(path))
        }
        
        registerMethod("list", true, "list(path:string):table -- List directory contents") { args ->
            val path = normalizePath(args.getOrNull(0)?.toString() ?: "")
            val prefix = if (path.isEmpty()) "" else "$path/"
            val entries = files.keys
                .filter { it.startsWith(prefix) }
                .map { 
                    val relative = it.removePrefix(prefix).split("/").first()
                    // Append / for directories (entries that have children)
                    val isDir = files.keys.any { key -> 
                        key.startsWith(if (prefix.isEmpty()) "$relative/" else "$prefix$relative/") 
                    }
                    if (isDir) "$relative/" else relative
                }
                .distinct()
                .sorted()
            arrayOf(entries)
        }
        
        registerMethod("makeDirectory", false, "makeDirectory(path:string):boolean -- Create directory") { args ->
            if (isReadOnly) return@registerMethod arrayOf(false, "filesystem is read-only")
            // Directories are implicit in our implementation
            arrayOf(true)
        }
        
        registerMethod("remove", false, "remove(path:string):boolean -- Remove file or directory") { args ->
            if (isReadOnly) return@registerMethod arrayOf(false, "filesystem is read-only")
            val path = normalizePath(args.getOrNull(0)?.toString() ?: "")
            val removed = files.remove(path)
            if (removed != null) {
                usedSpace -= removed.size
                arrayOf(true)
            } else {
                arrayOf(false, "no such file")
            }
        }
        
        registerMethod("rename", false, "rename(from:string,to:string):boolean -- Rename file") { args ->
            if (isReadOnly) return@registerMethod arrayOf(false, "filesystem is read-only")
            val from = normalizePath(args.getOrNull(0)?.toString() ?: "")
            val to = normalizePath(args.getOrNull(1)?.toString() ?: "")
            val data = files.remove(from)
            if (data != null) {
                files[to] = data
                arrayOf(true)
            } else {
                arrayOf(false, "no such file")
            }
        }
        
        registerMethod("size", true, "size(path:string):number -- Get file size") { args ->
            val path = normalizePath(args.getOrNull(0)?.toString() ?: "")
            arrayOf(files[path]?.size ?: 0)
        }
        
        registerMethod("lastModified", true, "lastModified(path:string):number -- Get last modified time") { args ->
            val path = normalizePath(args.getOrNull(0)?.toString() ?: "")
            arrayOf((files[path]?.lastModified ?: System.currentTimeMillis()) / 1000.0)
        }
        
        registerMethod("open", false, "open(path:string[,mode:string]):number -- Open file handle") { args ->
            val path = normalizePath(args.getOrNull(0)?.toString() ?: "")
            val mode = args.getOrNull(1)?.toString() ?: "r"
            
            if (mode.contains("w") && isReadOnly) {
                return@registerMethod arrayOf(null, "filesystem is read-only")
            }
            
            // Return file handle or nil + error
            val handle = openFile(path, mode)
            if (handle != null) {
                arrayOf(handle)
            } else {
                arrayOf(null, "file not found")
            }
        }
        
        registerMethod("read", true, "read(handle:number,count:number):string -- Read from file") { args ->
            val handle = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "invalid handle")
            val count = (args.getOrNull(1) as? Number)?.toInt() ?: 1024
            
            val result = readFromHandle(handle, count)
            arrayOf(result)
        }
        
        registerMethod("write", false, "write(handle:number,data:string):boolean -- Write to file") { args ->
            if (isReadOnly) return@registerMethod arrayOf(false, "filesystem is read-only")
            
            val handle = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(false, "invalid handle")
            val data = args.getOrNull(1)?.toString() ?: ""
            
            val success = writeToHandle(handle, data)
            arrayOf(success)
        }
        
        registerMethod("seek", false, "seek(handle:number,whence:string,offset:number):number -- Seek in file") { args ->
            val handle = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "invalid handle")
            val whence = args.getOrNull(1)?.toString() ?: "cur"
            val offset = (args.getOrNull(2) as? Number)?.toLong() ?: 0L
            
            val newPos = seekHandle(handle, whence, offset)
            arrayOf(newPos)
        }
        
        registerMethod("close", false, "close(handle:number) -- Close file handle") { args ->
            val handle = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf()
            closeHandle(handle)
            arrayOf()
        }
    }
    
    private fun normalizePath(path: String): String {
        return path.trim('/').replace("//", "/")
    }
    
    private fun isDirectory(path: String): Boolean {
        if (path.isEmpty()) return true
        val prefix = "$path/"
        return files.keys.any { it.startsWith(prefix) }
    }
    
    // Simple file handle implementation
    private data class FileHandle(
        val path: String,
        val mode: String,
        var position: Int = 0
    )
    
    private val handles = mutableMapOf<Int, FileHandle>()
    private var nextHandle = 1
    
    private fun openFile(path: String, mode: String): Int? {
        // For read mode, file must exist
        if (!mode.contains("w") && !mode.contains("a") && !files.containsKey(path)) {
            return null  // File not found
        }
        
        val handle = nextHandle++
        
        // Create file if writing and doesn't exist
        if (mode.contains("w") && !files.containsKey(path)) {
            files[path] = VirtualFile(ByteArray(0))
        }
        
        handles[handle] = FileHandle(path, mode, if (mode.contains("a")) files[path]?.size ?: 0 else 0)
        return handle
    }
    
    private fun readFromHandle(handle: Int, count: Int): String? {
        val fh = handles[handle] ?: return null
        val vf = files[fh.path] ?: return null
        val data = vf.data
        
        if (fh.position >= data.size) return null // EOF
        
        val end = minOf(fh.position + count, data.size)
        val result = String(data, fh.position, end - fh.position, StandardCharsets.UTF_8)
        fh.position = end
        return result
    }
    
    private fun writeToHandle(handle: Int, data: String): Boolean {
        val fh = handles[handle] ?: return false
        if (!fh.mode.contains("w") && !fh.mode.contains("a")) return false
        
        val bytes = data.toByteArray(StandardCharsets.UTF_8)
        val vf = files[fh.path] ?: return false
        val existing = vf.data
        
        // Calculate new size
        val newSize = maxOf(fh.position + bytes.size, existing.size)
        if (usedSpace - existing.size + newSize > capacity) {
            return false // Out of space
        }
        
        val newData = ByteArray(newSize)
        System.arraycopy(existing, 0, newData, 0, minOf(existing.size, newSize))
        System.arraycopy(bytes, 0, newData, fh.position, bytes.size)
        
        usedSpace = usedSpace - existing.size + newData.size
        vf.data = newData
        vf.lastModified = System.currentTimeMillis()
        fh.position += bytes.size
        
        return true
    }
    
    private fun seekHandle(handle: Int, whence: String, offset: Long): Long {
        val fh = handles[handle] ?: return -1
        val fileSize = files[fh.path]?.size ?: 0
        
        fh.position = when (whence) {
            "set" -> offset.toInt()
            "cur" -> fh.position + offset.toInt()
            "end" -> fileSize + offset.toInt()
            else -> fh.position
        }.coerceIn(0, fileSize)
        
        return fh.position.toLong()
    }
    
    private fun closeHandle(handle: Int) {
        handles.remove(handle)
    }
    
    /**
     * Write a file directly (for initialization).
     */
    fun writeFile(path: String, content: String) {
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        val normalized = normalizePath(path)
        val existing = files[normalized]
        
        usedSpace = usedSpace - (existing?.size ?: 0) + bytes.size
        files[normalized] = VirtualFile(bytes)
    }
    
    /**
     * Create a directory marker (directories are implicit but this helps visibility).
     */
    fun makeDirectory(path: String): Boolean {
        // Directories are implicit in our implementation
        // We don't actually need to store anything
        return true
    }
    
    /**
     * Read a file directly.
     */
    fun readFile(path: String): String? {
        val vf = files[normalizePath(path)] ?: return null
        return String(vf.data, StandardCharsets.UTF_8)
    }
}
