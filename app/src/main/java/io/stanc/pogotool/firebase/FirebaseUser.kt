package io.stanc.pogotool.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import io.stanc.pogotool.R
import io.stanc.pogotool.firebase.data.UserNotificationData
import io.stanc.pogotool.firebase.data.UsernameData
import io.stanc.pogotool.firebase.node.FirebaseUserNode
import io.stanc.pogotool.utils.KotlinUtils
import io.stanc.pogotool.utils.WaitingSpinner
import java.lang.ref.WeakReference

object FirebaseUser {
    private val TAG = javaClass.name

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val databaseUsers = FirebaseServer.database.child(FirebaseDatabase.DATABASE_USERS)

    private val authStateObservers = HashMap<Int, WeakReference<AuthStateObserver>>()
    private val userProfileObservers = HashMap<Int, WeakReference<UserProfileObserver>>()

    var currentUser: FirebaseUserNode? = null
        private set

    /**
     * user
     */

    fun changeUserName(newUserName: String, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {

        currentUser?.let { userNode ->

            FirebaseServer.setData(UsernameData(userNode.id, newUserName), object: FirebaseServer.OnCompleteCallback<Void> {
                override fun onSuccess(data: Void?) {
                    Log.i(TAG, "User profile updated.")
                    onCompletionCallback(true)
                }

                override fun onFailed(message: String?) {
                    Log.w(TAG, "User profile update failed. Error: $message")
                    onCompletionCallback(false)
                }
            })
        }
    }

//    fun reloadUserData(context: Context, onCompletionCallback: (taskSuccessful: Boolean) -> Unit = {}) {
//
//        currentUser?.reload()?.addOnCompleteListener { task ->
//
//            if (task.isSuccessful) {
//                createUserProfile(currentUser)
//            } else {
//                Log.w(TAG, "reloading currentUser data failed with error: ${task.exception?.message}")
//                Toast.makeText(context, context.getString(R.string.authentication_state_synchronization_failed), Toast.LENGTH_LONG).show()
//            }
//
//            onCompletionCallback(task.isSuccessful)
//        }
//    }

    private fun updateUserProfile() {

        auth.currentUser?.let { firebaseUser ->
            currentUser?.let { currentUser ->

                currentUser.id = firebaseUser.uid
                currentUser.isVerified = firebaseUser.isEmailVerified
//                currentUser.photoURL = firebaseUser.photoUrl

                userProfileObservers.forEach { it.value.get()?.userProfileChanged(currentUser) }

            } ?: kotlin.run {
                Log.e(TAG, "tried to update user profile, but currentUser: $currentUser")
            }

        } ?: kotlin.run {
            Log.e(TAG, "tried to update user profile, but auth.currentUser: ${auth.currentUser}")
        }
    }

    private fun createUserProfile(userConfig: UserConfig) {

        auth.currentUser?.let { firebaseUser ->

            currentUser = FirebaseUserNode(
                firebaseUser.uid,
                userConfig.name,
                userConfig.email,
                userConfig.team,
                null,
                firebaseUser.isEmailVerified
            )

            startListenToUserProfileChanges()

            userProfileObservers.forEach { it.value.get()?.userProfileChanged(currentUser) }

        } ?: kotlin.run {
            currentUser = null
            userProfileObservers.forEach { it.value.get()?.userProfileChanged(null) }
        }
    }

    /**
     * authentication
     */

    data class UserConfig(val email: String,
                          val password: String,
                          val name: String = "<not defined>",
                          val team: Int = -1)

    fun startAuthentication() {
        auth.addAuthStateListener { onAuthStateChanged() }
        startListenToUserProfileChanges()
    }

    fun stopAuthentication() {
        stopListenToUserProfileChanges()
        auth.removeAuthStateListener { onAuthStateChanged }
    }

    fun signUp(userConfig: UserConfig, onCompletionCallback: (taskSuccessful: Boolean, exception: String?) -> Unit = { _, _ ->}) {

        // TODO: verify user config data (valid email, secure password, valid name and valid team value)
        if (userConfig.email.isEmpty() || userConfig.password.isEmpty() || userConfig.name.isEmpty()) {
            return
        }

        WaitingSpinner.showProgress(R.string.spinner_title_sign_up)

        auth.createUserWithEmailAndPassword(userConfig.email, userConfig.password).addOnCompleteListener { task ->

            if (task.isSuccessful) {
                createUserProfile(userConfig)
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

    fun sendEmailVerification(onCompletionCallback: (taskSuccessful: Boolean, exception: String?) -> Unit = {_, _ ->}) {

        // Send verification email
        currentUser?.let { user ->

            WaitingSpinner.showProgress(R.string.spinner_title_email_verification)

            auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    Log.w(TAG, "email verification failed with error: ${task.exception?.message}")
                }

                WaitingSpinner.hideProgress()
                onCompletionCallback(task.isSuccessful, task.exception?.message)
            }
        } ?: kotlin.run { Log.w(TAG, "can not send email verification because current currentUser is null!") }
    }

    private fun updateUsersNotificationToken() {

        FirebaseServer.requestNotificationToken(object: FirebaseServer.OnCompleteCallback<String> {

            override fun onSuccess(data: String?) {
                KotlinUtils.safeLet(currentUser, data) { user, data ->
                    FirebaseServer.setData(UserNotificationData(user.id, data))
                } ?: kotlin.run {
                    Log.w(TAG, "User profile update failed. Error: currentUser: $currentUser, data: $data")
                }
            }

            override fun onFailed(message: String?) {
                Log.w(TAG, "User profile update failed. Error: $message")
            }
        })
    }

    // TODO...
    fun deleteUser() {
        // currentUser?.delete()
        // createUserProfile()
    }

    /**
     * authentication listener
     */

    interface UserProfileObserver {
        fun userProfileChanged(user: io.stanc.pogotool.firebase.node.FirebaseUserNode?)
    }

    fun addUserProfileObserver(observer: UserProfileObserver) {
        val weakObserver = WeakReference(observer)
        userProfileObservers[observer.hashCode()] = weakObserver
    }

    fun removeUserProfileObserver(observer: UserProfileObserver) {

        val checkCount = userProfileObservers.count()
        userProfileObservers.remove(observer.hashCode())
        val newCheckCount = userProfileObservers.count()
        if (newCheckCount >= checkCount) {
            Log.e(TAG, "could not remove observer: $observer from list: checkCount: $checkCount, newCheckCount: $newCheckCount")
        }
    }

    enum class AuthState {
        UserLoggedOut,
        UserLoggedInButUnverified,
        UserLoggedIn
    }

    interface AuthStateObserver {
        fun authStateChanged(newAuthState: AuthState)
    }

    fun addAuthStateObserver(observer: AuthStateObserver) {
        val weakObserver = WeakReference(observer)
        authStateObservers[observer.hashCode()] = weakObserver
    }

    fun removeAuthStateObserver(observer: AuthStateObserver) {
        authStateObservers.remove(observer.hashCode())
    }

    private val onAuthStateChanged: () -> Unit = {

        updateUserProfile()

        val authState = authState()
        Log.i(TAG, "onAuthStateChanged(${authState.name}) for user: $currentUser")
        authStateObservers.forEach { it.value.get()?.authStateChanged(authState) }

        when(authState) {
            AuthState.UserLoggedIn, AuthState.UserLoggedInButUnverified -> {
                updateUsersNotificationToken()
            }
            else -> { /* nothing */ }
        }
    }

    fun authState(): AuthState {

        currentUser?.isVerified?.let { isEmailVerified ->

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

    private fun startListenToUserProfileChanges() {
        currentUser?.let {
            databaseUsers.child(it.id).removeEventListener(userEventListener)
            databaseUsers.child(it.id).addChildEventListener(userEventListener)
        }
    }

    private fun stopListenToUserProfileChanges() {
        currentUser?.let {
            databaseUsers.child(it.id).removeEventListener(userEventListener)
        }
    }

    private val userEventListener = object: ChildEventListener {
        private val TAG = this.javaClass.name

        override fun onCancelled(p0: DatabaseError) {
            Log.e(TAG, "onCancelled(error: ${p0.code}, message: ${p0.message})")
        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
            Log.v(TAG, "onChildMoved(${p0.value}, p1: $p1), p0.key: ${p0.key}")
        }

        override fun onChildChanged(p0: DataSnapshot, p1: String?) {
            Log.d(TAG, "onChildChanged(${p0.value}, p1: $p1), p0.key: ${p0.key}")
        }

        override fun onChildAdded(p0: DataSnapshot, p1: String?) {
            Log.v(TAG, "onChildAdded(${p0.value}, p1: $p1), p0.key: ${p0.key} for ItemEventListener")
        }

        override fun onChildRemoved(p0: DataSnapshot) {
            Log.w(TAG, "onChildRemoved(${p0.value}), p0.key: ${p0.key} for ItemEventListener")
        }
    }
}