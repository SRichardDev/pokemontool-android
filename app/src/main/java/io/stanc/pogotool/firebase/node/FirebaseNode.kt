package io.stanc.pogotool.firebase.node


interface FirebaseNode {

    val id: String
    fun databasePath(): String
    fun data(): Map<String, Any>
}