package li.cil.oc.client.gui

import li.cil.oc.OpenComputers
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData

/**
 * Drive configuration screen.
 * Allows setting managed/unmanaged mode and locking the drive.
 */
class DriveScreen(
    private val driveStack: () -> ItemStack
) : Screen(Component.translatable("gui.opencomputers.drive")) {
    
    companion object {
        private val BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            OpenComputers.MOD_ID, "textures/gui/drive.png"
        )
        
        private const val WINDOW_WIDTH = 176
        private const val WINDOW_HEIGHT = 120
    }
    
    private var guiLeft = 0
    private var guiTop = 0
    
    private var managedButton: Button? = null
    private var unmanagedButton: Button? = null
    private var lockButton: Button? = null
    
    // State from drive NBT
    private var isUnmanaged = false
    private var isLocked = false
    
    override fun init() {
        super.init()
        
        guiLeft = (width - WINDOW_WIDTH) / 2
        guiTop = (height - WINDOW_HEIGHT) / 2
        
        // Read current state from drive
        readDriveState()
        
        // Managed mode button
        managedButton = Button.builder(Component.literal("Managed")) { _ ->
            setUnmanaged(false)
        }.bounds(guiLeft + 11, guiTop + 11, 74, 18).build()
        
        // Unmanaged mode button  
        unmanagedButton = Button.builder(Component.literal("Unmanaged")) { _ ->
            setUnmanaged(true)
        }.bounds(guiLeft + 91, guiTop + 11, 74, 18).build()
        
        // Lock button
        lockButton = Button.builder(Component.literal("Lock")) { _ ->
            lockDrive()
        }.bounds(guiLeft + 11, guiTop + WINDOW_HEIGHT - 42, 44, 18).build()
        
        addRenderableWidget(managedButton!!)
        addRenderableWidget(unmanagedButton!!)
        addRenderableWidget(lockButton!!)
        
        updateButtonStates()
    }
    
    private fun readDriveState() {
        val stack = driveStack()
        val customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
        val tag = customData.copyTag()
        isUnmanaged = tag.getBoolean("unmanaged")
        isLocked = tag.getBoolean("locked")
    }
    
    private fun setUnmanaged(unmanaged: Boolean) {
        isUnmanaged = unmanaged
        val stack = driveStack()
        val customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
        val tag = customData.copyTag()
        tag.putBoolean("unmanaged", unmanaged)
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
        // TODO: Send packet to server
        updateButtonStates()
    }
    
    private fun lockDrive() {
        if (isLocked) return
        isLocked = true
        val stack = driveStack()
        val customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
        val tag = customData.copyTag()
        tag.putBoolean("locked", true)
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
        // TODO: Send packet to server
        updateButtonStates()
    }
    
    private fun updateButtonStates() {
        managedButton?.active = isUnmanaged
        unmanagedButton?.active = !isUnmanaged
        lockButton?.active = !isLocked
    }
    
    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // Draw background
        guiGraphics.blit(
            RenderType::guiTextured,
            BACKGROUND,
            guiLeft, guiTop,
            0f, 0f,
            WINDOW_WIDTH, WINDOW_HEIGHT,
            256, 256
        )
        
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        
        // Draw labels
        guiGraphics.drawString(font, title, guiLeft + 8, guiTop + 6, 0x404040, false)
        
        // Draw mode description
        val modeText = if (isUnmanaged) "Unmanaged: Raw byte access" else "Managed: File system access"
        guiGraphics.drawString(font, modeText, guiLeft + 8, guiTop + 36, 0x606060, false)
        
        if (isLocked) {
            guiGraphics.drawString(font, "Drive is locked (read-only)", guiLeft + 8, guiTop + 50, 0x905050, false)
        }
    }
    
    override fun isPauseScreen(): Boolean = false
}
