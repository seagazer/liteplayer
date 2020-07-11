package com.seagazer.sample.cache

import android.content.Context
import com.danikula.videocache.HttpProxyCacheServer

/**
 * Helper to use AndroidVideoCache to cache video.
 *
 * Author: Seagazer
 * Date: 2020/7/10
 */
object VideoCacheHelper {

    private lateinit var server: HttpProxyCacheServer
    private var isInit = false

    fun isInit() = isInit

    fun init(context: Context) {
        if (isInit) {
            return
        }
        server = HttpProxyCacheServer(context.applicationContext)
        isInit = true
    }

    fun url(url: String): String {
        return server.getProxyUrl(url)
    }
}