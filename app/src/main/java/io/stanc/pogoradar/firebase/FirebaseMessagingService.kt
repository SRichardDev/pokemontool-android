package io.stanc.pogoradar.firebase

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.stanc.pogoradar.App
import io.stanc.pogoradar.NavDrawerActivity
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_FLAG
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_BODY
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_LATITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_LONGITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TITLE
import io.stanc.pogoradar.geohash.GeoHash
import io.stanc.pogoradar.utils.Kotlin


class FirebaseMessagingService: FirebaseMessagingService() {

    // Notification in Background/Foreground:
    // https://medium.com/@Miqubel/mastering-firebase-notifications-36a3ffe57c41

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
//        Log.i(TAG, "onMessageReceived(messageId: ${message.messageId}, messageType: ${message.messageType}, title: ${message.notification?.title}, body: ${message.notification?.body}, data: ${message.data})")

        Kotlin.safeLet( message.data[NOTIFICATION_TITLE],
                        message.data[NOTIFICATION_BODY],
                        message.data[NOTIFICATION_LATITUDE]?.toDoubleOrNull(),
                        message.data[NOTIFICATION_LONGITUDE]?.toDoubleOrNull()) { title, body, lat, lng ->

            Log.d(TAG, "Debug:: onMessageReceived(title: $title): geoHash: ${GeoHash(lat, lng)}")

            val intent = Intent(this, NavDrawerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra(NOTIFICATION_FLAG, true)
            intent.putExtra(NOTIFICATION_TITLE, title)
            intent.putExtra(NOTIFICATION_BODY, body)
            intent.putExtra(NOTIFICATION_LATITUDE, lat)
            intent.putExtra(NOTIFICATION_LONGITUDE, lng)

            val notification = buildNotification(title, body, intent)
            postNotification(notification)

        } ?: run {
            Log.e(TAG, "Could not build notification with data: ${message.data}.\nError: lat: ${message.data[NOTIFICATION_LATITUDE]} or lng: ${message.data[NOTIFICATION_LONGITUDE]} could not be cast to double or title or body do not exist.")
        }
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
        Log.i(TAG, "onNewToken(token: $token) for user: ${FirebaseUser.userData}")
        FirebaseUser.updateNotificationToken(token)
    }

    private fun buildNotification(title: String?, contentDescription: String?, intent: Intent): Notification {

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.icon_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_launcher))
            .setContentTitle(title)
            .setContentText(contentDescription)
            .setStyle(NotificationCompat.BigTextStyle())
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))

        return builder.build()
    }

    private fun postNotification(notification: Notification) {

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {

        val TAG = this::class.java.name

        private val NOTIFICATION_CHANNEL_ID: String = App.geString(R.string.firebase_notification_channel_id)!!
        private val NOTIFICATION_CHANNEL_NAME: String = App.geString(R.string.firebase_notification_channel_name)!!
        private val NOTIFICATION_ID: Int = App.getInteger(R.integer.firebase_notification_id)!!
    }
}