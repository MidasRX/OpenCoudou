package li.cil.oc.client.gui

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.AssemblerMenu
import li.cil.oc.common.container.AssemblerState
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class AssemblerScreen(
    menu: AssemblerMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<AssemblerMenu>(menu, playerInventory, title) {

    companion object {
        private val BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/assembler.png"
        )
        private val PROGRESS_BAR = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/progress_bar.png"
        )
    }

    private lateinit var assembleButton: Button

    init {
        imageWidth = 176
        imageHeight = 200
        inventoryLabelY = imageHeight - 94
    }

    override fun init() {
        super.init()
        
        assembleButton = Button.builder(Component.translatable("gui.opencomputers.assembler.assemble")) { _ ->
            minecraft?.gameMode?.handleInventoryButtonClick(menu.containerId, 0)
        }
            .bounds(leftPos + 98, topPos + 70, 70, 20)
            .build()
        
        addRenderableWidget(assembleButton)
    }

    override fun containerTick() {
        super.containerTick()
        assembleButton.active = menu.state == AssemblerState.IDLE
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        guiGraphics.blit(RenderType::guiTextured, BACKGROUND, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256)

        // Progress bar
        if (menu.state == AssemblerState.ASSEMBLING && menu.requiredEnergy > 0) {
            val progress = (menu.progress.toFloat() / menu.requiredEnergy.toFloat() * 24).toInt()
            guiGraphics.blit(RenderType::guiTextured, PROGRESS_BAR,
                leftPos + 98, topPos + 52,
                0f, 0f, progress, 16, 24, 16)
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)

        // Status text
        val statusKey = when (menu.state) {
            AssemblerState.IDLE -> "gui.opencomputers.assembler.idle"
            AssemblerState.ASSEMBLING -> "gui.opencomputers.assembler.assembling"
            AssemblerState.WAITING -> "gui.opencomputers.assembler.waiting"
        }
        guiGraphics.drawString(font, Component.translatable(statusKey), leftPos + 98, topPos + 40, 0x404040, false)
    }
}
