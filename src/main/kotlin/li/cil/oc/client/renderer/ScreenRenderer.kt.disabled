package li.cil.oc.client.renderer

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import li.cil.oc.common.block.ScreenBlock
import li.cil.oc.common.blockentity.ScreenBlockEntity
import li.cil.oc.util.OCLogger
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction

/**
 * Renders the text buffer content on screen blocks.
 * Like original OC: renders background color quads first, then text on top.
 */
class ScreenRenderer(context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<ScreenBlockEntity> {
    
    companion object {
        const val Z_OFFSET = 0.01
        const val CHAR_WIDTH = 6f
        const val CHAR_HEIGHT = 9f
    }
    
    private val font: Font = Minecraft.getInstance().font
    
    private var lastLogTime = 0L
    private var renderCount = 0
    
    override fun render(
        blockEntity: ScreenBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val buffer = blockEntity.buffer
        renderCount++
        
        val now = System.currentTimeMillis()
        if (now - lastLogTime > 5000) {
            lastLogTime = now
            val nonSpaceCount = buffer.charData.count { it > 32 }
            OCLogger.debug("[RENDERER] isOn=${blockEntity.isOn}, ${buffer.width}x${buffer.height}, nonSpace=$nonSpaceCount, renders=$renderCount")
        }
        
        if (!blockEntity.isOn) return
        if (buffer.width <= 0 || buffer.height <= 0) return
        
        val state = blockEntity.blockState
        val facing = state.getValue(ScreenBlock.FACING)
        
        poseStack.pushPose()
        
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
        
        val matrix = poseStack.last().pose()
        val light = LightTexture.FULL_BRIGHT
        
        // Render background quads first (per-cell background colors)
        val bgConsumer = bufferSource.getBuffer(RenderType.gui())
        for (y in 0 until buffer.height) {
            for (x in 0 until buffer.width) {
                val bg = buffer.getBackground(x, y)
                val r = ((bg shr 16) and 0xFF) / 255f
                val g = ((bg shr 8) and 0xFF) / 255f
                val b = (bg and 0xFF) / 255f
                
                val x0 = x * CHAR_WIDTH
                val y0 = y * CHAR_HEIGHT
                val x1 = x0 + CHAR_WIDTH
                val y1 = y0 + CHAR_HEIGHT
                
                bgConsumer.addVertex(matrix, x0, y0, 0.001f).setColor(r, g, b, 1f)
                bgConsumer.addVertex(matrix, x0, y1, 0.001f).setColor(r, g, b, 1f)
                bgConsumer.addVertex(matrix, x1, y1, 0.001f).setColor(r, g, b, 1f)
                bgConsumer.addVertex(matrix, x1, y0, 0.001f).setColor(r, g, b, 1f)
            }
        }
        
        // Render foreground text on top
        for (y in 0 until buffer.height) {
            for (x in 0 until buffer.width) {
                val charCode = buffer.getChar(x, y)
                if (charCode > 32) {
                    val fg = buffer.getForeground(x, y)
                    val color = fg or (0xFF shl 24)
                    
                    font.drawInBatch(
                        charCode.toChar().toString(),
                        x * CHAR_WIDTH,
                        y * CHAR_HEIGHT,
                        color,
                        false,
                        matrix,
                        bufferSource,
                        Font.DisplayMode.SEE_THROUGH,
                        0,
                        light
                    )
                }
            }
        }
        
        poseStack.popPose()
    }
    
    override fun shouldRenderOffScreen(blockEntity: ScreenBlockEntity): Boolean = true
    
    override fun getViewDistance(): Int = 64
}
