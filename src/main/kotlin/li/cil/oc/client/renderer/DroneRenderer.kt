package li.cil.oc.client.renderer

import com.mojang.blaze3d.vertex.PoseStack
import li.cil.oc.OpenComputers
import li.cil.oc.server.entity.DroneEntity
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
 * Custom render state for drone entities.
 */
class DroneRenderState : LivingEntityRenderState() {
    var isRunning: Boolean = false
    var lightColor: Int = 0x66DD55
    var tickCount: Int = 0
}

/**
 * Renderer for Drone entities.
 * Renders a simple hovering drone with animated propellers.
 */
class DroneRenderer(context: EntityRendererProvider.Context) : MobRenderer<DroneEntity, DroneRenderState, DroneModel>(
    context,
    DroneModel(context.bakeLayer(DroneModel.LAYER_LOCATION)),
    0.3f // shadow radius
) {
    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "textures/model/drone.png")
    }
    
    override fun getTextureLocation(state: DroneRenderState): ResourceLocation = TEXTURE
    
    override fun createRenderState(): DroneRenderState = DroneRenderState()
    
    override fun extractRenderState(entity: DroneEntity, state: DroneRenderState, partialTick: Float) {
        super.extractRenderState(entity, state, partialTick)
        state.isRunning = entity.isRunning()
        state.lightColor = entity.getLightColor()
        state.tickCount = entity.tickCount
    }
    
    override fun render(
        state: DroneRenderState,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        // Hover animation
        val hoverOffset = sin((state.tickCount + state.ageInTicks) * 0.15).toFloat() * 0.05f
        poseStack.pushPose()
        poseStack.translate(0.0, hoverOffset.toDouble(), 0.0)
        
        super.render(state, poseStack, buffer, packedLight)
        
        poseStack.popPose()
    }
}

/**
 * Simple drone model - cube body with propeller arms.
 */
class DroneModel(root: ModelPart) : EntityModel<DroneRenderState>(root) {
    private val body: ModelPart = root.getChild("body")
    private val propellerNE: ModelPart = root.getChild("propeller_ne")
    private val propellerNW: ModelPart = root.getChild("propeller_nw")
    private val propellerSE: ModelPart = root.getChild("propeller_se")
    private val propellerSW: ModelPart = root.getChild("propeller_sw")
    
    companion object {
        val LAYER_LOCATION = ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, "drone"),
            "main"
        )
        
        fun createBodyLayer(): LayerDefinition {
            val meshDefinition = MeshDefinition()
            val partDefinition = meshDefinition.root
            
            // Main body - small cube
            partDefinition.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                    .texOffs(0, 0)
                    .addBox(-3f, -3f, -3f, 6f, 6f, 6f, CubeDeformation.NONE),
                PartPose.offset(0f, 20f, 0f)
            )
            
            // Propeller arms with rotors
            val armOffset = 4f
            
            partDefinition.addOrReplaceChild(
                "propeller_ne",
                CubeListBuilder.create()
                    .texOffs(0, 12)
                    .addBox(-1f, 0f, -1f, 2f, 1f, 2f, CubeDeformation.NONE),
                PartPose.offset(armOffset, 17f, -armOffset)
            )
            
            partDefinition.addOrReplaceChild(
                "propeller_nw",
                CubeListBuilder.create()
                    .texOffs(0, 12)
                    .addBox(-1f, 0f, -1f, 2f, 1f, 2f, CubeDeformation.NONE),
                PartPose.offset(-armOffset, 17f, -armOffset)
            )
            
            partDefinition.addOrReplaceChild(
                "propeller_se",
                CubeListBuilder.create()
                    .texOffs(0, 12)
                    .addBox(-1f, 0f, -1f, 2f, 1f, 2f, CubeDeformation.NONE),
                PartPose.offset(armOffset, 17f, armOffset)
            )
            
            partDefinition.addOrReplaceChild(
                "propeller_sw",
                CubeListBuilder.create()
                    .texOffs(0, 12)
                    .addBox(-1f, 0f, -1f, 2f, 1f, 2f, CubeDeformation.NONE),
                PartPose.offset(-armOffset, 17f, armOffset)
            )
            
            return LayerDefinition.create(meshDefinition, 32, 32)
        }
    }
    
    override fun setupAnim(state: DroneRenderState) {
        super.setupAnim(state)
        
        // Spin propellers when running
        if (state.isRunning) {
            val spin = state.ageInTicks * 2f
            propellerNE.yRot = spin
            propellerNW.yRot = -spin
            propellerSE.yRot = -spin
            propellerSW.yRot = spin
        } else {
            propellerNE.yRot = 0f
            propellerNW.yRot = 0f
            propellerSE.yRot = 0f
            propellerSW.yRot = 0f
        }
    }
}
