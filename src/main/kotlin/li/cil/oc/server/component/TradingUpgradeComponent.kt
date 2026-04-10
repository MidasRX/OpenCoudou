package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB

/**
 * Trading upgrade - allows robots to trade with villagers.
 */
class TradingUpgradeComponent : AbstractComponent("trading") {
    
    companion object {
        const val TRADE_RANGE = 4.0
    }
    
    private var world: Level? = null
    private var position: BlockPos = BlockPos.ZERO
    
    init {
        registerMethod("getVillagers", true, "getVillagers():table -- Get nearby villagers") { _ ->
            val level = world ?: return@registerMethod arrayOf(emptyList<Any>())
            
            val centerX = position.x + 0.5
            val centerY = position.y + 0.5
            val centerZ = position.z + 0.5
            
            val box = AABB(
                centerX - TRADE_RANGE, centerY - TRADE_RANGE, centerZ - TRADE_RANGE,
                centerX + TRADE_RANGE, centerY + TRADE_RANGE, centerZ + TRADE_RANGE
            )
            
            val villagers = level.getEntitiesOfClass(Villager::class.java, box)
            
            val result = villagers.mapIndexed { index, villager ->
                mapOf(
                    "index" to (index + 1),
                    "profession" to villager.villagerData.profession.name,
                    "level" to villager.villagerData.level,
                    "x" to villager.x,
                    "y" to villager.y,
                    "z" to villager.z
                )
            }
            
            arrayOf(result)
        }
        
        registerMethod("getTrades", true, "getTrades(villagerIndex:number):table -- Get villager's trades") { args ->
            val villagerIndex = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
            
            val level = world ?: return@registerMethod arrayOf(emptyList<Any>())
            
            val centerX = position.x + 0.5
            val centerY = position.y + 0.5
            val centerZ = position.z + 0.5
            
            val box = AABB(
                centerX - TRADE_RANGE, centerY - TRADE_RANGE, centerZ - TRADE_RANGE,
                centerX + TRADE_RANGE, centerY + TRADE_RANGE, centerZ + TRADE_RANGE
            )
            
            val villagers = level.getEntitiesOfClass(Villager::class.java, box)
            
            if (villagerIndex !in villagers.indices) {
                return@registerMethod arrayOf(emptyList<Any>(), "villager not found")
            }
            
            val villager = villagers[villagerIndex]
            val offers = villager.offers
            
            val result = offers.mapIndexed { index, offer ->
                mapOf(
                    "index" to (index + 1),
                    "costA" to stackToTable(offer.baseCostA),
                    "costB" to if (offer.costB.isEmpty) null else stackToTable(offer.costB),
                    "result" to stackToTable(offer.result),
                    "uses" to offer.uses,
                    "maxUses" to offer.maxUses,
                    "disabled" to offer.isOutOfStock
                )
            }
            
            arrayOf(result)
        }
        
        registerMethod("trade", false, "trade(villagerIndex:number,tradeIndex:number[,count:number]):boolean,number -- Perform trade") { args ->
            val villagerIndex = (args.getOrNull(0) as? Number)?.toInt()?.minus(1) ?: 0
            val tradeIndex = (args.getOrNull(1) as? Number)?.toInt()?.minus(1) ?: 0
            val count = (args.getOrNull(2) as? Number)?.toInt()?.coerceIn(1, 64) ?: 1
            
            val level = world ?: return@registerMethod arrayOf(false, "no world")
            
            val centerX = position.x + 0.5
            val centerY = position.y + 0.5
            val centerZ = position.z + 0.5
            
            val box = AABB(
                centerX - TRADE_RANGE, centerY - TRADE_RANGE, centerZ - TRADE_RANGE,
                centerX + TRADE_RANGE, centerY + TRADE_RANGE, centerZ + TRADE_RANGE
            )
            
            val villagers = level.getEntitiesOfClass(Villager::class.java, box)
            
            if (villagerIndex !in villagers.indices) {
                return@registerMethod arrayOf(false, "villager not found")
            }
            
            val villager = villagers[villagerIndex]
            val offers = villager.offers
            
            if (tradeIndex !in offers.indices) {
                return@registerMethod arrayOf(false, "trade not found")
            }
            
            val offer = offers[tradeIndex]
            
            if (offer.isOutOfStock) {
                return@registerMethod arrayOf(false, "trade out of stock")
            }
            
            // Would perform trade - requires robot inventory integration
            // For now, just mark success
            
            arrayOf(true, count)
        }
    }
    
    fun setWorld(level: Level?, pos: BlockPos) {
        this.world = level
        this.position = pos
    }
    
    private fun stackToTable(stack: ItemStack): Map<String, Any?> {
        return mapOf(
            "name" to stack.item.builtInRegistryHolder().key().location().toString(),
            "label" to stack.hoverName.string,
            "count" to stack.count
        )
    }
}
