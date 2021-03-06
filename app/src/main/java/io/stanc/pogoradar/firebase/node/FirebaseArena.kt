package io.stanc.pogoradar.firebase.node

import android.os.Parcelable
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.DatabaseKeys.ARENAS
import io.stanc.pogoradar.firebase.DatabaseKeys.IS_EX
import io.stanc.pogoradar.firebase.DatabaseKeys.NAME
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_LATITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_LONGITUDE
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBMITTER
import io.stanc.pogoradar.firebase.DatabaseKeys.firebaseGeoHash
import io.stanc.pogoradar.geohash.GeoHash
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FirebaseArena private constructor(
    override val id: String,
    val name: String,
    val geoHash: GeoHash,
    val submitter: String,
    val isEX: Boolean = false,
    val raid: FirebaseRaid? = null): FirebaseNode, Parcelable {

    override fun databasePath() = "$ARENAS/${firebaseGeoHash(geoHash)}"

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data[NAME] = name
        data[IS_EX] = isEX
        data[NOTIFICATION_LATITUDE] = geoHash.toLocation().latitude
        data[NOTIFICATION_LONGITUDE] = geoHash.toLocation().longitude
        data[SUBMITTER] = submitter

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseArena? {

//            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val name = dataSnapshot.child(NAME).value as? String
            val isEX = (dataSnapshot.child(IS_EX).value as? Boolean) ?: run {
                (dataSnapshot.child(IS_EX).value as? String)?.toBoolean()
            }
            val latitude = (dataSnapshot.child(NOTIFICATION_LATITUDE).value as? Number)?.toDouble() ?: run {
                (dataSnapshot.child(NOTIFICATION_LATITUDE).value as? String)?.toDouble()
            }
            val longitude = (dataSnapshot.child(NOTIFICATION_LONGITUDE).value as? Number)?.toDouble() ?: run {
                (dataSnapshot.child(NOTIFICATION_LONGITUDE).value as? String)?.toDouble()
            }
            val submitter = dataSnapshot.child(SUBMITTER).value as? String

//            Log.v(TAG, "id: $id, name: $name, isEX: $isEX, latitude: $latitude, longitude: $longitude, submitter: $submitter")

            if (id != null && name != null && isEX != null && latitude != null && longitude != null && submitter != null) {
                val geoHash = GeoHash(latitude, longitude)
                val raid = FirebaseRaid.new(id, geoHash, dataSnapshot.child(RAID))
                return FirebaseArena(id, name, geoHash, submitter, isEX, raid)
            }

            return null
        }

        fun new(name: String, geoHash: GeoHash, user: String, isEX: Boolean): FirebaseArena {
            return FirebaseArena("", name, geoHash, user, isEX)
        }
    }
}