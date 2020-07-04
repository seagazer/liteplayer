package com.seagazer.liteplayer.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.seagazer.liteplayer.R
import com.seagazer.liteplayer.helper.MediaLogger
import com.seagazer.liteplayer.helper.TimeConverter
import com.seagazer.liteplayer.listener.PlayerStateChangedListener
import kotlin.math.abs

/**
 * An overlay handle gesture action to seek, set brightness and volume.
 *
 * Author: Seagazer
 * Date: 2020/7/3
 */
class LiteGestureOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), IGestureOverlay {
    private var seekController: TextView
    private lateinit var player: LitePlayerView
    private var isSeekShow = false
    private var edgeSize = 0f// left and right area to handle brightness and volume
    private var moveXDistance = 0
    private var currentPosition = 0L
    private var targetPosition = 0L
    private var isTouchInSeekArea = false
    private var isGestureSeeking = false

    init {
        LayoutInflater.from(context).inflate(R.layout.lite_gesture_overlay, this, true)
        seekController = findViewById(R.id.lite_gesture_seek_text)
    }

    private fun setSeekUI(progress: String) {
        seekController.text = progress
    }

    private fun showSeekController() {
        if (!isSeekShow) {
            seekController.visibility = View.VISIBLE
        }
        isSeekShow = true
    }

    override fun attachPlayer(player: LitePlayerView) {
        this.player = player
        this.player.addView(
            this, LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )
    }

    override fun getView() = this

    override fun show() {
        // do nothing
    }

    override fun hide() {
        seekController.visibility = View.INVISIBLE
        isSeekShow = false
    }

    override fun isShowing(): Boolean {
        return seekController.visibility == View.VISIBLE
    }

    override fun getPlayerStateChangedListener(): PlayerStateChangedListener? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        edgeSize = width * 1f / 5
    }

    override fun onDown(e: MotionEvent?) {
        isTouchInSeekArea = e!!.x > edgeSize && e.x < (width - edgeSize)
        MediaLogger.d("isTouchInSeekArea = $isTouchInSeekArea")
        if (isTouchInSeekArea) {
            currentPosition = player.getCurrentPosition()
        }
    }

    override fun onShowPress(e: MotionEvent?) {
    }

    override fun onSingleTapUp(e: MotionEvent?) {
    }

    override fun onSingleTapConfirmed(e: MotionEvent?) {
    }

    override fun onDoubleTap(e: MotionEvent?) {
        if (player.isPlaying()) {
            player.pause()
        } else {
            player.resume()
        }
    }

    override fun onLongPress(e: MotionEvent?) {
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float) {
        // seek: horizontal move
        if (isTouchInSeekArea && abs(e1!!.x - e2!!.x) > abs(e1.y - e2.y)) {
            isGestureSeeking = true
            moveXDistance += distanceX.toInt()
            if (moveXDistance != -1) {
                targetPosition = calculateTargetPosition()
                MediaLogger.d("${TimeConverter.timeToString(targetPosition)}/${TimeConverter.timeToString(player.getDuration())}")
                setSeekUI("${TimeConverter.timeToString(targetPosition)} / ${TimeConverter.timeToString(player.getDuration())}")
                showSeekController()
            }
        }
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float) {
    }

    private fun calculateTargetPosition(): Long {
        val percent = moveXDistance * 1f / width
        MediaLogger.d("seek percent =$percent")
        // Set max seek progress for single gesture action(down->move->up) is duration/3
        val duration = -(player.getDuration() * percent / 3).toLong()
        MediaLogger.d("seek duration =$duration")
        var targetPosition = currentPosition + duration
        if (targetPosition < 0) {
            targetPosition = 0
        } else if (targetPosition > player.getDuration()) {
            targetPosition = player.getDuration()
        }
        return targetPosition
    }

    override fun onGestureFinish(e: MotionEvent?) {
        if (isGestureSeeking) {
            // handle gesture seek action
            MediaLogger.d("seek to =${TimeConverter.timeToString(targetPosition)}")
            player.seekTo(targetPosition)
            moveXDistance = 0
            targetPosition = 0
            isGestureSeeking = false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }


}