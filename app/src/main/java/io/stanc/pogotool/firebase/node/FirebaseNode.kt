package io.stanc.pogotool.firebase.node

import io.stanc.pogotool.recyclerview.IdItem


interface FirebaseNode: IdItem {

    override val id: String
    fun databasePath(): String
    fun data(): Map<String, Any>
}