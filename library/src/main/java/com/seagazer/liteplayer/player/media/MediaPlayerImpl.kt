package com.seagazer.liteplayer.player.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.SurfaceHolder
import androidx.lifecycle.MutableLiveData
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.PlayerState
import com.seagazer.liteplayer.event.PlayerStateEvent
import com.seagazer.liteplayer.helper.MediaLogger
import com.seagazer.liteplayer.player.IPlayer

/**
 * Android media decoder.
 *
 * Author: Seagazer
 * Date: 2020/7/9
 */
class MediaPlayerImpl constructor(val context: Context) : IPlayer {
    private var player: MediaPlayer? = null
    private var currentState = PlayerState.STATE_NOT_INITIALIZED
    private var currentBufferedPercentage = 0
    private var videoWidth = 0
    private var videoHeight = 0
    private var isPendingSeek = false
    private var liveData: MutableLiveData<PlayerStateEvent>? = null
    private var startPosition = 0L
    private var dataSource: DataSource? = null
    private var surface: Surface? = null
    private var asyncToStart = false
    private var isBuffering = true

    private val preparedListener = MediaPlayer.OnPreparedListener { mp ->
        MediaLogger.d("prepared")
        setPlayerState(PlayerState.STATE_PREPARED)
        liveData?.value = PlayerStateEvent(PlayerState.STATE_PREPARED)
        if (asyncToStart && player != null) {
            MediaLogger.d("start play")
            player!!.start()
            if (startPosition > 0) {
                player!!.seekTo(startPosition.toInt())
            }
            setPlayerState(PlayerState.STATE_STARTED)
            liveData?.value = PlayerStateEvent(PlayerState.STATE_STARTED)
        }
    }

