package io.stanc.pogotool.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

object TimeCalculator {

    private val TAG = javaClass.name

    @SuppressLint("SimpleDateFormat")
    val clock = SimpleDateFormat("HH:mm")
    @SuppressLint("SimpleDateFormat")
    val hours = SimpleDateFormat("HH")
    @SuppressLint("SimpleDateFormat")
    val minutes = SimpleDateFormat("mm")

    fun format(timestamp: Long): String {
        return clock.format(timestamp)
    }

//    TODO: need original time from FirebaseServer (implement time function, see https://stackoverflow.com/questions/51455767/how-to-get-a-timestamp-from-firebase-android)
//    TODO: + time from server has to be modified w.r.t timezone and winter/sommer time
    fun currentTime(): String {
        return clock.format(Date())
    }

    fun addTime(timestamp: Long, minutes: String): String {

        var timestampHours =  minutes.format(timestamp).toInt()
        var timestampMinutes =  minutes.format(timestamp).toInt()
        val addMinutes = minutes.toInt()

        val additionalHours = (timestampMinutes + addMinutes) / 60

        if (additionalHours > 0) {
            timestampHours = (timestampHours + additionalHours) % 24
        }

        timestampMinutes = (timestampMinutes + addMinutes) % 60

        return hours.format(timestampHours)+":"+minutes.format(timestampMinutes)
    }

//    TODO: date check, we need date as well
//    fun timeExpired(timestamp: Long, minutes: String): Boolean {
//
//        val currentTime = currentTime()
//        val currentTimeHours = hours.format(currentTime)
//        val currentTimeMinutes = minutes.format(currentTime)
//
//        val time = addTime(timestamp, minutes)
//        val timeHours = hours.format(time)
//        val timeMinutes = minutes.format(time)
//
//        if (currentTimeHours > timeHours)
//    }
}