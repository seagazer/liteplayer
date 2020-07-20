package com.seagazer.sample.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.AspectRatio
import com.seagazer.liteplayer.config.PlayerType
import com.seagazer.liteplayer.config.RenderType
import com.seagazer.liteplayer.listener.SimplePlayerStateChangedListener
import com.seagazer.liteplayer.widget.LiteGestureController
import com.seagazer.liteplayer.widget.LiteMediaController
import com.seagazer.liteplayer.widget.LiteMediaTopbar
import com.seagazer.sample.R
import com.seagazer.sample.widget.LoadingOverlay
import kotlinx.android.synthetic.main.activity_aspect_ratio.*
import kotlinx.android.synthetic.main.activity_player.player_view

/**
 * Example for set aspectRatio.
 */
class AspectRatioActivity : AppCompatActivity() {

    private val url1 = "https://vfx.mtime.cn/Video/2019/03/19/mp4/190319212559089721.mp4"
    private val url2 = "https://vfx.mtime.cn/Video/2019/03/09/mp4/190309153658147087.mp4"
    private val urls = listOf(Pair(url1, "玩具总动员"), Pair(url2, "New Story"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aspect_ratio)
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
        player_view.attachMediaController(LiteMediaController(this))
        player_view.attachGestureController(LiteGestureController(this))
        player_view.attachMediaTopbar(LiteMediaTopbar(this))
        player_view.attachOverlay(LoadingOverlay(this).apply { show() })
        player_view.setRenderType(RenderType.TYPE_SURFACE_VIEW)
        player_view.setPlayerType(PlayerType.TYPE_EXO_PLAYER)
        player_view.setDataSource(DataSource(urls[0].first, urls[0].second))
        player_view.start()
        player_view.addPlayerStateChangedListener(object : SimplePlayerStateChangedListener() {
            override fun onCompleted() {
                playNext()
            }
        })
    }

    private fun playNext() {
        if (player_view.getDataSource()?.mediaUrl == url1) {
            player_view.setDataSource(DataSource(urls[1].first, urls[1].second))
        } else {
            player_view.setDataSource(DataSource(urls[0].first, urls[0].second))
        }
        player_view.start()
    }

    override fun onBackPressed() {
        if (player_view.isFullScreen()) {
            player_view.setFullScreenMode(false)
        } else {
            super.onBackPressed()
        }
    }
}
