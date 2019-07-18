package io.stanc.pogoradar.firebase

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Build.VERSION_CODES.O
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.stanc.pogoradar.R
import io.stanc.pogoradar.StartActivity
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_BODY
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_LATITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_LONGITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TITLE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TYPE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TYPE_CHAT
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TYPE_QUEST
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TYPE_RAID
import io.stanc.pogoradar.utils.Kotlin


class FirebaseMessagingService: FirebaseMessagingService() {

    private val TAG = this::class.java.name

    private val maxNumberOfNotifications = 10
    private var currentNotificationId = 0

    enum class NotificationType {
        Unknown,
        Raid,
        Quest,
        Chat
    }

    // Notification in Background/Foreground:
    // https://medium.com/@Miqubel/mastering-firebase-notifications-36a3ffe57c41
    // Notification explanation + grouping
    // https://medium.com/@krossovochkin/android-notifications-overview-and-pitfalls-517d1118ec83

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
//        Log.d(TAG, "Debug:: onMessageReceived(messageId: ${message.messageId}, messageType: ${message.messageType}, title: ${message.notification?.title}, body: ${message.notification?.body}, data: ${message.data})")

        when(notificationType(message.data)) {

            NotificationType.Raid -> postRaidOrQuestNotification(message, NOTIFICATION_TYPE_RAID)
            NotificationType.Quest -> postRaidOrQuestNotification(message, NOTIFICATION_TYPE_QUEST)
            NotificationType.Chat -> postChatNotification(message)
            else -> {}
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
//        Log.i(TAG, "Debug:: onDeletedMessages()")
    }

    override fun onMessageSent(var1: String) {
        super.onMessageSent(var1)
//        Log.i(TAG, "Debug:: onMessageSent(var1: $var1)")
    }

    override fun onSendError(var1: String, var2: Exception) {
        super.onSendError(var1, var2)
//        Log.i(TAG, "Debug:: onSendError(var1: $var1, var2: ${var2.message})")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
//        Log.i(TAG, "Debug:: onNewToken(token: $token) for user: ${FirebaseUser.userData}")
        FirebaseUser.updateNotificationToken(token)
    }

    private fun notificationType(messageData: Map<String, String>): NotificationType {

        return Kotlin.safeLet(messageData[NOTIFICATION_TITLE], messageData[NOTIFICATION_BODY]) { title, body ->

            if (title.contains("Feldforschung") || body.contains("Quest")) {
                NotificationType.Quest
            } else if (title.contains("Raid") || body.contains("Raidboss")) {
                NotificationType.Raid
            } else {
                NotificationType.Chat
            }

        } ?: run {
            NotificationType.Unknown
        }
    }

    private fun postRaidOrQuestNotification(message: RemoteMessage, notificationType: String) {

        Kotlin.safeLet(
            message.data[NOTIFICATION_TITLE],
            message.data[NOTIFICATION_BODY],
            message.data[NOTIFICATION_LATITUDE]?.toDoubleOrNull(),
            message.data[NOTIFICATION_LONGITUDE]?.toDoubleOrNull()) { title, body, lat, lng ->

            val intent = Intent(this, StartActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra(NOTIFICATION_TYPE, notificationType)
            intent.putExtra(NOTIFICATION_TITLE, title)
            intent.putExtra(NOTIFICATION_BODY, body)
            intent.putExtra(NOTIFICATION_LATITUDE, lat)
            intent.putExtra(NOTIFICATION_LONGITUDE, lng)

            postNotification(title, body, intent, notificationType)

        } ?: run {
            Log.e(TAG, "failed to build notification: $notificationType with message data: ${message.data}")
        }
    }

    private fun postChatNotification(message: RemoteMessage) {

        Kotlin.safeLet(
            message.data[NOTIFICATION_TITLE],
            message.data[NOTIFICATION_BODY]) { title, body ->

            val intent = Intent(this, StartActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra(NOTIFICATION_TYPE, NOTIFICATION_TYPE_CHAT)
            intent.putExtra(NOTIFICATION_TITLE, title)
            intent.putExtra(NOTIFICATION_BODY, body)

            postNotification(title, body, intent, NOTIFICATION_TYPE_CHAT)

        } ?: run {
            Log.e(TAG, "failed to build notification: $NOTIFICATION_TYPE_CHAT with message data: ${message.data}")
        }
    }

    private fun postNotification(title: String?, description: String?, intent: Intent, notificationType: String) {

        val notification = buildNotification(title, description, intent, notificationType)

        val manager = NotificationManagerCompat.from(this)

        if (Build.VERSION.SDK_INT >= O) {
            val channel = NotificationChannel(notificationType, notificationType, NotificationManager.IMPORTANCE_HIGH)
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            channel.enableVibration(true)
            manager.createNotificationChannel(channel)
        }

        manager.notify(currentNotificationId, notification)
        updateNotificationId()
    }

    private fun buildNotification(title: String?, description: String?, intent: Intent, notificationType: String): Notification {

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val builder = NotificationCompat.Builder(this, notificationType)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_launcher))
            .setSmallIcon(R.mipmap.icon_launcher)
            .setContentTitle(title)
            .setContentText(description)
            .setStyle(NotificationCompat.BigTextStyle())
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))

        return builder.build()
    }

    private fun updateNotificationId() {
        currentNotificationId = (currentNotificationId+1)%maxNumberOfNotifications
    }
}