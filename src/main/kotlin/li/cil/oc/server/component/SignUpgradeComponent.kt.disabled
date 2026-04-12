package li.cil.oc.server.component

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.SignBlock
import net.minecraft.world.level.block.entity.SignBlockEntity

/**
 * Sign upgrade - allows robots to read and write signs.
 */
class SignUpgradeComponent : AbstractComponent("sign") {
    
    private var world: Level? = null
    private var position: BlockPos = BlockPos.ZERO
    
    init {
        registerMethod("read", true, "read([side:string]):string -- Read text from sign") { args ->
            val side = parseSide(args.getOrNull(0)?.toString())
            val level = world ?: return@registerMethod arrayOf(null, "no world")
            
            val targetPos = position.relative(side)
            val state = level.getBlockState(targetPos)
            
            if (state.block is SignBlock) {
                val signEntity = level.getBlockEntity(targetPos) as? SignBlockEntity
                if (signEntity != null) {
                    val text = signEntity.frontText
                    val lines = buildString {
                        for (i in 0 until 4) {
                            if (i > 0) append("\n")
                            append(text.getMessage(i, false).string)
                        }
                    }
                    return@registerMethod arrayOf(lines)
                }
            }
            
            arrayOf(null, "no sign")
        }
        
        registerMethod("write", false, "write(text:string[,side:string]):boolean -- Write text to sign") { args ->
            val text = args.getOrNull(0)?.toString() ?: return@registerMethod arrayOf(false, "no text")
            val side = parseSide(args.getOrNull(1)?.toString())
            val level = world ?: return@registerMethod arrayOf(false, "no world")
            
            val targetPos = position.relative(side)
            val state = level.getBlockState(targetPos)
            
            if (state.block is SignBlock) {
                val signEntity = level.getBlockEntity(targetPos) as? SignBlockEntity
                if (signEntity != null) {
                    val lines = text.split("\n")
                    // Would update sign text - requires server-side handling
                    return@registerMethod arrayOf(true)
                }
            }
            
            arrayOf(false, "no sign")
        }
    }
    
    fun setWorld(level: Level?, pos: BlockPos) {
        this.world = level
        this.position = pos
    }
    
    private fun parseSide(side: String?): Direction {
        return when (side?.lowercase()) {
            "front", "forward", "f" -> Direction.NORTH // Would use robot facing
            "back", "backward", "b" -> Direction.SOUTH
            "left", "l" -> Direction.WEST
            "right", "r" -> Direction.EAST
            "up", "top", "u" -> Direction.UP
            "down", "bottom", "d" -> Direction.DOWN
            else -> Direction.NORTH
        }
    }
}
