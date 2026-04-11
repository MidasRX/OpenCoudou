package li.cil.oc.client.gui

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.PrinterMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class PrinterScreen(
    menu: PrinterMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<PrinterMenu>(menu, playerInventory, title) {

    companion object {
        private val BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/printer.png"
        )
    }

    init {
        imageWidth = 176
        imageHeight = 166
        inventoryLabelY = imageHeight - 94
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        guiGraphics.blit(RenderType::guiTextured, BACKGROUND, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256)

        // Chamelium tank
        if (menu.maxChamelium > 0) {
            val height = (menu.chamelium.toFloat() / menu.maxChamelium.toFloat() * 52).toInt()
            guiGraphics.fill(leftPos + 52, topPos + 17 + (52 - height), leftPos + 62, topPos + 69, 0xFF8888FF.toInt())
        }

        // Ink tank
        if (menu.maxInk > 0) {
            val height = (menu.ink.toFloat() / menu.maxInk.toFloat() * 52).toInt()
            guiGraphics.fill(leftPos + 66, topPos + 17 + (52 - height), leftPos + 76, topPos + 69, 0xFF222222.toInt())
        }

        // Progress arrow
        if (menu.progress > 0) {
            val progress = (menu.progress.toFloat() / 100f * 22).toInt()
            guiGraphics.fill(leftPos + 100, topPos + 35, leftPos + 100 + progress, topPos + 51, 0xFF00AA00.toInt())
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)

        // Tank tooltips
        if (mouseX >= leftPos + 52 && mouseX < leftPos + 62 && mouseY >= topPos + 17 && mouseY < topPos + 69) {
            guiGraphics.renderTooltip(font, Component.translatable("gui.opencomputers.printer.chamelium", menu.chamelium, menu.maxChamelium), mouseX, mouseY)
        }
        if (mouseX >= leftPos + 66 && mouseX < leftPos + 76 && mouseY >= topPos + 17 && mouseY < topPos + 69) {
            guiGraphics.renderTooltip(font, Component.translatable("gui.opencomputers.printer.ink", menu.ink, menu.maxInk), mouseX, mouseY)
        }
    }
}
