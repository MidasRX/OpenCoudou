package li.cil.oc.client.gui

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.ServerMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

/**
 * Server item GUI screen - shows server component slots.
 * Used when right-clicking a server item in hand.
 */
class ServerScreen(
    menu: ServerMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<ServerMenu>(menu, playerInventory, title) {

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/server.png")
        
        private val TIER_NAMES = arrayOf("Server (Tier 1)", "Server (Tier 2)", "Server (Tier 3)", "Server (Creative)")
    }
    
    init {
        imageWidth = 176
        imageHeight = 166
    }
    
    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }
    
    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        // Draw main background
        guiGraphics.blit(RenderType::guiTextured, TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256)
        
        // Draw component slots area
        val slotAreaX = leftPos + 60
        val slotAreaY = topPos + 15
        val slotAreaWidth = 56
        val slotAreaHeight = when (menu.tier) {
            1 -> 36  // 4 slots
            2 -> 54  // 6 slots
            else -> 72 // 8 slots
        }
        
        // Background for slot area
        guiGraphics.fill(
            slotAreaX, slotAreaY,
            slotAreaX + slotAreaWidth, slotAreaY + slotAreaHeight,
            0xFF1A1A1A.toInt()
        )
        
        // Draw tier indicator
        val tierColor = when (menu.tier) {
            1 -> 0xFFC0C0C0.toInt() // Silver
            2 -> 0xFFFFD700.toInt() // Gold
            3 -> 0xFFE5E4E2.toInt() // Platinum
            else -> 0xFF00FF00.toInt() // Creative green
        }
        guiGraphics.fill(leftPos + 60, topPos + 6, leftPos + 116, topPos + 12, tierColor)
    }
    
    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // Tier-specific title
        val tierName = TIER_NAMES.getOrElse(menu.tier - 1) { "Server" }
        guiGraphics.drawString(font, tierName, titleLabelX, titleLabelY, 0x404040, false)
        
        // Slot labels
        guiGraphics.drawString(font, "Components", 62, 76, 0x606060, false)
        
        // Player inventory label
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false)
    }
    
    override fun renderTooltip(guiGraphics: GuiGraphics, x: Int, y: Int) {
        super.renderTooltip(guiGraphics, x, y)
        
        // Slot type tooltips
        val slotInfo = when (menu.tier) {
            1 -> listOf("CPU", "RAM", "RAM", "HDD")
            2 -> listOf("CPU", "RAM", "RAM", "RAM", "RAM", "HDD")
            else -> listOf("CPU", "RAM", "RAM", "RAM", "RAM", "HDD", "HDD", "Component Bus")
        }
        
        for ((i, info) in slotInfo.withIndex()) {
            val slotX = leftPos + 62 + (i % 2) * 36
            val slotY = topPos + 17 + (i / 2) * 18
            
            if (x >= slotX && x < slotX + 16 && y >= slotY && y < slotY + 16) {
                val slot = menu.slots.getOrNull(i)
                if (slot != null && !slot.hasItem()) {
                    guiGraphics.renderTooltip(font, Component.literal(info), x, y)
                }
            }
        }
    }
}
