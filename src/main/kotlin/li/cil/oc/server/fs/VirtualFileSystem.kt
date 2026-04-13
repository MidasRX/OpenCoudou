package li.cil.oc.server.fs

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag

/**
 * In-memory virtual filesystem.
 * Supports files/directories, open/read/write/close, list, exists, etc.
 * Used for both hard drives (read-write) and loot disks (can be read-only).
 */
class VirtualFileSystem(
    val capacity: Long = 2 * 1024 * 1024, // 2MB default
    var readOnly: Boolean = false,
    var label: String = ""
) {
    // File tree
    private val root = VFSNode.Directory("/", mutableMapOf())
    // Open handles
    private val handles = mutableMapOf<Int, FileHandle>()
    private var nextHandle = 1

    val spaceUsed: Long get() = computeSize(root)

    private fun computeSize(node: VFSNode): Long = when (node) {
        is VFSNode.File -> node.data.size.toLong()
        is VFSNode.Directory -> node.children.values.sumOf { computeSize(it) }
    }

    // ========== Path resolution ==========

    private fun normalizePath(path: String): String {
        val cleaned = path.replace("\\", "/").removePrefix("/").removeSuffix("/")
        if (cleaned.isEmpty()) return ""
        // Resolve . and ..
        val parts = cleaned.split("/")
        val resolved = mutableListOf<String>()
        for (p in parts) {
            when (p) {
                "", "." -> {}
                ".." -> if (resolved.isNotEmpty()) resolved.removeLast()
                else -> resolved.add(p)
            }
        }
        return resolved.joinToString("/")
    }

    private fun resolve(path: String): VFSNode? {
        val norm = normalizePath(path)
        if (norm.isEmpty()) return root
        var node: VFSNode = root
        for (part in norm.split("/")) {
            node = (node as? VFSNode.Directory)?.children?.get(part) ?: return null
        }
        return node
    }

    private fun resolveParent(path: String): Pair<VFSNode.Directory, String>? {
        val norm = normalizePath(path)
        if (norm.isEmpty()) return null
        val parts = norm.split("/")
        val name = parts.last()
        var node: VFSNode = root
        for (part in parts.dropLast(1)) {
            node = (node as? VFSNode.Directory)?.children?.get(part) ?: return null
        }
        return (node as? VFSNode.Directory)?.let { it to name }
    }

    // ========== Filesystem operations ==========

    fun exists(path: String): Boolean = resolve(path) != null

    fun isDirectory(path: String): Boolean = resolve(path) is VFSNode.Directory

    fun size(path: String): Long {
        val node = resolve(path) ?: return 0
        return when (node) {
            is VFSNode.File -> node.data.size.toLong()
            is VFSNode.Directory -> 0
        }
    }

    fun lastModified(path: String): Long {
        val node = resolve(path) ?: return 0
        return when (node) {
            is VFSNode.File -> node.lastModified
            is VFSNode.Directory -> 0
        }
    }

    fun list(path: String): List<String>? {
        val node = resolve(path)
        if (node !is VFSNode.Directory) return null
        return node.children.map { (name, child) ->
            if (child is VFSNode.Directory) "$name/" else name
        }.sorted()
    }

    fun makeDirectory(path: String): Boolean {
        if (readOnly) return false
        val norm = normalizePath(path)
        if (norm.isEmpty()) return false
        var node: VFSNode = root
        for (part in norm.split("/")) {
            val dir = node as? VFSNode.Directory ?: return false
            val child = dir.children[part]
            if (child == null) {
                dir.children[part] = VFSNode.Directory(part, mutableMapOf())
                node = dir.children[part]!!
            } else {
                if (child !is VFSNode.Directory) return false
                node = child
            }
        }
        return true
    }

    fun remove(path: String): Boolean {
        if (readOnly) return false
        val (parent, name) = resolveParent(path) ?: return false
        return parent.children.remove(name) != null
    }

    fun rename(from: String, to: String): Boolean {
        if (readOnly) return false
        val (fromParent, fromName) = resolveParent(from) ?: return false
        val node = fromParent.children[fromName] ?: return false
        // Ensure destination parent exists
        val toNorm = normalizePath(to)
        val toParts = toNorm.split("/")
        val toName = toParts.last()
        var targetParent: VFSNode = root
        for (part in toParts.dropLast(1)) {
            targetParent = (targetParent as? VFSNode.Directory)?.children?.get(part) ?: return false
        }
        val targetDir = targetParent as? VFSNode.Directory ?: return false
        fromParent.children.remove(fromName)
        node.name = toName
        targetDir.children[toName] = node
        return true
    }

    // ========== File I/O ==========

    fun open(path: String, mode: String = "r"): Int? {
        val isRead = mode.startsWith("r")
        val isWrite = mode.startsWith("w")
        val isAppend = mode.startsWith("a")

        if ((isWrite || isAppend) && readOnly) return null

        val norm = normalizePath(path)
        if (norm.isEmpty()) return null

        if (isRead) {
            val node = resolve(path) as? VFSNode.File ?: return null
            val handle = nextHandle++
            handles[handle] = FileHandle(node, 0, false)
            return handle
        }

        if (isWrite || isAppend) {
            // Ensure parent exists
            val (parent, name) = resolveParent(path) ?: return null
            var file = parent.children[name] as? VFSNode.File
            if (file == null) {
                file = VFSNode.File(name, mutableListOf(), System.currentTimeMillis())
                parent.children[name] = file
            } else if (isWrite) {
                file.data.clear()
                file.lastModified = System.currentTimeMillis()
            }
            val handle = nextHandle++
            val offset = if (isAppend) file.data.size else 0
            handles[handle] = FileHandle(file, offset, true)
            return handle
        }

        return null
    }

    fun read(handle: Int, count: Int): ByteArray? {
        val fh = handles[handle] ?: return null
        val file = fh.file
        if (fh.offset >= file.data.size) return null // EOF
        val remaining = file.data.size - fh.offset
        val toRead = minOf(count, remaining)
        val data = ByteArray(toRead) { file.data[fh.offset + it] }
        fh.offset += toRead
        return data
    }

    fun write(handle: Int, data: ByteArray): Boolean {
        val fh = handles[handle] ?: return false
        if (!fh.writable) return false
        val currentFileSize = fh.file.data.size.toLong()
        val newFileSize = maxOf(currentFileSize, (fh.offset + data.size).toLong())
        val sizeIncrease = newFileSize - currentFileSize
        if (sizeIncrease > 0 && spaceUsed + sizeIncrease > capacity && !readOnly) return false
        val file = fh.file
        // Extend if needed
        while (file.data.size < fh.offset) file.data.add(0)
        for (i in data.indices) {
            if (fh.offset + i < file.data.size) {
                file.data[fh.offset + i] = data[i]
            } else {
                file.data.add(data[i])
            }
        }
        fh.offset += data.size
        file.lastModified = System.currentTimeMillis()
        return true
    }

    fun seek(handle: Int, whence: String, offset: Long): Long? {
        val fh = handles[handle] ?: return null
        val newPos = when (whence) {
            "set" -> offset
            "cur" -> fh.offset + offset
            "end" -> fh.file.data.size + offset
            else -> return null
        }
        fh.offset = newPos.toInt().coerceAtLeast(0)
        return fh.offset.toLong()
    }

    fun close(handle: Int): Boolean {
        return handles.remove(handle) != null
    }

    /** Close all open file handles (used on machine shutdown to prevent leaks). */
    fun closeAllHandles() {
        handles.clear()
    }

    // ========== Convenience: write a string file ==========

    fun writeFile(path: String, content: String) {
        val norm = normalizePath(path)
        val parts = norm.split("/")
        // Ensure parent directories exist
        if (parts.size > 1) {
            makeDirectory(parts.dropLast(1).joinToString("/"))
        }
        val (parent, name) = resolveParent(path) ?: return
        parent.children[name] = VFSNode.File(name, content.toByteArray().toMutableList(), System.currentTimeMillis())
    }

    fun readFile(path: String): String? {
        val node = resolve(path) as? VFSNode.File ?: return null
        return String(node.data.toByteArray())
    }

    // ========== Internal types ==========

    private class FileHandle(
        val file: VFSNode.File,
        var offset: Int,
        val writable: Boolean
    )

    // ========== NBT persistence ==========

    fun saveToTag(): CompoundTag {
        val tag = CompoundTag()
        tag.putLong("capacity", capacity)
        tag.putBoolean("readOnly", readOnly)
        tag.putString("label", label)
        tag.put("root", saveNode(root))
        return tag
    }

    private fun saveNode(node: VFSNode): CompoundTag {
        val tag = CompoundTag()
        tag.putString("name", node.name)
        when (node) {
            is VFSNode.File -> {
                tag.putString("type", "file")
                tag.putByteArray("data", node.data.toByteArray())
                tag.putLong("modified", node.lastModified)
            }
            is VFSNode.Directory -> {
                tag.putString("type", "dir")
                val children = ListTag()
                for ((_, child) in node.children) {
                    children.add(saveNode(child))
                }
                tag.put("children", children)
            }
        }
        return tag
    }

    companion object {
        fun loadFromTag(tag: CompoundTag): VirtualFileSystem {
            val fs = VirtualFileSystem(
                capacity = tag.getLong("capacity"),
                readOnly = tag.getBoolean("readOnly"),
                label = tag.getString("label")
            )
            // Load the file tree into the root
            if (tag.contains("root")) {
                val rootTag = tag.getCompound("root")
                loadNodeInto(fs.root, rootTag)
            }
            return fs
        }

        private fun loadNodeInto(dir: VFSNode.Directory, tag: CompoundTag) {
            val children = tag.getList("children", Tag.TAG_COMPOUND.toInt())
            for (i in 0 until children.size) {
                val childTag = children.getCompound(i)
                val name = childTag.getString("name")
                val type = childTag.getString("type")
                when (type) {
                    "file" -> {
                        val data = childTag.getByteArray("data")
                        val modified = childTag.getLong("modified")
                        dir.children[name] = VFSNode.File(name, data.toMutableList(), modified)
                    }
                    "dir" -> {
                        val subDir = VFSNode.Directory(name, mutableMapOf())
                        loadNodeInto(subDir, childTag)
                        dir.children[name] = subDir
                    }
                }
            }
        }
    }
}

sealed class VFSNode {
    abstract var name: String

    class File(
        override var name: String,
        val data: MutableList<Byte>,
        var lastModified: Long
    ) : VFSNode()

    class Directory(
        override var name: String,
        val children: MutableMap<String, VFSNode>
    ) : VFSNode()
}
