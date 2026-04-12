package li.cil.oc.common.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

class SwitchBlock(properties: Properties) : Block(properties) {
    fun isSideEnabled(level: Level, pos: BlockPos, side: Direction): Boolean = true
    fun getPacketsRelayed(level: Level, pos: BlockPos): Long = 0L
    fun getBytesRelayed(level: Level, pos: BlockPos): Long = 0L
    fun getQueueSize(level: Level, pos: BlockPos): Int = 0
}
