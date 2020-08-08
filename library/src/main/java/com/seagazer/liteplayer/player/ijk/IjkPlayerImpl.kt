package com.seagazer.liteplayer.player.ijk

import android.content.ContentResolver
import android.content.Context
import android.media.AudioManager
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
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer

/**
 * Ijk decoder.
 *
 * Author: Seagazer
 * Date: 2020/7/1
 */
class IjkPlayerImpl constructor(val context: Context) : IPlayer {

    private var player: IjkMediaPlayer? = null
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
    private var softwareDecode = true

    private val preparedListener = IMediaPlayer.OnPreparedListener {
        MediaLogger.d("prepared")
        setPlayerState(PlayerState.STATE_PREPARED)
        liveData?.value = PlayerStateEvent(PlayerState.STATE_PREPARED)
        if (asyncToStart && player != null) {
            MediaLogger.d("start play")
            player!!.start()
            if (startPosition > 0) {
                player!!.seekTo(startPosition)
            }
            setPlayerState(PlayerState.STATE_STARTED)
            liveData?.value = PlayerStateEvent(PlayerState.STATE_STARTED)
        }
    }

    private val videoSizeChangedListener =
        IMediaPlayer.OnVideoSizeChangedListener { _, width, height, _, _ ->
            this@IjkPlayerImpl.videoWidth = width
            this@IjkPlayerImpl.videoHeight = height
            MediaLogger.d("video size: $width x $height  : $currentState")
            liveData?.value = PlayerStateEvent(PlayerState.STATE_VIDEO_SIZE_CHANGED).apply {
                videoWidth = width
                videoHeight = height
            }
        }

    private val completionListener = IMediaPlayer.OnCompletionListener { _ ->
        MediaLogger.d("-> play completed: $currentState")
        setPlayerState(PlayerState.STATE_PLAYBACK_COMPLETE)
        liveData?.value = PlayerStateEvent(PlayerState.STATE_PLAYBACK_COMPLETE)
    }

