package com.seagazer.liteplayer.player

import android.view.Surface
import android.view.SurfaceHolder
import androidx.lifecycle.MutableLiveData
import com.seagazer.liteplayer.event.PlayerStateEvent

/**
 *
 * Author: Seagazer
 * Date: 2020/6/19
 */
interface IPlayer : IPlayerCore {

    fun registerStateObserver(liveData: MutableLiveData<PlayerStateEvent>)

    fun getPlayer(): IPlayerCore?

    fun setSurfaceHolder(surfaceHolder: SurfaceHolder)
    fun setSurface(surface: Surface)

}