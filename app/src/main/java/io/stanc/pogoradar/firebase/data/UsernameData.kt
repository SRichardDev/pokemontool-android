package io.stanc.pogoradar.firebase.data

import io.stanc.pogoradar.firebase.DatabaseKeys.USERS
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_NAME

class UsernameData(private val userId: String,
                   private val userName: String): FirebaseData {

    override val key: String = USER_NAME
    override fun databasePath(): String = "$USERS/$userId"
    override fun data(): String = userName
}