package io.stanc.pogotool.firebase.data

import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_ARENAS
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_POKESTOPS
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_REG_USER
import io.stanc.pogotool.geohash.GeoHash

data class FirebaseSubscription(
    override val id: String,
    val uid: String,
    val geoHash: GeoHash,
    val type: Type): FirebaseNode {

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
        // Hint: id := notificationToken
        data[id] = uid
        return data
    }
}