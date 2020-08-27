package com.seagazer.liteplayer.bean

/**
 * DataSource to hold media info.
 *
 * Author: Seagazer
 * Date: 2020/6/19
 */
data class DataSource(
    override var mediaUrl: String = "",
    override var mediaTitle: String = "",
    override var mediaAuthor: String = "",
    override var mediaPoster: String = "",
    override var mediaDesc: String = "",
    override var rawId: Int = -1
) : IDataSource

