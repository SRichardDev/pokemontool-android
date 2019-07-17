package io.stanc.pogoradar.firebase.node

import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.DatabaseKeys.CHAT
import io.stanc.pogoradar.firebase.DatabaseKeys.MEETUP_TIME
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_MEETUPS
import io.stanc.pogoradar.firebase.DatabaseKeys.PARTICIPANTS

data class FirebaseRaidMeetup(
    override val id: String,
    val meetupTime: String,
    var participantUserIds: MutableList<String>,
    val chats: List<FirebaseChat>): FirebaseNode {

    override fun databasePath(): String = RAID_MEETUPS

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[MEETUP_TIME] = meetupTime

        val dataParticipants = HashMap<String, String>()
        participantUserIds.forEach{ dataParticipants[it] = "" }
        data[PARTICIPANTS] = dataParticipants

        data[CHAT] = chats

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseRaidMeetup? {
            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key ?: run { return null }

            val meetupTime = dataSnapshot.child(MEETUP_TIME).value as? String

            val participantUserIds: List<String> = dataSnapshot.child(PARTICIPANTS).children.mapNotNull { it.key }

            val chats = mutableListOf<FirebaseChat>()
            for (chatSnapshot in dataSnapshot.child(CHAT).children) {
                FirebaseChat.new(id, chatSnapshot)?.let { chats.add(it) }
            }

            Log.v(TAG, "id: $id, meetupTime: $meetupTime, participantUserIds: $participantUserIds, chats: $chats")

            return if (meetupTime != null) {
                FirebaseRaidMeetup(id, meetupTime, participantUserIds.toMutableList(), chats)
            } else {
                null
            }
        }

        fun new(meetupTime: String): FirebaseRaidMeetup {
            return new("", meetupTime)
        }

        fun new(id: String, meetupTime: String): FirebaseRaidMeetup {
            return FirebaseRaidMeetup(id, meetupTime, participantUserIds = mutableListOf(), chats = emptyList())
        }
    }
}