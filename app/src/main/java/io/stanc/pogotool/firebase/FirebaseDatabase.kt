package io.stanc.pogotool.firebase

import android.util.Log
import com.google.firebase.database.*
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

    private val raidMeetupDidChangeCallback = object : FirebaseServer.OnNodeDidChangeCallback {
        override fun nodeChanged(dataSnapshot: DataSnapshot) {
            FirebaseRaidMeetup.new(dataSnapshot)?.let { raidMeetup ->
                raidMeetupObserverManager.observers(raidMeetup.id).filterNotNull().forEach { it.onItemChanged(raidMeetup) }
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
    private val raidMeetupObserverManager = ObserverManager<Observer<FirebaseRaidMeetup>>()

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
     * raids & meetups
     */

    fun pushRaid(raid: FirebaseRaid, raidMeetup: FirebaseRaidMeetup? = null, userId: String? = null) {
        FirebaseServer.createNode(raid)
        raidMeetup?.let {
            val raidMeetupId = pushRaidMeetup(raidMeetup, userId)
            Log.i(TAG, "Debug:: pushRaid() raidMeetupId: $raidMeetupId")
            raidMeetupId?.let { id ->
                FirebaseServer.setData("${raid.databasePath()}/$DATABASE_ARENA_RAID_MEETUP_ID", id, callbackForVoid())
            }
        }
    }

    fun pushRaidMeetup(raidMeetup: FirebaseRaidMeetup, userId: String? = null): String? {
        val raidMeetupId = FirebaseServer.createNodeByAutoId(raidMeetup.databasePath(), raidMeetup.data())
        raidMeetupId?.let { pushRaidMeetupParticipation(it) }
        Log.i(TAG, "Debug:: pushRaidMeetup() raidMeetupId: $raidMeetupId")
        return raidMeetupId
    }

//    fun requestRaidMeetup(raidMeetupId: String, callback: FirebaseServer.OnCompleteCallback) {
//        FirebaseServer.requestDataValue("$DATABASE_ARENA_RAID_MEETUPS/$raidMeetupId", object: FirebaseServer.OnCompleteCallback<DataSnapshot>{
//            override fun onSuccess(data: DataSnapshot?) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//            }
//
//            override fun onFailed(message: String?) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//            }
//
//        })
//    }

    fun addObserver(observer: Observer<FirebaseRaidMeetup>, raidMeetupId: String) {
        FirebaseServer.addNodeEventListener("$DATABASE_ARENA_RAID_MEETUPS/$raidMeetupId", raidMeetupDidChangeCallback)
        raidMeetupObserverManager.addObserver(observer, subId = raidMeetupId)
    }

    fun removeObserver(observer: Observer<FirebaseRaidMeetup>, raidMeetupId: String) {
        FirebaseServer.removeNodeEventListener("$DATABASE_ARENA_RAID_MEETUPS/$raidMeetupId", raidMeetupDidChangeCallback)
        raidMeetupObserverManager.removeObserver(observer, subId = raidMeetupId)
    }

    fun pushRaidMeetupParticipation(raidMeetupId: String) {

        FirebaseUser.userData?.id?.let {
            FirebaseServer.setDataKey("$DATABASE_ARENA_RAID_MEETUPS/$raidMeetupId/$DATABASE_ARENA_RAID_MEETUP_PARTICIPANTS", it)
        } ?: kotlin.run {
            Log.e(TAG, "could not push raid meetup participation, because User.userData?.id?: ${FirebaseUser.userData?.id}")
        }
    }

    fun cancelRaidMeetupParticipation(raidMeetupId: String) {
        FirebaseUser.userData?.id?.let {
            FirebaseServer.removeData("$DATABASE_ARENA_RAID_MEETUPS/$raidMeetupId/$DATABASE_ARENA_RAID_MEETUP_PARTICIPANTS/$it")
        } ?: kotlin.run {
            Log.e(TAG, "could not cancel raid meetup participation, because User.userData?.id?: ${FirebaseUser.userData?.id}")
        }
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
//            FirebaseServer.removeData()
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
        const val DATABASE_ARENA_RAID_MEETUP_ID = "raidMeetupId"
        const val DATABASE_ARENA_RAID_MEETUP_PARTICIPANTS = "participants"

        //    const val DATABASE_POKESTOPS = "pokestops"
        const val DATABASE_POKESTOPS = "test_pokestops"

        const val DATABASE_USERS = "users"
        const val DATABASE_USER_NOTIFICATION_TOKEN = "notificationToken"
        const val DATABASE_USER_TRAINER_NAME = "trainerName"

        const val NOTIFICATION_DATA_LATITUDE = "latitude"
        const val NOTIFICATION_DATA_LONGITUDE = "longitude"
    }
}