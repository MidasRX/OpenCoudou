package li.cil.oc.client.gui

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.RaidMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class RaidScreen(
    menu: RaidMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<RaidMenu>(menu, playerInventory, title) {

    companion object {
        private val BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/raid.png"
        )
    }

    private lateinit var labelField: EditBox

    init {
        imageWidth = 176
        imageHeight = 166
        inventoryLabelY = imageHeight - 94
    }

    override fun init() {
        super.init()
        
        labelField = EditBox(font, leftPos + 28, topPos + 58, 120, 16, Component.literal(""))
        labelField.setMaxLength(24)
        labelField.value = menu.raidLabel
        labelField.setResponder { text ->
            menu.updateLabel(text)
        }
        addRenderableWidget(labelField)
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        guiGraphics.blit(RenderType::guiTextured, BACKGROUND, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256)
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
        
        guiGraphics.drawString(font, Component.translatable("gui.opencomputers.raid.label"), leftPos + 28, topPos + 46, 0x404040, false)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (labelField.isFocused) {
            if (keyCode == 256) { // Escape
                labelField.isFocused = false
                return true
            }
            return labelField.keyPressed(keyCode, scanCode, modifiers)
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
}
