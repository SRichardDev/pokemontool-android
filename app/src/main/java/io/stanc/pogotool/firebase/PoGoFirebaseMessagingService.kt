package io.stanc.pogotool.firebase

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PoGoFirebaseMessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(var1: RemoteMessage) {
        Log.i(this.javaClass.name, "onMessageReceived(messageId: ${var1.messageId}, messageType: ${var1.messageType}, notification: ${var1.notification?.body})")
    }

    override fun onDeletedMessages() {
        Log.i(this.javaClass.name, "onDeletedMessages()")
    }

    override fun onMessageSent(var1: String) {
        Log.i(this.javaClass.name, "onMessageSent(var1: $var1)")
    }

    override fun onSendError(var1: String, var2: Exception) {
        Log.i(this.javaClass.name, "onSendError(var1: $var1, var2: ${var2.message})")
    }

    override fun onNewToken(var1: String) {
        Log.i(this.javaClass.name, "onNewToken(var1: $var1)")
    }
}