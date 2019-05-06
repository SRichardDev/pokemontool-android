package io.stanc.pogotool.firebase.node

import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_ARENA_RAID_MEETUPS

class FirebaseChat(
    override val id: String,
    private val raidMeetupId: String,
    val message: String,
    val senderId: String,
    val timestamp: Number): FirebaseNode {

    override fun databasePath(): String = "$DATABASE_ARENA_RAID_MEETUPS/$raidMeetupId/chat"

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data["message"] = message
        data["senderId"] = senderId
        data["timestamp"] = timestamp

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(raidMeetupId: String, dataSnapshot: DataSnapshot): FirebaseChat? {
            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val message = dataSnapshot.child("message").value as? String
            val senderId = dataSnapshot.child("senderId").value as? String
            val timestamp = dataSnapshot.child("timestamp").value as? Number

            Log.v(TAG, "raidMeetupId: $raidMeetupId, chatId: $id, message: $message, senderId: $senderId, timestamp: $timestamp")

            if (id != null && message != null && senderId != null && timestamp != null) {
                return FirebaseChat(id, raidMeetupId, message, senderId, timestamp)
            }

            return null
        }
    }
}