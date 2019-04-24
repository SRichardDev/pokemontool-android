package io.stanc.pogotool.firebase.node

import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.firebase.FirebaseDatabase
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_RAID
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.map.MapGridProvider.Companion.GEO_HASH_AREA_PRECISION

data class FirebaseArena(
    override val id: String,
    val name: String,
    val geoHash: GeoHash,
    val submitter: String,
    val isEX: Boolean = false,
    val raid: FirebaseRaid? = null): FirebaseNode {

    override fun databasePath() = "${FirebaseDatabase.DATABASE_ARENAS}/${geoHash.toString().substring(0, GEO_HASH_AREA_PRECISION)}"

    override fun data(): Map<String, Any> {
        val data = HashMap<String, Any>()

        data["name"] = name
        data["isEX"] = isEX
        data["latitude"] = geoHash.toLocation().latitude
        data["longitude"] = geoHash.toLocation().longitude
        data["submitter"] = submitter

        return data
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseArena? {

            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
            val name = dataSnapshot.child("name").value as? String
            val isEX = (dataSnapshot.child("isEX").value as? Boolean) ?: kotlin.run {
                (dataSnapshot.child("isEX").value as? String)?.toBoolean()
            }
            val latitude = (dataSnapshot.child("latitude").value as? Number)?.toDouble() ?: kotlin.run {
                (dataSnapshot.child("latitude").value as? String)?.toDouble()
            }
            val longitude = (dataSnapshot.child("longitude").value as? Number)?.toDouble() ?: kotlin.run {
                (dataSnapshot.child("longitude").value as? String)?.toDouble()
            }
            val submitter = dataSnapshot.child("submitter").value as? String

            Log.d(TAG, "Debug:: childs: ${dataSnapshot.children}")
            val raid = FirebaseRaid.new(dataSnapshot.child(DATABASE_RAID))

            Log.v(TAG, "id: $id, name: $name, isEX: $isEX, latitude: $latitude, longitude: $longitude, submitter: $submitter")

            if (id != null && name != null && isEX != null && latitude != null && longitude != null && submitter != null) {
                val geoHash = GeoHash(latitude, longitude)
                return FirebaseArena(id, name, geoHash, submitter, isEX, raid)
            }

            return null
        }
    }
}