    private val infoListener = MediaPlayer.OnInfoListener { mp, what, extra ->
        when (what) {
            MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                MediaLogger.d("render first frame  : $currentState")
                liveData?.value = PlayerStateEvent(PlayerState.STATE_RENDERED_FIRST_FRAME)
            }
            MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                isBuffering = true
                MediaLogger.d("-> loading start: $currentState")
                liveData?.value = PlayerStateEvent(PlayerState.STATE_BUFFER_START)
            }
            MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                isBuffering = false
                MediaLogger.d("-> loading end: $currentState")
                liveData?.value = PlayerStateEvent(PlayerState.STATE_BUFFER_END)
            }
        }
        true
    }

    private val errorListener = MediaPlayer.OnErrorListener { _, what, _ ->
        MediaLogger.e("play error = $what")
        setPlayerState(PlayerState.STATE_ERROR)
        liveData?.value = PlayerStateEvent(PlayerState.STATE_ERROR).apply {
            errorCode = what
        }
        true
    }

    private val bufferUpdateListener = MediaPlayer.OnBufferingUpdateListener { mp, percent ->
        currentBufferedPercentage = percent
        MediaLogger.d("loading ...: $currentState")
        liveData?.value = PlayerStateEvent(PlayerState.STATE_BUFFER_UPDATE).apply {
            bufferedPercentage = percent
        }
        if (isPendingSeek) {
            liveData?.value = PlayerStateEvent(PlayerState.STATE_SEEK_COMPLETED)
            isPendingSeek = false
        }
    }

    private val videoSizeChangedListener = MediaPlayer.OnVideoSizeChangedListener { mp, width, height ->
        this.videoWidth = width
        this.videoHeight = height
        MediaLogger.d("video size: $width x $height  : $currentState")
        liveData?.value = PlayerStateEvent(PlayerState.STATE_VIDEO_SIZE_CHANGED).apply {
            videoWidth = width
            videoHeight = height
        }
    }

    private val completionListener = MediaPlayer.OnCompletionListener {
        MediaLogger.d("-> play completed: $currentState")
        setPlayerState(PlayerState.STATE_PLAYBACK_COMPLETE)
        liveData?.value = PlayerStateEvent(PlayerState.STATE_PLAYBACK_COMPLETE)
    }

    private var audioAttributes: AudioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build()

    override fun registerStateObserver(liveData: MutableLiveData<PlayerStateEvent>) {
        if (this.liveData != liveData) {
            this.liveData = liveData
        }
    }

    override fun getPlayer() = this

    override fun setSurfaceHolder(surfaceHolder: SurfaceHolder) {
        this.surface = surfaceHolder.surface
        player?.setSurface(surfaceHolder.surface)
    }

    override fun setSurface(surface: Surface) {
        this.surface = surface
        player?.setSurface(surface)
    }

    override fun supportSoftwareDecode(softwareDecode: Boolean) {
        MediaLogger.w("Not support!")
    }

    override fun setDataSource(source: DataSource) {
        MediaLogger.d("-->$source")
        this.dataSource = source
        openVideo()
    }

    private fun openVideo() {
        destroy()
        try {
            currentBufferedPercentage = 0
            player = MediaPlayer()
            player!!.setAudioAttributes(audioAttributes)
            player!!.setOnPreparedListener(preparedListener)
            player!!.setOnVideoSizeChangedListener(videoSizeChangedListener)
            player!!.setOnCompletionListener(completionListener)
            player!!.setOnBufferingUpdateListener(bufferUpdateListener)
            player!!.setOnErrorListener(errorListener)
            player!!.setOnInfoListener(infoListener)
            player!!.setDataSource(context, Uri.parse(dataSource!!.mediaUrl))
            this.surface?.run {
                player!!.setSurface(surface)
            }
            MediaLogger.d("-->prepareAsync")
            player!!.prepareAsync()
            if (currentState == PlayerState.STATE_NOT_INITIALIZED) {
                setPlayerState(PlayerState.STATE_INITIALIZED)
                liveData?.value = PlayerStateEvent(PlayerState.STATE_INITIALIZED)
            }
        } catch (ex: Exception) {
            MediaLogger.w("Unable to open content: $dataSource, $ex")
            setPlayerState(PlayerState.STATE_ERROR)
            errorListener.onError(player, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0)
        }
    }

    override fun start() {
        // first setup player type will get audio focus, so will call this method before setDataSource
        if (this.dataSource == null) {
            MediaLogger.w("DataSource not found, can not start player")
            return
        }
        if (isInPlaybackState()) {
            if (currentState == PlayerState.STATE_PREPARED) {
                MediaLogger.d("start play")
                player?.run {
                    start()
                    asyncToStart = false
                    if (startPosition > 0) {
                        seekTo(startPosition.toInt())
                    }
                    setPlayerState(PlayerState.STATE_STARTED)
                    liveData?.value = PlayerStateEvent(PlayerState.STATE_STARTED)
                }
            } else {
                asyncToStart = true
            }
        }
    }

    override fun start(startPosition: Long) {
        MediaLogger.d("-->")
        this.startPosition = startPosition
        start()
    }

    override fun pause(fromUser: Boolean) {
        asyncToStart = false
        if (isInPlaybackState()) {
            if (player != null && player!!.isPlaying) {
                MediaLogger.d("pause play")
                player?.pause()
                setPlayerState(PlayerState.STATE_PAUSED)
                liveData?.value = PlayerStateEvent(PlayerState.STATE_PAUSED)
            }
        }
    }

    override fun resume() {
        if (isInPlaybackState() && getPlayerState() == PlayerState.STATE_PAUSED) {
            MediaLogger.d("resume play")
            player?.start()
            setPlayerState(PlayerState.STATE_STARTED)
            liveData?.value = PlayerStateEvent(PlayerState.STATE_STARTED)
        }
    }

    private fun isInPlaybackState(): Boolean {
        val state = getPlayerState()
        return state != PlayerState.STATE_ERROR &&
                state != PlayerState.STATE_NOT_INITIALIZED && state != PlayerState.STATE_STOPPED
    }

    override fun seekTo(position: Long) {
        MediaLogger.d("seek to: $position")
        isPendingSeek = true
        player?.seekTo(position.toInt())
        liveData?.value = PlayerStateEvent(PlayerState.STATE_SEEK_START)
    }

    override fun stop() {
        MediaLogger.d("stop play")
        asyncToStart = false
        isBuffering = false
        player?.stop()
        setPlayerState(PlayerState.STATE_STOPPED)
        liveData?.value = PlayerStateEvent(PlayerState.STATE_STOPPED)
    }

    override fun reset() {
        MediaLogger.d("reset play")
        asyncToStart = false
        isBuffering = false
        player?.reset()
        player?.run {
            setOnPreparedListener(null)
            setOnInfoListener(null)
            setOnErrorListener(null)
            setOnBufferingUpdateListener(null)
            setOnVideoSizeChangedListener(null)
            setOnCompletionListener(null)
            setOnInfoListener(null)
            release()
        }
        setPlayerState(PlayerState.STATE_STOPPED)
        liveData?.value = PlayerStateEvent(PlayerState.STATE_STOPPED)
    }

    override fun destroy() {
        MediaLogger.d("destroy player")
        asyncToStart = false
        isBuffering = false
        reset()
        player = null
        setPlayerState(PlayerState.STATE_NOT_INITIALIZED)
    }

    override fun getVideoWidth(): Int = videoWidth

    override fun getVideoHeight(): Int = videoHeight

    override fun getDuration() =
        if (player == null) {
            0
        } else {
            if (currentState == PlayerState.STATE_STARTED || currentState == PlayerState.STATE_PREPARED
                || currentState == PlayerState.STATE_PAUSED
            ) {
                player!!.duration.toLong()
            } else {
                0
            }
        }


    override fun isPlaying() = if (player == null) false else player!!.isPlaying

    override fun getBufferedPercentage() = if (player == null) 0 else currentBufferedPercentage

    override fun getCurrentPosition() = if (player == null) 0 else player!!.currentPosition.toLong()

    override fun setPlaySpeed(speed: Float) {
        // not support
        MediaLogger.w("MediaPlayer not support speed play")
    }

    override fun setVolume(volume: Int) {
        // do nothing, audio manager will handle it always
    }

    override fun setPlayerState(state: PlayerState) {
        currentState = state
    }

    override fun getPlayerState(): PlayerState = currentState


}