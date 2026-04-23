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

/**
 * Computer case GUI — uses the vanilla 3-row container background (generic_27.png)
 * which is exactly 176x166, matching our slot layout.
 *
 * generic_27.png layout (176x166, stored in a 256x256 atlas):
 *   - Top border:        y=0..16   (17px)
 *   - 3 container rows:  y=17..70  (3 × 18px = 54px)
 *   - Gap:               y=71..74  (4px)
 *   - Player inv label:  y=75..82  (8px)  ← we skip this, draw our own
 *   - Player inv rows:   y=83..136 (3 × 18px = 54px)
 *   - Gap:               y=137..140 (4px)
 *   - Hotbar:            y=141..158 (18px)
 *   - Bottom border:     y=159..165 (7px)
 */
class CaseScreen(
    menu: CaseMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<CaseMenu>(menu, playerInventory, title) {

    companion object {
        // 3-row container = exactly 176x166, perfect for our layout
        private val BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_27.png")
        private val BUTTON_POWER = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/button_power.png")
        private val BUTTON_RUN   = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/button_run.png")

        private const val POWER_BTN_X = 7
        private const val POWER_BTN_Y = 33
        private const val POWER_BTN_W = 18
        private const val POWER_BTN_H = 18

        private val SLOT_TYPE_NAMES = mapOf(
            CaseSlotConfig.CPU    to "CPU Slot",
            CaseSlotConfig.MEMORY to "Memory Slot",
            CaseSlotConfig.HDD    to "HDD Slot",
            CaseSlotConfig.CARD   to "Card Slot",
            CaseSlotConfig.EEPROM to "EEPROM Slot",
            CaseSlotConfig.FLOPPY to "Floppy Slot"
        )
    }

    init {
        imageWidth  = 176
        imageHeight = 166
        inventoryLabelY = imageHeight - 94
    }

    private var isPowered = false

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        // Draw the full 176x166 background in one blit
        guiGraphics.blit(
            RenderType::guiTextured, BACKGROUND,
            leftPos, topPos,
            0f, 0f,
            imageWidth, imageHeight,
            256, 256
        )

        // Power button
        val btnTex = if (isPowered) BUTTON_RUN else BUTTON_POWER
        val hovered = mouseX in (leftPos + POWER_BTN_X) until (leftPos + POWER_BTN_X + POWER_BTN_W) &&
                      mouseY in (topPos  + POWER_BTN_Y) until (topPos  + POWER_BTN_Y + POWER_BTN_H)
        val vOffset = if (hovered) 18f else 0f
        guiGraphics.blit(
            RenderType::guiTextured, btnTex,
            leftPos + POWER_BTN_X, topPos + POWER_BTN_Y,
            0f, vOffset,
            POWER_BTN_W, POWER_BTN_H,
            36, 36
        )
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }

    override fun renderTooltip(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // Show slot type tooltip for empty component slots
        val hoveredDef = menu.slotDefs.indices.firstOrNull { i ->
            val def = menu.slotDefs[i]
            !menu.slots[i].hasItem() &&
            mouseX in (leftPos + def.x) until (leftPos + def.x + 16) &&
            mouseY in (topPos  + def.y) until (topPos  + def.y + 16)
        }?.let { menu.slotDefs[it] }

        if (hoveredDef != null) {
            val name = SLOT_TYPE_NAMES[hoveredDef.type] ?: hoveredDef.type
            val tierStr = when (hoveredDef.maxTier) {
                1 -> " (Tier 1)"
                2 -> " (up to Tier 2)"
                3 -> " (up to Tier 3)"
                else -> ""
            }
            guiGraphics.renderTooltip(font, Component.literal(name + tierStr), mouseX, mouseY)
            return
        }

        super.renderTooltip(guiGraphics, mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val bx = leftPos + POWER_BTN_X
        val by = topPos  + POWER_BTN_Y
        if (button == 0 &&
            mouseX >= bx && mouseX < bx + POWER_BTN_W &&
            mouseY >= by && mouseY < by + POWER_BTN_H) {
            isPowered = !isPowered
            minecraft?.gameMode?.handleInventoryButtonClick(menu.containerId, 0)
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }
}
