package li.cil.oc.client.renderer

import com.mojang.blaze3d.vertex.PoseStack
import li.cil.oc.common.block.ScreenBlock
import li.cil.oc.common.blockentity.ScreenBlockEntity
import li.cil.oc.util.TextBuffer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction

/**
 * Renders the text buffer content on screen blocks.
 */
class ScreenRenderer(context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<ScreenBlockEntity> {
    
    companion object {
        // Z offset to prevent z-fighting with block face
        const val Z_OFFSET = 0.001f
    }
    
    private val font: Font = Minecraft.getInstance().font
    
    override fun render(
        blockEntity: ScreenBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        if (!blockEntity.isOn) return
        
        val buffer = blockEntity.buffer
        if (buffer.width == 0 || buffer.height == 0) return
        
        val state = blockEntity.blockState
        val facing = state.getValue(ScreenBlock.FACING)
        
        poseStack.pushPose()
        
        // Move to center of block
        poseStack.translate(0.5, 0.5, 0.5)
        
        // Rotate based on facing direction
        when (facing) {
            Direction.NORTH -> { /* Default, no rotation needed */ }
            Direction.SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f))
            Direction.WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90f))
            Direction.EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90f))
            else -> {}
        }
        
        // Move to front face of block
        poseStack.translate(0.0, 0.0, -0.5 - Z_OFFSET)
        
        // Flip text right-side up (text renders upside down by default)
        poseStack.scale(1f, -1f, 1f)
        
        // Calculate scale to fit text in block
        val screenWidth = 0.9f
        val screenHeight = 0.9f
        
        val charScaleX = screenWidth / (buffer.width * 6f) // Font char is ~6 pixels wide
        val charScaleY = screenHeight / (buffer.height * 9f) // Font char is ~9 pixels tall
        val scale = minOf(charScaleX, charScaleY)
        
        poseStack.scale(scale, scale, scale)
        
        // Center the text
        val textWidth = buffer.width * 6f
        val textHeight = buffer.height * 9f
        poseStack.translate((-textWidth / 2).toDouble(), (-textHeight / 2).toDouble(), 0.0)
        
        // Render each character
        val matrix = poseStack.last().pose()
        
        for (y in 0 until buffer.height) {
            for (x in 0 until buffer.width) {
                val char = buffer.getChar(x, y)
                val fg = buffer.getForeground(x, y)
                
                if (char != 0 && char != ' '.code) {
                    val charStr = char.toChar().toString()
                    val color = fg or (0xFF shl 24) // Add alpha
                    
                    // Draw character
                    font.drawInBatch(
                        charStr,
                        (x * 6).toFloat(),
                        (y * 9).toFloat(),
                        color,
                        false, // no shadow
                        matrix,
                        bufferSource,
                        Font.DisplayMode.NORMAL,
                        0, // background color (transparent)
                        packedLight
                    )
                }
            }
        }
        
        poseStack.popPose()
    }
    
    override fun shouldRenderOffScreen(blockEntity: ScreenBlockEntity): Boolean = true
    
    override fun getViewDistance(): Int = 64
}
