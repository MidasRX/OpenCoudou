package li.cil.oc.client.renderer

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import org.joml.Matrix4f
import kotlin.math.sin

/**
 * Renderer for microcontroller block entities.
 * Shows power LED and running status indicator.
 */
class MicrocontrollerRenderer(ctx: BlockEntityRendererProvider.Context) : BlockEntityRenderer<MicrocontrollerBlockEntity> {
    
    companion object {
        // LED position on the front face
        private const val LED_SIZE = 0.06f
        private const val LED_X = 0.5f        // Center
        private const val LED_Y = 0.8f        // Near top
        private const val LED_Z = -0.001f     // Just in front of face
        
        // Colors
        private const val COLOR_LED_OFF = 0x1a1a1a
        private const val COLOR_LED_RUNNING = 0x00FF00    // Green when running
        private const val COLOR_LED_STARTING = 0xFFFF00   // Yellow when starting
        private const val COLOR_LED_ERROR = 0xFF0000       // Red for error
    }
    
    override fun render(
        blockEntity: MicrocontrollerBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val level = blockEntity.level ?: return
        val state = blockEntity.blockState
        
        // Get facing if horizontal directional block
        val facing = if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            state.getValue(HorizontalDirectionalBlock.FACING)
        } else {
            Direction.NORTH
        }
        
        poseStack.pushPose()
        poseStack.translate(0.5, 0.0, 0.5)
        
        // Rotate to face the correct direction
        val rotation = when (facing) {
            Direction.NORTH -> 180f
            Direction.SOUTH -> 0f
            Direction.WEST -> 90f
            Direction.EAST -> -90f
            else -> 0f
        }
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation))
        poseStack.translate(-0.5, 0.0, -0.5)
        
        val vertexConsumer = buffer.getBuffer(RenderType.solid())
        val matrix = poseStack.last().pose()
        
        val isRunning = blockEntity.isRunning()
        val gameTime = level.gameTime + partialTick
        
        // Determine LED color based on state
        val ledColor = when {
            isRunning -> {
                // Gently pulse when running
                val pulse = (sin(gameTime * 0.1) * 0.1 + 0.9).toFloat()
                blendColor(COLOR_LED_RUNNING, 0x008800, 1f - pulse)
            }
            else -> COLOR_LED_OFF
        }
        
        // Render status LED
        renderLED(matrix, vertexConsumer, LED_X, LED_Y, ledColor, packedLight, packedOverlay)
        
        // If running, add a small activity indicator
        if (isRunning) {
            val activityX = LED_X + 0.15f
            val activityColor = if ((gameTime.toInt() / 5) % 3 == 0) 0x00AAFF else COLOR_LED_OFF
            renderLED(matrix, vertexConsumer, activityX, LED_Y, activityColor, packedLight, packedOverlay, LED_SIZE * 0.5f)
        }
        
        poseStack.popPose()
    }
    
    private fun renderLED(
        matrix: Matrix4f,
        buffer: VertexConsumer,
        xCenter: Float,
        yCenter: Float,
        color: Int,
        packedLight: Int,
        packedOverlay: Int,
        size: Float = LED_SIZE
    ) {
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        
        val x1 = xCenter - size / 2
        val x2 = xCenter + size / 2
        val y1 = yCenter - size / 2
        val y2 = yCenter + size / 2
        
        buffer.addVertex(matrix, x1, y1, LED_Z).setColor(r, g, b, 1f)
            .setUv(0f, 0f).setOverlay(packedOverlay).setLight(packedLight)
            .setNormal(0f, 0f, -1f)
        buffer.addVertex(matrix, x1, y2, LED_Z).setColor(r, g, b, 1f)
            .setUv(0f, 1f).setOverlay(packedOverlay).setLight(packedLight)
            .setNormal(0f, 0f, -1f)
        buffer.addVertex(matrix, x2, y2, LED_Z).setColor(r, g, b, 1f)
            .setUv(1f, 1f).setOverlay(packedOverlay).setLight(packedLight)
            .setNormal(0f, 0f, -1f)
        buffer.addVertex(matrix, x2, y1, LED_Z).setColor(r, g, b, 1f)
            .setUv(1f, 0f).setOverlay(packedOverlay).setLight(packedLight)
            .setNormal(0f, 0f, -1f)
    }
    
    private fun blendColor(color1: Int, color2: Int, t: Float): Int {
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF
        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF
        
        val r = (r1 + (r2 - r1) * t).toInt()
        val g = (g1 + (g2 - g1) * t).toInt()
        val b = (b1 + (b2 - b1) * t).toInt()
        
        return (r shl 16) or (g shl 8) or b
    }
    
    override fun shouldRenderOffScreen(blockEntity: MicrocontrollerBlockEntity): Boolean = false
}
