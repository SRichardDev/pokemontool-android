package io.stanc.pogotool.firebase.data

import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.geohash.MapFragment

class FirebasePokestop(val name: String,
                       val questName: String,
                       val geoHash: GeoHash): FirebaseData {



    override fun databasePath(): String {
        return "pokestops/${geoHash.toString().substring(0, MapFragment.GEO_HASH_AREA_PRECISION)}"
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
}