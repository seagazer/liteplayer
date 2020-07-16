package com.seagazer.liteplayer.widget

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.*
import com.seagazer.liteplayer.LitePlayerView
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
class LiteGestureController @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), IGesture {
    private lateinit var player: LitePlayerView
    private var edgeSize = 0f// left and right area to handle brightness and volume

    // seek
    private val seekController: TextView
    private var isSeekShow = false
    private var moveXDistance = 0
    private var currentPosition = 0L
    private var targetPosition = 0L
    private var isTouchInSeekArea = false
    private var isGestureSeeking = false

    private val verticalController: LinearLayout
    private val progressIcon: ImageView
    private val progress: ProgressBar
    private var moveYDistance = 0
    private var isVerticalControllerShow = false
    // brightness
    private var isTouchInBrightnessArea = false
    private var currentBrightness = 0f
    private var isBrightnessSetting = false
    // volume
    private var currentVolume = 0
    private var isTouchInVolumeArea = false
    private var isVolumeSetting = false
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    init {
        LayoutInflater.from(context).inflate(R.layout.lite_gesture_overlay, this, true)
        seekController = findViewById(R.id.lite_gesture_seek_text)
        verticalController = findViewById(R.id.lite_gesture_vertical_controller)
        progressIcon = findViewById(R.id.lite_gesture_icon)
        progress = findViewById(R.id.lite_gesture_progress)
    }

    private fun setSeekUI(progress: String) {
        seekController.text = progress
    }

    private fun showSeekController() {
        if (!isSeekShow) {
            seekController.visibility = View.VISIBLE
        }
        isSeekShow = true
        hideVerticalController()
    }

    private fun showVerticalController() {
        if (!isVerticalControllerShow) {
            verticalController.visibility = View.VISIBLE
        }
        isVerticalControllerShow = true
        hideSeekController()
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
        hideSeekController()
        hideVerticalController()
    }

    private fun hideVerticalController() {
        verticalController.visibility = View.INVISIBLE
        isVerticalControllerShow = false
    }

    private fun hideSeekController() {
        seekController.visibility = View.INVISIBLE
        isSeekShow = false
    }

    override fun isShowing(): Boolean {
        return seekController.visibility == View.VISIBLE || verticalController.visibility == View.VISIBLE
    }

    override fun getPlayerStateChangedListener(): PlayerStateChangedListener? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        edgeSize = width * 1f / 5
    }

    override fun onDown(e: MotionEvent?) {
        isTouchInSeekArea = e!!.x > edgeSize && e.x < (width - edgeSize)
        isTouchInBrightnessArea = e.x > 0 && e.x < edgeSize
        isTouchInVolumeArea = e.x > (width - edgeSize) && e.x < width
        MediaLogger.d("isTouchInSeekArea = $isTouchInSeekArea")
        if (isTouchInSeekArea) {
            currentPosition = player.getCurrentPosition()
        }
        MediaLogger.d("isTouchInBrightnessArea = $isTouchInBrightnessArea")
        if (isTouchInBrightnessArea) {
            currentBrightness = (context as Activity).window.attributes.screenBrightness
            if (currentBrightness < 0) {// -1 is auto brightness
                currentBrightness = 0.3f// default start from 0.3f
            }
            progressIcon.setImageResource(R.drawable.ic_brightness)
        }
        MediaLogger.d("isTouchInVolumeArea = $isTouchInVolumeArea")
        if (isTouchInVolumeArea) {
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (currentVolume == 0) {
                progressIcon.setImageResource(R.drawable.ic_volume_off)
            } else {
                progressIcon.setImageResource(R.drawable.ic_volume)
            }
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
            player.pause(true)
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
        // brightness or volume: vertical move
        else if (abs(e1!!.y - e2!!.y) > abs(e1.x - e2.x)) {
            moveYDistance += distanceY.toInt()
            // set brightness
            if (isTouchInBrightnessArea) {
                isBrightnessSetting = true
                val percent = calculateTargetPercent()
                if (context is Activity) {
                    val activity = context as Activity
                    val attributes = activity.window.attributes
                    var targetBrightness = currentBrightness + percent
                    MediaLogger.d("currentBrightness=$currentBrightness, percent=$percent, targetBrightness =$targetBrightness")
                    if (targetBrightness < 0) {
                        targetBrightness = 0.01f
                    }
                    if (targetBrightness > 1) {
                        targetBrightness = 1f
                    }
                    attributes.screenBrightness = targetBrightness
                    activity.window.attributes = attributes
                    progress.progress = (targetBrightness * 100).toInt()
                    showVerticalController()
                }
            }
            // set volume
            else if (isTouchInVolumeArea) {
                isVolumeSetting = true
                val percent = calculateTargetPercent()
                val disVolume: Int = (percent * maxVolume).toInt()
                var targetVolume = currentVolume + disVolume
                MediaLogger.d("currentVolume =$currentVolume, percent=$percent, targetVolume=$targetVolume")
                if (targetVolume < 0) {
                    targetVolume = 0
                }
                if (targetVolume > maxVolume) {
                    targetVolume = maxVolume
                }
                progress.progress = (targetVolume * 1f / maxVolume * 100).toInt()
                player.setVolume(targetVolume)
                if (targetVolume == 0) {
                    progressIcon.setImageResource(R.drawable.ic_volume_off)
                } else {
                    progressIcon.setImageResource(R.drawable.ic_volume)
                }
                showVerticalController()
            }
        }
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

    private fun calculateTargetPercent(): Float {
        var targetPercent = moveYDistance * 1f / height
        if (targetPercent < -1) {
            targetPercent = -1f
        } else if (targetPercent > 1) {
            targetPercent = 1f
        }
        return targetPercent
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float) {
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
        if (isBrightnessSetting) {
            moveYDistance = 0
            isBrightnessSetting = false
        }
        if (isVolumeSetting) {
            moveYDistance = 0
            isBrightnessSetting = false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }


}