    private val infoListener = IMediaPlayer.OnInfoListener { _, arg1, _ ->
        when (arg1) {
            IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING -> MediaLogger.d("MEDIA_INFO_VIDEO_TRACK_LAGGING:")
            IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                MediaLogger.d("render first frame  : $currentState")
                liveData?.value = PlayerStateEvent(PlayerState.STATE_RENDERED_FIRST_FRAME)
            }
            IMediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                isBuffering = true
                MediaLogger.d("-> loading start: $currentState")
                liveData?.value = PlayerStateEvent(PlayerState.STATE_BUFFER_START)
            }
            IMediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                isBuffering = false
                MediaLogger.d("-> loading end: $currentState")
                liveData?.value = PlayerStateEvent(PlayerState.STATE_BUFFER_END)
            }
            IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH -> {
                MediaLogger.d("MEDIA_INFO_NETWORK_BANDWIDTH")
            }
            IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING -> {
                MediaLogger.d("MEDIA_INFO_BAD_INTERLEAVING")
            }
            IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE -> {
                MediaLogger.d("MEDIA_INFO_NOT_SEEKABLE")
            }
            IMediaPlayer.MEDIA_INFO_METADATA_UPDATE -> {
                MediaLogger.d("MEDIA_INFO_METADATA_UPDATE")
            }
            IMediaPlayer.MEDIA_INFO_TIMED_TEXT_ERROR -> {
                MediaLogger.d("MEDIA_INFO_TIMED_TEXT_ERROR")
            }
            IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE -> {
                MediaLogger.d("MEDIA_INFO_UNSUPPORTED_SUBTITLE")
            }
            IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT -> {
                MediaLogger.d("MEDIA_INFO_SUBTITLE_TIMED_OUT")
            }
            IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED -> {
                MediaLogger.d("MEDIA_INFO_VIDEO_ROTATION_CHANGED")
            }
            IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START -> {
                MediaLogger.d("MEDIA_INFO_AUDIO_RENDERING_START")
            }
            IMediaPlayer.MEDIA_INFO_AUDIO_DECODED_START -> {
                MediaLogger.d("MEDIA_INFO_AUDIO_DECODED_START")
            }
            IMediaPlayer.MEDIA_INFO_AUDIO_SEEK_RENDERING_START -> {
                MediaLogger.d("MEDIA_INFO_AUDIO_SEEK_RENDERING_START")
            }
        }
        true
    }

    private val seekCompleteListener = IMediaPlayer.OnSeekCompleteListener {
        isPendingSeek = false
        MediaLogger.d("-> seek end: $currentState")
        liveData?.value = PlayerStateEvent(PlayerState.STATE_SEEK_COMPLETED)
    }

    private val errorListener = IMediaPlayer.OnErrorListener { _, framework_err, impl_err ->
        MediaLogger.e("play error = $framework_err + $impl_err")
        setPlayerState(PlayerState.STATE_ERROR)
        liveData?.value = PlayerStateEvent(PlayerState.STATE_ERROR).apply {
            errorCode = framework_err
        }
        true
    }

    private val bufferUpdateListener = IMediaPlayer.OnBufferingUpdateListener { _, percent ->
        // rtmp always percent = 0 and always callback here
        if (dataSource != null && !dataSource!!.mediaUrl.startsWith("rtmp:")) {
            currentBufferedPercentage = percent
            MediaLogger.d("loading ...: $currentState")
            liveData?.value = PlayerStateEvent(PlayerState.STATE_BUFFER_UPDATE).apply {
                bufferedPercentage = percent
            }
        }
    }

    override fun registerStateObserver(liveData: MutableLiveData<PlayerStateEvent>) {
        if (this.liveData != liveData) {
            this.liveData = liveData
        }
    }

    override fun getPlayer() = this

    override fun setSurfaceHolder(surfaceHolder: SurfaceHolder) {
        this.surface = surfaceHolder.surface
        player?.setSurface(surface)
    }

    override fun setSurface(surface: Surface) {
        this.surface = surface
        player?.setSurface(surface)
    }

    override fun supportSoftwareDecode(softwareDecode: Boolean) {
        if (this.softwareDecode != softwareDecode) {
            this.softwareDecode = softwareDecode
        }
    }

    override fun setDataSource(source: DataSource) {
        MediaLogger.d("-->$source")
        this.dataSource = source
        openVideo()
    }

    private fun openVideo() {
        try {
            currentBufferedPercentage = 0
            if (player == null) {
                player = IjkMediaPlayer()
                player!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "soundtouch", 1)
                player!!.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 0)
                player!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 5)
                player!!.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1)
            } else {
                stop()
                reset()
            }
            // 0 software decode, 1 mediacodec decode
            player!!.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", if (softwareDecode) 0L else 1L)
            player!!.setOnPreparedListener(preparedListener)
            player!!.setOnVideoSizeChangedListener(videoSizeChangedListener)
            player!!.setOnCompletionListener(completionListener)
            player!!.setOnSeekCompleteListener(seekCompleteListener)
            player!!.setOnBufferingUpdateListener(bufferUpdateListener)
            player!!.setOnErrorListener(errorListener)
            player!!.setOnInfoListener(infoListener)
            player!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            val mediaUrl = dataSource!!.mediaUrl
            val uri = Uri.parse(mediaUrl)
            when {
                mediaUrl.contains("android_asset") -> {//assets resource
                    MediaLogger.e("IjkPlayer not support play assets files!")
                }
                dataSource!!.rawId > 0 -> {// raw resource
                    val rawUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + dataSource!!.rawId)
                    player!!.setDataSource(RawDataSourceProvider.create(context, rawUri))
                }
                else -> {
                    when (uri.scheme) {
                        ContentResolver.SCHEME_ANDROID_RESOURCE -> {
                            player!!.setDataSource(RawDataSourceProvider.create(context, uri))
                        }
                        else -> {
                            player!!.setDataSource(context, uri)
                        }
                    }
                }
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
                        seekTo(startPosition)
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
        player?.seekTo(position)
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
        player?.resetListeners()
        player?.reset()
        setPlayerState(PlayerState.STATE_NOT_INITIALIZED)
        liveData?.value = PlayerStateEvent(PlayerState.STATE_NOT_INITIALIZED)
    }

    override fun destroy() {
        MediaLogger.d("destroy player")
        asyncToStart = false
        isBuffering = false
        reset()
        player?.setSurface(null)
        player?.release()
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
                player!!.duration
            } else {
                0
            }
        }


    override fun isPlaying() = if (player == null) false else player!!.isPlaying

    override fun getBufferedPercentage() = if (player == null) 0 else currentBufferedPercentage

    override fun getCurrentPosition() = if (player == null) 0 else player!!.currentPosition

    override fun setPlaySpeed(speed: Float) {
        player?.setSpeed(speed)
    }

    override fun setVolume(volume: Int) {
        // do nothing, audio manager will handle it always
    }

    override fun setPlayerState(state: PlayerState) {
        currentState = state
    }

    override fun getPlayerState(): PlayerState = currentState
}