package io.stanc.pogoradar.firebase.data

import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_TOKEN
import io.stanc.pogoradar.firebase.DatabaseKeys.USERS

class UserNotificationData(private val userId: String,
                           private val notificationToken: String): FirebaseData {

    override val key: String = NOTIFICATION_TOKEN
    override fun databasePath(): String = "$USERS/$userId"
    override fun data(): String = notificationToken
}