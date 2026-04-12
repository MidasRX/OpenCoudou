package li.cil.oc.client.gui

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.RackMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

/**
 * Server Rack GUI screen.
 * Shows 4 mountable slots and network connection controls.
 */
class RackScreen(
    menu: RackMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<RackMenu>(menu, playerInventory, title) {
    
    companion object {
        private val BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/rack.png"
        )
        
        // Side names for connection tooltips
        private val SIDE_NAMES = arrayOf("Top", "Bottom", "Right", "Left", "Back")
    }
    
    private var relayButton: Button? = null
    
    init {
        imageWidth = 176
        imageHeight = 210
        inventoryLabelY = imageHeight - 94
    }
    
    override fun init() {
        super.init()
        
        // Relay toggle button
        relayButton = Button.builder(
            Component.literal(if (menu.isRelayEnabled) "Relay: ON" else "Relay: OFF")
        ) { button ->
            menu.isRelayEnabled = !menu.isRelayEnabled
            button.message = Component.literal(if (menu.isRelayEnabled) "Relay: ON" else "Relay: OFF")
            // TODO: Send packet to server
        }.bounds(leftPos + 100, topPos + 103, 60, 14).build()
        
        addRenderableWidget(relayButton!!)
    }
    
    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        guiGraphics.blit(
            RenderType::guiTextured,
            BACKGROUND,
            leftPos, topPos,
            0f, 0f,
            imageWidth, imageHeight,
            256, 256
        )
        
        // Draw slot indicators showing which slots have items
        for (i in 0..3) {
            val slot = menu.slots[i]
            if (slot.hasItem()) {
                // Draw "active" indicator
                guiGraphics.fill(
                    leftPos + 38, topPos + 23 + i * 20,
                    leftPos + 43, topPos + 28 + i * 20,
                    0xFF00FF00.toInt()
                )
            }
        }
        
        // Draw network presence indicators
        for (mountable in 0..3) {
            for (connectable in 0..3) {
                if (menu.nodePresence[mountable][connectable]) {
                    val x = leftPos + 45 + connectable * 11
                    val y = topPos + 22 + mountable * 20
                    guiGraphics.fill(x, y, x + 3, y + 5, 0xFF66FF66.toInt())
                }
            }
        }
    }
    
    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }
    
    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false)
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false)
        
        // Draw slot labels
        for (i in 0..3) {
            guiGraphics.drawString(font, "Slot ${i + 1}", 44, 25 + i * 20, 0x606060, false)
        }
    }
}
