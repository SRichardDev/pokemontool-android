package io.stanc.pogoradar.firebase

import android.util.Log
import com.google.firebase.database.*
import io.stanc.pogoradar.firebase.DatabaseKeys.ARENAS
import io.stanc.pogoradar.firebase.DatabaseKeys.GEO_HASH_AREA_PRECISION
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_BOSSES
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_MEETUPS
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_MEETUP_ID
import io.stanc.pogoradar.firebase.DatabaseKeys.PARTICIPANTS
import io.stanc.pogoradar.firebase.DatabaseKeys.POKESTOPS
import io.stanc.pogoradar.firebase.DatabaseKeys.QUESTS
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_BOSS_ID
import io.stanc.pogoradar.firebase.DatabaseKeys.REGISTERED_USERS
import io.stanc.pogoradar.firebase.DatabaseKeys.SubscriptionType
import io.stanc.pogoradar.firebase.DatabaseKeys.USERS
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_PUBLIC_DATA
import io.stanc.pogoradar.firebase.DatabaseKeys.firebaseGeoHash
import io.stanc.pogoradar.firebase.FirebaseServer.OnCompleteCallback
import io.stanc.pogoradar.firebase.FirebaseServer.requestNode
import io.stanc.pogoradar.firebase.node.*
import io.stanc.pogoradar.geohash.GeoHash
import io.stanc.pogoradar.utils.Async
import io.stanc.pogoradar.utils.Async.awaitResponse
import io.stanc.pogoradar.utils.Kotlin
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
        FirebaseServer.createNodeByAutoId(arena.databasePath(), arena.data())?.let { id ->
            FirebaseUser.saveSubmittedArena(id, arena.geoHash)
        }
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

    fun pushRaid(raid: FirebaseRaid, newRaidMeetup: FirebaseRaidMeetup? = null) {

        removeRaidMeetupIfExists(raid.databasePath()) {

            FirebaseServer.setNode(raid)
            newRaidMeetup?.let {
                pushRaidMeetup(raid.databasePath(), newRaidMeetup)
            }
        }

        FirebaseUser.saveSubmittedRaids()
    }

    fun pushRaidMeetup(raidDatabasePath: String, raidMeetup: FirebaseRaidMeetup): String? {
        val raidMeetupId = FirebaseServer.createNodeByAutoId(raidMeetup.databasePath(), raidMeetup.data())

        raidMeetupId?.let { id ->
            FirebaseServer.setData("$raidDatabasePath/$RAID_MEETUP_ID", id, callbackForVoid())
            pushRaidMeetupParticipation(id)
        }

        return raidMeetupId
    }

    private fun removeRaidMeetupIfExists(raidDatabasePath: String, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        FirebaseServer.requestDataValue("$raidDatabasePath/$RAID_MEETUP_ID", object: OnCompleteCallback<Any?> {
            override fun onSuccess(data: Any?) {

                (data as? String)?.let { nodeId ->

                    FirebaseServer.removeNode(RAID_MEETUPS, nodeId, object: OnCompleteCallback<Void> {
                        override fun onSuccess(data: Void?) {
                            onCompletionCallback(true)
                        }

                        override fun onFailed(message: String?) {
                            onCompletionCallback(false)
                        }
                    })

                } ?: run {
                    onCompletionCallback(false)
                }
            }

            override fun onFailed(message: String?) {
                Log.e(TAG, "requestDataValue($raidDatabasePath/$RAID_MEETUP_ID) onFailed. message: $message")
                onCompletionCallback(false)
            }
        })


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
            FirebaseServer.addData("$RAID_MEETUPS/$raidMeetupId/$PARTICIPANTS", it, "")
        } ?: run {
            Log.e(TAG, "could not push raid meetup participation, because User.userData?.id?: ${FirebaseUser.userData?.id}")
        }
    }

    fun cancelRaidMeetupParticipation(raidMeetupId: String) {
        FirebaseUser.userData?.id?.let {
            FirebaseServer.removeData("$RAID_MEETUPS/$raidMeetupId/$PARTICIPANTS/$it")
        } ?: run {
            Log.e(TAG, "could not cancel raid meetup participation, because User.userData?.id?: ${FirebaseUser.userData?.id}")
        }
    }

    fun loadPublicUser(userId: String, onCompletionCallback: (publicUser: FirebasePublicUser) -> Unit) {

        requestNode("$USERS/$userId/$USER_PUBLIC_DATA", object: OnCompleteCallback<DataSnapshot> {

            override fun onSuccess(data: DataSnapshot?) {

                data?.let {
                    FirebasePublicUser.new(userId, data)?.let { onCompletionCallback(it) }
                }
            }

            override fun onFailed(message: String?) {
                Log.e(TAG, "loadPublicUser($userId) failed. Error: $message")
            }

        })
    }

    /**
     * pokestops
     */

    fun loadPokestops(geoHash: GeoHash) {
        FirebaseServer.addNodeEventListener("$POKESTOPS/$geoHash", pokestopsDidChangeCallback)
    }

    fun pushPokestop(pokestop: FirebasePokestop) {
        FirebaseServer.createNodeByAutoId(pokestop.databasePath(), pokestop.data())?.let { id ->
            FirebaseUser.saveSubmittedPokestop(id, pokestop.geoHash)
        }
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

        FirebaseServer.requestDataChilds(RAID_BOSSES, object : OnCompleteCallback<List<DataSnapshot>> {

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

        FirebaseServer.requestDataChilds(QUESTS, object : OnCompleteCallback<List<DataSnapshot>> {

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
        FirebaseUser.saveSubmittedQuests()
    }

    /**
     * location subscriptions
     */

    fun loadSubscriptions(onCompletionCallback: (geoHashes: List<GeoHash>?) -> Unit) {

        FirebaseUser.userData?.let { user ->

            val pokestopsGeoHashes = user.subscribedGeohashPokestops
            val arenaGeoHashes = user.subscribedGeohashArenas
            notifyCompletionCallback(pokestopsGeoHashes, arenaGeoHashes, onCompletionCallback)

        } ?: run {
            Log.e(TAG, "geohash subscription loading failed. FirebaseUser.userData: ${FirebaseUser.userData}")
            onCompletionCallback(null)
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
        subscribeFor(SubscriptionType.Arena, geoHash, onCompletionCallback)
        subscribeFor(SubscriptionType.Pokestop, geoHash, onCompletionCallback)
    }

    private fun subscribeFor(type: SubscriptionType, geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        FirebaseUser.userData?.let { user ->

            Log.d(TAG, "Debug:: subscribeFor(${type.name}), uid: ${user.id}, geoHash: $geoHash")

            FirebaseServer.addData("${type.subscriptionDatabaseKey}/${firebaseGeoHash(geoHash)}", user.id, "", object : OnCompleteCallback<Void> {

                override fun onSuccess(data: Void?) {
                    FirebaseUser.updateUserSubscription(type, geoHash) { taskSuccessful ->
                        onCompletionCallback(taskSuccessful)
                    }
                }

                override fun onFailed(message: String?) {
                    Log.w(TAG, "subscription onFailed for (${type.subscriptionDatabaseKey}), uid: ${user.id}, geoHash: $geoHash [$message]")
                    onCompletionCallback(false)
                }
            })

        } ?: run {
            Log.e(TAG, "subscribeFor ${type.subscriptionDatabaseKey} in $geoHash failed. FirebaseUser.userData: ${FirebaseUser.userData}")
            onCompletionCallback(false)
        }
    }

    fun removeSubscription(geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        unsubscribeFor(SubscriptionType.Arena, geoHash, onCompletionCallback)
        unsubscribeFor(SubscriptionType.Pokestop, geoHash, onCompletionCallback)
    }

    private fun unsubscribeFor(type: SubscriptionType, geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        FirebaseUser.userData?.let { user ->

            Log.d(TAG, "Debug:: unsubscribeFor(${type.name}), uid: ${user.id}, geoHash: $geoHash")

            FirebaseServer.removeData("${type.subscriptionDatabaseKey}/${firebaseGeoHash(geoHash)}/${user.id}", object : OnCompleteCallback<Void> {

                override fun onSuccess(data: Void?) {
                    FirebaseUser.removeUserSubscription(type, geoHash) { taskSuccessful ->
                        onCompletionCallback(taskSuccessful)
                    }
                }

                override fun onFailed(message: String?) {
                    Log.w(TAG, "unsubscription onFailed for (${type.subscriptionDatabaseKey}), uid: ${user.id}, geoHash: $geoHash [$message]")
                    onCompletionCallback(false)
                }
            })

        } ?: run {
            Log.e(TAG, "unsubscribeFor ${type.subscriptionDatabaseKey} in $geoHash failed. FirebaseUser.userData: ${FirebaseUser.userData}")
            onCompletionCallback(false)
        }
    }

    /**
     * private implementations
     */

    private fun callbackForVoid(onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) = object : OnCompleteCallback<Void> {
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

            FirebaseServer.requestDataChilds(databasePath, object : OnCompleteCallback<List<DataSnapshot>> {

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

        } ?: run {
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
}