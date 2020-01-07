package io.stanc.pogoradar.firebase.node

import io.stanc.pogoradar.recyclerview.IdItem

interface FirebaseNode: IdItem {

    fun databasePath(): String
    override val id: String
}