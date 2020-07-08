package com.seagazer.liteplayer.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.seagazer.liteplayer.R
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.helper.DpHelper
import com.seagazer.liteplayer.helper.TimeConverter
import com.seagazer.liteplayer.listener.PlayerStateChangedListener

/**
 * Bottom controller overlay for lite player to do control action like seek, start, pause and fullscreen.
 *
 * Author: Seagazer
 * Date: 2020/6/30
 */
class LiteMediaController @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), IController {

    private var toggle: ImageView
    private var fullScreen: ImageView
    private var seekBar: SeekBar
    private var progressTimer: TextView
    private lateinit var player: LitePlayerView
    private var duration = 0L

    init {
        setBackgroundResource(R.drawable.bg_lite_controller)
        setPadding(DpHelper.dp2px(context, 8f), DpHelper.dp2px(context, 8f), DpHelper.dp2px(context, 8f), DpHelper.dp2px(context, 8f))

        LayoutInflater.from(context).inflate(R.layout.lite_controller, this, true)
        toggle = findViewById(R.id.lite_controller_toggle)
        fullScreen = findViewById(R.id.lite_controller_aspect_ratio)
        seekBar = findViewById(R.id.lite_controller_seek_bar)
        progressTimer = findViewById(R.id.lite_controller_progress_timer)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                player.keepOverlayShow(true)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                player.keepOverlayShow(false)
            }

        })
        toggle.setOnClickListener {
            if (player.isPlaying()) {
                player.pause()
            } else {
                player.resume()
            }
        }
        fullScreen.setOnClickListener {
            if (player.isFullScreen()) {
                player.setFullScreenMode(false)
            } else {
                player.setFullScreenMode(true)
            }
        }
    }

    override fun attachPlayer(player: LitePlayerView) {
        this.player = player
        this.player.addView(this, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
        })
    }

    override fun onPlayerPrepared(dataSource: DataSource) {
        duration = player.getDuration()
        seekBar.max = duration.toInt()
    }

    @SuppressLint("SetTextI18n")
    override fun onProgressChanged(progress: Int, secondProgress: Int) {
        seekBar.progress = progress
        seekBar.secondaryProgress = secondProgress
        progressTimer.text =
            "${TimeConverter.timeToString(progress.toLong())} / ${TimeConverter.timeToString(duration)}"
    }

    override fun onStarted() {
        toggle.setImageResource(R.drawable.ic_pause)
    }

    override fun onPaused() {
        toggle.setImageResource(R.drawable.ic_play)
    }

    override fun getView() = this

    override fun show() {
        translationY = 0f
        visibility = View.VISIBLE
    }

    override fun hide() {
        animate().translationY(height.toFloat()).setDuration(300).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                visibility = View.INVISIBLE
            }
        }).start()
    }

    override fun isShowing() = visibility == View.VISIBLE

    override fun getPlayerStateChangedListener(): PlayerStateChangedListener? = null

    override fun displayModeChanged(isFullScreen: Boolean) {
        if (isFullScreen) {
            fullScreen.setImageResource(R.drawable.ic_normal_screen)
        } else {
            fullScreen.setImageResource(R.drawable.ic_full_screen)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return true
    }

}