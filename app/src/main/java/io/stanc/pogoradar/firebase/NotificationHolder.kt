package io.stanc.pogoradar.firebase

import android.util.Log

object NotificationHolder {
    private val TAG = javaClass.name

    private var lastReportedNotification: NotificationContent? = null

    fun reportNotification(notification: NotificationContent) {
        Log.i(TAG, "Debug:: reportNotification(notification)")
        lastReportedNotification = notification
    }

    fun consumeNotification(): NotificationContent? {
        Log.i(TAG, "Debug:: consumeNotification(), lastReportedNotification: $lastReportedNotification")
        val notification = lastReportedNotification?.copy()
        lastReportedNotification = null
        Log.i(TAG, "Debug:: consumeNotification(): $notification")
        return notification
    }
}