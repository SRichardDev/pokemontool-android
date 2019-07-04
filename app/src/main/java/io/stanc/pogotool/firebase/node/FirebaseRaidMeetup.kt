package io.stanc.pogotool.firebase.node

import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.firebase.DatabaseKeys.CHAT
import io.stanc.pogotool.firebase.DatabaseKeys.MEETUP_TIME
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_MEETUPS
import io.stanc.pogotool.firebase.DatabaseKeys.PARTICIPANTS

data class FirebaseRaidMeetup(
    override val id: String,
    val meetupTime: String,
    val participantUserIds: List<String>,
    val chat: List<FirebaseChat>): FirebaseNode {

    override fun databasePath(): String = RAID_MEETUPS

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[MEETUP_TIME] = meetupTime
        data[PARTICIPANTS] = participantUserIds
        data[CHAT] = participantUserIds

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
            for (childSnapshot in dataSnapshot.child(CHAT).children) {
                FirebaseChat.new(id, childSnapshot)?.let { chats.add(it) }
            }

//            Log.v(TAG, "id: $id, meetupTime: $meetupTime, participantUserIds: $participantUserIds")

            return if (meetupTime != null) {
                FirebaseRaidMeetup(id, meetupTime, participantUserIds, chats)
            } else {
                null
            }
        }
    }
}