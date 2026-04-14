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
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.common.NeoForge
import java.util.concurrent.ConcurrentHashMap

/**
 * Client-side sound management for looping sounds like computer running.
 * Mimics the original OpenComputers sound system.
 */
@OnlyIn(Dist.CLIENT)
object Sound {
    
    // Active looping sounds by position
    private val loopingSounds = ConcurrentHashMap<BlockPos, LoopingSound>()
    
    /**
     * Initialize the sound system - register tick handler
     */
    fun init() {
        NeoForge.EVENT_BUS.addListener(::onClientTick)
        OpenComputers.LOGGER.debug("Sound system initialized")
    }
    
    /**
     * Start a looping sound at the given position.
     */
    fun startLoop(pos: BlockPos, sound: SoundEvent, volume: Float = 0.5f) {
        if (loopingSounds.containsKey(pos)) return
        
        val mc = Minecraft.getInstance()
        if (mc.level == null) return  // Don't play sounds if no world
        
        val instance = LoopingSound(sound, pos, volume)
        loopingSounds[pos] = instance
        mc.soundManager.play(instance)
        OpenComputers.LOGGER.debug("Started sound at {}", pos)
    }
    
    /**
     * Stop a looping sound at the given position.
     */
    fun stopLoop(pos: BlockPos) {
        val sound = loopingSounds.remove(pos)
        if (sound != null) {
            sound.markStopped()
            OpenComputers.LOGGER.debug("Stopped sound at {}", pos)
        }
    }
    
    /**
     * Check if a sound is playing at position.
     */
    fun isPlaying(pos: BlockPos): Boolean = loopingSounds.containsKey(pos)
    
    /**
     * Stop all sounds (called on world unload).
     */
    fun stopAll() {
        for (pos in loopingSounds.keys.toList()) {
            stopLoop(pos)
        }
    }
    
    private fun onClientTick(event: ClientTickEvent.Post) {
        // Clean up stopped sounds
        val toRemove = loopingSounds.entries.filter { it.value.isStopped }.map { it.key }
        toRemove.forEach { loopingSounds.remove(it) }
    }
    
    /**
     * A looping sound instance that keeps playing until stopped.
     */
    private class LoopingSound(
        sound: SoundEvent,
        private val pos: BlockPos,
        private val baseVolume: Float
    ) : AbstractTickableSoundInstance(sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom()) {
        
        private var stopped = false
        
        init {
            this.x = pos.x + 0.5
            this.y = pos.y + 0.5
            this.z = pos.z + 0.5
            this.looping = true
            this.delay = 0
            this.volume = baseVolume
            this.relative = false
            this.attenuation = SoundInstance.Attenuation.LINEAR
        }
        
        override fun tick() {
            // Sound continues until stopped
        }
        
        fun markStopped() {
            stopped = true
        }
        
        override fun isStopped(): Boolean = stopped
    }
}
