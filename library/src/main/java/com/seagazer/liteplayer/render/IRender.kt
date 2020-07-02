package com.seagazer.liteplayer.render

import android.view.View
import androidx.lifecycle.MutableLiveData
import com.seagazer.liteplayer.config.AspectRatio
import com.seagazer.liteplayer.event.RenderStateEvent
import com.seagazer.liteplayer.player.IPlayer

/**
 *
 * Author: Seagazer
 * Date: 2020/6/19
 */
interface IRender {

    fun getRenderView(): View
    fun updateVideoSize(videoWidth: Int, videoHeight: Int)
    fun updateAspectRatio(aspectRatio: AspectRatio)
    fun bindPlayer(iPlayer: IPlayer)
    fun bindRenderMeasure(renderMeasure: RenderMeasure)
    fun registerStateObserver(liveData: MutableLiveData<RenderStateEvent>)
    fun release()
}