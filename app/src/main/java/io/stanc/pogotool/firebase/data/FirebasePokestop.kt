package io.stanc.pogotool.firebase.data

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import io.stanc.pogotool.MapFragment
import io.stanc.pogotool.geohash.GeoHash
import java.util.*

class FirebasePokestop(val id: String,
                       val name: String,
                       val geoHash: GeoHash,
                       val questName: String? = null,
                       val questReward: String? = null
                       ): FirebaseData {



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

    class DataEventListener(private val databaseReference: DatabaseReference,
                            private val geoHashArea: GeoHash,
                            private val onNewPokestopCallback: (pokestop: FirebasePokestop) -> Unit): ChildEventListener {

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
                        FirebasePokestop.new(childDataSnapshot)?.let { pokestop ->
                            onNewPokestopCallback(pokestop)
                        }
                    }
                }
            }
        }

        override fun onChildRemoved(p0: DataSnapshot) { /* not needed */ }
    }

    companion object {

        private val TAG = javaClass.name

        fun new(dataSnapshot: DataSnapshot): FirebasePokestop? {

//            Log.v(TAG, "dataSnapshot: ${dataSnapshot.value}")

            val id = dataSnapshot.key
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