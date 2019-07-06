package io.stanc.pogoradar.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import io.stanc.pogoradar.App
import io.stanc.pogoradar.R
import io.stanc.pogoradar.firebase.DatabaseKeys.NOTIFICATION_ACTIVE
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBMITTED_ARENAS
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBMITTED_POKESTOPS
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBMITTED_QUESTS
import io.stanc.pogoradar.firebase.DatabaseKeys.SUBMITTED_RAIDS
import io.stanc.pogoradar.firebase.DatabaseKeys.SubscriptionType
import io.stanc.pogoradar.firebase.DatabaseKeys.USERS
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_CODE
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_LEVEL
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_NAME
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_PUBLIC_DATA
import io.stanc.pogoradar.firebase.DatabaseKeys.USER_TEAM
import io.stanc.pogoradar.firebase.DatabaseKeys.firebaseGeoHash
import io.stanc.pogoradar.firebase.node.FirebaseUserNode
import io.stanc.pogoradar.firebase.node.Team
import io.stanc.pogoradar.geohash.GeoHash
import io.stanc.pogoradar.utils.ObserverManager
import io.stanc.pogoradar.utils.WaitingSpinner


object FirebaseUser {
    private val TAG = javaClass.name

    const val MIN_PASSWORD_LENGTH = 6

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val authStateObserverManager = ObserverManager<AuthStateObserver>()
    private val userDataObserverManager = ObserverManager<UserDataObserver>()

    var userData: FirebaseUserNode? = null
        private set(value) {
            field = value
            Log.i(TAG, "user data changed: $value")
            userDataObserverManager.observers().filterNotNull().forEach { it.userDataChanged(userData) }
        }

    private val authStateObserver = object : AuthStateObserver {
        override fun authStateChanged(newAuthState: AuthState) {
            if (newAuthState == FirebaseUser.AuthState.UserLoggedIn) {
                requestNotificationToken()
            }
        }
    }

    init {
        addAuthStateObserver(authStateObserver)
    }

    /**
     * user
     */

    fun changePushNotifications(isPushAktive: Boolean, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        userData?.let { userNode ->

            FirebaseServer.setData("$USERS/${userNode.id}/$NOTIFICATION_ACTIVE", isPushAktive, object: FirebaseServer.OnCompleteCallback<Void> {
                override fun onSuccess(data: Void?) {
                    onCompletionCallback(true)
                }

                override fun onFailed(message: String?) {
                    Log.w(TAG, "User profile update failed. Error: $message")
                    onCompletionCallback(false)
                }
            })
        } ?: run { Log.e(TAG, "cannot change push notifications: $isPushAktive, because: userData: $userData") }
    }

    fun saveSubmittedArena(arenaId: String, geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        saveSubmittedMapItem(arenaId, geoHash, SUBMITTED_ARENAS, onCompletionCallback)
    }

    fun saveSubmittedPokestop(pokestopId: String, geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        saveSubmittedMapItem(pokestopId, geoHash, SUBMITTED_POKESTOPS, onCompletionCallback)
    }

    fun saveSubmittedRaids(onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        userData?.submittedRaids?.let { numSubmittedRaids ->
            saveIncreasedSubmission(numSubmittedRaids.toInt()+1, SUBMITTED_RAIDS, onCompletionCallback)
        } ?: run {
            onCompletionCallback(false)
        }
    }

    fun saveSubmittedQuests(onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        userData?.submittedQuests?.let { numSubmittedQuests ->
            saveIncreasedSubmission(numSubmittedQuests.toInt()+1, SUBMITTED_QUESTS, onCompletionCallback)
        } ?: run {
            onCompletionCallback(false)
        }
    }

