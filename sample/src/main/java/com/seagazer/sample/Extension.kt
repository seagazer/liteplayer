package com.seagazer.sample

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.widget.Toast

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
