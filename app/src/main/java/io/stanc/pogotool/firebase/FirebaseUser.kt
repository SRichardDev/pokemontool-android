package io.stanc.pogotool.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.FirebaseDatabase.Companion.DATABASE_USERS
import io.stanc.pogotool.firebase.data.UsernameData
import io.stanc.pogotool.firebase.node.FirebaseUserNode
import io.stanc.pogotool.utils.ObserverManager
import io.stanc.pogotool.utils.WaitingSpinner
import java.lang.ref.WeakReference

object FirebaseUser {
    private val TAG = javaClass.name

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

    fun changeUserName(newUserName: String, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        userData?.let { userNode ->

            FirebaseServer.setData(UsernameData(userNode.id, newUserName), object: FirebaseServer.OnCompleteCallback<Void> {
                override fun onSuccess(data: Void?) {
                    onCompletionCallback(true)
                }

                override fun onFailed(message: String?) {
                    Log.w(TAG, "User profile update failed. Error: $message")
                    onCompletionCallback(false)
                }
            })
        } ?: kotlin.run { Log.e(TAG, "cannot change username: $newUserName, because: userData: $userData") }
    }

    private fun requestNotificationTokenToCreateUser(userConfig: UserConfig) {

        FirebaseServer.requestNotificationToken(object: FirebaseServer.OnCompleteCallback<String> {

            override fun onSuccess(data: String?) {
                data?.let { token ->
                    createUser(userConfig, token)
                } ?: kotlin.run { Log.w(TAG, "User profile creation failed. Error: token is $data") }
            }

            override fun onFailed(message: String?) {
                Log.w(TAG, "User profile creation failed. Error: $message")
            }
        })
    }

    private fun createUser(userConfig: UserConfig, userNotificationToken: String) {

        auth.currentUser?.let { firebaseUser ->

            val userData = FirebaseUserNode(
                firebaseUser.uid,
                userConfig.name,
                userConfig.email,
                userConfig.team,
                userNotificationToken
            )

            FirebaseServer.createNode(userData, object : FirebaseServer.OnCompleteCallback<Void> {

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
            Log.e(TAG, "could not create user for config: $userConfig and notificationToken: $userNotificationToken, because auth.currentUser: ${auth.currentUser}")
            FirebaseUser.userData = null
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

    data class UserConfig(val email: String,
                          val password: String,
                          val name: String = "<not defined>",
                          val team: Int = -1)

    fun signUp(userConfig: UserConfig, onCompletionCallback: (taskSuccessful: Boolean, exception: String?) -> Unit = { _, _ ->}) {

        // TODO: verify user config data (valid email, secure password, valid name and valid team value)
        if (userConfig.email.isEmpty() || userConfig.password.isEmpty() || userConfig.name.isEmpty()) {
            return
        }

        WaitingSpinner.showProgress(R.string.spinner_title_sign_up)

        auth.createUserWithEmailAndPassword(userConfig.email, userConfig.password).addOnCompleteListener { task ->

            if (task.isSuccessful) {
                requestNotificationTokenToCreateUser(userConfig)
            } else {
                Log.w(TAG, "signing up failed with error: ${task.exception?.message}")
            }

            onCompletionCallback(task.isSuccessful, task.exception?.message)
            WaitingSpinner.hideProgress()
        }
    }

    fun signIn(userConfig: UserConfig, onCompletionCallback: (taskSuccessful: Boolean, exception: String?) -> Unit = { _, _ ->}) {

        if (userConfig.email.isEmpty() || userConfig.password.isEmpty()) {
            return
        }

        WaitingSpinner.showProgress(R.string.spinner_title_sign_in)

        auth.signInWithEmailAndPassword(userConfig.email, userConfig.password).addOnCompleteListener { task ->

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
        observer.userDataChanged(userData)
    }

    fun removeUserDataObserver(observer: UserDataObserver) {
        userDataObserverManager.removeObserver(observer)
    }

    private fun startListenForUserDataChanges() {
        auth.currentUser?.uid?.let { uid ->
            FirebaseServer.addNodeEventListener("$DATABASE_USERS/$uid", userNodeDidChangeCallback)
        }
    }

    private fun stopListenForUserDataChanges() {
        auth.currentUser?.uid?.let { uid ->
            FirebaseServer.removeNodeEventListener("$DATABASE_USERS/$uid", userNodeDidChangeCallback)
        }
    }

    private val userNodeDidChangeCallback = object: FirebaseServer.OnNodeDidChangeCallback {
        override fun nodeChanged(dataSnapshot: DataSnapshot) {
            FirebaseUserNode.new(dataSnapshot)?.let { FirebaseUser.userData = it }
        }
    }
}