    private fun saveIncreasedSubmission(value: Number, submissionFirebaseKey: String, onCompletionCallback: (taskSuccessful: Boolean) -> Unit) {

        userData?.let { userNode ->

            FirebaseServer.setData("$USERS/${userNode.id}/$submissionFirebaseKey", value, object: FirebaseServer.OnCompleteCallback<Void> {
                override fun onSuccess(data: Void?) {
                    onCompletionCallback(true)
                }

                override fun onFailed(message: String?) {
                    Log.w(TAG, "sending $submissionFirebaseKey of $value failed. Error: $message")
                    onCompletionCallback(false)
                }
            })

        } ?: run {
            Log.e(TAG, "cannot send $submissionFirebaseKey of $value, because: userData: $userData")
        }
    }

    private fun saveSubmittedMapItem(id: String, geoHash: GeoHash, submissionFirebaseKey: String, onCompletionCallback: (taskSuccessful: Boolean) -> Unit) {

        userData?.let { userNode ->

            FirebaseServer.addData("$USERS/${userNode.id}/$submissionFirebaseKey", id, DatabaseKeys.firebaseGeoHash(geoHash), object: FirebaseServer.OnCompleteCallback<Void> {
                override fun onSuccess(data: Void?) {
                    onCompletionCallback(true)
                }

                override fun onFailed(message: String?) {
                    Log.w(TAG, "sending $submissionFirebaseKey of $id failed. Error: $message")
                    onCompletionCallback(false)
                }
            })

        } ?: run { Log.e(TAG, "cannot send $submissionFirebaseKey of $id, because: userData: $userData") }
    }

    fun requestNotificationToken() {

        FirebaseServer.requestNotificationToken(object: FirebaseServer.OnCompleteCallback<String> {

            override fun onSuccess(data: String?) {
                data?.let { token ->
                    updateNotificationToken(token)
                } ?: run {
                    Log.w(TAG, "request notification token failed. Error: token data: $data")
                }
            }

            override fun onFailed(message: String?) {
                Log.w(TAG, "request notification token failed. Error: $message")
            }
        })
    }

    private fun createUser(userLoginConfig: UserLoginConfig) {

        auth.currentUser?.let { firebaseUser ->

            val userData = FirebaseUserNode.new (
                firebaseUser.uid,
                userLoginConfig.email,
                userLoginConfig.name,
                userLoginConfig.team,
                userLoginConfig.level
            )

            updateServerData(userData)

        } ?: run {
            Log.e(TAG, "could not create user for config: $userLoginConfig, because auth.currentUser: ${auth.currentUser}")
            userData = null
        }
    }

    data class UserProfileConfig(val name: String,
                                 val code: String,
                                 val team: Number,
                                 val level: Number)

    fun updateUserProfile(userProfileConfig: UserProfileConfig) {

        auth.currentUser?.let { firebaseUser ->

            FirebaseServer.setData("$USERS/${firebaseUser.uid}/$USER_PUBLIC_DATA/$USER_NAME", userProfileConfig.name)
            FirebaseServer.setData("$USERS/${firebaseUser.uid}/$USER_PUBLIC_DATA/$USER_CODE", userProfileConfig.code)
            FirebaseServer.setData("$USERS/${firebaseUser.uid}/$USER_PUBLIC_DATA/$USER_TEAM", userProfileConfig.team)
            FirebaseServer.setData("$USERS/${firebaseUser.uid}/$USER_PUBLIC_DATA/$USER_LEVEL", userProfileConfig.level)
        }
    }



    fun updateUserSubscription(type: SubscriptionType, geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        auth.currentUser?.let { firebaseUser ->
            FirebaseServer.addData("$USERS/${firebaseUser.uid}/${type.userDataKey}", firebaseGeoHash(geoHash), "", object : FirebaseServer.OnCompleteCallback<Void> {

                override fun onSuccess(data: Void?) {
                    Log.i(TAG, "successfully subscribed ${type.name} for geoHash: $geoHash")
                    onCompletionCallback(true)
                }

                override fun onFailed(message: String?) {
                    Log.w(TAG, "failed to subscribe for ${type.name}! Error: $message")
                    onCompletionCallback(false)
                }
            })
        }
    }

