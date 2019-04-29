package io.stanc.pogotool.firebase

import android.util.Log
import com.google.firebase.database.*
import io.stanc.pogotool.firebase.data.RaidMeetup
import io.stanc.pogotool.firebase.node.*
import io.stanc.pogotool.firebase.node.FirebaseSubscription.Type
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.utils.Async
import io.stanc.pogotool.utils.Async.awaitResponse
import io.stanc.pogotool.utils.KotlinUtils
import kotlinx.coroutines.*
import java.lang.Exception
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

    class ItemEventListener<Item: FirebaseNode>(delegate: Delegate<Item>, private val newItem: (p0: DataSnapshot) -> Item?): ChildEventListener {
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
            if (!items.containsKey(item.id)) {
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

    private val databaseArena = FirebaseServer.database.child(DATABASE_ARENAS)
    private val arenaEventListener: ChildEventListener = ItemEventListener(arenaDelegate) { dataSnapshot-> FirebaseArena.new(dataSnapshot) }

    fun loadArenas(geoHash: GeoHash) {
        databaseArena.child(geoHash.toString()).removeEventListener(arenaEventListener)
        databaseArena.child(geoHash.toString()).addChildEventListener(arenaEventListener)
    }

    fun pushArena(arena: FirebaseArena) {
        FirebaseServer.createNodeByAutoId(arena.databasePath(), arena.data())
    }

    /**
     * pokestops
     */

    private val databasePokestop = FirebaseServer.database.child(DATABASE_POKESTOPS)
    private val pokestopEventListener: ChildEventListener = ItemEventListener(pokestopDelegate) { dataSnapshot-> FirebasePokestop.new(dataSnapshot) }

    fun loadPokestops(geoHash: GeoHash) {
        Log.d(TAG, "Debug:: loadPokestops for $geoHash")
        databasePokestop.child(geoHash.toString()).removeEventListener(pokestopEventListener)
        databasePokestop.child(geoHash.toString()).addChildEventListener(pokestopEventListener)
    }

    fun pushPokestop(pokestop: FirebasePokestop) {
        FirebaseServer.createNodeByAutoId(pokestop.databasePath(), pokestop.data())
    }

    /**
     * raid bosses
     */

    fun loadRaidBosses(onCompletionCallback: (raidBosses: List<FirebaseRaidboss>?) -> Unit) {

        FirebaseServer.requestDataChilds(DATABASE_ARENA_RAID_BOSSES, object : FirebaseServer.OnCompleteCallback<List<DataSnapshot>> {

            override fun onSuccess(data: List<DataSnapshot>?) {
                val raidBosses = mutableListOf<FirebaseRaidboss>()
                data?.forEach { FirebaseRaidboss.new(it)?.let { raidBosses.add(it) } }
                onCompletionCallback(raidBosses)
            }

            override fun onFailed(message: String?) {
                Log.d(TAG, "loadRaidBosses failed. Error: $message")
                onCompletionCallback(null)
            }

        })
    }

    fun pushRaid(raid: FirebaseRaid, raidMeetup: FirebaseRaidMeetup? = null) {
        FirebaseServer.createNode(raid)
        raidMeetup?.let {
            val raidMeetupId = FirebaseServer.createNodeByAutoId(raidMeetup.databasePath(), raidMeetup.data())
            Log.i(TAG, "Debug:: pushRaid, raidMeetupId: $raidMeetupId")
            raidMeetupId?.let { id ->
                val data = RaidMeetup(raid.databasePath(), id)
                FirebaseServer.setData(data, callbackForVoid())
            }
        }
    }

    /**
     * location subscriptions
     */

    fun loadSubscriptions(onCompletionCallback: (geoHashes: List<GeoHash>?) -> Unit) {

        GlobalScope.launch(Dispatchers.Default) {

            try {

                val pokestopsGeoHashes: List<GeoHash> = awaitResponse { loadSubscriptionsFromDatabase(databasePokestop, it) }
                val arenaGeoHashes: List<GeoHash> = awaitResponse { loadSubscriptionsFromDatabase(databaseArena, it) }

                notifyCompletionCallback(pokestopsGeoHashes, arenaGeoHashes, onCompletionCallback)

            } catch (e: Exception) {
                Log.e(TAG, "geohash loading failed. Exception: ${e.message}")
                notifyCompletionCallback(null, null, onCompletionCallback)
            }
        }
    }

    private fun notifyCompletionCallback(geoHashList1: List<GeoHash>?, geoHashList2: List<GeoHash>?, onCompletionCallback: (geoHashes: List<GeoHash>?) -> Unit) {

        CoroutineScope(Dispatchers.Main).launch {

            var geoHashes: List<GeoHash>? = null

            if (geoHashList1 != null && geoHashList2 != null) {
                geoHashes = geoHashList1.union(geoHashList2).distinct()

            } else if (geoHashList1 != null) {
                geoHashes = geoHashList1

            } else if (geoHashList2 != null) {
                geoHashes = geoHashList2
            }

            onCompletionCallback(geoHashes)
        }
    }

    fun removeAllSubscriptions() {
        removeSubscriptionsFromDatabase(databasePokestop, Type.Pokestop)
        removeSubscriptionsFromDatabase(databaseArena, Type.Arena)
    }

//    fun removeAllSubscriptions(onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
//
//        GlobalScope.launch(Dispatchers.Default) {
//
//            val firstTaskSuccessful: Boolean = awaitResponse { removeSubscriptionsFromDatabase(databasePokestop, Type.Pokestop, it) }
//            val secondTaskSuccessful: Boolean = awaitResponse { removeSubscriptionsFromDatabase(databaseArena, Type.Arena, it) }
//
//            CoroutineScope(Dispatchers.Main).launch {
//                onCompletionCallback(firstTaskSuccessful && secondTaskSuccessful)
//            }
//        }
//    }

    fun subscribeForPush(geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        Log.v(TAG, "subscribeForPush(geoHash: $geoHash), userID: ${FirebaseUser.currentUser?.id}, notificationToken: ${FirebaseUser.currentUser?.notificationToken}")
        KotlinUtils.safeLet(FirebaseUser.currentUser?.id, FirebaseUser.currentUser?.notificationToken) { uid, notificationToken ->

            subscribeFor(FirebaseSubscription.Type.Arena, uid, notificationToken, geoHash, onCompletionCallback)
            subscribeFor(FirebaseSubscription.Type.Pokestop, uid, notificationToken, geoHash, onCompletionCallback)
        }
    }

    private fun subscribeFor(type: FirebaseSubscription.Type, uid: String, notificationToken: String, geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        val data = FirebaseSubscription(id = notificationToken, uid = uid, geoHash = geoHash, type = type)
        FirebaseServer.createNode(data, callbackForVoid(onCompletionCallback))
    }

    // TODO: add onCompletionCallBack for removing ...
    fun removeSubscription(geoHash: GeoHash) {
        FirebaseUser.currentUser?.notificationToken?.let { userToken ->
            databaseArena.child(geoHash.toString()).child(DATABASE_REG_USER).child(userToken).removeValue()
            databasePokestop.child(geoHash.toString()).child(DATABASE_REG_USER).child(userToken).removeValue()

        }
    }

    /**
     * private implementations
     */

    private fun callbackForVoid(onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) = object : FirebaseServer.OnCompleteCallback<Void> {
        override fun onSuccess(data: Void?) {
            onCompletionCallback(true)
        }

        override fun onFailed(message: String?) {
            Log.d(TAG, "Data request failed. Error: $message")
            onCompletionCallback(false)
        }
    }

    private fun loadSubscriptionsFromDatabase(database: DatabaseReference, onResponse: Async.Response<List<GeoHash>>) {

        FirebaseUser.currentUser?.notificationToken?.let { userToken ->

            database.addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    Log.e(TAG, "onCancelled(${p0.message}) for loadSubscriptionsFromDatabase(database: $database)")
                    onResponse.onException(p0.toException())
                }

                override fun onDataChange(p0: DataSnapshot) {
                    Log.d(TAG, "Debug::load onDataChange(${p0.value})")
                    val geoHashes = registeredGeoHashes(p0, userToken)
                    onResponse.onCompletion(geoHashes)
                }
            })
        } ?: kotlin.run {
            val exception = Exception("tried to load subscriptions from database, but user: ${FirebaseUser.currentUser}, and notificationToken: ${FirebaseUser.currentUser?.notificationToken}")
            onResponse.onException(exception)
        }
    }

    private fun removeSubscriptionsFromDatabase(database: DatabaseReference, type: FirebaseSubscription.Type) {

        KotlinUtils.safeLet(FirebaseUser.currentUser?.id, FirebaseUser.currentUser?.notificationToken) { uid, userToken ->

            database.addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    Log.e(TAG, "onCancelled(${p0.message}) for loadSubscriptionsFromDatabase(database: $database)")
                }

                override fun onDataChange(p0: DataSnapshot) {
                    Log.d(TAG, "Debug::remove onDataChange(${p0.value})")
                    val geoHashes = registeredGeoHashes(p0, userToken)
                    for (geoHash in geoHashes) {
                        val data = FirebaseSubscription(id = userToken, uid = uid, geoHash = geoHash, type = type)
                        FirebaseServer.removeNode(data)
                    }
                }
            })
        }
    }

    fun formattedFirebaseGeoHash(geoHash: GeoHash): GeoHash? {

        val precision = geoHash.toString().length
        return if (precision >= 6) {
            GeoHash(geoHash.toString().substring(0, 6))
        } else {
            null
        }
    }

    private fun registeredGeoHashes(snapshot: DataSnapshot, usersNotificationToken: String): List<GeoHash> {

        val geoHashes = mutableListOf<GeoHash>()

        for (child in snapshot.children) {
            for (registered_user in child.child(DATABASE_REG_USER).children) {
//                Log.v(TAG, "Debug:: registered_user key: ${registered_user.key}, value: ${registered_user.value}")
                if (registered_user.key == usersNotificationToken) {
//                    Log.d(TAG, "Debug:: new geoHash: key: ${child.key}, value: ${child.value}")
                    val geoHash = GeoHash(child.key as String)
                    geoHashes.add(geoHash)
                }
            }
        }

        return geoHashes
    }

    /**
     * database constants
     */

    companion object {

        const val MAX_SUBSCRIPTIONS = 10

        const val DATABASE_REG_USER = "registered_user"

        const val DATABASE_ARENAS = "arenas"
        const val DATABASE_ARENA_RAID = "raid"
        const val DATABASE_ARENA_RAID_BOSSES = "raidBosses"
        const val DATABASE_ARENA_RAID_MEETUPS = "raidMeetups"

        //    const val DATABASE_POKESTOPS = "pokestops"
        const val DATABASE_POKESTOPS = "test_pokestops"

        const val DATABASE_USERS = "users"
        const val DATABASE_USER_NOTIFICATION_TOKEN = "notificationToken"
        const val DATABASE_USER_TRAINER_NAME = "trainerName"

        const val NOTIFICATION_DATA_LATITUDE = "latitude"
        const val NOTIFICATION_DATA_LONGITUDE = "longitude"
    }
}