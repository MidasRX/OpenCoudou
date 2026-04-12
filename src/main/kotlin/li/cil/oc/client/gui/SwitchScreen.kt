package li.cil.oc.client.gui

import li.cil.oc.OpenComputers
import li.cil.oc.common.container.SwitchMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

/**
 * Network Switch (Net Splitter) GUI screen.
 * Shows side connection toggles and network statistics.
 */
class SwitchScreen(
    menu: SwitchMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<SwitchMenu>(menu, playerInventory, title) {

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/gui/switch.png")
        
        private val SIDE_NAMES = arrayOf("Down", "Up", "North", "South", "West", "East")
        private val SIDE_COLORS_OPEN = arrayOf(
            0xFF44DD44.toInt(), // Down - green
            0xFF44DD44.toInt(), // Up - green
            0xFF44DD44.toInt(), // North - green
            0xFF44DD44.toInt(), // South - green
            0xFF44DD44.toInt(), // West - green
            0xFF44DD44.toInt()  // East - green
        )
    }
    
    private val sideButtons = mutableListOf<Button>()
    
    init {
        imageWidth = 176
        imageHeight = 166
    }
    
    override fun init() {
        super.init()
        
        // Create side toggle buttons
        val buttonStartX = leftPos + 50
        val buttonStartY = topPos + 20
        val buttonSize = 20
        val spacing = 22
        
        // Layout: cube-like view
        //        UP
        // WEST NORTH EAST SOUTH
        //       DOWN
        
        val buttonPositions = arrayOf(
            Pair(buttonStartX + spacing, buttonStartY + spacing * 2), // DOWN
            Pair(buttonStartX + spacing, buttonStartY), // UP
            Pair(buttonStartX + spacing, buttonStartY + spacing), // NORTH (center)
            Pair(buttonStartX + spacing * 3, buttonStartY + spacing), // SOUTH
            Pair(buttonStartX, buttonStartY + spacing), // WEST
            Pair(buttonStartX + spacing * 2, buttonStartY + spacing) // EAST
        )
        
        for (i in 0..5) {
            val (x, y) = buttonPositions[i]
            val button = Button.builder(Component.literal(SIDE_NAMES[i].first().toString())) { 
                toggleSide(i) 
            }
                .bounds(x, y, buttonSize, buttonSize)
                .build()
            sideButtons.add(button)
            addRenderableWidget(button)
        }
    }
    
    private fun toggleSide(sideOrdinal: Int) {
        minecraft?.gameMode?.handleInventoryButtonClick(menu.containerId, sideOrdinal)
    }
    
    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }
    
    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        // Draw main background
        guiGraphics.blit(RenderType::guiTextured, TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256)
        
        // Draw side status indicators on each button
        for (i in 0..5) {
            val button = sideButtons.getOrNull(i) ?: continue
            val isOpen = menu.isSideOpen(Direction.from3DDataValue(i))
            val color = if (isOpen) 0xFF44FF44.toInt() else 0xFFFF4444.toInt()
            
            // Small indicator square on button
            guiGraphics.fill(
                button.x + 2, button.y + 2,
                button.x + 6, button.y + 6,
                color
            )
        }
        
        // Draw statistics panel
        val statsX = leftPos + 8
        val statsY = topPos + 75
        guiGraphics.fill(statsX, statsY, statsX + 160, statsY + 6, 0xFF333333.toInt())
    }
    
    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false)
        
        // Statistics
        val statsY = 65
        guiGraphics.drawString(font, "Network Statistics:", 8, statsY, 0x404040, false)
        guiGraphics.drawString(font, "Packets: ${menu.packetsRelayed}", 8, statsY + 10, 0x606060, false)
        guiGraphics.drawString(font, "Bytes: ${menu.bytesRelayed}", 8, statsY + 20, 0x606060, false)
        guiGraphics.drawString(font, "Queue: ${menu.queueSize}", 8, statsY + 30, 0x606060, false)
        
        // Connection count
        val openCount = (0..5).count { menu.isSideOpen(Direction.from3DDataValue(it)) }
        guiGraphics.drawString(font, "Connections: $openCount/6", 90, statsY + 10, 0x606060, false)
        
        // Player inventory label
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false)
    }
    
    override fun renderTooltip(guiGraphics: GuiGraphics, x: Int, y: Int) {
        super.renderTooltip(guiGraphics, x, y)
        
        // Side button tooltips
        for ((i, button) in sideButtons.withIndex()) {
            if (x >= button.x && x < button.x + button.width && 
                y >= button.y && y < button.y + button.height) {
                val sideName = SIDE_NAMES[i]
                val isOpen = menu.isSideOpen(Direction.from3DDataValue(i))
                val status = if (isOpen) "Connected" else "Disconnected"
                val tooltip = listOf(
                    Component.literal(sideName),
                    Component.literal(status).withStyle { 
                        it.withColor(if (isOpen) 0x44FF44 else 0xFF4444) 
                    },
                    Component.literal("Click to toggle").withStyle { it.withColor(0x888888) }
                )
                guiGraphics.renderComponentTooltip(font, tooltip, x, y)
                break
            }
        }
    }
}
