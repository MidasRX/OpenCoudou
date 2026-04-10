package li.cil.oc.client.gui

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.CaseMenu
import li.cil.oc.common.container.CaseSlotConfig
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class CaseScreen(
    menu: CaseMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<CaseMenu>(menu, playerInventory, title) {

    companion object {
        private val BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/computer.png"
        )
        private val SLOT_BG = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/slot.png"
        )
        private val BUTTON_POWER = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/button_power.png"
        )
        private val BUTTON_RUN = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/button_run.png"
        )

        // Power button position relative to GUI left/top
        private const val POWER_BTN_X = 7
        private const val POWER_BTN_Y = 33
        private const val POWER_BTN_W = 18
        private const val POWER_BTN_H = 18

        private val SLOT_ICONS = mapOf(
            CaseSlotConfig.CARD to ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/items/icons/card.png"),
            CaseSlotConfig.CPU to ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/items/icons/cpu.png"),
            CaseSlotConfig.MEMORY to ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/items/icons/memory.png"),
            CaseSlotConfig.HDD to ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/items/icons/hdd.png"),
            CaseSlotConfig.EEPROM to ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/items/icons/eeprom.png"),
            CaseSlotConfig.FLOPPY to ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/items/icons/floppy.png"),
        )

        private val TIER_ICONS = mapOf(
            1 to ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/items/icons/tier0.png"),
            2 to ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/items/icons/tier1.png"),
            3 to ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/items/icons/tier2.png"),
        )
    }

    init {
        imageWidth = 176
        imageHeight = 166
        inventoryLabelY = imageHeight - 94
    }

    private var isPowered = false

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        guiGraphics.blit(RenderType::guiTextured, BACKGROUND, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256)

        // Power button — texture is 36x36: top 18x18 = normal, bottom 18x18 = hovered
        val btnTex = if (isPowered) BUTTON_RUN else BUTTON_POWER
        val hovered = mouseX >= leftPos + POWER_BTN_X && mouseX < leftPos + POWER_BTN_X + POWER_BTN_W
                   && mouseY >= topPos + POWER_BTN_Y && mouseY < topPos + POWER_BTN_Y + POWER_BTN_H
        val vOffset = if (hovered) 18f else 0f
        guiGraphics.blit(RenderType::guiTextured, btnTex,
            leftPos + POWER_BTN_X, topPos + POWER_BTN_Y,
            0f, vOffset, POWER_BTN_W, POWER_BTN_H, 36, 36)

        // Draw slot backgrounds and ghost icons for component slots
        for ((i, def) in menu.slotDefs.withIndex()) {
            val sx = leftPos + def.x - 1
            val sy = topPos + def.y - 1
            // Slot background (18x18)
            guiGraphics.blit(RenderType::guiTextured, SLOT_BG, sx, sy, 0f, 0f, 18, 18, 18, 18)

            // Ghost icon when slot is empty
            val slot = menu.slots[i]
            if (!slot.hasItem()) {
                val icon = SLOT_ICONS[def.type]
                if (icon != null) {
                    guiGraphics.blit(RenderType::guiTextured, icon, sx + 1, sy + 1, 0f, 0f, 16, 16, 16, 16)
                }
                // Tier indicator overlay
                val tierIcon = TIER_ICONS[def.maxTier]
                if (tierIcon != null) {
                    guiGraphics.blit(RenderType::guiTextured, tierIcon, sx + 1, sy + 1, 0f, 0f, 16, 16, 16, 16)
                }
            }
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val bx = leftPos + POWER_BTN_X
        val by = topPos + POWER_BTN_Y
        if (button == 0 && mouseX >= bx && mouseX < bx + POWER_BTN_W && mouseY >= by && mouseY < by + POWER_BTN_H) {
            isPowered = !isPowered
            minecraft?.gameMode?.handleInventoryButtonClick(menu.containerId, 0)
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }
}
