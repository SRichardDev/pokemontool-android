package io.stanc.pogotool.firebase.node

import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.firebase.DatabaseKeys.LATITUDE
import io.stanc.pogotool.firebase.DatabaseKeys.LONGITUDE
import io.stanc.pogotool.firebase.DatabaseKeys.NAME
import io.stanc.pogotool.firebase.DatabaseKeys.POKESTOPS
import io.stanc.pogotool.firebase.DatabaseKeys.QUEST
import io.stanc.pogotool.firebase.DatabaseKeys.SUBMITTER
import io.stanc.pogotool.firebase.DatabaseKeys.firebaseGeoHash
import io.stanc.pogotool.geohash.GeoHash
import java.util.*

data class FirebasePokestop private constructor(
    override val id: String,
    val name: String,
    val geoHash: GeoHash,
    val submitter: String,
    val quest: FirebaseQuest? = null): FirebaseNode {

    override fun databasePath(): String {
        return "$POKESTOPS/${firebaseGeoHash(geoHash)}"
    }

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[NAME] = name
        data[LATITUDE] = geoHash.toLocation().latitude
        data[LONGITUDE] = geoHash.toLocation().longitude
        data[SUBMITTER] = submitter

        return data
    }


    companion object {

        private val TAG = javaClass.name

        fun id(dataSnapshot: DataSnapshot): String? {
            return dataSnapshot.key
        }

        fun new(dataSnapshot: DataSnapshot): FirebasePokestop? {
//            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = id(dataSnapshot)
            val name = dataSnapshot.child(NAME).value as? String
            val latitude = (dataSnapshot.child(LATITUDE).value as? Number)?.toDouble() ?: kotlin.run {
                (dataSnapshot.child(LATITUDE).value as? String)?.toDouble()
            }
            val longitude = (dataSnapshot.child(LONGITUDE).value as? Number)?.toDouble() ?: kotlin.run {
                (dataSnapshot.child(LONGITUDE).value as? String)?.toDouble()
            }
            val submitter = dataSnapshot.child(SUBMITTER).value as? String

//            Log.v(TAG, "id: $id, name: $name, latitude: $latitude, longitude: $longitude, submitter: $submitter")

            if (id != null && name != null && latitude != null && longitude != null && submitter != null) {
                val geoHash = GeoHash(latitude, longitude)
                val quest = FirebaseQuest.new(id, geoHash, dataSnapshot.child(QUEST))
                return FirebasePokestop(id, name, geoHash, submitter, quest)
            }

            return null
        }

        fun new(name: String, geoHash: GeoHash, userId: String): FirebasePokestop {
            return FirebasePokestop("", name, geoHash, userId)
        }
    }
}