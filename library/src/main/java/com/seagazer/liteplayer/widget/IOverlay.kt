package com.seagazer.liteplayer.widget

import android.view.View
import com.seagazer.liteplayer.LitePlayerView
import com.seagazer.liteplayer.listener.PlayerStateChangedListener

/**
 * Base action for overlay cover like controller and topbar.
 *
 * Author: Seagazer
 * Date: 2020/7/1
 */
interface IOverlay {
    fun attachPlayer(player: LitePlayerView)

    fun getView(): View?

    fun show()

    fun hide()

    fun isShowing(): Boolean

    fun getPlayerStateChangedListener(): PlayerStateChangedListener?

    fun displayModeChanged(isFullScreen: Boolean)

    fun autoSensorModeChanged(isAutoSensor: Boolean)

    fun floatWindowModeChanged(isFloatWindow: Boolean)

}