package li.cil.oc.client.renderer

import com.mojang.blaze3d.vertex.*
import li.cil.oc.common.block.ScreenBlock
import li.cil.oc.common.blockentity.ScreenBlockEntity
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import org.joml.Matrix4f

/**
 * Simple screen renderer that displays TextBuffer contents in the world.
 * Renders background color and text characters.
 */
class SimpleScreenRenderer(context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<ScreenBlockEntity> {
    
    companion object {
        const val CHAR_WIDTH = 6f
        const val CHAR_HEIGHT = 9f
        const val Z_OFFSET = 0.001f
        const val MARGIN = 0.0625f  // 1 pixel margin
    }
    
    private val font: Font = context.font
    
    override fun render(
        blockEntity: ScreenBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val buffer = blockEntity.buffer
        if (buffer.width <= 0 || buffer.height <= 0) return
        
        val facing = try {
            blockEntity.blockState.getValue(ScreenBlock.FACING)
        } catch (_: Exception) {
            Direction.NORTH
        }
        
        poseStack.pushPose()
        
        // Transform to face the correct direction
        // Position at face of block with small offset to prevent z-fighting
        when (facing) {
            Direction.NORTH -> {
                poseStack.translate(1.0 - MARGIN, 1.0 - MARGIN, Z_OFFSET.toDouble())
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180f))
            }
            Direction.SOUTH -> {
                poseStack.translate(MARGIN.toDouble(), 1.0 - MARGIN, 1.0 - Z_OFFSET)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f))
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180f))
            }
            Direction.WEST -> {
                poseStack.translate(Z_OFFSET.toDouble(), 1.0 - MARGIN, 1.0 - MARGIN)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90f))
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180f))
            }
            Direction.EAST -> {
                poseStack.translate(1.0 - Z_OFFSET, 1.0 - MARGIN, MARGIN.toDouble())
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90f))
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180f))
            }
            else -> {
                poseStack.translate(1.0 - MARGIN, 1.0 - MARGIN, Z_OFFSET.toDouble())
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180f))
            }
        }
        
        // Screen area (inside margins)
        val screenWidth = 1f - MARGIN * 2
        val screenHeight = 1f - MARGIN * 2
        
        // Calculate scale to fit all text
        val textPixelWidth = buffer.width * CHAR_WIDTH
        val textPixelHeight = buffer.height * CHAR_HEIGHT
        val scaleX = screenWidth / textPixelWidth
        val scaleY = screenHeight / textPixelHeight
        val scale = minOf(scaleX, scaleY)
        
        // First render background cells
        val consumer = bufferSource.getBuffer(RenderType.translucent())
        val matrix = poseStack.last().pose()
        
        poseStack.scale(scale, scale, 1f)
        
        for (y in 0 until buffer.height) {
            for (x in 0 until buffer.width) {
                val idx = y * buffer.width + x
                val bg = buffer.bgData[idx]
                
                // Only render non-black backgrounds (optimization)
                if (bg != 0) {
                    val px = x * CHAR_WIDTH
                    val py = y * CHAR_HEIGHT
                    
                    val r = ((bg shr 16) and 0xFF) / 255f
                    val g = ((bg shr 8) and 0xFF) / 255f
                    val b = (bg and 0xFF) / 255f
                    
                    // Draw background quad
                    val scaledMatrix = poseStack.last().pose()
                    consumer.addVertex(scaledMatrix, px, py, 0f).setColor(r, g, b, 1f).setLight(LightTexture.FULL_BRIGHT).setNormal(0f, 0f, 1f)
                    consumer.addVertex(scaledMatrix, px, py + CHAR_HEIGHT, 0f).setColor(r, g, b, 1f).setLight(LightTexture.FULL_BRIGHT).setNormal(0f, 0f, 1f)
                    consumer.addVertex(scaledMatrix, px + CHAR_WIDTH, py + CHAR_HEIGHT, 0f).setColor(r, g, b, 1f).setLight(LightTexture.FULL_BRIGHT).setNormal(0f, 0f, 1f)
                    consumer.addVertex(scaledMatrix, px + CHAR_WIDTH, py, 0f).setColor(r, g, b, 1f).setLight(LightTexture.FULL_BRIGHT).setNormal(0f, 0f, 1f)
                }
            }
        }
        
        // Now render text characters
        for (y in 0 until buffer.height) {
            for (x in 0 until buffer.width) {
                val idx = y * buffer.width + x
                val char = buffer.charData[idx]
                if (char > 32) {  // Skip space and control characters
                    val fg = buffer.fgData[idx]
                    val px = x * CHAR_WIDTH
                    val py = y * CHAR_HEIGHT
                    
                    val charStr = String(Character.toChars(char))
                    font.drawInBatch(
                        charStr, 
                        px + 1f, py + 0.5f,  // Small offset for better centering
                        fg or (0xFF shl 24),  // Ensure full alpha
                        false,  // No shadow
                        poseStack.last().pose(),
                        bufferSource,
                        Font.DisplayMode.NORMAL,
                        0,  // No background
                        LightTexture.FULL_BRIGHT
                    )
                }
            }
        }
        
        poseStack.popPose()
    }
    
    override fun shouldRenderOffScreen(blockEntity: ScreenBlockEntity): Boolean = true
    
    override fun getViewDistance(): Int = 64
}
