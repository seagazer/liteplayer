package com.seagazer.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

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
}
