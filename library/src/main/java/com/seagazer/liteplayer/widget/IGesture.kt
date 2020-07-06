package com.seagazer.liteplayer.widget

import android.view.MotionEvent

/**
 *
 * Author: Seagazer
 * Date: 2020/7/3
 */
interface IGesture : IOverlay {
    fun onDown(e: MotionEvent?)

    fun onShowPress(e: MotionEvent?)

    fun onSingleTapUp(e: MotionEvent?)

    fun onSingleTapConfirmed(e: MotionEvent?)

    fun onDoubleTap(e: MotionEvent?)

    fun onLongPress(e: MotionEvent?)

    fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float)

    fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float)

    fun onGestureFinish(e: MotionEvent?)
}