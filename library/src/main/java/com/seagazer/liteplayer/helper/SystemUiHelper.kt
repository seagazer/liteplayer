package com.seagazer.liteplayer.helper

import android.content.Context

/**
 * Helper to get dimension for system ui.
 *
 * Author: Seagazer
 * Date: 2020/7/22
 */
object SystemUiHelper {

    fun getStatusBarHeight(context: Context): Int {
        var statusBarHeight = -1
        val resourceId = context.resources
            .getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }

}