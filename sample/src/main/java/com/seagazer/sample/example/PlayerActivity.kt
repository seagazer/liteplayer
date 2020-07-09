package com.seagazer.sample.example

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.PlayerType
import com.seagazer.liteplayer.config.RenderType
import com.seagazer.liteplayer.helper.OrientationSensorHelper
import com.seagazer.liteplayer.helper.TimeConverter
import com.seagazer.liteplayer.listener.SimplePlayerStateChangedListener
import com.seagazer.liteplayer.widget.LiteGestureController
import com.seagazer.liteplayer.widget.LiteMediaController
import com.seagazer.liteplayer.widget.LiteMediaTopbar
import com.seagazer.sample.R
import com.seagazer.sample.navigationTo
import com.seagazer.sample.toastShort
import com.seagazer.sample.widget.LoadingOverlay
import kotlinx.android.synthetic.main.activity_player.*

class PlayerActivity : AppCompatActivity() {

    private val url1 = "https://vfx.mtime.cn/Video/2019/03/19/mp4/190319212559089721.mp4"
    private val url2 = "https://vfx.mtime.cn/Video/2019/03/09/mp4/190309153658147087.mp4"
    private val urls = listOf(Pair(url1, "玩具总动员"), Pair(url2, "New Story"))
    private val msgProgress = 0x1
    private var currentPlayIndex = 0

    @SuppressLint("HandlerLeak")
    private val H: Handler = object : Handler() {
        @SuppressLint("SetTextI18n")
        override fun handleMessage(msg: Message) {
            if (msg.what == msgProgress) {
                seek_bar.progress = player_view.getCurrentPosition().toInt()
                media_info.text =
                    "${TimeConverter.timeToString(seek_bar.progress.toLong())} / ${TimeConverter.timeToString(
                        player_view.getDuration()
                    )}"
                sendEmptyMessageDelayed(msgProgress, 1000)
            }
        }
    }

    private lateinit var orientationSensorHelper: OrientationSensorHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        // seek
        seek_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player_view.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
        // progress
        progress_controller.setOnCheckedChangeListener { _, isChecked ->
            player_view.displayProgress(isChecked)
        }
        // sensor auto turn around
        orientationSensorHelper = OrientationSensorHelper(this)
        sensor.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                orientationSensorHelper.startWatching(this)
            } else {
                orientationSensorHelper.stopWatching()
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
        player_view.setAutoHideOverlay(true)

        player_view.start()
        player_view.setPlayerStateChangedListener(object : SimplePlayerStateChangedListener() {
            override fun onPrepared(dataSource: DataSource) {
                seek_bar.max = player_view.getDuration().toInt()
                H.sendEmptyMessageDelayed(msgProgress, 1000)
            }

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

            override fun onBufferUpdate(bufferedPercentage: Int) {
                seek_bar.secondaryProgress =
                    (player_view.getDuration() * bufferedPercentage * 1f / 100).toInt()
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
        currentPlayIndex = if (player_view.getDataSource()?.mediaUrl == url1) {
            1
        } else {
            0
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

    override fun onDestroy() {
        H.removeCallbacksAndMessages(null)
        orientationSensorHelper.stopWatching()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val isFullScreen = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        player_view.setFullScreenMode(isFullScreen)
    }

    fun jumpToActivity(view: View) {
        navigationTo(EmptyActivity::class.java)
    }

}
