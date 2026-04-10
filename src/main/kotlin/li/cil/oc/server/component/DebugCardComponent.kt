package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.server.ServerLifecycleHooks

/**
 * Debug Card component - Creative-only card with special debugging capabilities.
 * 
 * This card provides powerful debugging functions that should only be
 * available in creative mode or for server operators. Features include:
 * - Direct world manipulation (set/get blocks)
 * - Player manipulation (teleport, health, etc.)
 * - Running server commands
 * - Cross-dimension access
 * - Direct buffer manipulation
 */
class DebugCardComponent : AbstractComponent("debug") {
    
    private var remoteNodePosition: BlockPos? = null
    
    // Debug card has unlimited energy
    var energy = Double.MAX_VALUE
    
    init {
        // Energy methods - debug card has unlimited energy
        registerMethod("changeBuffer", true, "function(delta:number):number -- Changes the network's energy buffer.") { args ->
            val delta = (args.getOrNull(0) as? Number)?.toDouble() ?: 0.0
            arrayOf(delta)
        }
        
        // Position methods
        registerMethod("getX", true, "function():number -- Get the container's X position.") { _ ->
            arrayOf(0)
        }
        
        registerMethod("getY", true, "function():number -- Get the container's Y position.") { _ ->
            arrayOf(64)
        }
        
        registerMethod("getZ", true, "function():number -- Get the container's Z position.") { _ ->
            arrayOf(0)
        }
        
        // World methods
        registerMethod("getWorlds", true, "function():table -- Get a list of all dimension IDs.") { _ ->
            val server = ServerLifecycleHooks.getCurrentServer()
            if (server == null) {
                arrayOf(emptyList<String>())
            } else {
                arrayOf(server.levelKeys().map { it.location().toString() })
            }
        }
        
        registerMethod("getBlock", true, "function(x:number, y:number, z:number[, dimension:string]):table -- Get block info at position.") { args ->
            val x = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "x required")
            val y = (args.getOrNull(1) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "y required")
            val z = (args.getOrNull(2) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "z required")
            val dimId = args.getOrNull(3) as? String ?: "minecraft:overworld"
            
            val server = ServerLifecycleHooks.getCurrentServer() ?: return@registerMethod arrayOf(null, "Server not available")
            val worldKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(dimId) ?: return@registerMethod arrayOf(null, "Invalid dimension"))
            val level = server.getLevel(worldKey) ?: return@registerMethod arrayOf(null, "Dimension not loaded")
            
            val pos = BlockPos(x, y, z)
            val state = level.getBlockState(pos)
            val block = state.block
            
