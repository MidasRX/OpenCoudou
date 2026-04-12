package li.cil.oc.client.gui

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.RobotMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

/**
 * Robot GUI screen - shows robot inventory, power status, and controls.
 */
class RobotScreen(
    menu: RobotMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<RobotMenu>(menu, playerInventory, title) {

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/robot.png")
        private val POWER_ICON = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/icons/power.png")
    }
    
    private var powerButton: Button? = null
    
    init {
        // Adjust image size based on tier
        imageWidth = 176
        imageHeight = when (menu.tier) {
            1 -> 166 // 4x4 inventory
            2 -> 184 // 4x8 inventory
            3 -> 238 // 8x8 inventory
            else -> 256 // 10x10 inventory
        }
    }
    
    override fun init() {
        super.init()
        
        // Power button
        powerButton = Button.builder(Component.empty()) { togglePower() }
            .bounds(leftPos + 152, topPos + 6, 18, 18)
            .build()
        addRenderableWidget(powerButton!!)
    }
    
    private fun togglePower() {
        minecraft?.gameMode?.handleInventoryButtonClick(menu.containerId, 0)
    }
    
    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }
    
    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        // Draw main background
        guiGraphics.blit(RenderType::guiTextured, TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256)
        
        // Draw energy bar
        val energy = menu.energy
        val maxEnergy = menu.maxEnergy.coerceAtLeast(1)
        val energyPercent = (energy.toFloat() / maxEnergy).coerceIn(0f, 1f)
        
        val barX = leftPos + 152
        val barY = topPos + 26
        val barWidth = 16
        val barHeight = 52
        val filledHeight = (barHeight * energyPercent).toInt()
        
        // Energy bar background (dark)
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333.toInt())
        
        // Energy bar fill (green gradient)
        val color = when {
            energyPercent > 0.5f -> 0xFF44DD44.toInt()
            energyPercent > 0.25f -> 0xFFDDDD44.toInt()
            else -> 0xFFDD4444.toInt()
        }
        guiGraphics.fill(
            barX, barY + barHeight - filledHeight,
            barX + barWidth, barY + barHeight,
            color
        )
        
        // Draw power button indicator
        val buttonColor = if (menu.running) 0xFF44FF44.toInt() else 0xFFFF4444.toInt()
        guiGraphics.fill(leftPos + 154, topPos + 8, leftPos + 168, topPos + 22, buttonColor)
        
        // Draw selected slot highlight
        val selectedSlot = menu.selectedSlot
        val cols = when (menu.tier) {
            1 -> 4
            2 -> 4
            3 -> 8
            else -> 10
        }
        val slotX = leftPos + 8 + (selectedSlot % cols) * 18 - 1
        val slotY = topPos + 28 + (selectedSlot / cols) * 18 - 1
        guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0x8000FF00.toInt())
    }
    
    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false)
        
        // Energy display
        val energyText = "${menu.energy}/${menu.maxEnergy}"
        guiGraphics.drawString(font, energyText, 100, 8, 0x404040, false)
        
        // Running status
        val statusText = if (menu.running) "Running" else "Stopped"
        val statusColor = if (menu.running) 0x44DD44 else 0xDD4444
        guiGraphics.drawString(font, statusText, 100, 18, statusColor, false)
    }
    
    override fun renderTooltip(guiGraphics: GuiGraphics, x: Int, y: Int) {
        super.renderTooltip(guiGraphics, x, y)
        
        // Energy bar tooltip
        val barX = leftPos + 152
        val barY = topPos + 26
        if (x >= barX && x < barX + 16 && y >= barY && y < barY + 52) {
            val tooltip = listOf(
                Component.literal("Energy: ${menu.energy}/${menu.maxEnergy}"),
                Component.literal("${(menu.energy * 100 / menu.maxEnergy.coerceAtLeast(1))}%")
            )
            guiGraphics.renderComponentTooltip(font, tooltip, x, y)
        }
        
        // Power button tooltip
        if (x >= leftPos + 152 && x < leftPos + 170 && y >= topPos + 6 && y < topPos + 24) {
            val tooltip = if (menu.running) {
                Component.translatable("oc.gui.robot.power_off")
            } else {
                Component.translatable("oc.gui.robot.power_on")
            }
            guiGraphics.renderTooltip(font, tooltip, x, y)
        }
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // Number keys select inventory slot
        if (keyCode in 49..57) { // 1-9
            val slot = keyCode - 49
            if (slot < menu.tier * 16) {
                minecraft?.gameMode?.handleInventoryButtonClick(menu.containerId, slot + 1)
                return true
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
}
