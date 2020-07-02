package com.seagazer.liteplayer.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.seagazer.liteplayer.R
import com.seagazer.liteplayer.bean.DataSource
import com.seagazer.liteplayer.helper.DpHelper
import com.seagazer.liteplayer.listener.PlayerStateChangedListener

/**
 * Top bar overlay for lite player to display media info.
 *
 * Author: Seagazer
 * Date: 2020/6/30
 */
class LiteMediaTopbar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ITopbar {

    private lateinit var player: LitePlayerView
    private var back: ImageView
    private var title: TextView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundResource(R.drawable.bg_lite_top_bar)
        setPadding(DpHelper.dp2px(context, 8f), DpHelper.dp2px(context, 8f), DpHelper.dp2px(context, 8f), DpHelper.dp2px(context, 8f))

        LayoutInflater.from(context).inflate(R.layout.lite_top_bar, this, true)
        back = findViewById(R.id.lite_top_bar_back)
        title = findViewById(R.id.lite_top_bar_title)
        back.setOnClickListener {
            if (player.isFullScreen) {
                player.setFullScreenMode(false)
            } else {
                if (context is Activity) {
                    context.onBackPressed()
                }
            }
        }
    }

    override fun attachPlayer(player: LitePlayerView) {
        this.player = player
        this.player.addView(this, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP
        })
    }

    override fun onPlayerPrepared(dataSource: DataSource) {
        title.text = dataSource.mediaTitle
    }

    override fun getView() = this

    override fun show() {
        translationY = 0f
        visibility = View.VISIBLE
    }

    override fun hide() {
        animate().translationY(-height.toFloat()).setDuration(300).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                visibility = View.INVISIBLE
            }
        }).start()
    }

    override fun isShowing() = visibility == View.VISIBLE

    override fun getPlayerStateChangedListener(): PlayerStateChangedListener? = null

    override fun displayModeChanged(isFullScreen: Boolean) {
    }


}