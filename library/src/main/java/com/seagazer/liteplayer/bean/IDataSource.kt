package com.seagazer.liteplayer.bean

/**
 * DataSource to hold media info.
 *
 * Author: Seagazer
 * Date: 2020/8/26
 */
interface IDataSource {
    var mediaUrl: String
    var mediaTitle: String
    var mediaAuthor: String
    var mediaPoster: String
    var mediaDesc: String
    var rawId: Int
}