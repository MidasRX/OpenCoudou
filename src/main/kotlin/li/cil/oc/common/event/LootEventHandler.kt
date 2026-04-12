package li.cil.oc.common.event

import li.cil.oc.OpenComputers
import li.cil.oc.common.init.ModItems
import net.minecraft.world.item.ItemStack
import org.slf4j.LoggerFactory

/**
 * Handles loot table modifications to add OpenComputers items to world gen.
 * 
 * Note: In 1.21.4, LootTableLoadEvent may not exist - use LootDiskModifier instead.
 * This is kept for reference but may not be functional.
 */
object LootEventHandler {
    
    private val logger = LoggerFactory.getLogger("OpenComputers")
    
    // Loot tables that should receive OpenComputers items
    private val DUNGEON_LOOT_TABLES = listOf(
        "minecraft:chests/simple_dungeon",
        "minecraft:chests/abandoned_mineshaft",
        "minecraft:chests/stronghold_corridor",
        "minecraft:chests/stronghold_crossing",
        "minecraft:chests/stronghold_library"
    )
    
    private val VILLAGE_LOOT_TABLES = listOf(
        "minecraft:chests/village/village_weaponsmith",
        "minecraft:chests/village/village_toolsmith",
        "minecraft:chests/village/village_armorer"
    )
    
    private val END_LOOT_TABLES = listOf(
        "minecraft:chests/end_city_treasure"
    )
    
    /**
     * Modifies loot tables to include OpenComputers items.
     * Called during loot table loading.
     */
    fun modifyLootTable(tableName: String, addItemCallback: (ItemStack, Float, Int, Int) -> Unit) {
        when {
            tableName in DUNGEON_LOOT_TABLES -> {
                // Add basic components to dungeon loot
                addItemCallback(ItemStack(ModItems.TRANSISTOR.get()), 0.4f, 1, 4)
                addItemCallback(ItemStack(ModItems.MICROCHIP_TIER1.get()), 0.2f, 1, 2)
                addItemCallback(ItemStack(ModItems.MEMORY_TIER1.get()), 0.1f, 1, 1)
            }
            tableName in VILLAGE_LOOT_TABLES -> {
                // Add crafting materials to village smithy loot
                addItemCallback(ItemStack(ModItems.TRANSISTOR.get()), 0.5f, 2, 6)
                addItemCallback(ItemStack(ModItems.MICROCHIP_TIER1.get()), 0.3f, 1, 3)
                addItemCallback(ItemStack(ModItems.RAW_CIRCUIT_BOARD.get()), 0.2f, 1, 2)
            }
            tableName in END_LOOT_TABLES -> {
                // Add rare/high tier items to end city loot
                addItemCallback(ItemStack(ModItems.MICROCHIP_TIER3.get()), 0.4f, 1, 3)
                addItemCallback(ItemStack(ModItems.CPU_TIER3.get()), 0.2f, 1, 1)
                addItemCallback(ItemStack(ModItems.MEMORY_TIER3.get()), 0.15f, 1, 1)
                addItemCallback(ItemStack(ModItems.GPU_TIER3.get()), 0.1f, 1, 1)
            }
        }
    }
    
    /**
     * Get the list of items that can appear in structure loot.
     * Used by global loot modifiers.
     */
    fun getStructureLootItems(): List<StructureLootItem> {
        return listOf(
            // Basic components - common in dungeons
            StructureLootItem(
                item = { ModItems.TRANSISTOR.get() },
                weight = 30,
                minCount = 1,
                maxCount = 4,
                structures = listOf("minecraft:dungeon", "minecraft:mineshaft")
            ),
            StructureLootItem(
                item = { ModItems.MICROCHIP_TIER1.get() },
                weight = 15,
                minCount = 1,
                maxCount = 2,
                structures = listOf("minecraft:dungeon", "minecraft:stronghold")
            ),
            // Rare items in end cities
            StructureLootItem(
                item = { ModItems.CPU_TIER3.get() },
                weight = 5,
                minCount = 1,
                maxCount = 1,
                structures = listOf("minecraft:end_city")
            ),
            StructureLootItem(
                item = { ModItems.MEMORY_TIER3_5.get() },
                weight = 3,
                minCount = 1,
                maxCount = 1,
                structures = listOf("minecraft:end_city")
            )
        )
    }
    
    data class StructureLootItem(
        val item: () -> net.minecraft.world.item.Item,
        val weight: Int,
        val minCount: Int,
        val maxCount: Int,
        val structures: List<String>
    )
}
