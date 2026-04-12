package li.cil.oc.common.event

import li.cil.oc.OpenComputers
import li.cil.oc.common.blockentity.KeyboardBlockEntity
import li.cil.oc.common.blockentity.ScreenBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.InputEvent
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles keyboard input events for OpenComputers screens.
 */
@EventBusSubscriber(modid = OpenComputers.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = [Dist.CLIENT])
object InputEventHandler {
    
    private val logger = LoggerFactory.getLogger("OpenComputers")
    
    // Players currently interacting with screens
    private val activeScreens = ConcurrentHashMap<Player, ScreenInfo>()
    
    // Key repeat tracking
    private val heldKeys = ConcurrentHashMap<Int, Long>()
    private const val KEY_REPEAT_DELAY = 500L // ms before repeat starts
    private const val KEY_REPEAT_RATE = 50L   // ms between repeats
    
    data class ScreenInfo(
        val level: Level,
        val screenPos: BlockPos,
        val keyboardPos: BlockPos?,
        var lastInputTime: Long = 0
    )
    
    @SubscribeEvent
    @JvmStatic
    fun onKeyInput(event: InputEvent.Key) {
        val minecraft = net.minecraft.client.Minecraft.getInstance()
        val player = minecraft.player ?: return
        
        // Check if player is looking at a screen
        val screenInfo = activeScreens[player] ?: return
        
        val key = event.key
        val scancode = event.scanCode
        val action = event.action
        val mods = event.modifiers
        
        when (action) {
            org.lwjgl.glfw.GLFW.GLFW_PRESS -> {
                handleKeyPress(player, screenInfo, key, scancode, mods)
                heldKeys[key] = System.currentTimeMillis()
            }
            org.lwjgl.glfw.GLFW.GLFW_RELEASE -> {
                handleKeyRelease(player, screenInfo, key, scancode, mods)
                heldKeys.remove(key)
            }
            org.lwjgl.glfw.GLFW.GLFW_REPEAT -> {
                handleKeyPress(player, screenInfo, key, scancode, mods)
            }
        }
    }
    
    // Character input is handled through GLFW callbacks in the screen GUI
    // This event doesn't exist in NeoForge - character input goes through Screen.charTyped()
    
    @SubscribeEvent
    @JvmStatic
    fun onMouseScroll(event: InputEvent.MouseScrollingEvent) {
        val minecraft = net.minecraft.client.Minecraft.getInstance()
        val player = minecraft.player ?: return
        
        val screenInfo = activeScreens[player] ?: return
        
        // Send scroll event
        val delta = event.scrollDeltaY
        if (delta != 0.0) {
            sendScrollToScreen(player, screenInfo, delta)
        }
    }
    
    @SubscribeEvent
    @JvmStatic
    fun onMouseClick(event: InputEvent.MouseButton.Pre) {
        val minecraft = net.minecraft.client.Minecraft.getInstance()
        val player = minecraft.player ?: return
        
        val screenInfo = activeScreens[player] ?: return
        
        val button = event.button
        val action = event.action
        
        when (action) {
            org.lwjgl.glfw.GLFW.GLFW_PRESS -> {
                sendMouseDownToScreen(player, screenInfo, button)
            }
            org.lwjgl.glfw.GLFW.GLFW_RELEASE -> {
                sendMouseUpToScreen(player, screenInfo, button)
            }
        }
    }
    
    private fun handleKeyPress(player: Player, info: ScreenInfo, key: Int, scancode: Int, mods: Int) {
        // Convert to character
        val char = translateKeyToChar(key, mods)
        
        // Send key down network packet
        sendKeyDownToScreen(player, info, key, char, mods)
    }
    
    private fun handleKeyRelease(player: Player, info: ScreenInfo, key: Int, scancode: Int, mods: Int) {
        val char = translateKeyToChar(key, mods)
        sendKeyUpToScreen(player, info, key, char, mods)
    }
    
    private fun translateKeyToChar(key: Int, mods: Int): Char {
        val shift = (mods and org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT) != 0
        
        // Basic key to char translation
        return when (key) {
            in org.lwjgl.glfw.GLFW.GLFW_KEY_A..org.lwjgl.glfw.GLFW.GLFW_KEY_Z -> {
                val base = if (shift) 'A' else 'a'
                (base.code + (key - org.lwjgl.glfw.GLFW.GLFW_KEY_A)).toChar()
            }
            in org.lwjgl.glfw.GLFW.GLFW_KEY_0..org.lwjgl.glfw.GLFW.GLFW_KEY_9 -> {
                if (shift) {
                    ")!@#\$%^&*("[key - org.lwjgl.glfw.GLFW.GLFW_KEY_0]
                } else {
                    ('0'.code + (key - org.lwjgl.glfw.GLFW.GLFW_KEY_0)).toChar()
                }
            }
            org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE -> ' '
            org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER -> '\n'
            org.lwjgl.glfw.GLFW.GLFW_KEY_TAB -> '\t'
            org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE -> '\b'
            else -> '\u0000'
        }
    }
    
    private fun sendKeyDownToScreen(player: Player, info: ScreenInfo, key: Int, char: Char, mods: Int) {
        // TODO: Send network packet
        logger.debug("Key down: $key ($char) mods=$mods to ${info.screenPos}")
    }
    
    private fun sendKeyUpToScreen(player: Player, info: ScreenInfo, key: Int, char: Char, mods: Int) {
        // TODO: Send network packet
        logger.debug("Key up: $key ($char) mods=$mods to ${info.screenPos}")
    }
    
    private fun sendCharToScreen(player: Player, info: ScreenInfo, char: Char) {
        // TODO: Send network packet
        logger.debug("Char: $char to ${info.screenPos}")
    }
    
    private fun sendScrollToScreen(player: Player, info: ScreenInfo, delta: Double) {
        // TODO: Send network packet
        logger.debug("Scroll: $delta to ${info.screenPos}")
    }
    
    private fun sendMouseDownToScreen(player: Player, info: ScreenInfo, button: Int) {
        // TODO: Send network packet
        logger.debug("Mouse down: $button to ${info.screenPos}")
    }
    
    private fun sendMouseUpToScreen(player: Player, info: ScreenInfo, button: Int) {
        // TODO: Send network packet
        logger.debug("Mouse up: $button to ${info.screenPos}")
    }
    
    /**
     * Start screen interaction for a player.
     */
    fun startScreenInteraction(player: Player, level: Level, screenPos: BlockPos, keyboardPos: BlockPos?) {
        activeScreens[player] = ScreenInfo(level, screenPos, keyboardPos)
        logger.debug("Player ${player.name.string} started interaction with screen at $screenPos")
    }
    
    /**
     * Stop screen interaction for a player.
     */
    fun stopScreenInteraction(player: Player) {
        activeScreens.remove(player)
        heldKeys.clear()
        logger.debug("Player ${player.name.string} stopped screen interaction")
    }
    
    /**
     * Check if a player is interacting with a screen.
     */
    fun isInteracting(player: Player): Boolean {
        return activeScreens.containsKey(player)
    }
    
    /**
     * Get the screen a player is interacting with.
     */
    fun getActiveScreen(player: Player): ScreenInfo? {
        return activeScreens[player]
    }
}
