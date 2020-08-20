package com.seagazer.sample.example

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.FloatSize
import com.seagazer.liteplayer.listener.SimpleOverlayObserver
import com.seagazer.liteplayer.pip.LiteFloatWindow
import com.seagazer.sample.ConfigHolder
import com.seagazer.sample.R
import com.seagazer.sample.data.DataProvider
import com.seagazer.sample.showConfigInfo
import kotlinx.android.synthetic.main.activity_float_window.*

/**
 * Example for float window.
 */
class FloatWindowActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_float_window)
        showConfigInfo()
        // config
        player_view.setRenderType(ConfigHolder.renderType)
        player_view.setPlayerType(ConfigHolder.playerType)
        // observer overlay
        player_view.attachOverlay(object : SimpleOverlayObserver() {

            override fun floatWindowModeChanged(isFloatWindow: Boolean) {
                if (isFloatWindow) {
                    button.text = "exit float window"
                } else {
                    button.text = "enter float window"
                }
            }

        })
        // attach a float window, you can custom your floatWindow implement IFloatWindow.
        player_view.attachFloatWindow(LiteFloatWindow(this, player_view))
        // prepare video
        player_view.setDataSource(DataSource(DataProvider.url2))
        player_view.start()
    }

    fun floatWindow(view: View) {
        if (player_view.isFloatWindow()) {
            player_view.setFloatWindowMode(false)
        } else {
            player_view.setFloatWindowMode(true)
        }
    }

    private var isNormal = true

    fun changeSize(view: View) {
        if (isNormal) {
            player_view.setFloatSizeMode(FloatSize.LARGE)
            change_size.text = "LARGE"
        } else {
            player_view.setFloatSizeMode(FloatSize.NORMAL)
            change_size.text = "NORMAL"
        }
        isNormal = !isNormal
    }
}
