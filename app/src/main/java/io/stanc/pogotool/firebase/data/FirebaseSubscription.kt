package io.stanc.pogotool.firebase.data

import io.stanc.pogotool.firebase.FirebaseServer.DATABASE_ARENAS
import io.stanc.pogotool.firebase.FirebaseServer.DATABASE_POKESTOPS
import io.stanc.pogotool.firebase.FirebaseServer.DATABASE_REG_USER
import io.stanc.pogotool.geohash.GeoHash

class FirebaseSubscription(val userId: String,
                           val userToken: String,
                           val geoHash: GeoHash,
                           val type: Type): FirebaseData {

    enum class Type {
        Arena,
        Pokestop
    }

    override fun databasePath(): String {
        return when(type) {
            Type.Arena -> "$DATABASE_ARENAS/$geoHash/$DATABASE_REG_USER"
            Type.Pokestop -> "$DATABASE_POKESTOPS/$geoHash/$DATABASE_REG_USER"
        }
    }

    override fun data(): Map<String, String> {
        val data = HashMap<String, String>()
        data[userToken] = userId
        return data
    }
}