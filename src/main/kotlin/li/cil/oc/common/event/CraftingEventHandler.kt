package li.cil.oc.common.event

import li.cil.oc.OpenComputers
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import org.slf4j.LoggerFactory

/**
 * Handles crafting-related events for OpenComputers.
 */
@EventBusSubscriber(modid = OpenComputers.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
object CraftingEventHandler {
    
    private val logger = LoggerFactory.getLogger("OpenComputers")
    
    @SubscribeEvent
    @JvmStatic
    fun onItemCrafted(event: PlayerEvent.ItemCraftedEvent) {
        val player = event.entity
        val stack = event.crafting
        val itemId = stack.item.descriptionId
        
        // Track crafting for statistics
        if (itemId.contains("opencomputers")) {
            trackCraftedItem(player, stack.item.toString())
        }
        
        // Special handling for first-time crafts
        when {
            itemId.contains("case") && !hasCraftedBefore(player, "case") -> {
                markCrafted(player, "case")
                logger.debug("Player ${player.name.string} crafted their first computer case!")
            }
            itemId.contains("robot") && !hasCraftedBefore(player, "robot") -> {
                markCrafted(player, "robot")
                logger.debug("Player ${player.name.string} built their first robot!")
            }
            itemId.contains("drone") && !hasCraftedBefore(player, "drone") -> {
                markCrafted(player, "drone")
                logger.debug("Player ${player.name.string} built their first drone!")
            }
        }
    }
    
    // Note: ItemPickupEvent was removed in 1.21 - use EntityItemPickupEvent instead
    // Or track via advancement triggers
    
    // Player data tracking using persistent data
    private const val OC_DATA_KEY = "opencomputers_progress"
    
    private fun hasCraftedBefore(player: Player, item: String): Boolean {
        val data = player.persistentData
        if (!data.contains(OC_DATA_KEY)) return false
        val ocData = data.getCompound(OC_DATA_KEY)
        return ocData.getBoolean("crafted_$item")
    }
    
    private fun markCrafted(player: Player, item: String) {
        val data = player.persistentData
        val ocData = if (data.contains(OC_DATA_KEY)) {
            data.getCompound(OC_DATA_KEY)
        } else {
            net.minecraft.nbt.CompoundTag()
        }
        ocData.putBoolean("crafted_$item", true)
        data.put(OC_DATA_KEY, ocData)
    }
    
    private fun hasPickedUp(player: Player, item: String): Boolean {
        val data = player.persistentData
        if (!data.contains(OC_DATA_KEY)) return false
        val ocData = data.getCompound(OC_DATA_KEY)
        return ocData.getBoolean("pickup_$item")
    }
    
    private fun markPickedUp(player: Player, item: String) {
        val data = player.persistentData
        val ocData = if (data.contains(OC_DATA_KEY)) {
            data.getCompound(OC_DATA_KEY)
        } else {
            net.minecraft.nbt.CompoundTag()
        }
        ocData.putBoolean("pickup_$item", true)
        data.put(OC_DATA_KEY, ocData)
    }
    
    private fun trackCraftedItem(player: Player, item: String) {
        val data = player.persistentData
        val ocData = if (data.contains(OC_DATA_KEY)) {
            data.getCompound(OC_DATA_KEY)
        } else {
            net.minecraft.nbt.CompoundTag()
        }
        
        val craftCountKey = "craft_count_$item"
        val currentCount = if (ocData.contains(craftCountKey)) ocData.getInt(craftCountKey) else 0
        ocData.putInt(craftCountKey, currentCount + 1)
        data.put(OC_DATA_KEY, ocData)
    }
    
    /**
     * Get crafting statistics for a player.
     */
    fun getCraftingStats(player: Player): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        val data = player.persistentData
        
        if (data.contains(OC_DATA_KEY)) {
            val ocData = data.getCompound(OC_DATA_KEY)
            for (key in ocData.allKeys) {
                if (key.startsWith("craft_count_")) {
                    val item = key.removePrefix("craft_count_")
                    stats[item] = ocData.getInt(key)
                }
            }
        }
        
        return stats
    }
    
    /**
     * Reset all progress for a player (for debugging).
     */
    fun resetProgress(player: Player) {
        player.persistentData.remove(OC_DATA_KEY)
    }
}
