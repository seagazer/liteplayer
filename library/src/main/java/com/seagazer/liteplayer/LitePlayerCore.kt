package com.seagazer.liteplayer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder
import androidx.lifecycle.MutableLiveData
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.PlayerState
import com.seagazer.liteplayer.event.PlayerStateEvent
import com.seagazer.liteplayer.helper.MediaLogger
import com.seagazer.liteplayer.player.IPlayer

/**
 *  Core player manager to do play logic.
 *
 * Author: Seagazer
 * Date: 2020/6/19
 */
class LitePlayerCore constructor(val context: Context) : IPlayer {

    private var currentVolume: Int = 0
    private var maxVolume: Int = 0
    private var innerPlayer: IPlayer? = null
    private var appContext: Context = context.applicationContext
    private var audioManager: AudioManager
    private var audioAttributes: AudioAttributes
    private var audioFocusRequest: AudioFocusRequest? = null
    private var shouldPlayWhenReady = false

    init {
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
    }

    fun setupPlayer(player: IPlayer) {
        innerPlayer = player
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (shouldPlayWhenReady || getPlayerState() == PlayerState.STATE_PREPARED) {
                    // refresh current volume again because maybe other app will change the volume of system
                    val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    if (currentVolume != volume) {
                        setVolume(volume)
                    }
                }
                shouldPlayWhenReady = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (getPlayerState() == PlayerState.STATE_STARTED) {
                    setVolume((maxVolume * 0.1f).toInt())
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                shouldPlayWhenReady = getPlayerState() == PlayerState.STATE_STARTED
                pause(false)
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                abandonAudioFocus()
                pause(false)
            }
        }
    }

    private fun requestAudioFocus() {
        val result: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener(audioFocusListener)
                    .build()
            }
            result = audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            result = audioManager.requestAudioFocus(
                audioFocusListener,
                audioAttributes.contentType,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        // Call the listener whenever focus is granted - even the first time!
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            shouldPlayWhenReady = true
            audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        } else {
            MediaLogger.w("Playback not started: Audio focus request denied")
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener(audioFocusListener)
                    .build()
            }
            audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
        } else {
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }

    override fun registerStateObserver(liveData: MutableLiveData<PlayerStateEvent>) {
        innerPlayer?.registerStateObserver(liveData)
    }

    override fun getPlayer() = innerPlayer

    override fun setDataSource(source: DataSource) {
        innerPlayer?.setDataSource(source)
    }

    override fun start() {
        innerPlayer?.start()
        requestAudioFocus()
    }

    override fun start(startPosition: Long) {
        innerPlayer?.start(startPosition)
        requestAudioFocus()
    }

    override fun pause(fromUser: Boolean) {
        innerPlayer?.pause(fromUser)
    }

    override fun resume() {
        innerPlayer?.resume()
        requestAudioFocus()
    }

    override fun seekTo(position: Long) {
        innerPlayer?.seekTo(position)
    }

    override fun stop() {
        innerPlayer?.stop()
    }

    override fun reset() {
        innerPlayer?.reset()
    }

    override fun destroy() {
        innerPlayer?.destroy()
        abandonAudioFocus()
    }

    override fun setSurfaceHolder(surfaceHolder: SurfaceHolder) {
        innerPlayer?.setSurfaceHolder(surfaceHolder)
    }

    override fun setSurface(surface: Surface) {
        innerPlayer?.setSurface(surface)
    }

    override fun getVideoWidth() = if (innerPlayer != null) innerPlayer!!.getVideoWidth() else 0

    override fun getVideoHeight() = if (innerPlayer != null) innerPlayer!!.getVideoHeight() else 0

    override fun getDuration() = if (innerPlayer != null) innerPlayer!!.getDuration() else 0

    override fun isPlaying() = if (innerPlayer != null) innerPlayer!!.isPlaying() else false

    override fun getBufferedPercentage() = if (innerPlayer != null) innerPlayer!!.getBufferedPercentage() else 0

    override fun getCurrentPosition() = if (innerPlayer != null) innerPlayer!!.getCurrentPosition() else 0

    override fun setPlaySpeed(speed: Float) {
        innerPlayer?.setPlaySpeed(speed)
    }

    override fun setVolume(volume: Int) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
        currentVolume = volume
    }

    override fun setPlayerState(state: PlayerState) {
        innerPlayer?.setPlayerState(state)
    }

    override fun getPlayerState() = if (innerPlayer != null) innerPlayer!!.getPlayerState() else PlayerState.STATE_NOT_INITIALIZED
}