package com.seagazer.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.seagazer.sample.example.AspectRatioActivity
import com.seagazer.sample.example.MultiListPlayerActivity
import com.seagazer.sample.example.PlayerActivity
import com.seagazer.sample.example.ListPlayerActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun player(view: View) {
        navigationTo(PlayerActivity::class.java)
    }

    fun listPlayer(view: View) {
        navigationTo(ListPlayerActivity::class.java)
    }

    fun aspectRatio(view: View) {
        navigationTo(AspectRatioActivity::class.java)
    }

    fun multiListPlay(view: View) {
        navigationTo(MultiListPlayerActivity::class.java)
    }

    fun floatWindow(view: View) {}
    fun picInPic(view: View) {}
}
