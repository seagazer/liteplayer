package com.seagazer.liteplayer.widget

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.seagazer.liteplayer.LitePlayerCore
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.*
import com.seagazer.liteplayer.event.PlayerStateEvent
import com.seagazer.liteplayer.event.RenderStateEvent
import com.seagazer.liteplayer.helper.MediaLogger
import com.seagazer.liteplayer.helper.OrientationSensorHelper
import com.seagazer.liteplayer.listener.PlayerStateChangedListener
import com.seagazer.liteplayer.player.IPlayer
import com.seagazer.liteplayer.player.exo.ExoPlayerImpl
import com.seagazer.liteplayer.render.IRender
import com.seagazer.liteplayer.render.RenderMeasure
import com.seagazer.liteplayer.render.RenderSurfaceView
import com.seagazer.liteplayer.render.RenderTextureView

/**
 * A lite player view to play media source.
 *
 * Author: Seagazer
 * Date: 2020/6/19
 */
class LitePlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), IPlayerView, LifecycleObserver {

    companion object {
        const val MSG_PROGRESS_INNER = 0x1
        const val MSG_PROGRESS = 0x2
        const val MSG_SHOW_OVERLAY = 0x3
        const val MSG_HIDE_OVERLAY = 0x4
        const val PROGRESS_DELAY = 1000L
        const val AUTO_HIDE_DELAY = 3000L
        const val PROGRESS_STROKE_WIDTH = 6f
        const val DEFAULT_BACKGROUND_COLOR = Color.BLACK
        const val DEFAULT_PROGRESS_COLOR = Color.RED
    }

