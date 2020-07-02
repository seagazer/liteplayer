package com.seagazer.liteplayer.helper

/**
 *
 * Author: Seagazer
 * Date: 2020/6/28
 */
object TimeConverter {

    /**
     * @param time milliseconds
     * @return display format like [5:15:18]
     */
    fun timeToString(time: Long): String {
        var seconds: Int = (time / 1000).toInt()
        var minute: Int = seconds / 60
        seconds %= 60
        var hour = 0
        if (minute > 60) {
            hour = minute / 60
            minute %= 60
        }
        var result: String
        val resultMinute: String = if (minute < 10) "0$minute" else minute.toString()
        val resultSecond: String = if (seconds < 10) "0$seconds" else seconds.toString()
        result = if (hour != 0) {
            "$hour:$resultMinute:$resultSecond"
        } else {
            "$resultMinute:$resultSecond"
        }
        return result
    }
}