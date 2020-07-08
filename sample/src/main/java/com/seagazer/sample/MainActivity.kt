package com.seagazer.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.seagazer.sample.example.AspectRatioActivity
import com.seagazer.sample.example.PlayerActivity
import com.seagazer.sample.example.PlayerListActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun player(view: View) {
        navigationTo(PlayerActivity::class.java)
    }

    fun listPlayer(view: View) {
        navigationTo(PlayerListActivity::class.java)
    }

    fun aspectRatio(view: View) {
        navigationTo(AspectRatioActivity::class.java)
    }
}
