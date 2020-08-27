package com.seagazer.liteplayer.widget

import com.seagazer.liteplayer.bean.IDataSource

/**
 *
 * Author: Seagazer
 * Date: 2020/7/1
 */
interface ITopbar : IOverlay {

    fun onDataSourceChanged(dataSource: IDataSource)

}