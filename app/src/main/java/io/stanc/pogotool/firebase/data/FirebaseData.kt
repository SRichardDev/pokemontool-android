package io.stanc.pogotool.firebase.data

interface FirebaseData {

    val key: String
    fun databasePath(): String
    fun data(): Any
}