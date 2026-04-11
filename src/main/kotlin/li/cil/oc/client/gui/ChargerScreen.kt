package li.cil.oc.client.gui

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.ChargerMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class ChargerScreen(
    menu: ChargerMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<ChargerMenu>(menu, playerInventory, title) {

    companion object {
        private val BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/charger.png"
        )
    }

    init {
        imageWidth = 176
        imageHeight = 184
        inventoryLabelY = imageHeight - 94
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        guiGraphics.blit(RenderType::guiTextured, BACKGROUND, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256)

        // Charge speed indicator
        val speedWidth = (menu.chargeSpeed * 40).toInt()
        guiGraphics.fill(leftPos + 8, topPos + 90, leftPos + 8 + speedWidth, topPos + 96, 0xFF00AA00.toInt())
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)

        // Speed label
        val speedPercent = (menu.chargeSpeed * 100).toInt()
        guiGraphics.drawString(font, Component.translatable("gui.opencomputers.charger.speed", speedPercent), 
            leftPos + 52, topPos + 89, 0x404040, false)
    }
}
