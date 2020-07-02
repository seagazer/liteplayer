package com.seagazer.liteplayer.event

import com.seagazer.liteplayer.config.RenderState

/**
 *
 * Author: Seagazer
 * Date: 2020/6/20
 */
data class RenderStateEvent(
    val renderState: RenderState,
    var surfaceWidth: Int = 0, var surfaceHeight: Int = 0
)