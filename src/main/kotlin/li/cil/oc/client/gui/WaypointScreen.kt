package li.cil.oc.client.gui

import li.cil.oc.OpenComputers
import li.cil.oc.common.blockentity.WaypointBlockEntity
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/**
 * Waypoint label editor screen.
 * Simple text input for setting the waypoint label.
 */
class WaypointScreen(
    private val waypoint: WaypointBlockEntity
) : Screen(Component.translatable("gui.opencomputers.waypoint")) {
    
    companion object {
        private val BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/waypoint.png"
        )
        
        private const val GUI_WIDTH = 176
        private const val GUI_HEIGHT = 24
    }
    
    private var guiLeft = 0
    private var guiTop = 0
    private lateinit var textField: EditBox
    
    override fun init() {
        super.init()
        
        guiLeft = (width - GUI_WIDTH) / 2
        guiTop = (height - GUI_HEIGHT) / 2
        
        textField = EditBox(
            font,
            guiLeft + 7,
            guiTop + 6,
            162,
            12,
            Component.literal(waypoint.label)
        )
        textField.setMaxLength(32)
        textField.setBordered(false)
        textField.setTextColor(0xFFFFFF)
        textField.value = waypoint.label
        textField.setCanLoseFocus(false)
        textField.isFocused = true
        
        addRenderableWidget(textField)
    }
    
    override fun tick() {
        super.tick()
        
        // Close if player moved too far
        val player = minecraft?.player ?: return
        val distSq = player.distanceToSqr(
            waypoint.blockPos.x + 0.5,
            waypoint.blockPos.y + 0.5,
            waypoint.blockPos.z + 0.5
        )
        if (distSq > 64.0) {
            onClose()
        }
    }
    
    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // Draw background
        guiGraphics.blit(
            RenderType::guiTextured,
            BACKGROUND,
            guiLeft, guiTop,
            0f, 0f,
            GUI_WIDTH, GUI_HEIGHT,
            256, 256
        )
        
        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // Enter key - save and close
        if (keyCode == 257 || keyCode == 335) { // GLFW_KEY_ENTER or GLFW_KEY_KP_ENTER
            saveLabel()
            onClose()
            return true
        }
        
        // Escape - close without saving
        if (keyCode == 256) { // GLFW_KEY_ESCAPE
            onClose()
            return true
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
    
    private fun saveLabel() {
        val newLabel = textField.value.take(32)
        if (newLabel != waypoint.label) {
            waypoint.label = newLabel
            // In a real implementation, send packet to server
            waypoint.setChanged()
        }
    }
    
    override fun isPauseScreen(): Boolean = false
}
