package com.seagazer.liteplayer.list

/**
 *
 * Author: Seagazer
 * Date: 2020/9/4
 */
interface ListItemChangedListener {

    fun onDetachItemView(oldPosition: Int)
    fun onAttachItemView(newPosition: Int)

}