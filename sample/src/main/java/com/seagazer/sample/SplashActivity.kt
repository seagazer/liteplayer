package com.seagazer.sample

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_splash.*

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        setContentView(R.layout.activity_splash)
        logo.addGradientAnimListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                actionDelay({
                    navigationTo(MainActivity::class.java)
                    finish()
                }, 500)
            }
        })
        val anim = AnimatorSet()
        val alpha = ObjectAnimator.ofFloat(icon, "alpha", 0f, 1f)
        val translationY = ObjectAnimator.ofFloat(icon, "translationY", 100f, 0f)
        anim.playTogether(alpha, translationY)
        anim.duration = 1500
        anim.start()
    }
}
