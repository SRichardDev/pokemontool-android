package io.stanc.pogotool.firebase

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import io.stanc.pogotool.firebase.data.FirebaseArena
import io.stanc.pogotool.firebase.data.FirebaseItem
import io.stanc.pogotool.firebase.data.FirebasePokestop
import io.stanc.pogotool.firebase.data.FirebaseSubscription
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.utils.KotlinUtils
import java.lang.ref.WeakReference

class FirebaseDatabase(pokestopDelegate: Delegate<FirebasePokestop>,
                       arenaDelegate: Delegate<FirebaseArena>) {

    private val TAG = this.javaClass.name

    /**
     * data
     */

    interface Delegate<Item> {
        fun onItemAdded(item: Item)
        fun onItemChanged(item: Item)
        fun onItemRemoved(item: Item)
    }

    class ItemEventListener<Item: FirebaseItem>(delegate: Delegate<Item>, private val newItem: (p0: DataSnapshot) -> Item?): ChildEventListener {
        private val TAG = this.javaClass.name

        val items: HashMap<String, Item> = HashMap()
        private val delegate = WeakReference(delegate)

        override fun onCancelled(p0: DatabaseError) {
            Log.e(TAG, "onCancelled(error: ${p0.code}, message: ${p0.message}) for ItemEventListener")
        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
            Log.v(TAG, "onChildMoved(${p0.value}, p1: $p1), p0.key: ${p0.key} for ItemEventListener")
        }

        override fun onChildChanged(p0: DataSnapshot, p1: String?) {
            Log.d(TAG, "onChildChanged(${p0.value}, p1: $p1), p0.key: ${p0.key} for ItemEventListener")
            newItem(p0)?.let {
                removeItem(it)
                addItem(it)
            }
        }

        override fun onChildAdded(p0: DataSnapshot, p1: String?) {
            Log.v(TAG, "onChildAdded(${p0.value}, p1: $p1), p0.key: ${p0.key} for ItemEventListener")
            newItem(p0)?.let { addItem(it) }
        }

        override fun onChildRemoved(p0: DataSnapshot) {
            Log.w(TAG, "onChildRemoved(${p0.value}), p0.key: ${p0.key} for ItemEventListener")
            newItem(p0)?.let { removeItem(it) }
        }

        private fun addItem(item: Item) {
            Log.v(TAG, "addItem($item) 1, items: $items")
            if (!items.containsKey(item.id)) {
                Log.v(TAG, "addItem($item) 2")
                items[item.id] = item
                delegate.get()?.onItemAdded(item)
            }
        }

        private fun removeItem(item: Item) {
            if (items.containsKey(item.id)) {
                items.remove(item.id)
                delegate.get()?.onItemRemoved(item)
            }
        }
    }

    /**
     * arenas
     */

    private val databaseArena = FirebaseServer.database.child("arenas")
    private val arenaEventListener: ChildEventListener = ItemEventListener(arenaDelegate) { dataSnapshot-> FirebaseArena.new(dataSnapshot) }

    fun loadArenas(geoHash: GeoHash) {
        databaseArena.child(geoHash.toString()).removeEventListener(arenaEventListener)
        databaseArena.child(geoHash.toString()).addChildEventListener(arenaEventListener)
    }

    fun pushArena(arena: FirebaseArena) {

        firebaseGeoHash(arena.geoHash)?.let { childGeoHash ->
            databaseArena.child(childGeoHash.toString()).push().setValue(arena.data())
        }
    }

    /**
     * pokestops
     */

    private val databasePokestop = FirebaseServer.database.child("test_pokestops")
//    private val databasePokestop = FirebaseServer.database.child("pokestops")
    private val pokestopEventListener: ChildEventListener = ItemEventListener(pokestopDelegate) { dataSnapshot-> FirebasePokestop.new(dataSnapshot) }

    fun loadPokestops(geoHash: GeoHash) {
        databasePokestop.child(geoHash.toString()).removeEventListener(pokestopEventListener)
        databasePokestop.child(geoHash.toString()).addChildEventListener(pokestopEventListener)
    }

    fun pushPokestop(pokestop: FirebasePokestop) {

        firebaseGeoHash(pokestop.geoHash)?.let { childGeoHash ->
            databasePokestop.child(childGeoHash.toString()).push().setValue(pokestop.data())
        }
    }

    /**
     * data subscription
     */

    fun subscribeForPush(geoHash: GeoHash, onCompletedCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        Log.v(TAG, "subscribeForPush(geoHash: $geoHash), userID: ${FirebaseServer.currentUser?.id}, notificationToken: ${FirebaseServer.currentUser?.notificationToken}")
        KotlinUtils.safeLet(FirebaseServer.currentUser?.id, FirebaseServer.currentUser?.notificationToken) { id, token ->

            subscribeFor(FirebaseSubscription.Type.Arena, id, token, geoHash, onCompletedCallback)
            subscribeFor(FirebaseSubscription.Type.Pokestop, id, token, geoHash, onCompletedCallback)
        }
    }

    private fun subscribeFor(type: FirebaseSubscription.Type, userId: String, userToken: String, geoHash: GeoHash, onCompletedCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        val data = FirebaseSubscription(userId, userToken, geoHash, type)
        FirebaseServer.sendData(data, onCompletedCallback)
    }

    private fun firebaseGeoHash(geoHash: GeoHash): GeoHash? {

        val precision = geoHash.toString().length
        return if (precision >= 6) {
            GeoHash(geoHash.toString().substring(0, 6))
        } else {
            null
        }
    }
}