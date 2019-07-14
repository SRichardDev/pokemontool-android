package io.stanc.pogoradar.firebase.node

import android.os.Parcelable
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_LATITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_LONGITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.NAME
import io.stanc.pogoradar.firebase.DatabaseKeys.POKESTOPS
import io.stanc.pogoradar.firebase.DatabaseKeys.QUEST
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBMITTER
import io.stanc.pogoradar.firebase.DatabaseKeys.firebaseGeoHash
import io.stanc.pogoradar.geohash.GeoHash
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class FirebasePokestop private constructor(
    override val id: String,
    val name: String,
    val geoHash: GeoHash,
    val submitter: String,
    val quest: FirebaseQuest? = null) : FirebaseNode, Parcelable {

    override fun databasePath(): String {
        return "$POKESTOPS/${firebaseGeoHash(geoHash)}"
    }

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[NAME] = name
        data[NOTIFICATION_LATITUDE] = geoHash.toLocation().latitude
        data[NOTIFICATION_LONGITUDE] = geoHash.toLocation().longitude
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
            val latitude = (dataSnapshot.child(NOTIFICATION_LATITUDE).value as? Number)?.toDouble() ?: run {
                (dataSnapshot.child(NOTIFICATION_LATITUDE).value as? String)?.toDouble()
            }
            val longitude = (dataSnapshot.child(NOTIFICATION_LONGITUDE).value as? Number)?.toDouble() ?: run {
                (dataSnapshot.child(NOTIFICATION_LONGITUDE).value as? String)?.toDouble()
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

        fun new(name: String, geoHash: GeoHash, user: String): FirebasePokestop {
            return FirebasePokestop("", name, geoHash, user)
        }
    }
}