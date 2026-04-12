package li.cil.oc.client.gui

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.DroneMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

/**
 * Drone GUI screen - shows drone inventory, status text, and power controls.
 * Drones are simpler than robots with only 8 inventory slots.
 */
class DroneScreen(
    menu: DroneMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<DroneMenu>(menu, playerInventory, title) {

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/drone.png")
    }
    
    private var powerButton: Button? = null
    
    init {
        imageWidth = 176
        imageHeight = 166
    }
    
    override fun init() {
        super.init()
        
        // Power button
        powerButton = Button.builder(Component.empty()) { togglePower() }
            .bounds(leftPos + 8, topPos + 26, 18, 18)
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
        
        // Draw status text area background
        guiGraphics.fill(leftPos + 28, topPos + 8, leftPos + 148, topPos + 24, 0xFF222222.toInt())
        
        // Draw energy bar
        val energy = menu.energy
        val maxEnergy = menu.maxEnergy.coerceAtLeast(1)
        val energyPercent = (energy.toFloat() / maxEnergy).coerceIn(0f, 1f)
        
        val barX = leftPos + 152
        val barY = topPos + 8
        val barWidth = 16
        val barHeight = 54
        val filledHeight = (barHeight * energyPercent).toInt()
        
        // Energy bar background
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF333333.toInt())
        
        // Energy bar fill
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
        guiGraphics.fill(leftPos + 10, topPos + 28, leftPos + 24, topPos + 42, buttonColor)
    }
    
    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false)
        
        // Status indicator
        val statusText = if (menu.running) "Online" else "Offline"
        val statusColor = if (menu.running) 0x44FF44 else 0xFF4444
        guiGraphics.drawString(font, statusText, 30, 11, statusColor, false)
        
        // Energy display on energy bar
        val energyText = "${(menu.energy * 100 / menu.maxEnergy.coerceAtLeast(1))}%"
        guiGraphics.drawString(font, energyText, 152, 64, 0x404040, false)
    }
    
    override fun renderTooltip(guiGraphics: GuiGraphics, x: Int, y: Int) {
        super.renderTooltip(guiGraphics, x, y)
        
        // Energy bar tooltip
        val barX = leftPos + 152
        val barY = topPos + 8
        if (x >= barX && x < barX + 16 && y >= barY && y < barY + 54) {
            val tooltip = listOf(
                Component.literal("Energy: ${menu.energy}/${menu.maxEnergy}"),
                Component.literal("${(menu.energy * 100 / menu.maxEnergy.coerceAtLeast(1))}%")
            )
            guiGraphics.renderComponentTooltip(font, tooltip, x, y)
        }
        
        // Power button tooltip
        if (x >= leftPos + 8 && x < leftPos + 26 && y >= topPos + 26 && y < topPos + 44) {
            val tooltip = if (menu.running) {
                Component.translatable("oc.gui.drone.power_off")
            } else {
                Component.translatable("oc.gui.drone.power_on")
            }
            guiGraphics.renderTooltip(font, tooltip, x, y)
        }
    }
}
