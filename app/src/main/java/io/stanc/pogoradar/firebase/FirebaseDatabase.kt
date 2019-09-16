package io.stanc.pogoradar.firebase

import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.DatabaseKeys.ARENAS
import io.stanc.pogoradar.firebase.DatabaseKeys.CHAT
import io.stanc.pogoradar.firebase.DatabaseKeys.GEO_HASH_AREA_PRECISION
import io.stanc.pogoradar.firebase.DatabaseKeys.MEETUP_TIME
import io.stanc.pogoradar.firebase.DatabaseKeys.PARTICIPANTS
import io.stanc.pogoradar.firebase.DatabaseKeys.POKESTOPS
import io.stanc.pogoradar.firebase.DatabaseKeys.QUESTS
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_BOSSES
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_BOSS_ID
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_MEETUPS
import io.stanc.pogoradar.firebase.DatabaseKeys.RAID_MEETUP_ID
import io.stanc.pogoradar.firebase.DatabaseKeys.REGISTERED_USERS
import io.stanc.pogoradar.firebase.DatabaseKeys.USERS
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_PUBLIC_DATA
import io.stanc.pogoradar.firebase.DatabaseKeys.firebaseGeoHash
import io.stanc.pogoradar.firebase.FirebaseServer.OnCompleteCallback
import io.stanc.pogoradar.firebase.node.*
import io.stanc.pogoradar.firebase.notification.FirebaseNotification
import io.stanc.pogoradar.geohash.GeoHash
import kotlinx.coroutines.*


class FirebaseDatabase {

    private val TAG = this.javaClass.name

    /**
     * observing
     */

    private val pokestopObserverManager = FirebaseNodeObserverManager(newFirebaseNode = { dataSnapshot ->
        FirebasePokestop.new(dataSnapshot)
    })
    private val raidMeetupObserverManager = FirebaseNodeObserverManager(newFirebaseNode = { dataSnapshot ->
        FirebaseRaidMeetup.new(dataSnapshot)
    })

    /**
     * arenas
     */

    fun loadArenas(geoHashes: List<GeoHash>, onCompletionCallback: (arenas: List<FirebaseArena>?) -> Unit) {

        GlobalScope.launch(Dispatchers.Default) {

            val arenas = mutableListOf<FirebaseArena>()

            geoHashes.forEach { geoHash ->
                val result = async {
                    FirebaseServer.requestNode("$ARENAS/$geoHash")
                }

                result.await()?.children?.forEach { child ->
                    FirebaseArena.new(child)?.let {
                        arenas.add(it)
                    }
                }
            }

            CoroutineScope(Dispatchers.Main).launch { onCompletionCallback(arenas) }
        }
    }

    fun loadArenas(geoHash: GeoHash, onCompletionCallback: (arenas: List<FirebaseArena>?) -> Unit) {
        FirebaseServer.requestNode("$ARENAS/$geoHash", object: OnCompleteCallback<DataSnapshot> {

            override fun onSuccess(data: DataSnapshot?) {

                val arenas = mutableListOf<FirebaseArena>()

                data?.children?.forEach { child ->
                    FirebaseArena.new(child)?.let {
                        arenas.add(it)
                    }
                }

                onCompletionCallback(arenas)
            }

            override fun onFailed(message: String?) {
                Log.e(TAG, "loadArenas(geoHash: $geoHash) failed. Error: $message")
                onCompletionCallback(null)
            }
        })
    }

    fun pushArena(arena: FirebaseArena) {
        FirebaseServer.createNodeByAutoId(arena.databasePath(), arena.data())?.let { id ->
            FirebaseUser.saveSubmittedArena(id, arena.geoHash)
        }
    }

