package io.stanc.pogotool.firebase.data


interface FirebaseItem {

    val id: String
    fun databasePath(): String
    fun data(): Map<String, String>
}