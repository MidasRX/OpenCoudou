package li.cil.oc.client.gui

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.TabletMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

/**
 * Tablet item GUI screen - shows tablet upgrade slots.
 * Used when sneaking + right-clicking a tablet item in hand.
 */
class TabletScreen(
    menu: TabletMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<TabletMenu>(menu, playerInventory, title) {

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/tablet.png")
        
        private val TIER_NAMES = arrayOf("Tablet (Tier 1)", "Tablet (Tier 2)", "Tablet (Creative)")
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
        
        // Draw tablet icon/preview area
        guiGraphics.fill(leftPos + 20, topPos + 20, leftPos + 70, topPos + 70, 0xFF222222.toInt())
        
        // Draw tier indicator bar
        val tierColor = when (menu.tier) {
            1 -> 0xFFC0C0C0.toInt() // Silver
            2 -> 0xFFFFD700.toInt() // Gold
            else -> 0xFF00FF00.toInt() // Creative green
        }
        guiGraphics.fill(leftPos + 78, topPos + 6, leftPos + 98, topPos + 10, tierColor)
        
        // Draw upgrade slots area
        val slotsWidth = if (menu.tier > 2) 54 else 18
        val slotsHeight = when (menu.tier) {
            1 -> 36
            2 -> 54
            else -> 54
        }
        guiGraphics.fill(
            leftPos + 78, topPos + 15,
            leftPos + 78 + slotsWidth, topPos + 15 + slotsHeight,
            0xFF1A1A1A.toInt()
        )
    }
    
    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val tierName = TIER_NAMES.getOrElse(menu.tier - 1) { "Tablet" }
        guiGraphics.drawString(font, tierName, titleLabelX, titleLabelY, 0x404040, false)
        
        // Upgrade label
        guiGraphics.drawString(font, "Upgrades", 78, 72, 0x606060, false)
        
        // Player inventory label
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false)
    }
    
    override fun renderTooltip(guiGraphics: GuiGraphics, x: Int, y: Int) {
        super.renderTooltip(guiGraphics, x, y)
        
        // Tooltip for empty upgrade slots
        val slotCount = when (menu.tier) {
            1 -> 2
            2 -> 3
            else -> 9
        }
        val cols = if (slotCount <= 3) 1 else 3
        
        for (i in 0 until slotCount) {
            val slotX = leftPos + 80 + (i % cols) * 18
            val slotY = topPos + 17 + (i / cols) * 18
            
            if (x >= slotX && x < slotX + 16 && y >= slotY && y < slotY + 16) {
                val slot = menu.slots.getOrNull(i)
                if (slot != null && !slot.hasItem()) {
                    guiGraphics.renderTooltip(font, Component.literal("Upgrade Slot"), x, y)
                }
            }
        }
    }
}
