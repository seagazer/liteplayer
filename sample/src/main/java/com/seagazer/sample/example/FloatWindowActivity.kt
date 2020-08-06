package com.seagazer.sample.example

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.widget.LiteGestureController
import com.seagazer.sample.ConfigHolder
import com.seagazer.sample.R
import com.seagazer.sample.data.DataProvider
import com.seagazer.sample.showConfigInfo
import com.seagazer.sample.widget.LoadingOverlay
import kotlinx.android.synthetic.main.activity_float_window.*

/**
 * Example for float window.
 */
class FloatWindowActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_float_window)
        showConfigInfo()
        // config
        player_view.setRenderType(ConfigHolder.renderType)
        player_view.setPlayerType(ConfigHolder.playerType)
        // prepare video
        player_view.setDataSource(DataSource(DataProvider.url2))
        player_view.start()
    }

    fun enter(view: View) {
        player_view.setFloatWindowMode(true)
    }

    fun exit(view: View) {
        player_view.setFloatWindowMode(false)
    }
}
