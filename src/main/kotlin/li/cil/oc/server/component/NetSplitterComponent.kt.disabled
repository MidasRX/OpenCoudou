package li.cil.oc.server.component

import net.minecraft.core.Direction

/**
 * Net Splitter component - selectively connects/disconnects network sides.
 * Can be controlled via redstone or programmatically.
 */
class NetSplitterComponent : AbstractComponent("net_splitter") {
    
    // Connection state for each side (true = connected)
    private val sideConnections = mutableMapOf<Direction, Boolean>().apply {
        Direction.entries.forEach { this[it] = true }
    }
    
    init {
        registerMethod("open", false, "open(side:number):boolean -- Open (disconnect) a side") { args ->
            val sideNum = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(false)
            val side = numToDirection(sideNum) ?: return@registerMethod arrayOf(false)
            val oldState = sideConnections[side] ?: true
            sideConnections[side] = false
            arrayOf(oldState)
        }
        
        registerMethod("close", false, "close(side:number):boolean -- Close (connect) a side") { args ->
            val sideNum = (args.getOrNull(0) as? Number)?.toInt() ?: return@registerMethod arrayOf(false)
            val side = numToDirection(sideNum) ?: return@registerMethod arrayOf(false)
            val oldState = sideConnections[side] ?: true
            sideConnections[side] = true
            arrayOf(!oldState)
        }
        
        registerMethod("getSides", true, "getSides():table -- Get connection state of all sides") { _ ->
            val result = mutableMapOf<Int, Boolean>()
            Direction.entries.forEach { dir ->
                result[directionToNum(dir)] = sideConnections[dir] ?: true
            }
            arrayOf(result)
        }
        
        registerMethod("setSides", false, "setSides(sides:table):boolean -- Set connection state of all sides") { args ->
            @Suppress("UNCHECKED_CAST")
            val sides = args.getOrNull(0) as? Map<Any, Any> ?: return@registerMethod arrayOf(false)
            
            for ((key, value) in sides) {
                val sideNum = (key as? Number)?.toInt() ?: continue
                val connected = value as? Boolean ?: continue
                val side = numToDirection(sideNum) ?: continue
                sideConnections[side] = connected
            }
            
            arrayOf(true)
        }
    }
    
    fun isConnected(side: Direction): Boolean = sideConnections[side] ?: true
    
    fun setConnected(side: Direction, connected: Boolean) {
        sideConnections[side] = connected
    }
    
    private fun numToDirection(num: Int): Direction? {
        return when (num) {
            0 -> Direction.DOWN
            1 -> Direction.UP
            2 -> Direction.NORTH
            3 -> Direction.SOUTH
            4 -> Direction.WEST
            5 -> Direction.EAST
            else -> null
        }
    }
    
    private fun directionToNum(dir: Direction): Int {
        return when (dir) {
            Direction.DOWN -> 0
            Direction.UP -> 1
            Direction.NORTH -> 2
            Direction.SOUTH -> 3
            Direction.WEST -> 4
            Direction.EAST -> 5
        }
    }
}
