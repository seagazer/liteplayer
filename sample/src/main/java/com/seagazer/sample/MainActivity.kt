package com.seagazer.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.seagazer.sample.example.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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

    fun aspectRatio(view: View) {
        navigationTo(AspectRatioActivity::class.java)
    }

    fun multiListPlay(view: View) {
        navigationTo(MultiListPlayerActivity::class.java)
    }

    fun floatWindow(view: View) {
        navigationTo(FloatWindowActivity::class.java)
    }

    fun picInPic(view: View) {}

}
