package io.stanc.pogotool.firebase.data

import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.geohash.MapFragment.Companion.GEO_HASH_AREA_PRECISION

data class FirebaseArena(val name: String,
                         val isEX: Boolean,
                         val geoHash: GeoHash): FirebaseData {

    override fun databasePath(): String {
        return "arenas/${geoHash.toString().substring(0, GEO_HASH_AREA_PRECISION)}"
    }

    override fun data(): Map<String, String> {
        val data = HashMap<String, String>()
        data["name"] = name
        data["isEX"] = isEX.toString()
        data["latitude"] = geoHash.toLocation().latitude.toString()
        data["longitude"] = geoHash.toLocation().longitude.toString()
        // raid...
        // submitter

        return data
    }
}