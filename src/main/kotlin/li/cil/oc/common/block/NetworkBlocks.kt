package li.cil.oc.common.block

import li.cil.oc.common.blockentity.RaidBlockEntity
import li.cil.oc.common.blockentity.RelayBlockEntity
import li.cil.oc.common.blockentity.AccessPointBlockEntity
import li.cil.oc.common.blockentity.PowerConverterBlockEntity
import li.cil.oc.common.blockentity.MotionSensorBlockEntity
import li.cil.oc.common.blockentity.TransposerBlockEntity
import li.cil.oc.common.blockentity.WaypointBlockEntity
import li.cil.oc.common.blockentity.NetSplitterBlockEntity
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

/**
 * RAID block - stores multiple hard drives for combined filesystem.
 */
class RaidBlock(properties: Properties) : Block(properties), EntityBlock {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return RaidBlockEntity(pos, state)
    }
}

/**
 * Relay block - bridges networks while maintaining isolation.
 */
class RelayBlock(properties: Properties) : Block(properties), EntityBlock {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return RelayBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.RELAY.get()) {
            BlockEntityTicker { lvl, pos, st, be -> (be as RelayBlockEntity).tick() }
        } else null
    }
}

/**
 * Access Point block - wireless network access.
 */
class AccessPointBlock(properties: Properties) : Block(properties), EntityBlock {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return AccessPointBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.ACCESS_POINT.get()) {
            BlockEntityTicker { lvl, pos, st, be -> (be as AccessPointBlockEntity).tick() }
        } else null
    }
}

/**
 * Power Converter block - converts FE to OC energy.
 */
class PowerConverterBlock(properties: Properties) : Block(properties), EntityBlock {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return PowerConverterBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.POWER_CONVERTER.get()) {
            BlockEntityTicker { lvl, pos, st, be -> (be as PowerConverterBlockEntity).tick() }
        } else null
    }
}

/**
 * Motion Sensor block - detects entity movement.
 */
class MotionSensorBlock(properties: Properties) : Block(properties), EntityBlock {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return MotionSensorBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.MOTION_SENSOR.get()) {
            BlockEntityTicker { lvl, pos, st, be -> (be as MotionSensorBlockEntity).tick() }
        } else null
    }
}

/**
 * Transposer block - moves items/fluids between containers.
 */
class TransposerBlock(properties: Properties) : Block(properties), EntityBlock {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return TransposerBlockEntity(pos, state)
    }
}

/**
 * Waypoint block - navigation reference point.
 */
class WaypointBlock(properties: Properties) : Block(properties), EntityBlock {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return WaypointBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.WAYPOINT.get()) {
            BlockEntityTicker { lvl, pos, st, be -> (be as WaypointBlockEntity).tick() }
        } else null
    }
}

/**
 * Net Splitter block - dynamically connects/disconnects network sides.
 */
class NetSplitterBlock(properties: Properties) : Block(properties), EntityBlock {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return NetSplitterBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (type == ModBlockEntities.NET_SPLITTER.get()) {
            BlockEntityTicker { lvl, pos, st, be -> (be as NetSplitterBlockEntity).tick() }
        } else null
    }
}
