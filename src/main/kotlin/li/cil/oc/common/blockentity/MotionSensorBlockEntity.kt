package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import li.cil.oc.server.component.MotionSensorComponent
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * Motion Sensor block entity - detects entity movement within a configurable range.
 * Sends signals when entities move within detection range.
 */
class MotionSensorBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.MOTION_SENSOR.get(), pos, state) {
    
    private val motionSensor = MotionSensorComponent()
    
    // Detection range
    private var range = 8.0
    private val maxRange = 32.0
    
    // Sensitivity (minimum motion to trigger)
    private var sensitivity = 0.5
    
    // Tracking last known positions
    private val lastPositions = mutableMapOf<Int, Vec3>()
    
    // Detection settings
    private var detectPlayers = true
    private var detectMobs = true
    private var detectAnimals = true
    
    fun getComponent(): MotionSensorComponent = motionSensor
    
    fun tick() {
        if (level?.isClientSide == true) return
        
        detectMotion()
    }
    
    private fun detectMotion() {
        val center = Vec3(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)
        val detectionArea = AABB(
            center.x - range, center.y - range, center.z - range,
            center.x + range, center.y + range, center.z + range
        )
        
        val entities = level?.getEntitiesOfClass(LivingEntity::class.java, detectionArea) ?: return
        
        for (entity in entities) {
            if (!shouldDetect(entity)) continue
            
            val entityId = entity.id
            val currentPos = entity.position()
            val lastPos = lastPositions[entityId]
            
            if (lastPos != null) {
                val motion = currentPos.distanceTo(lastPos)
                if (motion >= sensitivity) {
                    onMotionDetected(entity, motion)
                }
            }
            
            lastPositions[entityId] = currentPos
        }
        
        // Clean up old entries for entities no longer in range
        lastPositions.keys.removeAll { entityId ->
            entities.none { it.id == entityId }
        }
    }
    
    private fun shouldDetect(entity: LivingEntity): Boolean {
        return when {
            entity is net.minecraft.world.entity.player.Player -> detectPlayers
            entity is net.minecraft.world.entity.Mob -> detectMobs
            entity is net.minecraft.world.entity.animal.Animal -> detectAnimals
            else -> true
        }
    }
    
    private fun onMotionDetected(entity: Entity, motion: Double) {
        // Would send event to connected computer
        // Signal: "motion", relX, relY, relZ, entityName
    }
    
    fun getRange(): Double = range
    
    fun setRange(newRange: Double) {
        range = newRange.coerceIn(1.0, maxRange)
        setChanged()
    }
    
    fun getSensitivity(): Double = sensitivity
    
    fun setSensitivity(newSensitivity: Double) {
        sensitivity = newSensitivity.coerceIn(0.1, 5.0)
        setChanged()
    }
    
    fun setDetectPlayers(detect: Boolean) {
        detectPlayers = detect
        setChanged()
    }
    
    fun setDetectMobs(detect: Boolean) {
        detectMobs = detect
        setChanged()
    }
    
    fun setDetectAnimals(detect: Boolean) {
        detectAnimals = detect
        setChanged()
    }
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putDouble("range", range)
        tag.putDouble("sensitivity", sensitivity)
        tag.putBoolean("detectPlayers", detectPlayers)
        tag.putBoolean("detectMobs", detectMobs)
        tag.putBoolean("detectAnimals", detectAnimals)
        tag.putString("motion_sensor_address", motionSensor.address)
    }
    
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        range = tag.getDouble("range")
        sensitivity = tag.getDouble("sensitivity")
        detectPlayers = tag.getBoolean("detectPlayers")
        detectMobs = tag.getBoolean("detectMobs")
        detectAnimals = tag.getBoolean("detectAnimals")
    }
}
