package io.stanc.pogotool.firebase.data

import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.MapFragment
import io.stanc.pogotool.geohash.GeoHash
import java.util.*

class FirebasePokestop(
    override val id: String,
    val name: String,
    val geoHash: GeoHash,
    val submitter: String): FirebaseItem {

    override fun databasePath(): String {
        return "items/${geoHash.toString().substring(0, MapFragment.GEO_HASH_AREA_PRECISION)}"
    }

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data["name"] = name
        data["latitude"] = geoHash.toLocation().latitude
        data["longitude"] = geoHash.toLocation().longitude
        data["submitter"] = submitter

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
            val name = dataSnapshot.child("name").value as? String
            val latitude = (dataSnapshot.child("latitude").value as? Number)?.toDouble() ?: kotlin.run {
                (dataSnapshot.child("latitude").value as? String)?.toDouble()
            }
            val longitude = (dataSnapshot.child("longitude").value as? Number)?.toDouble() ?: kotlin.run {
                (dataSnapshot.child("longitude").value as? String)?.toDouble()
            }
            val submitter = dataSnapshot.child("submitter").value as? String

//            Log.v(TAG, "id: $id, name: $name, latitudeNum: $latitudeNum, latitude: $latitude, longitudeNum: $longitudeNum, longitude: $longitude")

            if (id != null && name != null && latitude != null && longitude != null && submitter != null) {
                val geoHash = GeoHash(latitude, longitude)
                return FirebasePokestop(id, name, geoHash, submitter)
            }

            return null
        }
    }
}