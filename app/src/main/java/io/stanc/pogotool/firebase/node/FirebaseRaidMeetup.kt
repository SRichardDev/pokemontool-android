package io.stanc.pogotool.firebase.node

import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_ARENA_RAID_MEETUPS
import io.stanc.pogotool.firebase.data.RaidMeetupParticipant
import io.stanc.pogotool.utils.KotlinUtils

data class FirebaseRaidMeetup(
    override val id: String,
    val meetupTime: String,
    val participants: List<RaidMeetupParticipant>,
    val chat: List<FirebaseChat>): FirebaseNode {

    override fun databasePath(): String = DATABASE_ARENA_RAID_MEETUPS

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data["meetupTime"] = meetupTime
        data["participants"] = participants
        data["chat"] = participants

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseRaidMeetup? {
            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key ?: kotlin.run { return null }

            val meetupTime = dataSnapshot.child("meetupTime").value as? String

            val participants = mutableListOf<RaidMeetupParticipant>()
            for (childSnapshot in dataSnapshot.child("participants").children) {
                KotlinUtils.safeLet(childSnapshot.key, childSnapshot.value as? String) { key, value ->
                    participants.add(RaidMeetupParticipant(id, key, value))
                }
            }

            val chats = mutableListOf<FirebaseChat>()
            for (childSnapshot in dataSnapshot.child("chat").children) {
                FirebaseChat.new(id, childSnapshot)?.let { chats.add(it) }
            }

            Log.v(TAG, "id: $id, meetupTime: meetupTime, participants: participants")

            return if (meetupTime != null) {
                FirebaseRaidMeetup(id, meetupTime, participants, chats)
            } else {
                null
            }
        }
    }
}