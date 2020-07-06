package com.seagazer.sample.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.seagazer.liteplayer.helper.DpHelper
import com.seagazer.liteplayer.listener.PlayerStateChangedListener
import com.seagazer.liteplayer.listener.SimplePlayerStateChangedListener
import com.seagazer.liteplayer.widget.IOverlay
import com.seagazer.liteplayer.widget.LitePlayerView
import com.seagazer.sample.R

/**
 * A sample to show how to make a custom overlay for loading state.
 *
 * Author: Seagazer
 * Date: 2020/7/3
 */
class LoadingOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), IOverlay {

    private lateinit var player: LitePlayerView

    init {
        setBackgroundResource(R.drawable.bg_loading_overlay)
        val padding = DpHelper.dp2px(context, 8f)
        setPadding(padding, padding, padding, padding)
        LayoutInflater.from(context).inflate(R.layout.loading_overlay, this, true)
        hide()
    }

    override fun attachPlayer(player: LitePlayerView) {
        this.player = player
        player.addView(this, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            .apply {
                gravity = Gravity.CENTER
            })
    }

    override fun getView() = this

    override fun show() {
        visibility = View.VISIBLE
    }

    override fun hide() {
        visibility = View.INVISIBLE
    }

    override fun isShowing() = visibility == View.VISIBLE

    override fun getPlayerStateChangedListener(): PlayerStateChangedListener? {
        return object : SimplePlayerStateChangedListener() {
            override fun onLoadingStarted() {
                show()
            }

            override fun onLoadingCompleted() {
                hide()
            }
        }
    }
}