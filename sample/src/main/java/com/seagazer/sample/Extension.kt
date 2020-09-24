package com.seagazer.sample

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.widget.Toast
import com.seagazer.liteplayer.config.PlayerType
import com.seagazer.liteplayer.config.RenderType

/**
 *
 * Author: Seagazer
 * Date: 2020/6/13
 */
fun Context.toastShort(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Context.toastLong(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}

fun Activity.navigationTo(clazz: Class<*>) {
    val intent = Intent(this, clazz)
    startActivity(intent)
}

fun Activity.actionDelay(action: () -> Unit, delay: Long) {
    Handler().postDelayed({ action() }, delay)
}

fun Activity.showConfigInfo() {
    val playerText = when (ConfigHolder.playerType) {
        PlayerType.TYPE_MEDIA_PLAYER -> "MediaPlayer"
        PlayerType.TYPE_IJK_PLAYER -> "IjkPlayer"
        PlayerType.TYPE_EXO_PLAYER -> "ExoPlayer"
        else -> "CustomPlayer"
    }
    val renderText = when (ConfigHolder.renderType) {
        RenderType.TYPE_SURFACE_VIEW -> "SurfaceView"
        RenderType.TYPE_TEXTURE_VIEW -> "TextureView"
    }
    actionDelay({
        toastLong("当前内核:$playerText, 当前渲染:$renderText")
    }, 1000)
}
