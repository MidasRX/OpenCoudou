package li.cil.oc.common.block

import li.cil.oc.common.blockentity.AssemblerBlockEntity
import li.cil.oc.common.blockentity.CapacitorBlockEntity
import li.cil.oc.common.blockentity.ChargerBlockEntity
import li.cil.oc.common.blockentity.DisassemblerBlockEntity
import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

/**
 * Charger block - charges robots, tablets, and other devices.
 */
class ChargerBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return ChargerBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        if (type != ModBlockEntities.CHARGER.get()) return null
        
        return BlockEntityTicker { lvl, pos, st, blockEntity ->
            if (blockEntity is ChargerBlockEntity) {
                blockEntity.tick()
            }
        }
    }
}

/**
 * Assembler block - assembles robots, tablets, and drones from components.
 */
class AssemblerBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return AssemblerBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        if (type != ModBlockEntities.ASSEMBLER.get()) return null
        
        return BlockEntityTicker { lvl, pos, st, blockEntity ->
            if (blockEntity is AssemblerBlockEntity) {
                blockEntity.tick()
            }
        }
    }
}

/**
 * Disassembler block - breaks down robots and other crafted items into components.
 */
class DisassemblerBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return DisassemblerBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        if (type != ModBlockEntities.DISASSEMBLER.get()) return null
        
        return BlockEntityTicker { lvl, pos, st, blockEntity ->
            if (blockEntity is DisassemblerBlockEntity) {
                blockEntity.tick()
            }
        }
    }
}

/**
 * Capacitor block - stores energy for the network.
 */
class CapacitorBlock(properties: Properties) : Block(properties), EntityBlock {
    
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return CapacitorBlockEntity(pos, state)
    }
    
    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) return null
        if (type != ModBlockEntities.CAPACITOR.get()) return null
        
        return BlockEntityTicker { lvl, pos, st, blockEntity ->
            if (blockEntity is CapacitorBlockEntity) {
                blockEntity.tick()
            }
        }
    }
}
