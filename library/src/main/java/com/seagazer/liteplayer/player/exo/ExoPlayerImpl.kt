package com.seagazer.liteplayer.player.exo

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.Surface
import android.view.SurfaceHolder
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import com.seagazer.liteplayer.BuildConfig
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.PlayerState
import com.seagazer.liteplayer.helper.MediaLogger
import com.seagazer.liteplayer.event.PlayerStateEvent
import com.seagazer.liteplayer.player.IPlayerCore
import com.seagazer.liteplayer.player.IPlayer

/**
 *
 * Author: Seagazer
 * Date: 2020/6/19
 */
class ExoPlayerImpl constructor(val context: Context) : IPlayer {
    private val appContext = context.applicationContext
    private lateinit var player: SimpleExoPlayer
    private var dataSourceFactory: DefaultDataSourceFactory
    private var mediaSourceFactory: ProgressiveMediaSource.Factory
    private var currentState = PlayerState.STATE_NOT_INITIALIZED
    private var videoWidth = 0
    private var videoHeight = 0
    private var startPosition = 0L
    private var isPreparing = true
    private var isBuffering = true
    private var isPendingSeek = false
    private var liveData: MutableLiveData<PlayerStateEvent>? = null

    private val videoListener = object : VideoListener {
        override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
            this@ExoPlayerImpl.videoWidth = width
            this@ExoPlayerImpl.videoHeight = height
            MediaLogger.d("视频尺寸: $width x $height  : $currentState")
            liveData?.value = PlayerStateEvent(PlayerState.STATE_VIDEO_SIZE_CHANGED).apply {
                videoWidth = width
                videoHeight = height
            }
        }

        override fun onRenderedFirstFrame() {
            MediaLogger.d("首帧渲染  : $currentState")
            liveData?.value = PlayerStateEvent(PlayerState.STATE_RENDERED_FIRST_FRAME)
        }

