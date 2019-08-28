package io.stanc.pogoradar

import android.content.Context
import io.stanc.pogoradar.geohash.GeoHash
import android.util.Log
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.firebase.DatabaseKeys.USERS
import io.stanc.pogoradar.firebase.notification.FirebaseNotification
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
    private const val VERSION_INFO_105 = "VERSION_INFO_105"
    private var versionInfo105Displayed = false
        set(value) {
            field = value
            App.preferences?.edit()?.putBoolean(VERSION_INFO_105, value)?.apply()
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
            versionInfo105Displayed = preferences.getBoolean(VERSION_INFO_105, false)
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

    fun showVersionInfoIfNotAlreadyShown(context: Context) {
        if (FirebaseUser.authState() == FirebaseUser.AuthState.UserLoggedIn && FirebaseUser.userData != null && !versionInfo105Displayed) {
            Popup.showInfo(context, R.string.app_update_info_105_title, R.string.app_update_info_105_description)
            versionInfo105Displayed = true
        }
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

                // 1. push new user key and timestamp value
                FirebaseUser.updateUserTimestamp()

                // 2. delete deprecated user key "isPushActive"
                FirebaseServer.removeNode("$USERS/${user.id}", "isPushActive")

                // 3. load all geohash data, push to new topics and remove deprecated nodes in user:
                val SUBSCRIBED_GEOHASH_POKESTOPS = "subscribedGeohashPokestops"
                val SUBSCRIBED_GEOHASH_ARENAS = "subscribedGeohashArenas"

                FirebaseServer.requestDataChilds("$USERS/${user.id}/$SUBSCRIBED_GEOHASH_ARENAS", object : FirebaseServer.OnCompleteCallback<List<DataSnapshot>> {

                    override fun onSuccess(data: List<DataSnapshot>?) {

                        val geoHashes = data?.mapNotNull { it.key?.let { GeoHash(it) } }?.toList()
                        geoHashes?.forEach { FirebaseNotification.subscribeToArea(it) }

                        // deprecated nodes: "subscribedGeohashArenas" and "subscribedGeohashPokestops"
                        FirebaseServer.removeNode("$USERS/${user.id}", SUBSCRIBED_GEOHASH_ARENAS)
                        FirebaseServer.removeNode("$USERS/${user.id}", SUBSCRIBED_GEOHASH_POKESTOPS)
                    }

                    override fun onFailed(message: String?) {
                        Log.e(TAG, "version update change 105 failed. Error: $message")
                        versionChange105Updated = false
                    }
                })

                versionChange105Updated = true

            } ?: run {
                Log.e(TAG, "version update change 105 failed. User is not logged in.")
                versionChange105Updated = false
            }
        }
    }
}