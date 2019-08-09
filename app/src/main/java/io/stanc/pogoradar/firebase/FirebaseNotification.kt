package io.stanc.pogoradar.firebase

import android.util.Log
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBSCRIBED_GEOHASHES
import io.stanc.pogoradar.firebase.DatabaseKeys.USERS
import io.stanc.pogoradar.firebase.DatabaseKeys.firebaseGeoHash
import io.stanc.pogoradar.geohash.GeoHash


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

    fun subscribeToArea(geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        FirebaseUser.userData?.let { user ->

            if (user.subscribedGeohashes?.contains(geoHash) == false) {

                val formattedGeoHash = firebaseGeoHash(geoHash)

                FirebaseServer.addData("$USERS/${user.id}/$SUBSCRIBED_GEOHASHES", formattedGeoHash, "", object : FirebaseServer.OnCompleteCallback<Void> {

                    override fun onSuccess(data: Void?) {
                        FirebaseServer.subscribeToTopic(formattedGeoHash, object: FirebaseServer.OnCompleteCallback<Void> {

                            override fun onSuccess(data: Void?) {
                                onCompletionCallback(true)
                            }

                            override fun onFailed(message: String?) {
                                Log.e(TAG, "subscription failed for area $geoHash and user: ${user.id}. Message: [$message]")
                                FirebaseServer.removeData("$USERS/${user.id}/$SUBSCRIBED_GEOHASHES/$formattedGeoHash")
                                onCompletionCallback(false)
                            }

                        })
                    }

                    override fun onFailed(message: String?) {
                        Log.e(TAG, "subscription failed for area $geoHash and user: ${user.id}. Message: [$message]")
                        onCompletionCallback(false)
                    }
                })
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

                FirebaseServer.removeData("$USERS/${user.id}/$SUBSCRIBED_GEOHASHES/$formattedGeoHash", object : FirebaseServer.OnCompleteCallback<Void> {

                    override fun onSuccess(data: Void?) {
                        FirebaseServer.unsubscribeFromTopic(formattedGeoHash, object: FirebaseServer.OnCompleteCallback<Void> {

                            override fun onSuccess(data: Void?) {
                                onCompletionCallback(true)
                            }

                            override fun onFailed(message: String?) {
                                Log.e(TAG, "remove subscription failed for area $geoHash and user: ${user.id}. Message: [$message]")
                                FirebaseServer.addData("$USERS/${user.id}/$SUBSCRIBED_GEOHASHES", formattedGeoHash, "")
                                onCompletionCallback(false)
                            }

                        })
                    }

                    override fun onFailed(message: String?) {
                        Log.e(TAG, "remove subscription failed for area $geoHash and user: ${user.id}. Message: [$message]")
                        onCompletionCallback(false)
                    }
                })
            }

        } ?: run {
            Log.e(TAG, "remove subscription failed for area $geoHash. userData: ${FirebaseUser.userData}")
            onCompletionCallback(false)
        }
    }

    fun subscribeToRaidMeetup(raidMeetupId: String) {

    }

    fun unsubscribeFromRaidMeetup(raidMeetupId: String) {

    }

    fun changeActivation(isPushAktive: Boolean) {

        FirebaseUser.userData?.id?.let { userId ->

            if (isPushAktive) {

                registerForPushNotifications(DatabaseKeys.NOTIFICATION_TOPIC_PLATFORM)
                registerForPushNotifications(DatabaseKeys.NOTIFICATION_TOPIC_RAIDS)
                registerForPushNotifications(DatabaseKeys.NOTIFICATION_TOPIC_QUESTS)
                registerForPushNotifications(DatabaseKeys.NOTIFICATION_TOPIC_INCIDENTS)
                registerForPushNotifications("${DatabaseKeys.NOTIFICATION_TOPIC_LEVEL}1")
                registerForPushNotifications("${DatabaseKeys.NOTIFICATION_TOPIC_LEVEL}2")
                registerForPushNotifications("${DatabaseKeys.NOTIFICATION_TOPIC_LEVEL}3")
                registerForPushNotifications("${DatabaseKeys.NOTIFICATION_TOPIC_LEVEL}4")
                registerForPushNotifications("${DatabaseKeys.NOTIFICATION_TOPIC_LEVEL}5")

            } else {

                deregisterFromPushNotifications(DatabaseKeys.NOTIFICATION_TOPIC_PLATFORM)
                deregisterFromPushNotifications(DatabaseKeys.NOTIFICATION_TOPIC_RAIDS)
                deregisterFromPushNotifications(DatabaseKeys.NOTIFICATION_TOPIC_QUESTS)
                deregisterFromPushNotifications(DatabaseKeys.NOTIFICATION_TOPIC_INCIDENTS)
                deregisterFromPushNotifications("${DatabaseKeys.NOTIFICATION_TOPIC_LEVEL}1")
                deregisterFromPushNotifications("${DatabaseKeys.NOTIFICATION_TOPIC_LEVEL}2")
                deregisterFromPushNotifications("${DatabaseKeys.NOTIFICATION_TOPIC_LEVEL}3")
                deregisterFromPushNotifications("${DatabaseKeys.NOTIFICATION_TOPIC_LEVEL}4")
                deregisterFromPushNotifications("${DatabaseKeys.NOTIFICATION_TOPIC_LEVEL}5")
            }

        } ?: run {
            Log.e(TAG, "could not change push notification (isPushAktive: $isPushAktive), because user: ${FirebaseUser.userData?.id}")
        }
    }

    private fun registerForPushNotifications(topic: String) {

        FirebaseUser.userData?.let { user ->

            user.notificationTopics?.let { topics ->
                if (!topics.contains(topic)) {
                    FirebaseServer.subscribeToTopic(topic)
                    FirebaseServer.addData("${DatabaseKeys.USERS}/${user.id}/${DatabaseKeys.USER_TOPICS}", topic, "")
                }
            }
        }
    }

    private fun deregisterFromPushNotifications(topic: String) {

        FirebaseServer.unsubscribeFromTopic(topic)
        FirebaseUser.userData?.id?.let { userId ->
            FirebaseServer.removeData("${DatabaseKeys.USERS}/$userId/${DatabaseKeys.USER_TOPICS}/$topic")
        }
    }
}