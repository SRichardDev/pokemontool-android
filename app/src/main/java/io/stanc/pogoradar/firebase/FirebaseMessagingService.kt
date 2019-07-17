package io.stanc.pogoradar.firebase

import android.app.*
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
import io.stanc.pogoradar.StartActivity
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TYPE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_BODY
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_LATITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_LONGITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TITLE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TYPE_CHAT
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TYPE_RAID_QUEST
import io.stanc.pogoradar.utils.Kotlin


class FirebaseMessagingService: FirebaseMessagingService() {

    // Notification in Background/Foreground:
    // https://medium.com/@Miqubel/mastering-firebase-notifications-36a3ffe57c41

    // bundled Android notifications: https://blog.danlew.net/2017/02/07/correctly-handling-bundled-android-notifications/
    // Info: https://code.tutsplus.com/tutorials/android-o-how-to-use-notification-channels--cms-28616

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "onMessageReceived(messageId: ${message.messageId}, messageType: ${message.messageType}, title: ${message.notification?.title}, body: ${message.notification?.body}, data: ${message.data})")

        if(!tryToHandleRaidOrQuestMessage(message)) {
            if(!tryToHandleChatMessage(message)) {
                Log.e(TAG, "Could not build notification with data: ${message.data}")
            }
        }
    }

    private fun tryToHandleRaidOrQuestMessage(message: RemoteMessage): Boolean {

        var handled = false

        Kotlin.safeLet( message.data[NOTIFICATION_TITLE],
            message.data[NOTIFICATION_BODY],
            message.data[NOTIFICATION_LATITUDE]?.toDoubleOrNull(),
            message.data[NOTIFICATION_LONGITUDE]?.toDoubleOrNull()) { title, body, lat, lng ->

            val intent = Intent(this, StartActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra(NOTIFICATION_TYPE, NOTIFICATION_TYPE_RAID_QUEST)
            intent.putExtra(NOTIFICATION_TITLE, title)
            intent.putExtra(NOTIFICATION_BODY, body)
            intent.putExtra(NOTIFICATION_LATITUDE, lat)
            intent.putExtra(NOTIFICATION_LONGITUDE, lng)

            postNotification(title, body, intent, NOTIFICATION_TYPE_RAID_QUEST)

            handled = true
        }

        return handled
    }

    private fun tryToHandleChatMessage(message: RemoteMessage): Boolean {

        var handled = false

        Kotlin.safeLet( message.data[NOTIFICATION_TITLE],
                        message.data[NOTIFICATION_BODY]) { title, body ->

            val intent = Intent(this, StartActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra(NOTIFICATION_TYPE, NOTIFICATION_TYPE_CHAT)
            intent.putExtra(NOTIFICATION_TITLE, title)
            intent.putExtra(NOTIFICATION_BODY, body)

            postNotification(title, body, intent, NOTIFICATION_TYPE_CHAT)

            handled = true
        }

        return handled
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
//        Log.i(TAG, "onNewToken(token: $token) for user: ${FirebaseUser.userData}")
        FirebaseUser.updateNotificationToken(token)
    }

    private fun buildNotification(title: String?, description: String?, intent: Intent, notificationType: String): Notification {

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.icon_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon_launcher))
            .setContentTitle(title)
            .setContentText(description)
            .setStyle(NotificationCompat.BigTextStyle())
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setGroup(notificationType)
            .setGroupSummary(true)

        return builder.build()
    }

    private fun postNotification(title: String?, description: String?, intent: Intent, notificationType: String) {

        val notification = buildNotification(title, description, intent, notificationType)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            channel.enableVibration(true)
            manager.createNotificationChannel(channel)
        }

        when(notificationType) {
            NOTIFICATION_TYPE_RAID_QUEST -> manager.notify(NOTIFICATION_ID, notification)
            NOTIFICATION_TYPE_CHAT -> manager.notify(NOTIFICATION_ID+1, notification)
            else -> manager.notify(NOTIFICATION_ID, notification)
        }

    }

    companion object {

        val TAG = this::class.java.name

        private val NOTIFICATION_CHANNEL_ID: String = App.geString(R.string.notification_channel_id)!!
        private val NOTIFICATION_CHANNEL_NAME: String = App.geString(R.string.notification_channel_name)!!
        private val NOTIFICATION_ID: Int = App.getInteger(R.integer.firebase_notification_id)!!
    }
}