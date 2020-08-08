package com.seagazer.liteplayer.bean

/**
 * DataSource to hold media info.
 *
 * Author: Seagazer
 * Date: 2020/6/19
 */
data class DataSource(
    val mediaUrl: String, val mediaTitle: String = "",
    val mediaAuthor: String = "", val mediaPoster: String = "",
    val mediaDesc: String = ""
) {
    var rawId: Int = -1
}