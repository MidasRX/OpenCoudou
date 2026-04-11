package li.cil.oc.server.entity

import li.cil.oc.common.init.ModItems
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Container
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.items.ItemStackHandler
import java.util.*
import kotlin.math.min

/**
 * Drone entity implementation.
 * Drones are flying robots that can move freely in 3D space.
 */
class DroneEntity(
    entityType: EntityType<out DroneEntity>,
    level: Level
) : PathfinderMob(entityType, level), Container {
    
    // State
    private var running = false
    private var tier = 1
    private var maxEnergy = 10000.0
    private var currentEnergy = maxEnergy
    private var energyPerTick = 0.5
    
    // Movement
    private var targetPosition: Vec3? = null
    private var targetVelocity = Vec3.ZERO
    private var acceleration = 1.5
    private var maxVelocity = 6.0
    private var statusText = ""
    private var statusColor = 0xFFFFFF
    
    // Inventory
    private val inventory = ItemStackHandler(8)
    private var selectedSlot = 0
    
    // Owner
    private var ownerUUID: UUID? = null
    private var ownerName = ""
    private var lightColor = 0x66DD55
    
    companion object {
        private val DATA_RUNNING: EntityDataAccessor<Boolean> = SynchedEntityData.defineId(
            DroneEntity::class.java, EntityDataSerializers.BOOLEAN
        )
        private val DATA_STATUS_TEXT: EntityDataAccessor<String> = SynchedEntityData.defineId(
            DroneEntity::class.java, EntityDataSerializers.STRING
        )
        private val DATA_STATUS_COLOR: EntityDataAccessor<Int> = SynchedEntityData.defineId(
            DroneEntity::class.java, EntityDataSerializers.INT
        )
        private val DATA_LIGHT_COLOR: EntityDataAccessor<Int> = SynchedEntityData.defineId(
            DroneEntity::class.java, EntityDataSerializers.INT
        )
        
        fun createAttributes(): AttributeSupplier.Builder = createMobAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FLYING_SPEED, 0.4)
    }
    
    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(DATA_RUNNING, false)
        builder.define(DATA_STATUS_TEXT, "")
        builder.define(DATA_STATUS_COLOR, 0xFFFFFF)
        builder.define(DATA_LIGHT_COLOR, 0x66DD55)
    }
    
    override fun registerGoals() {
        goalSelector.addGoal(0, FloatGoal(this))
        goalSelector.addGoal(1, DroneFollowTargetGoal(this))
    }
    
    override fun tick() {
        super.tick()
        
        if (!level().isClientSide) {
            if (running) {
                currentEnergy -= energyPerTick
                if (currentEnergy <= 0) {
                    currentEnergy = 0.0
                    stop()
                }
            }
            
            // Movement towards target
            if (targetPosition != null) {
                val current = position()
                val target = targetPosition!!
                val delta = target.subtract(current)
                val distance = delta.length()
                
                if (distance > 0.1) {
                    val direction = delta.normalize()
                    val speed = min(maxVelocity, distance * 0.5)
                    val newVel = direction.scale(speed)
                    
                    targetVelocity = Vec3(
                        lerp(targetVelocity.x, newVel.x, acceleration * 0.1),
                        lerp(targetVelocity.y, newVel.y, acceleration * 0.1),
                        lerp(targetVelocity.z, newVel.z, acceleration * 0.1)
                    )
                    
                    deltaMovement = targetVelocity.scale(0.05)
                    move(MoverType.SELF, deltaMovement)
                } else {
                    targetPosition = null
                    targetVelocity = Vec3.ZERO
                    deltaMovement = Vec3.ZERO
                }
            } else {
                val hoverBob = kotlin.math.sin(tickCount * 0.1) * 0.01
                deltaMovement = Vec3(0.0, hoverBob, 0.0)
            }
        }
    }
    
    private fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t
    
    // State Control
    fun start(): Boolean {
        if (running) return false
        if (currentEnergy <= 0) return false
        running = true
        entityData.set(DATA_RUNNING, true)
        return true
    }
    
    fun stop(): Boolean {
        if (!running) return false
        running = false
        entityData.set(DATA_RUNNING, false)
        targetPosition = null
        targetVelocity = Vec3.ZERO
        return true
    }
    
    fun isRunning(): Boolean = running
    
    // Movement
    fun moveToTarget(x: Double, y: Double, z: Double): Boolean {
        if (!running) return false
        
        val target = Vec3(x, y, z)
        val current = position()
        val distance = current.distanceTo(target)
        
        val maxRange = when (tier) {
            1 -> 16.0
            2 -> 32.0
            else -> 64.0
        }
        
        if (distance > maxRange) return false
        
        val obstructed = level().getBlockCollisions(this, AABB.ofSize(target, 0.5, 0.5, 0.5)).iterator().hasNext()
        if (obstructed) return false
        
        targetPosition = target
        return true
    }
    
    fun getVelocity(): Vec3 = targetVelocity
    fun getOffset(): Vec3 = targetPosition?.subtract(position()) ?: Vec3.ZERO
    
    // Status
    fun setStatusText(text: String) {
        statusText = text.take(32)
        entityData.set(DATA_STATUS_TEXT, statusText)
    }
    
    fun getStatusText(): String = entityData.get(DATA_STATUS_TEXT)
    
    fun setStatusColor(color: Int) {
        statusColor = color and 0xFFFFFF
        entityData.set(DATA_STATUS_COLOR, statusColor)
    }
    
    fun getStatusColor(): Int = entityData.get(DATA_STATUS_COLOR)
    
    fun setLightColor(color: Int) {
        lightColor = color and 0xFFFFFF
        entityData.set(DATA_LIGHT_COLOR, lightColor)
    }
    
    fun getLightColor(): Int = entityData.get(DATA_LIGHT_COLOR)
    
    // Energy
    fun getEnergy(): Double = currentEnergy
    fun getMaxEnergy(): Double = maxEnergy
    fun addEnergy(amount: Double): Double {
        val added = min(amount, maxEnergy - currentEnergy)
        currentEnergy += added
        return added
    }
    
    // Inventory
    override fun getContainerSize(): Int = inventory.slots
    override fun isEmpty(): Boolean = (0 until inventory.slots).all { inventory.getStackInSlot(it).isEmpty }
    override fun getItem(slot: Int): ItemStack = inventory.getStackInSlot(slot)
    override fun removeItem(slot: Int, amount: Int): ItemStack = inventory.extractItem(slot, amount, false)
    override fun removeItemNoUpdate(slot: Int): ItemStack {
        val stack = inventory.getStackInSlot(slot)
        inventory.setStackInSlot(slot, ItemStack.EMPTY)
        return stack
    }
    override fun setItem(slot: Int, stack: ItemStack) = inventory.setStackInSlot(slot, stack)
    override fun setChanged() {}
    override fun stillValid(player: Player): Boolean = isAlive && player.distanceToSqr(this) <= 64.0
    override fun clearContent() = (0 until inventory.slots).forEach { inventory.setStackInSlot(it, ItemStack.EMPTY) }
    
    fun getSelectedSlot(): Int = selectedSlot
    fun setSelectedSlot(slot: Int) { selectedSlot = slot.coerceIn(0, inventory.slots - 1) }
    
    // Interaction
    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
        if (level().isClientSide) return InteractionResult.SUCCESS
        return InteractionResult.SUCCESS
    }
    
    override fun actuallyHurt(level: ServerLevel, source: DamageSource, amount: Float) {
        if (source.entity is Player && !running) {
            dropAsItem()
            return
        }
        super.actuallyHurt(level, source, amount)
    }
    
    private fun dropAsItem() {
        if (!level().isClientSide && level() is ServerLevel) {
            val serverLevel = level() as ServerLevel
            val item = ModItems.DRONE_CASE_TIER1.get()
            val stack = ItemStack(item)
            spawnAtLocation(serverLevel, stack)
            for (i in 0 until inventory.slots) {
                val itemStack = inventory.getStackInSlot(i)
                if (!itemStack.isEmpty) spawnAtLocation(serverLevel, itemStack)
            }
            discard()
        }
    }
    
    // Serialization
    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        saveData(tag)
    }
    
    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        loadData(tag)
    }
    
    private fun saveData(tag: CompoundTag) {
        tag.putBoolean("running", running)
        tag.putInt("tier", tier)
        tag.putDouble("energy", currentEnergy)
        tag.putDouble("maxEnergy", maxEnergy)
        tag.putInt("selectedSlot", selectedSlot)
        tag.putString("statusText", statusText)
        tag.putInt("statusColor", statusColor)
        tag.putInt("lightColor", lightColor)
        ownerUUID?.let { tag.putUUID("owner", it) }
        tag.putString("ownerName", ownerName)
        
        val invTag = ListTag()
        for (i in 0 until inventory.slots) {
            val stack = inventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                val slotTag = CompoundTag()
                slotTag.putInt("slot", i)
                stack.save(registryAccess(), slotTag)
                invTag.add(slotTag)
            }
        }
        tag.put("inventory", invTag)
    }
    
    private fun loadData(tag: CompoundTag) {
        running = tag.getBoolean("running")
        tier = tag.getInt("tier").coerceAtLeast(1)
        currentEnergy = tag.getDouble("energy")
        maxEnergy = tag.getDouble("maxEnergy")
        selectedSlot = tag.getInt("selectedSlot")
        statusText = tag.getString("statusText")
        statusColor = tag.getInt("statusColor")
        lightColor = tag.getInt("lightColor")
        if (tag.hasUUID("owner")) ownerUUID = tag.getUUID("owner")
        ownerName = tag.getString("ownerName")
        
        val invTag = tag.getList("inventory", 10)
        for (i in 0 until invTag.size) {
            val slotTag = invTag.getCompound(i)
            val slot = slotTag.getInt("slot")
            inventory.setStackInSlot(slot, ItemStack.parseOptional(registryAccess(), slotTag))
        }
        
        entityData.set(DATA_RUNNING, running)
        entityData.set(DATA_STATUS_TEXT, statusText)
        entityData.set(DATA_STATUS_COLOR, statusColor)
        entityData.set(DATA_LIGHT_COLOR, lightColor)
        applyTierSettings()
    }
    
    private fun applyTierSettings() {
        when (tier) {
            1 -> { maxEnergy = 10000.0; maxVelocity = 4.0; acceleration = 1.0 }
            2 -> { maxEnergy = 25000.0; maxVelocity = 6.0; acceleration = 1.5 }
            else -> { maxEnergy = 50000.0; maxVelocity = 8.0; acceleration = 2.0 }
        }
        currentEnergy = currentEnergy.coerceAtMost(maxEnergy)
    }
    
    fun setOwner(player: Player) {
        ownerUUID = player.uuid
        ownerName = player.name.string
    }
}

