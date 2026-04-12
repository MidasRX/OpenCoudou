package li.cil.oc.client.renderer

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import li.cil.oc.common.block.ScreenBlock
import li.cil.oc.common.blockentity.ScreenBlockEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction

/**
 * Simple screen renderer that displays TextBuffer contents.
 */
class SimpleScreenRenderer(context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<ScreenBlockEntity> {
    
    companion object {
        const val CHAR_WIDTH = 6f
        const val CHAR_HEIGHT = 9f
        const val Z_OFFSET = 0.005
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
        
        // Get facing direction from block state
        val facing = try {
            blockEntity.blockState.getValue(ScreenBlock.FACING)
        } catch (_: Exception) {
            Direction.NORTH
        }
        
        poseStack.pushPose()
        
        // Position based on facing direction
        when (facing) {
            Direction.NORTH -> {
                poseStack.translate(1.0, 1.0, -Z_OFFSET)
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180f))
            }
            Direction.SOUTH -> {
                poseStack.translate(0.0, 1.0, 1.0 + Z_OFFSET)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f))
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180f))
            }
            Direction.WEST -> {
                poseStack.translate(-Z_OFFSET, 1.0, 0.0)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90f))
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180f))
            }
            Direction.EAST -> {
                poseStack.translate(1.0 + Z_OFFSET, 1.0, 1.0)
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90f))
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180f))
            }
            else -> {
                poseStack.translate(1.0, 1.0, -Z_OFFSET)
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180f))
            }
        }
        
        // Calculate scale to fit text buffer into block face
        val margin = 0.05f
        val screenWidth = 1f - margin * 2
        val screenHeight = 1f - margin * 2
        val textPixelWidth = buffer.width * CHAR_WIDTH
        val textPixelHeight = buffer.height * CHAR_HEIGHT
        val scaleX = screenWidth / textPixelWidth
        val scaleY = screenHeight / textPixelHeight
        val scale = minOf(scaleX, scaleY)
        
        poseStack.translate(margin.toDouble(), margin.toDouble(), 0.0)
        poseStack.scale(scale, scale, 1f)
        
        // Render background quads first
        val bgConsumer = bufferSource.getBuffer(RenderType.gui())
        renderBackground(poseStack, bgConsumer, buffer)
        
        // Render text on top
        val matrix = poseStack.last().pose()
        for (y in 0 until buffer.height) {
            for (x in 0 until buffer.width) {
                val idx = y * buffer.width + x
                val char = buffer.charData[idx]
                if (char > 32) { // Skip spaces
                    val fg = buffer.fgData[idx]
                    val px = x * CHAR_WIDTH
                    val py = y * CHAR_HEIGHT
                    
                    // Draw character
                    poseStack.pushPose()
                    poseStack.translate(px.toDouble(), py.toDouble(), 0.01)
                    
                    val charStr = String(Character.toChars(char))
                    font.drawInBatch(
                        charStr,
                        0f, 0f,
                        fg or (0xFF shl 24), // Add alpha
                        false, // shadow
                        matrix,
                        bufferSource,
                        Font.DisplayMode.NORMAL,
                        0, // background color
                        LightTexture.FULL_BRIGHT
                    )
                    poseStack.popPose()
                }
            }
        }
        
        poseStack.popPose()
    }
    
    private fun renderBackground(poseStack: PoseStack, consumer: VertexConsumer, buffer: li.cil.oc.util.TextBuffer) {
        val matrix = poseStack.last().pose()
        
        for (y in 0 until buffer.height) {
            for (x in 0 until buffer.width) {
                val idx = y * buffer.width + x
                val bg = buffer.bgData[idx]
                
                val r = ((bg shr 16) and 0xFF) / 255f
                val g = ((bg shr 8) and 0xFF) / 255f
                val b = (bg and 0xFF) / 255f
                
                val x1 = x * CHAR_WIDTH
                val y1 = y * CHAR_HEIGHT
                val x2 = x1 + CHAR_WIDTH
                val y2 = y1 + CHAR_HEIGHT
                
                // Draw background quad
                consumer.addVertex(matrix, x1, y1, 0f).setColor(r, g, b, 1f)
                consumer.addVertex(matrix, x1, y2, 0f).setColor(r, g, b, 1f)
                consumer.addVertex(matrix, x2, y2, 0f).setColor(r, g, b, 1f)
                consumer.addVertex(matrix, x2, y1, 0f).setColor(r, g, b, 1f)
            }
        }
    }
    
    override fun shouldRenderOffScreen(blockEntity: ScreenBlockEntity): Boolean = true
}
