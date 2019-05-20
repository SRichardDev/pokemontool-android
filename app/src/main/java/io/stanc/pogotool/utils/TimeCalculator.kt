package io.stanc.pogotool.utils

import android.annotation.SuppressLint
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*


object TimeCalculator {

    private val TAG = javaClass.name

    @SuppressLint("SimpleDateFormat")
    val clock = SimpleDateFormat("HH:mm")

    fun format(date: Date): String = clock.format(date)
    fun format(hours: Int, minutes: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hours)
        calendar.set(Calendar.MINUTE, minutes)
        return format(calendar.time)

    }

    fun currentTime(): Date = Calendar.getInstance().time

    fun addTime(timestamp: Long, minutes: String): Date {
        val calendar = Calendar.getInstance()
        val timestampDate = Date(timestamp)

        calendar.time = timestampDate
        calendar.add(Calendar.MINUTE, minutes.toInt())

        return calendar.time
    }

    fun timeExpired(timestamp: Long, minutes: String): Boolean {
        val timeToCheck = addTime(timestamp, minutes)
        return timeExpired(timeToCheck)
    }

    fun timeExpired(date: Date): Boolean {
        val currentTime = currentTime()
        return currentTime.after(date)
    }
}