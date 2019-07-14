package io.stanc.pogoradar.firebase

object NotificationHolder {
    private val TAG = javaClass.name

    private var lastReportedNotification: NotificationContent? = null

    fun reportNotification(notification: NotificationContent) {
        lastReportedNotification = notification
    }

    fun consumeNotification(): NotificationContent? {
        val notification = lastReportedNotification?.copy()
        lastReportedNotification = null
        return notification
    }
}