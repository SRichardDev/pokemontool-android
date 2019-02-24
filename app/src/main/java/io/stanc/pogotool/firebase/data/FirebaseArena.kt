package io.stanc.pogotool.firebase.data

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import io.stanc.pogotool.MapFragment.Companion.GEO_HASH_AREA_PRECISION
import io.stanc.pogotool.firebase.FirebaseServer
import io.stanc.pogotool.geohash.GeoHash

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
//                        Log.v(TAG, "ARENAS: onChildAdded(key: ${p0.key}, value: ${p0.value}), childrenCount: ${p0.childrenCount}, children: ${p0.children}, p1: $p1")
//                        Log.d(TAG, "geoHashView for $objectID: $geoHashView contains geoHash: $geoHash")
                    FirebaseArena.new(p0)?.let { arena ->
                        Log.d(TAG, "Debug:: new Arena: ${arena.name} at ${arena.geoHash}")
                        onNewArenaCallback(arena)
                    }
                }
            }
        }

        override fun onChildRemoved(p0: DataSnapshot) { /* not needed */ }
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebaseArena? {

            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val name = dataSnapshot.child("name").value as? String
            val isEX = (dataSnapshot.child("isEX").value as? String)?.toBoolean()
            val latitude = (dataSnapshot.child("latitude").value as? String)?.toDouble()
            val longitude = (dataSnapshot.child("longitude").value as? String)?.toDouble()

//            Log.v(TAG, "dataSnapshot: name: $name, isEX: $isEX, latitude: $latitude, longitude: $longitude")

            if (name != null && isEX != null && latitude != null && longitude != null) {
                val geoHash = GeoHash(latitude, longitude)
                return FirebaseArena(name, isEX, geoHash)
            }

            return null
        }
    }
}