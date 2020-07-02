package com.seagazer.liteplayer.listener

import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.PlayerType

/**
 *
 * Author: Seagazer
 * Date: 2020/6/28
 */
open class SimplePlayerStateChangedListener : PlayerStateChangedListener {
    override fun onPrepared(dataSource: DataSource) {
    }

    override fun onPlaying() {
    }

    override fun onPaused() {
    }

    override fun onStopped() {
    }

    override fun onCompleted() {
    }

    override fun onIdle() {
    }

    override fun onError(playerType: PlayerType, errorCode: Int) {
    }

    override fun onLoadingStarted() {
    }

    override fun onLoadingCompleted() {
    }

    override fun onBufferUpdate(bufferedPercentage: Int) {
    }

    override fun onSeekStarted() {
    }

    override fun onSeekCompleted() {
    }

    override fun onRenderFirstFrame() {
    }

    override fun onVideoSizeChanged(width: Int, height: Int) {
    }
}