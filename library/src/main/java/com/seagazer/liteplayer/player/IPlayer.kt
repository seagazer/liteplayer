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

    /**
     * Add a liveData to observe the state changed of player.
     * @param liveData The liveData to observe player state.
     */
    fun registerStateObserver(liveData: MutableLiveData<PlayerStateEvent>)

    /**
     * Get a player core instance.
     * @return The instance of player core.
     */
    fun getPlayer(): IPlayer?

    /**
     * Set a surfaceHolder to render video.
     * @param surfaceHolder The surfaceHolder to render video
     */
    fun setSurfaceHolder(surfaceHolder: SurfaceHolder)

    /**
     * Set a surface to render video.
     * @param surface The surface to render video
     */
    fun setSurface(surface: Surface)

    /**
     * Set decode mode.
     * @param softwareDecode True software decode, false mediacodec decode
     */
    fun supportSoftwareDecode(softwareDecode: Boolean)

}