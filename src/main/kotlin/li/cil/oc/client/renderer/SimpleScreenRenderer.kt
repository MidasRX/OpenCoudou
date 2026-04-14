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

/**
 * Simple screen renderer that displays TextBuffer contents in the world.
 * Renders text on the front face of the screen block.
 */
class SimpleScreenRenderer(context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<ScreenBlockEntity> {
    
    companion object {
        const val Z_OFFSET = 0.005f      // Small offset to prevent z-fighting
        const val MARGIN = 2.0f / 16.0f  // 2-pixel bezel from block edge (matches texture)
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
        
        // Scale: fit all characters inside the margin area, uniform scale
        val available = 1.0f - MARGIN * 2
        val fontScale = minOf(
            available / (buffer.width  * 6f),
            available / (buffer.height * 9f)
        )

        poseStack.pushPose()

        // For each facing we position the origin at the viewer's top-left corner of the
        // display face, then rotate so that:
        //   local +X  = viewer's rightward direction on the face
        //   local +Y  = downward (font's +Y is down in glyph space, matches world -Y)
        // The XP(180) flip turns world +Y into local -Y (= font downward).
        when (facing) {
            // Face at z=0; viewer is at z<0 looking +z; text at z=-Z_OFFSET (in front of face)
            // col 0 = west (x=MARGIN), col increases east
            Direction.NORTH -> {
                poseStack.translate(MARGIN.toDouble(), (1.0 - MARGIN).toDouble(), -Z_OFFSET.toDouble())
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180f))
            }
            // Face at z=1; viewer is at z>1 looking -z; text at z=1+Z_OFFSET (in front of face)
            // col 0 = east (x=1-MARGIN), col increases west
            Direction.SOUTH -> {
                poseStack.translate((1.0 - MARGIN).toDouble(), (1.0 - MARGIN).toDouble(), (1.0 + Z_OFFSET).toDouble())
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f))
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180f))
            }
            // Face at x=1; viewer is at x>1 looking -x; text at x=1+Z_OFFSET (in front of face)
            // col 0 = south (z=1-MARGIN), col increases north
            Direction.EAST -> {
                poseStack.translate((1.0 + Z_OFFSET).toDouble(), (1.0 - MARGIN).toDouble(), (1.0 - MARGIN).toDouble())
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90f))
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180f))
            }
            // Face at x=0; viewer is at x<0 looking +x; text at x=-Z_OFFSET (in front of face)
            // col 0 = north (z=MARGIN), col increases south
            Direction.WEST -> {
                poseStack.translate(-Z_OFFSET.toDouble(), (1.0 - MARGIN).toDouble(), MARGIN.toDouble())
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90f))
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180f))
            }
            else -> {
                poseStack.translate(MARGIN.toDouble(), (1.0 - MARGIN).toDouble(), -Z_OFFSET.toDouble())
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180f))
            }
        }

        poseStack.scale(fontScale, fontScale, fontScale)

        // Render each character at its (column * 6, row * 9) position in local space
        for (row in 0 until buffer.height) {
            for (col in 0 until buffer.width) {
                val idx = row * buffer.width + col
                val char = buffer.charData[idx]
                if (char <= 32) continue

                val fg = buffer.fgData[idx]
                val charStr = String(Character.toChars(char))
                font.drawInBatch(
                    charStr,
                    col * 6f, row * 9f,
                    fg or (0xFF shl 24),
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.NORMAL,
                    0,
                    LightTexture.FULL_BRIGHT
                )
            }
        }

        poseStack.popPose()
    }
    
    override fun shouldRenderOffScreen(blockEntity: ScreenBlockEntity): Boolean = true
    
    override fun getViewDistance(): Int = 64
}
