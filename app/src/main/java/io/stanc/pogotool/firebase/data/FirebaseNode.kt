package io.stanc.pogotool.firebase.data


interface FirebaseNode {

    val id: String
    fun databasePath(): String
    fun data(): Map<String, Any>
}