package com.seagazer.sample.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import com.seagazer.liteplayer.LitePlayerView
import com.seagazer.liteplayer.config.PlayerType
import com.seagazer.liteplayer.helper.DpHelper
import com.seagazer.liteplayer.helper.MediaLogger
import com.seagazer.liteplayer.listener.SimplePlayerStateChangedListener
import com.seagazer.liteplayer.widget.IOverlay

/**
 *
 * Author: Seagazer
 * Date: 2020/7/25
 */
class ErrorOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr), IOverlay {

    init {
        textSize = DpHelper.sp2px(context, 6f).toFloat()
        setTextColor(Color.WHITE)
    }

    override fun attachPlayer(player: LitePlayerView) {
        player.addView(
            this, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            })
    }

    override fun getView() = this

    override fun show() {
        MediaLogger.e("show cover")
        alpha = 1f
    }

    override fun hide() {
        MediaLogger.e("hide cover")
        animate().alpha(0f).start()
    }

    override fun isShowing() = alpha == 1f

    override fun getPlayerStateChangedListener() = object : SimplePlayerStateChangedListener() {

        override fun onError(playerType: PlayerType, errorCode: Int) {
            text = "播放错误:$errorCode"
            show()
        }

        override fun onPlaying() {
            hide()
        }
    }

    override fun displayModeChanged(isFullScreen: Boolean) {
    }

    override fun autoSensorModeChanged(isAutoSensor: Boolean) {
    }

    override fun floatWindowModeChanged(isFloatWindow: Boolean) {
    }
}