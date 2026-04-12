package li.cil.oc.client.gui

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.RelayMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import java.text.DecimalFormat

/**
 * Relay GUI screen.
 * Shows upgrade slots in a tab and packet statistics.
 */
class RelayScreen(
    menu: RelayMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<RelayMenu>(menu, playerInventory, title) {
    
    companion object {
        private val BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/relay.png"
        )
        private val TAB = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/upgrade_tab.png"
        )
        
        private const val TAB_X = 176
        private const val TAB_Y = 10
        private const val TAB_WIDTH = 23
        private const val TAB_HEIGHT = 26
    }
    
    private val format = DecimalFormat("#.##")
    
    init {
        imageWidth = 176
        imageHeight = 166
        inventoryLabelY = imageHeight - 94
    }
    
    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        // Main background
        guiGraphics.blit(
            RenderType::guiTextured,
            BACKGROUND,
            leftPos, topPos,
            0f, 0f,
            imageWidth, imageHeight,
            256, 256
        )
        
        // Tab background for upgrade slots
        guiGraphics.blit(
            RenderType::guiTextured,
            TAB,
            leftPos + TAB_X, topPos + TAB_Y,
            0f, 0f,
            TAB_WIDTH, TAB_HEIGHT,
            32, 32
        )
    }
    
    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }
    
    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false)
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false)
        
        // Draw relay statistics
        val y = 20
        val x = 8
        val color = 0x606060
        
        guiGraphics.drawString(font, "Queue: ${menu.queueSize}/${menu.maxQueueSize}", x, y, color, false)
        
        val rate = if (menu.relayDelay > 0) {
            format.format(20.0 / menu.relayDelay * menu.relayAmount) + "hz"
        } else {
            "unlimited"
        }
        guiGraphics.drawString(font, "Rate: $rate", x, y + 10, color, false)
        
        guiGraphics.drawString(font, "Avg: ${menu.packetsPerCycleAvg} pkt/cycle", x, y + 20, color, false)
    }
}
