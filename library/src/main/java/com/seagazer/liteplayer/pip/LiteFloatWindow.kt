package com.seagazer.liteplayer.pip

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.seagazer.liteplayer.LitePlayerView
import com.seagazer.liteplayer.R
import com.seagazer.liteplayer.config.FloatSize
import com.seagazer.liteplayer.helper.DpHelper
import com.seagazer.liteplayer.helper.SystemUiHelper

/**
 * Helper to show float window.
 *
 * Author: Seagazer
 * Date: 2020/8/20
 */
class LiteFloatWindow(val context: Context, val litePlayerView: LitePlayerView) : IFloatWindow {
    companion object {
        const val FLOAT_SIZE_LARGE = 1.6f
        const val FLOAT_SIZE_NORMAL = 2.2f
    }

    private var statusBarHeight = -1
    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    private var downX = 0f
    private var downY = 0f
    private var floatSize = FloatSize.NORMAL
    private var isFloatWindowMode = false

    private val floatWindowLp by lazy {
        WindowManager.LayoutParams().apply {
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            // define aspectRatio of float window is 3:2
            val wr = if (floatSize == FloatSize.NORMAL) {
                FLOAT_SIZE_NORMAL
            } else {
                FLOAT_SIZE_LARGE
            }
            val w = context.resources.displayMetrics.widthPixels / wr
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

    @SuppressLint("ClickableViewAccessibility")
    override fun enterFloatWindow() {
        // lazy get status bar height
        if (statusBarHeight == -1) {
            statusBarHeight = SystemUiHelper.getStatusBarHeight(context)
            if (statusBarHeight <= 0) {
                statusBarHeight = DpHelper.dp2px(context, 25f)
            }
        }
        isFloatWindowMode = true
        windowManager.addView(floatWindowContainer, floatWindowLp)
        floatWindowContainer.layoutParams.width = floatWindowLp.width
        floatWindowContainer.layoutParams.height = floatWindowLp.height
        floatWindowContainer.addView(
            litePlayerView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        // add an exit button after the floatWindowContainer attach to window
        val bound = DpHelper.dp2px(context, 20f)
        floatWindowContainer.addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_close)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setOnClickListener {
                litePlayerView.setFloatWindowMode(false)
            }
        }, FrameLayout.LayoutParams(bound, bound).apply {
            gravity = Gravity.END
            setMargins(
                0,
                DpHelper.dp2px(context, 2f),
                DpHelper.dp2px(context, 2f), 0
            )
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

    override fun exitFloatWindow() {
        detachFromFloatWindow()
        isFloatWindowMode = false
    }

    private val floatWindowGesture by lazy {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent?): Boolean {
                downX = e!!.x
                downY = e.y + statusBarHeight
                return true
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                if (litePlayerView.isPlaying()) {
                    litePlayerView.pause(true)
                } else {
                    litePlayerView.resume()
                }
                return true
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
                floatWindowLp.x = (e2!!.rawX - downX).toInt()
                floatWindowLp.y = (e2.rawY - downY).toInt()
                windowManager.updateViewLayout(floatWindowContainer, floatWindowLp)
                return true
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun detachFromFloatWindow() {
        if (isFloatWindowMode) {
            windowManager.removeViewImmediate(floatWindowContainer)
            floatWindowContainer.removeAllViews()
            floatWindowContainer.setOnTouchListener(null)
        }
    }

    override fun refreshFloatWindowSize(sizeMode: FloatSize) {
        if (floatSize != sizeMode) {
            floatSize = sizeMode
            val wr = if (floatSize == FloatSize.NORMAL) {
                FLOAT_SIZE_NORMAL
            } else {
                FLOAT_SIZE_LARGE
            }
            val w = context.resources.displayMetrics.widthPixels / wr
            val h = w / 3 * 2
            floatWindowLp.width = w.toInt()
            floatWindowLp.height = h.toInt()
        }
    }

    override fun isFloatWindow(): Boolean {
        return isFloatWindowMode
    }
}