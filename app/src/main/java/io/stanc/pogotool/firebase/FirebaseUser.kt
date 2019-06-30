package io.stanc.pogotool.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.App
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.DatabaseKeys.NOTIFICATION_ACTIVE
import io.stanc.pogotool.firebase.DatabaseKeys.SUBMITTED_ARENAS
import io.stanc.pogotool.firebase.DatabaseKeys.SUBMITTED_POKESTOPS
import io.stanc.pogotool.firebase.DatabaseKeys.SUBMITTED_QUESTS
import io.stanc.pogotool.firebase.DatabaseKeys.SUBMITTED_RAIDS
import io.stanc.pogotool.firebase.DatabaseKeys.USERS
import io.stanc.pogotool.firebase.DatabaseKeys.USER_CODE
import io.stanc.pogotool.firebase.DatabaseKeys.USER_LEVEL
import io.stanc.pogotool.firebase.DatabaseKeys.USER_NAME
import io.stanc.pogotool.firebase.DatabaseKeys.USER_PUBLIC_DATA
import io.stanc.pogotool.firebase.DatabaseKeys.USER_TEAM
import io.stanc.pogotool.firebase.node.FirebaseUserNode
import io.stanc.pogotool.firebase.node.Team
import io.stanc.pogotool.geohash.GeoHash
import io.stanc.pogotool.utils.ObserverManager
import io.stanc.pogotool.utils.WaitingSpinner


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
        } ?: kotlin.run { Log.e(TAG, "cannot change push notifications: $isPushAktive, because: userData: $userData") }
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
        } ?: kotlin.run {
            onCompletionCallback(false)
        }
    }

    fun saveSubmittedQuests(onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
        userData?.submittedQuests?.let { numSubmittedQuests ->
            saveIncreasedSubmission(numSubmittedQuests.toInt()+1, SUBMITTED_QUESTS, onCompletionCallback)
        } ?: kotlin.run {
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

        } ?: kotlin.run {
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

        } ?: kotlin.run { Log.e(TAG, "cannot send $submissionFirebaseKey of $id, because: userData: $userData") }
    }

    private fun requestNotificationTokenToCreateUser(userLoginConfig: UserLoginConfig) {

        FirebaseServer.requestNotificationToken(object: FirebaseServer.OnCompleteCallback<String> {

            override fun onSuccess(data: String?) {
                data?.let { token ->
                    createUser(userLoginConfig, token)
                } ?: kotlin.run { Log.w(TAG, "User profile creation failed. Error: token is $data") }
            }

            override fun onFailed(message: String?) {
                Log.w(TAG, "User profile creation failed. Error: $message")
            }
        })
    }

    private fun createUser(userLoginConfig: UserLoginConfig, userNotificationToken: String) {

        auth.currentUser?.let { firebaseUser ->

            val userData = FirebaseUserNode.new (
                firebaseUser.uid,
                userLoginConfig.email,
                userLoginConfig.name,
                userLoginConfig.team,
                userLoginConfig.level,
                userNotificationToken
            )

            FirebaseServer.updateNode(userData, object : FirebaseServer.OnCompleteCallback<Void> {

                override fun onSuccess(data: Void?) {
                    Log.i(TAG, "successfully created user: [$userData]")
                    FirebaseUser.userData = userData
                }

                override fun onFailed(message: String?) {
                    Log.w(TAG, "failed to create user! Error: $message")
                    FirebaseUser.userData = null
                }
            })

        } ?: kotlin.run {
            Log.e(TAG, "could not create user for config: $userLoginConfig and notificationToken: $userNotificationToken, because auth.currentUser: ${auth.currentUser}")
            FirebaseUser.userData = null
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

        } ?: kotlin.run {
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
                requestNotificationTokenToCreateUser(userLoginConfig)
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
                FirebaseUser.userData = null
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
        Log.i(TAG, "Debug:: addUserDataObserver() userData: $userData")
        observer.userDataChanged(userData)
    }

    fun removeUserDataObserver(observer: UserDataObserver) {
        userDataObserverManager.removeObserver(observer)
    }

    private fun startListenForUserDataChanges() {
        Log.i(TAG, "Debug:: startListenForUserDataChanges() auth.currentUser?.uid: ${auth.currentUser?.uid}, path: ${"$USERS/${auth.currentUser?.uid}"}")
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
            Log.i(TAG, "Debug:: nodeChanged(dataSnapshot: $dataSnapshot)")
            FirebaseUserNode.new(dataSnapshot)?.let {
                Log.i(TAG, "Debug:: nodeChanged(dataSnapshot: $dataSnapshot) done.")
                FirebaseUser.userData = it }
        }
        override fun nodeRemoved(key: String) {
            FirebaseUser.userData = null
        }
    }

    /**
     * private
     */

    fun isEmailValid(email: CharSequence): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}