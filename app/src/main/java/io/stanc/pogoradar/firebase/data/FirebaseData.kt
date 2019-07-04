package io.stanc.pogoradar.firebase.data

interface FirebaseData {

    val key: String
    fun databasePath(): String
    fun data(): Any
}