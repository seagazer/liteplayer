package com.seagazer.liteplayer.widget

import com.seagazer.liteplayer.bean.IDataSource

/**
 *
 * Author: Seagazer
 * Date: 2020/6/30
 */
interface IController : IOverlay {

    fun onPlayerPrepared(dataSource: IDataSource)

    fun onProgressChanged(progress: Int, secondProgress: Int)

    fun onStarted()

    fun onPaused()

    fun reset()

}