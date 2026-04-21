package li.cil.oc.common.block

import li.cil.oc.common.blockentity.ScreenBlockEntity
import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.util.OCLogger
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.level.redstone.Orientation

class ScreenBlock(properties: Properties) : Block(properties), EntityBlock {

    companion object {
        val FACING = HorizontalDirectionalBlock.FACING
        val CONNECT_LEFT: BooleanProperty = BooleanProperty.create("connect_left")
        val CONNECT_RIGHT: BooleanProperty = BooleanProperty.create("connect_right")
        val CONNECT_UP: BooleanProperty = BooleanProperty.create("connect_up")
        val CONNECT_DOWN: BooleanProperty = BooleanProperty.create("connect_down")

        fun getLeftDir(facing: Direction): Direction = when (facing) {
            Direction.NORTH -> Direction.EAST
            Direction.SOUTH -> Direction.WEST
            Direction.EAST -> Direction.SOUTH
            Direction.WEST -> Direction.NORTH
            else -> Direction.EAST
        }

        fun getRightDir(facing: Direction): Direction = getLeftDir(facing).opposite
    }

    init {
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(CONNECT_LEFT, false)
            .setValue(CONNECT_RIGHT, false)
            .setValue(CONNECT_UP, false)
            .setValue(CONNECT_DOWN, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, CONNECT_LEFT, CONNECT_RIGHT, CONNECT_UP, CONNECT_DOWN)
    }

    private fun canConnect(state: BlockState, neighborState: BlockState): Boolean {
        if (neighborState.block != state.block) return false
        return neighborState.getValue(FACING) == state.getValue(FACING)
    }

    private fun computeConnections(state: BlockState, level: Level, pos: BlockPos): BlockState {
        val facing = state.getValue(FACING)
        val left = canConnect(state, level.getBlockState(pos.relative(getLeftDir(facing))))
        val right = canConnect(state, level.getBlockState(pos.relative(getRightDir(facing))))
        val up = canConnect(state, level.getBlockState(pos.above()))
        val down = canConnect(state, level.getBlockState(pos.below()))
        return state
            .setValue(CONNECT_LEFT, left)
            .setValue(CONNECT_RIGHT, right)
            .setValue(CONNECT_UP, up)
            .setValue(CONNECT_DOWN, down)
    }

    /**
     * Check if any adjacent block is a keyboard (any of 6 directions)
     */
    private fun hasKeyboard(level: Level, pos: BlockPos): Boolean {
        for (dir in Direction.values()) {
            val neighborPos = pos.relative(dir)
            val neighborBlock = level.getBlockState(neighborPos).block
            if (neighborBlock is KeyboardBlock) {
                OCLogger.debug("Screen at $pos found keyboard at $neighborPos (direction: $dir)")
                return true
            }
        }
        return false
    }

    /**
     * Collect all connected screens (multi-block) and check if ANY has keyboard adjacent
     */
    private fun multiBlockHasKeyboard(level: Level, pos: BlockPos, state: BlockState): Boolean {
        val visited = mutableSetOf<BlockPos>()
        val toCheck = ArrayDeque<BlockPos>()
        toCheck.add(pos)
        val facing = state.getValue(FACING)

        while (toCheck.isNotEmpty()) {
            val current = toCheck.removeFirst()
            if (current in visited) continue
            visited.add(current)

            // Check this screen for keyboard
            if (hasKeyboard(level, current)) {
                return true
            }

            // Add adjacent screens with same facing
            for (dir in listOf(getLeftDir(facing), getRightDir(facing), Direction.UP, Direction.DOWN)) {
                val neighbor = current.relative(dir)
                val neighborState = level.getBlockState(neighbor)
                if (neighborState.block is ScreenBlock && 
                    neighborState.getValue(FACING) == facing &&
                    neighbor !in visited) {
                    toCheck.add(neighbor)
                }
            }
        }
        return false
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        // facing = direction the display faces toward the player
        // player looks north → screen faces south (toward player) → use .opposite
        val facing = context.horizontalDirection.opposite
        val base = defaultBlockState().setValue(FACING, facing)
        return computeConnections(base, context.level, context.clickedPos)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, block: Block, orientation: Orientation?, moved: Boolean) {
        // neighborChanged handles redstone and some neighbor updates
        val newState = computeConnections(state, level, pos)
        if (newState != state) {
            level.setBlock(pos, newState, 3)
        }
    }

    override fun useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult {
        if (!level.isClientSide) {
            val screenBE = level.getBlockEntity(pos) as? ScreenBlockEntity
            val hasKb = multiBlockHasKeyboard(level, pos, state)
            val isConnected = screenBE?.connectedComputer != null
            
            OCLogger.debug("Screen interaction at $pos - hasKeyboard: $hasKb, connected: $isConnected")
            
            // Show warning messages but still allow opening screen
            if (!hasKb) {
                player.displayClientMessage(Component.translatable("message.opencomputers.screen.no_keyboard"), true)
            }
            if (!isConnected) {
                player.displayClientMessage(Component.translatable("message.opencomputers.screen.not_connected"), true)
            }
            return InteractionResult.CONSUME
        } else {
            // Client side: always open the terminal GUI to view screen contents
            // Messages about keyboard/connection are shown server-side
            net.minecraft.client.Minecraft.getInstance().setScreen(
                li.cil.oc.client.gui.TerminalScreen(pos)
            )
            return InteractionResult.SUCCESS
        }
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return ScreenBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        // Only tick on server side
        if (level.isClientSide) return null
        
        return if (type == ModBlockEntities.SCREEN.get()) {
            @Suppress("UNCHECKED_CAST")
            BlockEntityTicker<T> { lvl: Level, pos: BlockPos, s: BlockState, be: T -> 
                (be as ScreenBlockEntity).serverTick(lvl, pos, s) 
            }
        } else null
    }
}
