package io.stanc.pogoradar.firebase.node

import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.DatabaseKeys.POKESTOPS
import io.stanc.pogoradar.firebase.DatabaseKeys.QUEST
import io.stanc.pogoradar.firebase.DatabaseKeys.QUEST_ID
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBMITTER
import io.stanc.pogoradar.firebase.DatabaseKeys.TIMESTAMP
import io.stanc.pogoradar.firebase.DatabaseKeys.firebaseGeoHash
import io.stanc.pogoradar.firebase.FirebaseServer
import io.stanc.pogoradar.geohash.GeoHash

data class FirebaseQuest private constructor(
    override val id: String,
    val definitionId: String,
    val submitter: String,
    val timestamp: Any,
    val geoHash: GeoHash,
    val pokestopId: String): FirebaseNode {

    override fun databasePath(): String = "$POKESTOPS/${firebaseGeoHash(geoHash)}/$pokestopId/$QUEST"

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[QUEST_ID] = definitionId
        data[SUBMITTER] = submitter
        data[TIMESTAMP] = timestamp

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(pokestopId: String, geoHash: GeoHash, dataSnapshot: DataSnapshot): FirebaseQuest? {
//            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val definitionId = dataSnapshot.child(QUEST_ID).value as? String
            val submitter = dataSnapshot.child(SUBMITTER).value as? String
            val timestamp = dataSnapshot.child(TIMESTAMP).value as? Long

//            Log.v(TAG, "id: $id, definitionId: $definitionId, submitter: $submitter, timestamp: $timestamp, pokestopId: $pokestopId, geoHash: $geoHash")
            if (id != null && definitionId != null && submitter != null && timestamp != null) {
                return FirebaseQuest(id, definitionId, submitter, timestamp, geoHash, pokestopId)
            }

            return null
        }

        fun new(pokestopId: String, geoHash: GeoHash, questDefinitionId: String, userId: String): FirebaseQuest {
            return FirebaseQuest(QUEST, questDefinitionId, userId, FirebaseServer.timestamp(), geoHash, pokestopId)
        }
    }
}