    fun removeUserSubscription(type: SubscriptionType, geoHash: GeoHash, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        auth.currentUser?.let { firebaseUser ->
            FirebaseServer.removeData("$USERS/${firebaseUser.uid}/${type.userDataKey}/${firebaseGeoHash(geoHash)}", object : FirebaseServer.OnCompleteCallback<Void> {

                override fun onSuccess(data: Void?) {
                    Log.i(TAG, "successfully removed subscription ${type.name} for geoHash: $geoHash")
                    onCompletionCallback(true)
                }

                override fun onFailed(message: String?) {
                    Log.w(TAG, "failed to remove subscription for ${type.name}! Error: $message")
                    onCompletionCallback(false)
                }
            })
        }
    }

    fun updateNotificationToken(token: String) {
        userData?.copy(notificationToken = token)?.let { userDataCopy ->
            updateServerData(userDataCopy)
        }
    }

    private fun updateServerData(localUserData: FirebaseUserNode) {

        FirebaseServer.updateNode(localUserData, object : FirebaseServer.OnCompleteCallback<Void> {

            override fun onSuccess(data: Void?) {
                Log.v(TAG, "successfully updated user...")
            }

            override fun onFailed(message: String?) {
                Log.w(TAG, "failed to update user! Error: $message")
            }
        })
    }


    /**
     * authentication
     */

    enum class AuthState {
        UserLoggedOut,
        UserLoggedInButUnverified,
        UserLoggedIn
    }

    fun authState(): AuthState {

        auth.currentUser?.isEmailVerified?.let { isEmailVerified ->

            return if (isEmailVerified) {
                AuthState.UserLoggedIn
            } else {
                AuthState.UserLoggedInButUnverified
            }

        } ?: run {
            return AuthState.UserLoggedOut
        }
    }

    fun authStateText(context: Context): String {
        return when (authState()) {
            AuthState.UserLoggedIn -> context.getString(R.string.authentication_state_signed_in)
            AuthState.UserLoggedInButUnverified -> context.getString(R.string.authentication_state_signed_in_but_missing_verification)
            AuthState.UserLoggedOut -> context.getString(R.string.authentication_state_signed_out)
        }
    }

    fun startAuthentication() {
        auth.addAuthStateListener { onAuthStateChanged() }
        startListenForUserDataChanges()
        auth.currentUser?.reload()
    }

    fun stopAuthentication() {
        stopListenForUserDataChanges()
        auth.removeAuthStateListener { onAuthStateChanged() }
    }

    /**
     * login
     */

    data class UserLoginConfig(val email: String,
                               val password: String,
                               val name: String,
                               val team: Team,
                               val level: Int)

    fun signUp(userLoginConfig: UserLoginConfig, onCompletionCallback: (taskSuccessful: Boolean, exception: String?) -> Unit = { _, _ ->}) {

        if (userLoginConfig.password.length < MIN_PASSWORD_LENGTH) {
            onCompletionCallback(false, App.geString(R.string.exceptions_signup_password))
            return
        }

        if (!isEmailValid(userLoginConfig.email)) {
            onCompletionCallback(false, App.geString(R.string.exceptions_signup_email))
            return
        }

        if (userLoginConfig.name.isEmpty()) {
            onCompletionCallback(false, App.geString(R.string.exceptions_signup_name))
            return
        }

        WaitingSpinner.showProgress(R.string.spinner_title_sign_up)

        auth.createUserWithEmailAndPassword(userLoginConfig.email, userLoginConfig.password).addOnCompleteListener { task ->

            if (task.isSuccessful) {
                createUser(userLoginConfig)
            } else {
                Log.w(TAG, "signing up failed with error: ${task.exception?.message}")
            }

            onCompletionCallback(task.isSuccessful, task.exception?.message)
            WaitingSpinner.hideProgress()
        }
    }

