package li.cil.oc.client

import li.cil.oc.OpenComputers
import li.cil.oc.common.init.ModSoundEvents
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap

/**
 * Client-side sound management — port of original OpenComputers client/Sound.scala.
 * 
 * Features matching the original:
 * - Command queue with delayed start (like original's PriorityQueue[Command])
 * - Volume tracking that responds to MC sound settings
 * - Pause-aware (volume drops to 0 when game is paused on integrated server)
 * - Per-BlockPos looping sound instances
 * - World unload cleanup
 */
@OnlyIn(Dist.CLIENT)
object Sound {

    // Active looping sounds keyed by position (original uses TileEntity, we use BlockPos)
    private val sources = ConcurrentHashMap<BlockPos, LoopingSound>()

    // Command queue for delayed sound operations (like original's mutable.PriorityQueue)
    private val commandQueue = PriorityQueue<SoundCommand>(compareBy { it.whenMs })

    // Track last known volume to detect changes
    private var lastVolume = -1f

    /**
     * Start a looping sound at the given position, with optional delay in ms.
     * Matches original: Sound.startLoop(tileEntity, name, volume, delay)
     */
    fun startLoop(pos: BlockPos, sound: SoundEvent, volume: Float = 1f, delay: Long = 0) {
        synchronized(commandQueue) {
            commandQueue.add(StartCommand(System.currentTimeMillis() + delay, pos, sound, volume))
        }
    }

    /**
     * Stop a looping sound at the given position.
     * Matches original: Sound.stopLoop(tileEntity)
     */
    fun stopLoop(pos: BlockPos) {
        synchronized(commandQueue) {
            commandQueue.add(StopCommand(pos))
        }
    }

    /**
     * Check if a sound is playing at position.
     */
    fun isPlaying(pos: BlockPos): Boolean = sources.containsKey(pos)

    /**
     * Stop all sounds — called on world unload.
     * Matches original: onWorldUnload
     */
    fun stopAll() {
        synchronized(commandQueue) { commandQueue.clear() }
        for ((_, sound) in sources) {
            sound.markDone()
        }
        sources.clear()
    }

    /**
     * Must be called every client tick.
     * Handles volume updates, pause awareness, and command queue processing.
     */
    fun tick() {
        val mc = Minecraft.getInstance() ?: return
        if (mc.level == null) return

        // Update volume if MC settings changed or game paused/unpaused
        val currentVolume = if (isGamePaused()) 0f
            else mc.options.getSoundSourceVolume(SoundSource.BLOCKS)
        if (currentVolume != lastVolume) {
            lastVolume = currentVolume
            for ((_, sound) in sources) {
                sound.updateVolume(currentVolume)
            }
        }

        // Process command queue
        processQueue()

        // Clean up stopped sounds
        val stopped = sources.entries.filter { it.value.isDone() }
        stopped.forEach { sources.remove(it.key) }
    }

    private fun isGamePaused(): Boolean {
        val mc = Minecraft.getInstance()
        // On integrated server, respect pause state (like original)
        return mc.hasSingleplayerServer() && mc.isPaused
    }

    private fun processQueue() {
        val now = System.currentTimeMillis()
        synchronized(commandQueue) {
            while (commandQueue.isNotEmpty() && commandQueue.peek().whenMs <= now) {
                val cmd = commandQueue.poll() ?: break
                try {
                    cmd.execute()
                } catch (t: Throwable) {
                    OpenComputers.LOGGER.warn("Error processing sound command", t)
                }
            }
        }
    }

    // ========== Command classes (matching original's Command hierarchy) ==========

    private sealed class SoundCommand(val whenMs: Long) {
        abstract fun execute()
    }

    private class StartCommand(
        whenMs: Long,
        val pos: BlockPos,
        val sound: SoundEvent,
        val baseVolume: Float
    ) : SoundCommand(whenMs) {
        override fun execute() {
            if (sources.containsKey(pos)) {
                OpenComputers.LOGGER.info("Sound: skipping start at $pos (already playing)")
                return
            }
            val mc = Minecraft.getInstance()
            if (mc.level == null) return

            val volume = if (lastVolume >= 0) lastVolume else
                mc.options.getSoundSourceVolume(SoundSource.BLOCKS)
            val instance = LoopingSound(sound, pos, baseVolume, volume)
            sources[pos] = instance
            mc.soundManager.play(instance)
            OpenComputers.LOGGER.info("Sound: started loop at $pos (vol=${baseVolume * volume})")
        }
    }

    private class StopCommand(val pos: BlockPos) : SoundCommand(System.currentTimeMillis() + 1) {
        override fun execute() {
            val sound = sources.remove(pos)
            sound?.markDone()
            // Remove all pending commands for this position (like original)
            synchronized(commandQueue) {
                commandQueue.removeIf { cmd ->
                    when (cmd) {
                        is StartCommand -> cmd.pos == pos
                        is StopCommand -> cmd.pos == pos
                    }
                }
            }
        }
    }

    // ========== Looping sound instance ==========

    private class LoopingSound(
        sound: SoundEvent,
        private val pos: BlockPos,
        private val baseVolume: Float,
        currentBlockVolume: Float
    ) : AbstractTickableSoundInstance(sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom()) {

        @Volatile
        private var stopped = false

        init {
            this.x = pos.x + 0.5
            this.y = pos.y + 0.5
            this.z = pos.z + 0.5
            this.looping = true
            this.delay = 0
            this.volume = baseVolume * currentBlockVolume
            this.relative = false
            this.attenuation = SoundInstance.Attenuation.LINEAR
        }

        override fun tick() {
            // Sound continues until stopped — matching original's PseudoLoopingStream
        }

        fun updateVolume(blockCategoryVolume: Float) {
            this.volume = baseVolume * blockCategoryVolume
        }

        fun markDone() {
            stopped = true
        }

        fun isDone(): Boolean = stopped

        override fun isStopped(): Boolean = stopped
    }
}
