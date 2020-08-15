package com.seagazer.sample

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.seagazer.liteplayer.config.PlayerType
import com.seagazer.liteplayer.config.RenderType
import com.seagazer.liteplayer.helper.MediaLogger
import com.seagazer.sample.example.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 强制打开logcat
        MediaLogger.openLogger()
        // 设置logcat级别为debug
        MediaLogger.setLevel(MediaLogger.Level.DEBUG)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.config, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.exo -> {
                ConfigHolder.playerType = PlayerType.TYPE_EXO_PLAYER
            }
            R.id.ijk -> {
                ConfigHolder.playerType = PlayerType.TYPE_IJK_PLAYER
            }
            R.id.media -> {
                ConfigHolder.playerType = PlayerType.TYPE_MEDIA_PLAYER
            }
            R.id.surface -> {
                ConfigHolder.renderType = RenderType.TYPE_SURFACE_VIEW
            }
            R.id.texture -> {
                ConfigHolder.renderType = RenderType.TYPE_TEXTURE_VIEW
            }
        }
        item.isChecked = true
        return super.onOptionsItemSelected(item)
    }

    fun player(view: View) {
        navigationTo(BasePlayerActivity::class.java)
    }

    fun recyclerView(view: View) {
        navigationTo(ListPlayerActivity::class.java)
    }

    fun listView(view: View) {
        navigationTo(ListPlayerActivity2::class.java)
    }

    fun multiList(view: View) {
        navigationTo(ListPlayerActivity3::class.java)
    }

    fun viewpagerPlay(view: View) {
        navigationTo(MultiListPlayerActivity::class.java)
    }

    fun floatWindow(view: View) {
        navigationTo(FloatWindowActivity::class.java)
    }
    fun picInPic(view: View) {}

}
