package io.stanc.pogoradar.firebase.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Build.VERSION_CODES.O
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.MessagingStyle.MAXIMUM_RETAINED_MESSAGES
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
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TYPE_LOCAL
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TYPE_RAID
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.utils.Kotlin
import java.lang.ref.WeakReference


class FirebaseNotificationService: FirebaseMessagingService() {

    private val TAG = this::class.java.name

    private val maxNumberOfFirebaseNotifications = MAXIMUM_RETAINED_MESSAGES
    private var currentFirebaseNotificationId = 0
    private val latestFirebaseNotificationId = "latestFirebaseNotificationId"

    enum class NotificationType {
        Unknown,
        Raid,
        Quest,
        Chat
    }
    
    companion object {

        private const val NOTIFICATION_ID_LOCAL = 10681

        var instance: WeakReference<FirebaseNotificationService>? = null
            private set
    }

    /**
     * lifecycle
     */

    override fun onCreate() {
        super.onCreate()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        currentFirebaseNotificationId = sharedPreferences.getInt(latestFirebaseNotificationId, 0)
        instance = WeakReference(this)
    }

    override fun onDestroy() {
        instance = null
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.edit().putInt(latestFirebaseNotificationId, currentFirebaseNotificationId).apply()
        super.onDestroy()
    }

    // Notification in Background/Foreground:
    // https://medium.com/@Miqubel/mastering-firebase-notifications-36a3ffe57c41
    // Notification explanation + grouping
    // https://medium.com/@krossovochkin/android-notifications-overview-and-pitfalls-517d1118ec83

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Debug:: onMessageReceived(messageId: ${message.messageId}, messageType: ${message.messageType}, title: ${message.notification?.title}, body: ${message.notification?.body}, data: ${message.data})")

        // TODO: different notification types are NOT supported yet
//        when(notificationType(message.data)) {
//
//            NotificationType.Raid -> postRaidOrQuestNotification(message, NOTIFICATION_TYPE_RAID)
//            NotificationType.Quest -> postRaidOrQuestNotification(message, NOTIFICATION_TYPE_QUEST)
//            NotificationType.Chat -> postChatNotification(message)
//            else -> {}
//        }

        // TODO: just a workaround, see comment above
        if (message.data.containsKey(NOTIFICATION_LATITUDE) && message.data.containsKey(NOTIFICATION_LONGITUDE)) {
            postRaidOrQuestNotification(message, NOTIFICATION_TYPE_RAID)
        } else {
            postChatNotification(message)
        }
    }

    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.i(TAG, "onDeletedMessages()")
    }

    override fun onMessageSent(var1: String) {
        super.onMessageSent(var1)
        Log.i(TAG, "onMessageSent(var1: $var1)")
    }

    override fun onSendError(var1: String, var2: Exception) {
        super.onSendError(var1, var2)
        Log.i(TAG, "onSendError(var1: $var1, var2: ${var2.message})")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "onNewToken(token: $token) for user: ${FirebaseUser.userData}")
        FirebaseUser.updateNotificationToken(token)
    }

    /**
     * public interface
     */
    
    fun postLocalNotification(title: String, description: String) {
        Log.i(TAG, "postLocalNotification($title)")
        val intent = Intent(this, StartActivity::class.java)
        postNotification(title, description, intent, NOTIFICATION_TYPE_LOCAL, NOTIFICATION_ID_LOCAL)
    }

    /**
     * private
     */

    // TODO: determine notificationType should be handled explicitly with json keyword, not implicitly with string comparison
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

            postNotification(title, body, intent, notificationType, currentFirebaseNotificationId)
            updateNotificationId()

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

            postNotification(title, body, intent, NOTIFICATION_TYPE_CHAT, currentFirebaseNotificationId)
            updateNotificationId()

        } ?: run {
            Log.e(TAG, "failed to build notification: $NOTIFICATION_TYPE_CHAT with message data: ${message.data}")
        }
    }

    private fun postNotification(title: String?, description: String?, intent: Intent, notificationType: String, notificationId: Int) {

        val notification = buildNotification(title, description, intent, notificationType, notificationId)

        val manager = NotificationManagerCompat.from(this)

        if (Build.VERSION.SDK_INT >= O) {
            val channel = NotificationChannel(notificationType, notificationType, NotificationManager.IMPORTANCE_HIGH)
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            channel.enableVibration(true)
            manager.createNotificationChannel(channel)
        }

        Log.i(TAG, "postNotification($notificationType, title: $title, description: $description, intent: $intent)")
        manager.notify(currentFirebaseNotificationId, notification)
    }

    private fun buildNotification(title: String?, description: String?, intent: Intent, notificationType: String, notificationId: Int): Notification {

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_ONE_SHOT)

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
        currentFirebaseNotificationId = (currentFirebaseNotificationId+1)%maxNumberOfFirebaseNotifications
    }
}