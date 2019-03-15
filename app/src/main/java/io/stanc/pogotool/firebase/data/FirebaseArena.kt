package io.stanc.pogotool.firebase.data

import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.MapFragment.Companion.GEO_HASH_AREA_PRECISION
import io.stanc.pogotool.geohash.GeoHash

data class FirebaseArena(
    override val id: String,
    val name: String,
    val isEX: Boolean,
    val geoHash: GeoHash): FirebaseItem {

    override fun databasePath(): String {
        return "arenas/${geoHash.toString().substring(0, GEO_HASH_AREA_PRECISION)}"
    }

    override fun data(): Map<String, String> {
        val data = HashMap<String, String>()
        data["name"] = name
        data["isEX"] = isEX.toString()
        data["latitude"] = geoHash.toLocation().latitude.toString()
        data["longitude"] = geoHash.toLocation().longitude.toString()

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseArena? {

//            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val name = dataSnapshot.child("name").value as? String
            val isEX = (dataSnapshot.child("isEX").value as? Boolean)
            val latitude = (dataSnapshot.child("latitude").value as? Number)?.toDouble()
            val longitude = (dataSnapshot.child("longitude").value as? Number)?.toDouble()

//            Log.v(TAG, "id: $id, name: $name, isEX: $isEX, latitude: $latitude, longitude: $longitude")

            if (id != null && name != null && isEX != null && latitude != null && longitude != null) {
                val geoHash = GeoHash(latitude, longitude)
                return FirebaseArena(id, name, isEX, geoHash)
            }

            return null
        }
    }
}