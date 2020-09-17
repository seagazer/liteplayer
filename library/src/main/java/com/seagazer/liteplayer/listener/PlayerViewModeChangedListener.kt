package com.seagazer.liteplayer.listener

/**
 *
 * Author: Seagazer
 * Date: 2020/9/9
 */
interface PlayerViewModeChangedListener {
    fun onFullScreenModeChanged(isFullScreen: Boolean)
    fun onFloatWindowModeChanged(isFloatWindow: Boolean)
    fun onAutoSensorModeChanged(isAutoSensor: Boolean)
}