    /**
     * raids & meetups & chat
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
        FirebaseServer.setData("$RAID_MEETUPS/$raidMeetupId/$MEETUP_TIME", time)
    }

    private fun removeRaidMeetupIfExists(raidDatabasePath: String, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        FirebaseServer.requestDataValue("$raidDatabasePath/$RAID_MEETUP_ID", object: OnCompleteCallback<Any?> {
            override fun onSuccess(data: Any?) {

                (data as? String)?.let { nodeId ->

                    FirebaseServer.removeNode(RAID_MEETUPS, nodeId, onCompletionCallback)

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
        FirebaseServer.setData("$raidDatabasePath/$RAID_BOSS_ID", raidBoss.id)
    }

    fun addObserver(observer: FirebaseNodeObserver<FirebaseRaidMeetup>, raidMeetup: FirebaseRaidMeetup) {
        raidMeetupObserverManager.addObserver(observer, raidMeetup)
    }

    fun removeObserver(observer: FirebaseNodeObserver<FirebaseRaidMeetup>, raidMeetup: FirebaseRaidMeetup) {
        raidMeetupObserverManager.removeObserver(observer, raidMeetup)
    }

    fun pushRaidMeetupParticipation(raidMeetupId: String) {

        FirebaseUser.userData?.id?.let { userId ->
            FirebaseNotification.subscribeToRaidMeetup(raidMeetupId) { successful ->

                if (successful) {
                    FirebaseServer.addData("$RAID_MEETUPS/$raidMeetupId/$PARTICIPANTS", userId, "")
                } else {
                    Log.e(TAG, "could not push raidmeetup (with id: $raidMeetupId) participation, because subscribeToRaidMeetup failed.")
                }
            }
        } ?: run {
            Log.e(TAG, "could not push raid meetup participation, because User.userData?.id?: ${FirebaseUser.userData?.id}")
        }
    }

    fun cancelRaidMeetupParticipation(raidMeetupId: String) {
        FirebaseUser.userData?.id?.let { userId ->
            FirebaseNotification.unsubscribeFromRaidMeetup(raidMeetupId) { successful ->

                if (successful) {
                    FirebaseServer.removeData("$RAID_MEETUPS/$raidMeetupId/$PARTICIPANTS/$userId")
                } else {
                    Log.e(TAG, "could not cancel raidmeetup (with id: $raidMeetupId) participation, because unsubscribeFromRaidMeetup failed.")
                }
            }
        } ?: run {
            Log.e(TAG, "could not cancel raid meetup participation, because User.userData?.id?: ${FirebaseUser.userData?.id}")
        }
    }

    fun pushChatMessage(message: FirebaseChat): String? {
        val chatMessageId = FirebaseServer.createNodeByAutoId(message.databasePath(), message.data())
        return chatMessageId
    }

    fun addChatObserver(observer: FirebaseServer.OnChildDidChangeCallback, raidMeetupId: String) {
        FirebaseServer.addListEventListener("$RAID_MEETUPS/$raidMeetupId/$CHAT", observer)
    }

    fun removeChatObserver(observer: FirebaseServer.OnChildDidChangeCallback, raidMeetupId: String) {
        FirebaseServer.removeListEventListener("$RAID_MEETUPS/$raidMeetupId/$CHAT", observer)
    }

    fun loadPublicUser(userId: String, onCompletionCallback: (publicUser: FirebasePublicUser) -> Unit) {

        FirebaseServer.requestNode("$USERS/$userId/$USER_PUBLIC_DATA", object: OnCompleteCallback<DataSnapshot> {

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

    fun loadPokestops(geoHashes: List<GeoHash>, onCompletionCallback: (pokestops: List<FirebasePokestop>?) -> Unit) {

        GlobalScope.launch(Dispatchers.Default) {

            val pokestops = mutableListOf<FirebasePokestop>()

            geoHashes.forEach { geoHash ->
                val result = async {
                    FirebaseServer.requestNode("$POKESTOPS/$geoHash")
                }

                result.await()?.children?.forEach { child ->
                    FirebasePokestop.new(child)?.let {
                        pokestops.add(it)
                    }
                }
            }

            CoroutineScope(Dispatchers.Main).launch { onCompletionCallback(pokestops) }
        }
    }

    fun loadPokestops(geoHash: GeoHash, onCompletionCallback: (pokestops: List<FirebasePokestop>?) -> Unit) {
        FirebaseServer.requestNode("$POKESTOPS/$geoHash", object: OnCompleteCallback<DataSnapshot> {

            override fun onSuccess(data: DataSnapshot?) {

                val pokestops = mutableListOf<FirebasePokestop>()

                data?.children?.forEach { child ->
                    FirebasePokestop.new(child)?.let {
                        pokestops.add(it)
                    }
                }

                onCompletionCallback(pokestops)
            }

            override fun onFailed(message: String?) {
                Log.e(TAG, "loadPokestops(geoHash: $geoHash) failed. Error: $message")
                onCompletionCallback(null)
            }
        })
    }

    fun pushPokestop(pokestop: FirebasePokestop) {
        FirebaseServer.createNodeByAutoId(pokestop.databasePath(), pokestop.data())?.let { id ->
            FirebaseUser.saveSubmittedPokestop(id, pokestop.geoHash)
        }
    }

    fun addObserver(observer: FirebaseNodeObserver<FirebasePokestop>, pokestop: FirebasePokestop) {
        pokestopObserverManager.addObserver(observer, pokestop)
    }

    fun removeObserver(observer: FirebaseNodeObserver<FirebasePokestop>, pokestop: FirebasePokestop) {
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
     * private implementations
     */

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