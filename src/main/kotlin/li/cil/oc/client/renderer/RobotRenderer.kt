package li.cil.oc.client.renderer

import com.mojang.blaze3d.vertex.PoseStack
import li.cil.oc.OpenComputers
import li.cil.oc.server.entity.RobotEntity
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.resources.ResourceLocation
import kotlin.math.sin

/**
 * Custom render state for robot entities.
 */
class RobotRenderState : LivingEntityRenderState() {
    var isRunning: Boolean = false
    var lightColor: Int = 0x66DD55
    var tickCount: Int = 0
}

/**
 * Renderer for Robot entities.
 * Renders as a cube with a screen face and light indicator.
 */
class RobotRenderer(context: EntityRendererProvider.Context) : MobRenderer<RobotEntity, RobotRenderState, RobotModel>(
    context,
    RobotModel(context.bakeLayer(RobotModel.LAYER_LOCATION)),
    0.4f // shadow radius
) {
    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/blocks/robot.png")
    }
    
    override fun getTextureLocation(state: RobotRenderState): ResourceLocation = TEXTURE
    
    override fun createRenderState(): RobotRenderState = RobotRenderState()
    
    override fun extractRenderState(entity: RobotEntity, state: RobotRenderState, partialTick: Float) {
        super.extractRenderState(entity, state, partialTick)
        state.isRunning = entity.isRunning()
        state.lightColor = entity.getLightColor()
        state.tickCount = entity.tickCount
    }
    
    override fun render(
        state: RobotRenderState,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        poseStack.pushPose()
        
        // Idle bobbing animation when running
        if (state.isRunning) {
            val bob = sin((state.tickCount + state.ageInTicks) * 0.1).toFloat() * 0.02f
            poseStack.translate(0.0, bob.toDouble(), 0.0)
        }
        
        super.render(state, poseStack, buffer, packedLight)
        
        poseStack.popPose()
    }
}

/**
 * Simple robot model - cube body with a face screen.
 */
class RobotModel(root: ModelPart) : EntityModel<RobotRenderState>(root) {
    private val body: ModelPart = root.getChild("body")
    private val head: ModelPart = root.getChild("head")
    private val lightIndicator: ModelPart = root.getChild("light")
    
    companion object {
        val LAYER_LOCATION = ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "robot"),
            "main"
        )
        
        fun createBodyLayer(): LayerDefinition {
            val meshDefinition = MeshDefinition()
            val partDefinition = meshDefinition.root
            
            // Main body - larger cube
            partDefinition.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                    .texOffs(0, 0)
                    .addBox(-7f, 0f, -7f, 14f, 10f, 14f, CubeDeformation.NONE),
                PartPose.offset(0f, 14f, 0f)
            )
            
            // Head/screen area
            partDefinition.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                    .texOffs(0, 24)
                    .addBox(-5f, 0f, -5f, 10f, 4f, 10f, CubeDeformation.NONE),
                PartPose.offset(0f, 10f, 0f)
            )
            
            // Status light on top
            partDefinition.addOrReplaceChild(
                "light",
                CubeListBuilder.create()
                    .texOffs(56, 0)
                    .addBox(-1f, 0f, -1f, 2f, 2f, 2f, CubeDeformation.NONE),
                PartPose.offset(0f, 6f, 0f)
            )
            
            return LayerDefinition.create(meshDefinition, 64, 64)
        }
    }
    
    override fun setupAnim(state: RobotRenderState) {
        super.setupAnim(state)
        
        // Head rotation to follow look direction
        head.yRot = state.yRot * (Math.PI.toFloat() / 180f)
        head.xRot = state.xRot * (Math.PI.toFloat() / 180f)
        
        // Light blinks when running
        if (state.isRunning) {
            val blink = ((state.ageInTicks % 40) < 20)
            lightIndicator.visible = blink
        } else {
            lightIndicator.visible = false
        }
    }
}
