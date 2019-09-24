package io.stanc.pogoradar.firebase.notification

import android.util.Log
import io.stanc.pogoradar.UpdateManager
import io.stanc.pogoradar.firebase.DatabaseKeys
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBSCRIBED_GEOHASHES
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBSCRIBED_RAID_MEETUPS
import io.stanc.pogoradar.firebase.DatabaseKeys.USERS
import io.stanc.pogoradar.firebase.DatabaseKeys.firebaseGeoHash
import io.stanc.pogoradar.firebase.FirebaseServer
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.firebase.node.FirebaseUserNode
import io.stanc.pogoradar.geohash.GeoHash
import io.stanc.pogoradar.utils.Async.waitForCompletion
import io.stanc.pogoradar.utils.TimeCalculator
import kotlinx.coroutines.*


object FirebaseNotification {

    private val TAG = javaClass.name

    // TODO: e.g. see user id: r8vOaq7Z2QPlZs2PfGrYqGoXckR2
    // TODO: Firebase:user
    // "isPushActive", "notificationToken", ""
    // write: "subscribedGeohashArenas" + "subscribedGeohashPokestops"
    // read: "subscribedGeohashArenas"

    // TODO: How check topics:
    // https://developers.google.com/instance-id/reference/server
    // https://iid.googleapis.com/iid/info/<notification token>?details=true Authorization:key=<YOUR_API_KEY>
    // https://iid.googleapis.com/iid/info/cpCLrdGUDbY:APA91bGbNSsYxbm4G39ESLXt_L99RTSu1Nn8Yku_vl6wRYOZ1dVgMwI4-c7UD0-TpdU4e_3uL1C7o9OKtsBd5tGZFDAyk7Kg9HnqEAYpbqniIEDHG64eDogaqEAEK3IxZCvHePZF_L3A?details=true Authorization:key=AIzaSyDQQVaPUFFHgzAc0rXD4FDXsR8CfbmKzdE

    /**
     * Area subscriptions
     */

    fun requestAreaSubscriptions(onCompletionCallback: (geoHashes: List<GeoHash>?) -> Unit) {

        FirebaseUser.userData?.let { user ->
            onCompletionCallback(user.subscribedGeohashes)

        } ?: run {
            Log.e(TAG, "geohash subscription loading failed. FirebaseUser.userData: ${FirebaseUser.userData}")
            onCompletionCallback(null)
        }
    }

    fun subscribeToArea(geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        FirebaseUser.userData?.let { user ->

            if (user.subscribedGeohashes?.contains(geoHash) == false) {

                val formattedGeoHash = firebaseGeoHash(geoHash)

                FirebaseServer.addData("$USERS/${user.id}/$SUBSCRIBED_GEOHASHES", formattedGeoHash, "") { successful ->

                    if (successful) {

                        FirebaseServer.subscribeToTopic(formattedGeoHash) { successful ->
//                            Log.v(TAG, "Debug:: subscribeToArea($geoHash), successful: $successful")

                            if (successful) {
                                onCompletionCallback(true)
                            } else {
                                FirebaseServer.removeData("$USERS/${user.id}/$SUBSCRIBED_GEOHASHES/$formattedGeoHash")
                                onCompletionCallback(false)
                            }
                        }

                    } else {
                        onCompletionCallback(false)
                    }
                }
            }

        } ?: run {
            Log.e(TAG, "subscription failed for area $geoHash. userData: ${FirebaseUser.userData}")
            onCompletionCallback(false)
        }
    }

