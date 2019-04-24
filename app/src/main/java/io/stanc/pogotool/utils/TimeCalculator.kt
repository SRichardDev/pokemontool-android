package io.stanc.pogotool.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*


object TimeCalculator {

    private val TAG = javaClass.name

    @SuppressLint("SimpleDateFormat")
    val clock = SimpleDateFormat("HH:mm")

    fun format(date: Date): String {
        return clock.format(date)
    }

    fun currentTime(): Date {
        return Calendar.getInstance().time
    }

    fun addTime(timestamp: Long, minutes: String): Date {

        val timestampDate = Date(timestamp)

        val calendar = Calendar.getInstance()
        calendar.time = timestampDate
        calendar.add(Calendar.MINUTE, minutes.toInt())
        return calendar.time
    }

    fun timeExpired(timestamp: Long, minutes: String): Boolean {

        val currentTime = currentTime()
        val timeToCheck = addTime(timestamp, minutes)

        return timeToCheck.after(currentTime)
    }
}