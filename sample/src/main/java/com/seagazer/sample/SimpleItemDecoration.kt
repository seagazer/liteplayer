package com.seagazer.sample

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Author: Seagazer
 * Date: 2020/4/27
 */
open class SimpleItemDecoration constructor(private val left: Int, private val top: Int, private val right: Int, private val bottom: Int) :
    RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.set(left, top, right, bottom)
    }
}