    fun unsubscribeFromArea(geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        FirebaseUser.userData?.let { user ->

            if (user.subscribedGeohashes?.contains(geoHash) == true) {

                val formattedGeoHash = firebaseGeoHash(geoHash)

                FirebaseServer.removeData("$USERS/${user.id}/$SUBSCRIBED_GEOHASHES/$formattedGeoHash") { successful ->

                    if (successful) {

                        FirebaseServer.unsubscribeFromTopic(formattedGeoHash) { successful ->
//                            Log.v(TAG, "Debug:: unsubscribeFromArea($geoHash), unsubscribeFromTopic: $successful")
                            if (successful) {
                                onCompletionCallback(true)
                            } else {
                                FirebaseServer.addData("$USERS/${user.id}/$SUBSCRIBED_GEOHASHES", formattedGeoHash, "")
                                onCompletionCallback(false)
                            }
                        }
                    } else {
                        onCompletionCallback(false)
                    }
                }
            }

        } ?: run {
            Log.e(TAG, "remove subscription failed for area $geoHash. userData: ${FirebaseUser.userData}")
            onCompletionCallback(false)
        }
    }

    suspend fun unsubscribeFromArea(geoHash: GeoHash): Boolean {

        var successful = false

        FirebaseUser.userData?.let { user ->

            if (user.subscribedGeohashes?.contains(geoHash) == true) {

                val formattedGeoHash = firebaseGeoHash(geoHash)

                successful = waitForCompletion {
                    FirebaseServer.removeData("$USERS/${user.id}/$SUBSCRIBED_GEOHASHES/$formattedGeoHash", it)
                }

                if (successful) {
                    successful = waitForCompletion {
                        FirebaseServer.unsubscribeFromTopic(formattedGeoHash, it)
                    }
                }
            }
        }

        return successful
    }


    fun unsubscribeFromAllAreas(onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        GlobalScope.launch(Dispatchers.Default) {

            FirebaseUser.userData?.let { user ->

                var successfullyUnsubscribedAllAreas = true

                user.subscribedGeohashes?.forEach { geoHash ->
                    val result = async {
                        unsubscribeFromArea(
                            geoHash
                        )
                    }
                    if (!result.await()) {
                        successfullyUnsubscribedAllAreas = false
                    }
                }

                Log.v(TAG, "unsubscribeFromAllAreas() successfully: $successfullyUnsubscribedAllAreas")
                CoroutineScope(Dispatchers.Main).launch { onCompletionCallback(successfullyUnsubscribedAllAreas) }
            }
        }
    }

    /**
     * Raid meetup subscriptions
     */

    fun subscribeToRaidMeetup(raidMeetupId: String, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        FirebaseUser.userData?.let { user ->

            if (user.subscribedRaidMeetups?.contains(raidMeetupId) == false) {

                FirebaseServer.addData("$USERS/${user.id}/$SUBSCRIBED_RAID_MEETUPS", raidMeetupId, "") { successful ->

                    if (successful) {
                        FirebaseServer.subscribeToTopic(raidMeetupId) { successful ->
                            Log.v(TAG, "subscribeToRaidMeetup($raidMeetupId), successful: $successful")
                            if (successful) {
                                onCompletionCallback(true)
                            } else {
                                FirebaseServer.removeData("$USERS/${user.id}/$SUBSCRIBED_RAID_MEETUPS/$raidMeetupId")
                                onCompletionCallback(false)
                            }
                        }

                    } else {
                        onCompletionCallback(false)
                    }
                }
            }

        } ?: run {
            Log.e(TAG, "subscription failed for raidMeetup $raidMeetupId. userData: ${FirebaseUser.userData}")
            onCompletionCallback(false)
        }
    }

    fun unsubscribeFromRaidMeetup(raidMeetupId: String, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        FirebaseUser.userData?.let { user ->

            if (user.subscribedRaidMeetups?.contains(raidMeetupId) == true) {

                FirebaseServer.removeData("$USERS/${user.id}/$SUBSCRIBED_RAID_MEETUPS/$raidMeetupId") { successful ->

                    if (successful) {
                        FirebaseServer.unsubscribeFromTopic(raidMeetupId) { successful ->
                            Log.v(TAG, "unsubscribeFromRaidMeetup($raidMeetupId), successful: $successful")
                            if (successful) {
                                onCompletionCallback(true)
                            } else {
                                FirebaseServer.addData("$USERS/${user.id}/$SUBSCRIBED_RAID_MEETUPS", raidMeetupId, "")
                                onCompletionCallback(false)
                            }
                        }
                    } else {
                        onCompletionCallback(false)
                    }
                }
            }

        } ?: run {
            Log.e(TAG, "remove subscription failed for raidMeetup $raidMeetupId. userData: ${FirebaseUser.userData}")
            onCompletionCallback(false)
        }
    }

