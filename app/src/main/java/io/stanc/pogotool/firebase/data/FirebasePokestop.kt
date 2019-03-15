package io.stanc.pogotool.firebase.data

import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.MapFragment
import io.stanc.pogotool.geohash.GeoHash
import java.util.*

class FirebasePokestop(
    override val id: String,
    val name: String,
    val geoHash: GeoHash,
    val questName: String? = null,
    val questReward: String? = null
                       ): FirebaseItem {



    override fun databasePath(): String {
        return "items/${geoHash.toString().substring(0, MapFragment.GEO_HASH_AREA_PRECISION)}"
    }

    override fun data(): Map<String, String> {
        val data = HashMap<String, String>()
        data["name"] = name
        data["latitude"] = geoHash.toLocation().latitude.toString()
        data["longitude"] = geoHash.toLocation().longitude.toString()
        // quest...
        // submitter

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
            val latitudeNum = dataSnapshot.child("latitude").value as? Number
            val longitudeNum = dataSnapshot.child("longitude").value as? Number
            val latitude = latitudeNum?.toDouble()
            val longitude = longitudeNum?.toDouble()

            val questName = dataSnapshot.child("quest/name").value as? String
            val questReward = dataSnapshot.child("quest/reward").value as? String

//            Log.v(TAG, "id: $id, name: $name, latitudeNum: $latitudeNum, latitude: $latitude, longitudeNum: $longitudeNum, longitude: $longitude")

            if (id != null && name != null && latitude != null && longitude != null) {
                val geoHash = GeoHash(latitude, longitude)
                return FirebasePokestop(id, name, geoHash, questName, questReward)
            }

            return null
        }
    }
}