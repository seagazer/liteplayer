package com.seagazer.sample.data

import com.seagazer.sample.R

/**
 *
 * Author: Seagazer
 * Date: 2020/7/16
 */
object DataProvider {
    const val url1 = "https://vfx.mtime.cn/Video/2019/03/19/mp4/190319212559089721.mp4"
    const val url2 = "https://vfx.mtime.cn/Video/2019/03/09/mp4/190309153658147087.mp4"
    const val url3 = "rtmp://58.200.131.2:1935/livetv/hunantv"// Rtmp resource
    const val url4 = "file:///android_asset/assets.mp4"// Assets resource
    const val url5 = R.raw.raw// Raw resource

    val urls = arrayListOf(url1, url2)
}