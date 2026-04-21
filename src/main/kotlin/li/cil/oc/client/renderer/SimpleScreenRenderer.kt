package li.cil.oc.client.renderer

import com.mojang.blaze3d.vertex.*
import li.cil.oc.OpenComputers
import li.cil.oc.common.block.ScreenBlock
import li.cil.oc.common.blockentity.ScreenBlockEntity
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction

/**
 * Simple screen renderer that displays TextBuffer contents in the world.
 * Renders text on the front face of the screen block.
 */
class SimpleScreenRenderer(context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<ScreenBlockEntity> {
    
    companion object {
        const val Z_OFFSET = 0.005f      // Just enough to clear the block face without z-fighting
        const val MARGIN = 2.0f / 16.0f  // 2-pixel bezel from block edge (matches texture)
        private var debugLogTimer = 0
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

        // Debug: log buffer state periodically
        debugLogTimer++
        if (debugLogTimer % 200 == 0) {
            val nonSpace = buffer.charData.count { it > 32 }
            OpenComputers.LOGGER.info("RENDERER: buffer ${buffer.width}x${buffer.height}, nonSpaceChars=$nonSpace, charData[0]=${buffer.charData[0]}")
        }
        
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
            // facing=NORTH: display on north face (z=0), viewer looks south at it from z<0
            Direction.NORTH -> {
                poseStack.translate((1.0 - MARGIN).toDouble(), (1.0 - MARGIN).toDouble(), -Z_OFFSET.toDouble())
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f))
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180f))
            }
            // facing=SOUTH: display on south face (z=1), viewer looks north at it from z>1
            Direction.SOUTH -> {
                poseStack.translate(MARGIN.toDouble(), (1.0 - MARGIN).toDouble(), (1.0 + Z_OFFSET).toDouble())
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180f))
            }
            // facing=EAST: display on east face (x=1), viewer looks west at it from x>1
            Direction.EAST -> {
                poseStack.translate((1.0 + Z_OFFSET).toDouble(), (1.0 - MARGIN).toDouble(), MARGIN.toDouble())
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90f))
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180f))
            }
            // facing=WEST: display on west face (x=0), viewer looks east at it from x<0
            Direction.WEST -> {
                poseStack.translate(-Z_OFFSET.toDouble(), (1.0 - MARGIN).toDouble(), (1.0 - MARGIN).toDouble())
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90f))
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180f))
            }
            else -> {
                poseStack.translate(MARGIN.toDouble(), (1.0 - MARGIN).toDouble(), (1.0 + Z_OFFSET).toDouble())
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
