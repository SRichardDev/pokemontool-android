package io.stanc.pogotool.firebase

import android.util.Log
import com.google.firebase.database.*
import io.stanc.pogotool.firebase.DatabaseKeys.ARENAS
import io.stanc.pogotool.firebase.DatabaseKeys.GEO_HASH_AREA_PRECISION
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_BOSSES
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_MEETUPS
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_MEETUP_ID
import io.stanc.pogotool.firebase.DatabaseKeys.PARTICIPANTS
import io.stanc.pogotool.firebase.DatabaseKeys.POKESTOPS
import io.stanc.pogotool.firebase.DatabaseKeys.QUESTS
import io.stanc.pogotool.firebase.DatabaseKeys.RAID_BOSS_ID
import io.stanc.pogotool.firebase.DatabaseKeys.REGISTERED_USERS
import io.stanc.pogotool.firebase.DatabaseKeys.firebaseGeoHash
import io.stanc.pogotool.firebase.node.*
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.utils.Async
import io.stanc.pogotool.utils.Async.awaitResponse
import io.stanc.pogotool.utils.Kotlin
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

        override fun nodeRemoved(key: String) {
            this.arenaDelegate.get()?.onItemRemoved(key)
        }
    }

    private val pokestopsDidChangeCallback = object: FirebaseServer.OnNodeDidChangeCallback {
        private val pokestopDelegate = WeakReference(pokestopDelegate)

        override fun nodeChanged(dataSnapshot: DataSnapshot) {
            dataSnapshot.children.forEach { child ->
                FirebasePokestop.new(child)?.let { this.pokestopDelegate.get()?.onItemChanged(it) }
            }
        }

        override fun nodeRemoved(key: String) {
            this.pokestopDelegate.get()?.onItemRemoved(key)
        }
    }

    /**
     * delegation & observing
     */

    interface Delegate<Item> {
        fun onItemAdded(item: Item)
        fun onItemChanged(item: Item)
        fun onItemRemoved(itemId: String)
    }

    private val arenaObserverManager = FirebaseNodeObserverManager(newFirebaseNode = { dataSnapshot ->
        FirebaseArena.new(dataSnapshot)
    })
    private val pokestopObserverManager = FirebaseNodeObserverManager(newFirebaseNode = { dataSnapshot ->
        FirebasePokestop.new(dataSnapshot)
    })
    private val raidMeetupObserverManager = FirebaseNodeObserverManager(newFirebaseNode = { dataSnapshot ->
        FirebaseRaidMeetup.new(dataSnapshot)
    })

    /**
     * arenas
     */

    fun loadArenas(geoHash: GeoHash) {
        FirebaseServer.addNodeEventListener("$ARENAS/$geoHash", arenasDidChangeCallback)
    }

    fun pushArena(arena: FirebaseArena) {
        FirebaseServer.createNodeByAutoId(arena.databasePath(), arena.data())
    }

    fun addObserver(observer: FirebaseNodeObserverManager.Observer<FirebaseArena>, arena: FirebaseArena) {
        arenaObserverManager.addObserver(observer, arena)
    }

    fun removeObserver(observer: FirebaseNodeObserverManager.Observer<FirebaseArena>, arena: FirebaseArena) {
        arenaObserverManager.removeObserver(observer, arena)
    }

    /**
     * raids & meetups
     */

    fun pushRaid(raid: FirebaseRaid, raidMeetup: FirebaseRaidMeetup? = null) {
        FirebaseServer.setNode(raid)
        raidMeetup?.let {
            pushRaidMeetup(raid.databasePath(), it)
        }
    }

    fun pushRaidMeetup(raidDatabasePath: String, raidMeetup: FirebaseRaidMeetup): String? {
        val raidMeetupId = FirebaseServer.createNodeByAutoId(raidMeetup.databasePath(), raidMeetup.data())

        raidMeetupId?.let { id ->
            FirebaseServer.setData("$raidDatabasePath/$RAID_MEETUP_ID", id, callbackForVoid())
            pushRaidMeetupParticipation(id)
        }
        return raidMeetupId
    }

    fun pushRaidBoss(raidDatabasePath: String, raidBoss: FirebaseRaidbossDefinition) {
        FirebaseServer.setData("$raidDatabasePath/$RAID_BOSS_ID", raidBoss.id, callbackForVoid())
    }

    fun addObserver(observer: FirebaseNodeObserverManager.Observer<FirebaseRaidMeetup>, raidMeetup: FirebaseRaidMeetup) {
        raidMeetupObserverManager.addObserver(observer, raidMeetup)
    }

    fun removeObserver(observer: FirebaseNodeObserverManager.Observer<FirebaseRaidMeetup>, raidMeetup: FirebaseRaidMeetup) {
        raidMeetupObserverManager.removeObserver(observer, raidMeetup)
    }

    fun pushRaidMeetupParticipation(raidMeetupId: String) {

        FirebaseUser.userData?.id?.let {
            FirebaseServer.setDataKey("$RAID_MEETUPS/$raidMeetupId/$PARTICIPANTS", it)
        } ?: kotlin.run {
            Log.e(TAG, "could not push raid meetup participation, because User.userData?.id?: ${FirebaseUser.userData?.id}")
        }
    }

    fun cancelRaidMeetupParticipation(raidMeetupId: String) {
        FirebaseUser.userData?.id?.let {
            FirebaseServer.removeData("$RAID_MEETUPS/$raidMeetupId/$PARTICIPANTS/$it")
        } ?: kotlin.run {
            Log.e(TAG, "could not cancel raid meetup participation, because User.userData?.id?: ${FirebaseUser.userData?.id}")
        }
    }

    /**
     * pokestops
     */

    fun loadPokestops(geoHash: GeoHash) {
        FirebaseServer.addNodeEventListener("$POKESTOPS/$geoHash", pokestopsDidChangeCallback)
    }

    fun pushPokestop(pokestop: FirebasePokestop) {
        FirebaseServer.createNodeByAutoId(pokestop.databasePath(), pokestop.data())
    }

    fun addObserver(observer: FirebaseNodeObserverManager.Observer<FirebasePokestop>, pokestop: FirebasePokestop) {
        pokestopObserverManager.addObserver(observer, pokestop)
    }

    fun removeObserver(observer: FirebaseNodeObserverManager.Observer<FirebasePokestop>, pokestop: FirebasePokestop) {
        pokestopObserverManager.removeObserver(observer, pokestop)
    }

    /**
     * raid bosses
     */

    fun loadRaidBosses(onCompletionCallback: (raidBosses: List<FirebaseRaidbossDefinition>?) -> Unit) {

        FirebaseServer.requestDataChilds(RAID_BOSSES, object : FirebaseServer.OnCompleteCallback<List<DataSnapshot>> {

            override fun onSuccess(data: List<DataSnapshot>?) {
                val raidBosses = mutableListOf<FirebaseRaidbossDefinition>()
                data?.forEach { FirebaseRaidbossDefinition.new(it)?.let { raidBosses.add(it) } }
                onCompletionCallback(raidBosses)
            }

            override fun onFailed(message: String?) {
                Log.e(TAG, "loadRaidBosses failed. Error: $message")
                onCompletionCallback(null)
            }

        })
    }

    /**
     * Quests
     */

    fun loadQuests(onCompletionCallback: (quests: List<FirebaseQuestDefinition>?) -> Unit) {

        FirebaseServer.requestDataChilds(QUESTS, object : FirebaseServer.OnCompleteCallback<List<DataSnapshot>> {

            override fun onSuccess(data: List<DataSnapshot>?) {
                val quests = mutableListOf<FirebaseQuestDefinition>()
                data?.forEach { FirebaseQuestDefinition.new(it)?.let { quests.add(it) } }
                onCompletionCallback(quests)
            }

            override fun onFailed(message: String?) {
                Log.e(TAG, "loadQuests failed. Error: $message")
                onCompletionCallback(null)
            }

        })
    }

    fun pushQuest(quest: FirebaseQuest) {
        FirebaseServer.setNode(quest)
    }

    /**
     * location subscriptions
     */

    fun loadSubscriptions(onCompletionCallback: (geoHashes: List<GeoHash>?) -> Unit) {

        GlobalScope.launch(Dispatchers.Default) {

            try {

                val pokestopsGeoHashes: List<GeoHash> = awaitResponse { loadSubscriptionsFromDatabase(POKESTOPS, it) }
                val arenaGeoHashes: List<GeoHash> = awaitResponse { loadSubscriptionsFromDatabase(ARENAS, it) }

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
        Kotlin.safeLet(FirebaseUser.userData?.id, FirebaseUser.userData?.notificationToken) { uid, notificationToken ->

            subscribeFor(FirebaseSubscription.Type.Arena, uid, notificationToken, geoHash, onCompletionCallback)
            subscribeFor(FirebaseSubscription.Type.Pokestop, uid, notificationToken, geoHash, onCompletionCallback)
        }
    }

    private fun subscribeFor(type: FirebaseSubscription.Type, uid: String, notificationToken: String, geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        val data = FirebaseSubscription(id = notificationToken, uid = uid, geoHash = geoHash, type = type)
        FirebaseServer.updateNode(data, callbackForVoid(onCompletionCallback))
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
            Log.e(TAG, "Data request failed. Error: $message")
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
        return if (precision >= GEO_HASH_AREA_PRECISION) {
            GeoHash(firebaseGeoHash(geoHash))
        } else {
            null
        }
    }

    private fun registeredGeoHashes(dataSnapshots: List<DataSnapshot>, usersNotificationToken: String): List<GeoHash> {

        val geoHashes = mutableListOf<GeoHash>()

        for (child in dataSnapshots) {
            for (registered_user in child.child(REGISTERED_USERS).children) {
                if (registered_user.key == usersNotificationToken) {
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
    }
}