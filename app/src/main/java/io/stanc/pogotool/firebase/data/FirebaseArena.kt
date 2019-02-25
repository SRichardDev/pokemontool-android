package io.stanc.pogotool.firebase.data

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import io.stanc.pogotool.MapFragment.Companion.GEO_HASH_AREA_PRECISION
import io.stanc.pogotool.geohash.GeoHash

data class FirebaseArena(val id: String,
                         val name: String,
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

        return data
    }

    class DataEventListener(private val databaseReference: DatabaseReference,
                            private val geoHashArea: GeoHash,
                            private val onNewArenaCallback: (arena: FirebaseArena) -> Unit): ChildEventListener {

        override fun onCancelled(p0: DatabaseError) {
            Log.w(TAG, "onCancelled(error: ${p0.code}, message: ${p0.message}) for arenaEventListener")
            databaseReference.removeEventListener(this)
        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) { /* not needed */ }

        override fun onChildChanged(p0: DataSnapshot, p1: String?) {
            databaseReference.removeEventListener(this)
        }

        override fun onChildAdded(p0: DataSnapshot, p1: String?) {
            p0.key?.let {
                val geoHash = GeoHash(it)
                if (geoHashArea.boundingBox.contains(geoHash.toLocation())) {
//                    Log.d(TAG, "Debug:: dataSnapshot: $p0")
                    p0.children.forEach { childDataSnapshot ->
//                        Log.d(TAG, "Debug:: child: $childDataSnapshot")
                        FirebaseArena.new(childDataSnapshot)?.let { arena ->
                            onNewArenaCallback(arena)
                        }
                    }

                }
            }
        }

        override fun onChildRemoved(p0: DataSnapshot) { /* not needed */ }
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