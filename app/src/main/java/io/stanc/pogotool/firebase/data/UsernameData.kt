package io.stanc.pogotool.firebase.data

import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_USERS
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_USER_TRAINER_NAME

class UsernameData(private val userId: String,
                   private val userName: String): FirebaseData {

    override val key: String = DATABASE_USER_TRAINER_NAME
    override fun databasePath(): String = "$DATABASE_USERS/$userId"
    override fun data(): String = userName
}