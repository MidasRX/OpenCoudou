package li.cil.oc.client.gui

import li.cil.oc.OpenComputers
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/**
 * In-game manual screen for OpenComputers documentation.
 * Displays information about blocks, items, Lua APIs, and more.
 */
class ManualScreen : Screen(Component.translatable("gui.opencomputers.manual.title")) {
    
    companion object {
        private val BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/manual.png"
        )
        
        // Manual size
        private const val MANUAL_WIDTH = 256
        private const val MANUAL_HEIGHT = 192
        
        // Content area (inside borders)
        private const val CONTENT_X = 16
        private const val CONTENT_Y = 16
        private const val CONTENT_WIDTH = 224
        private const val CONTENT_HEIGHT = 144
        
        // Navigation button positions
        private const val NAV_Y = 168
        private const val PREV_X = 16
        private const val NEXT_X = 220
        private const val HOME_X = 118
        
        // Table of contents
        private val CHAPTERS = listOf(
            "overview" to "Overview",
            "blocks" to "Blocks",
            "items" to "Items",
            "components" to "Components",
            "lua" to "Lua Programming",
            "api" to "Lua API Reference",
            "recipes" to "Recipes"
        )
        
        // Chapter content (simplified - real implementation would load from resources)
        private val CONTENT = mapOf(
            "overview" to listOf(
                "=== OpenComputers ===",
                "",
                "OpenComputers adds programmable",
                "computers and robots to Minecraft.",
                "",
                "Computers run Lua programs and can",
                "interact with the world through",
                "various components.",
                "",
                "Getting Started:",
                "1. Build a Computer Case",
                "2. Add a CPU, Memory, and EEPROM",
                "3. Add a GPU and Screen",
                "4. Install OpenOS from a floppy",
                "5. Start programming!",
                "",
                "Use the navigation buttons below",
                "to explore the documentation."
            ),
            "blocks" to listOf(
                "=== Blocks ===",
                "",
                "Computer Cases:",
                "  Tier 1-3 and Creative cases house",
                "  computer components.",
                "",
                "Screens:",
                "  Multi-block displays for output.",
                "  Tier affects resolution.",
                "",
                "Server Racks:",
                "  Hold up to 4 server modules.",
                "",
                "Chargers:",
                "  Power robots and tablets.",
                "",
                "Cables:",
                "  Connect OC components.",
                "",
                "And many more! Check Items..."
            ),
            "items" to listOf(
                "=== Items ===",
                "",
                "CPUs: Process Lua code",
                "  Higher tiers = more components",
                "",
                "Memory (RAM): Program storage",
                "  More RAM = larger programs",
                "",
                "GPUs: Graphics processing",
                "  Higher tiers = better resolution",
                "",
                "HDDs: Persistent storage",
                "  Store files and programs",
                "",
                "Cards: Add capabilities",
                "  - Internet Card: Web access",
                "  - Redstone Card: RS control",
                "  - Network Card: Networking",
                "",
                "EEPROM: Boot firmware"
            ),
            "components" to listOf(
                "=== Components ===",
                "",
                "Components are the hardware parts",
                "that Lua programs can access.",
                "",
                "In Lua, use component.list()",
                "to see available components.",
                "",
                "Common components:",
                "  - computer: System control",
                "  - gpu: Graphics operations",
                "  - screen: Display target",
                "  - filesystem: File storage",
                "  - modem: Network comms",
                "",
                "Each component has methods",
                "you can call from Lua."
            ),
            "lua" to listOf(
                "=== Lua Programming ===",
                "",
                "OpenComputers uses Lua 5.3.",
                "",
                "Basic example:",
                "  print('Hello, World!')",
                "",
                "GPU example:",
                "  local gpu = component.gpu",
                "  gpu.set(1, 1, 'Hi!')",
                "",
                "File operations:",
                "  local f = io.open('/test', 'w')",
                "  f:write('data')",
                "  f:close()",
                "",
                "Events:",
                "  local e = {event.pull()}",
                "  print(e[1]) -- event name"
            ),
            "api" to listOf(
                "=== Lua API Reference ===",
                "",
                "component.list([filter])",
                "  Lists available components",
                "",
                "computer.shutdown([reboot])",
                "  Shuts down or reboots",
                "",
                "gpu.set(x, y, text)",
                "  Draws text at position",
                "",
                "gpu.setBackground(color)",
                "  Sets background color",
                "",
                "gpu.setForeground(color)",
                "  Sets foreground color",
                "",
                "event.pull([timeout])",
                "  Waits for an event"
            ),
            "recipes" to listOf(
                "=== Recipes ===",
                "",
                "Crafting recipes can be found",
                "using JEI/REI or similar mods.",
                "",
                "Key materials:",
                "  - Iron & Gold ingots",
                "  - Redstone",
                "  - Ender pearls (advanced)",
                "  - Diamonds (high tier)",
                "",
                "Progression:",
                "  Raw Materials -> Circuit Boards",
                "  -> Components -> Computers",
                "",
                "Tip: Start with Tier 1 items",
                "and work your way up."
            )
        )
    }
    
    private var leftPos = 0
    private var topPos = 0
    private var currentChapter = "overview"
    private var scrollOffset = 0
    
    override fun init() {
        super.init()
        
        leftPos = (width - MANUAL_WIDTH) / 2
        topPos = (height - MANUAL_HEIGHT) / 2
        
        // Previous button
        addRenderableWidget(Button.builder(Component.literal("<")) { prevChapter() }
            .bounds(leftPos + PREV_X, topPos + NAV_Y, 20, 20)
            .build())
        
        // Home button
        addRenderableWidget(Button.builder(Component.translatable("gui.opencomputers.manual.home")) { 
            currentChapter = "overview"
            scrollOffset = 0
        }
            .bounds(leftPos + HOME_X, topPos + NAV_Y, 40, 20)
            .build())
        
        // Next button
        addRenderableWidget(Button.builder(Component.literal(">")) { nextChapter() }
            .bounds(leftPos + NEXT_X, topPos + NAV_Y, 20, 20)
            .build())
    }
    
    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        
        // Draw background
        guiGraphics.fill(leftPos, topPos, leftPos + MANUAL_WIDTH, topPos + MANUAL_HEIGHT, 0xFF1a1a1a.toInt())
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + MANUAL_WIDTH - 2, topPos + MANUAL_HEIGHT - 2, 0xFF2a2a2a.toInt())
        
        // Draw content border
        guiGraphics.fill(leftPos + CONTENT_X - 2, topPos + CONTENT_Y - 2, 
            leftPos + CONTENT_X + CONTENT_WIDTH + 2, topPos + CONTENT_Y + CONTENT_HEIGHT + 2, 0xFF333333.toInt())
        guiGraphics.fill(leftPos + CONTENT_X, topPos + CONTENT_Y, 
            leftPos + CONTENT_X + CONTENT_WIDTH, topPos + CONTENT_Y + CONTENT_HEIGHT, 0xFF000000.toInt())
        
        // Draw chapter title
        val chapterTitle = CHAPTERS.find { it.first == currentChapter }?.second ?: "Manual"
        guiGraphics.drawCenteredString(font, chapterTitle, leftPos + MANUAL_WIDTH / 2, topPos + 4, 0x00AAFF)
        
        // Draw content
        val content = CONTENT[currentChapter] ?: listOf("No content available.")
        val lineHeight = 10
        val maxLines = CONTENT_HEIGHT / lineHeight
        
        val startLine = scrollOffset.coerceIn(0, (content.size - maxLines).coerceAtLeast(0))
        content.drop(startLine).take(maxLines).forEachIndexed { index, line ->
            val color = when {
                line.startsWith("===") -> 0xFFFF00  // Yellow for headers
                line.contains(":") && !line.contains("//") -> 0x00FF00  // Green for labels
                else -> 0xCCCCCC  // White for normal text
            }
            guiGraphics.drawString(font, line, leftPos + CONTENT_X + 4, topPos + CONTENT_Y + 4 + index * lineHeight, color)
        }
        
        // Draw scroll indicators if needed
        if (scrollOffset > 0) {
            guiGraphics.drawCenteredString(font, "▲", leftPos + CONTENT_X + CONTENT_WIDTH - 8, topPos + CONTENT_Y + 2, 0x666666)
        }
        if (startLine + maxLines < content.size) {
            guiGraphics.drawCenteredString(font, "▼", leftPos + CONTENT_X + CONTENT_WIDTH - 8, topPos + CONTENT_Y + CONTENT_HEIGHT - 10, 0x666666)
        }
        
        // Draw page number
        val chapterIndex = CHAPTERS.indexOfFirst { it.first == currentChapter } + 1
        guiGraphics.drawCenteredString(font, "$chapterIndex / ${CHAPTERS.size}", leftPos + MANUAL_WIDTH / 2, topPos + MANUAL_HEIGHT - 8, 0x666666)
        
        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }
    
    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val content = CONTENT[currentChapter] ?: return false
        val lineHeight = 10
        val maxLines = CONTENT_HEIGHT / lineHeight
        val maxScroll = (content.size - maxLines).coerceAtLeast(0)
        
        scrollOffset = (scrollOffset - scrollY.toInt()).coerceIn(0, maxScroll)
        return true
    }
    
    private fun prevChapter() {
        val currentIndex = CHAPTERS.indexOfFirst { it.first == currentChapter }
        if (currentIndex > 0) {
            currentChapter = CHAPTERS[currentIndex - 1].first
            scrollOffset = 0
        }
    }
    
    private fun nextChapter() {
        val currentIndex = CHAPTERS.indexOfFirst { it.first == currentChapter }
        if (currentIndex < CHAPTERS.size - 1) {
            currentChapter = CHAPTERS[currentIndex + 1].first
            scrollOffset = 0
        }
    }
    
    override fun isPauseScreen(): Boolean = false
    
    /**
     * Opens the manual screen.
     */
    fun open() {
        Minecraft.getInstance().setScreen(this)
    }
}
