package com.seagazer.liteplayer.player.media

import android.content.Context
import android.media.MediaPlayer
import android.view.Surface
import android.view.SurfaceHolder
import androidx.lifecycle.MutableLiveData
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.PlayerState
import com.seagazer.liteplayer.event.PlayerStateEvent
import com.seagazer.liteplayer.helper.MediaLogger
import com.seagazer.liteplayer.player.IPlayer
import com.seagazer.liteplayer.player.IPlayerCore

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

    private val preparedListener = MediaPlayer.OnPreparedListener { mp ->
        setPlayerState(PlayerState.STATE_PREPARED)
        mp!!.start()
    }

    private val errorListener = MediaPlayer.OnErrorListener { mp, what, extra ->
        MediaLogger.e("$what")
        setPlayerState(PlayerState.STATE_ERROR)
        liveData?.value = PlayerStateEvent(PlayerState.STATE_ERROR).apply {
            errorCode = what
        }
        false
    }

    private val bufferUpdateListener = MediaPlayer.OnBufferingUpdateListener { mp, percent ->
        currentBufferedPercentage = percent
        MediaLogger.d("加载中 ...: $currentState")
        liveData?.value =
            PlayerStateEvent(PlayerState.STATE_BUFFER_UPDATE).apply {
                videoWidth = getVideoWidth()
                videoHeight = getVideoHeight()
                bufferedPercentage = percent
            }
    }

    private val videoSizeChangedListener = MediaPlayer.OnVideoSizeChangedListener { mp, width, height ->
        this.videoWidth = width
        this.videoHeight = height
        MediaLogger.d("视频尺寸: $width x $height  : $currentState")
        liveData?.value = PlayerStateEvent(PlayerState.STATE_VIDEO_SIZE_CHANGED).apply {
            videoWidth = width
            videoHeight = height
        }
    }

    private val completionListener = MediaPlayer.OnCompletionListener {
        setPlayerState(PlayerState.STATE_PLAYBACK_COMPLETE)
        MediaLogger.d("-> 播放结束: $currentState")
        liveData?.value = PlayerStateEvent(PlayerState.STATE_PLAYBACK_COMPLETE)
    }

    private val infoListener = MediaPlayer.OnInfoListener { mp, what, extra ->
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    init {
        try {
            player = MediaPlayer()
            player!!.setOnPreparedListener(preparedListener)
            player!!.setOnErrorListener(errorListener)
            player!!.setOnBufferingUpdateListener(bufferUpdateListener)
            player!!.setOnVideoSizeChangedListener(videoSizeChangedListener)
            player!!.setOnCompletionListener(completionListener)
            player!!.setOnInfoListener(infoListener)
        } catch (ex: Exception) {
            MediaLogger.e(ex.message!!)
        }
    }

    override fun registerStateObserver(liveData: MutableLiveData<PlayerStateEvent>) {
        this.liveData = liveData
    }

    override fun getPlayer(): IPlayerCore? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setSurfaceHolder(surfaceHolder: SurfaceHolder) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setSurface(surface: Surface) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setDataSource(source: DataSource) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun start() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun start(startPosition: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun pause(fromUser: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun resume() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun seekTo(position: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stop() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun reset() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun destroy() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getVideoWidth(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getVideoHeight(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDuration(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isPlaying(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBufferedPercentage(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCurrentPosition(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setPlaySpeed(speed: Float) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setVolume(volume: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setPlayerState(state: PlayerState) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPlayerState(): PlayerState {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}