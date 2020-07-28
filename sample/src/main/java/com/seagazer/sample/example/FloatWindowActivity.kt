package com.seagazer.sample.example

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.config.PlayerType
import com.seagazer.liteplayer.config.RenderType
import com.seagazer.liteplayer.widget.LiteGestureController
import com.seagazer.sample.R
import com.seagazer.sample.data.DataProvider
import kotlinx.android.synthetic.main.activity_float_window.*

/**
 * Example for float window.
 */
class FloatWindowActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_float_window)
        // render type
        player_view.setRenderType(RenderType.TYPE_SURFACE_VIEW)
        // player type
        player_view.setPlayerType(PlayerType.TYPE_EXO_PLAYER)
        // prepare video
        player_view.setDataSource(DataSource(DataProvider.url2))
        player_view.attachGestureController(LiteGestureController(this))
        player_view.start()
    }

    fun enter(view: View) {
        player_view.setFloatWindowMode(true)
    }

    fun exit(view: View) {
        player_view.setFloatWindowMode(false)
    }
}
