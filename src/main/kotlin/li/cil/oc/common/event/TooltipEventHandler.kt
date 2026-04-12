package li.cil.oc.common.event

import li.cil.oc.OpenComputers
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent
import org.slf4j.LoggerFactory

/**
 * Handles tooltip additions for OpenComputers items.
 */
@EventBusSubscriber(modid = OpenComputers.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = [Dist.CLIENT])
object TooltipEventHandler {
    
    private val logger = LoggerFactory.getLogger("OpenComputers")
    
    @SubscribeEvent
    @JvmStatic
    fun onItemTooltip(event: ItemTooltipEvent) {
        val stack = event.itemStack
        val tooltip = event.toolTip
        val flags = event.flags
        
        val itemId = stack.item.descriptionId
        
        // Only process OpenComputers items
        if (!itemId.contains("opencomputers")) return
        
        // Add tier information
        addTierTooltip(stack, itemId, tooltip)
        
        // Add component slot info
        addSlotTooltip(stack, itemId, tooltip)
        
        // Add power consumption info
        addPowerTooltip(stack, itemId, tooltip)
        
        // Add special info for specific items
        addSpecialTooltip(stack, itemId, tooltip)
    }
    
    private fun addTierTooltip(stack: ItemStack, itemId: String, tooltip: MutableList<Component>) {
        val tier = extractTier(itemId)
        if (tier > 0) {
            val tierText = when (tier) {
                1 -> "§7Tier: §eI"
                2 -> "§7Tier: §6II"
                3 -> "§7Tier: §cIII"
                else -> null
            }
            tierText?.let { tooltip.add(Component.literal(it)) }
        }
    }
    
    private fun addSlotTooltip(stack: ItemStack, itemId: String, tooltip: MutableList<Component>) {
        val slotType = when {
            itemId.contains("cpu") -> "CPU Slot"
            itemId.contains("memory") || itemId.contains("ram") -> "Memory Slot"
            itemId.contains("gpu") || itemId.contains("graphics_card") -> "Card Slot (Tier 1)"
            itemId.contains("hdd") || itemId.contains("hard_drive") -> "HDD Slot"
            itemId.contains("eeprom") -> "EEPROM Slot"
            itemId.contains("card") -> "Card Slot"
            itemId.contains("upgrade") -> "Upgrade Slot"
            else -> null
        }
        
        slotType?.let {
            tooltip.add(Component.literal("§8$it"))
        }
    }
    
    private fun addPowerTooltip(stack: ItemStack, itemId: String, tooltip: MutableList<Component>) {
        // Show power consumption for components
        val consumption = when {
            itemId.contains("cpu_t1") -> 20
            itemId.contains("cpu_t2") -> 40
            itemId.contains("cpu_t3") -> 80
            itemId.contains("gpu_t1") -> 10
            itemId.contains("gpu_t2") -> 25
            itemId.contains("gpu_t3") -> 50
            itemId.contains("memory_t1") -> 5
            itemId.contains("memory_t2") -> 10
            itemId.contains("memory_t3") -> 15
            itemId.contains("internet_card") -> 30
            itemId.contains("wireless_network_card") -> 20
            itemId.contains("redstone_card") -> 5
            itemId.contains("data_card") -> 15
            itemId.contains("world_sensor_card") -> 25
            else -> 0
        }
        
        if (consumption > 0) {
            tooltip.add(Component.literal("§9Power: §b${consumption} RF/t"))
        }
    }
    
    private fun addSpecialTooltip(stack: ItemStack, itemId: String, tooltip: MutableList<Component>) {
        when {
            itemId.contains("analyzer") -> {
                tooltip.add(Component.literal("§7Right-click blocks to analyze"))
                tooltip.add(Component.literal("§7Shows component addresses"))
            }
            itemId.contains("terminal") -> {
                tooltip.add(Component.literal("§7Wireless terminal"))
                tooltip.add(Component.literal("§7Range: 16 blocks"))
            }
            itemId.contains("tablet") -> {
                tooltip.add(Component.literal("§7Portable computer"))
                tooltip.add(Component.literal("§7Shift+right-click to open"))
            }
            itemId.contains("manual") -> {
                tooltip.add(Component.literal("§7In-game documentation"))
                tooltip.add(Component.literal("§aRight-click to open"))
            }
            itemId.contains("debug_card") -> {
                tooltip.add(Component.literal("§4Creative-only item"))
                tooltip.add(Component.literal("§cAllows cheating"))
            }
            itemId.contains("creative") -> {
                tooltip.add(Component.literal("§4Creative-only item"))
            }
            itemId.contains("internet_card") -> {
                tooltip.add(Component.literal("§7Allows HTTP requests"))
                tooltip.add(Component.literal("§7and TCP connections"))
            }
            itemId.contains("linked_card") -> {
                tooltip.add(Component.literal("§7Quantum entangled card"))
                tooltip.add(Component.literal("§7Works across dimensions"))
            }
            itemId.contains("hover_upgrade") -> {
                val tier = extractTier(itemId)
                val height = when (tier) {
                    1 -> 8
                    2 -> 64
                    else -> 8
                }
                tooltip.add(Component.literal("§7Max hover height: §e$height blocks"))
            }
            itemId.contains("chunkloader_upgrade") -> {
                tooltip.add(Component.literal("§7Keeps chunks loaded"))
                tooltip.add(Component.literal("§7while robot is active"))
            }
            itemId.contains("experience_upgrade") -> {
                tooltip.add(Component.literal("§7Stores experience"))
                tooltip.add(Component.literal("§7from killed mobs"))
            }
            itemId.contains("angel_upgrade") -> {
                tooltip.add(Component.literal("§7Place blocks in mid-air"))
            }
        }
    }
    
    private fun extractTier(itemId: String): Int {
        return when {
            itemId.contains("_t1") || itemId.contains("tier1") || itemId.contains("_1") -> 1
            itemId.contains("_t2") || itemId.contains("tier2") || itemId.contains("_2") -> 2
            itemId.contains("_t3") || itemId.contains("tier3") || itemId.contains("_3") -> 3
            else -> 0
        }
    }
}
