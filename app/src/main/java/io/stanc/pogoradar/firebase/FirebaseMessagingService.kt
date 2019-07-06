package io.stanc.pogoradar.firebase

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.stanc.pogoradar.App
import io.stanc.pogoradar.NavDrawerActivity
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.DatabaseKeys.BODY
import io.stanc.pogoradar.firebase.DatabaseKeys.LATITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.LONGITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.SOUND
import io.stanc.pogoradar.firebase.DatabaseKeys.TITLE
import io.stanc.pogoradar.geohash.GeoHash
import io.stanc.pogoradar.screen.MapInteractionFragment
import io.stanc.pogoradar.utils.Kotlin


class FirebaseMessagingService: FirebaseMessagingService() {

    // Notification in Background/Foreground:
    // https://medium.com/@Miqubel/mastering-firebase-notifications-36a3ffe57c41

    // TODO: Info: just in foreground

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.i(TAG, "Debug:: onMessageReceived(messageId: ${message.messageId}, messageType: ${message.messageType}, title: ${message.notification?.title}, body: ${message.notification?.body}, data: ${message.data})")

        // for click on notification -> start activity
        val intent = Intent(this, NavDrawerActivity::class.java)

        Kotlin.safeLet( message.data[TITLE],
                        message.data[BODY],
                        message.data[LATITUDE]?.toDoubleOrNull(),
                        message.data[LONGITUDE]?.toDoubleOrNull()) { title, body, lat, lng ->

            Log.d(TAG, "Debug:: onMessageReceived: geoHash: ${GeoHash(lat, lng)}")
            val notification = NotificationService.Notification(title, body, lat, lng)
            NotificationService.reportNotification(notification)

        } ?: run {
            Log.e(TAG, "Could not build notification with data: ${message.data}. Error: lat: ${message.data[LATITUDE]} or lng: ${message.data[LONGITUDE]} could not be cast to double.")
        }

        val notification = buildNotification(message.notification?.title, message.notification?.body, intent)
        postNotification(notification)
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.i(TAG, "Debug:: onDeletedMessages()")
    }

    override fun onMessageSent(var1: String) {
        super.onMessageSent(var1)
        Log.i(TAG, "Debug:: onMessageSent(var1: $var1)")
    }

    override fun onSendError(var1: String, var2: Exception) {
        super.onSendError(var1, var2)
        Log.i(TAG, "Debug:: onSendError(var1: $var1, var2: ${var2.message})")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "Debug:: onNewToken(token: $token), FirebaseUser: ${FirebaseUser.userData}")
        FirebaseUser.updateNotificationToken(token)
    }

    private fun postNotification(notification: Notification) {
        Log.d(TAG, "Debug:: postNotification(notification: $notification)")

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

        Log.d(TAG, "Debug:: buildNotification(title: $title, text: $title)")
        return builder.build()
    }

    companion object {

        val TAG = this::class.java.name

        private val NOTIFICATION_CHANNEL_ID: String = App.geString(R.string.firebase_notification_channel_id)!!
        private val NOTIFICATION_CHANNEL_NAME: String = App.geString(R.string.firebase_notification_channel_name)!!
        private val NOTIFICATION_ID: Int = App.getInteger(R.integer.firebase_notification_id)!!
    }
}