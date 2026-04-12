package li.cil.oc.common.blockentity

import li.cil.oc.common.init.ModBlockEntities
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class HologramBlockEntity(pos: BlockPos, state: BlockState, tier: Int = 1
) : BlockEntity(ModBlockEntities.HOLOGRAM.get(), pos, state) {
    var tier: Int = tier
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putInt("tier", tier)
    }
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        tier = tag.getInt("tier")
    }
    companion object {
        @JvmStatic
        fun tick(level: Level, pos: BlockPos, state: BlockState, blockEntity: HologramBlockEntity) {}
    }
}

class MicrocontrollerBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.MICROCONTROLLER.get(), pos, state) {
    var isPowered: Boolean = false
    var tier: Int = 1
    
    fun isRunning(): Boolean = isPowered
    fun start(): Boolean { isPowered = true; return true }
    fun stop() { isPowered = false }
    fun tick() {}
    
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putBoolean("powered", isPowered)
    }
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        isPowered = tag.getBoolean("powered")
    }
}

class MotionSensorBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.MOTION_SENSOR.get(), pos, state) {
    fun tick() {}
}

class NetSplitterBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.NET_SPLITTER.get(), pos, state) {
    fun tick() {}
    fun toggleSide(side: net.minecraft.core.Direction) {}
}

class PowerConverterBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.POWER_CONVERTER.get(), pos, state) {
    fun tick() {}
}

class PrinterBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.PRINTER.get(), pos, state) {
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
    }
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
    }
    companion object {
        @JvmStatic
        fun tick(level: Level, pos: BlockPos, state: BlockState, blockEntity: PrinterBlockEntity) {}
    }
}

class RaidBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.RAID.get(), pos, state) {
    var label: String = ""
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
        tag.putString("label", label)
    }
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
        label = tag.getString("label")
    }
}

class GeolyzerBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.GEOLYZER.get(), pos, state) {
    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)
    }
    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)
    }
}

class DiskDriveBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.DISK_DRIVE.get(), pos, state) {
    fun tick() {}
    fun getDisk(): net.minecraft.world.item.ItemStack = net.minecraft.world.item.ItemStack.EMPTY
    fun setDisk(stack: net.minecraft.world.item.ItemStack) {}
    fun canInsert(stack: net.minecraft.world.item.ItemStack): Boolean = true
    fun dropContents(level: Level, pos: BlockPos) {}
}

class DisassemblerBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.DISASSEMBLER.get(), pos, state) {
    fun tick() {}
}

class KeyboardBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.KEYBOARD.get(), pos, state) {
    fun keyDown(player: net.minecraft.world.entity.player.Player, charCode: Int, keyCode: Int) {}
    fun keyUp(player: net.minecraft.world.entity.player.Player, charCode: Int, keyCode: Int) {}
    fun clipboard(player: net.minecraft.world.entity.player.Player, text: String) {}
    fun tick() {}
}

class PowerDistributorBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.POWER_DISTRIBUTOR.get(), pos, state) {
    fun tick() {}
}

class PrintBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.PRINT.get(), pos, state) {
    var isActive: Boolean = false
    var lightLevel: Int = 0
    var redstoneLevel: Int = 0
    var isButton: Boolean = false
    var redstoneActivated: Boolean = false
    
    fun tick() {}
    fun getVoxelShape(): net.minecraft.world.phys.shapes.VoxelShape = 
        net.minecraft.world.phys.shapes.Shapes.block()
    fun toggle() { isActive = !isActive }
}

class RedstoneIOBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.REDSTONE_IO.get(), pos, state) {
    fun tick() {}
    fun getOutput(side: net.minecraft.core.Direction): Int = 0
    fun getComparatorOutput(): Int = 0
}

class RobotProxyBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.ROBOT_PROXY.get(), pos, state) {
    fun tick() {}
}

class ServerRackBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.SERVER_RACK.get(), pos, state) {
    fun tick() {}
}

class AdapterBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.ADAPTER.get(), pos, state) {
    fun scanNeighbors() {}
}

class ChargerBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.CHARGER.get(), pos, state) {
    fun tick() {}
}

class AssemblerBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.ASSEMBLER.get(), pos, state) {
    fun tick() {}
    fun startAssembly(): Boolean = false
}

class CapacitorBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.CAPACITOR.get(), pos, state) {
    fun tick() {}
}

class CarpetedCapacitorBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.CARPETED_CAPACITOR.get(), pos, state) {
    fun tick() {}
}

class AccessPointBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.ACCESS_POINT.get(), pos, state) {
    fun tick() {}
}

class CableBlockEntity(pos: BlockPos, state: BlockState
) : BlockEntity(ModBlockEntities.CABLE.get(), pos, state) {
    fun updateConnections() {}
}