class DroneFollowTargetGoal(private val drone: DroneEntity) : Goal() {
    override fun canUse(): Boolean = drone.isRunning()
    override fun tick() {}
}

/**
 * Robot entity implementation.
 * Robots are ground-based machines with larger inventory.
 */
class RobotEntity(
    entityType: EntityType<out RobotEntity>,
    level: Level
) : PathfinderMob(entityType, level), Container {
    
    // State
    private var running = false
    private var tier = 1
    private var maxEnergy = 50000.0
    private var currentEnergy = maxEnergy
    private var energyPerTick = 1.0
    
    // Movement
    private var moving = false
    private var targetPos: BlockPos? = null
    private var facing: Direction = Direction.NORTH
    
    // Inventory
    private val mainInventory = ItemStackHandler(16)
    private val toolInventory = ItemStackHandler(1)
    private var selectedSlot = 0
    
    // Owner
    private var ownerUUID: UUID? = null
    private var ownerName = ""
    private var lightColor = 0x66DD55
    
    companion object {
        private val DATA_RUNNING: EntityDataAccessor<Boolean> = SynchedEntityData.defineId(
            RobotEntity::class.java, EntityDataSerializers.BOOLEAN
        )
        private val DATA_FACING: EntityDataAccessor<Direction> = SynchedEntityData.defineId(
            RobotEntity::class.java, EntityDataSerializers.DIRECTION
        )
        private val DATA_LIGHT_COLOR: EntityDataAccessor<Int> = SynchedEntityData.defineId(
            RobotEntity::class.java, EntityDataSerializers.INT
        )
        private val DATA_MOVING: EntityDataAccessor<Boolean> = SynchedEntityData.defineId(
            RobotEntity::class.java, EntityDataSerializers.BOOLEAN
        )
        
        fun createAttributes(): AttributeSupplier.Builder = createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 2.0)
    }
    
    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(DATA_RUNNING, false)
        builder.define(DATA_FACING, Direction.NORTH)
        builder.define(DATA_LIGHT_COLOR, 0x66DD55)
        builder.define(DATA_MOVING, false)
    }
    
    override fun registerGoals() {
        goalSelector.addGoal(0, FloatGoal(this))
        goalSelector.addGoal(1, RobotMoveGoal(this))
    }
    
    override fun tick() {
        super.tick()
        
        if (!level().isClientSide) {
            if (running) {
                currentEnergy -= energyPerTick
                if (currentEnergy <= 0) {
                    currentEnergy = 0.0
                    stop()
                }
            }
            
            // Handle movement
            if (moving && targetPos != null) {
                val target = Vec3.atCenterOf(targetPos!!)
                val current = position()
                val distance = current.distanceTo(target)
                
                if (distance > 0.1) {
                    val direction = target.subtract(current).normalize()
                    val speed = 0.15
                    deltaMovement = direction.scale(speed)
                    move(MoverType.SELF, deltaMovement)
                } else {
                    setPos(target.x, target.y, target.z)
                    moving = false
                    targetPos = null
                    entityData.set(DATA_MOVING, false)
                }
            }
        }
    }
    
    // State Control
    fun start(): Boolean {
        if (running) return false
        if (currentEnergy <= 0) return false
        running = true
        entityData.set(DATA_RUNNING, true)
        return true
    }
    
    fun stop(): Boolean {
        if (!running) return false
        running = false
        entityData.set(DATA_RUNNING, false)
        moving = false
        targetPos = null
        return true
    }
    
    fun isRunning(): Boolean = running
    
    // Movement
    fun moveInDirection(direction: Direction): Boolean {
        if (!running) return false
        if (moving) return false
        
        val from = blockPosition()
        val to = from.relative(direction)
        
        if (!canMoveTo(to)) return false
        
        val cost = getMovementEnergyCost(direction)
        if (currentEnergy < cost) return false
        
        currentEnergy -= cost
        targetPos = to
        moving = true
        entityData.set(DATA_MOVING, true)
        return true
    }
    
    fun turn(clockwise: Boolean): Boolean {
        if (!running) return false
        facing = if (clockwise) facing.clockWise else facing.counterClockWise
        entityData.set(DATA_FACING, facing)
        return true
    }
    
    fun getFacing(): Direction = entityData.get(DATA_FACING)
    
    private fun canMoveTo(pos: BlockPos): Boolean {
        val state = level().getBlockState(pos)
        val below = level().getBlockState(pos.below())
        return state.isAir && !below.isAir
    }
    
    private fun getMovementEnergyCost(direction: Direction): Double = when (direction) {
        Direction.UP -> 15.0
        Direction.DOWN -> 5.0
        else -> 10.0
    }
    
    // Energy
    fun getEnergy(): Double = currentEnergy
    fun getMaxEnergy(): Double = maxEnergy
    fun addEnergy(amount: Double): Double {
        val added = min(amount, maxEnergy - currentEnergy)
        currentEnergy += added
        return added
    }
    
    // Inventory
    override fun getContainerSize(): Int = mainInventory.slots
    override fun isEmpty(): Boolean = (0 until mainInventory.slots).all { mainInventory.getStackInSlot(it).isEmpty }
    override fun getItem(slot: Int): ItemStack = mainInventory.getStackInSlot(slot)
    override fun removeItem(slot: Int, amount: Int): ItemStack = mainInventory.extractItem(slot, amount, false)
    override fun removeItemNoUpdate(slot: Int): ItemStack {
        val stack = mainInventory.getStackInSlot(slot)
        mainInventory.setStackInSlot(slot, ItemStack.EMPTY)
        return stack
    }
    override fun setItem(slot: Int, stack: ItemStack) = mainInventory.setStackInSlot(slot, stack)
    override fun setChanged() {}
    override fun stillValid(player: Player): Boolean = isAlive && player.distanceToSqr(this) <= 64.0
    override fun clearContent() = (0 until mainInventory.slots).forEach { mainInventory.setStackInSlot(it, ItemStack.EMPTY) }
    
    fun getSelectedSlot(): Int = selectedSlot
    fun setSelectedSlot(slot: Int) { selectedSlot = slot.coerceIn(0, mainInventory.slots - 1) }
    fun getToolSlot(): ItemStack = toolInventory.getStackInSlot(0)
    fun setToolSlot(stack: ItemStack) = toolInventory.setStackInSlot(0, stack)
    
    // Light
    fun setLightColor(color: Int) {
        lightColor = color and 0xFFFFFF
        entityData.set(DATA_LIGHT_COLOR, lightColor)
    }
    fun getLightColor(): Int = entityData.get(DATA_LIGHT_COLOR)
    
    // Interaction
    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
        if (level().isClientSide) return InteractionResult.SUCCESS
        return InteractionResult.SUCCESS
    }
    
    override fun actuallyHurt(level: ServerLevel, source: DamageSource, amount: Float) {
        if (source.entity is Player && !running) {
            dropAsItem()
            return
        }
        super.actuallyHurt(level, source, amount)
    }
    
    private fun dropAsItem() {
        if (!level().isClientSide && level() is ServerLevel) {
            val serverLevel = level() as ServerLevel
            val item = ModItems.ROBOT_CASE_TIER1.get()
            val stack = ItemStack(item)
            spawnAtLocation(serverLevel, stack)
            for (i in 0 until mainInventory.slots) {
                val itemStack = mainInventory.getStackInSlot(i)
                if (!itemStack.isEmpty) spawnAtLocation(serverLevel, itemStack)
            }
            discard()
        }
    }
    
    // Serialization
    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        saveData(tag)
    }
    
    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        loadData(tag)
    }
    
    private fun saveData(tag: CompoundTag) {
        tag.putBoolean("running", running)
        tag.putInt("tier", tier)
        tag.putDouble("energy", currentEnergy)
        tag.putDouble("maxEnergy", maxEnergy)
        tag.putInt("selectedSlot", selectedSlot)
        tag.putString("facing", facing.name)
        tag.putInt("lightColor", lightColor)
        ownerUUID?.let { tag.putUUID("owner", it) }
        tag.putString("ownerName", ownerName)
        
        val mainTag = ListTag()
        for (i in 0 until mainInventory.slots) {
            val stack = mainInventory.getStackInSlot(i)
            if (!stack.isEmpty) {
                val slotTag = CompoundTag()
                slotTag.putInt("slot", i)
                stack.save(registryAccess(), slotTag)
                mainTag.add(slotTag)
            }
        }
        tag.put("mainInventory", mainTag)
        
        val toolStack = toolInventory.getStackInSlot(0)
        if (!toolStack.isEmpty) {
            val toolTag = CompoundTag()
            toolStack.save(registryAccess(), toolTag)
            tag.put("toolSlot", toolTag)
        }
    }
    
    private fun loadData(tag: CompoundTag) {
        running = tag.getBoolean("running")
        tier = tag.getInt("tier").coerceAtLeast(1)
        currentEnergy = tag.getDouble("energy")
        maxEnergy = tag.getDouble("maxEnergy")
        selectedSlot = tag.getInt("selectedSlot")
        facing = try { Direction.valueOf(tag.getString("facing")) } catch (_: Exception) { Direction.NORTH }
        lightColor = tag.getInt("lightColor")
        if (tag.hasUUID("owner")) ownerUUID = tag.getUUID("owner")
        ownerName = tag.getString("ownerName")
        
        val mainTag = tag.getList("mainInventory", 10)
        for (i in 0 until mainTag.size) {
            val slotTag = mainTag.getCompound(i)
            val slot = slotTag.getInt("slot")
            mainInventory.setStackInSlot(slot, ItemStack.parseOptional(registryAccess(), slotTag))
        }
        
        if (tag.contains("toolSlot")) {
            toolInventory.setStackInSlot(0, ItemStack.parseOptional(registryAccess(), tag.getCompound("toolSlot")))
        }
        
        entityData.set(DATA_RUNNING, running)
        entityData.set(DATA_FACING, facing)
        entityData.set(DATA_LIGHT_COLOR, lightColor)
        applyTierSettings()
    }
    
    private fun applyTierSettings() {
        when (tier) {
            1 -> maxEnergy = 50000.0
            2 -> maxEnergy = 100000.0
            else -> maxEnergy = 200000.0
        }
        currentEnergy = currentEnergy.coerceAtMost(maxEnergy)
    }
    
    fun setOwner(player: Player) {
        ownerUUID = player.uuid
        ownerName = player.name.string
    }
}

class RobotMoveGoal(private val robot: RobotEntity) : Goal() {
    override fun canUse(): Boolean = robot.isRunning()
    override fun tick() {}
}
