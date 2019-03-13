package io.stanc.pogotool.firebase

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.stanc.pogotool.firebase.data.FirebaseArena
import io.stanc.pogotool.firebase.data.FirebaseData
import io.stanc.pogotool.firebase.data.FirebasePokestop
import io.stanc.pogotool.firebase.data.FirebaseSubscription
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.utils.KotlinUtils

class FirebaseDatabase {

    private val TAG = this.javaClass.name

    //    TODO?: use FirebaseFirestore instead of realtime FirebaseDatabase, but iOS uses FirebaseDatabase
//    private val firebase = FirebaseFirestore.getInstance()

    private val databaseArena = FirebaseServer.database.child("arenas")
    private val databasePokestop = FirebaseServer.database.child("test_pokestops") //  test_pokestops pokestops

    private val pokestops: HashMap<String, FirebasePokestop> = HashMap()
    private val arenas: HashMap<String, FirebaseArena> = HashMap()

    /**
     * data registration/getter
     */

    fun loadPokestops(geoHash: GeoHash) {

//        val pokestopEventListener = FirebasePokestop.DataEventListener(databasePokestop, geoHashParent, onNewPokestopCallback)
        databasePokestop.child(geoHash.toString()).addChildEventListener(object : ChildEventListener {

            override fun onCancelled(p0: DatabaseError) {
                Log.w(TAG, "onCancelled(error: ${p0.code}, message: ${p0.message}) for pokestopEventListener")
//                databaseReference.removeEventListener(this)
            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) { /* not needed */ }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                Log.w(TAG, "onChildChanged(${p0.value}, p1: $p1) for pokestopEventListener")
//                databaseReference.removeEventListener(this)
            }

            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                Log.w(TAG, "onChildAdded(${p0.value}, p1: $p1) for pokestopEventListener")
                p0.key?.let {
                    val geoHash = GeoHash(it)
                    Log.w(TAG, "onChildAdded() geoHash: $geoHash for pokestopEventListener")
//                    if (geoHashArea.boundingBox.contains(geoHash.toLocation())) {
////                    Log.d(TAG, "Debug:: dataSnapshot: $p0")
//                        p0.children.forEach { childDataSnapshot ->
//                            //                        Log.d(TAG, "Debug:: child: $childDataSnapshot")
//                            FirebasePokestop.new(childDataSnapshot)?.let { pokestop ->
//                                onNewPokestopCallback(pokestop)
//                            }
//                        }
//                    }
                }
            }

            override fun onChildRemoved(p0: DataSnapshot) {
                Log.w(TAG, "onChildRemoved(${p0.value}) for pokestopEventListener")
                /* not needed */ }
        })
    }

//    func loadPokestops(for geohash: String) {
//        guard geohash != "" else { return }
//        pokestopsRef.child(geohash).observe(.value, with: { snapshot in
//                if let result = snapshot.children.allObjects as? [DataSnapshot] {
//                    for child in result {
//                        guard let pokestop: Pokestop = decode(from: child) else { continue }
//
//                        if let localPokestop = self.pokestops[pokestop.id] {
//                            if localPokestop == pokestop { continue }
//                            print("Updated Pokestop")
//                            self.pokestops[pokestop.id] = pokestop
//                            self.delegate?.didUpdatePokestop(pokestop: pokestop)
//                        } else {
//                            print("Added Pokestop")
//                            self.pokestops[pokestop.id] = pokestop
//                            self.delegate?.didAddPokestop(pokestop: pokestop)
//                        }
//                    }
//                }
//        })
//    }

    fun requestForData(northeast: LatLng, southwest: LatLng, onNewArenaCallback: (arena: FirebaseArena) -> Unit, onNewPokestopCallback: (pokestop: FirebasePokestop) -> Unit) {

        val geoHashStringNE = GeoHash(northeast).toString()
        val geoHashStringSW = GeoHash(southwest).toString()
        // TODO: the commonPrefix geohash is too large !
        val geoHasMatrix = GeoHash.geoHashMatrix(northeast, southwest)
//        val geoHashParent = GeoHash(geoHashStringNE.commonPrefixWith(geoHashStringSW))
        Log.i(TAG, "requestForData geoHashStringNE: $geoHashStringNE, geoHashStringSW: $geoHashStringSW => geoHasMatrix: $geoHasMatrix")

//        val arenaEventListener = FirebaseArena.DataEventListener(databaseArena, geoHashParent, onNewArenaCallback)
//        databaseArena.addChildEventListener(arenaEventListener)

//        val databaseReferencePokestop = database.child(DATABASE_POKESTOPS)
//        val pokestopEventListener = FirebasePokestop.DataEventListener(databaseReferencePokestop, geoHashParent, onNewPokestopCallback)
//        databaseReferencePokestop.addChildEventListener(pokestopEventListener)

        // TODO: for debugging many points only
//        val pokestopEventListener = FirebasePokestop.DataEventListener(databasePokestop, geoHashParent, onNewPokestopCallback)
//        databasePokestop.addChildEventListener(pokestopEventListener)
    }


    // TODO: request data (all GeoHashes for arenas, pokestops and subscriptions)

    private fun registerForArea(geoHash: GeoHash, onDataChanged: (data: String) -> Unit) {

        val databaseChildPath = "${FirebaseServer.DATABASE_ARENAS}/$geoHash"
        FirebaseServer.registerForData(databaseChildPath, onDataChanged)
    }

    /**
     * data subscription
     */

    fun subscribeForPush(geoHash: GeoHash, onCompletedCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        Log.v(TAG, "subscribeForPush(geoHash: $geoHash), userID: ${FirebaseServer.currentUser.id}, notificationToken: ${FirebaseServer.currentUser.notificationToken}")
        KotlinUtils.safeLet(FirebaseServer.currentUser.id, FirebaseServer.currentUser.notificationToken) { id, token ->

            subscribeFor(FirebaseSubscription.Type.Arena, id, token, geoHash, onCompletedCallback)
            subscribeFor(FirebaseSubscription.Type.Pokestop, id, token, geoHash, onCompletedCallback)
        }
    }

    private fun subscribeFor(type: FirebaseSubscription.Type, userId: String, userToken: String, geoHash: GeoHash, onCompletedCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        val data = FirebaseSubscription(userId, userToken, geoHash, type)
        FirebaseServer.sendData(data, onCompletedCallback)
    }

    /**
     * data update
     */

    // TODO: Debug mode:

//    func savePokestop(_ pokestop: Pokestop) {
//        let data = try! FirebaseEncoder().encode(pokestop)
//            pokestopsRef.child(pokestop.geohash).childByAutoId().setValue(data)
//        }

//    func saveArena(_ arena: Arena) {
//        let data = try! FirebaseEncoder().encode(arena)
//            arenasRef.child(arena.geohash).childByAutoId().setValue(data)
//        }

    // TODO: save raid only !
    fun sendArena(name: String, geoHash: GeoHash, isEX: Boolean = false) {
//        val data = FirebaseArena(name, isEX, geoHash)
//        sendData(data)
    }
//    func saveRaid(arena: Arena) {
//        guard let arenaID = arena.id else { return }
//        let data = try! FirebaseEncoder().encode(arena.raid)
//            var data1 = data as! [String: Any]
//            data1["timestamp"] = ServerValue.timestamp()
//            arenasRef.child(arena.geohash).child(arenaID).child("raid").setValue(data1)
//        }

    fun sendPokestop(name: String, geoHash: GeoHash, questName: String = "debug Quest") {
//        val data = FirebasePokestop(name, geoHash)
//        sendData(data)
    }
}