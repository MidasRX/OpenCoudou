package li.cil.oc.client.gui

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.DisassemblerMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class DisassemblerScreen(
    menu: DisassemblerMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<DisassemblerMenu>(menu, playerInventory, title) {

    companion object {
        private val BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/disassembler.png"
        )
    }

    init {
        imageWidth = 176
        imageHeight = 166
        inventoryLabelY = imageHeight - 94
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        guiGraphics.blit(RenderType::guiTextured, BACKGROUND, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256)

        // Progress arrow
        if (menu.total > 0) {
            val progress = (menu.progress.toFloat() / menu.total.toFloat() * 22).toInt()
            guiGraphics.fill(leftPos + 52, topPos + 35, leftPos + 52 + progress, topPos + 51, 0xFF00AA00.toInt())
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }
}
