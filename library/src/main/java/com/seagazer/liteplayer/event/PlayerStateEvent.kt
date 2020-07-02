package com.seagazer.liteplayer.event

import com.seagazer.liteplayer.config.PlayerState

/**
 *
 * Author: Seagazer
 * Date: 2020/6/20
 */
data class PlayerStateEvent(
    val playerState: PlayerState
) {
    var errorCode: Int = -999
    var videoWidth: Int = 0
    var videoHeight: Int = 0
    var bufferedPercentage: Int = 0
}