package io.stanc.pogotool.firebase.node

import io.stanc.pogotool.firebase.DatabaseKeys.ARENAS
import io.stanc.pogotool.firebase.DatabaseKeys.POKESTOPS
import io.stanc.pogotool.firebase.DatabaseKeys.REGISTERED_USERS
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
            Type.Arena -> "$ARENAS/$geoHash/$REGISTERED_USERS"
            Type.Pokestop -> "$POKESTOPS/$geoHash/$REGISTERED_USERS"
        }
    }

    override fun data(): Map<String, String> {
        val data = HashMap<String, String>()
        // Hint: id := notificationToken
        data[id] = uid
        return data
    }
}