    private var dataSource: DataSource? = null
    // event observer
    private val renderStateObserver = MutableLiveData<RenderStateEvent>()
    private val playerStateObserver = MutableLiveData<PlayerStateEvent>()
    private var playerStateListeners = mutableListOf<PlayerStateChangedListener>()
    // config
    private var render: IRender? = null
    private var playerType: PlayerType? = null
    private var renderType: RenderType? = null
    // display mode
    private var androidParent: ViewGroup? = null// android content container
    private var directParent: ViewGroup? = null// the container of this player view
    private var childIndex = 0// the child view index of parent
    private lateinit var originLayoutParams: ViewGroup.LayoutParams
    private val renderMeasure by lazy {
        RenderMeasure()
    }
    var isFullScreen = false
    // progress
    private var isShowProgress = true
    private var currentProgress = 0
    private var maxProgress = 0
    private val progressPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = DEFAULT_PROGRESS_COLOR
            strokeWidth = PROGRESS_STROKE_WIDTH
        }
    }
    // controller
    private var controller: IController? = null
    private var isAutoHideOverlay = true
    // topbar
    private var topbar: ITopbar? = null
    // custom overlay
    private var customOverlays = mutableListOf<IOverlay>()
    // sensor
    private val sensorHelper: OrientationSensorHelper by lazy {
        OrientationSensorHelper(context)
    }

    // message event handler
    private val handler by lazy {
        @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message) {
                currentProgress = getCurrentPosition().toInt()
                if (msg.what == MSG_PROGRESS_INNER) {
                    sendEmptyMessageDelayed(MSG_PROGRESS_INNER, PROGRESS_DELAY)
                    invalidate()
                } else if (msg.what == MSG_PROGRESS) {
                    val secondProgress = getBufferedPercentage().coerceAtMost(100) * 1.0f / 100 * getDuration()
                    controller?.onProgressChanged(currentProgress, secondProgress.toInt())
                    sendEmptyMessageDelayed(MSG_PROGRESS, PROGRESS_DELAY)
                } else if (msg.what == MSG_SHOW_OVERLAY) {
                    controller?.show()
                    topbar?.show()
                    hideDefaultProgress()
                    if (isAutoHideOverlay) {
                        sendEmptyMessageDelayed(MSG_HIDE_OVERLAY, AUTO_HIDE_DELAY)
                    }
                } else if (msg.what == MSG_HIDE_OVERLAY) {
                    controller?.hide()
                    topbar?.hide()
                    showDefaultProgress()
                }
            }
        }
    }

    init {
        MediaLogger.d("----> 初始化")
        if (!LitePlayerCore.isInit) {
            MediaLogger.d("----> 初始化 PlayerManager")
            LitePlayerCore.init(context)
        }
        setBackgroundColor(DEFAULT_BACKGROUND_COLOR)
        registerMediaEventObservers(context)
        registerLifecycle()
    }

    fun attachMediaController(controller: IController) {
        this.controller = controller
        if (indexOfChild(controller.getView()) == -1) {
            this.controller!!.attachPlayer(this)
        }
    }

    fun attachMediaTopbar(topbar: ITopbar) {
        this.topbar = topbar
        if (indexOfChild(topbar.getView()) == -1) {
            this.topbar!!.attachPlayer(this)
        }
    }

    fun attachOverlay(overlay: IOverlay) {
        this.customOverlays.add(overlay)
        addOverlayInner(overlay)
    }

    private fun addOverlayInner(overlay: IOverlay) {
        if (indexOfChild(overlay.getView()) == -1) {
            overlay.attachPlayer(this)
            overlay.getPlayerStateChangedListener()?.run {
                setPlayerStateChangedListener(this)
            }
        }
    }

    private fun registerMediaEventObservers(context: Context) {
        if (context !is FragmentActivity) {
            throw IllegalStateException("The current activity must be sub class of FragmentActivity!")
        }
        renderStateObserver.observe(context, Observer { event ->
            when (event.renderState) {
                RenderState.STATE_SURFACE_CREATED -> {
                    MediaLogger.d("----> Surface创建")
                    LitePlayerCore.getPlayer()?.let { player ->
                        MediaLogger.d("----> 播放器绑定surface")
                        render?.bindPlayer(player)
                    }
                }
                RenderState.STATE_SURFACE_CHANGED -> {
                    MediaLogger.d("----> Surface改变")
                }
                RenderState.STATE_SURFACE_DESTROYED -> {
                    MediaLogger.d("----> Surface销毁")
                }
            }
        })
        playerStateObserver.observe(context, Observer { event ->
            when (event.playerState) {
                PlayerState.STATE_NOT_INITIALIZED -> {
                    MediaLogger.d("----> 播放待初始化")
                }
                PlayerState.STATE_PREPARED -> {
                    MediaLogger.d("----> 播放准备")
                    playerStateListeners.forEach {
                        it.onPrepared(dataSource!!)
                    }
                }
                PlayerState.STATE_STARTED -> {
                    MediaLogger.d("----> 播放开始")
                    keepScreenOn = true
                    playerStateListeners.forEach {
                        it.onPlaying()
                    }
                    maxProgress = getDuration().toInt()
                    controller?.onPlayerPrepared(getDataSource()!!)
                    topbar?.onPlayerPrepared(getDataSource()!!)
                    controller?.onStarted()
                    handler.sendEmptyMessage(MSG_PROGRESS)
                    if (isShowProgress) {
                        handler.removeMessages(MSG_PROGRESS_INNER)
                        handler.sendEmptyMessage(MSG_PROGRESS_INNER)
                    }
                    handler.sendEmptyMessage(MSG_SHOW_OVERLAY)
                }
                PlayerState.STATE_PAUSED -> {
                    MediaLogger.d("----> 播放暂停")
                    keepScreenOn = false
                    playerStateListeners.forEach {
                        it.onPaused()
                    }
                    controller?.onPaused()
                    handler.removeMessages(MSG_PROGRESS)
                    handler.removeMessages(MSG_PROGRESS_INNER)
                }
                PlayerState.STATE_STOPPED -> {
                    MediaLogger.d("----> 播放停止")
                    keepScreenOn = false
                    playerStateListeners.forEach {
                        it.onStopped()
                    }
                    handler.removeMessages(MSG_PROGRESS)
                    handler.removeMessages(MSG_PROGRESS_INNER)
                }
                PlayerState.STATE_PLAYBACK_COMPLETE -> {
                    MediaLogger.d("----> 播放完毕")
                    keepScreenOn = false
                    playerStateListeners.forEach {
                        it.onCompleted()
                    }
                    handler.removeMessages(MSG_PROGRESS)
                    handler.removeMessages(MSG_PROGRESS_INNER)
                }
                PlayerState.STATE_ERROR -> {
                    MediaLogger.d("----> 播放错误")
                    keepScreenOn = false
                    playerStateListeners.forEach {
                        it.onError(playerType!!, event.errorCode)
                    }
                    handler.removeMessages(MSG_PROGRESS)
                    handler.removeMessages(MSG_PROGRESS_INNER)
                }
                PlayerState.STATE_VIDEO_SIZE_CHANGED -> {
                    MediaLogger.d("---->视频宽高改变: ${event.videoWidth} * ${event.videoHeight}，刷新surface render初始尺寸:")
                    render?.updateVideoSize(event.videoWidth, event.videoHeight)
                    playerStateListeners.forEach {
                        it.onVideoSizeChanged(event.videoWidth, event.videoHeight)
                    }
                }
                PlayerState.STATE_RENDERED_FIRST_FRAME -> {
                    MediaLogger.d("----> 视频渲染首帧")
                    playerStateListeners.forEach {
                        it.onRenderFirstFrame()
                    }
                }
                PlayerState.STATE_SURFACE_SIZE_CHANGED -> {
                    MediaLogger.d("----> Surface尺寸改变")
                }
                PlayerState.STATE_BUFFER_START -> {
                    MediaLogger.d("----> 开始缓冲")
                    playerStateListeners.forEach {
                        it.onLoadingStarted()
                    }
                }
                PlayerState.STATE_BUFFER_END -> {
                    MediaLogger.d("----> 缓冲结束")
                    playerStateListeners.forEach {
                        it.onLoadingCompleted()
                    }
                }
                PlayerState.STATE_SEEK_START -> {
                    MediaLogger.d("----> seek开始")
                    playerStateListeners.forEach {
                        it.onSeekStarted()
                    }
                }
                PlayerState.STATE_SEEK_COMPLETED -> {
                    MediaLogger.d("----> seek结束")
                    playerStateListeners.forEach {
                        it.onSeekCompleted()
                    }
                    start()
                }
                PlayerState.STATE_BUFFER_UPDATE -> {
                    MediaLogger.d("----> 缓冲进度更新: ${event.bufferedPercentage}")
                    playerStateListeners.forEach {
                        it.onBufferUpdate(event.bufferedPercentage)
                    }
                }
            }
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        MediaLogger.d("attach window")
        if (androidParent == null) {
            findParent()
        }
        if (isShowProgress) {// always false when first attach
            showDefaultProgress()
        }
        controller?.let {
            handler.sendEmptyMessage(MSG_PROGRESS)
            handler.sendEmptyMessage(MSG_SHOW_OVERLAY)
        }
    }

    private fun findParent() {
        if (parent == null) {
            return
        }
        directParent = parent as ViewGroup
        childIndex = directParent!!.indexOfChild(this)
        var p: ViewGroup = parent as ViewGroup
        originLayoutParams = layoutParams
        while (p.id != android.R.id.content) {
            p = p.parent as ViewGroup
        }
        androidParent = p
        MediaLogger.d("android parent= $androidParent")
        MediaLogger.d("direct parent= $directParent")
        MediaLogger.d("directParentIndex= $childIndex")
        MediaLogger.d("originLayoutParams= $originLayoutParams")
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)// clear message queue
        currentProgress = 0// reset progress
        super.onDetachedFromWindow()
        MediaLogger.d("detach window")
    }

    fun displayProgress(showProgress: Boolean) {
        if (this.isShowProgress == showProgress) {
            return
        }
        if (showProgress) {
            showDefaultProgress()
        } else {
            hideDefaultProgress()
        }
    }

    private fun showDefaultProgress() {
        MediaLogger.d("show progress")
        this.isShowProgress = true
        handler.sendEmptyMessageDelayed(MSG_PROGRESS_INNER, PROGRESS_DELAY)
    }

    private fun hideDefaultProgress() {
        MediaLogger.d("hide progress")
        this.isShowProgress = false
        handler.removeMessages(MSG_PROGRESS_INNER)
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        if (isShowProgress) {
            val h = measuredHeight - PROGRESS_STROKE_WIDTH
            canvas!!.save()
            canvas.clipRect(0f, h, measuredWidth.toFloat(), measuredHeight.toFloat())
            canvas.drawLine(
                0f,
                measuredHeight.toFloat(),
                measuredWidth * currentProgress * 1f / maxProgress,
                measuredHeight.toFloat(),
                progressPaint
            )
            canvas.restore()
        }
    }

    fun setFullScreenMode(isFullScreen: Boolean) {
        // do nothing if current has no attach parent
        if (this.isFullScreen != isFullScreen && parent != null) {
            if (isFullScreen) {
                // if is list player, the directParent will be changed
                directParent = parent as ViewGroup
                childIndex = directParent!!.indexOfChild(this)
                this.isFullScreen = true
                detachVideoContainer()
                androidParent?.addView(this, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
                controller?.displayModeChanged(true)
                topbar?.displayModeChanged(true)
                if (context is Activity) {
                    val activity = context as Activity
                    if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        sensorHelper.startWatching(activity, true)
                    }
                }
                MediaLogger.d("开启全屏: $width * $height")
            } else {
                this.isFullScreen = false
                detachVideoContainer()
                directParent?.addView(this, childIndex, originLayoutParams)
                controller?.displayModeChanged(false)
                topbar?.displayModeChanged(false)
                if (context is Activity) {
                    val activity = context as Activity
                    if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                    sensorHelper.stopWatching()
                }
                MediaLogger.d("退出全屏: $width * $height")
            }
        }
    }

    private fun detachVideoContainer() {
        if (parent != null) {
            val parent: ViewGroup = parent as ViewGroup
            parent.removeView(this)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            handler.removeMessages(MSG_HIDE_OVERLAY)
            if (controller != null) {
                if (controller!!.isShowing()) {
                    handler.sendEmptyMessage(MSG_HIDE_OVERLAY)
                } else {
                    handler.sendEmptyMessage(MSG_SHOW_OVERLAY)
                }
            } else if (topbar != null) {
                if (topbar!!.isShowing()) {
                    handler.sendEmptyMessage(MSG_HIDE_OVERLAY)
                } else {
                    handler.sendEmptyMessage(MSG_SHOW_OVERLAY)
                }
            }
        } else if (event.action == MotionEvent.ACTION_MOVE) {
            //TODO("seek setVolume setBlight")
        } else if (event.action == MotionEvent.ACTION_UP) {
            //TODO("seek completed")
        }
        return true
    }

    private fun registerLifecycle() {
        if (context is FragmentActivity) {
            MediaLogger.d("attached, register lifecycle")
            (context as FragmentActivity).lifecycle.addObserver(this)
        }
    }

    private fun unregisterLifecycle() {
        if (context is FragmentActivity) {
            (context as FragmentActivity).lifecycle.removeObserver(this)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onActivityResume() {
        MediaLogger.d("-->")
        resume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onActivityStop() {
        MediaLogger.d("-->")
        pause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onActivityDestroy() {
        MediaLogger.d("-->")
        stop()
        destroy()
        unregisterLifecycle()
    }

    override fun registerPlayerStateObserver(liveData: MutableLiveData<PlayerStateEvent>) {
        LitePlayerCore.registerStateObserver(liveData)
    }

    override fun registerRenderStateObserver(liveData: MutableLiveData<RenderStateEvent>) {
        render!!.registerStateObserver(liveData)
    }

    override fun setPlayerStateChangedListener(listener: PlayerStateChangedListener) {
        if (playerStateListeners.indexOf(listener) == -1) {
            playerStateListeners.add(listener)
        }
    }

    override fun setRenderType(renderType: RenderType) {
        // release last render if changed render
        if (this.renderType == renderType) {
            return
        }
        render?.release()
        removeAllViews()
        this.renderType = renderType
        render = when (renderType) {
            RenderType.TYPE_SURFACE_VIEW -> RenderSurfaceView(context).apply {
                bindRenderMeasure(renderMeasure)
            }
            RenderType.TYPE_TEXTURE_VIEW -> RenderTextureView(context).apply {
                bindRenderMeasure(renderMeasure)
            }
        }
        MediaLogger.d("改变render: $renderType")
        addView(
            render!!.getRenderView(), 0, LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        )
        controller?.attachPlayer(this)
        topbar?.attachPlayer(this)
        customOverlays.forEach {
            addOverlayInner(it)
        }
        registerRenderStateObserver(renderStateObserver)
    }

    override fun setAspectRatio(aspectRatio: AspectRatio) {
        render?.updateAspectRatio(aspectRatio)
    }

    override fun setPlayerType(playerType: PlayerType) {
        // release last player if changed player
        if (this.playerType == playerType) {
            return
        }
        LitePlayerCore.reset()
        LitePlayerCore.destroy()
        when (playerType) {
            PlayerType.TYPE_EXO_PLAYER -> {
                LitePlayerCore.setupPlayer(ExoPlayerImpl(context))
            }
            PlayerType.TYPE_IJK_PLAYER -> {
                //TODO()
            }
            PlayerType.TYPE_MEDIA_PLAYER -> {
                //TODO()
            }
        }
        MediaLogger.d("改变player: $playerType")
        this.playerType = playerType
        registerPlayerStateObserver(playerStateObserver)
    }

    override fun getPlayer(): IPlayer? {
        return LitePlayerCore.getPlayer()
    }

    override fun isInPlaybackState(): Boolean {
        return LitePlayerCore.isPlaying()
    }

    override fun setDataSource(source: DataSource) {
        dataSource = source
        LitePlayerCore.setDataSource(source)
    }

    override fun start() {
        LitePlayerCore.start()
    }

    override fun start(startPosition: Long) {
        LitePlayerCore.start(startPosition)
    }

    override fun pause() {
        LitePlayerCore.pause()
    }

    override fun resume() {
        LitePlayerCore.resume()
    }

    override fun seekTo(position: Long) {
        LitePlayerCore.seekTo(position)
    }

    override fun stop() {
        LitePlayerCore.stop()
    }

    override fun reset() {
        LitePlayerCore.reset()
    }

    override fun destroy() {
        LitePlayerCore.destroy()
    }

    override fun getVideoWidth(): Int {
        return LitePlayerCore.getVideoWidth()
    }

    override fun getVideoHeight(): Int {
        return LitePlayerCore.getVideoHeight()
    }

    override fun getDuration(): Long {
        return LitePlayerCore.getDuration()
    }

    override fun isPlaying(): Boolean {
        return LitePlayerCore.isPlaying()
    }

    override fun getBufferedPercentage(): Int {
        return LitePlayerCore.getBufferedPercentage()
    }

    override fun getCurrentPosition(): Long {
        return LitePlayerCore.getCurrentPosition()
    }

    override fun setPlaySpeed(speed: Float) {
        LitePlayerCore.setPlaySpeed(speed)
    }

    override fun setVolume(left: Float, right: Float) {
        LitePlayerCore.setVolume(left, right)
    }

    override fun setPlayerState(state: PlayerState) {
        LitePlayerCore.setPlayerState(state)
    }

    override fun getPlayerState(): PlayerState {
        return LitePlayerCore.getPlayerState()
    }

    override fun getDataSource() = dataSource

    fun setAutoHideOverlay(autoHide: Boolean) {
        isAutoHideOverlay = autoHide
    }

}