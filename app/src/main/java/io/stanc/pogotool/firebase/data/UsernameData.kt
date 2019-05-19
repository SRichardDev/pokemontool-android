package io.stanc.pogotool.firebase.data

import io.stanc.pogotool.firebase.DatabaseKeys.USERS
import io.stanc.pogotool.firebase.DatabaseKeys.USER_NAME

class UsernameData(private val userId: String,
                   private val userName: String): FirebaseData {

    override val key: String = USER_NAME
    override fun databasePath(): String = "$USERS/$userId"
    override fun data(): String = userName
}