package li.cil.oc.server.component

import net.minecraft.world.item.ItemStack

/**
 * Database upgrade component - stores item stack information for comparison.
 * Used by transposer, geolyzer, and inventory controller to identify items.
 * 
 * Tier 1: 9 slots
 * Tier 2: 25 slots
 * Tier 3: 81 slots
 */
class DatabaseUpgradeComponent(
    val tier: Int = 1
) : AbstractComponent("database") {
    
    companion object {
        val SLOT_COUNTS = intArrayOf(9, 25, 81)
    }
    
    // Database entries - stores item stack fingerprints
    private val entries: Array<DatabaseEntry?> = arrayOfNulls(getSlotCount())
    
    init {
        registerMethod("get", true, "get(slot:number):table -- Get info for database entry") { args ->
            val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
            if (slot < 0 || slot >= entries.size) {
                return@registerMethod arrayOf(null, "invalid slot")
            }
            
            val entry = entries[slot] ?: return@registerMethod arrayOf(null)
            arrayOf(entry.toTable())
        }
        
        registerMethod("set", false, "set(slot:number,itemId:string,damage:number,maxDamage:number,maxCount:number,label:string):boolean -- Set database entry") { args ->
            val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
            if (slot < 0 || slot >= entries.size) {
                return@registerMethod arrayOf(false, "invalid slot")
            }
            
            val itemId = args.getOrNull(1)?.toString() ?: ""
            val damage = (args.getOrNull(2) as? Number)?.toInt() ?: 0
            val maxDamage = (args.getOrNull(3) as? Number)?.toInt() ?: 0
            val maxCount = (args.getOrNull(4) as? Number)?.toInt() ?: 64
            val label = args.getOrNull(5)?.toString() ?: itemId
            
            if (itemId.isEmpty()) {
                entries[slot] = null
            } else {
                entries[slot] = DatabaseEntry(itemId, damage, maxDamage, maxCount, label)
            }
            arrayOf(true)
        }
        
        registerMethod("clear", false, "clear(slot:number):boolean -- Clear database entry") { args ->
            val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
            if (slot < 0 || slot >= entries.size) {
                return@registerMethod arrayOf(false, "invalid slot")
            }
            entries[slot] = null
            arrayOf(true)
        }
        
        registerMethod("indexOf", true, "indexOf(itemId:string[,damage:number]):number -- Find slot containing item") { args ->
            val itemId = args.getOrNull(0)?.toString() ?: return@registerMethod arrayOf(-1)
            val damage = (args.getOrNull(1) as? Number)?.toInt()
            
            for (i in entries.indices) {
                val entry = entries[i] ?: continue
                if (entry.itemId == itemId && (damage == null || entry.damage == damage)) {
                    return@registerMethod arrayOf(i + 1) // 1-indexed
                }
            }
            arrayOf(-1)
        }
        
        registerMethod("computeHash", true, "computeHash(slot:number):string -- Get hash of entry") { args ->
            val slot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
            if (slot < 0 || slot >= entries.size) {
                return@registerMethod arrayOf(null, "invalid slot")
            }
            
            val entry = entries[slot] ?: return@registerMethod arrayOf("")
            arrayOf(entry.computeHash())
        }
        
        registerMethod("copy", false, "copy(fromSlot:number,toSlot:number,targetAddress:string):boolean -- Copy entry to another database") { args ->
            val fromSlot = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
            val toSlot = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
            val targetAddress = args.getOrNull(2)?.toString()
            
            if (fromSlot < 0 || fromSlot >= entries.size) {
                return@registerMethod arrayOf(false, "invalid source slot")
            }
            
            // Local copy within same database
            if (targetAddress.isNullOrEmpty() || targetAddress == address) {
                if (toSlot < 0 || toSlot >= entries.size) {
                    return@registerMethod arrayOf(false, "invalid target slot")
                }
                entries[toSlot] = entries[fromSlot]?.copy()
                return@registerMethod arrayOf(true)
            }
            
            // Cross-database copy would require network lookup
            arrayOf(false, "cross-database copy not implemented")
        }
        
        registerMethod("clone", false, "clone(targetAddress:string):boolean -- Clone entire database") { args ->
            // Would copy all entries to another database
            arrayOf(false, "not implemented")
        }
    }
    
    private fun getSlotCount(): Int = SLOT_COUNTS.getOrElse(tier - 1) { 9 }
    
    /**
     * Store an ItemStack into the database at the given slot.
     */
    fun storeStack(slot: Int, stack: ItemStack): Boolean {
        if (slot < 0 || slot >= entries.size) return false
        if (stack.isEmpty) {
            entries[slot] = null
            return true
        }
        
        entries[slot] = DatabaseEntry(
            itemId = stack.item.descriptionId,
            damage = stack.damageValue,
            maxDamage = stack.maxDamage,
            maxCount = stack.maxStackSize,
            label = stack.displayName.string
        )
        return true
    }
    
    /**
     * Compare an ItemStack to a database entry.
     */
    fun compareStack(slot: Int, stack: ItemStack, checkNbt: Boolean = false): Boolean {
        if (slot < 0 || slot >= entries.size) return false
        val entry = entries[slot] ?: return stack.isEmpty
        if (stack.isEmpty) return false
        
        return entry.itemId == stack.item.descriptionId &&
               entry.damage == stack.damageValue
    }
    
    /**
     * Get database entry at slot.
     */
    fun getEntry(slot: Int): DatabaseEntry? {
        if (slot < 0 || slot >= entries.size) return null
        return entries[slot]
    }
    
    /**
     * Database entry data class.
     */
    data class DatabaseEntry(
        val itemId: String,
        val damage: Int,
        val maxDamage: Int,
        val maxCount: Int,
        val label: String
    ) {
        fun toTable(): Map<String, Any> = mapOf(
            "name" to itemId,
            "damage" to damage,
            "maxDamage" to maxDamage,
            "maxCount" to maxCount,
            "label" to label
        )
        
        fun computeHash(): String {
            var hash = 0
            itemId.forEach { hash = hash * 31 + it.code }
            hash = hash * 31 + damage
            return String.format("%08x", hash)
        }
    }
}
