package io.stanc.pogoradar.firebase.node

import android.os.Parcelable
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.DatabaseKeys.POKESTOPS
import io.stanc.pogoradar.firebase.DatabaseKeys.QUEST
import io.stanc.pogoradar.firebase.DatabaseKeys.QUEST_ID
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBMITTER_ID
import io.stanc.pogoradar.firebase.DatabaseKeys.TIMESTAMP
import io.stanc.pogoradar.firebase.DatabaseKeys.firebaseGeoHash
import io.stanc.pogoradar.firebase.FirebaseServer
import io.stanc.pogoradar.firebase.FirebaseServer.TIMESTAMP_SERVER
import io.stanc.pogoradar.geohash.GeoHash
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FirebaseQuest private constructor(
    override val id: String,
    val definitionId: String,
    val submitter: String,
    val timestamp: Long,
    val geoHash: GeoHash,
    val pokestopId: String): FirebaseDataNode, Parcelable {

    override fun databasePath(): String = "$POKESTOPS/${firebaseGeoHash(geoHash)}/$pokestopId"

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[QUEST_ID] = definitionId
        data[SUBMITTER_ID] = submitter
        data[TIMESTAMP] = if(timestamp == TIMESTAMP_SERVER) FirebaseServer.timestamp() else timestamp

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(pokestopId: String, geoHash: GeoHash, dataSnapshot: DataSnapshot): FirebaseQuest? {
//            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val definitionId = dataSnapshot.child(QUEST_ID).value as? String
            val submitter = dataSnapshot.child(SUBMITTER_ID).value as? String
            val timestamp = dataSnapshot.child(TIMESTAMP).value as? Long

//            Log.v(TAG, "id: $id, definitionId: $definitionId, submitterId: $submitterId, timestamp: $timestamp, pokestopId: $pokestopId, geoHash: $geoHash")
            if (id != null && definitionId != null && submitter != null && timestamp != null) {
                return FirebaseQuest(id, definitionId, submitter, timestamp, geoHash, pokestopId)
            }

            return null
        }

        fun new(pokestopId: String, geoHash: GeoHash, questDefinitionId: String, userId: String): FirebaseQuest {
            return FirebaseQuest(QUEST, questDefinitionId, userId, TIMESTAMP_SERVER, geoHash, pokestopId)
        }
    }
}