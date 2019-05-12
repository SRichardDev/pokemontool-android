package io.stanc.pogotool.firebase

import android.app.Notification
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import io.stanc.pogotool.NavDrawerActivity
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.NOTIFICATION_DATA_LATITUDE
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.NOTIFICATION_DATA_LONGITUDE


class FirebaseMessagingService: FirebaseMessagingService() {

    // Notification in Background/Foreground:
    // https://medium.com/@Miqubel/mastering-firebase-notifications-36a3ffe57c41

    override fun onMessageReceived(message: RemoteMessage) {
        Log.i(TAG, "onMessageReceived(messageId: ${message.messageId}, messageType: ${message.messageType}, title: ${message.notification?.title}, body: ${message.notification?.body}, data: ${message.data})")

        val intent = Intent(this, NavDrawerActivity::class.java)
        intent.putExtra(NOTIFICATION_DATA_LATITUDE, message.data[NOTIFICATION_DATA_LATITUDE])
        intent.putExtra(NOTIFICATION_DATA_LONGITUDE, message.data[NOTIFICATION_DATA_LONGITUDE])

        val notification = buildNotification(message.notification?.title, message.notification?.body, intent)
        postNotification(notification)
    }

    override fun onDeletedMessages() {
        Log.i(TAG, "onDeletedMessages()")
    }

    override fun onMessageSent(var1: String) {
        Log.i(TAG, "onMessageSent(var1: $var1)")
    }

    override fun onSendError(var1: String, var2: Exception) {
        Log.i(TAG, "onSendError(var1: $var1, var2: ${var2.message})")
    }

    override fun onNewToken(var1: String) {
        Log.i(TAG, "onNewToken(var1: $var1)")
    }

    private fun postNotification(notification: Notification) {
        Log.d(TAG, "postNotification(notification: $notification)")

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String?, text: String?, intent: Intent): Notification {

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.icon_launcher)
            .setContentTitle(title)
            .setContentText(text).setAutoCancel(true)
            .setContentIntent(pendingIntent)

        Log.d(TAG, "buildNotification(title: $title, text: $title)")
        return builder.build()
    }

    companion object {

        val TAG = this::class.java.name

        const val NOTIFICATION_ID = 0
        const val NOTIFICATION_CHANNEL_ID = "3131830000"
        const val NOTIFICATION_CHANNEL_NAME = "FirebaseMessagingService"
    }
}