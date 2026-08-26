package com.andrew.hamsterpet

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

enum class PetSound(val resourceId: Int) {
    EAT(R.raw.sfx_eat),
    PLAY(R.raw.sfx_play),
    PAT(R.raw.sfx_pat),
    SLEEP(R.raw.sfx_sleep),
    BUILD(R.raw.sfx_build),
}

data class SoundPlayback(val sampleId: Int, val volume: Float)

class SoundLoadQueue {
    private data class Pending(val sound: PetSound, val volume: Float)

    private val sampleIds = mutableMapOf<PetSound, Int>()
    private val loadedSamples = mutableSetOf<Int>()
    private val settledSamples = mutableSetOf<Int>()
    private val pending = mutableListOf<Pending>()

    @Synchronized
    fun register(sound: PetSound, sampleId: Int) {
        sampleIds[sound] = sampleId
    }

    @Synchronized
    fun request(sound: PetSound, volume: Float): List<SoundPlayback> {
        val sampleId = sampleIds[sound] ?: return emptyList()
        if (sampleId in loadedSamples) return listOf(SoundPlayback(sampleId, volume))
        if (sampleId !in settledSamples) pending += Pending(sound, volume)
        return emptyList()
    }

    @Synchronized
    fun loaded(sampleId: Int, succeeded: Boolean): List<SoundPlayback> {
        if (!settledSamples.add(sampleId)) return emptyList()
        if (succeeded) loadedSamples += sampleId
        val requests = pending.filter { sampleIds[it.sound] == sampleId }
        pending.removeAll(requests.toSet())
        return if (succeeded) requests.map { SoundPlayback(sampleId, it.volume) } else emptyList()
    }
}

object PetAudio {
    private var pool: SoundPool? = null
    private val loadQueue = SoundLoadQueue()

    @Synchronized
    fun initialize(context: Context) {
        if (pool != null) return
        val created = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
        pool = created
        created.setOnLoadCompleteListener { soundPool, sampleId, status ->
            loadQueue.loaded(sampleId, status == 0).forEach { playback ->
                soundPool.play(playback.sampleId, playback.volume, playback.volume, 1, 0, 1f)
            }
        }
        PetSound.entries.forEach { sound ->
            loadQueue.register(sound, created.load(context.applicationContext, sound.resourceId, 1))
        }
    }

    fun play(context: Context, sound: PetSound, volume: Float = 0.72f) {
        initialize(context)
        val activePool = pool ?: return
        loadQueue.request(sound, volume).forEach { playback ->
            activePool.play(playback.sampleId, playback.volume, playback.volume, 1, 0, 1f)
        }
    }
}
