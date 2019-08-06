package io.stanc.pogoradar.firebase

import android.util.Log
import com.google.firebase.database.*
import io.stanc.pogoradar.firebase.DatabaseKeys.ARENAS
import io.stanc.pogoradar.firebase.DatabaseKeys.GEO_HASH_AREA_PRECISION
import io.stanc.pogoradar.firebase.DatabaseKeys.MEETUP_TIME
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
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

    fun pushRaid(raid: FirebaseRaid, newRaidMeetup: FirebaseRaidMeetup) {
        removeRaidMeetupIfExists(raid.databasePath()) {

            FirebaseUser.userData?.id?.let { userId ->
                val userParticipants =  newRaidMeetup.meetupTime != DatabaseKeys.DEFAULT_MEETUP_TIME
                if (userParticipants && !newRaidMeetup.participantUserIds.contains(userId)) {
                    newRaidMeetup.participantUserIds.add(userId)
                }
            }

            createRaidMeetup(newRaidMeetup)?.let { raidMeetupId ->
                raid.raidMeetupId = raidMeetupId
            }

            FirebaseServer.setNode(raid)
            FirebaseUser.saveSubmittedRaids()
        }
    }

    private fun createRaidMeetup(raidMeetup: FirebaseRaidMeetup): String? {
        val raidMeetupId = FirebaseServer.createNodeByAutoId(raidMeetup.databasePath(), raidMeetup.data())
        return raidMeetupId
    }

    fun changeRaidMeetupTime(raidMeetupId: String, time: String) {
        FirebaseServer.setData("$RAID_MEETUPS/$raidMeetupId/$MEETUP_TIME", time, callbackForVoid())
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

        FirebaseServer.keepSyncDatabaseChilds(RAID_BOSSES)
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

        FirebaseServer.keepSyncDatabaseChilds(QUESTS)
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
     * location subscriptions - interface
     */

    fun loadSubscriptions(onCompletionCallback: (geoHashes: List<GeoHash>?) -> Unit) {

        FirebaseUser.userData?.let { user ->

            notifyCompletionCallback(user.subscribedGeohashPokestops, user.subscribedGeohashArenas, onCompletionCallback)

        } ?: run {
            Log.e(TAG, "geohash subscription loading failed. FirebaseUser.userData: ${FirebaseUser.userData}")
            onCompletionCallback(null)
        }
    }

    fun addSubscriptionForPush(geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        addSubscriptionFor(SubscriptionType.Arena, geoHash, onCompletionCallback)
        addSubscriptionFor(SubscriptionType.Pokestop, geoHash, onCompletionCallback)
    }

//    fun setSubscriptionsForPush(geoHashes: List<GeoHash>, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
//
//        val oldArenaGeoHashes = FirebaseUser.userData?.subscribedGeohashArenas ?: emptyList()
//        val oldPokestopGeoHashes = FirebaseUser.userData?.subscribedGeohashPokestops ?: emptyList()
//
//        Log.d(TAG, "Debug:: setSubscriptionsForPush($geoHashes) oldArenaGeoHashes: $oldArenaGeoHashes, oldPokestopGeoHashes: $oldPokestopGeoHashes")
//        setSubscriptionsFor(SubscriptionType.Arena, geoHashes, oldArenaGeoHashes, onCompletionCallback)
//        setSubscriptionsFor(SubscriptionType.Pokestop, geoHashes, oldPokestopGeoHashes, onCompletionCallback)
//    }

    fun removePushSubscription(geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        removeSubscriptionFor(SubscriptionType.Arena, geoHash, onCompletionCallback)
        removeSubscriptionFor(SubscriptionType.Pokestop, geoHash, onCompletionCallback)
    }

    fun removeAllPushSubscriptions(onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        FirebaseUser.userData?.let { user ->

            removeSubscriptionsFor(SubscriptionType.Arena, user.subscribedGeohashArenas, onCompletionCallback = {
                removeSubscriptionsFor(SubscriptionType.Pokestop, user.subscribedGeohashPokestops, onCompletionCallback)
            })
        }
    }

    /**
     * location subscriptions - implementation
     */

//    fun subscribeToTopic(topic: String, onCompletionCallback: OnCompleteCallback<Void>? = null) {
//        FirebaseMessaging.getInstance().subscribeToTopic(topic).addOnCompleteListener { task ->
//            onCompletionCallback?.let { callback<Void, Void>(task, it) }
//        }
//    }
//
//    fun subscribeFromTopic(topic: String, onCompletionCallback: OnCompleteCallback<Void>? = null) {
//        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).addOnCompleteListener { task ->
//            onCompletionCallback?.let { callback<Void, Void>(task, it) }
//        }
//    }

    private fun addSubscriptionFor(type: SubscriptionType, geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        FirebaseUser.userData?.let { user ->

            FirebaseServer.addData("${type.subscriptionDatabaseKey}/${firebaseGeoHash(geoHash)}", user.id, "", object : OnCompleteCallback<Void> {

                override fun onSuccess(data: Void?) {
                    FirebaseUser.addUserSubscription(type, geoHash) { taskSuccessful ->
                        onCompletionCallback(taskSuccessful)
                    }
                }

                override fun onFailed(message: String?) {
                    Log.w(TAG, "subscription onFailed for (${type.subscriptionDatabaseKey}), uid: ${user.id}, geoHash: $geoHash [$message]")
                    onCompletionCallback(false)
                }
            })

        } ?: run {
            Log.e(TAG, "addSubscriptionFor ${type.subscriptionDatabaseKey} in $geoHash failed. FirebaseUser.userData: ${FirebaseUser.userData}")
            onCompletionCallback(false)
        }
    }

//    private fun setSubscriptionsFor(type: SubscriptionType, newGeoHashes: List<GeoHash>, oldGeoHashes: List<GeoHash>, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
//
//        val geoHashesToAdd = newGeoHashes.minus(oldGeoHashes)
//        val geoHashesToRemove = oldGeoHashes.minus(newGeoHashes)
//        Log.i(TAG, "Debug:: setSubscriptionsFor(${type.name}) \ngeoHashesToAdd: $geoHashesToAdd\ngeoHashesToRemove: $geoHashesToRemove")
//
//        onCompletionCallback(false)
//        addSubscriptionsFor(type, geoHashesToAdd, onCompletionCallback = { firstTaskSuccessful ->
//            removeSubscriptionsFor(type, geoHashesToRemove, onCompletionCallback = { secondTaskSuccessful ->
//                onCompletionCallback(firstTaskSuccessful && secondTaskSuccessful)
//            })
//        })
//    }

    private fun addSubscriptionsFor(type: SubscriptionType, geoHashes: List<GeoHash>, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        FirebaseUser.userData?.let { user ->

            val data = HashMap<String, Any>()
            geoHashes.forEach { data[firebaseGeoHash(it)] = "" }

            FirebaseServer.addData(type.subscriptionDatabaseKey, data, object : OnCompleteCallback<Void> {

                override fun onSuccess(data: Void?) {
                    FirebaseUser.addUserSubscriptions(type, geoHashes) { taskSuccessful ->
                        onCompletionCallback(taskSuccessful)
                    }
                }

                override fun onFailed(message: String?) {
                    Log.w(TAG, "addSubscriptions failed for (${type.subscriptionDatabaseKey}), uid: ${user.id}, geoHashes: $geoHashes: [$message]")
                    onCompletionCallback(false)
                }
            })

        } ?: run {
            Log.e(TAG, "addSubscriptions ${type.subscriptionDatabaseKey} in $geoHashes failed. FirebaseUser.userData: ${FirebaseUser.userData}")
            onCompletionCallback(false)
        }
    }

    private fun removeSubscriptionFor(type: SubscriptionType, geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        FirebaseUser.userData?.let { user ->

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
            Log.e(TAG, "removeSubscriptionFor ${type.subscriptionDatabaseKey} in $geoHash failed. FirebaseUser.userData: ${FirebaseUser.userData}")
            onCompletionCallback(false)
        }
    }

    private fun removeSubscriptionsFor(type: SubscriptionType, geoHashes: List<GeoHash>?, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        var successful = true

        CoroutineScope(Dispatchers.Main).launch {

            geoHashes?.forEach { geoHash ->

                val result = async{ waitForRemoveSubscription(type, geoHash) }.await()
                successful = successful && result
            }

            onCompletionCallback(successful)
        }
    }

    suspend fun waitForRemoveSubscription(type: SubscriptionType, geoHash: GeoHash): Boolean =
        suspendCoroutine { result ->
            val callback = object : (Boolean) -> Unit {
                override fun invoke(taskSuccessful: Boolean) {
                    result.resume(taskSuccessful)
                }
            }
            removeSubscriptionFor(type, geoHash, callback)
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