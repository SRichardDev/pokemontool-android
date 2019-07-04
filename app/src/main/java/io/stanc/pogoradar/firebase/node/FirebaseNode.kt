package io.stanc.pogoradar.firebase.node

import io.stanc.pogoradar.recyclerview.IdItem


interface FirebaseNode: IdItem {

    override val id: String
    fun databasePath(): String
    fun data(): Map<String, Any>
}