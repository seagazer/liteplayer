package com.seagazer.liteplayer.widget

import com.seagazer.liteplayer.bean.DataSource

/**
 *
 * Author: Seagazer
 * Date: 2020/6/30
 */
interface IController : IOverlay {

    fun onPlayerPrepared(dataSource: DataSource)

    fun onProgressChanged(progress: Int, secondProgress: Int)

    fun onStarted()

    fun onPaused()

    fun reset()

}