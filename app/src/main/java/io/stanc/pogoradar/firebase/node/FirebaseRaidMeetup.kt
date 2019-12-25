package io.stanc.pogoradar.firebase.node

import android.os.Parcelable
import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.DatabaseKeys
import io.stanc.pogoradar.firebase.DatabaseKeys.CHAT_ID
import io.stanc.pogoradar.firebase.DatabaseKeys.MEETUP_TIME
import io.stanc.pogoradar.firebase.DatabaseKeys.PARTICIPANTS
import io.stanc.pogoradar.geohash.GeoHash
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FirebaseRaidMeetup private constructor(
    override val id: String,
    private val parentDatabasePath: String,
    val chatId: String,
    val meetupTimestamp: Long,
    var participantUserIds: MutableList<String>): FirebaseNode, Parcelable {

    override fun databasePath(): String = parentDatabasePath

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[CHAT_ID] = chatId
        data[MEETUP_TIME] = meetupTimestamp

        val dataParticipants = HashMap<String, String>()
        participantUserIds.forEach{ dataParticipants[it] = "" }
        data[PARTICIPANTS] = dataParticipants

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun parentDatabasePath(geoHash: GeoHash, arenaId: String): String {
            return "${DatabaseKeys.ARENAS}/${DatabaseKeys.firebaseGeoHash(geoHash)}/$arenaId/${DatabaseKeys.RAID}"
        }

        fun new(parentDatabasePath: String, dataSnapshot: DataSnapshot): FirebaseRaidMeetup? {
            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key

            val chatId = dataSnapshot.child(CHAT_ID).value as? String
            val meetupTimestamp = dataSnapshot.child(MEETUP_TIME).value as? Long
            val participantUserIds: List<String> = dataSnapshot.child(PARTICIPANTS).children.mapNotNull { it.key }

            Log.v(TAG, "id: $id, meetupTimestamp: $meetupTimestamp, participantUserIds: $participantUserIds")

            return if (id != null && meetupTimestamp != null && chatId != null) {
                FirebaseRaidMeetup(id, parentDatabasePath, chatId, meetupTimestamp, participantUserIds.toMutableList())
            } else {
                null
            }
        }

        fun new(parentDatabasePath: String, meetupTimestamp: Long, chatId: String): FirebaseRaidMeetup {
            val id = DatabaseKeys.RAID_MEETUP
            return FirebaseRaidMeetup(id, parentDatabasePath, chatId, meetupTimestamp, participantUserIds = mutableListOf())
        }
    }
}