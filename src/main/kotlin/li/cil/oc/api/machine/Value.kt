package li.cil.oc.api.machine

import net.minecraft.nbt.CompoundTag

/**
 * Represents a value that can be passed between Lua and Java.
 * 
 * Values can be serialized to NBT for persistence across saves and loads.
 * They can also be disposed of when no longer needed.
 * 
 * Common implementations:
 * - Primitive values (numbers, strings, booleans)
 * - Tables (maps of key-value pairs)
 * - Userdata (custom Java objects exposed to Lua)
 * 
 * @see Context
 */
interface Value {
    /**
     * Loads the value's state from NBT.
     * 
     * @param nbt The tag to load from
     */
    fun load(nbt: CompoundTag)
    
    /**
     * Saves the value's state to NBT.
     * 
     * @param nbt The tag to save to
     */
    fun save(nbt: CompoundTag)
    
    /**
     * Disposes of this value, releasing any resources.
     * Called when the value is garbage collected in Lua.
     * 
     * @param context The machine context
     */
    fun dispose(context: Context)
}


