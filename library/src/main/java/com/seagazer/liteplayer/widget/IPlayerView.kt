package com.seagazer.liteplayer.widget

import androidx.lifecycle.MutableLiveData
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.AspectRatio
import com.seagazer.liteplayer.config.PlayerType
import com.seagazer.liteplayer.config.RenderType
import com.seagazer.liteplayer.event.PlayerStateEvent
import com.seagazer.liteplayer.event.RenderStateEvent
import com.seagazer.liteplayer.listener.PlayerStateChangedListener
import com.seagazer.liteplayer.player.IPlayer
import com.seagazer.liteplayer.player.IPlayerCore

/**
 *
 * Author: Seagazer
 * Date: 2020/6/20
 */
interface IPlayerView : IPlayerCore {
    /**
     * You should not call this method, otherwise you should handle all state of player.
     * If you want to listen player state, you should call {setPlayerStateChangedListener(PlayerStateChangedListener)} instead.
     */
    fun registerPlayerStateObserver(liveData: MutableLiveData<PlayerStateEvent>)
    /**
     * You should not call this method, otherwise you should handle all state of render surface.
     */
    fun registerRenderStateObserver(liveData: MutableLiveData<RenderStateEvent>)

    fun setPlayerStateChangedListener(listener: PlayerStateChangedListener)

    fun setRenderType(renderType: RenderType)
    fun setAspectRatio(aspectRatio: AspectRatio)

    fun setPlayerType(playerType: PlayerType)
    fun getPlayer(): IPlayer?

    fun attachMediaController(controller: IController)
    fun attachMediaTopbar(topbar: ITopbar)
    fun attachGestureController(gestureOverlay: IGesture)
    fun attachOverlay(overlay: IOverlay)

    fun getDataSource(): DataSource?

    fun displayProgress(showProgress: Boolean)
    fun setProgressColor(progressColor: Int, secondProgressColor: Int)

    fun setFullScreenMode(isFullScreen: Boolean)
    fun isFullScreen(): Boolean
    fun setAutoSensorEnable(enable: Boolean)

}