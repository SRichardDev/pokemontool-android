package io.stanc.pogotool.firebase.data

import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_USER_NOTIFICATION_TOKEN
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_USERS

class UserNotificationData(private val userId: String,
                           private val notificationToken: String): FirebaseData {

    override val key: String = DATABASE_USER_NOTIFICATION_TOKEN
    override fun databasePath(): String = "$DATABASE_USERS/$userId"
    override fun data(): String = notificationToken
}