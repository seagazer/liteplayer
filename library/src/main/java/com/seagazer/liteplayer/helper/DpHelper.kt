package com.seagazer.liteplayer.helper

import android.content.Context
import android.util.TypedValue

/**
 *
 * Author: Seagazer
 * Date: 2020/7/1
 */
object DpHelper {

    fun dp2px(context: Context, dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()
    }
}