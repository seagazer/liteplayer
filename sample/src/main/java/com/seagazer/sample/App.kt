package com.seagazer.sample

import android.app.Application
import com.seagazer.sample.cache.VideoCacheHelper

/**
 *
 * Author: Seagazer
 * Date: 2020/7/11
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        VideoCacheHelper.init(this)
    }
}