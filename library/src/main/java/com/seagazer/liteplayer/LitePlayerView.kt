package com.seagazer.liteplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Message
import android.provider.Settings
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.*
import com.seagazer.liteplayer.event.PlayerStateEvent
import com.seagazer.liteplayer.event.RenderStateEvent
import com.seagazer.liteplayer.helper.MediaLogger
import com.seagazer.liteplayer.helper.OrientationSensorHelper
import com.seagazer.liteplayer.listener.PlayerStateChangedListener
import com.seagazer.liteplayer.listener.RenderStateChangedListener
import com.seagazer.liteplayer.pip.IFloatWindow
import com.seagazer.liteplayer.player.exo.ExoPlayerImpl
import com.seagazer.liteplayer.player.ijk.IjkPlayerImpl
import com.seagazer.liteplayer.player.media.MediaPlayerImpl
import com.seagazer.liteplayer.render.IRender
import com.seagazer.liteplayer.render.RenderMeasure
import com.seagazer.liteplayer.render.RenderSurfaceView
import com.seagazer.liteplayer.render.RenderTextureView
import com.seagazer.liteplayer.widget.*
import java.lang.ref.WeakReference

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
        const val MSG_PROGRESS = 0x2
        const val MSG_SHOW_OVERLAY = 0x3
        const val MSG_HIDE_OVERLAY = 0x4
        const val PROGRESS_DELAY = 1000L
        const val AUTO_HIDE_DELAY = 3000L
        const val PROGRESS_STROKE_WIDTH = 6f
        const val DEFAULT_BACKGROUND_COLOR = Color.BLACK
        const val DEFAULT_PROGRESS_COLOR = 0xffD81BA2
    }

    private val litePlayerCore: LitePlayerCore
    private var activityReference: WeakReference<Activity>? = null
    private var dataSource: DataSource? = null
    private var isUserPaused = false
    private var repeat = false

    // event observer
    private val renderStateObserver = MutableLiveData<RenderStateEvent>()
    private val playerStateObserver = MutableLiveData<PlayerStateEvent>()
    private val playerStateListeners = mutableListOf<PlayerStateChangedListener>()
    private val renderStateListeners = mutableListOf<RenderStateChangedListener>()

    // config
    private var render: IRender? = null
    private var playerType: PlayerType? = null
    private var renderType: RenderType? = null
    private var softwareDecode = true
    var autoHideDelay = AUTO_HIDE_DELAY

    // display mode
    private var isSurfaceCreated = false
    private var androidParent: ViewGroup? = null// android content container
    private var directParent: ViewGroup? = null// the container of this player view
    private var childIndex = 0// the child view index of parent
    private lateinit var originLayoutParams: ViewGroup.LayoutParams
    private val renderMeasure by lazy {
        RenderMeasure()
    }
    private var isFullScreen = false

    // progress
    private var isOverlayDisplaying = false
    private var isSupportProgress = false
    private var currentProgress = 0
    private var maxProgress = 0
    private var progressColor = DEFAULT_PROGRESS_COLOR.toInt()
    private var secondProgressColor = Color.LTGRAY
    private val progressPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = progressColor
            strokeWidth = PROGRESS_STROKE_WIDTH
        }
    }

    // controller
    private var mediaController: IController? = null
    private var isAutoHideOverlay = true

    // topbar
    private var topbar: ITopbar? = null

    // gesture overlay
    private var gestureController: IGesture? = null

    // custom overlay
    private var customOverlays = mutableListOf<IOverlay>()

    // sensor
    private val sensorHelper: OrientationSensorHelper by lazy {
        OrientationSensorHelper(context)
    }

    // float window
    private var floatWindow: IFloatWindow? = null
    private var isActivityBackground = false

    // message event handler
    private val handler by lazy {
        @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what == MSG_PROGRESS) {
                    currentProgress = getCurrentPosition().toInt()
                    val secondProgress = getBufferedPercentage().coerceAtMost(100) * 1.0f / 100 * getDuration()
                    mediaController?.onProgressChanged(currentProgress, secondProgress.toInt())
                    val state = getPlayerState()
                    if (state != PlayerState.STATE_PLAYBACK_COMPLETE && state != PlayerState.STATE_STOPPED
                        && state != PlayerState.STATE_PAUSED && state != PlayerState.STATE_ERROR
                    ) {
                        sendEmptyMessageDelayed(MSG_PROGRESS, PROGRESS_DELAY)
                    }
                    if (currentProgress > 0 && maxProgress > 0) {
                        invalidate()
                    }
                } else if (msg.what == MSG_SHOW_OVERLAY) {
                    mediaController?.show()
                    topbar?.show()
                    isOverlayDisplaying = true
                    if (currentProgress > 0 && maxProgress > 0) {
                        invalidate()
                    }
                    if (isAutoHideOverlay) {
                        sendEmptyMessageDelayed(
                            MSG_HIDE_OVERLAY,
                            autoHideDelay
                        )
                    }
                } else if (msg.what == MSG_HIDE_OVERLAY) {
                    mediaController?.hide()
                    topbar?.hide()
                    isOverlayDisplaying = false
                    if (isSupportProgress && currentProgress > 0 && maxProgress > 0) {
                        invalidate()
                    }
                }
            }
        }
    }

    init {
        MediaLogger.d("----> init")
        if (context is Activity) {
            activityReference = WeakReference(context)
        }
        litePlayerCore = LitePlayerCore(context)
        setBackgroundColor(DEFAULT_BACKGROUND_COLOR)
        registerMediaEventObservers(context)
        registerLifecycle()
    }

    override fun attachMediaController(controller: IController) {
        this.mediaController = controller
        if (indexOfChild(this.mediaController!!.getView()) == -1) {
            this.mediaController!!.attachPlayer(this)
        }
    }

    override fun attachMediaTopbar(topbar: ITopbar) {
        this.topbar = topbar
        if (indexOfChild(this.topbar!!.getView()) == -1) {
            this.topbar!!.attachPlayer(this)
        }
    }

    override fun attachGestureController(gestureOverlay: IGesture) {
        this.gestureController = gestureOverlay
        if (indexOfChild(this.gestureController!!.getView()) == -1) {
            this.gestureController!!.attachPlayer(this)
        }
    }

    override fun attachOverlay(overlay: IOverlay) {
        this.customOverlays.add(overlay)
        addOverlayInner(overlay)
    }

    override fun attachFloatWindow(floatWindow: IFloatWindow) {
        this.floatWindow = floatWindow
    }

    private fun addOverlayInner(overlay: IOverlay) {
        if (indexOfChild(overlay.getView()) == -1) {
            overlay.attachPlayer(this)
            overlay.getPlayerStateChangedListener()?.run {
                addPlayerStateChangedListener(this)
            }
            overlay.getRenderStateChangedListener()?.run {
                addRenderStateChangedListener(this)
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
                    MediaLogger.i("----> surface create")
                    isSurfaceCreated = true
                    tryBindSurface()
                    renderStateListeners.forEach {
                        it.onSurfaceCreated()
                    }
                }
                RenderState.STATE_SURFACE_CHANGED -> {
                    MediaLogger.i("----> surface changed")
                    renderStateListeners.forEach {
                        it.onSurfaceChanged()
                    }
                }
                RenderState.STATE_SURFACE_DESTROYED -> {
                    isSurfaceCreated = false
                    MediaLogger.i("----> surface destroy")
                    renderStateListeners.forEach {
                        it.onSurfaceDestroy()
                    }
                }
            }
        })
        playerStateObserver.observe(context, Observer { event ->
            when (event.playerState) {
                PlayerState.STATE_NOT_INITIALIZED -> {
                    MediaLogger.i("----> player not init")
                }
                PlayerState.STATE_INITIALIZED -> {
                    MediaLogger.i("----> player init")
                    if (isSurfaceCreated) {
                        tryBindSurface()
                    }
                }
                PlayerState.STATE_PREPARED -> {
                    MediaLogger.i("----> player prepared")
                    playerStateListeners.forEach {
                        it.onPrepared(dataSource!!)
                    }
                }
                PlayerState.STATE_STARTED -> {
                    MediaLogger.i("----> player started")
                    isUserPaused = false
                    keepScreenOn = true
                    playerStateListeners.forEach {
                        it.onPlaying()
                    }
                    maxProgress = getDuration().toInt()
                    mediaController?.onPlayerPrepared(getDataSource()!!)
                    mediaController?.onStarted()
                    handler.sendEmptyMessage(MSG_PROGRESS)
                }
                PlayerState.STATE_PAUSED -> {
                    MediaLogger.i("----> player paused")
                    keepScreenOn = false
                    playerStateListeners.forEach {
                        it.onPaused()
                    }
                    mediaController?.onPaused()
                }
                PlayerState.STATE_STOPPED -> {
                    MediaLogger.i("----> player stopped")
                    keepScreenOn = false
                    playerStateListeners.forEach {
                        it.onStopped()
                    }
                }
                PlayerState.STATE_PLAYBACK_COMPLETE -> {
                    MediaLogger.i("----> player completed")
                    keepScreenOn = false
                    playerStateListeners.forEach {
                        it.onCompleted()
                    }
                    if (repeat) {
                        getDataSource()?.let { dataSource ->
                            setDataSource(dataSource)
                            start()
                        }
                    }
                }
                PlayerState.STATE_ERROR -> {
                    MediaLogger.i("----> player error")
                    keepScreenOn = false
                    playerStateListeners.forEach {
                        it.onError(playerType!!, event.errorCode)
                    }
                }
                PlayerState.STATE_VIDEO_SIZE_CHANGED -> {
                    MediaLogger.i("---->video size changed: ${event.videoWidth} * ${event.videoHeight}，refresh surface render size")
                    render?.updateVideoSize(event.videoWidth, event.videoHeight)
                    playerStateListeners.forEach {
                        it.onVideoSizeChanged(event.videoWidth, event.videoHeight)
                    }
                }
                PlayerState.STATE_RENDERED_FIRST_FRAME -> {
                    MediaLogger.i("----> render first frame")
                    playerStateListeners.forEach {
                        it.onRenderFirstFrame()
                    }
                }
                PlayerState.STATE_SURFACE_SIZE_CHANGED -> {
                    MediaLogger.i("----> surface size changed")
                }
                PlayerState.STATE_BUFFER_START -> {
                    MediaLogger.i("----> start loading")
                    playerStateListeners.forEach {
                        it.onLoadingStarted()
                    }
                }
                PlayerState.STATE_BUFFER_END -> {
                    MediaLogger.i("----> end loading")
                    playerStateListeners.forEach {
                        it.onLoadingCompleted()
                    }
                }
                PlayerState.STATE_SEEK_START -> {
                    MediaLogger.i("----> start seek")
                    playerStateListeners.forEach {
                        it.onSeekStarted()
                    }
                }
                PlayerState.STATE_SEEK_COMPLETED -> {
                    MediaLogger.i("----> end seek")
                    playerStateListeners.forEach {
                        it.onSeekCompleted()
                    }
                }
                PlayerState.STATE_BUFFER_UPDATE -> {
                    MediaLogger.i("----> buffer update: ${event.bufferedPercentage}")
                    playerStateListeners.forEach {
                        it.onBufferUpdate(event.bufferedPercentage)
                    }
                }
            }
        })
    }

    private fun tryBindSurface() {
        litePlayerCore.getPlayer()?.let { player ->
            render?.let { render ->
                MediaLogger.w("----> surface bind player")
                render.bindPlayer(player)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (androidParent == null) {
            findParent()
        }
        if (isSupportProgress) {
            invalidate()
        }
        handler.sendEmptyMessage(MSG_PROGRESS)
        if (mediaController != null || topbar != null) {
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
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)// clear message queue
        currentProgress = 0// reset progress
        super.onDetachedFromWindow()
    }

    override fun setProgressColor(progressColor: Int, secondProgressColor: Int) {
        this.progressColor = progressColor
        this.secondProgressColor = secondProgressColor
    }

    override fun displayProgress(showProgress: Boolean) {
        if (this.isSupportProgress == showProgress) {
            return
        }
        isSupportProgress = showProgress
        invalidate()
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        if (!isOverlayDisplaying && isSupportProgress) {
            val h = measuredHeight - PROGRESS_STROKE_WIDTH
            canvas!!.save()
            canvas.clipRect(0f, h, measuredWidth.toFloat(), measuredHeight.toFloat())
            // draw second progress
            progressPaint.color = secondProgressColor
            canvas.drawLine(
                0f,
                measuredHeight.toFloat(),
                getBufferedPercentage().coerceAtMost(100) * 1.0f / 100 * measuredWidth,
                measuredHeight.toFloat(),
                progressPaint
            )
            // draw current progress
            progressPaint.color = progressColor
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

    override fun isFullScreen() = isFullScreen

    override fun setAutoSensorEnable(enable: Boolean) {
        if (enable) {
            sensorListener.enable()
            mediaController?.autoSensorModeChanged(true)
        } else {
            mediaController?.autoSensorModeChanged(false)
            sensorListener.disable()
        }
        notifyAutoSensorModeChanged(enable)
    }

    private fun notifyAutoSensorModeChanged(isAutoSensor: Boolean) {
        customOverlays.forEach {
            it.autoSensorModeChanged(isAutoSensor)
        }
    }

    private val sensorListener: OrientationEventListener by lazy {
        object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_UI) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation in 46..134) {
                    (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    setFullScreenMode(true)
                } else if (orientation in 136..224) {
                    (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    setFullScreenMode(false)
                } else if (orientation in 226..314) {
                    (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    setFullScreenMode(true)
                } else if ((orientation in 316..359) || (orientation in 1..44)) {
                    (context as Activity).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    setFullScreenMode(false)
                }
            }
        }
    }

    override fun setFullScreenMode(isFullScreen: Boolean) {
        // do nothing if current has no attach parent
        if (this.isFullScreen != isFullScreen && parent != null) {
            if (isFullScreen) {
                enterFullScreen()
            } else {
                exitFullScreen()
            }
        }
    }

    private fun enterFullScreen() {
        activityReference?.let {
            it.get()?.run {
                if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    && requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                ) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    sensorHelper.startWatching(this, true)
                }
            }
        }
        // if is list player, the directParent will be changed
        directParent = parent as ViewGroup
        childIndex = directParent!!.indexOfChild(this)
        adjustFullScreen(true)
        this.isFullScreen = true
        detachVideoContainer()
        androidParent?.addView(this, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        notifyDisplayModeChanged(true)
        MediaLogger.w("enter fullscreen: $width x $height")
    }

    private fun notifyDisplayModeChanged(isFullScreen: Boolean) {
        mediaController?.displayModeChanged(isFullScreen)
        topbar?.displayModeChanged(isFullScreen)
        customOverlays.forEach {
            it.displayModeChanged(isFullScreen)
        }
    }

    private fun exitFullScreen() {
        activityReference?.let {
            it.get()?.run {
                if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    sensorHelper.stopWatching()
                }
            }
        }
        adjustFullScreen(false)
        this.isFullScreen = false
        detachVideoContainer()
        directParent?.addView(this, childIndex, originLayoutParams)
        notifyDisplayModeChanged(false)
        MediaLogger.w("exit fullscreen: $width x $height")
    }

    private fun adjustFullScreen(isFullScreen: Boolean) {
        activityReference?.let {
            it.get()?.run {
                val decorView = window.decorView
                if (isFullScreen) {
                    decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                } else {
                    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                }
            }
        }
    }

    private fun detachVideoContainer() {
        if (parent != null) {
            val parent: ViewGroup = parent as ViewGroup
            parent.removeView(this)
        }
    }

    private var handleTouchEvent = true

    /**
     * Set false to handle touch by super.onTouchEvent(event) so mediaController, gestureController not work anymore.
     */
    fun handleTouchEvent(handleTouchEvent: Boolean) {
        this.handleTouchEvent = handleTouchEvent
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!handleTouchEvent) {
            return super.onTouchEvent(event)
        }
        if (event!!.action == MotionEvent.ACTION_UP) {
            gestureController?.onGestureFinish(event)
            gestureController?.hide()
            parent.requestDisallowInterceptTouchEvent(false)
        }
        return if ((gestureController != null || mediaController != null) && !isFloatWindow()) {
            controllerDetector.onTouchEvent(event)
        } else {
            super.onTouchEvent(event)
        }
    }

    private val controllerDetector by lazy {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent?): Boolean {
                gestureController?.onDown(e)
                return true
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
                gestureController?.onScroll(e1, e2, distanceX, distanceY)
                return true
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                gestureController?.onDoubleTap(e)
                return true
            }

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                gestureController?.onSingleTapUp(e)
                return false
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                gestureController?.onFling(e1, e2, velocityX, velocityY)
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                if (mediaController != null) {
                    handler.removeMessages(MSG_HIDE_OVERLAY)
                    if (mediaController!!.isShowing()) {
                        handler.sendEmptyMessage(MSG_HIDE_OVERLAY)
                    } else {
                        handler.sendEmptyMessage(MSG_SHOW_OVERLAY)
                    }
                } else if (topbar != null) {
                    handler.removeMessages(MSG_HIDE_OVERLAY)
                    if (topbar!!.isShowing()) {
                        handler.sendEmptyMessage(MSG_HIDE_OVERLAY)
                    } else {
                        handler.sendEmptyMessage(MSG_SHOW_OVERLAY)
                    }
                }
                gestureController?.onSingleTapConfirmed(e)
                return true
            }

            override fun onShowPress(e: MotionEvent?) {
                gestureController?.onShowPress(e)
            }

            override fun onLongPress(e: MotionEvent?) {
                gestureController?.onLongPress(e)
            }
        })
    }

    private fun registerLifecycle() {
        if (context is FragmentActivity) {
            MediaLogger.d("attached, register lifecycle")
            (context as FragmentActivity).lifecycle.addObserver(this)
        } else {
            MediaLogger.w("Not support lifecycle, you must handle player state when activity stop or resume by yourself!")
        }
    }

    private fun unregisterLifecycle() {
        if (context is FragmentActivity) {
            MediaLogger.d("detached, unregister lifecycle")
            (context as FragmentActivity).lifecycle.removeObserver(this)
        } else {
            MediaLogger.w("Not support lifecycle, you must handle player state when activity stop or resume by yourself!")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onActivityResume() {
        isActivityBackground = false
        if (!isUserPaused) {
            resume()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onActivityStop() {
        isActivityBackground = true
        if (!isFloatWindow() && (!isUserPaused || isPlaying())) {
            pause(false)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onActivityDestroy() {
        activityReference?.run {
            clear()
        }
        floatWindow?.detachFromFloatWindow()
        stop()
        destroy()
        unregisterLifecycle()
    }

    override fun registerPlayerStateObserver(liveData: MutableLiveData<PlayerStateEvent>) {
        litePlayerCore.registerStateObserver(liveData)
    }

    override fun registerRenderStateObserver(liveData: MutableLiveData<RenderStateEvent>) {
        render!!.registerStateObserver(liveData)
    }

    override fun addPlayerStateChangedListener(listener: PlayerStateChangedListener) {
        if (playerStateListeners.indexOf(listener) == -1) {
            playerStateListeners.add(listener)
        }
    }

    override fun addRenderStateChangedListener(listener: RenderStateChangedListener) {
        if (renderStateListeners.indexOf(listener) == -1) {
            renderStateListeners.add(listener)
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
        MediaLogger.w("set render: $renderType")
        render = when (renderType) {
            RenderType.TYPE_SURFACE_VIEW -> RenderSurfaceView(context).apply {
                bindRenderMeasure(renderMeasure)
            }
            RenderType.TYPE_TEXTURE_VIEW -> RenderTextureView(context).apply {
                bindRenderMeasure(renderMeasure)
            }
        }
        addView(
            render!!.getRenderView(), 0, LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        )
        mediaController?.attachPlayer(this)
        topbar?.attachPlayer(this)
        gestureController?.attachPlayer(this)
        customOverlays.forEach {
            addOverlayInner(it)
        }
        registerRenderStateObserver(renderStateObserver)
    }

    override fun setAspectRatio(aspectRatio: AspectRatio) {
        if (render == null) {
            throw RuntimeException("Instance of render view is null, you must call setRenderType() to create a render view first!")
        }
        render!!.updateAspectRatio(aspectRatio)
    }

    override fun setPlayerType(playerType: PlayerType) {
        // release last player if changed player
        if (this.playerType == playerType) {
            return
        }
        litePlayerCore.reset()
        litePlayerCore.destroy()
        MediaLogger.w("set player: $playerType")
        when (playerType) {
            PlayerType.TYPE_EXO_PLAYER -> {
                litePlayerCore.setupPlayer(ExoPlayerImpl(context))
            }
            PlayerType.TYPE_IJK_PLAYER -> {
                litePlayerCore.setupPlayer(IjkPlayerImpl(context))
            }
            PlayerType.TYPE_MEDIA_PLAYER -> {
                litePlayerCore.setupPlayer(MediaPlayerImpl(context))
            }
        }
        litePlayerCore.supportSoftwareDecode(softwareDecode)
        this.playerType = playerType
        registerPlayerStateObserver(playerStateObserver)
        handler.removeMessages(MSG_PROGRESS)
    }

    override fun getPlayer() = litePlayerCore.getPlayer()

    override fun getRender(): IRender? = render

    override fun setDataSource(source: DataSource) {
        dataSource = source
        MediaLogger.w("setDataSource: $source")
        // when ijkplayer change decode mode with texture view, system may get this error:
        // 19543-19665 E/BufferQueueProducer: [SurfaceTexture-0-19543-0] connect: already connected (cur=2 req=2)
        // 19543-19665 E/IJKMEDIA: SDL_Android_NativeWindow_display_l: ANativeWindow_lock: failed -22
        // so we always destroy the texture and reAttach the texture
        render?.let {
            removeView(it.getRenderView())
            addView(
                it.getRenderView(), 0, LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                )
            )
        }
        // reset ui right now
        currentProgress = 0
        mediaController?.reset()
        topbar?.onDataSourceChanged(getDataSource()!!)
        litePlayerCore.setDataSource(source)
    }

    override fun start() {
        litePlayerCore.start()
    }

    override fun start(startPosition: Long) {
        litePlayerCore.start(startPosition)
    }

    override fun pause(fromUser: Boolean) {
        isUserPaused = fromUser
        litePlayerCore.pause(fromUser)
    }

    override fun resume() {
        litePlayerCore.resume()
    }

    override fun seekTo(position: Long) {
        litePlayerCore.seekTo(position)
    }

    override fun stop() {
        litePlayerCore.stop()
    }

    override fun reset() {
        litePlayerCore.reset()
    }

    override fun destroy() {
        litePlayerCore.destroy()
    }

    override fun getVideoWidth() = litePlayerCore.getVideoWidth()

    override fun getVideoHeight() = litePlayerCore.getVideoHeight()

    override fun getDuration() = litePlayerCore.getDuration()

    override fun isPlaying() = litePlayerCore.isPlaying()

    override fun getBufferedPercentage() = litePlayerCore.getBufferedPercentage()

    override fun getCurrentPosition() = litePlayerCore.getCurrentPosition()

    override fun setPlaySpeed(speed: Float) {
        if (playerType == null) {
            throw RuntimeException("Instance of player is null, you must call setPlayerType() to create a player first!")
        }
        litePlayerCore.setPlaySpeed(speed)
    }

    override fun setVolume(volume: Int) {
        if (playerType == null) {
            throw RuntimeException("Instance of player is null, you must call setPlayerType() to create a player first!")
        }
        litePlayerCore.setVolume(volume)
    }

    override fun setPlayerState(state: PlayerState) {
        litePlayerCore.setPlayerState(state)
    }

    override fun getPlayerState() = litePlayerCore.getPlayerState()

    override fun getDataSource() = dataSource

    /**
     * Set auto hide the mediaController and topBar.
     * @param autoHide True auto hide, false otherwise.
     */
    fun setAutoHideOverlay(autoHide: Boolean) {
        isAutoHideOverlay = autoHide
    }

    /**
     * Stop or resume auto hide the mediaController and topBar one time.
     * @param keep True stop auto hide, false resume auto hide.
     */
    fun keepOverlayShow(keep: Boolean) {
        if (keep) {
            handler.removeMessages(MSG_HIDE_OVERLAY)
        } else {
            handler.sendEmptyMessage(MSG_HIDE_OVERLAY)
        }
    }

    override fun setFloatWindowMode(isFloatWindow: Boolean) {
        if (floatWindow?.isFloatWindow() != isFloatWindow) {
            if (isFloatWindow) {
                // check overlay permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(context)) {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        (context as Activity).startActivityForResult(intent, 0x110)
                    } else {
                        enterFloatWindow()
                    }
                }
            } else {
                exitFloatWindow()
            }
        }
    }

    override fun isFloatWindow() = if (floatWindow == null) false else floatWindow!!.isFloatWindow()

    private fun enterFloatWindow() {
        floatWindow?.let {
            notifyFloatWindowModeChanged(true)
            detachVideoContainer()
            it.enterFloatWindow()
        }
    }

    private fun exitFloatWindow() {
        floatWindow?.let {
            notifyFloatWindowModeChanged(false)
            detachVideoContainer()
            it.exitFloatWindow()
            directParent?.addView(this)
            // if current activity is background running when close float window, we pause the player and resume play when activity resume.
            if (isActivityBackground) {
                pause(false)
            }
        }
    }

    private fun notifyFloatWindowModeChanged(floatWindow: Boolean) {
        mediaController?.floatWindowModeChanged(floatWindow)
        gestureController?.floatWindowModeChanged(floatWindow)
        topbar?.floatWindowModeChanged(floatWindow)
        customOverlays.forEach {
            it.floatWindowModeChanged(floatWindow)
        }
    }

    override fun setFloatSizeMode(sizeMode: FloatSize) {
        floatWindow?.refreshFloatWindowSize(sizeMode)
    }

    override fun setRepeatMode(repeat: Boolean) {
        this.repeat = repeat
    }

    override fun supportSoftwareDecode(softwareDecode: Boolean) {
        if (this.softwareDecode != softwareDecode) {
            this.softwareDecode = softwareDecode
            if (playerType != null) {
                litePlayerCore.supportSoftwareDecode(softwareDecode)
            } else {
                MediaLogger.w("Instance of player is null, this method may called after you setup a playerType!")
            }
        }
    }

}