package li.cil.oc.client.gui

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.DatabaseMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

/**
 * Database Upgrade GUI screen.
 * Shows a grid of slots for storing item database entries.
 * Tier 1: 3x3, Tier 2: 5x5, Tier 3: 9x9
 */
class DatabaseScreen(
    menu: DatabaseMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<DatabaseMenu>(menu, playerInventory, title) {
    
    companion object {
        // Base background texture
        private val BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/database.png"
        )
        
        // Overlay for additional tiers
        private val BACKGROUND_T2 = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/database1.png"
        )
        private val BACKGROUND_T3 = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/database2.png"
        )
    }
    
    init {
        imageWidth = 176
        imageHeight = 256
        inventoryLabelY = 162
    }
    
    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        // Base background
        guiGraphics.blit(
            RenderType::guiTextured,
            BACKGROUND,
            leftPos, topPos,
            0f, 0f,
            imageWidth, imageHeight,
            256, 256
        )
        
        // Overlay for tier 2+
        if (menu.tier > 0) {
            guiGraphics.blit(
                RenderType::guiTextured,
                BACKGROUND_T2,
                leftPos, topPos,
                0f, 0f,
                imageWidth, imageHeight,
                256, 256
            )
        }
        
        // Overlay for tier 3
        if (menu.tier > 1) {
            guiGraphics.blit(
                RenderType::guiTextured,
                BACKGROUND_T3,
                leftPos, topPos,
                0f, 0f,
                imageWidth, imageHeight,
                256, 256
            )
        }
    }
    
    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }
    
    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false)
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false)
    }
}
