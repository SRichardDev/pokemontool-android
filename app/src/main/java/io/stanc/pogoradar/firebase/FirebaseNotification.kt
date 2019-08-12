package io.stanc.pogoradar.firebase

import android.util.Log
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBSCRIBED_GEOHASHES
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBSCRIBED_RAID_MEETUPS
import io.stanc.pogoradar.firebase.DatabaseKeys.USERS
import io.stanc.pogoradar.firebase.DatabaseKeys.firebaseGeoHash
import io.stanc.pogoradar.geohash.GeoHash
import io.stanc.pogoradar.utils.Async.waitForCompletion
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

    fun loadAreaSubscriptions(onCompletionCallback: (geoHashes: List<GeoHash>?) -> Unit) {

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

                Log.v(TAG, "Debug:: subscribeToArea($geoHash)")
                FirebaseServer.addData("$USERS/${user.id}/$SUBSCRIBED_GEOHASHES", formattedGeoHash, "") { successful ->
                    Log.v(TAG, "Debug:: subscribeToArea($geoHash), addData: $successful")
                    if (successful) {

                        FirebaseServer.subscribeToTopic(formattedGeoHash) { successful ->
                            Log.v(TAG, "Debug:: subscribeToArea($geoHash), subscribeToTopic: $successful")

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

                Log.v(TAG, "Debug:: unsubscribeFromArea($geoHash)")
                FirebaseServer.removeData("$USERS/${user.id}/$SUBSCRIBED_GEOHASHES/$formattedGeoHash") { successful ->
                    Log.v(TAG, "Debug:: unsubscribeFromArea($geoHash), removeData: $successful")
                    if (successful) {

                        FirebaseServer.unsubscribeFromTopic(formattedGeoHash) { successful ->
                            Log.v(TAG, "Debug:: unsubscribeFromArea($geoHash), unsubscribeFromTopic: $successful")
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

        Log.d(TAG, "Debug:: try unsubscribeFromArea($geoHash)...")

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

        Log.v(TAG, "Debug:: unsubscribeFromArea($geoHash) successful: $successful")

        return successful
    }


    fun unsubscribeFromAllAreas(onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        GlobalScope.launch(Dispatchers.Default) {

            Log.i(TAG, "Debug:: unsubscribeFromAllAreas...")

            FirebaseUser.userData?.let { user ->

                var successfullyUnsubscribedAllAreas = true

                user.subscribedGeohashes?.forEach { geoHash ->
                    val result = async { unsubscribeFromArea(geoHash) }
                    if (!result.await()) {
                        successfullyUnsubscribedAllAreas = false
                    }
                }

                Log.i(TAG, "Debug:: unsubscribeFromAllAreas() successfullyUnsubscribedAllAreas: $successfullyUnsubscribedAllAreas")
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

                FirebaseServer.removeData("$USERS/${user.id}/$SUBSCRIBED_GEOHASHES/$raidMeetupId") { successful ->

                    if (successful) {
                        FirebaseServer.unsubscribeFromTopic(raidMeetupId) { successful ->

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

    /**
     * notification settings
     */

    fun changeActivation(isPushActive: Boolean) {

        FirebaseUser.userData?.id?.let { userId ->

            if (isPushActive) {
                registerForPushNotifications(DatabaseKeys.NOTIFICATION_TOPIC_ANDROID)
            } else {
                unregisterFromPushNotifications(DatabaseKeys.NOTIFICATION_TOPIC_ANDROID)
            }

        } ?: run {
            Log.e(TAG, "could not change push notification (isPushActive: $isPushActive), because user: ${FirebaseUser.userData?.id}")
        }
    }

    private fun registerForPushNotifications(topic: String) {

        FirebaseUser.userData?.let { user ->

            if (user.notificationTopics?.contains(topic) != true) {
                Log.v(TAG, "Debug:: registerForPushNotifications($topic)")
                FirebaseServer.subscribeToTopic(topic) { successful ->
                    Log.v(TAG, "Debug:: registerForPushNotifications($topic), subscribeToTopic: $successful")
                    if (successful) {
                        FirebaseServer.addData("$USERS/${user.id}/${DatabaseKeys.USER_TOPICS}", topic, "")
                    }
                }
            } else {
                Log.w(TAG, "could not register for push notification '$topic', because already added. user.notificationTopics: ${user.notificationTopics}")
            }

        } ?: run {
            Log.e(TAG, "could not register for push notification '$topic', because user: ${FirebaseUser.userData}")
        }
    }

    private fun unregisterFromPushNotifications(topic: String) {

        FirebaseUser.userData?.id?.let { userId ->
            Log.v(TAG, "Debug:: deregisterFromPushNotifications($topic)")
            FirebaseServer.unsubscribeFromTopic(topic) { successful ->
                Log.v(TAG, "Debug:: deregisterFromPushNotifications($topic), unsubscribeFromTopic: $successful")
                if (successful) {
                    FirebaseServer.removeData("$USERS/$userId/${DatabaseKeys.USER_TOPICS}/$topic")
                }
            }
        } ?: run {
            Log.e(TAG, "could not deregister from push notification '$topic', because user: ${FirebaseUser.userData?.id}")
        }
    }
}