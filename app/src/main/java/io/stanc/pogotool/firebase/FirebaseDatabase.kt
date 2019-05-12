package io.stanc.pogotool.firebase

import android.util.Log
import com.google.firebase.database.*
import io.stanc.pogotool.firebase.data.RaidMeetup
import io.stanc.pogotool.firebase.node.*
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.utils.Async
import io.stanc.pogotool.utils.Async.awaitResponse
import io.stanc.pogotool.utils.KotlinUtils
import io.stanc.pogotool.utils.ObserverManager
import kotlinx.coroutines.*
import java.lang.Exception
import java.lang.ref.WeakReference

class FirebaseDatabase(pokestopDelegate: Delegate<FirebasePokestop>? = null,
                       arenaDelegate: Delegate<FirebaseArena>? = null) {

    private val TAG = this.javaClass.name

    private val arenasDidChangeCallback = object: FirebaseServer.OnNodeDidChangeCallback {
        private val arenaDelegate = WeakReference(arenaDelegate)

        override fun nodeChanged(dataSnapshot: DataSnapshot) {
            dataSnapshot.children.forEach { child ->
                FirebaseArena.new(child)?.let { this.arenaDelegate.get()?.onItemChanged(it) }
            }
        }
    }

    private val pokestopsDidChangeCallback = object: FirebaseServer.OnNodeDidChangeCallback {
        private val pokestopDelegate = WeakReference(pokestopDelegate)

        override fun nodeChanged(dataSnapshot: DataSnapshot) {
//            Log.i(TAG, "Debug:: nodeChanged(dataSnapshot: $dataSnapshot)")
            dataSnapshot.children.forEach { child ->
                FirebasePokestop.new(child)?.let { this.pokestopDelegate.get()?.onItemChanged(it) }
            }
        }
    }

    private val arenaDidChangeCallback = object : FirebaseServer.OnNodeDidChangeCallback {
        override fun nodeChanged(dataSnapshot: DataSnapshot) {
            FirebaseArena.new(dataSnapshot)?.let { arena ->
                arenaObserverManager.observers(arena.id).filterNotNull().forEach { it.onItemChanged(arena) }
            }
        }
    }

    /**
     * delegation & observing
     */

    interface Delegate<Item> {
        fun onItemAdded(item: Item)
        fun onItemChanged(item: Item)
        fun onItemRemoved(item: Item)
    }

    interface Observer<Item> {
        fun onItemChanged(item: Item)
    }

    private val arenaObserverManager = ObserverManager<Observer<FirebaseArena>>()

    /**
     * arenas
     */

    fun loadArenas(geoHash: GeoHash) {
        FirebaseServer.addNodeEventListener("$DATABASE_ARENAS/$geoHash", arenasDidChangeCallback)
    }

    fun pushArena(arena: FirebaseArena) {
        FirebaseServer.createNodeByAutoId(arena.databasePath(), arena.data())
    }

    fun addObserver(observer: Observer<FirebaseArena>, arena: FirebaseArena) {
        FirebaseServer.addNodeEventListener("${arena.databasePath()}/${arena.id}", arenaDidChangeCallback)
        arenaObserverManager.addObserver(observer, subId = arena.id)
    }

    fun removeObserver(observer: Observer<FirebaseArena>, arena: FirebaseArena) {
        FirebaseServer.removeNodeEventListener("${arena.databasePath()}/${arena.id}", arenaDidChangeCallback)
        arenaObserverManager.removeObserver(observer, subId = arena.id)
    }

    /**
     * pokestops
     */

    fun loadPokestops(geoHash: GeoHash) {
        FirebaseServer.addNodeEventListener("$DATABASE_POKESTOPS/$geoHash", pokestopsDidChangeCallback)
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

                val pokestopsGeoHashes: List<GeoHash> = awaitResponse { loadSubscriptionsFromDatabase(DATABASE_POKESTOPS, it) }
                val arenaGeoHashes: List<GeoHash> = awaitResponse { loadSubscriptionsFromDatabase(DATABASE_ARENAS, it) }

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

    fun subscribeForPush(geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        Log.v(TAG, "subscribeForPush(geoHash: $geoHash), userID: ${FirebaseUser.userData?.id}, notificationToken: ${FirebaseUser.userData?.notificationToken}")
        KotlinUtils.safeLet(FirebaseUser.userData?.id, FirebaseUser.userData?.notificationToken) { uid, notificationToken ->

            subscribeFor(FirebaseSubscription.Type.Arena, uid, notificationToken, geoHash, onCompletionCallback)
            subscribeFor(FirebaseSubscription.Type.Pokestop, uid, notificationToken, geoHash, onCompletionCallback)
        }
    }

    private fun subscribeFor(type: FirebaseSubscription.Type, uid: String, notificationToken: String, geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        val data = FirebaseSubscription(id = notificationToken, uid = uid, geoHash = geoHash, type = type)
        FirebaseServer.createNode(data, callbackForVoid(onCompletionCallback))
    }

    // TODO: remove subscriptions in user data on firebase server
    // TODO: for arena
    // TODO: for pokestops
    fun removeSubscription(geoHash: GeoHash) {
        FirebaseUser.userData?.notificationToken?.let { userToken ->
            Log.e(TAG, "NOT implemented yet: removeSubscription($geoHash) !")
//            FirebaseServer.removeValue()
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

    private fun loadSubscriptionsFromDatabase(databasePath: String, onResponse: Async.Response<List<GeoHash>>) {

        FirebaseUser.userData?.notificationToken?.let { userToken ->

            FirebaseServer.requestDataChilds(databasePath, object : FirebaseServer.OnCompleteCallback<List<DataSnapshot>> {

                override fun onSuccess(data: List<DataSnapshot>?) {
                    Log.d(TAG, "Debug:: loadSubscriptions onSuccess($data)")
                    data?.let {
                        val geoHashes = registeredGeoHashes(it, userToken)
                        onResponse.onCompletion(geoHashes)
                    }
                }

                override fun onFailed(message: String?) {
                    Log.d(TAG, "loadSubscriptions failed. Error: $message")
                    onResponse.onException(Exception(message))
                }

            })

        } ?: kotlin.run {
            val exception = Exception("tried to load subscriptions from database, but user: ${FirebaseUser.userData}, and notificationToken: ${FirebaseUser.userData?.notificationToken}")
            onResponse.onException(exception)
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

    private fun registeredGeoHashes(dataSnapshots: List<DataSnapshot>, usersNotificationToken: String): List<GeoHash> {

        val geoHashes = mutableListOf<GeoHash>()

        for (child in dataSnapshots) {
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