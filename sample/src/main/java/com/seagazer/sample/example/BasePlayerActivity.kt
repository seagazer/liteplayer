package com.seagazer.sample.example

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.PlayerType
import com.seagazer.liteplayer.config.RenderType
import com.seagazer.liteplayer.listener.SimplePlayerStateChangedListener
import com.seagazer.liteplayer.widget.LiteGestureController
import com.seagazer.liteplayer.widget.LiteMediaController
import com.seagazer.liteplayer.widget.LiteMediaTopbar
import com.seagazer.sample.R
import com.seagazer.sample.cache.VideoCacheHelper
import com.seagazer.sample.data.DataProvider
import com.seagazer.sample.navigationTo
import com.seagazer.sample.toastShort
import com.seagazer.sample.widget.LoadingOverlay
import kotlinx.android.synthetic.main.activity_base_player.*


/**
 * Example for base use.
 */
class BasePlayerActivity : AppCompatActivity() {

    private val urls =
        listOf(
            Pair(VideoCacheHelper.url(DataProvider.url1), "玩具总动员"), Pair(VideoCacheHelper.url(DataProvider.url2), "New Story")
            , Pair(DataProvider.url3, "RTMP")
        )
    private var currentPlayIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_player)
        // progress
        player_view.setProgressColor(resources.getColor(R.color.colorAccent), resources.getColor(R.color.colorPrimaryDark))
        progress_controller.setOnCheckedChangeListener { _, isChecked ->
            player_view.displayProgress(isChecked)
        }
        // sensor auto turn around
        sensor.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                player_view.setAutoSensorEnable(true)
            } else {
                player_view.setAutoSensorEnable(false)
            }
        }
        // player config
        player_config.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.player_exo -> {
                    player_view.setPlayerType(PlayerType.TYPE_EXO_PLAYER)
                    player_view.setDataSource(DataSource(urls[currentPlayIndex].first, urls[currentPlayIndex].second))
                    player_view.start()
                }
                R.id.player_media -> {
                    player_view.setPlayerType(PlayerType.TYPE_MEDIA_PLAYER)
                    player_view.setDataSource(DataSource(urls[currentPlayIndex].first, urls[currentPlayIndex].second))
                    player_view.start()
                }
            }
        }
        // render view
        render_config.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.render_surface_view -> player_view.setRenderType(RenderType.TYPE_SURFACE_VIEW)
                R.id.render_texture_view -> player_view.setRenderType(RenderType.TYPE_TEXTURE_VIEW)
            }
        }
        player_view.setRenderType(RenderType.TYPE_SURFACE_VIEW)
        // player type
        player_view.setPlayerType(PlayerType.TYPE_EXO_PLAYER)
        // prepare video
        player_view.setDataSource(DataSource(urls[currentPlayIndex].first, urls[currentPlayIndex].second))
        // media controller, topbar and gesture controller
        player_view.attachMediaController(LiteMediaController(this))
        player_view.attachMediaTopbar(LiteMediaTopbar(this))
        player_view.attachGestureController(LiteGestureController(this))
        // custom loading overlay
        player_view.attachOverlay(LoadingOverlay(this).apply { show() })
        player_view.setAutoHideOverlay(false)
        player_view.start()
        player_view.addPlayerStateChangedListener(object : SimplePlayerStateChangedListener() {

            override fun onCompleted() {
                playNext()
            }

            override fun onError(playerType: PlayerType, errorCode: Int) {
                toastShort("播放错误: $errorCode")
            }

            override fun onLoadingStarted() {
                toastShort("正在缓冲")
            }

            override fun onLoadingCompleted() {
                toastShort("缓冲结束")
            }

        })

    }

    fun pausePlay(view: View) {
        if (player_view.isPlaying()) {
            player_view.pause(true)
        } else {
            player_view.resume()
        }
    }

    fun stopPlay(view: View) {
        player_view.stop()
    }

    fun playNext(view: View) {
        playNext()
    }

    private fun playNext() {
        currentPlayIndex++
        if (currentPlayIndex > urls.size - 1) {
            currentPlayIndex = 0
        }
        player_view.setDataSource(DataSource(urls[currentPlayIndex].first, urls[currentPlayIndex].second))
        player_view.start()
    }

    override fun onBackPressed() {
        if (player_view.isFullScreen()) {
            player_view.setFullScreenMode(false)
        } else {
            super.onBackPressed()
        }
    }

    fun jumpToActivity(view: View) {
        navigationTo(EmptyActivity::class.java)
    }

}
