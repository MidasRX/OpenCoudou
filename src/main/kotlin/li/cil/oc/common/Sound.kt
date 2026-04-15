package li.cil.oc.common

import li.cil.oc.OpenComputers
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.Level
import java.util.WeakHashMap

/**
 * Server-side one-shot sound player — port of original OpenComputers common/Sound.scala.
 * 
 * Plays a sound at a position with a 500ms cooldown per position+sound combo
 * to prevent sound spam (e.g. rapid HDD access).
 */
object Sound {
    // Cooldown map: BlockPos -> (soundName -> lastPlayTimeMs)
    private val globalTimeouts = WeakHashMap<BlockPos, MutableMap<String, Long>>()

    /**
     * Play a sound at the given position with 500ms cooldown.
     * Matches original: Sound.play(host, name)
     */
    fun play(level: Level, pos: BlockPos, name: String, volume: Float = 1f) {
        synchronized(this) {
            val hostTimeouts = globalTimeouts[pos]
            if (hostTimeouts != null) {
                val lastPlayed = hostTimeouts[name] ?: 0L
                if (lastPlayed > System.currentTimeMillis()) return // Still in cooldown
            }

            val soundLocation = ResourceLocation.fromNamespaceAndPath(OpenComputers.MOD_ID, name)
            val soundHolder = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT
                .get(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.SOUND_EVENT, soundLocation
                ))
            if (soundHolder.isPresent) {
                level.playSound(
                    null, // no specific player = all nearby players hear it
                    pos.x + 0.5, pos.y + 0.5, pos.z + 0.5,
                    soundHolder.get(),
                    SoundSource.BLOCKS,
                    volume,
                    1.0f
                )
            }

            globalTimeouts.getOrPut(pos) { mutableMapOf() }[name] =
                System.currentTimeMillis() + 500
        }
    }

    /**
     * Play floppy insert sound. Matches original: Sound.playDiskInsert(host)
     */
    fun playDiskInsert(level: Level, pos: BlockPos) {
        play(level, pos, "floppy_insert")
    }

    /**
     * Play floppy eject sound. Matches original: Sound.playDiskEject(host)
     */
    fun playDiskEject(level: Level, pos: BlockPos) {
        play(level, pos, "floppy_eject")
    }
}
