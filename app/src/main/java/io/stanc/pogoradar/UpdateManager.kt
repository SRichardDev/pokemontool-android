package io.stanc.pogoradar

import io.stanc.pogoradar.geohash.GeoHash
import android.content.pm.PackageManager
import android.content.Context
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.DatabaseKeys.USERS
import io.stanc.pogoradar.firebase.FirebaseDefinitions.raidBosses
import io.stanc.pogoradar.firebase.FirebaseNotification
import io.stanc.pogoradar.firebase.FirebaseServer
import io.stanc.pogoradar.firebase.FirebaseUser
import io.stanc.pogoradar.firebase.node.FirebaseUserNode


object UpdateManager {
    private val TAG = javaClass.name

    private const val VERSION_CHANGE_105 = "VERSION_CHANGE_105"
    private var versionChange105Updated = false
        set(value) {
            field = value
            App.preferences?.edit()?.putBoolean(VERSION_CHANGE_105, value)?.apply()
        }

    private val authStateObserver = object : FirebaseUser.AuthStateObserver {
        override fun authStateChanged(newAuthState: FirebaseUser.AuthState) {
            tryToUpdateDatabaseDependingOnAppVersion()
        }
    }

    private val userDataObserver = object : FirebaseUser.UserDataObserver {
        override fun userDataChanged(user: FirebaseUserNode?) {
            tryToUpdateDatabaseDependingOnAppVersion()
        }
    }

    init {
        App.preferences?.let { preferences ->
            versionChange105Updated = preferences.getBoolean(VERSION_CHANGE_105, false)
        }
    }

    fun start() {
        FirebaseUser.addAuthStateObserver(authStateObserver)
        FirebaseUser.addUserDataObserver(userDataObserver)
    }

    fun stop() {
        FirebaseUser.removeAuthStateObserver(authStateObserver)
        FirebaseUser.removeUserDataObserver(userDataObserver)
    }

    private fun tryToUpdateDatabaseDependingOnAppVersion() {
        if (FirebaseUser.authState() == FirebaseUser.AuthState.UserLoggedIn && FirebaseUser.userData != null) {
            updateDatabaseDependingOnAppVersion()
        }
    }

    fun updateDatabaseDependingOnAppVersion() {

        App.versionCode?.let { versionCode ->

            changesForVersion105AndAbove(versionCode)

        } ?: run {
            Log.w(TAG, "tried to updateDatabaseDependingOnAppVersion, but App.versionCode: ${App.versionCode}")
        }
    }
    
    private fun changesForVersion105AndAbove(currentVersionCode: Long) {

        if (currentVersionCode >= 105 && !versionChange105Updated) {

            FirebaseUser.userData?.let { user ->

                // deprecated nodes in user:
                val SUBSCRIBED_GEOHASH_POKESTOPS = "subscribedGeohashPokestops"
                val SUBSCRIBED_GEOHASH_ARENAS = "subscribedGeohashArenas"
                val NOTIFICATION_ACTIVE = "isPushActive"

                FirebaseServer.requestDataChilds("$USERS/${user.id}/$SUBSCRIBED_GEOHASH_ARENAS", object : FirebaseServer.OnCompleteCallback<List<DataSnapshot>> {

                    override fun onSuccess(data: List<DataSnapshot>?) {

                        // 1. load deprecated "subscribedGeohashArenas" and? "subscribedGeohashPokestops" and push to "subscribedGeohashes"
                        val geoHashes = data?.mapNotNull { it.key?.let { GeoHash(it) } }?.toList()
                        geoHashes?.forEach { FirebaseNotification.subscribeToArea(it) }

                        // 2. delete deprecated paths "subscribedGeohashArenas" and "subscribedGeohashPokestops"
                        FirebaseServer.removeNode("$USERS/${user.id}", SUBSCRIBED_GEOHASH_ARENAS)
                        FirebaseServer.removeNode("$USERS/${user.id}", SUBSCRIBED_GEOHASH_POKESTOPS)

                        // 3. delete deprecated value "isPushActive"
                        FirebaseServer.removeNode("$USERS/${user.id}", NOTIFICATION_ACTIVE)

                        versionChange105Updated = true
                    }

                    override fun onFailed(message: String?) {
                        Log.e(TAG, "version update change 105 failed. Error: $message")
                        versionChange105Updated = false
                    }
                })

            } ?: run {
                Log.e(TAG, "version update change 105 failed. User is not logged in.")
                versionChange105Updated = false
            }
        }
    }

    // Sending Condition: 'raids' in topics && 'u281xg' in topics && 'level-1' in topics

//    fun loadTopics(block : @escaping (_ topics: [Messaging.Topic]?, _ error: Error?) -> Void ) {
//
//
//
//    }
//        if let token = InstanceID.instanceID().token() {
//            let url = URL(string: "https://iid.googleapis.com/iid/info/\(token)?details=true")!
//            var request = URLRequest(url: url)
//            request.addValue("key=\(Messaging.accessToken)", forHTTPHeaderField: "Authorization")
//            let dataTask = URLSession.shared.dataTask(with: request) { (data, response, error) in
//            DataManager.shared.make(request: urlRequest, block: { (data, error) in
//                if let data = data {
//                    let decoder = JSONDecoder()
//                    let rel = try? decoder.decode(Rel.self, from: data)
//                    block(rel?.topics, error)
//                } else {
//                    block(nil, error)
//                }
//            }
//            dataTask.resume()
//        }
//        }
}