    fun signIn(email: String, password: String, onCompletionCallback: (taskSuccessful: Boolean, exception: String?) -> Unit = { _, _ ->}) {

        if (email.isEmpty() || password.isEmpty()) {
            onCompletionCallback(false, App.geString(R.string.exceptions_sigin_email_password))
            return
        }

        WaitingSpinner.showProgress(R.string.spinner_title_sign_in)

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->

            if (!task.isSuccessful) {
                Log.w(TAG, "signing in failed with error: ${task.exception?.message}")
            }

            onCompletionCallback(task.isSuccessful, task.exception?.message)
            WaitingSpinner.hideProgress()
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun sendEmailVerification(onCompletionCallback: (taskSuccessful: Boolean, exception: String?) -> Unit = { _, _ ->}) {

        WaitingSpinner.showProgress(R.string.spinner_title_email_verification)

        auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { task ->

            if (task.isSuccessful) {
                signOut()
            } else {
                Log.w(TAG, "email verification failed with error: ${task.exception?.message}")
            }

            WaitingSpinner.hideProgress()
            onCompletionCallback(task.isSuccessful, task.exception?.message)
        }
    }

    // TODO...
    fun deleteUser() {
        // userData?.delete()
//        auth.currentUser?.delete()
    }

    /**
     * Authentication State Listener
     */

    interface AuthStateObserver {
        fun authStateChanged(newAuthState: AuthState)
    }

    fun addAuthStateObserver(observer: AuthStateObserver) {
        authStateObserverManager.addObserver(observer)
        observer.authStateChanged(authState())
    }

    fun removeAuthStateObserver(observer: AuthStateObserver) {
        authStateObserverManager.removeObserver(observer)
    }

    private val onAuthStateChanged: () -> Unit = {
        Log.i(TAG, "onAuthStateChanged(${authState().name}) for user: $userData")
        authStateObserverManager.observers().filterNotNull().forEach { it.authStateChanged(authState()) }
        updateUserDataDependingOnAuthState()
    }

    private fun updateUserDataDependingOnAuthState() {
        when (authState()) {
            AuthState.UserLoggedOut -> {
                stopListenForUserDataChanges()
                userData = null
            }
            AuthState.UserLoggedInButUnverified,
            AuthState.UserLoggedIn -> {
                startListenForUserDataChanges()
            }
        }
    }

    /**
     * User Data Listener
     */

    interface UserDataObserver {
        fun userDataChanged(user: FirebaseUserNode?)
    }

    fun addUserDataObserver(observer: UserDataObserver) {
        userDataObserverManager.addObserver(observer)
        observer.userDataChanged(userData)
    }

    fun removeUserDataObserver(observer: UserDataObserver) {
        userDataObserverManager.removeObserver(observer)
    }

    private fun startListenForUserDataChanges() {
        auth.currentUser?.uid?.let { uid ->
            FirebaseServer.removeNodeEventListener("$USERS/$uid", userNodeDidChangeCallback)
            FirebaseServer.addNodeEventListener("$USERS/$uid", userNodeDidChangeCallback)
        }
    }

    private fun stopListenForUserDataChanges() {
        auth.currentUser?.uid?.let { uid ->
            FirebaseServer.removeNodeEventListener("$USERS/$uid", userNodeDidChangeCallback)
        }
    }

    private val userNodeDidChangeCallback = object: FirebaseServer.OnNodeDidChangeCallback {
        override fun nodeChanged(dataSnapshot: DataSnapshot) {
            FirebaseUserNode.new(dataSnapshot)?.let {
                userData = it
            }
        }
        override fun nodeRemoved(key: String) {
            Log.w(TAG, "user (key: $key) removed!")
            userData = null
        }
    }

    /**
     * private
     */

    fun isEmailValid(email: CharSequence): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}