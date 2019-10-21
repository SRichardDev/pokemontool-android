package io.stanc.pogoradar.firebase.node

import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.DatabaseKeys.CHAT
import io.stanc.pogoradar.firebase.DatabaseKeys.MEETUP_TIME
import io.stanc.pogoradar.firebase.DatabaseKeys.PARTICIPANTS
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_MEETUPS
import io.stanc.pogoradar.firebase.DatabaseKeys.TIMESTAMP_NONE

data class FirebaseRaidMeetup(
    override val id: String,
    val meetupTimestamp: Long,
    var participantUserIds: MutableList<String>,
    val chat: List<FirebaseChat>): FirebaseNode {

    override fun databasePath(): String = RAID_MEETUPS

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[MEETUP_TIME] = meetupTimestamp

        val dataParticipants = HashMap<String, String>()
        participantUserIds.forEach{ dataParticipants[it] = "" }
        data[PARTICIPANTS] = dataParticipants

        data[CHAT] = chat

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseRaidMeetup? {
            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key ?: run { return null }

            val meetupTimestamp = dataSnapshot.child(MEETUP_TIME).value as? Long

            val participantUserIds: List<String> = dataSnapshot.child(PARTICIPANTS).children.mapNotNull { it.key }

            val chat = mutableListOf<FirebaseChat>()
            for (chatSnapshot in dataSnapshot.child(CHAT).children) {
                FirebaseChat.new(id, chatSnapshot)?.let { chat.add(it) }
            }

            Log.v(TAG, "id: $id, meetupTimestamp: $meetupTimestamp, participantUserIds: $participantUserIds, chat: $chat")

            return if (meetupTimestamp != null) {
                FirebaseRaidMeetup(id, meetupTimestamp, participantUserIds.toMutableList(), chat)
            } else {
                null
            }
        }

        fun new(meetupTimestamp: Long): FirebaseRaidMeetup {
            return FirebaseRaidMeetup("", meetupTimestamp, participantUserIds = mutableListOf(), chat = emptyList())
        }

        fun new(id: String): FirebaseRaidMeetup {
            val meetupTimestamp = TIMESTAMP_NONE
            return FirebaseRaidMeetup(id, meetupTimestamp, participantUserIds = mutableListOf(), chat = emptyList())
        }
    }
}