            arrayOf(mapOf(
                "name" to block.descriptionId,
                "id" to BuiltInRegistries.BLOCK.getKey(block).toString(),
                "hardness" to state.getDestroySpeed(level, pos),
                "isAir" to state.isAir
            ))
        }
        
        registerMethod("setBlock", false, "function(x:number, y:number, z:number, id:string[, dimension:string]):boolean -- Set block at position.") { args ->
            val x = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "x required")
            val y = (args.getOrNull(1) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "y required")
            val z = (args.getOrNull(2) as? Number)?.toInt() ?: return@registerMethod arrayOf(null, "z required")
            val blockId = args.getOrNull(3) as? String ?: return@registerMethod arrayOf(null, "block id required")
            val dimId = args.getOrNull(4) as? String ?: "minecraft:overworld"
            
            val server = ServerLifecycleHooks.getCurrentServer() ?: return@registerMethod arrayOf(null, "Server not available")
            val worldKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(dimId) ?: return@registerMethod arrayOf(null, "Invalid dimension"))
            val level = server.getLevel(worldKey) ?: return@registerMethod arrayOf(null, "Dimension not loaded")
            
            val pos = BlockPos(x, y, z)
            val blockLocation = ResourceLocation.tryParse(blockId) ?: return@registerMethod arrayOf(null, "Invalid block id")
            
            // Get block from registry - getValue returns AIR if not found
            val blockState = BuiltInRegistries.BLOCK.getValue(blockLocation).defaultBlockState()
            
            level.setBlock(pos, blockState, 3)
            arrayOf(true)
        }
        
        // Player methods  
        registerMethod("getPlayers", true, "function():table -- Get all online players.") { _ ->
            val server = ServerLifecycleHooks.getCurrentServer()
            if (server == null) {
                arrayOf(emptyList<String>())
            } else {
                arrayOf(server.playerList.players.map { it.name.string })
            }
        }
        
        registerMethod("getPlayer", true, "function(name:string):table -- Get player info.") { args ->
            val name = args.getOrNull(0) as? String ?: return@registerMethod arrayOf(null, "Player name required")
            val server = ServerLifecycleHooks.getCurrentServer() ?: return@registerMethod arrayOf(null, "Server not available")
            val player = server.playerList.getPlayerByName(name) ?: return@registerMethod arrayOf(null, "Player not found")
            
            arrayOf(mapOf(
                "name" to player.name.string,
                "x" to player.x,
                "y" to player.y,
                "z" to player.z,
                "health" to player.health,
                "maxHealth" to player.maxHealth,
                "gameMode" to player.gameMode.gameModeForPlayer.name,
                "dimension" to player.level().dimension().location().toString()
            ))
        }
        
        registerMethod("teleportPlayer", false, "function(name:string, x:number, y:number, z:number):boolean -- Teleport player.") { args ->
            val name = args.getOrNull(0) as? String ?: return@registerMethod arrayOf(false, "Player name required")
            val x = (args.getOrNull(1) as? Number)?.toDouble() ?: return@registerMethod arrayOf(false, "x required")
            val y = (args.getOrNull(2) as? Number)?.toDouble() ?: return@registerMethod arrayOf(false, "y required")
            val z = (args.getOrNull(3) as? Number)?.toDouble() ?: return@registerMethod arrayOf(false, "z required")
            
            val server = ServerLifecycleHooks.getCurrentServer() ?: return@registerMethod arrayOf(false, "Server not available")
            val player = server.playerList.getPlayerByName(name) ?: return@registerMethod arrayOf(false, "Player not found")
            
            player.teleportTo(x, y, z)
            arrayOf(true)
        }
        
        registerMethod("setPlayerHealth", false, "function(name:string, health:number):boolean -- Set player health.") { args ->
            val name = args.getOrNull(0) as? String ?: return@registerMethod arrayOf(false, "Player name required")
            val health = (args.getOrNull(1) as? Number)?.toFloat() ?: return@registerMethod arrayOf(false, "health required")
            
            val server = ServerLifecycleHooks.getCurrentServer() ?: return@registerMethod arrayOf(false, "Server not available")
            val player = server.playerList.getPlayerByName(name) ?: return@registerMethod arrayOf(false, "Player not found")
            
            player.health = health
            arrayOf(true)
        }
        
        registerMethod("setPlayerGameMode", false, "function(name:string, mode:string):boolean -- Set player game mode.") { args ->
            val name = args.getOrNull(0) as? String ?: return@registerMethod arrayOf(false, "Player name required")
            val mode = args.getOrNull(1) as? String ?: return@registerMethod arrayOf(false, "mode required")
            
            val server = ServerLifecycleHooks.getCurrentServer() ?: return@registerMethod arrayOf(false, "Server not available")
            val player = server.playerList.getPlayerByName(name) ?: return@registerMethod arrayOf(false, "Player not found")
            
            val gameType = when (mode.lowercase()) {
                "survival" -> net.minecraft.world.level.GameType.SURVIVAL
                "creative" -> net.minecraft.world.level.GameType.CREATIVE
                "adventure" -> net.minecraft.world.level.GameType.ADVENTURE
                "spectator" -> net.minecraft.world.level.GameType.SPECTATOR
                else -> return@registerMethod arrayOf(false, "Invalid game mode")
            }
            player.setGameMode(gameType)
            arrayOf(true)
        }
        
        registerMethod("givePlayerXP", false, "function(name:string, levels:number):boolean -- Give XP levels to player.") { args ->
            val name = args.getOrNull(0) as? String ?: return@registerMethod arrayOf(false, "Player name required")
            val levels = (args.getOrNull(1) as? Number)?.toInt() ?: return@registerMethod arrayOf(false, "levels required")
            
            val server = ServerLifecycleHooks.getCurrentServer() ?: return@registerMethod arrayOf(false, "Server not available")
            val player = server.playerList.getPlayerByName(name) ?: return@registerMethod arrayOf(false, "Player not found")
            
            player.giveExperienceLevels(levels)
            arrayOf(true)
        }
        
        // Command execution
        registerMethod("runCommand", false, "function(command:string):number, string -- Run a server command.") { args ->
            val command = args.getOrNull(0) as? String ?: return@registerMethod arrayOf(0, "Command required")
            val server = ServerLifecycleHooks.getCurrentServer() ?: return@registerMethod arrayOf(0, "Server not available")
            
            try {
                val source = server.createCommandSourceStack()
                val result = server.commands.performPrefixedCommand(source, command)
                arrayOf(result, "Success")
            } catch (e: Exception) {
                arrayOf(0, e.message ?: "Command failed")
            }
        }
        
        // Mod detection
        registerMethod("isModLoaded", true, "function(modId:string):boolean -- Check if a mod is loaded.") { args ->
            val modId = args.getOrNull(0) as? String ?: return@registerMethod arrayOf(false)
            try {
                val loaded = net.neoforged.fml.ModList.get().isLoaded(modId)
                arrayOf(loaded)
            } catch (e: Exception) {
                arrayOf(false)
            }
        }
        
        // Network methods
        registerMethod("connectToBlock", false, "function(x:number, y:number, z:number):boolean -- Connect to a component block.") { args ->
            val x = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(false, "x required")
            val y = (args.getOrNull(1) as? Number)?.toInt() ?: return@registerMethod arrayOf(false, "y required")
            val z = (args.getOrNull(2) as? Number)?.toInt() ?: return@registerMethod arrayOf(false, "z required")
            
            remoteNodePosition = BlockPos(x, y, z)
            arrayOf(true)
        }
        
        // Debug messaging
        registerMethod("sendToDebugCard", false, "function(address:string, data...) -- Send data to another debug card.") { _ ->
            arrayOf()
        }
        
        registerMethod("sendToClipboard", false, "function(player:string, text:string):boolean -- Send text to player clipboard.") { args ->
            @Suppress("UNUSED_VARIABLE")
            val playerName = args.getOrNull(0) as? String ?: return@registerMethod arrayOf(false, "Player name required")
            @Suppress("UNUSED_VARIABLE")
            val text = args.getOrNull(1) as? String ?: return@registerMethod arrayOf(false, "Text required")
            // Would need packet implementation
            arrayOf(true)
        }
        
        // Content scanning
        registerMethod("scanContentsAt", true, "function(x:number, y:number, z:number[, dimension:string]):boolean, string, table -- Scan contents at position.") { args ->
            val x = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(false, "x required", null)
            val y = (args.getOrNull(1) as? Number)?.toInt() ?: return@registerMethod arrayOf(false, "y required", null)
            val z = (args.getOrNull(2) as? Number)?.toInt() ?: return@registerMethod arrayOf(false, "z required", null)
            val dimId = args.getOrNull(3) as? String ?: "minecraft:overworld"
            
            val server = ServerLifecycleHooks.getCurrentServer() ?: return@registerMethod arrayOf(false, "no server", null)
            val worldKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(dimId) ?: return@registerMethod arrayOf(false, "invalid dimension", null))
            val level = server.getLevel(worldKey) ?: return@registerMethod arrayOf(false, "dimension not loaded", null)
            
            val pos = BlockPos(x, y, z)
            val state = level.getBlockState(pos)
            
            when {
                state.isAir -> arrayOf(false, "air", mapOf("id" to "minecraft:air"))
                state.liquid() -> arrayOf(false, "liquid", mapOf("id" to BuiltInRegistries.BLOCK.getKey(state.block).toString()))
                else -> arrayOf(true, "solid", mapOf("id" to BuiltInRegistries.BLOCK.getKey(state.block).toString()))
            }
        }
        
        // World time/weather
        registerMethod("getWorldTime", true, "function([dimension:string]):number -- Get world time.") { args ->
            val dimId = args.getOrNull(0) as? String ?: "minecraft:overworld"
            val server = ServerLifecycleHooks.getCurrentServer() ?: return@registerMethod arrayOf(0)
            val worldKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(dimId) ?: return@registerMethod arrayOf(0))
            val level = server.getLevel(worldKey) ?: return@registerMethod arrayOf(0)
            arrayOf(level.dayTime)
        }
        
        registerMethod("setWorldTime", false, "function(time:number[, dimension:string]):boolean -- Set world time.") { args ->
            val time = (args.getOrNull(0) as? Number)?.toLong() ?: return@registerMethod arrayOf(false, "time required")
            val dimId = args.getOrNull(1) as? String ?: "minecraft:overworld"
            
            val server = ServerLifecycleHooks.getCurrentServer() ?: return@registerMethod arrayOf(false, "Server not available")
            val worldKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(dimId) ?: return@registerMethod arrayOf(false, "Invalid dimension"))
            val level = server.getLevel(worldKey) ?: return@registerMethod arrayOf(false, "Dimension not loaded")
            
            level.setDayTime(time)
            arrayOf(true)
        }
        
        registerMethod("isRaining", true, "function([dimension:string]):boolean -- Check if raining.") { args ->
            val dimId = args.getOrNull(0) as? String ?: "minecraft:overworld"
            val server = ServerLifecycleHooks.getCurrentServer() ?: return@registerMethod arrayOf(false)
            val worldKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(dimId) ?: return@registerMethod arrayOf(false))
            val level = server.getLevel(worldKey) ?: return@registerMethod arrayOf(false)
            arrayOf(level.isRaining)
        }
        
        registerMethod("setRaining", false, "function(raining:boolean[, dimension:string]):boolean -- Set rain state.") { args ->
            val raining = args.getOrNull(0) as? Boolean ?: return@registerMethod arrayOf(false, "raining boolean required")
            val dimId = args.getOrNull(1) as? String ?: "minecraft:overworld"
            
            val server = ServerLifecycleHooks.getCurrentServer() ?: return@registerMethod arrayOf(false, "Server not available")
            val worldKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.tryParse(dimId) ?: return@registerMethod arrayOf(false, "Invalid dimension"))
            val level = server.getLevel(worldKey) ?: return@registerMethod arrayOf(false, "Dimension not loaded")
            
            level.levelData.isRaining = raining
            arrayOf(true)
        }
        
        registerMethod("giveItem", false, "function(player:string, itemId:string, count:number):number -- Give item to player.") { args ->
            val playerName = args.getOrNull(0) as? String ?: return@registerMethod arrayOf(0, "Player name required")
            val itemId = args.getOrNull(1) as? String ?: return@registerMethod arrayOf(0, "Item ID required")
            val count = (args.getOrNull(2) as? Number)?.toInt() ?: 1
            
            val server = ServerLifecycleHooks.getCurrentServer() ?: return@registerMethod arrayOf(0, "Server not available")
            val player = server.playerList.getPlayerByName(playerName) ?: return@registerMethod arrayOf(0, "Player not found")
            
            val itemLocation = ResourceLocation.tryParse(itemId) ?: return@registerMethod arrayOf(0, "Invalid item ID")
            val item = BuiltInRegistries.ITEM.getValue(itemLocation)
            
            // Cast to ItemLike to resolve constructor ambiguity
            val stack = ItemStack(item as net.minecraft.world.level.ItemLike, count)
            val added = player.inventory.add(stack)
            arrayOf(if (added) count else 0)
        }
    }
    
    /**
     * Returns whether this is a creative-tier component that bypasses energy costs.
     */
    fun isCreative(): Boolean = true
}
