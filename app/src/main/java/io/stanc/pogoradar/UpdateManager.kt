package io.stanc.pogoradar

import android.util.Log
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.geohash.GeoHash

class UpdateManager {

    fun updateUsersDatabase() {

        // 1. try to load "subscribedGeohashArenas" & "subscribedGeohashPokestops" and push to "subscribedGeohashes"
        // 2. after that delete "subscribedGeohashArenas" & "subscribedGeohashPokestops"
        // 3. delete "isPushActive"
//        loadDeprecatedSubscriptions()
    }

    fun loadDeprecatedSubscriptions(onCompletionCallback: (geoHashes: List<GeoHash>?) -> Unit) {

//        FirebaseUser.userData?.let { user ->
//
//            notifyCompletionCallback(user.subscribedGeohashes, user.subscribedGeohashArenas, onCompletionCallback)
//
//        } ?: run {
//            Log.e(TAG, "geohash subscription loading failed. FirebaseUser.userData: ${FirebaseUser.userData}")
//            onCompletionCallback(null)
//        }
    }
}