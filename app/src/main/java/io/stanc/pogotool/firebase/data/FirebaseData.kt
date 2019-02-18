package io.stanc.pogotool.firebase.data

interface FirebaseData {

    fun databasePath(): String
    fun data(): Map<String, String>
}