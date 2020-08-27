package com.seagazer.liteplayer.pip

import com.seagazer.liteplayer.LitePlayerView
import com.seagazer.liteplayer.config.FloatSize

/**
 *
 * Author: Seagazer
 * Date: 2020/8/20
 */
interface IFloatWindow {

    /**
     * Display float window.
     */
    fun enterFloatWindow()

    /**
     * Close float window.
     */
    fun exitFloatWindow()

    /**
     * Callback when this floatWindow attach to LitePlayerView.
     */
    fun attachPlayer(litePlayerView: LitePlayerView)

    /**
     * Detach player view from float window container.
     */
    fun detachFromFloatWindow()

    /**
     * Change size of float window view.
     * @param sizeMode Large or normal.
     */
    fun refreshFloatWindowSize(sizeMode: FloatSize)

    /**
     * Check current state is float window or not.
     * @return True float window, false otherwise.
     */
    fun isFloatWindow(): Boolean
}