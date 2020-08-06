package com.seagazer.sample.example

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.AspectRatio
import com.seagazer.liteplayer.config.PlayerType
import com.seagazer.liteplayer.listener.SimplePlayerStateChangedListener
import com.seagazer.liteplayer.widget.LiteGestureController
import com.seagazer.liteplayer.widget.LiteMediaController
import com.seagazer.liteplayer.widget.LiteMediaTopbar
import com.seagazer.sample.*
import com.seagazer.sample.cache.VideoCacheHelper
import com.seagazer.sample.data.DataProvider
import com.seagazer.sample.widget.ErrorOverlay
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
        showConfigInfo()

        // show progress
        player_view.setProgressColor(resources.getColor(R.color.colorAccent), Color.YELLOW)
        progress_controller.setOnCheckedChangeListener { _, isChecked ->
            player_view.displayProgress(isChecked)
        }
        progress_controller.isChecked = true
        // sensor auto turn around
        sensor.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                player_view.setAutoSensorEnable(true)
            } else {
                player_view.setAutoSensorEnable(false)
            }
        }
        // video aspect ratio
        aspect_ratio.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.ratio_4_3 -> player_view.setAspectRatio(AspectRatio.W_4_3)
                R.id.ratio_16_9 -> player_view.setAspectRatio(AspectRatio.W_16_9)
                R.id.ratio_21_9 -> player_view.setAspectRatio(AspectRatio.W_21_9)
                R.id.ratio_fill -> player_view.setAspectRatio(AspectRatio.FILL)
                R.id.ratio_stretch -> player_view.setAspectRatio(AspectRatio.STRETCH)
                R.id.ratio_origin -> player_view.setAspectRatio(AspectRatio.ORIGIN)
                R.id.ratio_auto -> player_view.setAspectRatio(AspectRatio.AUTO)
            }
        }
        // decode mode, only ijkplayer support software decode
        soft_decode.setOnCheckedChangeListener { _, isChecked ->
            player_view.supportSoftwareDecode(isChecked)
        }
        // config
        player_view.setRenderType(ConfigHolder.renderType)
        player_view.setPlayerType(ConfigHolder.playerType)
        // prepare video
        player_view.setDataSource(DataSource(urls[currentPlayIndex].first, urls[currentPlayIndex].second))
        // media controller, topbar and gesture controller
        player_view.attachMediaController(LiteMediaController(this))
        player_view.attachMediaTopbar(LiteMediaTopbar(this))
        player_view.attachGestureController(LiteGestureController(this).apply {
            supportSeek = true
            supportBrightness = true
            supportVolume = true
        })
        // custom loading overlay
        player_view.attachOverlay(LoadingOverlay(this))
        // custom error overlay
        player_view.attachOverlay(ErrorOverlay(this))
        player_view.setAutoHideOverlay(true)
        // add event listener
        player_view.addPlayerStateChangedListener(object : SimplePlayerStateChangedListener() {

            override fun onPlaying() {
                pause_resume.text = "pause"
            }

            override fun onPaused() {
                pause_resume.text = "resume"
            }

            override fun onCompleted() {
                playNext()
            }

            override fun onError(playerType: PlayerType, errorCode: Int) {
                toastShort("播放错误: $errorCode")
            }

        })
        // start play
        player_view.start()

    }

    fun pausePlay(view: View) {
        if (player_view.isPlaying()) {
            player_view.pause(true)
        } else {
            player_view.resume()
        }
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
