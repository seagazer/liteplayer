package com.seagazer.liteplayer

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
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
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.*
import com.seagazer.liteplayer.event.PlayerStateEvent
import com.seagazer.liteplayer.event.RenderStateEvent
import com.seagazer.liteplayer.helper.DpHelper
import com.seagazer.liteplayer.helper.MediaLogger
import com.seagazer.liteplayer.helper.OrientationSensorHelper
import com.seagazer.liteplayer.helper.SystemUiHelper
import com.seagazer.liteplayer.listener.PlayerStateChangedListener
import com.seagazer.liteplayer.player.exo.ExoPlayerImpl
import com.seagazer.liteplayer.player.media.MediaPlayerImpl
import com.seagazer.liteplayer.render.IRender
import com.seagazer.liteplayer.render.RenderMeasure
import com.seagazer.liteplayer.render.RenderSurfaceView
import com.seagazer.liteplayer.render.RenderTextureView
import com.seagazer.liteplayer.widget.*
import java.lang.RuntimeException
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

    // event observer
    private val renderStateObserver = MutableLiveData<RenderStateEvent>()
    private val playerStateObserver = MutableLiveData<PlayerStateEvent>()
    private val playerStateListeners = mutableListOf<PlayerStateChangedListener>()

    // config
    private var render: IRender? = null
    private var playerType: PlayerType? = null
    private var renderType: RenderType? = null

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
    private var controller: IController? = null
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
    private var statusBarHeight = -1
    private var isFloatWindowMode = false
    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    private var downX = 0f
    private var downY = 0f
    private val lp by lazy {
        WindowManager.LayoutParams().apply {
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            // define aspectRatio of float window is 3:2
            val w = context.resources.displayMetrics.widthPixels / 2.2f
            val h = w / 3 * 2
            width = w.toInt()
            height = h.toInt()
            gravity = Gravity.START or Gravity.TOP
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
        }
    }
    private val floatWindowContainer by lazy {
        FrameLayout(context)
    }
    private var isActivityBackground = false

    // message event handler
    private val handler by lazy {
        @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what == MSG_PROGRESS) {
                    currentProgress = getCurrentPosition().toInt()
                    val secondProgress = getBufferedPercentage().coerceAtMost(100) * 1.0f / 100 * getDuration()
                    controller?.onProgressChanged(currentProgress, secondProgress.toInt())
                    sendEmptyMessageDelayed(
                        MSG_PROGRESS,
                        PROGRESS_DELAY
                    )
                    if (currentProgress > 0 && maxProgress > 0) {
                        invalidate()
                    }
                } else if (msg.what == MSG_SHOW_OVERLAY) {
                    controller?.show()
                    topbar?.show()
                    isOverlayDisplaying = true
                    if (currentProgress > 0 && maxProgress > 0) {
                        invalidate()
                    }
                    if (isAutoHideOverlay) {
                        sendEmptyMessageDelayed(
                            MSG_HIDE_OVERLAY,
                            AUTO_HIDE_DELAY
                        )
                    }
                } else if (msg.what == MSG_HIDE_OVERLAY) {
                    controller?.hide()
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
        MediaLogger.d("----> init PlayerCore")
        litePlayerCore = LitePlayerCore(context)
        setBackgroundColor(DEFAULT_BACKGROUND_COLOR)
        registerMediaEventObservers(context)
        registerLifecycle()
    }

    override fun attachMediaController(controller: IController) {
        this.controller = controller
        if (indexOfChild(this.controller!!.getView()) == -1) {
            this.controller!!.attachPlayer(this)
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

    private fun addOverlayInner(overlay: IOverlay) {
        if (indexOfChild(overlay.getView()) == -1) {
            overlay.attachPlayer(this)
            overlay.getPlayerStateChangedListener()?.run {
                addPlayerStateChangedListener(this)
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
                    MediaLogger.d("----> surface create")
                    isSurfaceCreated = true
                    tryBindSurface()
                }
                RenderState.STATE_SURFACE_CHANGED -> {
                    MediaLogger.d("----> surface changed")
                }
                RenderState.STATE_SURFACE_DESTROYED -> {
                    isSurfaceCreated = false
                    MediaLogger.d("----> surface destroy")
                }
            }
        })
        playerStateObserver.observe(context, Observer { event ->
            when (event.playerState) {
                PlayerState.STATE_NOT_INITIALIZED -> {
                    MediaLogger.d("----> player not init")
                }
                PlayerState.STATE_INITIALIZED -> {
                    MediaLogger.d("----> player init")
                    if (isSurfaceCreated) {
                        tryBindSurface()
                    }
                }
                PlayerState.STATE_PREPARED -> {
                    MediaLogger.d("----> player prepared")
                    playerStateListeners.forEach {
                        it.onPrepared(dataSource!!)
                    }
                }
                PlayerState.STATE_STARTED -> {
                    MediaLogger.d("----> player started")
                    isUserPaused = false
                    keepScreenOn = true
                    playerStateListeners.forEach {
                        it.onPlaying()
                    }
                    maxProgress = getDuration().toInt()
                    controller?.onPlayerPrepared(getDataSource()!!)
                    topbar?.onPlayerPrepared(getDataSource()!!)
                    controller?.onStarted()
                    handler.sendEmptyMessage(MSG_PROGRESS)
                }
                PlayerState.STATE_PAUSED -> {
                    MediaLogger.d("----> player paused")
                    keepScreenOn = false
                    playerStateListeners.forEach {
                        it.onPaused()
                    }
                    controller?.onPaused()
                    handler.removeMessages(MSG_PROGRESS)
                }
                PlayerState.STATE_STOPPED -> {
                    MediaLogger.d("----> player stopped")
                    keepScreenOn = false
                    playerStateListeners.forEach {
                        it.onStopped()
                    }
                    handler.removeMessages(MSG_PROGRESS)
                }
                PlayerState.STATE_PLAYBACK_COMPLETE -> {
                    MediaLogger.d("----> player completed")
                    keepScreenOn = false
                    playerStateListeners.forEach {
                        it.onCompleted()
                    }
                    handler.removeMessages(MSG_PROGRESS)
                }
                PlayerState.STATE_ERROR -> {
                    MediaLogger.d("----> player error")
                    keepScreenOn = false
                    playerStateListeners.forEach {
                        it.onError(playerType!!, event.errorCode)
                    }
                    handler.removeMessages(MSG_PROGRESS)
                }
                PlayerState.STATE_VIDEO_SIZE_CHANGED -> {
                    MediaLogger.d("---->video size changed: ${event.videoWidth} * ${event.videoHeight}，refresh surface render size")
                    render?.updateVideoSize(event.videoWidth, event.videoHeight)
                    playerStateListeners.forEach {
                        it.onVideoSizeChanged(event.videoWidth, event.videoHeight)
                    }
                }
                PlayerState.STATE_RENDERED_FIRST_FRAME -> {
                    MediaLogger.d("----> render first frame")
                    playerStateListeners.forEach {
                        it.onRenderFirstFrame()
                    }
                }
                PlayerState.STATE_SURFACE_SIZE_CHANGED -> {
                    MediaLogger.d("----> surface size changed")
                }
                PlayerState.STATE_BUFFER_START -> {
                    MediaLogger.d("----> start buffer")
                    playerStateListeners.forEach {
                        it.onLoadingStarted()
                    }
                }
                PlayerState.STATE_BUFFER_END -> {
                    MediaLogger.d("----> end buffer")
                    playerStateListeners.forEach {
                        it.onLoadingCompleted()
                    }
                }
                PlayerState.STATE_SEEK_START -> {
                    MediaLogger.d("----> start seek")
                    playerStateListeners.forEach {
                        it.onSeekStarted()
                    }
                }
                PlayerState.STATE_SEEK_COMPLETED -> {
                    MediaLogger.d("----> end seek")
                    playerStateListeners.forEach {
                        it.onSeekCompleted()
                    }
                }
                PlayerState.STATE_BUFFER_UPDATE -> {
                    MediaLogger.d("----> buffer update: ${event.bufferedPercentage}")
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
                MediaLogger.d("----> surface bind player")
                render.bindPlayer(player)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        MediaLogger.d("attach window")
        if (androidParent == null) {
            findParent()
        }
        if (isSupportProgress) {
            invalidate()
        }
        handler.sendEmptyMessage(MSG_PROGRESS)
        if (controller != null || topbar != null) {
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
            controller?.autoSensorModeChanged(true)
        } else {
            controller?.autoSensorModeChanged(false)
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
        controller?.displayModeChanged(true)
        topbar?.displayModeChanged(true)
        notifyDisplayModeChanged(true)
        MediaLogger.d("enter fullscreen: $width * $height")
    }

    private fun notifyDisplayModeChanged(isFullScreen: Boolean) {
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
        controller?.displayModeChanged(false)
        topbar?.displayModeChanged(false)
        notifyDisplayModeChanged(false)
        MediaLogger.d("exit fullscreen: $width * $height")
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
        }
        if (gestureController != null && !isFloatWindowMode) {
            return gestureDetector.onTouchEvent(event)
        } else {
            return super.onTouchEvent(event)
        }
    }

    private val gestureDetector by lazy {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent?): Boolean {
                gestureController?.onDown(e)
                if (gestureController != null) {
                    parent.requestDisallowInterceptTouchEvent(true)
                }
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
                if (controller != null) {
                    handler.removeMessages(MSG_HIDE_OVERLAY)
                    if (controller!!.isShowing()) {
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
        if (!isFloatWindowMode && (!isUserPaused || isPlaying())) {
            pause(false)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onActivityDestroy() {
        activityReference?.run {
            clear()
        }
        detachFromFloatWindow()
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
        MediaLogger.d("set render: $renderType")
        addView(
            render!!.getRenderView(), 0, LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        )
        controller?.attachPlayer(this)
        topbar?.attachPlayer(this)
        gestureController?.attachPlayer(this)
        customOverlays.forEach {
            addOverlayInner(it)
        }
        registerRenderStateObserver(renderStateObserver)
    }

    override fun setAspectRatio(aspectRatio: AspectRatio) {
        if (render == null) {
            throw RuntimeException("You has not setup a RenderType, you must call setRenderType() first!")
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
        when (playerType) {
            PlayerType.TYPE_EXO_PLAYER -> {
                litePlayerCore.setupPlayer(ExoPlayerImpl(context))
            }
            PlayerType.TYPE_IJK_PLAYER -> {
                //TODO()
                throw RuntimeException("Not support now, please use TYPE_EXO_PLAYER or TYPE_MEDIA_PLAYER instead.")
            }
            PlayerType.TYPE_MEDIA_PLAYER -> {
                litePlayerCore.setupPlayer(MediaPlayerImpl(context))
            }
        }
        MediaLogger.d("set player: $playerType")
        this.playerType = playerType
        registerPlayerStateObserver(playerStateObserver)
        handler.removeMessages(MSG_PROGRESS)
    }

    override fun getPlayer() = litePlayerCore.getPlayer()

    override fun setDataSource(source: DataSource) {
        dataSource = source
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
        litePlayerCore.setPlaySpeed(speed)
    }

    override fun setVolume(volume: Int) {
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
        if (this.isFloatWindowMode != isFloatWindow) {
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

    override fun isFloatWindow() = isFloatWindowMode

    @SuppressLint("ClickableViewAccessibility")
    private fun enterFloatWindow() {
        // lazy get status bar height
        if (statusBarHeight == -1) {
            statusBarHeight = SystemUiHelper.getStatusBarHeight(context)
            if (statusBarHeight <= 0) {
                statusBarHeight = DpHelper.dp2px(context, 25f)
            }
        }
        isFloatWindowMode = true
        // hide all inner overlay
        notifyFloatWindowModeChanged(true)
        detachVideoContainer()
        windowManager.addView(floatWindowContainer, lp)
        floatWindowContainer.layoutParams.width = lp.width
        floatWindowContainer.layoutParams.height = lp.height
        floatWindowContainer.addView(this, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        // add an exit button after the floatWindowContainer attach to window
        val bound = DpHelper.dp2px(context, 20f)
        floatWindowContainer.addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_close)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setOnClickListener {
                setFloatWindowMode(false)
            }
        }, LayoutParams(bound, bound).apply {
            gravity = Gravity.END
            setMargins(0, DpHelper.dp2px(context, 2f), DpHelper.dp2px(context, 2f), 0)
        })
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(floatWindowContainer, "scaleX", 0f, 1f),
                ObjectAnimator.ofFloat(floatWindowContainer, "scaleY", 0f, 1f),
                ObjectAnimator.ofFloat(floatWindowContainer, "alpha", 0f, 1f)
            )
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        // float window touch event
        floatWindowContainer.setOnTouchListener { _, event ->
            floatWindowGesture.onTouchEvent(event)
        }
    }

    private val floatWindowGesture by lazy {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent?): Boolean {
                downX = e!!.x
                downY = e.y + statusBarHeight
                return true
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                if (this@LitePlayerView.isPlaying()) {
                    this@LitePlayerView.pause(true)
                } else {
                    this@LitePlayerView.resume()
                }
                return true
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
                lp.x = (e2!!.rawX - downX).toInt()
                lp.y = (e2.rawY - downY).toInt()
                windowManager.updateViewLayout(floatWindowContainer, lp)
                return true
            }
        })
    }

    private fun exitFloatWindow() {
        notifyFloatWindowModeChanged(false)
        detachFromFloatWindow()
        detachVideoContainer()
        directParent?.addView(this)
        isFloatWindowMode = false
        // if current activity is background running when close float window, we pause the player and resume play when activity resume.
        if (isActivityBackground) {
            pause(false)
        }
    }

    private fun notifyFloatWindowModeChanged(floatWindow: Boolean) {
        controller?.floatWindowModeChanged(floatWindow)
        gestureController?.floatWindowModeChanged(floatWindow)
        topbar?.floatWindowModeChanged(floatWindow)
        customOverlays.forEach {
            it.floatWindowModeChanged(floatWindow)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun detachFromFloatWindow() {
        if (isFloatWindowMode) {
            windowManager.removeViewImmediate(floatWindowContainer)
            floatWindowContainer.removeAllViews()
            floatWindowContainer.setOnTouchListener(null)
        }
    }

}