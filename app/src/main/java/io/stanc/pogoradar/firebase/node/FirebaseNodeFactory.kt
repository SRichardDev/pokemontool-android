package io.stanc.pogoradar.firebase.node

object FirebaseNodeFactory {

    fun createChat(raid: FirebaseRaid, senderId: String, message: String): FirebaseChat {
        val parentDatabasePath = "${raid.databasePath()}/${raid.id}"
       return FirebaseChat.new(parentDatabasePath, senderId, message)
    }
}