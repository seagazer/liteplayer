package com.seagazer.liteplayer.player

import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.PlayerState

/**
 *
 * Author: Seagazer
 * Date: 2020/6/29
 */
interface IPlayerCore {
    fun setDataSource(source: DataSource)
    fun start()
    fun start(startPosition: Long)
    fun pause(fromUser: Boolean)
    fun resume()
    fun seekTo(position: Long)
    fun stop()
    fun reset()
    fun destroy()

    fun getVideoWidth(): Int
    fun getVideoHeight(): Int

    fun getDuration(): Long
    fun isPlaying(): Boolean
    fun getBufferedPercentage(): Int
    fun getCurrentPosition(): Long
    fun setPlaySpeed(speed: Float)
    fun setVolume(volume: Int)

    fun setPlayerState(state: PlayerState)
    fun getPlayerState(): PlayerState
}