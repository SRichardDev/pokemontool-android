package io.stanc.pogotool.firebase.node

import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_RAID_MEETUPS

data class FirebaseRaidMeetup(
    override val id: String,
    val meetupTime: String,
    val participants: List<String>): FirebaseNode {

    override fun databasePath(): String = DATABASE_RAID_MEETUPS

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data["meetupTime"] = meetupTime

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseRaidMeetup? {
            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val meetupTime = dataSnapshot.child("meetupTime").value as? String
            val participants = mutableListOf<String>()
            for (participant in dataSnapshot.child("participants").children) {
                (participant.value as? String)?.let { participants.add(it) }
            }

            Log.v(TAG, "id: $id, meetupTime: meetupTime, participants: participants")

            if (id != null && meetupTime != null) {
                return FirebaseRaidMeetup(id, meetupTime, participants)
            }

            return null
        }
    }
}