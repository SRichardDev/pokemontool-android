package io.stanc.pogotool.utils

import android.annotation.SuppressLint
import android.util.Log
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.DAY_OF_YEAR
import java.util.Calendar.YEAR


object TimeCalculator {

    private val TAG = javaClass.name

    /**
     * string nextDate
     */

    @SuppressLint("SimpleDateFormat")
    val clock = SimpleDateFormat("HH:mm")

    fun format(date: Date): String = clock.format(date)
    fun format(hours: Int, minutes: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hours)
        calendar.set(Calendar.MINUTE, minutes)
        return format(calendar.time)
    }
    fun nextDate(clock: String): Date? {

        return date(clock)?.let { clockDate ->

            nextClockDate(clockDate, currentDate())

        } ?: kotlin.run {
            Log.e(TAG, "could not nextDate clock $clock w.r.t. expected clock nextDate ${this.clock.toLocalizedPattern()}")
            null
        }
    }


    /**
     * calculate time
     */

    fun addTime(timestamp: Long, minutes: String): Date {
        val timestampDate = Date(timestamp)
        return addTime(timestampDate, minutes)
    }

    fun addTime(timestamp: Long, minutes: Int): Date {
        val timestampDate = Date(timestamp)
        return addTime(timestampDate, minutes)
    }

    fun addTime(date: Date, minutes: Int): Date {
        val calendar = Calendar.getInstance()

        calendar.time = date
        calendar.add(Calendar.MINUTE, minutes)

        return calendar.time
    }

    fun addTime(date: Date, minutes: String): Date {
        val calendar = Calendar.getInstance()

        calendar.time = date
        calendar.add(Calendar.MINUTE, minutes.toInt())

        return calendar.time
    }

    fun minutesUntil(timestamp: Long, time: String): Long? {
        val diff = nextDateAfterTimestamp(timestamp, time)?.let { date ->
            Log.d(TAG, "Time:: minutesUntil($timestamp, $time) date: $date, current: ${currentDate()}")
            val diffTime = date.time - currentDate().time
            diffTime / (1000 * 60)
        } ?: kotlin.run {
            null
        }

        Log.d(TAG, "Time:: minutesUntil($timestamp, $time) => $diff")

        return diff
    }

    /**
     * time comparison
     */

    fun timeExpired(timestamp: Long, time: String): Boolean? {
        return nextDateAfterTimestamp(timestamp, time)?.let {
            timeExpired(it)
        } ?: kotlin.run {
            null
        }
    }

    fun timeExpired(date: Date): Boolean = currentDate().after(date)

    fun currentDate(): Date = Calendar.getInstance().time

    fun currentDay(timestamp: Long): Boolean {
        val now = Calendar.getInstance()

        val timestampCalender = Calendar.getInstance()
        timestampCalender.time = Date(timestamp)

        return  now.get(DAY_OF_YEAR) == timestampCalender.get(DAY_OF_YEAR) &&
                now.get(YEAR) == timestampCalender.get(YEAR)
    }

    /**
     * private
     */

    private fun date(time: String): Date? {
        return try {
            clock.parse(time)
        } catch (e: ParseException) {
            e.printStackTrace()
            null
        }
    }

    private fun nextDateAfterTimestamp(timestamp: Long, clock: String): Date? {
        return date(clock)?.let { clockDate ->

            nextClockDate(clockDate, Date(timestamp))

        } ?: kotlin.run {
            Log.e(TAG, "could not nextDate clock $clock w.r.t. expected clock nextDate ${this.clock.toLocalizedPattern()}")
            null
        }
    }

    private fun nextClockDate(clockDate: Date, compareDate: Date): Date {

        val compareCalendar = Calendar.getInstance()
        compareCalendar.time = compareDate

        val temp = Calendar.getInstance()
        temp.time = clockDate

        val clockCalendar = Calendar.getInstance()
        clockCalendar.time = compareDate
        clockCalendar.set(Calendar.HOUR_OF_DAY, temp.get(Calendar.HOUR_OF_DAY))
        clockCalendar.set(Calendar.MINUTE, temp.get(Calendar.MINUTE))

        if (clockCalendar.time.before(compareCalendar.time)) {
            clockCalendar.set(DAY_OF_YEAR, clockCalendar.get(DAY_OF_YEAR)+1)
        }

        return clockCalendar.time
    }
}