    suspend fun unsubscribeFromRaidMeetup(raidMeetupId: String): Boolean {

        var successful = false

        FirebaseUser.userData?.let { user ->

            if (user.subscribedRaidMeetups?.contains(raidMeetupId) == true) {

                successful = waitForCompletion { FirebaseServer.removeData("$USERS/${user.id}/$SUBSCRIBED_RAID_MEETUPS/$raidMeetupId", it) }

                if (successful) {
                    successful = waitForCompletion {
                        FirebaseServer.unsubscribeFromTopic(raidMeetupId, it)
                    }
                }
            }
        }

        return successful
    }

    fun unsubscribeFromAllOldRaidMeetups(onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        FirebaseUser.userData?.let { user ->

            user.timestampAppLastOpened?.let { timestamp ->

                if (!TimeCalculator.isCurrentDay(timestamp as Long)) {

                    GlobalScope.launch(Dispatchers.Default) {

                        var successfullyUnsubscribedAllRaidMeetups = true

                        user.subscribedRaidMeetups?.forEach { raidMeetupId ->

                            val result = async {
                                unsubscribeFromRaidMeetup(raidMeetupId)
                            }
                            if (!result.await()) {
                                successfullyUnsubscribedAllRaidMeetups = false
                            }
                        }

                        Log.v(TAG, "unsubscribeFromAllOldRaidMeetups() successfully: $successfullyUnsubscribedAllRaidMeetups")
                        CoroutineScope(Dispatchers.Main).launch { onCompletionCallback(successfullyUnsubscribedAllRaidMeetups) }
                    }

                } else {
                    onCompletionCallback(true)
                }

            }  ?: run {
                Log.e(TAG, "unsubscribeFromAllOldRaidMeetups failed. userData.timestamp: ${user.timestampAppLastOpened}")
                onCompletionCallback(false)
            }

        } ?: run {
            Log.e(TAG, "unsubscribeFromAllOldRaidMeetups failed. userData: ${FirebaseUser.userData}")
            onCompletionCallback(false)
        }
    }

    /**
     * Topic subscriptions
     */

    fun subscribeToTopic(topic: String) {

        FirebaseUser.userData?.let { user ->

            if (user.notificationTopics?.contains(topic) != true) {
                FirebaseServer.subscribeToTopic(topic) { successful ->
                    Log.v(TAG, "subscribeToTopic($topic), successful: $successful")
                    if (successful) {
                        FirebaseServer.addData(
                            "$USERS/${user.id}/${DatabaseKeys.USER_TOPICS}",
                            topic,
                            ""
                        )
                    }
                }
            } else {
                Log.w(TAG, "could not register for push notification '$topic', because already added. user.notificationTopics: ${user.notificationTopics}")
            }

        } ?: run {
            Log.e(TAG, "could not register for push notification '$topic', because user: ${FirebaseUser.userData}")
        }
    }

    fun unsubscribeFromTopic(topic: String) {

        FirebaseUser.userData?.id?.let { userId ->
            FirebaseServer.unsubscribeFromTopic(topic) { successful ->
                Log.v(TAG, "unsubscribeFromTopic($topic), successful: $successful")
                if (successful) {
                    FirebaseServer.removeData("$USERS/$userId/${DatabaseKeys.USER_TOPICS}/$topic")
                }
            }
        } ?: run {
            Log.e(TAG, "could not deregister from push notification '$topic', because user: ${FirebaseUser.userData?.id}")
        }
    }
}