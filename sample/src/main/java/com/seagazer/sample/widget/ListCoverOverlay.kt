package com.seagazer.sample.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.seagazer.liteplayer.LitePlayerView
import com.seagazer.liteplayer.helper.MediaLogger
import com.seagazer.liteplayer.listener.SimplePlayerStateChangedListener
import com.seagazer.liteplayer.widget.IOverlay

/**
 *
 * Author: Seagazer
 * Date: 2020/7/14
 */
class ListCoverOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), IOverlay {

    fun setCover(resource: Int) {
        setBackgroundResource(resource)
    }

    override fun attachPlayer(player: LitePlayerView) {
        player.addView(
            this, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    override fun getView() = this

    override fun show() {
        if (!isShowing()) {
            MediaLogger.d("show cover")
            alpha = 1f
        }
    }

    override fun hide() {
        if (isShowing()) {
            MediaLogger.d("hide cover")
            animate().alpha(0f).start()
        }
    }

    override fun isShowing() = alpha == 1f

    override fun getPlayerStateChangedListener() = object : SimplePlayerStateChangedListener() {

        override fun onRenderFirstFrame() {
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