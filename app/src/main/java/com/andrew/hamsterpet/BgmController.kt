package com.andrew.hamsterpet

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer

class BgmController(
    context: Context,
    private val repository: PetStateRepository,
) : AudioManager.OnAudioFocusChangeListener {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(audioAttributes)
        .setAcceptsDelayedFocusGain(true)
        .setOnAudioFocusChangeListener(this)
        .build()
    private var player: MediaPlayer? = null
    private var currentTrack: BgmTrack? = null
    private var removeListener: (() -> Unit)? = null
    private var started = false
    private var hasFocus = false
    private var released = false

    fun start() {
        if (started || released) return
        started = true
        removeListener = repository.addListener(::sync)
        sync(repository.current())
    }

    private fun sync(state: PetState) {
        if (released) return
        if (!state.musicEnabled || state.bgmVolume == 0) {
            player?.pause()
            abandonFocus()
            return
        }
        val track = BgmTrack.fromIndex(state.bgmTrackIndex)
        if (track != currentTrack || player == null) {
            replacePlayer(track)
        }
        val gain = BgmTrack.volumeGain(state.bgmVolume)
        player?.setVolume(gain, gain)
        if (requestFocus()) player?.start()
    }

    private fun replacePlayer(track: BgmTrack) {
        player?.release()
        player = runCatching {
            appContext.resources.openRawResourceFd(track.resourceId).use { source ->
                MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(source.fileDescriptor, source.startOffset, source.length)
                    isLooping = true
                    prepare()
                }
            }
        }.getOrNull()
        currentTrack = track
    }

    private fun requestFocus(): Boolean {
        if (hasFocus) return true
        val result = audioManager.requestAudioFocus(focusRequest)
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasFocus
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (released) return
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                val state = repository.current()
                val gain = BgmTrack.volumeGain(state.bgmVolume)
                player?.setVolume(gain, gain)
                if (state.musicEnabled) player?.start()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                val gain = BgmTrack.volumeGain(repository.current().bgmVolume) * .2f
                player?.setVolume(gain, gain)
            }
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            -> {
                hasFocus = false
                player?.pause()
            }
        }
    }

    private fun abandonFocus() {
        if (!hasFocus) return
        audioManager.abandonAudioFocusRequest(focusRequest)
        hasFocus = false
    }

    fun release() {
        if (released) return
        released = true
        removeListener?.invoke()
        removeListener = null
        player?.let { activePlayer ->
            runCatching { activePlayer.stop() }
            activePlayer.release()
        }
        player = null
        currentTrack = null
        abandonFocus()
    }
}
