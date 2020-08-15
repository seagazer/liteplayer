package com.seagazer.liteplayer.listener

/**
 *
 * Author: Seagazer
 * Date: 2020/8/15
 */
interface RenderStateChangedListener {
    fun onSurfaceCreated()
    fun onSurfaceChanged()
    fun onSurfaceDestroy()
}