package com.seagazer.liteplayer.listener

import android.view.View
import com.seagazer.liteplayer.LitePlayerView
import com.seagazer.liteplayer.widget.IOverlay

/**
 *
 * Author: Seagazer
 * Date: 2020/8/8
 */
open class SimpleOverlayObserver : IOverlay {
    override fun attachPlayer(player: LitePlayerView) {
    }

    override fun getView(): View? {
        return null
    }

    override fun show() {
    }

    override fun hide() {
    }

    override fun isShowing(): Boolean {
        return false
    }

    override fun getPlayerStateChangedListener(): PlayerStateChangedListener? = null

    override fun getRenderStateChangedListener(): RenderStateChangedListener? = null

    override fun displayModeChanged(isFullScreen: Boolean) {
    }

    override fun autoSensorModeChanged(isAutoSensor: Boolean) {
    }

    override fun floatWindowModeChanged(isFloatWindow: Boolean) {
    }

}