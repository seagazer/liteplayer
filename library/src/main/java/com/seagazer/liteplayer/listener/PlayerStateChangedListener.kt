package com.seagazer.liteplayer.listener

import com.seagazer.liteplayer.bean.IDataSource
import com.seagazer.liteplayer.config.PlayerType

/**
 *
 * Author: Seagazer
 * Date: 2020/6/28
 */
interface PlayerStateChangedListener {
    fun onPrepared(dataSource: IDataSource)
    fun onPlaying()
    fun onPaused()
    fun onStopped()
    fun onCompleted()
    fun onIdle()
    fun onError(playerType: PlayerType, errorCode: Int)
    fun onLoadingStarted()
    fun onLoadingCompleted()
    fun onBufferUpdate(bufferedPercentage: Int)
    fun onSeekStarted()
    fun onSeekCompleted()

    fun onRenderFirstFrame()
    fun onVideoSizeChanged(width: Int, height: Int)
}