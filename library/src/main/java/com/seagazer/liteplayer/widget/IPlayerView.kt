package com.seagazer.liteplayer.widget

import androidx.lifecycle.MutableLiveData
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.AspectRatio
import com.seagazer.liteplayer.config.FloatSize
import com.seagazer.liteplayer.config.PlayerType
import com.seagazer.liteplayer.config.RenderType
import com.seagazer.liteplayer.event.PlayerStateEvent
import com.seagazer.liteplayer.event.RenderStateEvent
import com.seagazer.liteplayer.listener.PlayerStateChangedListener
import com.seagazer.liteplayer.listener.RenderStateChangedListener
import com.seagazer.liteplayer.pip.IFloatWindow
import com.seagazer.liteplayer.player.IPlayer
import com.seagazer.liteplayer.player.IPlayerCore
import com.seagazer.liteplayer.render.IRender

/**
 *
 * Author: Seagazer
 * Date: 2020/6/20
 */
interface IPlayerView : IPlayerCore {
    /**
     * You should not call this method, otherwise you should handle all state of player.
     * If you want to listen player state, you should call {@link addPlayerStateChangedListener(PlayerStateChangedListener)} instead.
     */
    fun registerPlayerStateObserver(liveData: MutableLiveData<PlayerStateEvent>)

    /**
     * You should not call this method, otherwise you should handle all state of render surface.
     */
    fun registerRenderStateObserver(liveData: MutableLiveData<RenderStateEvent>)

    /**
     * Add a listener to listen the state changed of player.
     * @param listener The listener to listen player state.
     */
    fun addPlayerStateChangedListener(listener: PlayerStateChangedListener)

    /**
     * Add a listener to listen the state changed of render.
     * @param listener The listener to listen render state.
     */
    fun addRenderStateChangedListener(listener: RenderStateChangedListener)

    /**
     * Set a type to display the picture of video.
     * @param renderType TYPE_SURFACE_VIEW or TYPE_TEXTURE_VIEW
     */
    fun setRenderType(renderType: RenderType)

    /**
     * Set the aspectRatio to display the picture of video.
     * @param aspectRatio  AUTO, ORIGIN, FILL, STRETCH, W_16_9, W_4_3, W_21_9
     */
    fun setAspectRatio(aspectRatio: AspectRatio)

    /**
     * Set core player to do the play logic.
     * @param playerType TYPE_EXO_PLAYER, TYPE_IJK_PLAYER or TYPE_MEDIA_PLAYER
     */
    fun setPlayerType(playerType: PlayerType)

    /**
     * Get a player core instance.
     * @return The instance of player core.
     */
    fun getPlayer(): IPlayer?

    /**
     * Get a render instance.
     * @return The instance of render.
     */
    fun getRender(): IRender?

    /**
     * Attach an overlay to control the player.
     *
     * @param controller IController to be attached.
     */
    fun attachMediaController(controller: IController)

    /**
     * Attach an overlay to show the title of player.
     *
     * @param topbar ITopbar to be attached.
     */
    fun attachMediaTopbar(topbar: ITopbar)

    /**
     * Attach an overlay to handle the gesture to control player.
     *
     * @param gestureOverlay IGesture to be attached.
     */
    fun attachGestureController(gestureOverlay: IGesture)

    /**
     * Attach a custom overlay to do something when playing.
     *
     * @param overlay IOverlay to be attached.
     */
    fun attachOverlay(overlay: IOverlay)

    /**
     * Attach a float window to handle float window display mode.
     *
     * @param floatWindow FloatWindow to be attached.
     */
    fun attachFloatWindow(floatWindow: IFloatWindow)

    /**
     * Get current playing data source.
     * @return The current playing data source.
     */
    fun getDataSource(): DataSource?

    /**
     * Set visibility of inner progressbar.
     * @param showProgress True show, false hide.
     */
    fun displayProgress(showProgress: Boolean)

    /**
     * Set color of inner progressbar.
     * @param progressColor Color of progress.
     * @param secondProgressColor Color of second progress.
     */
    fun setProgressColor(progressColor: Int, secondProgressColor: Int)

    /**
     * Enter or exit fullscreen.
     * @param isFullScreen True enter fullscreen, false otherwise.
     */
    fun setFullScreenMode(isFullScreen: Boolean)

    /**
     * Check current state is fullscreen or not.
     * @return True fullscreen, false otherwise.
     */
    fun isFullScreen(): Boolean

    /**
     * Enter or exit float window.
     * @param isFloatWindow True enter float window, false otherwise.
     */
    fun setFloatWindowMode(isFloatWindow: Boolean)

    /**
     * Check current state is float window or not.
     * @return True float window, false otherwise.
     */
    fun isFloatWindow(): Boolean

    /**
     * Set auto enter or exit fullscreen mode by sensor.
     * @param enable True enable, false otherwise.
     */
    fun setAutoSensorEnable(enable: Boolean)

    /**
     * Set decode mode.
     * @param softwareDecode True software decode, false mediacodec decode
     */
    fun supportSoftwareDecode(softwareDecode: Boolean)

    /**
     * Set float window size.
     * @param sizeMode FloatSize.NORMAL or FloatSize.LARGE
     */
    fun setFloatSizeMode(sizeMode: FloatSize)

    /**
     * Set repeat play when completed or not.
     * @param repeat True replay after completed, false stop after completed.
     */
    fun setRepeatMode(repeat: Boolean)

}