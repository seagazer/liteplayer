package com.seagazer.liteplayer.helper

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.lang.ref.WeakReference
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * Helper to handle system accelerometer sensor event.
 *
 * Author: Seagazer
 * Date: 2020/6/30
 */
class OrientationSensorHelper(context: Context) {
    private var activityReference: WeakReference<Activity>? = null

    private val sensorListener = object : SensorEventListener {
        val DATA_X = 0
        val DATA_Y = 1
        val DATA_Z = 2
        val ORIENTATION_UNKNOWN = -1

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

        override fun onSensorChanged(event: SensorEvent?) {
            val values = event!!.values
            var orientation = ORIENTATION_UNKNOWN
            val x = -values[DATA_X]
            val y = -values[DATA_Y]
            val z = -values[DATA_Z]
            val magnitude = x * x + y * y
            // Don't trust the angle if the magnitude is small compared to the y value
            if (magnitude * 4 >= z * z) {
                val oneEightyOverPi = 57.29577957855f
                val angle = atan2((-y).toDouble(), x.toDouble()) * oneEightyOverPi
                orientation = 90 - angle.roundToInt()
                // normalize to 0 - 359 range
                while (orientation >= 360) {
                    orientation -= 360
                }
                while (orientation < 0) {
                    orientation += 360
                }
            }
            activityReference?.let {
                it.get()?.run {
                    if (orientation in 46..134) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    } else if (orientation in 136..224) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    } else if (orientation in 226..314) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    } else if ((orientation in 316..359) || (orientation in 1..44)) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                }
            }
        }
    }

    private val fullScreenSensorListener = object : SensorEventListener {
        val DATA_X = 0
        val DATA_Y = 1
        val DATA_Z = 2
        val ORIENTATION_UNKNOWN = -1

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

        override fun onSensorChanged(event: SensorEvent?) {
            val values = event!!.values
            var orientation = ORIENTATION_UNKNOWN
            val x = -values[DATA_X]
            val y = -values[DATA_Y]
            val z = -values[DATA_Z]
            val magnitude = x * x + y * y
            // Don't trust the angle if the magnitude is small compared to the y value
            if (magnitude * 4 >= z * z) {
                val oneEightyOverPi = 57.29577957855f
                val angle = atan2((-y).toDouble(), x.toDouble()) * oneEightyOverPi
                orientation = 90 - angle.roundToInt()
                // normalize to 0 - 359 range
                while (orientation >= 360) {
                    orientation -= 360
                }
                while (orientation < 0) {
                    orientation += 360
                }
            }
            activityReference?.let {
                it.get()?.run {
                    if (orientation in 46..134) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    } else if (orientation in 226..314) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                }
            }
        }
    }

    private val sm: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var currentListener: SensorEventListener? = null

    fun startWatching(activity: Activity, isFullScreen: Boolean) {
        if (isFullScreen) {
            startWatching2(activity)
        } else {
            startWatching1(activity)
        }
    }

    fun startWatching(activity: Activity) {
        startWatching(activity, false)
    }


    private fun startWatching1(activity: Activity) {
        activityReference = WeakReference(activity)
        currentListener = sensorListener
        sm.registerListener(currentListener, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    private fun startWatching2(activity: Activity) {
        activityReference = WeakReference(activity)
        currentListener = fullScreenSensorListener
        sm.registerListener(currentListener, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopWatching() {
        currentListener?.run {
            sm.unregisterListener(this)
        }
    }

}