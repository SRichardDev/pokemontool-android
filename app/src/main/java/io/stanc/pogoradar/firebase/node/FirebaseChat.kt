package io.stanc.pogoradar.firebase.node

import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.DatabaseKeys.CHAT
import io.stanc.pogoradar.firebase.DatabaseKeys.CHAT_MESSAGE
import io.stanc.pogoradar.firebase.DatabaseKeys.CHAT_SENDER_ID
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_MEETUPS
import io.stanc.pogoradar.firebase.DatabaseKeys.TIMESTAMP
import io.stanc.pogoradar.firebase.FirebaseServer
import io.stanc.pogoradar.firebase.FirebaseServer.TIMESTAMP_SERVER

class FirebaseChat private constructor(
    override val id: String,
    private val raidMeetupId: String,
    val message: String,
    val senderId: String,
    val timestamp: Number): FirebaseNode {

    override fun databasePath(): String = "$RAID_MEETUPS/$raidMeetupId/$CHAT"

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[CHAT_MESSAGE] = message
        data[CHAT_SENDER_ID] = senderId
        data[TIMESTAMP] = if(timestamp == TIMESTAMP_SERVER) FirebaseServer.timestamp() else timestamp

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(raidMeetupId: String, dataSnapshot: DataSnapshot): FirebaseChat? {
            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val message = dataSnapshot.child(CHAT_MESSAGE).value as? String
            val senderId = dataSnapshot.child(CHAT_SENDER_ID).value as? String
            val timestamp = dataSnapshot.child(TIMESTAMP).value as? Number

            Log.v(TAG, "id: $id, message: $message, senderId: $senderId, timestamp: $timestamp")

            if (id != null && message != null && senderId != null && timestamp != null) {
                return FirebaseChat(id, raidMeetupId, message, senderId, timestamp)
            }

            return null
        }

        fun new(raidMeetupId: String, senderId: String, message: String): FirebaseChat {
            return FirebaseChat("", raidMeetupId, message, senderId, TIMESTAMP_SERVER)
        }
    }
}