        override fun onSurfaceSizeChanged(width: Int, height: Int) {
            MediaLogger.d("surface改变: $width x $height  : $currentState")
            liveData?.value = PlayerStateEvent(PlayerState.STATE_SURFACE_SIZE_CHANGED)
        }
    }

    private val eventListener = object : Player.EventListener {
        override fun onLoadingChanged(isLoading: Boolean) {
            if (!isLoading) {
                MediaLogger.d("加载中 ...: $currentState")
                liveData?.value =
                    PlayerStateEvent(PlayerState.STATE_BUFFER_UPDATE).apply {
                        videoWidth = getVideoWidth()
                        videoHeight = getVideoHeight()
                        bufferedPercentage = player.bufferedPercentage
                    }
            }
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            MediaLogger.d("-> 播放状态改变: $playbackState / $playWhenReady === : $currentState")
            // 已经开始播放，两种状态：播放，暂停
            if (!isPreparing) {
                if (playWhenReady) {
                    MediaLogger.d("-> 播放: $currentState")
                    setPlayerState(PlayerState.STATE_STARTED)
                    liveData?.value = PlayerStateEvent(PlayerState.STATE_STARTED)
                } else {
                    MediaLogger.d("-> 暂停: $currentState")
                    setPlayerState(PlayerState.STATE_PAUSED)
                    liveData?.value = PlayerStateEvent(PlayerState.STATE_PAUSED)
                }
            }
            // setDataSource初始化，第一次启播状态：播放，准备好了但是未播放
            if (isPreparing) {
                if (playbackState == Player.STATE_READY) {
                    liveData?.value = PlayerStateEvent(PlayerState.STATE_PREPARED)
                    isPreparing = false
                    if (playWhenReady) {
                        MediaLogger.d("-> 播放: $currentState")
                        setPlayerState(PlayerState.STATE_STARTED)
                        liveData?.value = PlayerStateEvent(PlayerState.STATE_STARTED)
                    } else {
                        MediaLogger.d("-> 准备状态: $currentState")
                        setPlayerState(PlayerState.STATE_PREPARED)
                    }
                    if (startPosition > 0) {
                        liveData?.value = PlayerStateEvent(PlayerState.STATE_SEEK_START)
                        player.seekTo(startPosition)
                        startPosition = 0
                    }
                }
            }
            // 正在缓冲状态，播放器准备结束或者播放结束 -> 缓冲结束
            if (isBuffering) {
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                    isBuffering = false
                    MediaLogger.d("-> 缓冲结束: $currentState")
                    liveData?.value = PlayerStateEvent(PlayerState.STATE_BUFFER_END)
                }
            }

            // 开始拖动进度动作，播放器准备结束 -> 拖动结束
            if (isPendingSeek) {
                if (playbackState == Player.STATE_READY) {
                    isPendingSeek = false
                    MediaLogger.d("-> seek结束: $currentState")
                    liveData?.value = PlayerStateEvent(PlayerState.STATE_SEEK_COMPLETED)
                }
            }
            // 已经开始播放了，缓冲状态，停止状态
            if (!isPreparing) {
                if (playbackState == Player.STATE_BUFFERING) {
                    isBuffering = true
                    MediaLogger.d("-> 开始缓冲: $currentState")
                    liveData?.value = PlayerStateEvent(PlayerState.STATE_BUFFER_START)
                } else if (playbackState == Player.STATE_ENDED) {
                    setPlayerState(PlayerState.STATE_PLAYBACK_COMPLETE)
                    MediaLogger.d("-> 播放结束: $currentState")
                    liveData?.value = PlayerStateEvent(PlayerState.STATE_PLAYBACK_COMPLETE)
                }
            }
        }

        @SuppressLint("SwitchIntDef")
        override fun onPlayerError(error: ExoPlaybackException) {
            MediaLogger.e("${error.message}")
            setPlayerState(PlayerState.STATE_ERROR)
            when (error.type) {
                ExoPlaybackException.TYPE_SOURCE -> {
                    MediaLogger.e("-->TYPE_SOURCE")
                }
                ExoPlaybackException.TYPE_RENDERER -> {
                    MediaLogger.e("-->TYPE_RENDERER")
                }
                ExoPlaybackException.TYPE_UNEXPECTED -> {
                    MediaLogger.e("-->TYPE_UNEXPECTED")
                }
            }
            liveData?.value = PlayerStateEvent(PlayerState.STATE_ERROR).apply {
                errorCode = error.type
            }
        }
    }

    init {
        player = SimpleExoPlayer.Builder(appContext)
            .setTrackSelector(DefaultTrackSelector(appContext))
            .build()
        player.addVideoListener(videoListener)
        player.addListener(eventListener)
        if (BuildConfig.DEBUG) {
            player.addAnalyticsListener(EventLogger(DefaultTrackSelector(appContext)))
        }
        dataSourceFactory = DefaultDataSourceFactory(appContext, Util.getUserAgent(appContext, appContext.packageName))
        mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
    }

    private fun isInPlaybackState(): Boolean {
        val state = getPlayerState()
        return state != PlayerState.STATE_ERROR &&
                state != PlayerState.STATE_NOT_INITIALIZED && state != PlayerState.STATE_STOPPED
    }

    override fun registerStateObserver(liveData: MutableLiveData<PlayerStateEvent>) {
        if (this.liveData != liveData) {
            this.liveData = liveData
        }
    }

    override fun getPlayer(): IPlayerCore? {
        return this
    }

    override fun setDataSource(source: DataSource) {
        val mediaSource = mediaSourceFactory.createMediaSource(Uri.parse(source.mediaUrl))
        player.prepare(mediaSource)
        isPreparing = true
    }

    override fun start() {
        player.playWhenReady = true
        setPlayerState(PlayerState.STATE_STARTED)
    }

    override fun start(startPosition: Long) {
        this.startPosition = startPosition
        start()
    }

    override fun pause() {
        MediaLogger.d("")
        val state = getPlayerState()
        if (state != PlayerState.STATE_STOPPED && state != PlayerState.STATE_ERROR &&
            state != PlayerState.STATE_NOT_INITIALIZED && state != PlayerState.STATE_PAUSED
        ) {
            player.playWhenReady = false
            setPlayerState(PlayerState.STATE_PAUSED)
            MediaLogger.d("暂停播放--->: $currentState")
        }
    }

    override fun resume() {
        MediaLogger.d("")
        if (isInPlaybackState() && getPlayerState() == PlayerState.STATE_PAUSED) {
            player.playWhenReady = true
            setPlayerState(PlayerState.STATE_STARTED)
            MediaLogger.d("恢复播放--->: $currentState")
        }
    }

    override fun setSurfaceHolder(surfaceHolder: SurfaceHolder) {
        player.setVideoSurfaceHolder(surfaceHolder)
    }

    override fun setSurface(surface: Surface) {
        player.setVideoSurface(surface)
    }

    override fun getVideoWidth() = videoWidth

    override fun getVideoHeight() = videoHeight

    override fun getDuration(): Long {
        return if (player.duration < 0) {
            0
        } else {
            player.duration
        }
    }

    override fun isPlaying(): Boolean {
        val state = player.playbackState
        return if (state == Player.STATE_BUFFERING || state == Player.STATE_READY) {
            player.playWhenReady
        } else {
            false
        }
    }

    override fun getBufferedPercentage() = player.bufferedPercentage

    override fun seekTo(position: Long) {
        if (isInPlaybackState()) {
            isPendingSeek = true
        }
        liveData?.value = PlayerStateEvent(PlayerState.STATE_SEEK_START)
        player.seekTo(position)
    }

    override fun stop() {
        isPreparing = true
        isBuffering = false
        isPendingSeek = false
        setPlayerState(PlayerState.STATE_STOPPED)
        player.stop()
    }

    override fun reset() {
        // not support reset, just stop
        stop()
    }

    override fun destroy() {
        isPreparing = true
        isBuffering = false
        isPendingSeek = false
        setPlayerState(PlayerState.STATE_NOT_INITIALIZED)
        player.removeListener(eventListener)
        player.removeVideoListener(videoListener)
        player.release()
    }

    override fun getCurrentPosition() = player.currentPosition

    override fun setPlaySpeed(speed: Float) {
        val parameters = PlaybackParameters(speed, 1f)
        player.setPlaybackParameters(parameters)
    }

    override fun setVolume(left: Float, right: Float) {
        player.volume = left
    }

    override fun setPlayerState(state: PlayerState) {
        currentState = state
    }

    override fun getPlayerState() = currentState

}