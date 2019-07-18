package io.stanc.pogoradar.utils

import android.annotation.SuppressLint
import android.util.Log
import com.google.common.math.LongMath
import java.math.RoundingMode
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

        return dateOfToday(time)?.let { date ->

            val diffTime = date.time - currentDate().time
            LongMath.divide(diffTime, 1000 * 60, RoundingMode.CEILING)

        } ?: run {
            null
        }
    }

    /**
     * time comparison
     */

    fun timeExpired(timestamp: Long, time: String): Boolean? {
        return date(timestamp, time)?.let {
            timeExpired(it)
//
//            if (expired) {
//                Log.v(TAG, "Debug:: date(timestamp: $timestamp, time: $time) => $it after? currentDate: ${currentDate()} => isExpired: $expired")
//            } else {
//                Log.w(TAG, "Debug:: date(timestamp: $timestamp, time: $time) => $it after? currentDate: ${currentDate()} => isExpired: $expired")
//            }
//            expired
        } ?: run {
            null
        }
    }

    fun timeExpired(date: Date): Boolean = currentDate().after(date)

    fun currentDate(): Date = Calendar.getInstance().time

    fun isCurrentDay(timestamp: Long): Boolean {
        val now = Calendar.getInstance()

        val timestampCalender = Calendar.getInstance()
        timestampCalender.time = Date(timestamp)

        return  now.get(DAY_OF_YEAR) == timestampCalender.get(DAY_OF_YEAR) &&
                now.get(YEAR) == timestampCalender.get(YEAR)
    }

    fun dateOfToday(time: String): Date? {

        return try {

            val temp = Calendar.getInstance()
            temp.time = clock.parse(time)

            val clockCalendar = Calendar.getInstance()
            clockCalendar.time = currentDate()
            clockCalendar.set(Calendar.HOUR_OF_DAY, temp.get(Calendar.HOUR_OF_DAY))
            clockCalendar.set(Calendar.MINUTE, temp.get(Calendar.MINUTE))

            clockCalendar.time

        } catch (e: ParseException) {
            e.printStackTrace()
            null
        }
    }

    fun date(timestamp: Long, time: String): Date? {

        return try {

            val temp = Calendar.getInstance()
            temp.time = clock.parse(time)

            val clockCalendar = Calendar.getInstance()
            clockCalendar.time = Date(timestamp)
            clockCalendar.set(Calendar.HOUR_OF_DAY, temp.get(Calendar.HOUR_OF_DAY))
            clockCalendar.set(Calendar.MINUTE, temp.get(Calendar.MINUTE))

            clockCalendar.time

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * private
     */

//    private fun nextDateAfterTimestamp(timestamp: Long, clock: String): Date? {
//        return dateOfToday(clock)?.let { clockDate ->
//
//            nextClockDate(clockDate, Date(timestamp))
//
//        } ?: run {
//            Log.e(TAG, "could not get next dateOfToday clock $clock w.r.t. expected clock nextDate ${this.clock.toLocalizedPattern()}")
//            null
//        }
//    }
//
//    private fun nextClockDate(clockDate: Date, compareDate: Date): Date {
//
//        val compareCalendar = Calendar.getInstance()
//        compareCalendar.time = compareDate
//
//        val temp = Calendar.getInstance()
//        temp.time = clockDate
//
//        val clockCalendar = Calendar.getInstance()
//        clockCalendar.time = compareDate
//        clockCalendar.set(Calendar.HOUR_OF_DAY, temp.get(Calendar.HOUR_OF_DAY))
//        clockCalendar.set(Calendar.MINUTE, temp.get(Calendar.MINUTE))
//
//        if (clockCalendar.time.before(compareCalendar.time)) {
//            clockCalendar.set(DAY_OF_YEAR, clockCalendar.get(DAY_OF_YEAR)+1)
//        }
//
//        return clockCalendar.time
//    }
}