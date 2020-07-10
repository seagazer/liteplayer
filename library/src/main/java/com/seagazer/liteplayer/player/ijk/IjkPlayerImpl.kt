package com.seagazer.liteplayer.player.ijk

import android.content.Context
import android.view.Surface
import android.view.SurfaceHolder
import androidx.lifecycle.MutableLiveData
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.PlayerState
import com.seagazer.liteplayer.event.PlayerStateEvent
import com.seagazer.liteplayer.player.IPlayer
import com.seagazer.liteplayer.player.IPlayerCore

/**
 * Ijk decoder.
 *
 * Author: Seagazer
 * Date: 2020/7/1
 */
class IjkPlayerImpl constructor(val context: Context) : IPlayer {
    override fun registerStateObserver(liveData: MutableLiveData<PlayerStateEvent>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPlayer(): IPlayerCore? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setSurfaceHolder(surfaceHolder: SurfaceHolder) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setSurface(surface: Surface) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setDataSource(source: DataSource) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun start() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun start(startPosition: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun pause(fromUser: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun resume() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun seekTo(position: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stop() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun reset() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun destroy() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getVideoWidth(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getVideoHeight(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDuration(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isPlaying(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBufferedPercentage(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCurrentPosition(): Long {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setPlaySpeed(speed: Float) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setVolume(volume: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setPlayerState(state: PlayerState) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPlayerState(): PlayerState {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}