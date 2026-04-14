package li.cil.oc.client.renderer

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import li.cil.oc.client.Sound
import li.cil.oc.common.blockentity.CaseBlockEntity
import li.cil.oc.common.init.ModSoundEvents
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import org.joml.Matrix4f

/**
 * Renderer for computer case block entities.
 * Shows power status LED indicator on the front of the case.
 * Also manages the looping computer running sound.
 */
class CaseRenderer(ctx: BlockEntityRendererProvider.Context) : BlockEntityRenderer<CaseBlockEntity> {
    
    companion object {
        // LED position on the front face
        private const val LED_SIZE = 0.04f
        private const val LED_X = 0.9f        // Near right edge
        private const val LED_Y_POWER = 0.85f  // Power LED at top
        private const val LED_Y_HDD = 0.78f    // HDD activity LED below
        private const val LED_Z = -0.001f      // Just in front of face
        
        // Colors
        private const val COLOR_LED_OFF = 0x1a1a1a
        private const val COLOR_POWER_ON = 0x00FF00    // Green when running
        private const val COLOR_POWER_OFF = 0x330000   // Dim red when off
        private const val COLOR_HDD_ACTIVE = 0xFF8800  // Orange for HDD activity
    }
    
    override fun render(
        blockEntity: CaseBlockEntity,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        val level = blockEntity.level ?: return
        val state = blockEntity.blockState
        val facing = state.getValue(HorizontalDirectionalBlock.FACING)
        val pos = blockEntity.blockPos
        
        val isPowered = blockEntity.isPowered
        
        // Handle sound - start when powered, stop when not
        if (isPowered && !Sound.isPlaying(pos)) {
            Sound.startLoop(pos, ModSoundEvents.COMPUTER_RUNNING.get(), 0.5f)
        } else if (!isPowered && Sound.isPlaying(pos)) {
            Sound.stopLoop(pos)
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
        
        val gameTime = level.gameTime + partialTick
        
        // Power LED
        val powerColor = if (isPowered) COLOR_POWER_ON else COLOR_POWER_OFF
        renderLED(matrix, vertexConsumer, LED_X, LED_Y_POWER, powerColor, packedLight, packedOverlay)
        
        // HDD activity LED (blinks when computer is running)
        if (isPowered) {
            // Simulate HDD activity with random-ish blinking
            val isActive = ((gameTime.toInt() * 7) % 13) < 6
            val hddColor = if (isActive) COLOR_HDD_ACTIVE else COLOR_LED_OFF
            renderLED(matrix, vertexConsumer, LED_X, LED_Y_HDD, hddColor, packedLight, packedOverlay)
        } else {
            renderLED(matrix, vertexConsumer, LED_X, LED_Y_HDD, COLOR_LED_OFF, packedLight, packedOverlay)
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
        packedOverlay: Int
    ) {
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        
        val x1 = xCenter - LED_SIZE / 2
        val x2 = xCenter + LED_SIZE / 2
        val y1 = yCenter - LED_SIZE / 2
        val y2 = yCenter + LED_SIZE / 2
        
        // LED quad on front face (z = 0, but slightly offset)
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
    
    override fun shouldRenderOffScreen(blockEntity: CaseBlockEntity): Boolean = false
}
