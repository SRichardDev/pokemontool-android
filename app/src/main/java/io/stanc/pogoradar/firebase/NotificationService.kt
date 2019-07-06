package io.stanc.pogoradar.firebase

import android.util.Log

object NotificationService {
    private val TAG = javaClass.name

    data class Notification(val title: String,
                            val body: String,
                            val latitude: Double,
                            val longitude: Double)

    private var lastReportedNotification: Notification? = null

    fun reportNotification(notification: Notification) {
        Log.i(TAG, "Debug:: reportNotification(notification)")
        lastReportedNotification = notification
    }

    fun consumeNotification(): Notification? {
        Log.i(TAG, "Debug:: consumeNotification(), lastReportedNotification: $lastReportedNotification")
        val notification = lastReportedNotification?.copy()
        lastReportedNotification = null
        Log.i(TAG, "Debug:: consumeNotification(): $notification")
        return notification
    }
}